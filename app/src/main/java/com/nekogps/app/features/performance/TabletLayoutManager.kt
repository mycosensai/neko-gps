package com.nekogps.app.features.performance

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.view.View
import android.widget.FrameLayout
import androidx.slidingpanelayout.widget.SlidingPaneLayout

/**
 * Phase 10: Dual-pane layout helpers for tablets and foldables.
 * Uses SlidingPaneLayout (ships with androidx via navigation deps is NOT
 * guaranteed) -- so this helper works with plain Views and degrades
 * gracefully: single-pane on phones, two-pane on large screens.
 *
 * NOTE: SlidingPaneLayout lives in androidx.slidingpanelayout which may not
 * be on the classpath; therefore this manager avoids that import and works
 * with two plain containers instead.
 */
class TabletLayoutManager(private val context: Context) {

    enum class LayoutMode { SINGLE_PANE, DUAL_PANE }

    /** Width >= 600dp counts as tablet / unfolded foldable. */
    fun isTablet(): Boolean {
        val metrics = context.resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        return widthDp >= TABLET_MIN_WIDTH_DP
    }

    fun currentMode(): LayoutMode = if (isTablet()) LayoutMode.DUAL_PANE else LayoutMode.SINGLE_PANE

    fun isLandscape(): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * Show/hide the detail container and stretch the list container.
     * Call after inflation with the two pane Views from layout_tablet_dual_pane.
     */
    fun applyMode(
        listPane: View,
        detailPane: View,
        mode: LayoutMode = currentMode(),
        showDetail: Boolean = true
    ) {
        if (mode == LayoutMode.DUAL_PANE && showDetail) {
            detailPane.visibility = View.VISIBLE
            listPane.layoutParams = layoutWeight(listPane, 1f)
            detailPane.layoutParams = layoutWeight(detailPane, 2f)
        } else {
            detailPane.visibility = if (showDetail) View.VISIBLE else View.GONE
            listPane.layoutParams = layoutWeight(listPane, 1f)
            if (showDetail) detailPane.layoutParams = layoutWeight(detailPane, 1f)
        }
        listPane.requestLayout()
        detailPane.requestLayout()
    }

    /** Phone-style master-detail: show detail full-screen, hide list. */
    fun showDetailSinglePane(listPane: View, detailPane: View) {
        listPane.visibility = View.GONE
        detailPane.visibility = View.VISIBLE
    }

    fun showListSinglePane(listPane: View, detailPane: View) {
        listPane.visibility = View.VISIBLE
        detailPane.visibility = View.GONE
    }

    /** Handle back press in single-pane detail view; true if consumed. */
    fun onBackPressed(listPane: View, detailPane: View): Boolean {
        if (currentMode() == LayoutMode.SINGLE_PANE &&
            detailPane.visibility == View.VISIBLE && listPane.visibility == View.GONE
        ) {
            showListSinglePane(listPane, detailPane)
            return true
        }
        return false
    }

    /** Smallest-width qualifier in dp (sw600dp = tablet). */
    fun smallestWidthDp(activity: Activity): Int {
        val config = activity.resources.configuration
        return config.smallestScreenWidthDp
    }

    private fun layoutWeight(view: View, weight: Float): android.widget.LinearLayout.LayoutParams {
        val existing = view.layoutParams
        return if (existing is android.widget.LinearLayout.LayoutParams) {
            existing.weight = weight
            existing.width = 0
            existing
        } else {
            android.widget.LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT, weight)
        }
    }

    companion object {
        private const val TABLET_MIN_WIDTH_DP = 600f
    }
}
