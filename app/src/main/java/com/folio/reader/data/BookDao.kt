package com.folio.reader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun get(id: String): Book?

    @Query("SELECT * FROM books WHERE id = :id")
    fun observe(id: String): Flow<Book?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: Book)

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Highlight>>

    @Insert
    suspend fun insert(highlight: Highlight)

    @Delete
    suspend fun delete(highlight: Highlight)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: ReadingSession)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<ReadingSession>>

    @Query("SELECT COALESCE(SUM(durationMillis),0) FROM sessions WHERE day = :day")
    suspend fun totalForDay(day: String): Long
}
