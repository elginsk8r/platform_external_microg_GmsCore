/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.gms.R
import com.google.android.material.button.MaterialButton

/**
 * Card in the style of Safety Center issue cards: label, title, summary and a full width action button
 * that is green when [positive] and amber otherwise.
 */
class IssueCardPreference : Preference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    var label: CharSequence? = null
        set(value) {
            field = value
            notifyChanged()
        }

    var buttonText: CharSequence? = null
        set(value) {
            field = value
            notifyChanged()
        }

    var positive: Boolean = false
        set(value) {
            field = value
            notifyChanged()
        }

    var onButtonClick: (() -> Unit)? = null

    /** Shows a dismiss button when set. */
    var onDismiss: (() -> Unit)? = null
        set(value) {
            field = value
            notifyChanged()
        }

    init {
        layoutResource = R.layout.preference_issue_card
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
        // Preference uses the secondary text color for titles of non-selectable preferences, restore primary
        val primaryColor = context.obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary)).use { it.getColorStateList(0) }
        if (primaryColor != null) (holder.findViewById(android.R.id.title) as TextView).setTextColor(primaryColor)
        (holder.findViewById(R.id.issue_card_label) as TextView).text = label
        holder.findViewById(R.id.issue_card_dismiss).apply {
            val dismiss = onDismiss
            visibility = if (dismiss != null) View.VISIBLE else View.GONE
            setOnClickListener { dismiss?.invoke() }
        }
        (holder.findViewById(R.id.issue_card_button) as MaterialButton).apply {
            text = buttonText
            backgroundTintList = ContextCompat.getColorStateList(context, if (positive) R.color.issue_card_button_ok else R.color.issue_card_button_warn)
            setOnClickListener { onButtonClick?.invoke() }
        }
    }
}
