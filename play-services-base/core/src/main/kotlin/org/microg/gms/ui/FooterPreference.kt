/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import org.microg.gms.base.core.R

class FooterPreference : Preference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context) : super(context)

    private var learnMoreText: CharSequence? = null
    private var learnMoreListener: View.OnClickListener? = null

    init {
        layoutResource = R.layout.preference_footer
        if (icon == null) setIcon(R.drawable.ic_info_outline)
        order = ORDER_FOOTER
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val learnMore = holder.findViewById(R.id.learn_more) as? TextView ?: return
        val listener = learnMoreListener
        if (listener == null) {
            learnMore.visibility = View.GONE
            return
        }
        val text = SpannableString(learnMoreText ?: context.getText(R.string.pref_footer_learn_more))
        text.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) = listener.onClick(widget)
        }, 0, text.length, 0)
        learnMore.text = text
        learnMore.movementMethod = LinkMovementMethod.getInstance()
        learnMore.visibility = View.VISIBLE
    }

    fun setLearnMoreText(text: CharSequence?) {
        if (learnMoreText == text) return
        learnMoreText = text
        notifyChanged()
    }

    fun setLearnMoreAction(listener: View.OnClickListener?) {
        if (learnMoreListener == listener) return
        learnMoreListener = listener
        notifyChanged()
    }

    companion object {
        private const val ORDER_FOOTER = Int.MAX_VALUE - 1
    }
}
