package com.universe.remote.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.remote.domain.ListeningRepository
import com.universe.remote.domain.StatsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class StatsRange(val label: String, val days: Long) {
    WEEK("7 days", 7), MONTH("30 days", 30), ALL("All time", 0);
    fun since(now: Long = System.currentTimeMillis()) =
        if (days == 0L) 0L else now - days * 24 * 60 * 60 * 1_000
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel @Inject constructor(repository: ListeningRepository) : ViewModel() {
    private val _range = MutableStateFlow(StatsRange.WEEK)
    val range = _range.asStateFlow()
    val snapshot = range.flatMapLatest { repository.statsSince(it.since()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsSnapshot())
    val totals = combine(
        repository.totalSince(StatsRange.WEEK.since()),
        repository.totalSince(StatsRange.MONTH.since()),
        repository.totalSince(0),
    ) { week, month, all -> listOf(week, month, all) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(0L, 0L, 0L))
    fun select(range: StatsRange) { _range.value = range }
}
