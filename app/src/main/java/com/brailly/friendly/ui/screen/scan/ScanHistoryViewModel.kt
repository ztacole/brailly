package com.brailly.friendly.ui.screen.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.brailly.friendly.data.scan.ScanHistoryEntity
import com.brailly.friendly.data.scan.ScanHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Menyediakan riwayat pemindaian ke [ScanHistoryScreen].
 *
 * Halaman-halaman riwayat ditahan di `viewModelScope` lewat `cachedIn`, supaya
 * rotasi layar atau kembalinya pengguna dari layar lain tidak memaksa
 * pembacaan ulang dari basis data dan tidak melompatkan posisi gulir.
 */
class ScanHistoryViewModel(
    private val repository: ScanHistoryRepository,
) : ViewModel() {

    val entries: Flow<PagingData<ScanHistoryEntity>> =
        repository.pagedEntries().cachedIn(viewModelScope)

    val count: StateFlow<Int> = repository.observeCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = 0,
        )

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clear() }
    }

    private companion object {
        /**
         * Pengamatan jumlah entri dipertahankan sebentar setelah layar
         * ditinggalkan, agar perpindahan layar sekejap tidak memulai ulang
         * kueri Room.
         */
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
