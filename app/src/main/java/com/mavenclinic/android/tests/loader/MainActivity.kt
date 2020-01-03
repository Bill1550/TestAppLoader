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
        const val EXTRA_TARGET = "target"
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
        Timber.i("target='${intent.getStringExtra("target")}'")

//        intent.data?.let { uri ->
//            uri.lastPathSegment?.let { downloadFileName ->
//                Timber.i("Target file name=$downloadFileName")

        lifecycleScope.launchWhenCreated {

            var file: File? = null

            try {
                showLoadingUx(true)
                val uri = requireNotNull( intent.data ){ "Missing download URL"}
                val downloadFileName = requireNotNull( uri.lastPathSegment ){"Improperly formatted URL"}
                val target = requireNotNull( intent.getStringExtra(EXTRA_TARGET)){"Target missing"}
                file = downloadFile(this@MainActivity, uri, downloadFileName )
                bindFileInfo(file, target)
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

    private fun showLoadingUx( loading: Boolean ) {
        progressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }

    private fun bindPackageInfo( info: PackageInfo? ) {
        Timber.i("Package info, last updated=${info?.lastUpdateTime}")
        packageNameValueView.text = info?.packageName ?: "--"
        versionNameValueView.text = info?.versionName ?: "--"
    }

    private fun bindFileInfo( file: File?, target: String ){
        fileNameValueView.text = file?.name ?: ""
        lastUpdatedValueView.text = file?.lastModified()?.asMillisecondTime()?.toZonedDateTime()?.toString() ?: ""
        targetValueView.text = target
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


    private fun getBootstrapperIntent( target: String ): Intent {

        return Intent()
    }
}
