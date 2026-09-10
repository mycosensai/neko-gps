package com.nekogps.app.features.performance

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/**
 * Phase 10: Smooth map transitions, polyline animations, marker effects.
 * All helpers are no-ops when given empty input; safe to call from UI thread.
 */
class AnimationManager {

    private val handler = Handler(Looper.getMainLooper())
    private val interpolator = AccelerateDecelerateInterpolator()
    private var routeAnimator: ValueAnimator? = null

    interface MapAnimationListener {
        fun onAnimationStart() {}
        fun onAnimationEnd() {}
        fun onAnimationCancel() {}
    }

    /** Smoothly animate map center + zoom to a target. */
    fun animateMapTo(
        mapView: MapView,
        target: GeoPoint,
        targetZoom: Double = mapView.zoomLevelDouble,
        durationMs: Long = 600,
        listener: MapAnimationListener? = null
    ) {
        val controller = mapView.controller
        val startCenter = mapView.mapCenter as? GeoPoint ?: GeoPoint(target.latitude, target.longitude)
        val startZoom = mapView.zoomLevelDouble
        listener?.onAnimationStart()
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs.coerceIn(
                MIN_ANIM_DURATION_MS,
                MAX_ANIM_DURATION_MS
            )
            interpolator = this@AnimationManager.interpolator
            addUpdateListener { v ->
                val f = v.animatedFraction
                val lat = startCenter.latitude + (target.latitude - startCenter.latitude) * f
                val lon = startCenter.longitude + (target.longitude - startCenter.longitude) * f
                controller.setCenter(GeoPoint(lat, lon))
                controller.setZoom(startZoom + (targetZoom - startZoom) * f)
            }
        }
        animator.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(a: android.animation.Animator) = Unit
            override fun onAnimationEnd(a: android.animation.Animator) { listener?.onAnimationEnd() }
            override fun onAnimationCancel(a: android.animation.Animator) { listener?.onAnimationCancel() }
            override fun onAnimationRepeat(a: android.animation.Animator) = Unit
        })
        animator.start()
    }

    /** Gradually reveal a route polyline point-by-point. */
    fun animatePolyline(
        mapView: MapView,
        line: Polyline,
        fullPath: List<GeoPoint>,
        durationMs: Long = 1500,
        onDone: (() -> Unit)? = null
    ) {
        routeAnimator?.cancel()
        if (fullPath.size < 2) {
            line.setPoints(fullPath)
            mapView.invalidate()
            onDone?.invoke()
            return
        }
        val perPoint = (durationMs / fullPath.size).coerceAtLeast(
            MIN_PER_POINT_MS
        )
        routeAnimator = ValueAnimator.ofInt(2, fullPath.size).apply {
            duration = perPoint * fullPath.size
            interpolator = this@AnimationManager.interpolator
            addUpdateListener { v ->
                val n = (v.animatedValue as Int).coerceIn(2, fullPath.size)
                line.setPoints(fullPath.subList(0, n))
                mapView.invalidate()
            }
        }
        routeAnimator?.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(a: android.animation.Animator) = Unit
            override fun onAnimationEnd(a: android.animation.Animator) { onDone?.invoke() }
            override fun onAnimationCancel(a: android.animation.Animator) = Unit
            override fun onAnimationRepeat(a: android.animation.Animator) = Unit
        })
        routeAnimator?.start()
    }

    fun cancelPolylineAnimation() {
        routeAnimator?.cancel()
        routeAnimator = null
    }

    /** Scale-pop for any backing View (e.g. callout bubble). */
    fun popView(view: View, durationMs: Long = 250) {
        val anim = ScaleAnimation(
            POP_SCALE_FROM,
            POP_SCALE_TO,
            POP_SCALE_FROM,
            POP_SCALE_TO,
            Animation.RELATIVE_TO_SELF,
            POP_PIVOT,
            Animation.RELATIVE_TO_SELF,
            POP_PIVOT
        ).apply {
            duration = durationMs
            interpolator = this@AnimationManager.interpolator
        }
        view.startAnimation(anim)
    }

    fun fadeIn(view: View, durationMs: Long = 300) {
        val anim = AlphaAnimation(0f, 1f).apply { duration = durationMs }
        view.visibility = View.VISIBLE
        view.startAnimation(anim)
    }

    fun fadeOut(view: View, durationMs: Long = 300, hideOnEnd: Boolean = true) {
        val anim = AlphaAnimation(1f, 0f).apply { duration = durationMs }
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(a: Animation?) = Unit
            override fun onAnimationRepeat(a: Animation?) = Unit
            override fun onAnimationEnd(a: Animation?) {
                if (hideOnEnd) view.visibility = View.GONE
            }
        })
        view.startAnimation(anim)
    }

    fun pulseView(view: View, repeat: Int = 3) {
        val anim = AlphaAnimation(
            PULSE_ALPHA_FROM,
            PULSE_ALPHA_TO
        ).apply {
            duration = PULSE_DURATION_MS
            repeatCount = repeat
            repeatMode = Animation.REVERSE
        }
        view.startAnimation(anim)
    }

    fun clearAll(view: View) {
        routeAnimator?.cancel()
        routeAnimator = null
        view.clearAnimation()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val MIN_ANIM_DURATION_MS = 100L
        private const val MAX_ANIM_DURATION_MS = 3000L
        private const val MIN_PER_POINT_MS = 16L
        private const val POP_SCALE_FROM = 0.6f
        private const val POP_SCALE_TO = 1.0f
        private const val POP_PIVOT = 0.5f
        private const val PULSE_ALPHA_FROM = 1f
        private const val PULSE_ALPHA_TO = 0.4f
        private const val PULSE_DURATION_MS = 400L
    }
}
