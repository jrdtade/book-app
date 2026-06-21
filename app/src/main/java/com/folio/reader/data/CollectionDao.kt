package com.folio.reader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Shelf>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shelf: Shelf)

    @Delete
    suspend fun delete(shelf: Shelf)

    @Query("DELETE FROM book_collections WHERE collectionId = :collectionId")
    suspend fun clearMembers(collectionId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBook(crossRef: BookCollectionCrossRef)

    @Query("DELETE FROM book_collections WHERE bookId = :bookId AND collectionId = :collectionId")
    suspend fun removeBook(bookId: String, collectionId: String)

    @Query("SELECT collectionId FROM book_collections WHERE bookId = :bookId")
    fun observeCollectionIdsForBook(bookId: String): Flow<List<String>>

    @Query("SELECT bookId FROM book_collections WHERE collectionId = :collectionId")
    fun observeBookIdsInCollection(collectionId: String): Flow<List<String>>
}
