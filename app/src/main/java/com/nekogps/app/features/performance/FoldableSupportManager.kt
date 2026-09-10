package com.nekogps.app.features.performance

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.View

/**
 * Phase 10: Handle fold/unfold events and adapt UI.
 * Uses only framework APIs (no androidx.window dependency) so it compiles
 * against the current dependency set: screen-size / orientation / hinge-gap
 * heuristics drive posture detection.
 */
class FoldableSupportManager {

    enum class Posture { FLAT_CLOSED, HALF_FOLDED, FLAT_OPEN, DUAL_SCREEN }

    interface FoldableListener {
        fun onPostureChanged(posture: Posture) {}
        fun onSpanningChanged(isSpanning: Boolean) {}
    }

    var listener: FoldableListener? = null

    private var lastPosture: Posture? = null
    private var lastSpanning = false

    /** Call from Activity.onConfigurationChanged(). */
    fun onConfigurationChanged(activity: Activity, newConfig: Configuration) {
        val posture = detectPosture(activity)
        if (posture != lastPosture) {
            lastPosture = posture
            listener?.onPostureChanged(posture)
        }
        val spanning = isSpanning(activity)
        if (spanning != lastSpanning) {
            lastSpanning = spanning
            listener?.onSpanningChanged(spanning)
        }
        Log.d(TAG, "config changed: posture=" + posture + " spanning=" + spanning)
    }

    /** Call from onResume() to emit the initial state. */
    fun refresh(activity: Activity): Posture {
        val posture = detectPosture(activity)
        lastPosture = posture
        lastSpanning = isSpanning(activity)
        return posture
    }

    fun currentPosture(activity: Activity): Posture = detectPosture(activity)

    /**
     * Adapt two panes to the current posture:
     * - DUAL_SCREEN: split side by side, avoid hinge via weight split
     * - HALF_FOLDED: stack vertically (tabletop mode), detail on top half
     * - else: default tablet/phone behavior
     */
    fun adaptLayout(activity: Activity, listPane: View, detailPane: View, tablet: TabletLayoutManager) {
        when (detectPosture(activity)) {
            Posture.DUAL_SCREEN -> {
                listPane.visibility = View.VISIBLE
                detailPane.visibility = View.VISIBLE
                tablet.applyMode(listPane, detailPane, TabletLayoutManager.LayoutMode.DUAL_PANE, true)
            }
            Posture.HALF_FOLDED -> {
                // Tabletop: keep both visible; parent LinearLayout orientation
                // can be flipped to vertical by the Activity if desired.
                listPane.visibility = View.VISIBLE
                detailPane.visibility = View.VISIBLE
            }
            Posture.FLAT_OPEN -> {
                tablet.applyMode(listPane, detailPane, TabletLayoutManager.LayoutMode.DUAL_PANE, true)
            }
            Posture.FLAT_CLOSED -> {
                tablet.applyMode(listPane, detailPane, TabletLayoutManager.LayoutMode.SINGLE_PANE, false)
            }
        }
    }

    /** Heuristic posture detection without androidx.window. */
    private fun detectPosture(activity: Activity): Posture {
        val config = activity.resources.configuration
        val metrics = activity.resources.displayMetrics
        val hinge = hingeBounds(activity)

        // Dual-screen devices report a hinge / fold gap via display cutout-ish APIs
        // on newer OEMs; fall back to extreme aspect ratios.
        if (hinge != null && !hinge.isEmpty) return Posture.DUAL_SCREEN

        val wDp = metrics.widthPixels / metrics.density
        val hDp = metrics.heightPixels / metrics.density
        val ratio = if (hDp > 0) wDp / hDp else 1f

        // Half-folded (flex/tabletop) keeps a squarish window with landscape hinge;
        // best framework signal pre-WindowManager is orientation + small height.
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE && hDp < 500) {
            return Posture.HALF_FOLDED
        }
        if (wDp >= 840 || (ratio > 2.2f)) return Posture.DUAL_SCREEN
        if (wDp >= 600) return Posture.FLAT_OPEN
        return Posture.FLAT_CLOSED
    }

    private fun isSpanning(activity: Activity): Boolean {
        val p = detectPosture(activity)
        return p == Posture.DUAL_SCREEN || p == Posture.FLAT_OPEN
    }

    /** Best-effort hinge bounds; null on non-foldables. */
    private fun hingeBounds(activity: Activity): Rect? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val cutout = activity.window?.decorView?.rootWindowInsets?.displayCutout
                cutout?.boundingRects?.firstOrNull()
            } else null
        } catch (e: Exception) {
            Log.w("FoldableSupportManager", "hingeBounds: suppressed Exception", e)
            null }
    }

    companion object {
        private const val TAG = "FoldableSupport"
    }
}
