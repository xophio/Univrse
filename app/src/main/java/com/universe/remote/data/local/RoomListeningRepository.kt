package com.universe.remote.data.local

import com.universe.remote.domain.ListeningRepository
import com.universe.remote.domain.LocalTag
import com.universe.remote.domain.LocalTrack
import com.universe.remote.domain.PlayRecord
import com.universe.remote.domain.SavedPlay
import com.universe.remote.domain.TagCount
import com.universe.remote.domain.TrackStat
import com.universe.remote.domain.ArtistStat
import com.universe.remote.domain.TimeBucket
import com.universe.remote.domain.MoodStat
import com.universe.remote.domain.StatsSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

@Singleton
class RoomListeningRepository @Inject constructor(
    private val history: HistoryDao, private val tagDao: TagDao, private val stats: StatsDao,
) : ListeningRepository {
    override val latestPlays = history.latestPlays().map { rows ->
        rows.map { PlayRecord(it.id, it.title, it.artist, it.startedAt, it.listenedMs,
            it.source, it.completed, it.skipped) }
    }
    override val tags = tagDao.allTags().map { rows -> rows.map { LocalTag(it.id, it.name, it.color) } }
    override val tagCounts = tagDao.tagCounts().map { rows ->
        rows.map { TagCount(it.name, it.color, it.count) }
    }
    override fun tagsForTrack(trackId: String) = tagDao.tagsForTrack(trackId).map { rows ->
        rows.map { LocalTag(it.id, it.name, it.color) }
    }
    override fun noteForTrack(trackId: String) = tagDao.noteForTrack(trackId)
    override fun totalSince(since: Long) = stats.totalSince(since)
    override fun statsSince(since: Long) = combine(
        stats.totalSince(since), stats.topTracks(since), stats.topArtists(since),
        stats.byHour(since), stats.byDay(since), stats.byMood(since),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        StatsSnapshot(
            totalMs = values[0] as Long,
            tracks = (values[1] as List<TrackStatRow>).map {
                TrackStat(it.trackId, it.title, it.artist, it.listenedMs, it.plays) },
            artists = (values[2] as List<ArtistStatRow>).map {
                ArtistStat(it.artist, it.listenedMs, it.plays) },
            hours = (values[3] as List<TimeBucketRow>).map { TimeBucket(it.bucket, it.listenedMs) },
            days = (values[4] as List<TimeBucketRow>).map { TimeBucket(it.bucket, it.listenedMs) },
            moods = (values[5] as List<MoodStatRow>).map { MoodStat(it.name, it.color, it.listenedMs) },
        )
    }.flowOn(Dispatchers.Default)
    override fun playCounts() = stats.playCounts().map { rows -> rows.associate { it.trackId to it.plays } }
        .flowOn(Dispatchers.Default)
    override fun tracksWithAllTags(tagIds: List<Long>) =
        if (tagIds.isEmpty()) flowOf(emptySet())
        else stats.tracksWithAllTags(tagIds, tagIds.size).map { it.toSet() }
    override suspend fun saveTrack(track: LocalTrack) = history.upsertTrack(TrackEntity(
        track.id, track.title, track.artist, track.album, track.coverUrl, track.durationMs))
    override suspend fun ensureTrack(track: LocalTrack) = history.insertTrackIfMissing(TrackEntity(
        track.id, track.title, track.artist, track.album, track.coverUrl, track.durationMs))
    override suspend fun startPlay(trackId: String, startedAt: Long, source: String) =
        history.insertPlay(PlayLogEntity(trackId = trackId, startedAt = startedAt,
            source = source, lastObservedAt = startedAt))
    override suspend fun recentOpenPlay(trackId: String, cutoff: Long) =
        history.recentOpenPlay(trackId, cutoff)?.let {
            SavedPlay(it.id, it.listenedMs, it.startedAt, it.lastObservedAt, it.source, it.completed)
        }
    override suspend fun closeStalePlays(cutoff: Long) = history.closeStalePlays(cutoff)
    override suspend fun updatePlay(id: Long, listenedMs: Long, completed: Boolean, skipped: Boolean,
        observedAt: Long, closedAt: Long?) =
        history.updatePlay(id, listenedMs, completed, skipped, observedAt, closedAt)
    override suspend fun addTag(name: String, color: Long) =
        tagDao.insertTag(TagEntity(name = name.trim(), color = color))
    override suspend fun renameTag(id: Long, name: String) = tagDao.renameTag(id, name.trim())
    override suspend fun recolorTag(id: Long, color: Long) = tagDao.recolorTag(id, color)
    override suspend fun deleteTag(id: Long) = tagDao.deleteTag(id)
    override suspend fun toggleTag(trackId: String, tagId: Long) {
        if (tagDao.hasTrackTag(trackId, tagId)) tagDao.removeTrackTag(trackId, tagId)
        else tagDao.addTrackTag(TrackTagEntity(trackId, tagId))
    }
    override suspend fun saveNote(trackId: String, text: String) {
        if (text.isBlank()) tagDao.deleteNote(trackId)
        else tagDao.upsertNote(TrackNoteEntity(trackId, text.trim()))
    }
}
