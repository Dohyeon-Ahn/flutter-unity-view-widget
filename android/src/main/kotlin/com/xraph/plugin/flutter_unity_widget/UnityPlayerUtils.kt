package com.xraph.plugin.flutter_unity_widget

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowManager
import android.widget.FrameLayout
import com.unity3d.player.IUnityPlayerLifecycleEvents
import com.unity3d.player.UnityPlayer
import java.util.concurrent.CopyOnWriteArraySet


class UnityPlayerUtils {

    companion object {
        private const val LOG_TAG = "UnityPlayerUtils"

        var controllers: ArrayList<FlutterUnityWidgetController> = ArrayList()
        var unityPlayer: CustomUnityPlayer? = null
        var activity: Activity? = null
        var prevActivityRequestedOrientation: Int? = null

        var options: FlutterUnityWidgetOptions = FlutterUnityWidgetOptions()

        var unityPaused: Boolean = false
        var unityLoaded: Boolean = false
        var viewStaggered: Boolean = false

        private val mUnityEventListeners = CopyOnWriteArraySet<UnityEventListener>()

        fun focus() {
            try {
                unityPlayer!!.windowFocusChanged(unityPlayer!!.requestFocus())
                unityPlayer!!.resume()
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        /**
         * Create a new unity player with callback
         */
        @SuppressLint("NewApi")
        fun createUnityPlayer(ule: IUnityPlayerLifecycleEvents, callback: OnCreateUnityViewCallback?) {
            if (activity == null) {
                throw java.lang.Exception("Unity activity is null")
            }

            if (unityPlayer != null) {
                unityLoaded = true
                unityPlayer!!.bringToFront()
                unityPlayer!!.requestLayout()
                unityPlayer!!.invalidate()
                focus()
                callback?.onReady()
                return
            }

            try {
                unityPlayer = CustomUnityPlayer(activity!!, ule)

                // UnityPlayer 내부의 SurfaceView를 투명하게 설정
                // setZOrderOnTop(true)는 Unity가 다른 UI 위에 그려지게 하며 투명도를 허용합니다.
                // Flutter UI가 Unity 위에 올라와야 한다면 상황에 따라 조절이 필요할 수 있습니다.
                if (unityPlayer != null) {
                    // SurfaceView 포맷 설정
                    (unityPlayer as ViewGroup).getChildAt(0)?.let { surfaceView ->
                        if (surfaceView is android.view.SurfaceView) {
                            surfaceView.setZOrderOnTop(true)
                            surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
                        }
                    }
                }

                // Assign mUnityPlayer in the Activity, see FlutterUnityActivity.kt for more details
                if(activity is FlutterUnityActivity) {
                    (activity!! as FlutterUnityActivity)?.mUnityPlayer = (unityPlayer as java.lang.Object?);
                } else if(activity is IFlutterUnityActivity) {
                    (activity!! as IFlutterUnityActivity)?.setUnityPlayer(unityPlayer as java.lang.Object?);
                } else {
                     Log.e(LOG_TAG, "Could not set mUnityPlayer in activity");
                }
                

                // unityPlayer!!.z = (-1).toFloat()
                // addUnityViewToBackground(activity!!)
                unityLoaded = true

                if (!options.fullscreenEnabled) {
                    activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                    activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                } else {
                    activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                }

                // Unity View 배경 투명 처리
                activity!!.window.setFormat(PixelFormat.TRANSLUCENT)
                activity!!.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                focus()
                callback?.onReady()
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun postMessage(gameObject: String, methodName: String, message: String) {
            UnityPlayer.UnitySendMessage(gameObject, methodName, message)
        }

        fun pause() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.pause()
                    unityPaused = true
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun resume() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.resume()
                    unityPaused = false
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun unload() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.unload()
                    unityLoaded = false
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, e.toString())
            }
        }

        fun quitPlayer() {
            try {
                if (unityPlayer != null) {
                    unityPlayer!!.quit()
                    unityLoaded = false
                }
            } catch (e: Error) {
                e.message?.let { Log.e(LOG_TAG, it) }
            }
        }

        /**
         * Invoke by unity C#
         */
        @JvmStatic
        fun onUnitySceneLoaded(name: String, buildIndex: Int, isLoaded: Boolean, isValid: Boolean) {
            for (listener in mUnityEventListeners) {
                try {
                    listener.onSceneLoaded(name, buildIndex, isLoaded, isValid)
                } catch (e: Exception) {
                    e.message?.let { Log.e(LOG_TAG, it) }
                }
            }
        }

        /**
         * Invoke by unity C#
         */
        @JvmStatic
        fun onUnityMessage(message: String) {
            Log.d("UnityListener", "total listeners are ${mUnityEventListeners.size}")
            for (listener in mUnityEventListeners) {
                try {
                    listener.onMessage(message)
                } catch (e: Exception) {
                    e.message?.let { Log.e(LOG_TAG, it) }
                }
            }
        }

        fun addUnityEventListener(listener: UnityEventListener) {
            mUnityEventListeners.add(listener)
        }

        fun removeUnityEventListener(listener: UnityEventListener) {
            mUnityEventListeners.remove(listener)
        }

        private fun shakeActivity() {
            unityPlayer?.windowFocusChanged(true)
            if (prevActivityRequestedOrientation != null) {
                activity?.requestedOrientation = prevActivityRequestedOrientation!!
            }
        }

        fun removePlayer(controller: FlutterUnityWidgetController) {
            if (unityPlayer!!.parent == controller.view) {
                if (controllers.isEmpty()) {
                    (controller.view as FrameLayout).removeView(unityPlayer)
                    pause()
                    shakeActivity()
                } else {
                    controllers[controllers.size - 1].reattachToView()
                }
            }
        }

        fun reset() {
            unityLoaded = false
        }

        fun addUnityViewToGroup(group: ViewGroup) {
             val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
//             val layoutParams = ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT)
//            val layoutParams = ViewGroup.LayoutParams(570, 770)
            group.addView(unityPlayer, layoutParams)
        }

        fun addUnityViewToBackground() {
            if (unityPlayer == null) {
                return
            }
            if (unityPlayer!!.parent != null) {
                (unityPlayer!!.parent as ViewGroup).removeView(unityPlayer)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                unityPlayer!!.z = -1f
            }
            val layoutParams = ViewGroup.LayoutParams(1, 1)
            activity!!.addContentView(unityPlayer, layoutParams)
        }
    }
}