/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.R
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.checkin.CheckinPreferences
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.GcmPrefs
import org.microg.gms.safetynet.SafetyNetPreferences
import org.microg.gms.ui.settings.SettingsProvider
import org.microg.gms.ui.settings.getAllSettingsProviders
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {
    private val createdPreferences = mutableListOf<Preference>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        findPreference<Preference>(PREF_ACCOUNTS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.accountManagerFragment)
            true
        }
        findPreference<Preference>(PREF_CHECKIN)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openCheckinSettings)
            true
        }
        findPreference<Preference>(PREF_GCM)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openGcmSettings)
            true
        }
        findPreference<Preference>(PREF_SNET)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openSafetyNetSettings)
            true
        }
        findPreference<Preference>(PREF_LOCATION)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openLocationSettings)
            true
        }
        findPreference<Preference>(PREF_VENDING)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openVendingSettings)
            true
        }
        findPreference<Preference>(PREF_WORK_PROFILE)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openWorkProfileSettings)
            true
        }
        findPreference<Preference>(PREF_GAMES)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.gameManagerFragment)
            true
        }
        findPreference<Preference>(PREF_SELF_CHECK)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.selfcheckFragment)
            true
        }
        findPreference<Preference>(PREF_GOOGLE_MORE)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openMoreGoogleSettings)
            true
        }
        findPreference<Preference>(PREF_PASSKEYS)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.passkeyManagerFragment)
            true
        }
        findPreference<SwitchPreferenceCompat>(PREF_HIDE_LAUNCHER_ICON)?.apply {
            restoreLauncherIconIfNeeded(requireContext())
            isVisible = canHideLauncherIcon(requireContext())
            isChecked = isLauncherIconHidden(requireContext())
            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                if (newValue == true && requireActivity().intent.component == getLauncherComponent(requireContext())) {
                    // The task started through the launcher alias is removed when it is disabled, reopen in a new task
                    startActivity(Intent(requireContext(), MainSettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK))
                }
                setLauncherIconHidden(requireContext(), newValue == true)
                true
            }
        }

        findPreference<Preference>(PREF_ABOUT)!!.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                findNavController().navigate(requireContext(), R.id.openAbout)
                true
            }
            summary = getString(org.microg.tools.ui.R.string.about_version_str, AboutFragment.getSelfVersion())
        }

        for (entry in getAllSettingsProviders(requireContext()).flatMap { it.getEntriesStatic(requireContext()) }) {
            entry.createPreference()
        }
        if (!showIcons) {
            for (category in (SERVICE_CATEGORIES + PREFCAT_FOOTER).mapNotNull { findPreference<PreferenceCategory>(it) }) {
                category.isIconSpaceReserved = false
                for (i in 0 until category.preferenceCount) category.getPreference(i).hideIcon()
            }
        }
        sortServiceCategories()
        updateCategoryVisibility()
    }

    private val showIcons: Boolean
        get() = resources.getBoolean(R.bool.config_settings_show_icons)

    private fun Preference.hideIcon() {
        // Reading icon loads it from its resource id first, so clearing it is not a no-op
        if (icon != null) icon = null
        isIconSpaceReserved = false
    }

    /**
     * Sorts entries of service categories by their title, like the Google services settings.
     */
    private fun sortServiceCategories() {
        val collator = Collator.getInstance()
        for (category in SERVICE_CATEGORIES.mapNotNull { findPreference<PreferenceCategory>(it) }) {
            category.isOrderingAsAdded = false
            (0 until category.preferenceCount).map { category.getPreference(it) }
                .sortedWith { a, b -> collator.compare(a.title?.toString().orEmpty(), b.title?.toString().orEmpty()) }
                .forEachIndexed { index, preference -> preference.order = index }
        }
    }

    private fun updateCategoryVisibility() {
        for (category in (SERVICE_CATEGORIES + PREFCAT_HEADER).mapNotNull { findPreference<PreferenceCategory>(it) }) {
            category.isVisible = (0 until category.preferenceCount).any { category.getPreference(it).isVisible }
        }
    }

    private fun SettingsProvider.Companion.Entry.createPreference(): Preference? {
        val preference = Preference(requireContext()).fillFromEntry(this)
        try {
            if (findPreference<PreferenceCategory>(when (group) {
                    SettingsProvider.Companion.Group.HEADER -> PREFCAT_HEADER
                    SettingsProvider.Companion.Group.GOOGLE -> "prefcat_google_apps"
                    SettingsProvider.Companion.Group.OTHER -> "prefcat_other_services"
                    SettingsProvider.Companion.Group.FOOTER -> PREFCAT_FOOTER
                    SettingsProvider.Companion.Group.PERSONAL_DEVICE_SAFETY -> "prefcat_personal_device_safety"
                    SettingsProvider.Companion.Group.PRIVACY_SECURITY -> "prefcat_privacy_security"
                    SettingsProvider.Companion.Group.DEVICE_POLICY -> "prefcat_device_policy"
                    SettingsProvider.Companion.Group.GOOGLE_APPS -> "prefcat_google_apps"
                })?.addPreference(preference) == true) {
                createdPreferences.add(preference)
                return preference
            } else {
                Log.w(TAG, "Preference not added $key")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed adding preference $key", e)
        }
        return null
    }

    private fun Preference.fillFromEntry(entry: SettingsProvider.Companion.Entry): Preference {
        key = entry.key
        title = entry.title
        summary = entry.summary
        icon = entry.icon
        if (!showIcons && entry.group != SettingsProvider.Companion.Group.HEADER) hideIcon()
        isPersistent = false
        isVisible = true
        setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), entry.navigationId)
            true
        }
        return this
    }

    override fun onResume() {
        super.onResume()
        val context = requireContext()
        if (GcmPrefs.get(requireContext()).isEnabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            database.close()
            findPreference<Preference>(PREF_GCM)!!.summary = context.getString(org.microg.gms.base.core.R.string.service_status_enabled_short) + " - " + context.resources.getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount)
        } else {
            findPreference<Preference>(PREF_GCM)!!.setSummary(org.microg.gms.base.core.R.string.service_status_disabled_short)
        }

        findPreference<Preference>(PREF_CHECKIN)!!.setSummary(if (CheckinPreferences.isEnabled(requireContext())) org.microg.gms.base.core.R.string.service_status_enabled_short else org.microg.gms.base.core.R.string.service_status_disabled_short)
        findPreference<Preference>(PREF_SNET)!!.setSummary(if (SafetyNetPreferences.isEnabled(requireContext())) org.microg.gms.base.core.R.string.service_status_enabled_short else org.microg.gms.base.core.R.string.service_status_disabled_short)

        // Cloud messaging and device attestation depend on device registration
        val checkinEnabled = CheckinPreferences.isEnabled(context)
        for (preference in listOf(PREF_GCM, PREF_SNET).mapNotNull { findPreference<Preference>(it) }) {
            preference.isEnabled = checkinEnabled
            if (!checkinEnabled) preference.setSummary(R.string.pref_requires_checkin_summary)
        }

        lifecycleScope.launch {
            val passed = withContext(Dispatchers.IO) { SelfCheckFragment.basicChecksPassed(context) }
            // A dismissed warning stays hidden until the basic checks pass again
            val uiPreferences = context.getSharedPreferences(UI_PREFERENCES, Context.MODE_PRIVATE)
            if (passed) uiPreferences.edit().remove(KEY_SELF_CHECK_WARNING_DISMISSED).apply()
            findPreference<IssueCardPreference>(PREF_SELF_CHECK_CARD)?.apply {
                label = getString(org.microg.tools.ui.R.string.self_check_title)
                title = getString(if (passed) R.string.self_check_card_ok_title else R.string.self_check_card_warn_title)
                summary = getString(if (passed) R.string.self_check_card_ok_summary else R.string.self_check_card_warn_summary)
                buttonText = getString(R.string.self_check_card_button)
                positive = passed
                onButtonClick = { findNavController().navigate(requireContext(), R.id.selfcheckFragment) }
                onDismiss = if (passed) null else {
                    {
                        uiPreferences.edit().putBoolean(KEY_SELF_CHECK_WARNING_DISMISSED, true).apply()
                        isVisible = false
                    }
                }
                isVisible = passed || !uiPreferences.getBoolean(KEY_SELF_CHECK_WARNING_DISMISSED, false)
            }
        }

        lifecycleScope.launchWhenResumed {
            val entries = getAllSettingsProviders(requireContext()).flatMap { it.getEntriesDynamic(requireContext()) }
            for (preference in createdPreferences) {
                if (!entries.any { it.key == preference.key }) preference.isVisible = false
            }
            for (entry in entries) {
                val preference = createdPreferences.find { it.key == entry.key }
                if (preference != null) preference.fillFromEntry(entry)
                else entry.createPreference()
            }
            sortServiceCategories()
            updateCategoryVisibility()
        }
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_SNET = "pref_snet"
        const val PREF_LOCATION = "pref_location"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_VENDING = "pref_vending"
        const val PREF_WORK_PROFILE = "pref_work_profile"
        const val PREF_ACCOUNTS = "pref_accounts"
        const val PREF_GAMES = "pref_games"
        const val PREF_PASSKEYS = "pref_passkeys"
        const val PREF_GOOGLE_MORE = "pref_google_more"
        const val PREF_SELF_CHECK = "pref_self_check"
        const val PREF_SELF_CHECK_CARD = "pref_self_check_card"
        const val PREF_HIDE_LAUNCHER_ICON = "pref_hide_launcher_icon"

        private const val UI_PREFERENCES = "org.microg.gms_settings_ui"
        private const val KEY_SELF_CHECK_WARNING_DISMISSED = "self_check_warning_dismissed"

        private const val PREFCAT_HEADER = "prefcat_header"
        private const val PREFCAT_FOOTER = "prefcat_footer"
        private val SERVICE_CATEGORIES = listOf(
            "prefcat_google_apps",
            "prefcat_privacy_security",
            "prefcat_autofill_passwords",
            "prefcat_personal_device_safety",
            "prefcat_device_policy",
            "prefcat_other_services"
        )
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
