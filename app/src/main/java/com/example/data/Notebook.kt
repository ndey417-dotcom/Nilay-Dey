package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val lastModified: Long = System.currentTimeMillis()
)
