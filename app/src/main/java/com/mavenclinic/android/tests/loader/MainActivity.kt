package com.mavenclinic.android.tests.loader

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.mavenclinic.android.tests.utilities.Constants
import com.mavenclinic.android.tests.utilities.extensions.getApkInfoOrThrow
import com.mavenclinic.android.tests.utilities.extensions.versionCodeCompat
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
        const val RC_CALL_BOOTSTRAPPER = 123
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

        lifecycleScope.launchWhenCreated {

            var file: File? = null

            try {
                showLoadingUx(true)
                val uri = requireNotNull( intent.data ){ "Missing download URL"}
                val downloadFileName = requireNotNull( uri.lastPathSegment ){"Improperly formatted URL"}
                val target = requireNotNull( intent.getStringExtra(EXTRA_TARGET)){"Target missing"}
                bindArguments( uri, target)
                file = downloadFile(this@MainActivity, uri, downloadFileName )
                bindFileInfo(file)
                val apkInfo = getApkInfoOrThrow( file )
                bindPackageInfo(apkInfo)

                val loadingUri = FileProvider.getUriForFile(this@MainActivity, "com.mavenclinic.tests.loader.fileprovider", file)

                val bootstrapperIntent = getBootstrapperIntent(target).apply {
                    data = loadingUri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                startActivityForResult(bootstrapperIntent, RC_CALL_BOOTSTRAPPER)
            } catch (e: Exception) {
                Timber.e("Error when attempting to download file: ${e.summary()}")
                displayError(e)
                file?.safelyDelete()
            } finally {
                showLoadingUx(false)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode){
            RC_CALL_BOOTSTRAPPER -> handleLoadResult(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun handleLoadResult( resultCode: Int, data: Intent? ) {
        Timber.i("loader result: code=${resultCode}")
        if ( resultCode != Activity.RESULT_OK ){
            displayError( data?.getStringExtra(Constants.EXTRA_ERROR_TYPE) ?: "Unknown", data?.getStringExtra(Constants.EXTRA_ERROR_MESSAGE))
        } else {
            displaySuccess( data?.getParcelableExtra( Constants.EXTRA_PACKAGE_INFO ) )
        }
    }

    private fun showLoadingUx( loading: Boolean ) {
        progressBar.visibility = if (loading) View.VISIBLE else View.INVISIBLE
    }

    private fun bindArguments(uri: Uri, target: String ) {
        // TODO add source URI to layout
        targetValueView.text = target
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

    private fun displayError( t: Throwable ){
        displayError( t.javaClass.simpleName, t.message)
    }

    private fun displayError( errorClass: String, message: String? ) {

        AlertDialog.Builder(this)
            .setTitle( R.string.title_error)
            .setMessage( "${errorClass}\n${message}" )
            .setPositiveButton(R.string.button_label_exit){ dialog, _ ->
                finish()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun displaySuccess( info: PackageInfo? ) {
        AlertDialog.Builder(this)
            .setTitle( R.string.title_success)
            .setMessage( "Installed ${info?.packageName}" )
            .setPositiveButton(R.string.button_label_exit){ dialog, _ ->
                finish()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }


    /**
     * Make sure desired bootstrapper is installed, if not install it.
     * Throws an exception if the bootstrapper cannot be loaded.
     */
    private suspend fun getBootstrapperIntent( target: String ): Intent {


        val info = packageManager.getPackageInfo( "${BuildConfig.BOOTSTRAPPER_APP_ID}.${target}", 0)

        checkNotNull(info){"Bootstrapper not available for $target"}

        Timber.i("Bootstrapper id=${info.packageName} version=${info.versionCodeCompat}")

        return Intent(Constants.ACTION_INSTALL_APK).apply {
            component = ComponentName( info.packageName, "${BuildConfig.BOOTSTRAPPER_APP_ID}.BootstrappingActivity")
        }
    }
}
