package com.cbstudio.autostartup

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds


/**
 * Created by ImL1s on 2018/7/26.
 * Description:
 */
class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d(DEBUG_TAG, "onCreate")
    }

}