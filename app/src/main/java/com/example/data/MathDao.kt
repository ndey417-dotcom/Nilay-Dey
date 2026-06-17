package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MathDao {
    // Notebook operations
    @Query("SELECT * FROM notebooks ORDER BY lastModified DESC")
    fun getAllNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE id = :id LIMIT 1")
    suspend fun getNotebookById(id: Long): Notebook?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: Notebook): Long

    @Update
    suspend fun updateNotebook(notebook: Notebook)

    @Delete
    suspend fun deleteNotebook(notebook: Notebook)

    // Page operations
    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY pageNumber ASC")
    fun getPagesForNotebook(notebookId: Long): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getPageByNumber(notebookId: Long, pageNumber: Int): Page?

    @Query("SELECT * FROM pages WHERE notebookId = :notebookId ORDER BY pageNumber ASC")
    suspend fun getPagesList(notebookId: Long): List<Page>

    @Query("SELECT COUNT(*) FROM pages WHERE notebookId = :notebookId")
    suspend fun getPageCount(notebookId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: Page): Long

    @Update
    suspend fun updatePage(page: Page)

    @Delete
    suspend fun deletePage(page: Page)

    @Query("DELETE FROM pages WHERE notebookId = :notebookId")
    suspend fun deletePagesForNotebook(notebookId: Long)
}
