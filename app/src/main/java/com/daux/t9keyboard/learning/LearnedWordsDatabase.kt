package com.daux.t9keyboard.learning

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Local-only store of the personal dictionary. Nothing ever leaves the device. */
@Database(entities = [LearnedWord::class], version = 1, exportSchema = false)
abstract class LearnedWordsDatabase : RoomDatabase() {

    abstract fun learnedWords(): LearnedWordDao

    companion object {
        private const val NAME = "learned_words.db"

        @Volatile
        private var instance: LearnedWordsDatabase? = null

        fun get(context: Context): LearnedWordsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LearnedWordsDatabase::class.java,
                    NAME
                ).build().also { instance = it }
            }
    }
}
