/*
 * SPDX-FileCopyrightText: 2017 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.GET_ACCOUNTS
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_PHONE_STATE
import android.Manifest.permission.RECEIVE_SMS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.CrossProfileApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.R
import org.microg.gms.common.Constants
import org.microg.tools.selfcheck.InstalledPackagesChecks
import org.microg.tools.selfcheck.PermissionCheckGroup
import org.microg.tools.selfcheck.RomSpoofSignatureChecks
import org.microg.tools.selfcheck.SelfCheckGroup
import org.microg.tools.selfcheck.SelfCheckGroup.CheckResolver
import org.microg.tools.selfcheck.SelfCheckGroup.Result
import org.microg.tools.selfcheck.SelfCheckGroup.ResultCollector
import org.microg.tools.selfcheck.SystemChecks

private const val TAG = "SelfCheck"

class SelfCheckFragment : PreferenceFragmentCompat() {
    private var allPassed: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        reset()
    }

    private fun reset() {
        val screen = preferenceScreen ?: return
        val context = requireContext()
        screen.removeAll()
        // Only checks that did not pass are shown, until one is added this shows all passed
        allPassed = ResultPreference(context, R.drawable.ic_self_check_ok).apply {
            isPersistent = false
            isIconSpaceReserved = false
            setTitle(R.string.self_check_all_passed)
        }.also { screen.addPreference(it) }
        for (group in getSelfChecks(context, includePermissions = true)) {
            val category = PreferenceCategory(context).apply {
                title = group.getGroupName(context)
                isIconSpaceReserved = false
                isVisible = false
            }
            screen.addPreference(category)
            val collector = CategoryResultCollector(category)
            try {
                group.doChecks(context, collector)
            } catch (e: Exception) {
                Log.w(TAG, "Failed during check ${group.getGroupName(context)}", e)
                collector.addResult("Self-check failed:", Result.Negative, "An exception occurred during self-check. Please report this issue.")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        reset()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        reset()
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Shows each result that did not pass as a preference with a status icon, checks with a resolver can be clicked
     * to resolve.
     */
    private inner class CategoryResultCollector(private val category: PreferenceCategory) : ResultCollector {
        override fun addResult(name: String?, value: Result?, resolution: String?) = addResult(name, value, resolution, null)

        override fun addResult(name: String?, value: Result?, resolution: String?, resolver: CheckResolver?) {
            if (value == null || value == Result.Positive) return
            activity?.runOnUiThread {
                val context = context ?: return@runOnUiThread
                val statusIcon = if (value == Result.Negative) R.drawable.ic_self_check_warn else org.microg.gms.base.core.R.drawable.ic_info_outline
                category.addPreference(ResultPreference(context, statusIcon).apply {
                    isPersistent = false
                    isSingleLineTitle = false
                    isIconSpaceReserved = false
                    title = name
                    summary = resolution
                    if (resolver != null) {
                        setOnPreferenceClickListener {
                            resolver.tryResolve(this@SelfCheckFragment)
                            true
                        }
                    }
                })
                category.isVisible = true
                allPassed?.isVisible = false
            }
        }
    }

    /**
     * Shows the check result as status icon on the end.
     */
    private class ResultPreference(context: Context, @DrawableRes private val statusIcon: Int) : Preference(context) {
        init {
            widgetLayoutResource = R.layout.preference_widget_status
        }

        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            (holder.findViewById(R.id.status_icon) as ImageView).setImageResource(statusIcon)
        }
    }

    /**
     * Opens self-check in microG Settings.
     */
    class AsActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("x-gms-settings://self-check"), this, MainSettingsActivity::class.java))
            finish()
        }
    }

    companion object {
        /**
         * Runs the basic checks (signature spoofing, installed packages and system), skipping optional permissions.
         * Blocking, call from a background thread.
         */
        fun basicChecksPassed(context: Context): Boolean {
            var passed = true
            val collector = object : ResultCollector {
                override fun addResult(name: String?, value: Result?, resolution: String?) {
                    if (value == Result.Negative) passed = false
                }

                override fun addResult(name: String?, value: Result?, resolution: String?, resolver: CheckResolver?) = addResult(name, value, resolution)
            }
            for (group in getSelfChecks(context, includePermissions = false)) {
                runCatching { group.doChecks(context, collector) }.onFailure { Log.w(TAG, it) }
            }
            return passed
        }

        private fun getSelfChecks(context: Context, includePermissions: Boolean) = buildList<SelfCheckGroup> {
            if (context.packageName == Constants.GMS_PACKAGE_NAME) add(RomSpoofSignatureChecks())
            add(InstalledPackagesChecks())
            if (SDK_INT >= 23) {
                if (includePermissions) add(PermissionChecks())
                add(SystemChecks())
            }
        }
    }

    private class PermissionChecks : PermissionCheckGroup(*buildList {
        add(ACCESS_COARSE_LOCATION)
        add(ACCESS_FINE_LOCATION)
        if (SDK_INT >= 29) add(ACCESS_BACKGROUND_LOCATION)
        add(READ_EXTERNAL_STORAGE)
        add(WRITE_EXTERNAL_STORAGE)
        add(GET_ACCOUNTS)
        if (SDK_INT >= 33) add(POST_NOTIFICATIONS)
        add(READ_PHONE_STATE)
        add(RECEIVE_SMS)
    }.toTypedArray()) {
        override fun doChecks(context: Context, collector: ResultCollector) {
            super.doChecks(context, collector)
            val pm = context.packageManager
            // Add SYSTEM_ALERT_WINDOW appops permission
            if (SDK_INT >= 23) try {
                val permLabel = pm.getPermissionInfo("android.permission.SYSTEM_ALERT_WINDOW", 0).loadLabel(pm)
                collector.addResult(
                    context.getString(org.microg.tools.ui.R.string.self_check_name_permission, permLabel),
                    if (Settings.canDrawOverlays(context)) Result.Positive else Result.Negative,
                    context.getString(org.microg.tools.ui.R.string.self_check_resolution_permission)
                ) { fragment ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    @Suppress("DEPRECATION")
                    fragment.startActivityForResult(intent, 42)
                }
            } catch (e: Exception) {
                Log.w("SelfCheckPerms", e)
            }
            // Add INTERACT_ACROSS_PROFILES appop permission (INTERACT_ACROSS_USERS is superior)
            if (SDK_INT >= 30) try {
                val crossProfile = context.getSystemService(CrossProfileApps::class.java)
                collector.addResult(
                    context.getString(org.microg.tools.ui.R.string.self_check_name_permission_interact_across_profiles),
                    if (context.checkSelfPermission("android.permission.INTERACT_ACROSS_USERS") == PackageManager.PERMISSION_GRANTED
                        || crossProfile.canInteractAcrossProfiles()) Result.Positive else Result.Negative,
                    context.getString(org.microg.tools.ui.R.string.self_check_resolution_permission),
                    if (crossProfile.canRequestInteractAcrossProfiles()) CheckResolver { fragment ->
                        @Suppress("DEPRECATION")
                        fragment.startActivityForResult(crossProfile.createRequestInteractAcrossProfilesIntent(), 43)
                    } else null
                )
            } catch (e: Exception) {
                Log.w("SelfCheckPerms", e)
            }
        }
    }
}
