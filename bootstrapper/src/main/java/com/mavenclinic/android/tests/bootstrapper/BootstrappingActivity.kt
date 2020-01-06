package com.mavenclinic.android.tests.bootstrapper

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.mavenclinic.android.tests.apk.handleInstallationResponseIntent
import com.mavenclinic.android.tests.apk.installApk
import com.mavenclinic.android.tests.utilities.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Activity and app that actually loads the target app.
 * Operates w/o UI under control of main loader app.
 */
class BootstrappingActivity : AppCompatActivity() {

    companion object {
        const val STATE_FILE_PATH = "file_path"
    }

    private var filePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filePath = savedInstanceState?.getString( STATE_FILE_PATH )
        setContentView(R.layout.activity_main)

        Timber.i("Start action=${intent.action}")
        if (!handleIntent(intent)) {
            Timber.w("Unsupported start action: ${intent.action}")
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString( STATE_FILE_PATH, filePath )
    }

    override fun onNewIntent(intent: Intent?) {
        Timber.i("onNewIntent, action=${intent?.action}")
        if (!handleIntent(intent))
            super.onNewIntent(intent)
    }

    private fun handleIntent( intent: Intent?): Boolean {
        intent ?: return false

        return  if ( intent.action == Constants.ACTION_INSTALL_APK ){
            handleInstallIntent( intent )
            true
        } else
            try {
                handleInstallationResponseIntent(intent){ pi ->
                    Timber.i("Installation success: package=${pi}, filePath=${filePath}")
                    setResult(
                        Activity.RESULT_OK,
                        Intent().also {
                            it.putExtra( Constants.EXTRA_PACKAGE_INFO, pi )
                            it.putExtra( Constants.EXTRA_FILE_NAME, filePath )
                        }
                    )
                    finish()
                }
            } catch (e: Exception) {
                returnError(e)
                true
            }
    }

    private fun handleInstallIntent( intent: Intent) {

        Timber.i("Install intent, uri=${intent.data}")

        lifecycleScope.launch {

            withContext( Dispatchers.IO ) {
                try {
                    intent.data?.let { uri ->
                        filePath = intent.getStringExtra( Constants.EXTRA_FILE_NAME )
                        installApk(uri)
                    }
                } catch (e: Exception) {
                    returnError(e)
                }
            }
            // result will be in intent handled by 'handleInstallationResponseIntent'
        }
    }

    private fun returnError( t: Throwable ) {
        Timber.e("Returning error: ${t.javaClass.simpleName} ${t.message}")
        setResult( Constants.RESULT_ERROR, t.toErrorIntent() )
                finish()
    }


    private fun Throwable.toErrorIntent( ): Intent = Intent(Intent.ACTION_APP_ERROR).also {
        it.putExtra( Constants.EXTRA_ERROR_TYPE, this.javaClass.name )
        it.putExtra( Constants.EXTRA_ERROR_MESSAGE, this.message )
        it.putExtra( Constants.EXTRA_FILE_NAME, filePath )
    }
}
