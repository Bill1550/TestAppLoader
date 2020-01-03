package com.mavenclinic.android.tests.bootstrapper

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.mavenclinic.android.tests.utilities.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Activity and app that actually loads the target app.
 * Operates w/o UI under control of main loader app.
 */
class BootstrappingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.i("Start action=${intent.action}")
        if ( intent.action == Constants.ACTION_INSTALL_APK )
            handleInstallIntent(intent)
        else {
            Timber.w("Unsupported start action: ${intent.action}")
            finish()
        }
    }

    private fun handleInstallIntent( intent: Intent) {

        Timber.i("Install intent, uri=${intent.data}")

        lifecycleScope.launch {

            delay(1000)

            Timber.i("done w/ load")

            // simulate an error
            //setResult(Constants.RESULT_ERROR, Exception("this is a test").toErrorIntent() )
            setResult(
                Activity.RESULT_OK,
                Intent().also { it.putExtra( Constants.EXTRA_PACKAGE_INFO, packageManager.getPackageInfo(packageName,0)) }
            )

            finish()
        }
    }

    private fun Throwable.toErrorIntent( ): Intent = Intent(Intent.ACTION_APP_ERROR).also {
        it.putExtra( Constants.EXTRA_ERROR_TYPE, this.javaClass.name )
        it.putExtra( Constants.EXTRA_ERROR_MESSAGE, this.message )
    }
}
