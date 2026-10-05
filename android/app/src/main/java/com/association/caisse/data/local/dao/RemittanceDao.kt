package com.association.caisse.data.local.dao

import androidx.room.*
import com.association.caisse.data.local.entity.RemittanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemittanceDao {

    @Query("SELECT * FROM remittances ORDER BY date DESC, id DESC")
    fun getAllRemittances(): Flow<List<RemittanceEntity>>

    @Query("SELECT * FROM remittances ORDER BY date DESC, id DESC LIMIT 1")
    suspend fun getLastRemittance(): RemittanceEntity?

    @Query("SELECT * FROM remittances WHERE id = :id LIMIT 1")
    suspend fun getRemittanceById(id: Long): RemittanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemittance(remittance: RemittanceEntity): Long

    @Update
    suspend fun updateRemittance(remittance: RemittanceEntity)

    @Delete
    suspend fun deleteRemittance(remittance: RemittanceEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM remittances")
    fun getTotalRemittancesSum(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM remittances WHERE date LIKE :monthPrefix || '%'")
    fun getRemittancesSumForMonth(monthPrefix: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM remittances WHERE date LIKE :yearPrefix || '%'")
    fun getRemittancesSumForYear(yearPrefix: String): Flow<Long>
}
