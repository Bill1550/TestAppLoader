package com.mavenclinic.android.tests.utility

import android.app.DownloadManager
import android.content.Context
import android.os.Build


val Context.downloadManager: DownloadManager?
    get() = if ( Build.VERSION.SDK_INT < 23 )
        getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    else
        getSystemService(DownloadManager::class.java)
