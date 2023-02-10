package com.sygic.adas.visiontestapp

import android.app.Application
import com.testfairy.TestFairy

class VisionApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        if(!BuildConfig.DEBUG && BuildConfig.TEST_FAIRY_KEY.isNotBlank()) {
            TestFairy.begin(this, BuildConfig.TEST_FAIRY_KEY)
        }
    }
}