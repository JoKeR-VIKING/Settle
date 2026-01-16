package com.settle.tracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDraftDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(draft: ExpenseEntity)

    @Query("SELECT * FROM expense_drafts ORDER BY timestamp DESC LIMIT 50")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_drafts WHERE id = :expenseId LIMIT 1")
    fun getOne(expenseId: String): Flow<ExpenseEntity?>

    @Query("DELETE FROM expense_drafts WHERE id = :id")
    suspend fun delete(id: String)
}
