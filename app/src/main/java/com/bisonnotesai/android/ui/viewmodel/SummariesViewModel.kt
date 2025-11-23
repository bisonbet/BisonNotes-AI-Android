package com.bisonnotesai.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisonnotesai.android.domain.model.Summary
import com.bisonnotesai.android.domain.repository.SummaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI summaries screen
 */
@HiltViewModel
class SummariesViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val summaries: StateFlow<List<Summary>> = summaryRepository.getAllSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Summaries are automatically loaded via Flow from repository
                // This is just for UI feedback
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSummaryById(id: String): StateFlow<Summary?> {
        return summaryRepository.getSummaryById(id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
    }

    fun deleteSummary(summaryId: String) {
        viewModelScope.launch {
            try {
                summaryRepository.deleteSummary(summaryId)
            } catch (e: Exception) {
                _error.value = "Failed to delete summary: ${e.message}"
            }
        }
    }
}
