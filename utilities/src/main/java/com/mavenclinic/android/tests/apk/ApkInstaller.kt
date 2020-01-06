package com.mavenclinic.android.tests.apk

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.IllegalStateException

private const val ACTION_INSTALLATION_COMPLETE = "com.mavenclinic.android.tests.apk.ACTION_INSTALLATION_COMPLETE"

suspend fun AppCompatActivity.installApk(apiUri: Uri ) {
        Timber.i("Install APK from $apiUri")
        packageManager.packageInstaller.let { installer ->

            withContext( Dispatchers.IO ) {
                val sessionId = installer.createSession(
                        PackageInstaller.SessionParams( PackageInstaller.SessionParams.MODE_FULL_INSTALL)
                )

                val size = contentResolver.query( apiUri, null, null, null, null )?.use { cursor ->
                    cursor.moveToFirst()?.takeIf { it }?.let {
                        cursor.getLong( cursor.getColumnIndex( OpenableColumns.SIZE ) )
                    }
                } ?: -1L

                Timber.i("APK size=$size")

                installer.openSession(sessionId).use { session ->

                    // copy APK into session
                    contentResolver.openInputStream( apiUri)?.use {  inputStream ->
                        session.openWrite("package", 0, size).use { pkgStream ->
                            val bytes = ByteArray(16384)
                            var n = 0
                            while ( inputStream.read(bytes).also { n = it } > 0 )
                                pkgStream.write(bytes, 0, n)

                            session.fsync(pkgStream)
                        }
                    }

                    val pendingIntent = PendingIntent.getActivity(
                        this@installApk, 0,
                        Intent(this@installApk, this@installApk.javaClass).apply {
                            action = ACTION_INSTALLATION_COMPLETE }, 0
                    )

                    Timber.i("Committing installation session")
                    session.commit(pendingIntent.intentSender)
                }
            }
        }
}

fun AppCompatActivity.handleInstallationResponseIntent( intent: Intent, successHandler: (PackageInfo)->Unit  ) : Boolean {
    if ( intent.action != ACTION_INSTALLATION_COMPLETE )
      return false

    Timber.i("Installation complete intent, extras=${intent.extras?.keySet()?.toList()}")
    intent.extras?.let { extras ->
        val status = extras.getInt( PackageInstaller.EXTRA_STATUS ).apply { Timber.i("Installation status=$this") }
        when( status ){
            PackageInstaller.STATUS_PENDING_USER_ACTION -> (extras.get(Intent.EXTRA_INTENT) as? Intent)?.let { startActivity(it) }

            PackageInstaller.STATUS_SUCCESS ->
                extras.getString( PackageInstaller.EXTRA_PACKAGE_NAME )?.let { pn ->
                    successHandler( packageManager.getPackageInfo(pn,0) )
                }

            PackageInstaller.STATUS_FAILURE,
            PackageInstaller.STATUS_FAILURE_ABORTED,
            PackageInstaller.STATUS_FAILURE_BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                throw IllegalStateException("Installation failure: ${extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)}")

            else -> Timber.e("Unrecognized installation status: $status")
        }
    }


    return true
}
