package com.mavenclinic.android.tests.bootstrapper

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import timber.log.Timber

/**
 * Activity and app that actually loads the target app.
 * Operates w/o UI under control of main loader app.
 */
class BootstrappingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.i("hello")
    }
}
