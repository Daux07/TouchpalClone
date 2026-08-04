package com.daux.t9keyboard.learning

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Local-only store of the personal dictionary. Nothing ever leaves the device. */
@Database(entities = [LearnedWord::class], version = 2, exportSchema = false)
abstract class LearnedWordsDatabase : RoomDatabase() {

    abstract fun learnedWords(): LearnedWordDao

    companion object {
        private const val NAME = "learned_words.db"

        /**
         * Version 2 adds `display`, the written form of a word whose capitals no rule
         * could have produced (Step 3.6).
         *
         * **Written as a migration and not as `fallbackToDestructiveMigration`**, which
         * would have been one line. That line would delete the personal dictionary of
         * everyone who already had one — the only data in this app the user cannot get
         * back, because they built it by typing. Existing rows get `NULL`, which is
         * exactly right: they were learned before capitals could be remembered, so
         * nothing is known about how they are written.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE learned_words ADD COLUMN display TEXT")
            }
        }

        @Volatile
        private var instance: LearnedWordsDatabase? = null

        fun get(context: Context): LearnedWordsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LearnedWordsDatabase::class.java,
                    NAME
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
