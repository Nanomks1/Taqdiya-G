package az.pdr.taqdiya2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.pdr.taqdiya2.data.local.AppDatabase
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.repository.TaqdiyaRepository
import az.pdr.taqdiya2.ui.components.AllItemsPriceTrackerDialog
import az.pdr.taqdiya2.ui.components.AppSettingsSheet
import az.pdr.taqdiya2.ui.components.BackupRestoreDialog
import az.pdr.taqdiya2.ui.components.DictionaryDialog
import az.pdr.taqdiya2.ui.components.EditShoppingItemDialog
import az.pdr.taqdiya2.ui.components.GlobalTactileSnackbarHost
import az.pdr.taqdiya2.ui.components.ItemPriceHistorySheet
import az.pdr.taqdiya2.ui.components.PassingExpenseSheet
import az.pdr.taqdiya2.ui.components.PinLockScreen
import az.pdr.taqdiya2.ui.components.PocketBudgetDialog
import az.pdr.taqdiya2.ui.components.QuickAddPadSheet
import az.pdr.taqdiya2.ui.components.ShareListDialog
import az.pdr.taqdiya2.ui.components.TopPocketBar
import az.pdr.taqdiya2.ui.components.TrashBinDialog
import az.pdr.taqdiya2.ui.components.VendorManagerDialog
import az.pdr.taqdiya2.ui.screens.AtVendorScreen
import az.pdr.taqdiya2.ui.screens.HomePrepScreen
import az.pdr.taqdiya2.ui.screens.PocketAuditScreen
import az.pdr.taqdiya2.ui.theme.TaqdiyaTheme
import az.pdr.taqdiya2.ui.viewmodel.HomePrepViewModel
import az.pdr.taqdiya2.widget.TaqdiyaWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val bootPrefs = getSharedPreferences("taqdiya_boot_prefs", MODE_PRIVATE)
        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val defaultTheme = if (isSystemDark) "DARK" else "LIGHT"
        val cachedThemeMode = bootPrefs.getString("themeMode", defaultTheme) ?: defaultTheme
        val cachedUiScaleMode = bootPrefs.getString("uiScaleMode", "NORMAL") ?: "NORMAL"
        val cachedShowRial = bootPrefs.getBoolean("showRial", true)
        val cachedPrivacyMask = bootPrefs.getBoolean("privacyMaskAmounts", false)
        val cachedPrivacyPocketOnly = bootPrefs.getBoolean("privacyMaskPocketOnly", false)

        val cachedColorPalette = bootPrefs.getString("colorPalette", "TERRACOTTA") ?: "TERRACOTTA"
        val cachedAnimationMode = bootPrefs.getString("animationMode", "LIGHT") ?: "LIGHT"
        val cachedFontFamilyKey = bootPrefs.getString("fontFamilyKey", "CAIRO") ?: "CAIRO"
        val cachedFontReg = bootPrefs.getString("customFontRegularName", "") ?: ""
        val cachedFontMed = bootPrefs.getString("customFontMediumName", "") ?: ""
        val cachedFontBold = bootPrefs.getString("customFontBoldName", "") ?: ""

        val initialDark = when (cachedThemeMode) {
            "DARK" -> true
            "SYSTEM" -> isSystemDark
            else -> false
        }
        val initialScheme = az.pdr.taqdiya2.ui.theme.resolveColorScheme(cachedColorPalette, initialDark)
        window.decorView.setBackgroundColor(
            android.graphics.Color.argb(
                255,
                (initialScheme.background.red * 255).toInt(),
                (initialScheme.background.green * 255).toInt(),
                (initialScheme.background.blue * 255).toInt()
            )
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialSettings = az.pdr.taqdiya2.data.local.AppSettingsEntity(
            id = 1,
            themeMode = cachedThemeMode,
            showRial = cachedShowRial,
            uiScaleMode = cachedUiScaleMode,
            privacyMaskAmounts = cachedPrivacyMask,
            privacyMaskPocketOnly = cachedPrivacyPocketOnly,
            colorPalette = cachedColorPalette,
            animationMode = cachedAnimationMode,
            fontFamilyKey = cachedFontFamilyKey,
            customFontRegularName = cachedFontReg,
            customFontMediumName = cachedFontMed,
            customFontBoldName = cachedFontBold
        )
        az.pdr.taqdiya2.util.CurrencyFormatter.showRialState = cachedShowRial
        az.pdr.taqdiya2.util.CurrencyFormatter.privacyMaskState = cachedPrivacyMask
        az.pdr.taqdiya2.util.CurrencyFormatter.privacyMaskPocketOnlyState = cachedPrivacyPocketOnly

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TaqdiyaRepository(database.taqdiyaDao())

        setContent {
            val viewModel: HomePrepViewModel = viewModel(
                factory = HomePrepViewModel.Factory(repository, initialSettings)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isLoading, uiState.appSettings) {
                if (!uiState.isLoading) {
                    bootPrefs.edit()
                        .putString("themeMode", uiState.appSettings.themeMode)
                        .putString("uiScaleMode", uiState.appSettings.uiScaleMode)
                        .putBoolean("showRial", uiState.appSettings.showRial)
                        .putBoolean("privacyMaskAmounts", uiState.appSettings.privacyMaskAmounts)
                        .putBoolean("privacyMaskPocketOnly", uiState.appSettings.privacyMaskPocketOnly)
                        .putString("colorPalette", uiState.appSettings.colorPalette)
                        .putString("animationMode", uiState.appSettings.animationMode)
                        .putString("fontFamilyKey", uiState.appSettings.fontFamilyKey)
                        .putString("customFontRegularName", uiState.appSettings.customFontRegularName)
                        .putString("customFontMediumName", uiState.appSettings.customFontMediumName)
                        .putString("customFontBoldName", uiState.appSettings.customFontBoldName)
                        .apply()
                }
            }

            val customFontSignature = "${uiState.appSettings.customFontRegularName}|${uiState.appSettings.customFontMediumName}|${uiState.appSettings.customFontBoldName}"

            TaqdiyaTheme(
                themeMode = uiState.appSettings.themeMode,
                uiScaleMode = uiState.appSettings.uiScaleMode,
                colorPalette = uiState.appSettings.colorPalette,
                fontFamilyKey = uiState.appSettings.fontFamilyKey,
                customFontSignature = customFontSignature
            ) {
                TaqdiyaApp(
                    viewModel = viewModel,
                    onExitApp = { finish() }
                )
            }
        }
    }
}

@Composable
fun TaqdiyaApp(
    viewModel: HomePrepViewModel,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showQuickAddPad by rememberSaveable { mutableStateOf(false) }
    var showPassingExpenseSheet by rememberSaveable { mutableStateOf(false) }
    var showToolsMenuDialog by rememberSaveable { mutableStateOf(false) }
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var showDictionaryDialog by rememberSaveable { mutableStateOf(false) }
    var showPriceTrackerDialog by rememberSaveable { mutableStateOf(false) }
    var showVendorManagerDialog by rememberSaveable { mutableStateOf(false) }
    var showShareDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showTrashBinDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupRestoreDialog by rememberSaveable { mutableStateOf(false) }
    var editingShoppingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }
    var priceHistoryTarget by remember { mutableStateOf<Triple<String, Long, Double>?>(null) }

    // Parent menu stack tracking so pressing Back or Close inside a nested tool/setting returns to its parent menu
    var returnMenuAfterSubDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var didOpenSettingsFromTools by rememberSaveable { mutableStateOf(false) }

    fun closeSubDialogAndRestoreParent(closeAction: () -> Unit) {
        closeAction()
        when (returnMenuAfterSubDialog) {
            "SETTINGS" -> {
                returnMenuAfterSubDialog = null
                showSettingsSheet = true
            }
            "TOOLS" -> {
                returnMenuAfterSubDialog = null
                showToolsMenuDialog = true
            }
        }
    }

    fun closeSettingsAndRestoreParent() {
        showSettingsSheet = false
        if (didOpenSettingsFromTools) {
            didOpenSettingsFromTools = false
            showToolsMenuDialog = true
        }
    }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Keep Home Screen AppWidget automatically synchronized with live pocket balance, missing items, and Day/Night theme
    LaunchedEffect(
        uiState.remainingPocketDirhams,
        uiState.shoppingItems.size,
        uiState.appSettings.showRial,
        uiState.appSettings.privacyMaskAmounts,
        uiState.appSettings.privacyMaskPocketOnly,
        uiState.appSettings.themeMode
    ) {
        if (!uiState.isLoading) {
            TaqdiyaWidgetProvider.requestWidgetUpdate(context)
        }
    }

    // Listen to Global Undo Events (Deletions & Critical Actions)
    LaunchedEffect(viewModel) {
        var activeSnackbarJob: Job? = null
        viewModel.undoEvents.collect { undoAction ->
            activeSnackbarJob?.cancel()
            snackbarHostState.currentSnackbarData?.dismiss()
            activeSnackbarJob = launch {
                val result = snackbarHostState.showSnackbar(
                    message = undoAction.message,
                    actionLabel = "تراجع الآن",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.performUndo(undoAction)
                } else if (result == SnackbarResult.Dismissed) {
                    viewModel.dismissActiveUndo()
                }
            }
        }
    }

    // Listen to Status Messages (Confirmations & Undo Restore Notices)
    LaunchedEffect(viewModel) {
        viewModel.statusMessages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Double-Back to Exit & Hierarchical Sub-Menu Back Navigation
    BackHandler {
        when {
            uiState.isPinLocked -> onExitApp()
            priceHistoryTarget != null -> priceHistoryTarget = null
            editingShoppingItem != null -> editingShoppingItem = null
            showBackupRestoreDialog -> closeSubDialogAndRestoreParent { showBackupRestoreDialog = false }
            showTrashBinDialog -> closeSubDialogAndRestoreParent { showTrashBinDialog = false }
            showPassingExpenseSheet -> closeSubDialogAndRestoreParent { showPassingExpenseSheet = false }
            showPriceTrackerDialog -> closeSubDialogAndRestoreParent { showPriceTrackerDialog = false }
            showDictionaryDialog -> closeSubDialogAndRestoreParent { showDictionaryDialog = false }
            showVendorManagerDialog -> closeSubDialogAndRestoreParent { showVendorManagerDialog = false }
            showShareDialog -> closeSubDialogAndRestoreParent { showShareDialog = false }
            showSettingsSheet -> closeSettingsAndRestoreParent()
            showQuickAddPad -> showQuickAddPad = false
            showBudgetDialog -> showBudgetDialog = false
            showToolsMenuDialog -> showToolsMenuDialog = false
            selectedTab != 0 -> selectedTab = 0
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    onExitApp()
                } else {
                    lastBackPressTime = now
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = "اضغط رجوع مرة أخرى للخروج من التطبيق",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    if (uiState.isPinLocked) {
        PinLockScreen(
            onTryUnlockPin = viewModel::unlockWithPin
        )
        return
    }

    val activeMissingItems = remember(uiState.shoppingItems) {
        uiState.shoppingItems.filter { !it.isDeleted && !it.isPurchased }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopPocketBar(
                remainingPocketDirhams = uiState.remainingPocketDirhams,
                initialPocketDirhams = uiState.initialPocketDirhams,
                totalMissingItemsCount = activeMissingItems.size,
                privacyMaskAmounts = uiState.appSettings.privacyMaskAmounts,
                showRial = uiState.appSettings.showRial,
                onTogglePrivacyMask = viewModel::togglePrivacyMask,
                onOpenSettingsClick = {
                    didOpenSettingsFromTools = false
                    showSettingsSheet = true
                },
                onEditBudgetClick = { showBudgetDialog = true },
                onOpenToolsSheetClick = { showToolsMenuDialog = true },
                onShareListClick = {
                    returnMenuAfterSubDialog = null
                    showShareDialog = true
                }
            )
        },
        snackbarHost = {
            GlobalTactileSnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "التحضير",
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "التحضير",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_home_prep")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeMissingItems.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ) {
                                        Text(activeMissingItems.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Scale else Icons.Outlined.Scale,
                                contentDescription = "عند البائع",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "عند البائع",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_at_vendor")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.FactCheck else Icons.Outlined.FactCheck,
                            contentDescription = "جرد الجيب",
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "جرد الجيب",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_pocket_audit")
                )
            }
        }
    ) { innerPadding ->
        val animMode = uiState.appSettings.animationMode
        val renderTabContent: @Composable (Int) -> Unit = { tabIndex ->
            when (tabIndex) {
                0 -> {
                    HomePrepScreen(
                        uiState = uiState,
                        onOpenQuickPadForVendor = { vendorId ->
                            viewModel.clearLastTouchedItem()
                            if (vendorId != null) {
                                viewModel.onVendorSelectedForInput(vendorId)
                            }
                            showQuickAddPad = true
                        },
                        onQuickAddDictionaryItem = viewModel::quickAddFromDictionary,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onFilterVendorChanged = viewModel::onFilterVendorChanged,
                        onEditItemClick = { editingShoppingItem = it },
                        onDeleteItemClick = viewModel::deleteShoppingItem,
                        onOpenDictionaryClick = {
                            returnMenuAfterSubDialog = null
                            showDictionaryDialog = true
                        },
                        onOpenPriceTrackerClick = {
                            returnMenuAfterSubDialog = null
                            showPriceTrackerDialog = true
                        },
                        onOpenVendorsClick = {
                            returnMenuAfterSubDialog = null
                            showVendorManagerDialog = true
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                1 -> {
                    AtVendorScreen(
                        uiState = uiState,
                        onSelectMarketVendor = viewModel::onSelectMarketVendor,
                        onToggleKeepScreenAwake = viewModel::toggleKeepScreenAwake,
                        onConfirmItemPurchase = viewModel::confirmItemPurchase,
                        onUnmarkItemPurchased = viewModel::unmarkItemPurchased,
                        onDeleteItemClick = viewModel::deleteShoppingItem,
                        onConfirmVendorPayment = viewModel::confirmVendorPayment,
                        onResetVendorPaymentAndBasket = viewModel::resetVendorPaymentAndBasket,
                        onUpdateVendorPendingBalance = viewModel::updateVendorPendingBalance,
                        onOpenPassingExpenseClick = {
                            returnMenuAfterSubDialog = null
                            showPassingExpenseSheet = true
                        },
                        onOpenItemPriceHistory = { itemName, vendorId, fallbackDh ->
                            priceHistoryTarget = Triple(itemName, vendorId, fallbackDh)
                        },
                        onOpenQuickPadForVendor = { vendorId ->
                            viewModel.clearLastTouchedItem()
                            if (vendorId != null) {
                                viewModel.onVendorSelectedForInput(vendorId)
                            }
                            showQuickAddPad = true
                        },
                        onReturnToHomePrep = { selectedTab = 0 },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                2 -> {
                    PocketAuditScreen(
                        uiState = uiState,
                        onOpenPassingExpenseClick = {
                            returnMenuAfterSubDialog = null
                            showPassingExpenseSheet = true
                        },
                        onDeletePassingExpense = viewModel::deletePassingExpense,
                        onConfirmVendorPayment = viewModel::confirmVendorPayment,
                        onResetVendorPaymentAndBasket = viewModel::resetVendorPaymentAndBasket,
                        onUpdateVendorPendingBalance = viewModel::updateVendorPendingBalance,
                        onFinishAndArchiveRound = viewModel::finishAndArchiveCurrentRound,
                        onLoadArchivedRoundItems = viewModel::loadArchivedRoundItems,
                        onDeleteArchivedRound = viewModel::deleteArchivedRound,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        when (animMode) {
            "OFF" -> renderTabContent(selectedTab)
            "FULL" -> {
                androidx.compose.animation.AnimatedContent(
                    targetState = selectedTab,
                    label = "full_tab_transition"
                ) { targetTab ->
                    renderTabContent(targetTab)
                }
            }
            else -> {
                androidx.compose.animation.Crossfade(
                    targetState = selectedTab,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 150),
                    label = "light_tab_crossfade"
                ) { targetTab ->
                    renderTabContent(targetTab)
                }
            }
        }
    }

    if (showQuickAddPad) {
        QuickAddPadSheet(
            vendors = uiState.vendors,
            dictionaryItems = uiState.dictionaryItems,
            shoppingItems = uiState.shoppingItems,
            selectedVendorId = uiState.selectedVendorIdForInput,
            lastTouchedItemId = uiState.lastTouchedItemId,
            itemNameInput = uiState.itemNameInput,
            itemNoteInput = uiState.itemNoteInput,
            autoMatchedVendorName = uiState.autoMatchedVendorName,
            activeUndo = uiState.activeUndo,
            onPerformUndo = { undoAction ->
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.performUndo(undoAction)
            },
            onDismissUndo = {
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.dismissActiveUndo()
            },
            onSelectVendor = viewModel::onVendorSelectedForInput,
            onToggleTile = viewModel::toggleQuickDictionaryTile,
            onApplyQuickNote = viewModel::applyQuickNoteToActiveItem,
            onClearTargetItem = viewModel::clearLastTouchedItem,
            onAddCustomUnit = viewModel::addCustomUnitForItem,
            onRemoveCustomUnit = viewModel::removeCustomUnitForItem,
            onItemNameChanged = viewModel::onItemNameChanged,
            onItemNoteChanged = viewModel::onItemNoteChanged,
            onSubmitCustomItem = viewModel::addItemFromInput,
            onDismiss = { showQuickAddPad = false }
        )
    }

    if (showPassingExpenseSheet) {
        PassingExpenseSheet(
            onAddExpense = viewModel::addPassingExpense,
            onDismiss = { closeSubDialogAndRestoreParent { showPassingExpenseSheet = false } }
        )
    }

    if (showToolsMenuDialog) {
        AlertDialog(
            onDismissRequest = { showToolsMenuDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    text = "أدوات الكناش والإدارة",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                returnMenuAfterSubDialog = "TOOLS"
                                showPriceTrackerDialog = true
                            }
                            .testTag("menu_open_price_tracker_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QueryStats,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "كناش الأثمنة ومؤشر الغلاء والرخاء",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تاريخ أثمنة السلع، مقارنة الرخاء والغلاء، وإضافة سعر يدوي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                returnMenuAfterSubDialog = "TOOLS"
                                showPassingExpenseSheet = true
                            }
                            .testTag("menu_open_passing_expense_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "+ تسجيل مصروف عابر",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "خبز، طاكسي، أكياس، أو أي مصروف سريع خارج البائعين",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                returnMenuAfterSubDialog = "TOOLS"
                                showDictionaryDialog = true
                            }
                            .testTag("menu_open_dictionary_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "قاموس الأغراض ودمج الأسماء",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إعادة تسمية السلع أو دمج اسمين (مثل لوبيا ولوبيا بيضاء)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                returnMenuAfterSubDialog = "TOOLS"
                                showVendorManagerDialog = true
                            }
                            .testTag("menu_open_vendors_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "إدارة قائمة البائعين",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إضافة بائع جديد، تعديل الأسماء، أو تغيير الترتيب",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                returnMenuAfterSubDialog = "TOOLS"
                                showShareDialog = true
                            }
                            .testTag("menu_open_share_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "مشاركة القائمة نصياً (واتساب)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إرسال قائمة النواقص كلها أو قائمة بائع محدد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                returnMenuAfterSubDialog = "TOOLS"
                                showTrashBinDialog = true
                            }
                            .testTag("menu_open_trash_bin_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "سلة المحذوفات (${uiState.totalTrashCount})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "استرجاع الأغراض والمصاريف والبائعين المحذوفين خلال 30 يوماً",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                didOpenSettingsFromTools = true
                                showSettingsSheet = true
                            }
                            .testTag("menu_open_settings_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "الإعدادات والخصوصية والمظهر",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "الوضع النهاري/الليلي، تكبير الخط والأزرار، الريال، وعين الخصوصية والرمز السري",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
                                returnMenuAfterSubDialog = "TOOLS"
                                showBackupRestoreDialog = true
                            }
                            .testTag("menu_open_backup_restore_card"),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "النسخ الاحتياطي الشامل وودجت الشاشة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "حفظ واسترجاع كامل الكناش (JSON) وتثبيت ودجت الشاشة الرئيسية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showToolsMenuDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    if (showBudgetDialog) {
        PocketBudgetDialog(
            currentBudgetDirhams = uiState.initialPocketDirhams,
            onDismiss = { showBudgetDialog = false },
            onSaveDirhams = viewModel::updateInitialPocket
        )
    }

    if (showPriceTrackerDialog) {
        AllItemsPriceTrackerDialog(
            dictionaryItems = uiState.dictionaryItems,
            priceHistory = uiState.priceHistory,
            onSelectItemForHistory = { dictItem ->
                priceHistoryTarget = Triple(
                    dictItem.name,
                    dictItem.defaultVendorId,
                    dictItem.lastPriceDirhams
                )
            },
            onDismiss = { closeSubDialogAndRestoreParent { showPriceTrackerDialog = false } }
        )
    }

    if (showDictionaryDialog) {
        DictionaryDialog(
            dictionaryItems = uiState.dictionaryItems,
            vendors = uiState.vendors,
            activeUndo = uiState.activeUndo,
            onPerformUndo = { undoAction ->
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.performUndo(undoAction)
            },
            onDismissUndo = {
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.dismissActiveUndo()
            },
            onAddEntry = viewModel::addDictionaryEntry,
            onRenameEntry = viewModel::renameDictionaryEntry,
            onMergeEntries = viewModel::mergeDictionaryEntries,
            onDeleteEntry = viewModel::deleteDictionaryEntry,
            onDismiss = { closeSubDialogAndRestoreParent { showDictionaryDialog = false } }
        )
    }

    if (showVendorManagerDialog) {
        VendorManagerDialog(
            vendors = uiState.vendors,
            activeUndo = uiState.activeUndo,
            onPerformUndo = { undoAction ->
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.performUndo(undoAction)
            },
            onDismissUndo = {
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.dismissActiveUndo()
            },
            onAddVendor = viewModel::addVendor,
            onUpdateVendor = viewModel::updateVendor,
            onMoveVendor = viewModel::moveVendorOrder,
            onDeleteVendor = viewModel::deleteVendor,
            onDismiss = { closeSubDialogAndRestoreParent { showVendorManagerDialog = false } }
        )
    }

    if (showShareDialog) {
        ShareListDialog(
            vendors = uiState.vendors,
            shoppingItems = uiState.shoppingItems,
            onDismiss = { closeSubDialogAndRestoreParent { showShareDialog = false } },
            onMessageCopied = { msg ->
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                }
            }
        )
    }

    if (showSettingsSheet) {
        AppSettingsSheet(
            settings = uiState.appSettings,
            onUpdateThemeMode = viewModel::updateThemeMode,
            onUpdateUiScaleMode = viewModel::updateUiScaleMode,
            onUpdateColorPalette = viewModel::updateColorPalette,
            onUpdateAnimationMode = viewModel::updateAnimationMode,
            onUpdateFontFamilyKey = viewModel::updateFontFamilyKey,
            onCustomFontImported = viewModel::onCustomFontSlotImported,
            onClearCustomFonts = viewModel::clearAllCustomFonts,
            onNotifyMessage = viewModel::notifyStatusMessage,
            onToggleShowRial = viewModel::toggleShowRial,
            onTogglePrivacyMask = viewModel::togglePrivacyMask,
            onUpdatePrivacyMaskPocketOnly = viewModel::updatePrivacyMaskPocketOnly,
            onSetPinCode = viewModel::setOrUpdatePinCode,
            onRemovePinCode = viewModel::removePinCode,
            onLockAppNow = viewModel::lockAppNow,
            onOpenBackupRestore = {
                showSettingsSheet = false
                returnMenuAfterSubDialog = "SETTINGS"
                showBackupRestoreDialog = true
            },
            onDismiss = { closeSettingsAndRestoreParent() }
        )
    }

    if (showTrashBinDialog) {
        TrashBinDialog(
            deletedItems = uiState.deletedShoppingItems,
            deletedExpenses = uiState.deletedPassingExpenses,
            deletedVendors = uiState.deletedVendors,
            vendors = uiState.vendors,
            onRestoreShoppingItem = viewModel::restoreFromTrashShoppingItem,
            onRestorePassingExpense = viewModel::restoreFromTrashPassingExpense,
            onRestoreVendor = viewModel::restoreFromTrashVendor,
            onPermanentDeleteShoppingItem = viewModel::permanentlyDeleteFromTrashShoppingItem,
            onPermanentDeletePassingExpense = viewModel::permanentlyDeleteFromTrashPassingExpense,
            onPermanentDeleteVendor = viewModel::permanentlyDeleteFromTrashVendor,
            onEmptyAllTrash = viewModel::emptyAllTrash,
            onDismiss = { closeSubDialogAndRestoreParent { showTrashBinDialog = false } }
        )
    }

    if (showBackupRestoreDialog) {
        BackupRestoreDialog(
            uiState = uiState,
            onExportBackupJson = viewModel::exportBackupJson,
            onImportBackupJson = viewModel::importBackupJson,
            onNotifyMessage = viewModel::notifyStatusMessage,
            onDismiss = { closeSubDialogAndRestoreParent { showBackupRestoreDialog = false } }
        )
    }

    val itemToEdit = editingShoppingItem
    if (itemToEdit != null) {
        EditShoppingItemDialog(
            item = itemToEdit,
            vendors = uiState.vendors,
            dictionaryItems = uiState.dictionaryItems,
            onAddCustomUnit = viewModel::addCustomUnitForItem,
            onRemoveCustomUnit = viewModel::removeCustomUnitForItem,
            onDismiss = { editingShoppingItem = null },
            onSave = { newName, newNote, newVendorId ->
                viewModel.updateShoppingItem(itemToEdit, newName, newNote, newVendorId)
            }
        )
    }

    val targetHistory = priceHistoryTarget
    if (targetHistory != null) {
        ItemPriceHistorySheet(
            itemName = targetHistory.first,
            defaultVendorId = targetHistory.second,
            vendors = uiState.vendors,
            allPriceHistory = uiState.priceHistory,
            fallbackLastPriceDirhams = targetHistory.third,
            activeUndo = uiState.activeUndo,
            onPerformUndo = { undoAction ->
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.performUndo(undoAction)
            },
            onDismissUndo = {
                snackbarHostState.currentSnackbarData?.dismiss()
                viewModel.dismissActiveUndo()
            },
            onAddManualPriceRecord = viewModel::addManualPriceRecord,
            onDeletePriceRecord = viewModel::deletePriceHistoryRecord,
            onDismiss = { priceHistoryTarget = null }
        )
    }
}

