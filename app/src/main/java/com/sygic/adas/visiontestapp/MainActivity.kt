package com.sygic.adas.visiontestapp

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.IdRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }
}

fun Activity.getSupportActionBar(): ActionBar? {
    return (this as AppCompatActivity).supportActionBar
}

fun MainActivity.navigate(@IdRes id: Int) {
    findNavController(R.id.nav_fragment).navigate(id)
}