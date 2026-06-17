package com.example.data

import kotlinx.coroutines.flow.Flow

class MathRepository(private val mathDao: MathDao) {
    val allNotebooks: Flow<List<Notebook>> = mathDao.getAllNotebooks()

    suspend fun getNotebookById(id: Long): Notebook? = mathDao.getNotebookById(id)

    suspend fun createNotebook(name: String): Long {
        val notebook = Notebook(name = name, lastModified = System.currentTimeMillis())
        val notebookId = mathDao.insertNotebook(notebook)
        
        // Auto-create Page 1
        val firstPage = Page(notebookId = notebookId, pageNumber = 1, content = "")
        mathDao.insertPage(firstPage)
        
        return notebookId
    }

    suspend fun renameNotebook(id: Long, newName: String) {
        val existing = mathDao.getNotebookById(id)
        if (existing != null) {
            mathDao.updateNotebook(existing.copy(name = newName, lastModified = System.currentTimeMillis()))
        }
    }

    suspend fun deleteNotebook(notebook: Notebook) {
        mathDao.deletePagesForNotebook(notebook.id)
        mathDao.deleteNotebook(notebook)
    }

    fun getPagesForNotebook(notebookId: Long): Flow<List<Page>> {
        return mathDao.getPagesForNotebook(notebookId)
    }

    suspend fun getPageByNumber(notebookId: Long, pageNumber: Int): Page? {
        return mathDao.getPageByNumber(notebookId, pageNumber)
    }

    suspend fun addPage(notebookId: Long): Page {
        val count = mathDao.getPageCount(notebookId)
        val newPageNumber = count + 1
        val newPage = Page(notebookId = notebookId, pageNumber = newPageNumber, content = "")
        val id = mathDao.insertPage(newPage)
        return newPage.copy(id = id)
    }

    suspend fun savePage(page: Page) {
        mathDao.insertPage(page)
        val notebook = mathDao.getNotebookById(page.notebookId)
        if (notebook != null) {
            mathDao.updateNotebook(notebook.copy(lastModified = System.currentTimeMillis()))
        }
    }

    suspend fun deletePageAndReindex(page: Page) {
        // Delete targeted page
        mathDao.deletePage(page)
        
        // Retrieve remaining pages, and re-order page numbers to be continuously starting from 1
        val remaining = mathDao.getPagesList(page.notebookId)
        remaining.forEachIndexed { index, p ->
            val expectedPageNum = index + 1
            if (p.pageNumber != expectedPageNum) {
                mathDao.insertPage(p.copy(pageNumber = expectedPageNum))
            }
        }
        
        // Ensure there is always at least 1 page
        val currentCount = mathDao.getPageCount(page.notebookId)
        if (currentCount == 0) {
            val emptyFirstPage = Page(notebookId = page.notebookId, pageNumber = 1, content = "")
            mathDao.insertPage(emptyFirstPage)
        }
    }
}
