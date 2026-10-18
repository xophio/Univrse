package com.universe.remote.ui.mood

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.universe.remote.domain.LocalTag
import com.universe.remote.domain.LocalTrack
import com.universe.remote.ui.components.Label
import com.universe.remote.ui.components.UniverseGlyph
import com.universe.remote.ui.components.UniverseIcon
import com.universe.remote.ui.theme.AppTheme

@Composable
fun TrackMoodChips(track: LocalTrack, model: MoodViewModel, modifier: Modifier = Modifier) {
    val selected by remember(track.id) { model.tagsForTrack(track.id) }.collectAsState(initial = emptyList())
    var editor by remember(track.id) { mutableStateOf(false) }
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppTheme.small),
        verticalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        selected.forEach { tag -> MoodChip(tag, onClick = { editor = true }) }
        Box(Modifier.height(AppTheme.tagChipHeight).background(AppTheme.surface, AppTheme.pill)
            .clickable(role = Role.Button, onClickLabel = "Add or edit mood tags") { editor = true }
            .padding(horizontal = AppTheme.small), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.tiny)) {
                UniverseIcon(UniverseGlyph.ADD, size = AppTheme.tagAddIcon,
                    color = AppTheme.lime, strokeWidth = AppTheme.tagAddStroke)
                Label("tag", AppTheme.caption.copy(color = AppTheme.lime))
            }
        }
    }
    if (editor) TagDialog(track, model, onDismiss = { editor = false })
}

@Composable
fun TagDialog(track: LocalTrack, model: MoodViewModel, onDismiss: () -> Unit) {
    val all by model.tags.collectAsStateWithLifecycle()
    val selected by remember(track.id) { model.tagsForTrack(track.id) }.collectAsState(initial = emptyList())
    var newName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(AppTheme.navSolid, AppTheme.card)
            .border(AppTheme.hairline, AppTheme.border, AppTheme.card)
            .verticalScroll(rememberScrollState()).padding(AppTheme.large),
            verticalArrangement = Arrangement.spacedBy(AppTheme.medium)) {
            Label("MOOD TAGS", AppTheme.eyebrow)
            BasicText(track.title, style = AppTheme.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            all.forEach { tag ->
                TagEditorRow(tag, selected.any { it.id == tag.id },
                    onToggle = { model.toggle(track, tag.id) },
                    onRename = { model.rename(tag.id, it) },
                    onRecolor = {
                        val palette = AppTheme.moodColors
                        val next = (palette.indexOfFirst { it.value.toLong() == tag.color } + 1).mod(palette.size)
                        model.recolor(tag.id, palette[next].value.toLong())
                    },
                    onDelete = { model.delete(tag.id) })
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
                Box(Modifier.weight(1f).background(AppTheme.surface, AppTheme.artShape)
                    .padding(AppTheme.medium)) {
                    if (newName.isEmpty()) Label("New tag", AppTheme.caption)
                    BasicTextField(newName, onValueChange = { newName = it.take(30) },
                        textStyle = AppTheme.body, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                Label("ADD", AppTheme.eyebrow, Modifier.clickable(role = Role.Button) {
                    model.addTag(newName); newName = ""
                }.padding(AppTheme.small))
            }
            Label("DONE", AppTheme.eyebrow, Modifier.align(Alignment.End)
                .clickable(role = Role.Button, onClick = onDismiss).padding(AppTheme.small))
        }
    }
}

@Composable
private fun TagEditorRow(tag: LocalTag, selected: Boolean, onToggle: () -> Unit,
    onRename: (String) -> Unit, onRecolor: () -> Unit, onDelete: () -> Unit) {
    var editing by remember(tag.id) { mutableStateOf(false) }
    var name by remember(tag.id) { mutableStateOf(tag.name) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        Box(Modifier.size(AppTheme.tagSwatch).background(Color(tag.color.toULong()), CircleShape)
            .clickable(role = Role.Button, onClickLabel = "Change color", onClick = onRecolor))
        if (editing) {
            BasicTextField(name, onValueChange = { name = it.take(30) }, singleLine = true,
                textStyle = AppTheme.body, modifier = Modifier.weight(1f))
            Label("SAVE", AppTheme.eyebrow, Modifier.clickable(role = Role.Button) {
                onRename(name); editing = false
            })
        } else {
            Box(Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = "Toggle tag",
                onClick = onToggle)) {
                Label((if (selected) "✓ " else "○ ") + tag.name,
                    AppTheme.body.copy(color = if (selected) AppTheme.lime else AppTheme.foreground))
            }
            Label("EDIT", AppTheme.caption, Modifier.clickable(role = Role.Button) { editing = true })
            Box(Modifier.size(AppTheme.touch).clickable(role = Role.Button,
                onClickLabel = "Delete tag", onClick = onDelete), contentAlignment = Alignment.Center) {
                UniverseIcon(UniverseGlyph.CLOSE, color = AppTheme.error)
            }
        }
    }
}

@Composable
private fun MoodChip(tag: LocalTag, onClick: () -> Unit) {
    Label(tag.name, AppTheme.caption.copy(color = AppTheme.ink),
        Modifier.background(Color(tag.color.toULong()), AppTheme.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.medium, vertical = AppTheme.small))
}

@Composable
fun TrackNote(track: LocalTrack, model: MoodViewModel) {
    val saved by remember(track.id) { model.noteForTrack(track.id) }.collectAsState(initial = null)
    var draft by remember(track.id) { mutableStateOf("") }
    LaunchedEffect(saved, track.id) { draft = saved.orEmpty() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.small)) {
        Label("NOTE", AppTheme.eyebrow)
        Box(Modifier.fillMaxWidth().heightIn(min = AppTheme.noteHeight)
            .background(AppTheme.surface, AppTheme.card).padding(AppTheme.medium)) {
            if (draft.isEmpty()) Label("Write a note about this track…", AppTheme.caption)
            BasicTextField(draft, onValueChange = { draft = it.take(1_000) },
                textStyle = AppTheme.body, modifier = Modifier.fillMaxWidth())
        }
        Label("SAVE NOTE", AppTheme.eyebrow, Modifier.align(Alignment.End)
            .clickable(role = Role.Button) { model.saveNote(track, draft) }.padding(AppTheme.small))
    }
}
