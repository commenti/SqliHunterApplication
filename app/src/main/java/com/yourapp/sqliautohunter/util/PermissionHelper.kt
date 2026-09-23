package com.yourapp.sqliautohunter.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Runtime permission and OS-exemption gatekeeper.
 *
 * Three buckets the app actually cares about:
 *   1. POST_NOTIFICATIONS (API 33+) — required for the foreground service
 *      notification. On API 26–32 it's implicitly granted.
 *   2. Battery optimization exemption — the service survives Doze only if the
 *      user exempts us. Request via ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.
 *   3. Overlay / full-screen intent permissions — not used; left out.
 *
 * Everything here is read-only or intent-based. No side effects beyond
 * launching a system dialog. Callers own the activity-result flow.
 */
object PermissionHelper {

    /** Permissions the app must request at first launch. */
    fun requiredRuntimePermissions(): List<String> {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        return perms
    }

    /** True if every required runtime permission is already granted. */
    fun hasAllRuntimePermissions(context: Context): Boolean {
        return requiredRuntimePermissions().all { granted(context, it) }
    }

    /** Individual check — wraps ContextCompat, returns true on pre-API-33 for POST_NOTIF. */
    fun granted(context: Context, permission: String): Boolean {
        if (permission == Manifest.permission.POST_NOTIFICATIONS &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            return true
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** True if notifications are enabled at both the app and channel level. */
    fun notificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** True if the app is already exempt from battery optimization. */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Launch the system dialog to request battery-optimization exemption.
     * Falls back to the settings screen if the direct intent isn't resolvable.
     * Activity-scoped — pass the host activity, not a Context wrapper.
     */
    fun requestIgnoreBatteryOptimizations(activity: Activity, requestCode: Int) {
        val pkg = activity.packageName
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:$pkg"))
        if (direct.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(direct, requestCode)
            return
        }
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        activity.startActivityForResult(fallback, requestCode)
    }

    /** Open the OS notification settings for this app — used when notif is off. */
    fun openNotificationSettings(activity: Activity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${activity.packageName}"))
        }
        activity.startActivity(intent)
    }

    /** Open this app's system settings page (fallback for anything else). */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${activity.packageName}"))
        activity.startActivity(intent)
    }

    /**
     * True if we have enough to run the foreground service in a stable way:
     *   - notification permission granted (or N/A)
     *   - battery optimization ignored
     * Used by the Dashboard banner to nudge the user toward the two prompts.
     */
    fun isScanReady(context: Context): Boolean =
        hasAllRuntimePermissions(context) && isBatteryOptimizationIgnored(context)

    /** Convenience for the UI: which of the two gates is currently closed? */
    data class ReadinessGaps(
        val missingNotificationPermission: Boolean,
        val batteryOptimizationNotIgnored: Boolean
    ) {
        val any: Boolean
            get() = missingNotificationPermission || batteryOptimizationNotIgnored
    }

    fun readGaps(context: Context): ReadinessGaps = ReadinessGaps(
        missingNotificationPermission = !hasAllRuntimePermissions(context),
        batteryOptimizationNotIgnored = !isBatteryOptimizationIgnored(context)
    )
}