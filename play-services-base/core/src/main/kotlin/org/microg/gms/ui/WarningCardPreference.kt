/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.microg.gms.base.core.R

/**
 * Warning shown as a colored card, like the banner message preference of the expressive Settings theme.
 */
class WarningCardPreference : Preference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }
    constructor(context: Context) : super(context) {
        init(null)
    }

    /** Uses the red high attention colors instead of the yellow medium attention colors. */
    var highAttention: Boolean = false
        set(value) {
            field = value
            notifyChanged()
        }

    private fun init(attrs: AttributeSet?) {
        layoutResource = R.layout.preference_warning_card
        // Not selectable unless requested, for warnings with an action to resolve them
        isSelectable = attrs?.getAttributeBooleanValue(ANDROID_NAMESPACE, "selectable", false) ?: false
        if (icon == null) setIcon(R.drawable.ic_warning_card)
        val a = context.obtainStyledAttributes(attrs, R.styleable.WarningCardPreference)
        highAttention = a.getInt(R.styleable.WarningCardPreference_attentionLevel, 1) == 0
        a.recycle()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
        val background = if (highAttention) R.color.warning_card_background_high else R.color.warning_card_background_medium
        val accent = if (highAttention) R.color.warning_card_accent_high else R.color.warning_card_accent_medium
        holder.findViewById(R.id.warning_card).apply {
            backgroundTintList = ContextCompat.getColorStateList(context, background)
            clipToOutline = true
        }
        (holder.findViewById(android.R.id.icon) as ImageView).imageTintList = ContextCompat.getColorStateList(context, accent)
        // Preference uses the secondary text color for titles of non-selectable preferences, restore primary
        val primaryColor = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary)).let {
            val color = it.getColorStateList(0)
            it.recycle()
            color
        }
        if (primaryColor != null) (holder.findViewById(android.R.id.title) as TextView).setTextColor(primaryColor)
    }

    companion object {
        private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
