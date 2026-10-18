package com.universe.remote.domain

import kotlinx.coroutines.flow.Flow

data class LocalTrack(
    val id: String, val title: String, val artist: String, val album: String,
    val coverUrl: String?, val durationMs: Long,
)
data class LocalTag(val id: Long, val name: String, val color: Long)
data class PlayRecord(
    val id: Long, val title: String, val artist: String, val startedAt: Long,
    val listenedMs: Long, val source: String, val completed: Boolean, val skipped: Boolean,
)
data class TagCount(val name: String, val color: Long, val count: Int)
data class SavedPlay(
    val id: Long, val listenedMs: Long, val startedAt: Long, val lastObservedAt: Long,
    val source: String, val completed: Boolean,
)
data class TrackStat(val trackId: String, val title: String, val artist: String,
    val listenedMs: Long, val plays: Int)
data class ArtistStat(val artist: String, val listenedMs: Long, val plays: Int)
data class TimeBucket(val bucket: Int, val listenedMs: Long)
data class MoodStat(val name: String, val color: Long, val listenedMs: Long)
data class StatsSnapshot(
    val totalMs: Long = 0,
    val tracks: List<TrackStat> = emptyList(),
    val artists: List<ArtistStat> = emptyList(),
    val hours: List<TimeBucket> = emptyList(),
    val days: List<TimeBucket> = emptyList(),
    val moods: List<MoodStat> = emptyList(),
)

interface ListeningRepository {
    val latestPlays: Flow<List<PlayRecord>>
    val tags: Flow<List<LocalTag>>
    val tagCounts: Flow<List<TagCount>>
    fun tagsForTrack(trackId: String): Flow<List<LocalTag>>
    fun noteForTrack(trackId: String): Flow<String?>
    fun totalSince(since: Long): Flow<Long>
    fun statsSince(since: Long): Flow<StatsSnapshot>
    fun playCounts(): Flow<Map<String, Int>>
    fun tracksWithAllTags(tagIds: List<Long>): Flow<Set<String>>
    suspend fun saveTrack(track: LocalTrack)
    suspend fun ensureTrack(track: LocalTrack)
    suspend fun startPlay(trackId: String, startedAt: Long, source: String): Long
    suspend fun recentOpenPlay(trackId: String, cutoff: Long): SavedPlay?
    suspend fun closeStalePlays(cutoff: Long)
    suspend fun updatePlay(id: Long, listenedMs: Long, completed: Boolean, skipped: Boolean,
        observedAt: Long, closedAt: Long?)
    suspend fun addTag(name: String, color: Long): Long
    suspend fun renameTag(id: Long, name: String)
    suspend fun recolorTag(id: Long, color: Long)
    suspend fun deleteTag(id: Long)
    suspend fun toggleTag(trackId: String, tagId: Long)
    suspend fun saveNote(trackId: String, text: String)
}
