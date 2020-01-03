package com.mavenclinic.android.tests.bootstrapper

import android.app.Application
import timber.log.Timber

class BootstrapperApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant( Timber.DebugTree() )
    }
}