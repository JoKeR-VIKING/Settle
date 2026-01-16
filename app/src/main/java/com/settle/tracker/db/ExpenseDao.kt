package com.settle.tracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDraftDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(draft: ExpenseEntity)

    @Query("SELECT * FROM expense_drafts ORDER BY createdAt DESC LIMIT 50")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Query("DELETE FROM expense_drafts WHERE id = :id")
    suspend fun delete(id: String)
}
