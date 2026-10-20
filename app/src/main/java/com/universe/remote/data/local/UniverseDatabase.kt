package com.universe.remote.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String?,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
)

@Entity(tableName = "play_logs", foreignKeys = [ForeignKey(
    entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"])],
    indices = [Index("track_id"), Index("started_at")])
data class PlayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "listened_ms") val listenedMs: Long = 0,
    val source: String,
    val completed: Boolean = false,
    val skipped: Boolean = false,
    @ColumnInfo(name = "last_observed_at") val lastObservedAt: Long,
    @ColumnInfo(name = "closed_at") val closedAt: Long? = null,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
)

@Entity(tableName = "track_tags", primaryKeys = ["track_id", "tag_id"],
    foreignKeys = [
        ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tag_id"], onDelete = ForeignKey.CASCADE),
    ], indices = [Index("tag_id")])
data class TrackTagEntity(
    @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

@Entity(tableName = "track_notes", foreignKeys = [ForeignKey(
    entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["track_id"], onDelete = ForeignKey.CASCADE)])
data class TrackNoteEntity(
    @PrimaryKey @ColumnInfo(name = "track_id") val trackId: String,
    val note: String,
)

@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey @ColumnInfo(name = "track_id") val trackId: String,
    @ColumnInfo(name = "synced_lyrics") val syncedLyrics: String?,
    @ColumnInfo(name = "plain_lyrics") val plainLyrics: String?,
    val instrumental: Boolean,
    @ColumnInfo(name = "lrclib_id") val lrclibId: Long?,
    val missing: Boolean,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Long,
)

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_cache WHERE track_id = :trackId LIMIT 1")
    suspend fun find(trackId: String): LyricsEntity?

    @Upsert
    suspend fun upsert(lyrics: LyricsEntity)
}

data class PlayHistoryRow(
    val id: Long,
    val title: String,
    val artist: String,
    val startedAt: Long,
    val listenedMs: Long,
    val source: String,
    val completed: Boolean,
    val skipped: Boolean,
)

data class TagCountRow(val name: String, val color: Long, val count: Int)

@Dao
interface HistoryDao {
    @Upsert suspend fun upsertTrack(track: TrackEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTrackIfMissing(track: TrackEntity)
    @Insert suspend fun insertPlay(log: PlayLogEntity): Long
    @Query("UPDATE play_logs SET listened_ms = :listened, completed = :completed, skipped = :skipped, last_observed_at = :seen, closed_at = :closed WHERE id = :id")
    suspend fun updatePlay(id: Long, listened: Long, completed: Boolean, skipped: Boolean, seen: Long, closed: Long?)
    @Query("SELECT * FROM play_logs WHERE track_id = :trackId AND closed_at IS NULL AND last_observed_at >= :cutoff ORDER BY id DESC LIMIT 1")
    suspend fun recentOpenPlay(trackId: String, cutoff: Long): PlayLogEntity?
    @Query("UPDATE play_logs SET closed_at = last_observed_at WHERE closed_at IS NULL AND last_observed_at < :cutoff")
    suspend fun closeStalePlays(cutoff: Long)
    @Query("SELECT p.id, t.title, t.artist, p.started_at AS startedAt, p.listened_ms AS listenedMs, p.source, p.completed, p.skipped FROM play_logs p JOIN tracks t ON p.track_id = t.id ORDER BY p.started_at DESC, p.id DESC LIMIT 50")
    fun latestPlays(): Flow<List<PlayHistoryRow>>
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun allTags(): Flow<List<TagEntity>>
    @Query("SELECT tags.* FROM tags INNER JOIN track_tags ON tags.id = track_tags.tag_id WHERE track_tags.track_id = :trackId ORDER BY tags.name COLLATE NOCASE")
    fun tagsForTrack(trackId: String): Flow<List<TagEntity>>
    @Query("SELECT tags.name, tags.color, COUNT(track_tags.track_id) AS count FROM tags LEFT JOIN track_tags ON tags.id = track_tags.tag_id GROUP BY tags.id ORDER BY count DESC, tags.name COLLATE NOCASE")
    fun tagCounts(): Flow<List<TagCountRow>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTag(tag: TagEntity): Long
    @Query("UPDATE tags SET name = :name WHERE id = :id")
    suspend fun renameTag(id: Long, name: String)
    @Query("UPDATE tags SET color = :color WHERE id = :id")
    suspend fun recolorTag(id: Long, color: Long)
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addTrackTag(link: TrackTagEntity)
    @Query("DELETE FROM track_tags WHERE track_id = :trackId AND tag_id = :tagId")
    suspend fun removeTrackTag(trackId: String, tagId: Long)
    @Query("SELECT EXISTS(SELECT 1 FROM track_tags WHERE track_id = :trackId AND tag_id = :tagId)")
    suspend fun hasTrackTag(trackId: String, tagId: Long): Boolean
    @Query("SELECT note FROM track_notes WHERE track_id = :trackId")
    fun noteForTrack(trackId: String): Flow<String?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertNote(note: TrackNoteEntity)
    @Query("DELETE FROM track_notes WHERE track_id = :trackId")
    suspend fun deleteNote(trackId: String)
}

@Database(entities = [TrackEntity::class, PlayLogEntity::class, TagEntity::class,
    TrackTagEntity::class, TrackNoteEntity::class, LyricsEntity::class], version = 2, exportSchema = true)
abstract class UniverseDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun tagDao(): TagDao
    abstract fun statsDao(): StatsDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `lyrics_cache` (`track_id` TEXT NOT NULL, `synced_lyrics` TEXT, `plain_lyrics` TEXT, `instrumental` INTEGER NOT NULL, `lrclib_id` INTEGER, `missing` INTEGER NOT NULL, `fetched_at` INTEGER NOT NULL, PRIMARY KEY(`track_id`))""")
            }
        }
    }
}
