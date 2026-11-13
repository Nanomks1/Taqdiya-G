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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Scale
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
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.pdr.taqdiya2.data.local.AppDatabase
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.repository.TaqdiyaRepository
import az.pdr.taqdiya2.ui.components.DictionaryDialog
import az.pdr.taqdiya2.ui.components.EditShoppingItemDialog
import az.pdr.taqdiya2.ui.components.PocketBudgetDialog
import az.pdr.taqdiya2.ui.components.QuickAddPadSheet
import az.pdr.taqdiya2.ui.components.ShareListDialog
import az.pdr.taqdiya2.ui.components.TopPocketBar
import az.pdr.taqdiya2.ui.components.VendorManagerDialog
import az.pdr.taqdiya2.ui.screens.AtVendorScreen
import az.pdr.taqdiya2.ui.screens.HomePrepScreen
import az.pdr.taqdiya2.ui.screens.UpcomingBatchScreen
import az.pdr.taqdiya2.ui.theme.TaqdiyaTheme
import az.pdr.taqdiya2.ui.viewmodel.HomePrepViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = TaqdiyaRepository(database.taqdiyaDao())

        setContent {
            TaqdiyaTheme {
                val viewModel: HomePrepViewModel = viewModel(
                    factory = HomePrepViewModel.Factory(repository)
                )
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showQuickAddPad by rememberSaveable { mutableStateOf(false) }
    var showToolsMenuDialog by rememberSaveable { mutableStateOf(false) }
    var showBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var showDictionaryDialog by rememberSaveable { mutableStateOf(false) }
    var showVendorManagerDialog by rememberSaveable { mutableStateOf(false) }
    var showShareDialog by rememberSaveable { mutableStateOf(false) }
    var editingShoppingItem by remember { mutableStateOf<ShoppingItemEntity?>(null) }

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Listen to Undo Events (Delete Item / Vendor / Dictionary Entry)
    LaunchedEffect(viewModel) {
        viewModel.undoEvents.collect { undoAction ->
            val result = snackbarHostState.showSnackbar(
                message = undoAction.message,
                actionLabel = "تراجع",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.performUndo(undoAction)
            }
        }
    }

    // Listen to Status Messages
    LaunchedEffect(viewModel) {
        viewModel.statusMessages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Double-Back to Exit & Secondary Sheet/Tab Back Navigation
    BackHandler {
        when {
            editingShoppingItem != null -> editingShoppingItem = null
            showQuickAddPad -> showQuickAddPad = false
            showBudgetDialog -> showBudgetDialog = false
            showDictionaryDialog -> showDictionaryDialog = false
            showVendorManagerDialog -> showVendorManagerDialog = false
            showShareDialog -> showShareDialog = false
            showToolsMenuDialog -> showToolsMenuDialog = false
            selectedTab != 0 -> selectedTab = 0
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    onExitApp()
                } else {
                    lastBackPressTime = now
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "اضغط رجوع مرة أخرى للخروج من التطبيق",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }
        }
    }

    val activeMissingItems = remember(uiState.shoppingItems) {
        uiState.shoppingItems.filter { !it.isDeleted && !it.isPurchased }
    }
    val activeVendorsWithItemsCount = remember(uiState.shoppingItems) {
        uiState.shoppingItems.filter { !it.isDeleted }.map { it.vendorId }.distinct().size
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopPocketBar(
                remainingPocketDirhams = uiState.remainingPocketDirhams,
                initialPocketDirhams = uiState.initialPocketDirhams,
                totalMissingItemsCount = activeMissingItems.size,
                onEditBudgetClick = { showBudgetDialog = true },
                onOpenToolsSheetClick = { showToolsMenuDialog = true },
                onShareListClick = { showShareDialog = true }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                            contentDescription = "التحضير في البيت",
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "التحضير في البيت",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
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
                            fontWeight = FontWeight.Bold
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
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("nav_tab_pocket_audit")
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                HomePrepScreen(
                    uiState = uiState,
                    onOpenQuickPadForVendor = { vendorId ->
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
                    onOpenDictionaryClick = { showDictionaryDialog = true },
                    onOpenVendorsClick = { showVendorManagerDialog = true },
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
                    onOpenQuickPadForVendor = { vendorId ->
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
                UpcomingBatchScreen(
                    title = "شاشة [جرد الجيب والأرشيف]",
                    batchLabel = "مجدولة للدفعة الثالثة (Batch 3)",
                    description = "ستتضمن هذه الشاشة في الدفعة الثالثة: تأكيد الدفع النهائي للبائع وتعديل المبلغ المدفوع فعلياً، والصرف العالق (بقا ليا عندو / بقا ليه عندي)، والمصاريف العابرة، وكشف الحساب الختامي المفصل بعد العودة للبيت مع إنهاء الجولة وترحيل النواقص المتبقية.",
                    isMarketScreen = false,
                    missingItemsCount = activeMissingItems.size,
                    activeVendorsWithItemsCount = activeVendorsWithItemsCount,
                    initialPocketDirhams = uiState.initialPocketDirhams,
                    onReturnToHomePrep = { selectedTab = 0 },
                    modifier = Modifier.padding(innerPadding)
                )
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
            onSelectVendor = viewModel::onVendorSelectedForInput,
            onToggleTile = viewModel::toggleQuickDictionaryTile,
            onApplyQuickNote = viewModel::applyQuickNoteToActiveItem,
            onItemNameChanged = viewModel::onItemNameChanged,
            onItemNoteChanged = viewModel::onItemNoteChanged,
            onSubmitCustomItem = viewModel::addItemFromInput,
            onDismiss = { showQuickAddPad = false }
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showToolsMenuDialog = false
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

    if (showDictionaryDialog) {
        DictionaryDialog(
            dictionaryItems = uiState.dictionaryItems,
            vendors = uiState.vendors,
            onAddEntry = viewModel::addDictionaryEntry,
            onRenameEntry = viewModel::renameDictionaryEntry,
            onMergeEntries = viewModel::mergeDictionaryEntries,
            onDeleteEntry = viewModel::deleteDictionaryEntry,
            onDismiss = { showDictionaryDialog = false }
        )
    }

    if (showVendorManagerDialog) {
        VendorManagerDialog(
            vendors = uiState.vendors,
            onAddVendor = viewModel::addVendor,
            onUpdateVendor = viewModel::updateVendor,
            onMoveVendor = viewModel::moveVendorOrder,
            onDeleteVendor = viewModel::deleteVendor,
            onDismiss = { showVendorManagerDialog = false }
        )
    }

    if (showShareDialog) {
        ShareListDialog(
            vendors = uiState.vendors,
            shoppingItems = uiState.shoppingItems,
            onDismiss = { showShareDialog = false },
            onMessageCopied = { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                }
            }
        )
    }

    val itemToEdit = editingShoppingItem
    if (itemToEdit != null) {
        EditShoppingItemDialog(
            item = itemToEdit,
            vendors = uiState.vendors,
            onDismiss = { editingShoppingItem = null },
            onSave = { newName, newNote, newVendorId ->
                viewModel.updateShoppingItem(itemToEdit, newName, newNote, newVendorId)
            }
        )
    }
}
