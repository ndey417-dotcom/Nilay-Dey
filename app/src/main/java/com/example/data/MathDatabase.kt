package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Notebook::class, Page::class], version = 1, exportSchema = false)
abstract class MathDatabase : RoomDatabase() {
    abstract fun mathDao(): MathDao

    companion object {
        @Volatile
        private var INSTANCE: MathDatabase? = null

        fun getDatabase(context: Context): MathDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MathDatabase::class.java,
                    "math_notebook_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
