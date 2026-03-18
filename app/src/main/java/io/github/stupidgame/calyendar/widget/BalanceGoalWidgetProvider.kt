package io.github.stupidgame.calyendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import io.github.stupidgame.calyendar.CalYendarApplication
import io.github.stupidgame.calyendar.MainActivity
import io.github.stupidgame.calyendar.R
import io.github.stupidgame.calyendar.data.GoalComparisonSnapshot
import io.github.stupidgame.calyendar.data.loadGoalComparisonSnapshot
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BalanceGoalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        enqueueUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action in refreshActions) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds =
                appWidgetManager.getAppWidgetIds(
                    ComponentName(context, BalanceGoalWidgetProvider::class.java)
                )
            enqueueUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun enqueueUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "io.github.stupidgame.calyendar.action.REFRESH_BALANCE_GOAL_WIDGET"

        private const val ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED"
        private const val ACTION_TIME_SET = "android.intent.action.TIME_SET"
        private const val ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED"

        private val refreshActions =
            setOf(
                ACTION_REFRESH,
                ACTION_DATE_CHANGED,
                ACTION_TIME_SET,
                ACTION_TIMEZONE_CHANGED
            )

        suspend fun refreshAll(context: Context) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val appWidgetIds =
                appWidgetManager.getAppWidgetIds(
                    ComponentName(appContext, BalanceGoalWidgetProvider::class.java)
                )

            updateWidgets(appContext, appWidgetManager, appWidgetIds)
        }

        fun hasActiveWidgets(context: Context): Boolean {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val appWidgetIds =
                appWidgetManager.getAppWidgetIds(
                    ComponentName(appContext, BalanceGoalWidgetProvider::class.java)
                )

            return appWidgetIds.isNotEmpty()
        }

        suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) {
                return
            }

            val appContext = context.applicationContext
            val repository = (appContext as CalYendarApplication).repository
            val snapshot = repository.loadGoalComparisonSnapshot(LocalDate.now())
            val remoteViews = buildRemoteViews(appContext, snapshot)
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
        }

        private fun buildRemoteViews(
            context: Context,
            snapshot: GoalComparisonSnapshot
        ): RemoteViews {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_balance_goal)

            remoteViews.setTextViewText(
                R.id.widget_balance_amount,
                context.getString(R.string.widget_yen_amount, snapshot.comparisonBalance)
            )
            remoteViews.setTextColor(
                R.id.widget_balance_amount,
                if (snapshot.comparisonBalance >= 0) {
                    Color.parseColor("#F7FFF9")
                } else {
                    Color.parseColor("#FFD7D7")
                }
            )

            val nextGoal = snapshot.nextGoal
            if (nextGoal != null) {
                remoteViews.setTextViewText(
                    R.id.widget_goal_name,
                    context.getString(
                        R.string.widget_goal_name_with_date,
                        nextGoal.month + 1,
                        nextGoal.day,
                        nextGoal.name
                    )
                )
                remoteViews.setTextViewText(
                    R.id.widget_goal_amount,
                    context.getString(R.string.widget_yen_amount, nextGoal.amount)
                )
                remoteViews.setViewVisibility(R.id.widget_goal_amount, View.VISIBLE)
            } else {
                remoteViews.setTextViewText(
                    R.id.widget_goal_name,
                    context.getString(R.string.widget_no_goal)
                )
                remoteViews.setTextViewText(
                    R.id.widget_goal_amount,
                    context.getString(R.string.widget_no_goal_amount)
                )
                remoteViews.setViewVisibility(R.id.widget_goal_amount, View.VISIBLE)
            }

            val launchIntent =
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            val pendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            remoteViews.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            return remoteViews
        }
    }
}
