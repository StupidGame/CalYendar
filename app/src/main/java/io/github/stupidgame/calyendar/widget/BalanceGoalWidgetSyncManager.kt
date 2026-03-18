package io.github.stupidgame.calyendar.widget

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BalanceGoalWidgetSyncManager(
    private val context: Context,
    private val database: RoomDatabase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val observer =
        object : InvalidationTracker.Observer("transactions", "financial_goals") {
            override fun onInvalidated(tables: Set<String>) {
                scope.launch {
                    if (BalanceGoalWidgetProvider.hasActiveWidgets(context)) {
                        BalanceGoalWidgetProvider.refreshAll(context)
                    }
                }
            }
        }

    fun start() {
        database.invalidationTracker.addObserver(observer)
        scope.launch {
            if (BalanceGoalWidgetProvider.hasActiveWidgets(context)) {
                BalanceGoalWidgetProvider.refreshAll(context)
            }
        }
    }
}
