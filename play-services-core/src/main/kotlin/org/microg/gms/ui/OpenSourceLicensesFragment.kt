/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.BuildConfig
import java.text.Collator

class OpenSourceLicensesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()
        preferenceScreen = preferenceManager.createPreferenceScreen(context).apply {
            for (library in LIBRARIES.sortedWith(compareBy(Collator.getInstance()) { it.name })) {
                addPreference(Preference(context).apply {
                    title = library.name
                    summary = library.license
                    isSelectable = false
                    isIconSpaceReserved = false
                })
            }
        }
    }

    private data class Library(val name: String, val license: String)

    companion object {
        private val LIBRARIES = buildList {
            when (BuildConfig.FLAVOR_maps.lowercase()) {
                "vtm" -> {
                    add(Library("V™", "GNU LGPLv3, Hannes Janetzek and devemux86"))
                    add(Library("SLF4J", "MIT License, QOS.ch"))
                }
                "mapbox", "maplibre" -> add(Library("MapLibre Native for Android", "Two-Clause BSD, MapLibre contributors"))
                "hms" -> add(Library("Huawei MapKit", "Proprietary, Huawei Technologies"))
            }
            add(Library("Android Jetpack", "Apache License 2.0, The Android Open Source Project"))
            add(Library("CBOR", "Creative Commons Zero, Peter O"))
            add(Library("CircleImageView", "Apache License 2.0, Henning Dodenhof"))
            add(Library("ChromeCast Java API v2", "Apache License 2.0, Vitaly Litvak"))
            add(Library("Conscrypt", "Apache License 2.0, The Android Open Source Project"))
            add(Library("Cronet", "BSD-style License, The Chromium Authors"))
            add(Library("Kotlin", "Apache License 2.0, JetBrains s.r.o."))
            add(Library("Material Components", "Apache License 2.0, The Android Open Source Project"))
            add(Library("Volley", "Apache License 2.0, The Android Open Source Project"))
            add(Library("Wire Protocol Buffers", "Apache License 2.0, Square Inc."))
            add(Library("ZXing", "Apache License 2.0, ZXing authors"))
        }
    }
}
