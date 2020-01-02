package com.mavenclinic.android.tests.loader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.mavenclinic.android.tests.utility.asMillisecondTime
import com.mavenclinic.android.tests.utility.summary
import com.mavenclinic.android.tests.utility.toZonedDateTime
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_DOWNLOAD = "com.mavenclinic.test.DOWNLOAD"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent( intent: Intent) {

        when( intent.action ){
            ACTION_DOWNLOAD -> handleDownloadIntent(intent)
            else -> Timber.w("Unrecognized intent action: ${intent.action}")
        }

    }

    private fun handleDownloadIntent( intent: Intent ) {

        Timber.i("Download intent received: uri=${intent.data}")

        intent.data?.let { uri ->
            uri.lastPathSegment?.let { targetFileName ->
                Timber.i("Target file name=$targetFileName")

                lifecycleScope.launchWhenCreated {

                    try {
                        val file = downloadFile(this@MainActivity, uri, targetFileName )
                        val packageInfo = packageManager.getPackageArchiveInfo( file.path, 0 )

                        Timber.i("Downloaded package name=${packageInfo?.packageName}, version=${packageInfo?.versionName}, updated=${packageInfo?.lastUpdateTime?.asMillisecondTime()?.toZonedDateTime()?.toString()}")
                    } catch (e: Exception) {
                        Timber.e("Error when attempting to download file: ${e.summary()}")
                    }
                }
            }
        }

    }
}
