package com.universe.remote.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe.remote.domain.ListeningRepository
import com.universe.remote.domain.LocalTrack
import com.universe.remote.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MoodViewModel @Inject constructor(private val repository: ListeningRepository) : ViewModel() {
    val tags = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val counts = repository.tagCounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val plays = repository.latestPlays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun tagsForTrack(id: String) = repository.tagsForTrack(id)
    fun noteForTrack(id: String) = repository.noteForTrack(id)
    fun toggle(track: LocalTrack, tagId: Long) = viewModelScope.launch {
        repository.ensureTrack(track)
        repository.toggleTag(track.id, tagId)
    }
    fun addTag(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.addTag(name.trim(), AppTheme.lime.value.toLong())
    }
    fun rename(id: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.renameTag(id, name)
    }
    fun recolor(id: Long, color: Long) = viewModelScope.launch { repository.recolorTag(id, color) }
    fun delete(id: Long) = viewModelScope.launch { repository.deleteTag(id) }
    fun saveNote(track: LocalTrack, text: String) = viewModelScope.launch {
        repository.ensureTrack(track)
        repository.saveNote(track.id, text)
    }
}
