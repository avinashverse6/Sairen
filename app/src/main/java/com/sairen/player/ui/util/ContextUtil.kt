package com.sairen.player.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps the Activity from a (possibly wrapped) Compose context. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
