package com.mavenclinic.android.tests.loader

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import timber.log.Timber

class LoaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant( Timber.DebugTree() )
        AndroidThreeTen.init(this)
    }
}