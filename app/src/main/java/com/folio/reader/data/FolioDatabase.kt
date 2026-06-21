package com.folio.reader.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun statusToString(status: ReadStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): ReadStatus = ReadStatus.valueOf(value)
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN synopsis TEXT")
        db.execSQL("ALTER TABLE books ADD COLUMN collectionId TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS collections (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, createdAt INTEGER NOT NULL)",
        )
    }
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
