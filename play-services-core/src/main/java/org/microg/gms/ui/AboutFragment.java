/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.gms.BuildConfig;
import com.google.android.gms.R;

public class AboutFragment extends PreferenceFragmentCompat {
    private static final String PREF_HEADER = "pref_about_header";
    private static final String PREF_VERSION = "pref_about_version";
    private static final String PREF_LICENSES = "pref_about_licenses";

    public static String getSelfVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences_about);
        AppHeadingPreference header = findPreference(PREF_HEADER);
        header.setLayoutResource(R.layout.preference_about_header);
        header.setPackageName(requireContext().getPackageName());
        findPreference(PREF_VERSION).setSummary(getSelfVersion());
        findPreference(PREF_LICENSES).setOnPreferenceClickListener(preference -> {
            UtilsKt.navigate(NavHostFragment.findNavController(this), requireContext(), R.id.openLicenses, null);
            return true;
        });
    }
}
