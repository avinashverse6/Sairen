package com.sairen.player.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Permission helpers.
 *
 * Audio-only by design:
 *  * API 33+ -> `READ_MEDIA_AUDIO` (grants audio only; no photos/video).
 *  * API 26..32 -> `READ_EXTERNAL_STORAGE` declared with `maxSdkVersion="32"`.
 * No `MANAGE_EXTERNAL_STORAGE`, no `WRITE_EXTERNAL_STORAGE`, no location.
 */
object Permissions {

    fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasAudioAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, audioPermission()) ==
            PackageManager.PERMISSION_GRANTED

    /** Notifications only became a runtime permission in API 33. */
    fun requiresNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasNotificationAccess(context: Context): Boolean =
        !requiresNotificationPermission() ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * True while the system is still willing to show the request dialog.
     * Once this is false after a denial, the only path left is app settings.
     */
    fun shouldShowRationale(activity: Activity?, permission: String): Boolean {
        if (activity == null) return false
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
