/*
 * SPDX-FileCopyrightText: 2025 The Android Open Source Project
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import android.view.ViewOutlineProvider
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

/**
 * Draws consecutive preferences of a group as rounded cards, like the expressive Settings theme.
 *
 * Grouping follows SettingsPreferenceGroupAdapter of AOSP SettingsLib.
 */
class ExpressiveGroupDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val density = context.resources.displayMetrics.density
    private val outerRadius = 20 * density
    private val innerRadius = 4 * density
    private val sideMargin = (16 * density).toInt()
    private val gap = (2 * density).toInt()
    private val groupSpacing = (16 * density).toInt()
    private val categoryOffset = (8 * density).toInt()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceBright, 0)
    }
    private val path = Path()
    private val rect = RectF()

    private fun isGroupDivider(preference: Preference) = preference is PreferenceCategory ||
            preference is FooterPreference || preference is TopIntroPreference || preference is SwitchBarPreference ||
            preference is WarningCardPreference || preference is AppHeadingPreference ||
            (!preference.isSelectable && preference.title.isNullOrEmpty())

    private fun buildPositionStates(adapter: PreferenceGroupAdapter): IntArray {
        val states = IntArray(adapter.itemCount)
        var previous = -1
        var previousParent: Preference? = null
        fun close() {
            if (previous >= 0) states[previous] = if (states[previous] == STATE_FIRST) STATE_SINGLE else STATE_LAST
        }
        for (i in 0 until adapter.itemCount) {
            val preference = adapter.getItem(i) ?: continue
            if (isGroupDivider(preference)) {
                close()
                previous = -1
                continue
            }
            if (previous < 0 || preference.parent != previousParent) {
                close()
                states[i] = STATE_FIRST
            } else {
                states[i] = STATE_MIDDLE
            }
            previous = i
            previousParent = preference.parent
        }
        close()
        return states
    }

    private fun positionState(parent: RecyclerView, view: View): Int {
        val adapter = parent.adapter as? PreferenceGroupAdapter ?: return STATE_NONE
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return STATE_NONE
        return buildPositionStates(adapter).getOrElse(position) { STATE_NONE }
    }

    private fun radii(state: Int): FloatArray {
        val top = if (state == STATE_FIRST || state == STATE_SINGLE) outerRadius else innerRadius
        val bottom = if (state == STATE_LAST || state == STATE_SINGLE) outerRadius else innerRadius
        return floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter as? PreferenceGroupAdapter
        val position = parent.getChildAdapterPosition(view)
        val preference = if (adapter != null && position != RecyclerView.NO_POSITION) adapter.getItem(position) else null
        val positionState = positionState(parent, view)
        if (positionState == STATE_NONE) {
            view.clipToOutline = false
            view.outlineProvider = ViewOutlineProvider.BACKGROUND
            if (preference is PreferenceCategory && !preference.title.isNullOrEmpty()) {
                outRect.left = if (preference.isIconSpaceReserved) sideMargin else categoryOffset
            }
            return
        }
        outRect.left = sideMargin
        outRect.right = sideMargin
        if (positionState == STATE_MIDDLE || positionState == STATE_LAST) {
            outRect.top = gap
        } else if (position > 0) {
            // Separate from a previous group unless there is a category title in between
            val previous = adapter?.getItem(position - 1)
            if (previous !is PreferenceCategory || previous.title.isNullOrEmpty()) outRect.top = groupSpacing
        }
        val itemRadii = radii(positionState)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val outlinePath = Path().apply {
                    addRoundRect(RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()), itemRadii, Path.Direction.CW)
                }
                if (android.os.Build.VERSION.SDK_INT >= 30) outline.setPath(outlinePath) else outline.setConvexPath(outlinePath)
            }
        }
        view.clipToOutline = true
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val positionState = positionState(parent, child)
            if (positionState == STATE_NONE) continue
            rect.set(child.left.toFloat(), child.top + child.translationY, child.right.toFloat(), child.bottom + child.translationY)
            path.reset()
            path.addRoundRect(rect, radii(positionState), Path.Direction.CW)
            paint.alpha = (child.alpha * 255).toInt()
            c.drawPath(path, paint)
        }
    }

    companion object {
        private const val STATE_NONE = 0
        private const val STATE_FIRST = 1
        private const val STATE_MIDDLE = 2
        private const val STATE_LAST = 3
        private const val STATE_SINGLE = 4
    }
}
