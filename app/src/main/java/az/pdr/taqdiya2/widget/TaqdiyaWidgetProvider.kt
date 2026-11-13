package az.pdr.taqdiya2.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import az.pdr.taqdiya2.MainActivity
import az.pdr.taqdiya2.R
import az.pdr.taqdiya2.data.local.AppDatabase
import az.pdr.taqdiya2.util.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaqdiyaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context.applicationContext, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = AppWidgetManager.getInstance(context.applicationContext)
                    val ids = manager.getAppWidgetIds(
                        ComponentName(context.applicationContext, TaqdiyaWidgetProvider::class.java)
                    )
                    if (ids.isNotEmpty()) {
                        updateWidgetsInternal(context.applicationContext, manager, ids)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "az.pdr.taqdiya2.action.REFRESH_WIDGET"

        fun requestWidgetUpdate(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val appContext = context.applicationContext
                val manager = AppWidgetManager.getInstance(appContext)
                val ids = manager.getAppWidgetIds(
                    ComponentName(appContext, TaqdiyaWidgetProvider::class.java)
                )
                if (ids.isNotEmpty()) {
                    updateWidgetsInternal(appContext, manager, ids)
                }
            }
        }

        private suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val dao = AppDatabase.getInstance(context).taqdiyaDao()
            val settings = dao.getAppSettingsOnce()
            val showRial = settings?.showRial ?: true
            val maskAmounts = settings?.privacyMaskAmounts ?: false

            val activeRound = dao.getActiveRoundOnce()
            val initialPocket = activeRound?.initialPocketDirhams ?: 200.0
            val roundItems = if (activeRound != null) {
                dao.getItemsForRoundOnce(activeRound.id)
            } else {
                emptyList()
            }

            val allPayments = if (activeRound != null) {
                dao.getAllVendorPaymentsForBackup().filter { it.roundId == activeRound.id }
            } else {
                emptyList()
            }
            val allExpenses = if (activeRound != null) {
                dao.getAllPassingExpensesForBackup().filter { it.roundId == activeRound.id && !it.isDeleted }
            } else {
                emptyList()
            }

            val purchasedItems = roundItems.filter { it.isPurchased && !it.isDeleted }
            val paymentsByVendor = allPayments.associateBy { it.vendorId }
            val involvedVendorIds = (purchasedItems.map { it.vendorId } + paymentsByVendor.keys).toSet()
            val vendorSpent = involvedVendorIds.sumOf { vId ->
                paymentsByVendor[vId]?.actualPaidDirhams
                    ?: purchasedItems.filter { it.vendorId == vId }.sumOf { it.totalDirhams }
            }
            val passingSpent = allExpenses.sumOf { it.amountDirhams }
            val remainingDirhams = (initialPocket - (vendorSpent + passingSpent)).coerceAtLeast(0.0)

            val pocketText = when {
                maskAmounts && showRial -> "*** درهم (*** ريال)"
                maskAmounts -> "*** درهم"
                showRial -> {
                    val dh = CurrencyFormatter.formatAmountPlain(remainingDirhams)
                    val rials = CurrencyFormatter.dirhamsToRials(remainingDirhams)
                    "$dh درهم ($rials ريال)"
                }
                else -> "${CurrencyFormatter.formatAmountPlain(remainingDirhams)} درهم"
            }

            val missingItems = roundItems.filter { !it.isPurchased && !it.isDeleted }
            val itemsSummaryText = if (missingItems.isEmpty()) {
                "النواقص (0): لا توجد أغراض معلقة — اضغط للإضافة"
            } else {
                val previewNames = missingItems.take(5).joinToString(" • ") { it.name }
                val moreCount = (missingItems.size - 5).coerceAtLeast(0)
                val suffix = if (moreCount > 0) " (+$moreCount أخرى)" else ""
                "النواقص (${missingItems.size}): $previewNames$suffix"
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                1001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val refreshIntent = Intent(context, TaqdiyaWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1002,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_taqdiya_home)
                views.setTextViewText(R.id.widget_pocket_value, pocketText)
                views.setTextViewText(R.id.widget_items_summary, itemsSummaryText)
                views.setOnClickPendingIntent(R.id.widget_root_container, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPendingIntent)
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
