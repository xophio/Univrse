package com.universe.remote.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TrackStatRow(val trackId: String, val title: String, val artist: String,
    val listenedMs: Long, val plays: Int)
data class ArtistStatRow(val artist: String, val listenedMs: Long, val plays: Int)
data class TimeBucketRow(val bucket: Int, val listenedMs: Long)
data class MoodStatRow(val name: String, val color: Long, val listenedMs: Long)
data class PlayCountRow(val trackId: String, val plays: Int)

@Dao
interface StatsDao {
    @Query("SELECT COALESCE(SUM(listened_ms), 0) FROM play_logs WHERE started_at >= :since")
    fun totalSince(since: Long): Flow<Long>

    @Query("SELECT p.track_id AS trackId, t.title, t.artist, SUM(p.listened_ms) AS listenedMs, COUNT(*) AS plays FROM play_logs p JOIN tracks t ON t.id = p.track_id WHERE p.started_at >= :since GROUP BY p.track_id ORDER BY listenedMs DESC LIMIT 10")
    fun topTracks(since: Long): Flow<List<TrackStatRow>>

    @Query("SELECT t.artist, SUM(p.listened_ms) AS listenedMs, COUNT(*) AS plays FROM play_logs p JOIN tracks t ON t.id = p.track_id WHERE p.started_at >= :since GROUP BY t.artist ORDER BY listenedMs DESC LIMIT 5")
    fun topArtists(since: Long): Flow<List<ArtistStatRow>>

    @Query("SELECT CAST(strftime('%H', started_at / 1000, 'unixepoch', 'localtime') AS INTEGER) AS bucket, SUM(listened_ms) AS listenedMs FROM play_logs WHERE started_at >= :since GROUP BY bucket ORDER BY bucket")
    fun byHour(since: Long): Flow<List<TimeBucketRow>>

    @Query("SELECT CAST(strftime('%w', started_at / 1000, 'unixepoch', 'localtime') AS INTEGER) AS bucket, SUM(listened_ms) AS listenedMs FROM play_logs WHERE started_at >= :since GROUP BY bucket ORDER BY bucket")
    fun byDay(since: Long): Flow<List<TimeBucketRow>>

    @Query("SELECT tags.name, tags.color, SUM(p.listened_ms) AS listenedMs FROM play_logs p JOIN track_tags tt ON tt.track_id = p.track_id JOIN tags ON tags.id = tt.tag_id WHERE p.started_at >= :since GROUP BY tags.id ORDER BY listenedMs DESC")
    fun byMood(since: Long): Flow<List<MoodStatRow>>

    @Query("SELECT track_id AS trackId, COUNT(*) AS plays FROM play_logs GROUP BY track_id")
    fun playCounts(): Flow<List<PlayCountRow>>

    @Query("SELECT track_id FROM track_tags WHERE tag_id IN (:tagIds) GROUP BY track_id HAVING COUNT(DISTINCT tag_id) = :tagCount")
    fun tracksWithAllTags(tagIds: List<Long>, tagCount: Int): Flow<List<String>>
}
