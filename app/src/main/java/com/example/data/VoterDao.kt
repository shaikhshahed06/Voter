package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VoterDao {
    @Query("SELECT * FROM voters ORDER BY fullName ASC")
    fun getAllVoters(): Flow<List<Voter>>

    @Query("SELECT * FROM voters WHERE id = :id LIMIT 1")
    fun getVoterById(id: Int): Flow<Voter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoter(voter: Voter): Long

    @Update
    suspend fun updateVoter(voter: Voter)

    @Delete
    suspend fun deleteVoter(voter: Voter)

    @Query("DELETE FROM voters")
    suspend fun deleteAllVoters()
}
