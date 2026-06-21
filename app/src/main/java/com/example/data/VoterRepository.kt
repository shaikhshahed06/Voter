package com.example.data

import kotlinx.coroutines.flow.Flow

class VoterRepository(private val voterDao: VoterDao) {
    val allVoters: Flow<List<Voter>> = voterDao.getAllVoters()

    fun getVoterById(id: Int): Flow<Voter?> = voterDao.getVoterById(id)

    suspend fun insertVoter(voter: Voter): Long = voterDao.insertVoter(voter)

    suspend fun updateVoter(voter: Voter) = voterDao.updateVoter(voter)

    suspend fun deleteVoter(voter: Voter) = voterDao.deleteVoter(voter)

    suspend fun deleteAllVoters() = voterDao.deleteAllVoters()
}
