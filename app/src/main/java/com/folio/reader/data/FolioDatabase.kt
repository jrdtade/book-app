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
}

@Database(
    entities = [Book::class, Highlight::class, ReadingSession::class, Shelf::class, BookCollectionCrossRef::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
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
