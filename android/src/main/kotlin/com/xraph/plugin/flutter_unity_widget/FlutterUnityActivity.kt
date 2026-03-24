package com.xraph.plugin.flutter_unity_widget

import android.graphics.PixelFormat
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity

/* 
The following Unity versions expect the mUnityPlayer property on the main activity: 
- 2020.3.46 or higher
- 2021.3.19 - 2021.3.20
- 2022.2.4 - 2022.3.18

Unity will function without it, but many Unity plugins (like ARFoundation) will not.
Implement FlutterUnityActivity or the interface to fix these plugins.

https://github.com/juicycleff/flutter-unity-view-widget/pull/908
*/

open class FlutterUnityActivity: FlutterActivity() {
    @JvmField
    var mUnityPlayer: java.lang.Object? = null
    /**
     * PixelFormat을 TRANSLUCENT로 설정해 Unity View 배경을 투명하게 만든다.
     * super.onCreate() 전에 호출해야 Surface 생성 시 포맷이 적용된다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFormat(PixelFormat.TRANSLUCENT)
        super.onCreate(savedInstanceState)
    }
}


/*
 A function that is called when initializing Unity.
 Expected use is to set a mUnityPlayer property, just as defined in FlutterUnityActivity above.
*/
interface IFlutterUnityActivity {
    fun setUnityPlayer(unityPlayer: java.lang.Object?)
}