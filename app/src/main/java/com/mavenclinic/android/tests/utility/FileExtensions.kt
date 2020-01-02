package com.mavenclinic.android.tests.utility

import timber.log.Timber
import java.io.File

/**
 * Ensures that this directory exists
 */
fun File.ensureDir(): File {
    if (!exists())
        check(mkdirs()) { "Unable to create directory: ${this.path}" }

    return this
}

fun File.safelyDelete(): Boolean {
    return try {
        delete()
    } catch ( t: Throwable ) {
        Timber.e("Exception when deleting file: ${t.summary()}")
        false
    }
}