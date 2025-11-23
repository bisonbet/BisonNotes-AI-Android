package com.bisonnotesai.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisonnotesai.android.domain.model.Recording
import com.bisonnotesai.android.domain.model.Transcript
import com.bisonnotesai.android.domain.repository.RecordingRepository
import com.bisonnotesai.android.domain.repository.TranscriptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for transcripts screen
 * Manages transcript list, search, and filtering
 */
@HiltViewModel
class TranscriptsViewModel @Inject constructor(
    private val transcriptRepository: TranscriptRepository,
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    // All transcripts
    private val _transcripts = MutableStateFlow<List<TranscriptWithRecording>>(emptyList())
    val transcripts: StateFlow<List<TranscriptWithRecording>> = _transcripts.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered transcripts based on search
    val filteredTranscripts: StateFlow<List<TranscriptWithRecording>> = combine(
        _transcripts,
        _searchQuery
    ) { transcripts, query ->
        if (query.isBlank()) {
            transcripts
        } else {
            transcripts.filter { transcriptWithRecording ->
                val transcript = transcriptWithRecording.transcript
                val recording = transcriptWithRecording.recording

                // Search in transcript text, recording name
                transcript.fullText().contains(query, ignoreCase = true) ||
                recording.name.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Selected transcript for detail view
    private val _selectedTranscript = MutableStateFlow<TranscriptWithRecording?>(null)
    val selectedTranscript: StateFlow<TranscriptWithRecording?> = _selectedTranscript.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow<TranscriptUiState>(TranscriptUiState.Loading)
    val uiState: StateFlow<TranscriptUiState> = _uiState.asStateFlow()

    init {
        loadTranscripts()
    }

    /**
     * Load all transcripts with their recordings
     */
    private fun loadTranscripts() {
        viewModelScope.launch {
            _uiState.value = TranscriptUiState.Loading

            try {
                transcriptRepository.getAllTranscripts()
                    .collect { transcriptList ->
                        // Load recordings for each transcript
                        val transcriptsWithRecordings = transcriptList.mapNotNull { transcript ->
                            val recording = recordingRepository.getRecording(transcript.recordingId)
                            if (recording != null) {
                                TranscriptWithRecording(transcript, recording)
                            } else {
                                null
                            }
                        }

                        _transcripts.value = transcriptsWithRecordings
                        _uiState.value = if (transcriptsWithRecordings.isEmpty()) {
                            TranscriptUiState.Empty
                        } else {
                            TranscriptUiState.Success
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = TranscriptUiState.Error(
                    e.message ?: "Failed to load transcripts"
                )
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Clear search
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    /**
     * Select transcript for detail view
     */
    fun selectTranscript(transcriptWithRecording: TranscriptWithRecording) {
        _selectedTranscript.value = transcriptWithRecording
    }

    /**
     * Clear selected transcript
     */
    fun clearSelection() {
        _selectedTranscript.value = null
    }

    /**
     * Export transcript as text
     */
    fun exportAsText(transcript: Transcript): String {
        return buildString {
            appendLine("Transcript")
            appendLine("=" .repeat(50))
            appendLine()
            appendLine("Engine: ${transcript.engine.toDisplayString()}")
            appendLine("Confidence: ${(transcript.confidence * 100).toInt()}%")
            appendLine("Created: ${transcript.createdAt}")
            appendLine()
            appendLine("-" .repeat(50))
            appendLine()
            appendLine(transcript.formattedText())
        }
    }

    /**
     * Export transcript as markdown
     */
    fun exportAsMarkdown(transcript: Transcript, recordingName: String): String {
        return buildString {
            appendLine("# $recordingName - Transcript")
            appendLine()
            appendLine("**Engine:** ${transcript.engine.toDisplayString()}")
            appendLine("**Confidence:** ${(transcript.confidence * 100).toInt()}%")
            appendLine("**Created:** ${transcript.createdAt}")
            appendLine("**Word Count:** ${transcript.wordCount()}")
            appendLine()
            appendLine("---")
            appendLine()

            // Add formatted text with speaker labels
            transcript.segments.forEach { segment ->
                val speaker = segment.speaker?.let {
                    transcript.speakerMappings[it] ?: "Speaker $it"
                }

                if (speaker != null) {
                    appendLine("**$speaker:** ${segment.text}")
                } else {
                    appendLine(segment.text)
                }
                appendLine()
            }
        }
    }

    /**
     * Delete transcript
     */
    fun deleteTranscript(transcriptId: String) {
        viewModelScope.launch {
            transcriptRepository.deleteTranscript(transcriptId)
                .onFailure { error ->
                    _uiState.value = TranscriptUiState.Error(
                        error.message ?: "Failed to delete transcript"
                    )
                }
        }
    }
}

/**
 * Data class combining transcript with its recording
 */
data class TranscriptWithRecording(
    val transcript: Transcript,
    val recording: Recording
)

/**
 * UI state for transcripts screen
 */
sealed class TranscriptUiState {
    object Loading : TranscriptUiState()
    object Success : TranscriptUiState()
    object Empty : TranscriptUiState()
    data class Error(val message: String) : TranscriptUiState()
}
