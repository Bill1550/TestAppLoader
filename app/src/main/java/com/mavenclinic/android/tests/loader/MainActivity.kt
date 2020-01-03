package com.mavenclinic.android.tests.loader

import android.content.Intent
import android.content.pm.PackageInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.mavenclinic.android.tests.utilities.extensions.getApkInfoOrThrow
import com.mavenclinic.android.tests.utilities.time.asMillisecondTime
import com.mavenclinic.android.tests.utilities.time.toZonedDateTime
import com.mavenclinic.android.tests.utility.*
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.File

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

                    var file: File? = null

                    try {
                        showLoadingUx(true)
                        file = downloadFile(this@MainActivity, uri, targetFileName )
                        bindFileInfo(file)
                        val packageInfo = getApkInfoOrThrow( file )
                        bindPackageInfo(packageInfo)

                    } catch (e: Exception) {
                        Timber.e("Error when attempting to download file: ${e.summary()}")
                        displayError(e)
                        file?.safelyDelete()
                    } finally {
                        showLoadingUx(false)
                    }
                }
            }
        }

    }

    private fun showLoadingUx( loading: Boolean ) {
        progressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }

    private fun bindPackageInfo( info: PackageInfo? ) {
        Timber.i("Package info, last updated=${info?.lastUpdateTime}")
        packageNameValueView.text = info?.packageName ?: "--"
        versionNameValueView.text = info?.versionName ?: "--"
    }

    private fun bindFileInfo( file: File? ){
        fileNameValueView.text = file?.name ?: ""
        lastUpdatedValueView.text = file?.lastModified()?.asMillisecondTime()?.toZonedDateTime()?.toString() ?: ""
    }

    private fun displayError( t: Throwable ) {

        AlertDialog.Builder(this)
            .setTitle( R.string.title_error)
            .setMessage( "${t.javaClass.simpleName}\n${t.message}" )
            .setPositiveButton(R.string.button_label_exit){ dialog, _ ->
                finish()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}
