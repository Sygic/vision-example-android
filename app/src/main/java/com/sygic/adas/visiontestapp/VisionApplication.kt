package com.sygic.adas.visiontestapp

import android.app.Application
import com.testfairy.TestFairy

class VisionApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        if(!BuildConfig.DEBUG) {
            TestFairy.begin(this, "SDK-AQzTxOBz")
        }
    }
}