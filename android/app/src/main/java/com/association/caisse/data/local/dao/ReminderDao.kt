package com.association.caisse.data.local.dao

import androidx.room.*
import com.association.caisse.data.local.entity.ReminderLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminder_logs ORDER BY date DESC, id DESC")
    fun getAllReminderLogs(): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_logs WHERE memberId = :memberId ORDER BY date DESC, id DESC")
    fun getReminderLogsForMember(memberId: Long): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_logs WHERE memberId = :memberId ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLastReminderForMember(memberId: Long): ReminderLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderLog(log: ReminderLogEntity): Long

    @Delete
    suspend fun deleteReminderLog(log: ReminderLogEntity)

    @Query("DELETE FROM reminder_logs WHERE id = :id")
    suspend fun deleteReminderLogById(id: Long)
}
