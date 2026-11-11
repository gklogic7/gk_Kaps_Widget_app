package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

class KapsConverters {
    @TypeConverter
    fun fromMediaFormat(value: MediaFormat): String = value.name

    @TypeConverter
    fun toMediaFormat(value: String): MediaFormat =
        runCatching { MediaFormat.valueOf(value) }.getOrDefault(MediaFormat.PNG)

    @TypeConverter
    fun fromSizePreset(value: SizePreset): String = value.name

    @TypeConverter
    fun toSizePreset(value: String): SizePreset =
        runCatching { SizePreset.valueOf(value) }.getOrDefault(SizePreset.MEDIUM)

    @TypeConverter
    fun fromScaleMode(value: ScaleMode): String = value.name

    @TypeConverter
    fun toScaleMode(value: String): ScaleMode =
        runCatching { ScaleMode.valueOf(value) }.getOrDefault(ScaleMode.FIT_INSIDE)

    @TypeConverter
    fun fromBackgroundMode(value: BackgroundMode): String = value.name

    @TypeConverter
    fun toBackgroundMode(value: String): BackgroundMode =
        runCatching { BackgroundMode.valueOf(value) }.getOrDefault(BackgroundMode.TRANSPARENT)

    @TypeConverter
    fun fromLoopMode(value: LoopMode): String = value.name

    @TypeConverter
    fun toLoopMode(value: String): LoopMode =
        runCatching { LoopMode.valueOf(value) }.getOrDefault(LoopMode.CONTINUOUS)
}

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widget_configs ORDER BY isPlacedOnHomeScreen DESC, updatedAt DESC")
    fun observeAllWidgets(): Flow<List<WidgetConfigEntity>>

    @Query("SELECT * FROM widget_configs ORDER BY isPlacedOnHomeScreen DESC, updatedAt DESC")
    suspend fun getAllWidgetsSnapshot(): List<WidgetConfigEntity>

    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId LIMIT 1")
    suspend fun getWidgetById(widgetId: Int): WidgetConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidget(config: WidgetConfigEntity)

    @Update
    suspend fun updateWidget(config: WidgetConfigEntity)

    @Query("DELETE FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun deleteWidgetById(widgetId: Int)

    @Query("SELECT MIN(widgetId) FROM widget_configs")
    suspend fun getMinWidgetId(): Int?
}

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY isBuiltInSample ASC, importedAt DESC")
    fun observeAllMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items ORDER BY isBuiltInSample ASC, importedAt DESC")
    suspend fun getAllMediaSnapshot(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(item: MediaItemEntity): Long

    @Update
    suspend fun updateMedia(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaById(id: Long)
}

@Database(
    entities = [WidgetConfigEntity::class, MediaItemEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(KapsConverters::class)
abstract class WidgetDatabase : RoomDatabase() {
    abstract fun widgetDao(): WidgetDao
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: WidgetDatabase? = null

        fun getInstance(context: Context): WidgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WidgetDatabase::class.java,
                    "kaps_widget_studio.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
