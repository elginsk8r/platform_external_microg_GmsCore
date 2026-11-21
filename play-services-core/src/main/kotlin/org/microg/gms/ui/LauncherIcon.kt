/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.R

private const val TAG = "LauncherIcon"
private const val LAUNCHER_ACTIVITY = "org.microg.gms.ui.SettingsActivity"

fun getLauncherComponent(context: Context) = ComponentName(context, LAUNCHER_ACTIVITY)

/**
 * The launcher icon may only be hidden when microG Settings are reachable from the system Settings app instead.
 */
fun canHideLauncherIcon(context: Context): Boolean {
    val isSystemApp = (context.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    return context.resources.getBoolean(R.bool.ia_settings_enabled) && isSystemApp
}

fun isLauncherIconHidden(context: Context): Boolean {
    return context.packageManager.getComponentEnabledSetting(getLauncherComponent(context)) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
}

fun setLauncherIconHidden(context: Context, hidden: Boolean) {
    val state = if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    context.packageManager.setComponentEnabledSetting(getLauncherComponent(context), state, PackageManager.DONT_KILL_APP)
}

/**
 * Restores the launcher icon if it was hidden but microG Settings are no longer reachable otherwise.
 */
fun restoreLauncherIconIfNeeded(context: Context) {
    if (!canHideLauncherIcon(context) && isLauncherIconHidden(context)) {
        Log.i(TAG, "microG Settings are not reachable from system Settings, restoring launcher icon")
        setLauncherIconHidden(context, false)
    }
}

class LauncherIconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        restoreLauncherIconIfNeeded(context)
    }
}
