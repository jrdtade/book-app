package com.folio.reader.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun statusToString(status: ReadStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): ReadStatus = ReadStatus.valueOf(value)

    @TypeConverter
    fun mediaTypeToString(mediaType: MediaType): String = mediaType.name

    @TypeConverter
    fun stringToMediaType(value: String): MediaType = MediaType.valueOf(value)
}

@Database(
    entities = [Book::class, Highlight::class, Bookmark::class, ReadingSession::class, Shelf::class, BookCollectionCrossRef::class],
    version = 6,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun sessionDao(): SessionDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        @Volatile private var instance: FolioDatabase? = null

        fun get(context: Context): FolioDatabase = instance ?: synchronized(this) {
            instance ?: androidx.room.Room.databaseBuilder(
                context.applicationContext,
                FolioDatabase::class.java,
                "folio.db",
            )
                // Pre-release schema churn (synopsis cache, collections); no installs to preserve yet.
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
        }
    }
}
