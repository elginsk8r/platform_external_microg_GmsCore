/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.nearby.exposurenotification.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient

/**
 * Opens the exposure notification settings in microG Settings.
 */
class ExposureNotificationsSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(ExposureNotificationClient.ACTION_EXPOSURE_NOTIFICATION_SETTINGS)
            .setClassName(packageName, "org.microg.gms.ui.MainSettingsActivity")
            .setData(Uri.parse("x-gms-settings://exposure-notifications")))
        finish()
    }
}
