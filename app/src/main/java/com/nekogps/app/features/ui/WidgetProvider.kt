package com.nekogps.app.features.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nekogps.app.MainActivity
import java.util.Locale

/**
 * Home-screen widget showing current location, distance to next waypoint and ETA.
 * Navigation state is pushed via [pushNavState]; the widget reads the latest
 * snapshot from SharedPreferences so updates work even when the app is dead.
 */
class WidgetProvider : AppWidgetProvider() {

    companion object {
        private const val PREFS = "nav_widget_prefs"
        private const val KEY_LOCATION = "location"
        private const val KEY_DISTANCE = "distance"
        private const val KEY_ETA = "eta"
        private const val KEY_NEXT = "next_turn"

        const val ACTION_REFRESH = "com.nekogps.app.features.ui.WIDGET_REFRESH"
        private const val MINUTES_PER_HOUR = 60

        fun pushNavState(
            context: Context,
            locationLabel: String,
            distanceToNext: String,
            eta: String,
            nextTurn: String
        ) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_LOCATION, locationLabel)
                .putString(KEY_DISTANCE, distanceToNext)
                .putString(KEY_ETA, eta)
                .putString(KEY_NEXT, nextTurn)
                .apply()
            refreshAll(context)
        }

        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, WidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        fun formatEta(etaMinutes: Long): String {
            if (etaMinutes <= 0) return "Arrived"
            val h = etaMinutes / MINUTES_PER_HOUR
            val m = etaMinutes % MINUTES_PER_HOUR
            return if (h > 0) String.format(Locale.US, "%dh %02dm", h, m)
            else String.format(Locale.US, "%d min", m)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, com.nekogps.app.R.layout.widget_nav).apply {
                setTextViewText(
                    com.nekogps.app.R.id.tv_widget_location,
                    prefs.getString(KEY_LOCATION, context.getString(com.nekogps.app.R.string.widget_no_fix)) ?: ""
                )
                setTextViewText(
                    com.nekogps.app.R.id.tv_widget_distance,
                    prefs.getString(KEY_DISTANCE, "--") ?: "--"
                )
                setTextViewText(
                    com.nekogps.app.R.id.tv_widget_eta,
                    prefs.getString(KEY_ETA, "--") ?: "--"
                )
                setTextViewText(
                    com.nekogps.app.R.id.tv_widget_next,
                    prefs.getString(KEY_NEXT, context.getString(com.nekogps.app.R.string.widget_no_route)) ?: ""
                )
                val openIntent = PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(com.nekogps.app.R.id.widget_root, openIntent)
            }
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshAll(context)
    }
}
