package az.pdr.taqdiya2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import az.pdr.taqdiya2.data.local.DictionaryItemEntity
import az.pdr.taqdiya2.data.local.ShoppingItemEntity
import az.pdr.taqdiya2.data.local.ShoppingRoundEntity
import az.pdr.taqdiya2.data.local.VendorEntity
import az.pdr.taqdiya2.data.repository.TaqdiyaRepository
import az.pdr.taqdiya2.util.CurrencyFormatter
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
        UndoAction("تم حذف الغرض «$itemName»")

    data class DeletedVendor(val vendorId: Long, val vendorName: String) :
        UndoAction("تم حذف البائع «$vendorName»")

    data class DeletedDictionaryItem(val entry: DictionaryItemEntity) :
        UndoAction("تم حذف «${entry.name}» من القاموس")
}

data class HomePrepUiState(
    val isLoading: Boolean = true,
    val activeRound: ShoppingRoundEntity? = null,
    val vendors: List<VendorEntity> = emptyList(),
    val shoppingItems: List<ShoppingItemEntity> = emptyList(),
    val dictionaryItems: List<DictionaryItemEntity> = emptyList(),
    val itemNameInput: String = "",
    val itemNoteInput: String = "",
    val selectedVendorIdForInput: Long? = null,
    val autoMatchedVendorName: String? = null,
    val searchQuery: String = "",
    val filterVendorId: Long? = null,
    val lastTouchedItemId: Long? = null,
    val selectedMarketVendorId: Long? = null,
    val keepScreenAwakeInMarket: Boolean = true
) {
    val initialPocketDirhams: Double
        get() = activeRound?.initialPocketDirhams ?: 200.0

    val totalPurchasedDirhams: Double
        get() = shoppingItems.filter { it.isPurchased && !it.isDeleted }.sumOf { it.totalDirhams }

    val remainingPocketDirhams: Double
        get() = (initialPocketDirhams - totalPurchasedDirhams).coerceAtLeast(0.0)
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
    val keepScreenAwakeInMarket: Boolean = true
)

class HomePrepViewModel(
    private val repository: TaqdiyaRepository
) : ViewModel() {

    private val draftState = MutableStateFlow(InputDraftState())

    private val _undoEvents = MutableSharedFlow<UndoAction>(extraBufferCapacity = 1)
    val undoEvents: SharedFlow<UndoAction> = _undoEvents.asSharedFlow()

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val statusMessages: SharedFlow<String> = _statusMessages.asSharedFlow()

    val uiState: StateFlow<HomePrepUiState> = combine(
        repository.activeRound,
        repository.activeVendors,
        repository.allActiveItems,
        repository.dictionaryItems,
        draftState
    ) { round, vendors, allItems, dictItems, draft ->
        val roundItems = if (round != null) {
            allItems.filter { it.roundId == round.id && !it.isDeleted }
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
            roundItems.any { it.id == id }
        } ?: roundItems.firstOrNull()?.id

        // Active market vendors (vendors who have at least 1 item in this round)
        val vendorsWithItemsIds = roundItems.map { it.vendorId }.toSet()
        val marketVendors = vendors.filter { vendorsWithItemsIds.contains(it.id) }
        val resolvedMarketVendorId = when {
            draft.selectedMarketVendorId != null && marketVendors.any { it.id == draft.selectedMarketVendorId } ->
                draft.selectedMarketVendorId
            marketVendors.isNotEmpty() -> {
                // Prefer first vendor with unpurchased items, otherwise first market vendor
                marketVendors.firstOrNull { v ->
                    roundItems.any { it.vendorId == v.id && !it.isPurchased }
                }?.id ?: marketVendors.first().id
            }
            else -> vendors.firstOrNull()?.id
        }

        HomePrepUiState(
            isLoading = false,
            activeRound = round,
            vendors = vendors,
            shoppingItems = roundItems,
            dictionaryItems = dictItems,
            itemNameInput = draft.itemNameInput,
            itemNoteInput = draft.itemNoteInput,
            selectedVendorIdForInput = resolvedVendorId,
            autoMatchedVendorName = draft.autoMatchedVendorName,
            searchQuery = draft.searchQuery,
            filterVendorId = draft.filterVendorId,
            lastTouchedItemId = validLastTouchedId,
            selectedMarketVendorId = resolvedMarketVendorId,
            keepScreenAwakeInMarket = draft.keepScreenAwakeInMarket
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomePrepUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
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
        viewModelScope.launch {
            repository.confirmItemPurchase(
                item = item,
                purchaseMode = purchaseMode,
                unitPriceDirhams = unitPriceDirhams,
                quantity = quantity,
                cleaningFeeDirhams = cleaningFeeDirhams,
                totalDirhams = totalDirhams
            )
            _statusMessages.tryEmit(
                "تم شراء «${item.name}» بـ ${CurrencyFormatter.formatDirhams(totalDirhams)} (${CurrencyFormatter.formatRials(totalDirhams)})"
            )
        }
    }

    fun unmarkItemPurchased(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.unmarkItemPurchased(item)
            _statusMessages.tryEmit("تمت إعادة «${item.name}» إلى قائمة الانتظار")
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
        val vendorId = state.selectedVendorIdForInput ?: state.vendors.firstOrNull()?.id ?: return

        viewModelScope.launch {
            val created = repository.addShoppingItemAndLearn(
                roundId = roundId,
                name = name,
                note = state.itemNoteInput.trim(),
                vendorId = vendorId
            )
            draftState.value = draftState.value.copy(
                itemNameInput = "",
                itemNoteInput = "",
                autoMatchedVendorName = null,
                isVendorManuallyOverridden = false,
                lastTouchedItemId = created.id,
                selectedMarketVendorId = vendorId
            )
        }
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
                _undoEvents.tryEmit(UndoAction.DeletedShoppingItem(existingItem.id, existingItem.name))
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
        val targetId = state.lastTouchedItemId ?: state.shoppingItems.firstOrNull()?.id ?: return
        val targetItem = state.shoppingItems.firstOrNull { it.id == targetId } ?: return
        viewModelScope.launch {
            val newNote = if (targetItem.note == noteText) "" else noteText
            repository.updateShoppingItem(targetItem.copy(note = newNote))
        }
    }

    fun quickAddFromDictionary(dictItem: DictionaryItemEntity) {
        toggleQuickDictionaryTile(dictItem, null)
    }

    fun updateInitialPocket(amountDirhams: Double) {
        val roundId = uiState.value.activeRound?.id ?: return
        viewModelScope.launch {
            repository.updateInitialPocket(roundId, amountDirhams)
            _statusMessages.tryEmit("تم تحديث ميزانية الجيب بنجاح")
        }
    }

    fun updateShoppingItem(item: ShoppingItemEntity, newName: String, newNote: String, newVendorId: Long) {
        val cleanName = newName.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.updateShoppingItem(
                item.copy(
                    name = cleanName,
                    note = newNote.trim(),
                    vendorId = newVendorId
                )
            )
            draftState.value = draftState.value.copy(lastTouchedItemId = item.id)
            _statusMessages.tryEmit("تم تعديل الغرض «$cleanName»")
        }
    }

    fun deleteShoppingItem(item: ShoppingItemEntity) {
        viewModelScope.launch {
            repository.softDeleteShoppingItem(item.id)
            _undoEvents.tryEmit(UndoAction.DeletedShoppingItem(item.id, item.name))
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
            _undoEvents.tryEmit(UndoAction.DeletedVendor(vendor.id, vendor.name))
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
        viewModelScope.launch {
            repository.renameDictionaryEntry(itemId, cleanName, newVendorId, newDefaultNote)
            _statusMessages.tryEmit("تم تحديث «$cleanName» في القاموس والقائمة")
        }
    }

    fun mergeDictionaryEntries(source: DictionaryItemEntity, target: DictionaryItemEntity) {
        if (source.id == target.id) return
        viewModelScope.launch {
            repository.mergeDictionaryEntries(source.id, target.id)
            _statusMessages.tryEmit("تم دمج «${source.name}» داخل «${target.name}» بنجاح")
        }
    }

    fun deleteDictionaryEntry(entry: DictionaryItemEntity) {
        viewModelScope.launch {
            repository.deleteDictionaryEntry(entry)
            _undoEvents.tryEmit(UndoAction.DeletedDictionaryItem(entry))
        }
    }

    fun performUndo(action: UndoAction) {
        viewModelScope.launch {
            when (action) {
                is UndoAction.DeletedShoppingItem -> {
                    repository.restoreShoppingItem(action.itemId)
                    _statusMessages.tryEmit("تم استرجاع الغرض «${action.itemName}»")
                }
                is UndoAction.DeletedVendor -> {
                    repository.restoreVendor(action.vendorId)
                    _statusMessages.tryEmit("تم استرجاع البائع «${action.vendorName}»")
                }
                is UndoAction.DeletedDictionaryItem -> {
                    repository.restoreDictionaryEntry(action.entry)
                    _statusMessages.tryEmit("تم استرجاع «${action.entry.name}» للقاموس")
                }
            }
        }
    }

    class Factory(private val repository: TaqdiyaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomePrepViewModel(repository) as T
        }
    }
}
