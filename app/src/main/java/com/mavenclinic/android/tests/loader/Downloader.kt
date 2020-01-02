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
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

                    downloadId?.let { id ->

                        context.downloadManager?.getUriForDownloadedFile(id)?.let {
                            // file was successfully downloaded
                            continuation.resume(file)
                        } ?: let {
                            // there was some sort of error
                            context.downloadManager?.query(DownloadManager.Query().setFilterById(id))
                                ?.takeIf {it.moveToFirst() }?.let { cursor ->
                                    val reason = cursor.getInt( cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                                    continuation.resumeWithException (
                                        when( reason ){
                                            404 -> FileNotFoundException("Can not find ${source.path}")
                                            DownloadManager.ERROR_HTTP_DATA_ERROR -> IOException("HTTP data error")
                                            DownloadManager.ERROR_INSUFFICIENT_SPACE -> OutOfMemoryError("Insufficient space for file")
                                            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> IOException("Too many redirects")
                                            else -> Exception("Error downloading: $reason")
                                    }
                                )
                            } ?: continuation.resumeWithException( Exception("Unable to download file"))
                        }
                    }
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