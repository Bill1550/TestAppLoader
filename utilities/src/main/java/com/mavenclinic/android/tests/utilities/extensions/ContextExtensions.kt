package com.mavenclinic.android.tests.utilities.extensions

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import java.io.File
import java.lang.IllegalArgumentException


val Context.downloadManager: DownloadManager?
    get() = if ( Build.VERSION.SDK_INT < 23 )
        getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    else
        getSystemService(DownloadManager::class.java)


/**
 * Returns a PackageInfo object for the specified file, or throws an error
 * if the file does not exist or isn't an APK.
 */
fun Context.getApkInfoOrThrow(file: File): PackageInfo {
    if (!file.exists() || !file.isFile() )
        throw IllegalArgumentException("APK file does not exist or is not a file")

    return packageManager.getPackageArchiveInfo(file.path,0) ?: throw IllegalArgumentException("${file.name} is not an APK")
}