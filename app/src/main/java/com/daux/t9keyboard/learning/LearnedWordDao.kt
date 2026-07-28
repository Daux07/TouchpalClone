package com.daux.t9keyboard.learning

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LearnedWordDao {

    /** The whole personal dictionary, loaded once at startup into RAM. */
    @Query("SELECT * FROM learned_words")
    fun all(): List<LearnedWord>

    /**
     * Persist a confirmation. Called with the use count already computed in RAM,
     * so the DB is only a mirror of the in-memory index — never read during typing.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(word: LearnedWord)

    @Query("DELETE FROM learned_words WHERE word = :word")
    fun delete(word: String)
}
