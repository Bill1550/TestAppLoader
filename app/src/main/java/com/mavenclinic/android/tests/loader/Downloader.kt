package com.mavenclinic.android.tests.loader

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.mavenclinic.android.tests.utility.downloadManager
import com.mavenclinic.android.tests.utility.safelyDelete
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

/**
 * Uses the download manager to download a file into the external storage directory as a
 * convenient suspend function.  Should be launched from a lifecycleScope to ensure the
 * broadcast receiver is removed on cancellation.
 */
suspend fun downloadFile(context: Context, source: Uri, fileName: String): File {
        Timber.i("Download ${source} to $fileName")

        return suspendCancellableCoroutine { continuation ->

            var downloadId: Long? = null

            val file = File(context.getExternalFilesDir(null), fileName).apply {
                safelyDelete()  // make sure file is initially deleted.
            }

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(broadcastContext: Context?, intent: Intent?) {
                    Timber.i(
                        "Broadcast received: action=${intent?.action}, downloadId=${intent?.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID,
                            -1
                        )}"
                    )

                    context.unregisterReceiver(this)

                    // TODO check for error
                    continuation.resume(file)
                }
            }

            continuation.invokeOnCancellation {
                downloadId?.let {
                    context.downloadManager?.remove(it)
                }
                context.unregisterReceiver(receiver)
            }

            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )

            downloadId = context.downloadManager?.enqueue(
                DownloadManager.Request(source)
                    .setDestinationInExternalFilesDir(context, null, fileName)
                    .apply { Timber.i("Enqueuing $this") }
            )
        }
}