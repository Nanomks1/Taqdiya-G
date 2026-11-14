package az.pdr.taqdiya2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import az.pdr.taqdiya2.data.local.AppSettingsEntity
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.PassingExpenseEntity
import az.pdr.taqdiya2.data.local.PriceHistoryEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.data.local.VendorPaymentEntity
import az.pdr.taqdiya2.data.repository.TaqdiyaRepository
import az.pdr.taqdiya2.util.CurrencyFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UndoAction(val message: String) {
    data class DeletedShoppingItem(val itemId: Long, val itemName: String) :
        UndoAction("تم نقل الغرض «$itemName» إلى سلة المحذوفات")

    data class DeletedVendor(val vendorId: Long, val vendorName: String) :
        UndoAction("تم نقل البائع «$vendorName» إلى سلة المحذوفات")

    data class DeletedDictionaryItem(val entry: DictionaryItemEntity) :
        UndoAction("تم حذف «${entry.name}» من القاموس")

    data class UnmarkedPurchase(val previousSnapshot: ShoppingItemEntity) :
        UndoAction("تم إرجاع «${previousSnapshot.name}» للانتظار")

    data class ConfirmedPurchase(val previousSnapshot: ShoppingItemEntity, val paidDirhams: Double) :
        UndoAction("تم وضع «${previousSnapshot.name}» في القفة (${CurrencyFormatter.formatDirhams(paidDirhams)})")

    data class EditedShoppingItem(val previousSnapshot: ShoppingItemEntity, val newName: String) :
        UndoAction("تم تعديل الغرض «$newName»")

    data class UpdatedPocketBudget(
        val roundId: Long,
        val previousBudgetDirhams: Double,
        val newBudgetDirhams: Double
    ) : UndoAction("تم تعديل الميزانية إلى ${CurrencyFormatter.formatDirhams(newBudgetDirhams)}")

    data class MergedDictionaryItems(
        val deletedSource: DictionaryItemEntity,
        val previousTarget: DictionaryItemEntity,
        val affectedItemsBeforeMerge: List<ShoppingItemEntity>
    ) : UndoAction("تم دمج «${deletedSource.name}» في «${previousTarget.name}»")

    data class RenamedDictionaryItem(
        val previousEntry: DictionaryItemEntity,
        val affectedItemsBeforeRename: List<ShoppingItemEntity>,
        val newName: String
    ) : UndoAction("تم تغيير اسم «${previousEntry.name}» إلى «$newName»")

    data class ConfirmedVendorPayment(
        val roundId: Long,
        val vendorId: Long,
        val vendorName: String,
        val actualPaidDirhams: Double,
        val previousPayment: VendorPaymentEntity?,
        val previousPendingBalanceDirhams: Double
    ) : UndoAction("تم تأكيد دفع ${CurrencyFormatter.formatDirhams(actualPaidDirhams)} لـ «$vendorName»")

    data class AdjustedVendorPendingBalance(
        val vendorId: Long,
        val vendorName: String,
        val previousBalanceDirhams: Double,
        val newBalanceDirhams: Double
    ) : UndoAction("تم تحديث الصرف العالق عند «$vendorName»")

    data class AddedPassingExpense(
        val expenseId: Long,
        val title: String,
        val amountDirhams: Double
    ) : UndoAction("تم تسجيل مصروف عابر «$title» (${CurrencyFormatter.formatDirhams(amountDirhams)})")

    data class DeletedPassingExpense(
        val expenseId: Long,
        val title: String,
        val amountDirhams: Double
    ) : UndoAction("تم نقل المصروف العابر «$title» إلى سلة المحذوفات")

    data class DeletedArchivedRound(
        val round: ShoppingRoundEntity
    ) : UndoAction("تم حذف جولة «${round.title}» من الأرشيف")

    data class DeletedPriceRecord(
        val record: PriceHistoryEntity
    ) : UndoAction("تم حذف سجل ثمن «${record.itemName}» (${CurrencyFormatter.formatDirhams(record.unitPriceDirhams)})")

    data class ResetVendorPaymentAndBasket(
        val roundId: Long,
        val vendorId: Long,
        val vendorName: String,
        val previousPayment: VendorPaymentEntity?,
        val previousPurchasedItems: List<ShoppingItemEntity>
    ) : UndoAction("تم إلغاء وتصفير حساب «$vendorName»")
}

data class HomePrepUiState(
    val isLoading: Boolean = true,
    val activeRound: ShoppingRoundEntity? = null,
    val archivedRounds: List<ShoppingRoundEntity> = emptyList(),
    val vendors: List<VendorEntity> = emptyList(),
    val shoppingItems: List<ShoppingItemEntity> = emptyList(),
    val vendorPayments: List<VendorPaymentEntity> = emptyList(),
    val passingExpenses: List<PassingExpenseEntity> = emptyList(),
    val dictionaryItems: List<DictionaryItemEntity> = emptyList(),
    val priceHistory: List<PriceHistoryEntity> = emptyList(),
    val appSettings: AppSettingsEntity = AppSettingsEntity(),
    val deletedShoppingItems: List<ShoppingItemEntity> = emptyList(),
    val deletedPassingExpenses: List<PassingExpenseEntity> = emptyList(),
    val deletedVendors: List<VendorEntity> = emptyList(),
    val isPinLocked: Boolean = false,
    val itemNameInput: String = "",
    val itemNoteInput: String = "",
    val selectedVendorIdForInput: Long? = null,
    val autoMatchedVendorName: String? = null,
    val searchQuery: String = "",
    val filterVendorId: Long? = null,
    val lastTouchedItemId: Long? = null,
    val selectedMarketVendorId: Long? = null,
    val keepScreenAwakeInMarket: Boolean = true,
    val activeUndo: UndoAction? = null
) {
    val totalTrashCount: Int
        get() = deletedShoppingItems.size + deletedPassingExpenses.size + deletedVendors.size

    val initialPocketDirhams: Double
        get() = activeRound?.initialPocketDirhams ?: 200.0

    val totalCalculatedItemsDirhams: Double
        get() = shoppingItems.filter { it.isPurchased && !it.isDeleted }.sumOf { it.totalDirhams }

    val totalActualVendorPaidDirhams: Double
        get() {
            val activePurchased = shoppingItems.filter { it.isPurchased && !it.isDeleted }
            val paymentsByVendor = vendorPayments.associateBy { it.vendorId }
            val involvedVendorIds = (activePurchased.map { it.vendorId } + paymentsByVendor.keys).toSet()
            return involvedVendorIds.sumOf { vendorId ->
                val confirmedPayment = paymentsByVendor[vendorId]
                if (confirmedPayment != null) {
                    confirmedPayment.actualPaidDirhams
                } else {
                    activePurchased.filter { it.vendorId == vendorId }.sumOf { it.totalDirhams }
                }
            }
        }

    val totalPassingExpensesDirhams: Double
        get() = passingExpenses.filter { !it.isDeleted }.sumOf { it.amountDirhams }

    val totalActualSpentDirhams: Double
        get() = totalActualVendorPaidDirhams + totalPassingExpensesDirhams

    val totalPurchasedDirhams: Double
        get() = totalActualSpentDirhams

    val totalForgivenSavingsDirhams: Double
        get() = vendorPayments.sumOf { it.forgivenDiscountDirhams }

    val netPocketBalanceDirhams: Double
        get() = initialPocketDirhams - totalActualSpentDirhams

    val remainingPocketDirhams: Double
        get() = netPocketBalanceDirhams.coerceAtLeast(0.0)
}

private data class InputDraftState(
    val itemNameInput: String = "",
    val itemNoteInput: String = "",
    val selectedVendorIdForInput: Long? = null,
    val isVendorManuallyOverridden: Boolean = false,
    val autoMatchedVendorName: String? = null,
    val searchQuery: String = "",
    val filterVendorId: Long? = null,
    val lastTouchedItemId: Long? = null,
    val selectedMarketVendorId: Long? = null,
    val keepScreenAwakeInMarket: Boolean = true,
    val isSessionPinUnlocked: Boolean = false,
    val isManualPinLockRequested: Boolean = false,
    val activeUndo: UndoAction? = null
)

private data class RoundLedgerStreams(
    val activeRound: ShoppingRoundEntity?,
    val archivedRounds: List<ShoppingRoundEntity>,
    val allVendorPayments: List<VendorPaymentEntity>,
    val allPassingExpenses: List<PassingExpenseEntity>,
    val allPriceHistory: List<PriceHistoryEntity>
)

private data class SettingsAndTrashStreams(
    val appSettings: AppSettingsEntity,
    val deletedShoppingItems: List<ShoppingItemEntity>,
    val deletedPassingExpenses: List<PassingExpenseEntity>,
    val deletedVendors: List<VendorEntity>
)

class HomePrepViewModel(
    private val repository: TaqdiyaRepository,
    initialSettings: AppSettingsEntity = AppSettingsEntity()
) : ViewModel() {

    private val draftState = MutableStateFlow(InputDraftState())
    private val undoStack = ArrayDeque<UndoAction>()
    private var undoExpiryJob: Job? = null

    private val _undoEvents = MutableSharedFlow<UndoAction>(extraBufferCapacity = 1)
    val undoEvents: SharedFlow<UndoAction> = _undoEvents.asSharedFlow()

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val statusMessages: SharedFlow<String> = _statusMessages.asSharedFlow()

    private val ledgerStreams = combine(
        repository.activeRound,
        repository.archivedRounds,
        repository.allVendorPayments,
        repository.allPassingExpenses,
        repository.allPriceHistory
    ) { round, archived, payments, expenses, priceHistory ->
        RoundLedgerStreams(
            activeRound = round,
            archivedRounds = archived,
            allVendorPayments = payments,
            allPassingExpenses = expenses,
            allPriceHistory = priceHistory
        )
    }

    private val settingsAndTrashStreams = combine(
        repository.appSettings,
        repository.deletedShoppingItems,
        repository.deletedPassingExpenses,
        repository.deletedVendors
    ) { settings, delItems, delExpenses, delVendors ->
        val resolvedSettings = settings ?: initialSettings
        CurrencyFormatter.showRialState = resolvedSettings.showRial
        CurrencyFormatter.privacyMaskState = resolvedSettings.privacyMaskAmounts
        CurrencyFormatter.privacyMaskPocketOnlyState = resolvedSettings.privacyMaskPocketOnly
        SettingsAndTrashStreams(
            appSettings = resolvedSettings,
            deletedShoppingItems = delItems,
            deletedPassingExpenses = delExpenses,
            deletedVendors = delVendors
        )
    }

    private val coreDataStreams = combine(
        ledgerStreams,
        settingsAndTrashStreams,
        repository.activeVendors,
        repository.allActiveItems,
        repository.dictionaryItems
    ) { ledger, settingsAndTrash, vendors, allItems, dictItems ->
        Quint(ledger, settingsAndTrash, vendors, allItems, dictItems)
    }

    private data class Quint<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    val uiState: StateFlow<HomePrepUiState> = combine(
        coreDataStreams,
        draftState
    ) { core, draft ->
        val ledger = core.first
        val settingsAndTrash = core.second
        val vendors = core.third
        val allItems = core.fourth
        val dictItems = core.fifth

        val round = ledger.activeRound
        val roundItems = if (round != null) {
            allItems.filter { it.roundId == round.id && !it.isDeleted }
        } else {
            emptyList()
        }

        val roundVendorPayments = if (round != null) {
            ledger.allVendorPayments.filter { it.roundId == round.id }
        } else {
            emptyList()
        }

        val roundPassingExpenses = if (round != null) {
            ledger.allPassingExpenses.filter { it.roundId == round.id && !it.isDeleted }
        } else {
            emptyList()
        }

        val resolvedVendorId = when {
            draft.selectedVendorIdForInput != null && vendors.any { it.id == draft.selectedVendorIdForInput } ->
                draft.selectedVendorIdForInput
            vendors.isNotEmpty() -> vendors.first().id
            else -> null
        }

        val validLastTouchedId = draft.lastTouchedItemId?.takeIf { id ->
            roundItems.any { it.id == id && !it.isPurchased }
        }

        val vendorsWithItemsIds = roundItems.map { it.vendorId }.toSet()
        val marketVendors = vendors.filter { vendorsWithItemsIds.contains(it.id) }
        val resolvedMarketVendorId = when {
            draft.selectedMarketVendorId != null && marketVendors.any { it.id == draft.selectedMarketVendorId } ->
                draft.selectedMarketVendorId
            marketVendors.isNotEmpty() -> {
                marketVendors.firstOrNull { v ->
                    roundItems.any { it.vendorId == v.id && !it.isPurchased }
                }?.id ?: marketVendors.first().id
            }
            else -> vendors.firstOrNull()?.id
        }

        val hasPin = settingsAndTrash.appSettings.pinCode.isNotBlank()
        val isLocked = hasPin && (!draft.isSessionPinUnlocked || draft.isManualPinLockRequested)

        HomePrepUiState(
            isLoading = false,
            activeRound = round,
            archivedRounds = ledger.archivedRounds,
            vendors = vendors,
            shoppingItems = roundItems,
            vendorPayments = roundVendorPayments,
            passingExpenses = roundPassingExpenses,
            dictionaryItems = dictItems,
            priceHistory = ledger.allPriceHistory,
            appSettings = settingsAndTrash.appSettings,
            deletedShoppingItems = settingsAndTrash.deletedShoppingItems,
            deletedPassingExpenses = settingsAndTrash.deletedPassingExpenses,
            deletedVendors = settingsAndTrash.deletedVendors,
            isPinLocked = isLocked,
            itemNameInput = draft.itemNameInput,
            itemNoteInput = draft.itemNoteInput,
            selectedVendorIdForInput = resolvedVendorId,
            autoMatchedVendorName = draft.autoMatchedVendorName,
            searchQuery = draft.searchQuery,
            filterVendorId = draft.filterVendorId,
            lastTouchedItemId = validLastTouchedId,
            selectedMarketVendorId = resolvedMarketVendorId,
            keepScreenAwakeInMarket = draft.keepScreenAwakeInMarket,
            activeUndo = draft.activeUndo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomePrepUiState(appSettings = initialSettings)
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    private fun pushUndoAction(action: UndoAction) {
        undoStack.addLast(action)
        if (undoStack.size > 15) {
            undoStack.removeFirst()
        }
        draftState.value = draftState.value.copy(activeUndo = action)
        _undoEvents.tryEmit(action)

        undoExpiryJob?.cancel()
        undoExpiryJob = viewModelScope.launch {
            delay(5000L)
            if (draftState.value.activeUndo == action) {
                draftState.value = draftState.value.copy(activeUndo = null)
            }
        }
    }

    fun dismissActiveUndo() {
        undoExpiryJob?.cancel()
        draftState.value = draftState.value.copy(activeUndo = null)
    }

    fun onSelectMarketVendor(vendorId: Long) {
        draftState.value = draftState.value.copy(selectedMarketVendorId = vendorId)
    }

    fun toggleKeepScreenAwake() {
        val next = !draftState.value.keepScreenAwakeInMarket
        draftState.value = draftState.value.copy(keepScreenAwakeInMarket = next)
        _statusMessages.tryEmit(
            if (next) "تم تفعيل إبقاء الشاشة مضاءة أثناء التسوق"
            else "تم إيقاف إبقاء الشاشة مضاءة"
        )
    }

    fun confirmItemPurchase(
        item: ShoppingItemEntity,
        purchaseMode: String,
        unitPriceDirhams: Double,
        quantity: Double,
        cleaningFeeDirhams: Double,
        totalDirhams: Double
    ) {
        val previousSnapshot = item.copy()
        viewModelScope.launch {
            repository.confirmItemPurchase(
                item = item,
                purchaseMode = purchaseMode,
                unitPriceDirhams = unitPriceDirhams,
                quantity = quantity,
                cleaningFeeDirhams = cleaningFeeDirhams,
                totalDirhams = totalDirhams
            )
            pushUndoAction(
                UndoAction.ConfirmedPurchase(
                    previousSnapshot = previousSnapshot,
                    paidDirhams = totalDirhams
                )
            )
        }
    }

    fun unmarkItemPurchased(item: ShoppingItemEntity) {
        val previousSnapshot = item.copy()
        viewModelScope.launch {
            repository.unmarkItemPurchased(item)
            pushUndoAction(UndoAction.UnmarkedPurchase(previousSnapshot = previousSnapshot))
        }
    }

    // --- Batch 4: Price History Manual Actions ---
    fun addManualPriceRecord(
        itemName: String,
        vendorId: Long,
        purchaseMode: String,
        unitPriceDirhams: Double
    ) {
        if (unitPriceDirhams <= 0.0 || itemName.isBlank()) return
        val vendorName = uiState.value.vendors.firstOrNull { it.id == vendorId }?.name ?: "البائع"
        viewModelScope.launch {
            repository.addManualPriceRecord(
                itemName = itemName,
                vendorId = vendorId,
                vendorName = vendorName,
                purchaseMode = purchaseMode,
                unitPriceDirhams = unitPriceDirhams
            )
            _statusMessages.tryEmit("تم تسجيل ثمن مرجعي (${CurrencyFormatter.formatDirhams(unitPriceDirhams)}) لـ «$itemName»")
        }
    }

    fun deletePriceHistoryRecord(record: PriceHistoryEntity) {
        viewModelScope.launch {
            repository.deletePriceHistoryRecord(record.id)
            pushUndoAction(UndoAction.DeletedPriceRecord(record))
        }
    }

    // --- Batch 3: Confirm Vendor Payment & Pending Change ---
    fun confirmVendorPayment(
        vendor: VendorEntity,
        calculatedTotalDirhams: Double,
        actualPaidDirhams: Double,
        forgivenDiscountDirhams: Double,
        newVendorPendingBalanceDirhams: Double
    ) {
        val state = uiState.value
        val roundId = state.activeRound?.id ?: return
        val previousPayment = state.vendorPayments.firstOrNull { it.vendorId == vendor.id }
        val previousPendingBalance = vendor.pendingBalanceDirhams

        viewModelScope.launch {
            repository.confirmVendorPayment(
                roundId = roundId,
                vendorId = vendor.id,
                calculatedTotalDirhams = calculatedTotalDirhams,
                actualPaidDirhams = actualPaidDirhams,
                forgivenDiscountDirhams = forgivenDiscountDirhams,
                newVendorPendingBalanceDirhams = newVendorPendingBalanceDirhams
            )
            pushUndoAction(
                UndoAction.ConfirmedVendorPayment(
                    roundId = roundId,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    actualPaidDirhams = actualPaidDirhams,
                    previousPayment = previousPayment,
                    previousPendingBalanceDirhams = previousPendingBalance
                )
            )
        }
    }

    fun updateVendorPendingBalance(vendor: VendorEntity, newPendingBalanceDirhams: Double) {
        val previousBalance = vendor.pendingBalanceDirhams
        if (kotlin.math.abs(previousBalance - newPendingBalanceDirhams) < 0.001) return
        viewModelScope.launch {
            repository.updateVendorPendingBalance(vendor.id, newPendingBalanceDirhams)
            pushUndoAction(
                UndoAction.AdjustedVendorPendingBalance(
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    previousBalanceDirhams = previousBalance,
                    newBalanceDirhams = newPendingBalanceDirhams
                )
            )
        }
    }

    fun resetVendorPaymentAndBasket(vendor: VendorEntity, alsoUnmarkBasketItems: Boolean = true) {
        val state = uiState.value
        val roundId = state.activeRound?.id ?: return
        val previousPayment = state.vendorPayments.firstOrNull { it.vendorId == vendor.id }
        val vendorPurchasedItems = if (alsoUnmarkBasketItems) {
            state.shoppingItems.filter { it.vendorId == vendor.id && it.isPurchased && !it.isDeleted }
                .map { it.copy() }
        } else {
            emptyList()
        }
        if (previousPayment == null && vendorPurchasedItems.isEmpty()) return

        viewModelScope.launch {
            repository.cancelVendorPaymentAndOptionallyResetBasket(
                roundId = roundId,
                vendorId = vendor.id,
                itemsToUnmark = vendorPurchasedItems
            )
            pushUndoAction(
                UndoAction.ResetVendorPaymentAndBasket(
                    roundId = roundId,
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    previousPayment = previousPayment,
                    previousPurchasedItems = vendorPurchasedItems
                )
            )
        }
    }

    // --- Batch 3: Passing Expenses (+ مصروف عابر) ---
    fun addPassingExpense(title: String, amountDirhams: Double) {
        if (amountDirhams <= 0.0) return
        val roundId = uiState.value.activeRound?.id ?: return
        val cleanTitle = title.trim().ifEmpty { "مصروف عابر" }
        viewModelScope.launch {
            val created = repository.addPassingExpense(
                roundId = roundId,
                title = cleanTitle,
                amountDirhams = amountDirhams
            )
            pushUndoAction(
                UndoAction.AddedPassingExpense(
                    expenseId = created.id,
                    title = created.title,
                    amountDirhams = created.amountDirhams
                )
            )
        }
    }

    fun deletePassingExpense(expense: PassingExpenseEntity) {
        viewModelScope.launch {
            repository.softDeletePassingExpense(expense.id)
            pushUndoAction(
                UndoAction.DeletedPassingExpense(
                    expenseId = expense.id,
                    title = expense.title,
                    amountDirhams = expense.amountDirhams
                )
            )
        }
    }

    // --- Batch 3: Finish Round, Archive & Rollover Unpurchased Items ---
    fun finishAndArchiveCurrentRound(
        newPocketDirhams: Double,
        newRoundTitle: String,
        rolloverUnpurchasedItems: Boolean
    ) {
        val state = uiState.value
        val currentRound = state.activeRound ?: return
        val unpurchasedItems = state.shoppingItems.filter { !it.isPurchased && !it.isDeleted }
        val purchasedCount = state.shoppingItems.count { it.isPurchased && !it.isDeleted }
        val totalSpent = state.totalActualSpentDirhams
        val totalSavings = state.totalForgivenSavingsDirhams

        viewModelScope.launch {
            repository.finishAndArchiveCurrentRound(
                currentRound = currentRound,
                newPocketDirhams = newPocketDirhams,
                newRoundTitle = newRoundTitle,
                rolloverUnpurchasedItems = rolloverUnpurchasedItems,
                unpurchasedItems = unpurchasedItems,
                totalSpentDirhams = totalSpent,
                totalSavingsDirhams = totalSavings,
                purchasedItemsCount = purchasedCount
            )
            val rolloverMsg = if (rolloverUnpurchasedItems && unpurchasedItems.isNotEmpty()) {
                " مع ترحيل ${unpurchasedItems.size} نواقص للجولة الجديدة"
            } else {
                ""
            }
            _statusMessages.tryEmit("تم حفظ الجولة في الأرشيف وبدء جولة جديدة$rolloverMsg")
        }
    }

    suspend fun loadArchivedRoundItems(roundId: Long): List<ShoppingItemEntity> {
        return repository.getItemsForRoundOnce(roundId)
    }

    fun deleteArchivedRound(round: ShoppingRoundEntity) {
        viewModelScope.launch {
            repository.deleteArchivedRound(round.id)
            pushUndoAction(UndoAction.DeletedArchivedRound(round))
        }
    }

    fun onItemNameChanged(newName: String) {
        val currentState = uiState.value
        val normalized = CurrencyFormatter.normalizeArabicName(newName)
        var matchedVendorId = draftState.value.selectedVendorIdForInput
        var matchedVendorName: String? = null
        var suggestedNote = draftState.value.itemNoteInput

        if (normalized.isNotEmpty()) {
            val exactMatch = currentState.dictionaryItems.firstOrNull {
                it.normalizedName == normalized
            } ?: currentState.dictionaryItems.firstOrNull {
                it.normalizedName.startsWith(normalized) && normalized.length >= 2
            }

            if (exactMatch != null) {
                val vendor = currentState.vendors.firstOrNull { it.id == exactMatch.defaultVendorId }
                if (vendor != null) {
                    matchedVendorId = vendor.id
                    matchedVendorName = vendor.name
                    if (suggestedNote.isBlank() && exactMatch.defaultNote.isNotBlank()) {
                        suggestedNote = exactMatch.defaultNote
                    }
                }
            }
        }

        draftState.value = draftState.value.copy(
            itemNameInput = newName,
            itemNoteInput = suggestedNote,
            selectedVendorIdForInput = matchedVendorId,
            autoMatchedVendorName = matchedVendorName
        )
    }

    fun onItemNoteChanged(newNote: String) {
        draftState.value = draftState.value.copy(itemNoteInput = newNote)
    }

    fun onVendorSelectedForInput(vendorId: Long) {
        draftState.value = draftState.value.copy(
            selectedVendorIdForInput = vendorId,
            isVendorManuallyOverridden = true,
            autoMatchedVendorName = null
        )
    }

    fun onSearchQueryChanged(query: String) {
        draftState.value = draftState.value.copy(searchQuery = query)
    }

    fun onFilterVendorChanged(vendorId: Long?) {
        draftState.value = draftState.value.copy(filterVendorId = vendorId)
    }

    fun addItemFromInput() {
        val state = uiState.value
        val roundId = state.activeRound?.id ?: return
        val name = state.itemNameInput.trim()
        if (name.isEmpty()) return
        val cleanNote = state.itemNoteInput.trim()
        val vendorId = state.selectedVendorIdForInput ?: state.vendors.firstOrNull()?.id ?: return

        viewModelScope.launch {
            val created = repository.addShoppingItemAndLearn(
                roundId = roundId,
                name = name,
                note = cleanNote,
                vendorId = vendorId
            )
            // If user already entered a note (e.g. "علبة" for "جافيل"), do not leave the item stuck in the 1-tap unit bar
            draftState.value = draftState.value.copy(
                itemNameInput = "",
                itemNoteInput = "",
                autoMatchedVendorName = null,
                isVendorManuallyOverridden = false,
                lastTouchedItemId = if (cleanNote.isNotEmpty()) null else created.id,
                selectedMarketVendorId = vendorId
            )
        }
    }

    fun clearLastTouchedItem() {
        draftState.value = draftState.value.copy(lastTouchedItemId = null)
    }

    fun toggleQuickDictionaryTile(dictItem: DictionaryItemEntity, overrideVendorId: Long? = null) {
        val state = uiState.value
        val roundId = state.activeRound?.id ?: return
        val existingItem = state.shoppingItems.firstOrNull {
            !it.isDeleted && !it.isPurchased &&
                CurrencyFormatter.normalizeArabicName(it.name) == dictItem.normalizedName
        }

        viewModelScope.launch {
            if (existingItem != null) {
                repository.softDeleteShoppingItem(existingItem.id)
                if (draftState.value.lastTouchedItemId == existingItem.id) {
                    draftState.value = draftState.value.copy(lastTouchedItemId = null)
                }
                pushUndoAction(UndoAction.DeletedShoppingItem(existingItem.id, existingItem.name))
            } else {
                val targetVendorId = overrideVendorId
                    ?: state.vendors.firstOrNull { it.id == dictItem.defaultVendorId }?.id
                    ?: state.vendors.firstOrNull()?.id ?: return@launch

                val created = repository.addShoppingItemAndLearn(
                    roundId = roundId,
                    name = dictItem.name,
                    note = dictItem.defaultNote,
                    vendorId = targetVendorId
                )
                draftState.value = draftState.value.copy(
                    lastTouchedItemId = created.id,
                    selectedMarketVendorId = targetVendorId
                )
            }
        }
    }

    fun applyQuickNoteToActiveItem(noteText: String) {
        val state = uiState.value
        val targetId = state.lastTouchedItemId ?: return
        val targetItem = state.shoppingItems.firstOrNull { it.id == targetId } ?: return
        viewModelScope.launch {
            val newNote = if (targetItem.note == noteText) "" else noteText
            repository.updateShoppingItem(targetItem.copy(note = newNote))
        }
    }

    fun addCustomUnitForItem(itemName: String, vendorId: Long, customUnit: String) {
        val cleanName = itemName.trim()
        val cleanUnit = customUnit.trim()
        if (cleanName.isEmpty() || cleanUnit.isEmpty()) return
        viewModelScope.launch {
            repository.addCustomUnitForItem(cleanName, vendorId, cleanUnit)
            _statusMessages.tryEmit("تمت إضافة المقدار «$cleanUnit» لـ «$cleanName»")
        }
    }

    fun removeCustomUnitForItem(itemName: String, customUnit: String) {
        val cleanName = itemName.trim()
        val cleanUnit = customUnit.trim()
        if (cleanName.isEmpty() || cleanUnit.isEmpty()) return
        viewModelScope.launch {
            repository.removeCustomUnitForItem(cleanName, cleanUnit)
            _statusMessages.tryEmit("تم حذف المقدار «$cleanUnit» من «$cleanName»")
        }
    }

    fun quickAddFromDictionary(dictItem: DictionaryItemEntity) {
        toggleQuickDictionaryTile(dictItem, null)
    }

    fun updateInitialPocket(amountDirhams: Double) {
        val state = uiState.value
        val roundId = state.activeRound?.id ?: return
        val previousBudget = state.initialPocketDirhams
        if (kotlin.math.abs(previousBudget - amountDirhams) < 0.001) return

        viewModelScope.launch {
            repository.updateInitialPocket(roundId, amountDirhams)
            pushUndoAction(
                UndoAction.UpdatedPocketBudget(
                    roundId = roundId,
                    previousBudgetDirhams = previousBudget,
                    newBudgetDirhams = amountDirhams
                )
            )
        }
    }

    fun updateShoppingItem(item: ShoppingItemEntity, newName: String, newNote: String, newVendorId: Long) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        val previousSnapshot = item.copy()
        viewModelScope.launch {
            repository.updateShoppingItem(
                item.copy(
                    name = cleanName,
                    note = newNote.trim(),
                    vendorId = newVendorId
                )
            )
            draftState.value = draftState.value.copy(lastTouchedItemId = item.id)
            pushUndoAction(UndoAction.EditedShoppingItem(previousSnapshot, cleanName))
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.softDeleteShoppingItem(item.id)
            pushUndoAction(UndoAction.DeletedShoppingItem(item.id, item.name))
        }
    }

    fun addVendor(name: String, specialty: String, iconKey: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            val created = repository.addVendor(cleanName, specialty.trim(), iconKey)
            draftState.value = draftState.value.copy(selectedVendorIdForInput = created.id)
            _statusMessages.tryEmit("تمت إضافة البائع «$cleanName»")
        }
    }

    fun updateVendor(vendor: VendorEntity, newName: String, newSpecialty: String, newIconKey: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.updateVendor(
                vendor.copy(
                    name = cleanName,
                    specialty = newSpecialty.trim(),
                    iconKey = newIconKey
                )
            )
            _statusMessages.tryEmit("تم تحديث بيانات «$cleanName»")
        }
    }

    fun moveVendorOrder(vendorId: Long, moveUp: Boolean) {
        viewModelScope.launch {
            repository.moveVendorOrder(vendorId, moveUp)
        }
    }

    fun deleteVendor(vendor: VendorEntity) {
        viewModelScope.launch {
            repository.softDeleteVendor(vendor.id)
            pushUndoAction(UndoAction.DeletedVendor(vendor.id, vendor.name))
        }
    }

    fun addDictionaryEntry(name: String, vendorId: Long, defaultNote: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.addOrUpdateDictionaryEntry(cleanName, vendorId, defaultNote)
            _statusMessages.tryEmit("تم حفظ «$cleanName» في القاموس الذكي")
        }
    }

    fun renameDictionaryEntry(itemId: Long, newName: String, newVendorId: Long, newDefaultNote: String) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        val currentState = uiState.value
        val previousEntry = currentState.dictionaryItems.firstOrNull { it.id == itemId } ?: return
        val affectedShoppingItems = currentState.shoppingItems.filter {
            it.name.trim().equals(previousEntry.name.trim(), ignoreCase = true)
        }.map { it.copy() }

        viewModelScope.launch {
            repository.renameDictionaryEntry(itemId, cleanName, newVendorId, newDefaultNote)
            pushUndoAction(
                UndoAction.RenamedDictionaryItem(
                    previousEntry = previousEntry,
                    affectedItemsBeforeRename = affectedShoppingItems,
                    newName = cleanName
                )
            )
        }
    }

    fun mergeDictionaryEntries(source: DictionaryItemEntity, target: DictionaryItemEntity) {
        if (source.id == target.id) return
        val affectedShoppingItems = uiState.value.shoppingItems.filter {
            it.name.trim().equals(source.name.trim(), ignoreCase = true)
        }.map { it.copy() }

        viewModelScope.launch {
            repository.mergeDictionaryEntries(source.id, target.id)
            pushUndoAction(
                UndoAction.MergedDictionaryItems(
                    deletedSource = source.copy(),
                    previousTarget = target.copy(),
                    affectedItemsBeforeMerge = affectedShoppingItems
                )
            )
        }
    }

    fun deleteDictionaryEntry(entry: DictionaryItemEntity) {
        viewModelScope.launch {
            repository.deleteDictionaryEntry(entry)
            pushUndoAction(UndoAction.DeletedDictionaryItem(entry))
        }
    }

    fun performUndo(action: UndoAction) {
        undoExpiryJob?.cancel()
        undoStack.remove(action)
        val nextTopUndo = undoStack.lastOrNull()
        draftState.value = draftState.value.copy(activeUndo = nextTopUndo)

        viewModelScope.launch {
            when (action) {
                is UndoAction.DeletedShoppingItem -> {
                    repository.restoreShoppingItem(action.itemId)
                    _statusMessages.tryEmit("تم استرجاع الغرض «${action.itemName}» بنجاح")
                }

                is UndoAction.DeletedVendor -> {
                    repository.restoreVendor(action.vendorId)
                    _statusMessages.tryEmit("تم استرجاع البائع «${action.vendorName}» بنجاح")
                }

                is UndoAction.DeletedDictionaryItem -> {
                    repository.restoreDictionaryEntry(action.entry)
                    _statusMessages.tryEmit("تم استرجاع «${action.entry.name}» للقاموس بنجاح")
                }

                is UndoAction.UnmarkedPurchase -> {
                    repository.confirmItemPurchase(
                        item = action.previousSnapshot,
                        purchaseMode = action.previousSnapshot.purchaseMode,
                        unitPriceDirhams = action.previousSnapshot.unitPriceDirhams,
                        quantity = action.previousSnapshot.quantity,
                        cleaningFeeDirhams = action.previousSnapshot.cleaningFeeDirhams,
                        totalDirhams = action.previousSnapshot.totalDirhams
                    )
                    _statusMessages.tryEmit("تمت إعادة «${action.previousSnapshot.name}» إلى القفة")
                }

                is UndoAction.ConfirmedPurchase -> {
                    repository.revertConfirmedPurchase(action.previousSnapshot)
                    _statusMessages.tryEmit("تم التراجع عن شراء «${action.previousSnapshot.name}»")
                }

                is UndoAction.EditedShoppingItem -> {
                    repository.updateShoppingItem(action.previousSnapshot)
                    _statusMessages.tryEmit("تم التراجع عن تعديل «${action.previousSnapshot.name}»")
                }

                is UndoAction.UpdatedPocketBudget -> {
                    repository.updateInitialPocket(action.roundId, action.previousBudgetDirhams)
                    _statusMessages.tryEmit(
                        "تمت استعادة الميزانية السابقة (${CurrencyFormatter.formatDirhams(action.previousBudgetDirhams)})"
                    )
                }

                is UndoAction.MergedDictionaryItems -> {
                    repository.restoreDictionaryEntry(action.deletedSource)
                    repository.restoreDictionaryEntry(action.previousTarget)
                    action.affectedItemsBeforeMerge.forEach { oldItem ->
                        repository.updateShoppingItem(oldItem)
                    }
                    _statusMessages.tryEmit("تم التراجع عن دمج «${action.deletedSource.name}»")
                }

                is UndoAction.RenamedDictionaryItem -> {
                    repository.renameDictionaryEntry(
                        itemId = action.previousEntry.id,
                        newName = action.previousEntry.name,
                        newVendorId = action.previousEntry.defaultVendorId,
                        newDefaultNote = action.previousEntry.defaultNote
                    )
                    action.affectedItemsBeforeRename.forEach { oldItem ->
                        repository.updateShoppingItem(oldItem)
                    }
                    _statusMessages.tryEmit("تمت استعادة الاسم السابق «${action.previousEntry.name}»")
                }

                is UndoAction.ConfirmedVendorPayment -> {
                    repository.revertVendorPayment(
                        roundId = action.roundId,
                        vendorId = action.vendorId,
                        previousPayment = action.previousPayment,
                        previousVendorPendingBalanceDirhams = action.previousPendingBalanceDirhams
                    )
                    _statusMessages.tryEmit("تم التراجع عن تأكيد الدفع لـ «${action.vendorName}»")
                }

                is UndoAction.AdjustedVendorPendingBalance -> {
                    repository.updateVendorPendingBalance(
                        vendorId = action.vendorId,
                        pendingBalanceDirhams = action.previousBalanceDirhams
                    )
                    _statusMessages.tryEmit("تمت استعادة الصرف العالق السابق عند «${action.vendorName}»")
                }

                is UndoAction.AddedPassingExpense -> {
                    repository.softDeletePassingExpense(action.expenseId)
                    _statusMessages.tryEmit("تم إلغاء المصروف العابر «${action.title}»")
                }

                is UndoAction.DeletedPassingExpense -> {
                    repository.restorePassingExpense(action.expenseId)
                    _statusMessages.tryEmit("تم استرجاع المصروف العابر «${action.title}»")
                }

                is UndoAction.DeletedArchivedRound -> {
                    repository.restoreArchivedRound(action.round)
                    _statusMessages.tryEmit("تم استرجاع جولة «${action.round.title}» للأرشيف")
                }

                is UndoAction.DeletedPriceRecord -> {
                    repository.restorePriceHistoryRecord(action.record)
                    _statusMessages.tryEmit("تم استرجاع سجل ثمن «${action.record.itemName}»")
                }

                is UndoAction.ResetVendorPaymentAndBasket -> {
                    if (action.previousPayment != null) {
                        repository.revertVendorPayment(
                            roundId = action.roundId,
                            vendorId = action.vendorId,
                            previousPayment = action.previousPayment,
                            previousVendorPendingBalanceDirhams = action.previousPayment.recordedPendingChangeDirhams
                        )
                    }
                    action.previousPurchasedItems.forEach { prevItem ->
                        repository.confirmItemPurchase(
                            item = prevItem,
                            purchaseMode = prevItem.purchaseMode,
                            unitPriceDirhams = prevItem.unitPriceDirhams,
                            quantity = prevItem.quantity,
                            cleaningFeeDirhams = prevItem.cleaningFeeDirhams,
                            totalDirhams = prevItem.totalDirhams
                        )
                    }
                    _statusMessages.tryEmit("تمت استعادة حساب القفة والدفع عند «${action.vendorName}»")
                }
            }
        }
    }

    // --- Batch 5: Settings, Privacy Eye, PIN Lock & Trash Bin ---
    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            repository.updateAppSettings { it.copy(themeMode = themeMode) }
            val modeLabel = when (themeMode) {
                "DARK" -> "الوضع الليلي المريح"
                "SYSTEM" -> "التبديل التلقائي حسب الهاتف"
                else -> "الوضع النهاري الساطع للسوق"
            }
            _statusMessages.tryEmit("تم تفعيل $modeLabel")
        }
    }

    fun updateUiScaleMode(uiScaleMode: String) {
        viewModelScope.launch {
            repository.updateAppSettings { it.copy(uiScaleMode = uiScaleMode) }
            val scaleLabel = when (uiScaleMode) {
                "LARGE" -> "الحجم الكبير للسوق (112%)"
                "EXTRA_LARGE" -> "الحجم الضخم جداً (122%)"
                else -> "الحجم العادي الواضح (100%)"
            }
            _statusMessages.tryEmit("تم ضبط حجم الخط والأزرار: $scaleLabel")
        }
    }

    fun toggleShowRial() {
        viewModelScope.launch {
            val updated = repository.updateAppSettings { it.copy(showRial = !it.showRial) }
            CurrencyFormatter.showRialState = updated.showRial
            _statusMessages.tryEmit(
                if (updated.showRial) "تم إظهار حساب الريال (1 درهم = 20 ريال)"
                else "تم الاكتفاء بعرض الدرهم وإخفاء الريال"
            )
        }
    }

    fun togglePrivacyMask() {
        viewModelScope.launch {
            val updated = repository.updateAppSettings { it.copy(privacyMaskAmounts = !it.privacyMaskAmounts) }
            CurrencyFormatter.privacyMaskState = updated.privacyMaskAmounts
            CurrencyFormatter.privacyMaskPocketOnlyState = updated.privacyMaskPocketOnly
            _statusMessages.tryEmit(
                if (updated.privacyMaskAmounts) {
                    if (updated.privacyMaskPocketOnly) {
                        "عين الخصوصية مفعلة: تم إخفاء المبلغ في الجيب فقط بـ (***)"
                    } else {
                        "عين الخصوصية مفعلة: تم إخفاء كل المبالغ بـ (***)"
                    }
                } else {
                    "تم إظهار المبالغ المالية"
                }
            )
        }
    }

    fun updatePrivacyMaskPocketOnly(pocketOnly: Boolean) {
        viewModelScope.launch {
            val updated = repository.updateAppSettings {
                it.copy(
                    privacyMaskPocketOnly = pocketOnly,
                    privacyMaskAmounts = true
                )
            }
            CurrencyFormatter.privacyMaskState = updated.privacyMaskAmounts
            CurrencyFormatter.privacyMaskPocketOnlyState = updated.privacyMaskPocketOnly
            _statusMessages.tryEmit(
                if (pocketOnly) "تم ضبط عين الخصوصية على: إخفاء المبلغ في الجيب فقط"
                else "تم ضبط عين الخصوصية على: إخفاء كل المبالغ (الجيب والسلع)"
            )
        }
    }

    fun setOrUpdatePinCode(newPin: String) {
        val cleanPin = newPin.trim()
        if (cleanPin.length != 4 || !cleanPin.all { it.isDigit() }) return
        viewModelScope.launch {
            repository.updateAppSettings { it.copy(pinCode = cleanPin) }
            draftState.value = draftState.value.copy(
                isSessionPinUnlocked = true,
                isManualPinLockRequested = false
            )
            _statusMessages.tryEmit("تم حفظ وتفعيل الرمز السري (PIN) لحماية الكناش")
        }
    }

    fun removePinCode() {
        viewModelScope.launch {
            repository.updateAppSettings { it.copy(pinCode = "") }
            draftState.value = draftState.value.copy(
                isSessionPinUnlocked = false,
                isManualPinLockRequested = false
            )
            _statusMessages.tryEmit("تم إلغاء قفل الرمز السري (PIN)")
        }
    }

    fun unlockWithPin(enteredPin: String): Boolean {
        val currentPin = uiState.value.appSettings.pinCode
        return if (enteredPin == currentPin || currentPin.isBlank()) {
            draftState.value = draftState.value.copy(
                isSessionPinUnlocked = true,
                isManualPinLockRequested = false
            )
            true
        } else {
            false
        }
    }

    fun lockAppNow() {
        if (uiState.value.appSettings.pinCode.isNotBlank()) {
            draftState.value = draftState.value.copy(
                isSessionPinUnlocked = false,
                isManualPinLockRequested = true
            )
        }
    }

    // --- Trash Bin Recovery & Permanent Deletion ---
    fun restoreFromTrashShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.restoreShoppingItem(item.id)
            _statusMessages.tryEmit("تم استرجاع الغرض «${item.name}» من سلة المحذوفات")
        }
    }

    fun restoreFromTrashPassingExpense(expense: PassingExpenseEntity) {
        viewModelScope.launch {
            repository.restorePassingExpense(expense.id)
            _statusMessages.tryEmit("تم استرجاع المصروف العابر «${expense.title}» من سلة المحذوفات")
        }
    }

    fun restoreFromTrashVendor(vendor: VendorEntity) {
        viewModelScope.launch {
            repository.restoreVendor(vendor.id)
            _statusMessages.tryEmit("تم استرجاع البائع «${vendor.name}» من سلة المحذوفات")
        }
    }

    fun permanentlyDeleteFromTrashShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.permanentlyDeleteShoppingItem(item.id)
            _statusMessages.tryEmit("تم الحذف النهائي للغرض «${item.name}»")
        }
    }

    fun permanentlyDeleteFromTrashPassingExpense(expense: PassingExpenseEntity) {
        viewModelScope.launch {
            repository.permanentlyDeletePassingExpense(expense.id)
            _statusMessages.tryEmit("تم الحذف النهائي للمصروف «${expense.title}»")
        }
    }

    fun permanentlyDeleteFromTrashVendor(vendor: VendorEntity) {
        viewModelScope.launch {
            repository.permanentlyDeleteVendor(vendor.id)
            _statusMessages.tryEmit("تم الحذف النهائي للبائع «${vendor.name}»")
        }
    }

    fun emptyAllTrash() {
        viewModelScope.launch {
            repository.emptyAllTrash()
            _statusMessages.tryEmit("تم تفريغ سلة المحذوفات بالكامل")
        }
    }

    // --- Batch 6: Full Backup & Restore (النسخ الاحتياطي الشامل) ---
    suspend fun exportBackupJson(): String {
        return repository.exportFullBackupJson()
    }

    suspend fun importBackupJson(rawJson: String): Result<String> {
        val result = repository.importFullBackupJson(rawJson)
        return result.map { payload ->
            CurrencyFormatter.showRialState = payload.appSettings.showRial
            CurrencyFormatter.privacyMaskState = payload.appSettings.privacyMaskAmounts
            CurrencyFormatter.privacyMaskPocketOnlyState = payload.appSettings.privacyMaskPocketOnly
            "تم استرجاع النسخة الاحتياطية بنجاح (${payload.vendors.size} بائعين، ${payload.dictionaryItems.size} سلعة في القاموس، ${payload.shoppingItems.size} أغراض)"
        }
    }

    fun notifyStatusMessage(message: String) {
        _statusMessages.tryEmit(message)
    }

    class Factory(
        private val repository: TaqdiyaRepository,
        private val initialSettings: AppSettingsEntity = AppSettingsEntity()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomePrepViewModel(repository, initialSettings) as T
        }
    }
}


