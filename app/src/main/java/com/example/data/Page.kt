package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pages")
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val notebookId: Long,
    val pageNumber: Int, // 1-indexed
    val content: String = ""
)
