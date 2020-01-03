package com.mavenclinic.android.tests.utilities.extensions

import android.content.pm.PackageInfo
import android.os.Build

val PackageInfo.versionCodeCompat: Long
    get() = if (Build.VERSION.SDK_INT >= 28 )
                longVersionCode
            else
                versionCode.toLong()