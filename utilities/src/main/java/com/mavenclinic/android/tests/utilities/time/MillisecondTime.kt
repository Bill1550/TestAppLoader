package com.mavenclinic.android.tests.utilities.time

import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

inline class MillisecondTime( val value: Long )

fun MillisecondTime.toZonedDateTime(): ZonedDateTime =
    ZonedDateTime.ofInstant( Instant.ofEpochMilli(value), ZoneId.systemDefault())

fun Long.asMillisecondTime(): MillisecondTime =
    MillisecondTime(this)