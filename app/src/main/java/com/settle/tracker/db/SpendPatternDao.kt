package com.settle.tracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SpendPatternDao {
    @Insert
    suspend fun insertPattern(pattern: SpendPatternEntity)

    @Update
    suspend fun updatePattern(pattern: SpendPatternEntity)

    @Query("SELECT * FROM spend_patterns")
    suspend fun getAllPatterns(): List<SpendPatternEntity>

    @Query("SELECT * FROM spend_patterns WHERE id = :id LIMIT 1")
    suspend fun getPattern(id: String): SpendPatternEntity?

    @Insert
    suspend fun insertNegative(negative: SpendPatternNegativeEntity)

    @Query("SELECT * FROM spend_pattern_negatives WHERE patternId = :patternId")
    suspend fun getNegatives(patternId: String): List<SpendPatternNegativeEntity>

    @Insert
    suspend fun insertCandidate(candidate: PatternCandidateEntity)

    @Query("SELECT * FROM pattern_candidates WHERE label = :label AND category = :category")
    suspend fun getCandidates(label: String, category: String): List<PatternCandidateEntity>

    @Query("DELETE FROM pattern_candidates WHERE label = :label AND category = :category")
    suspend fun clearCandidates(label: String, category: String)
}
