package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Voter
import com.example.data.VoterRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VoterViewModel(
    application: Application,
    private val repository: VoterRepository
) : AndroidViewModel(application) {

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedPartyFilter = MutableStateFlow("All")
    val selectedStatusFilter = MutableStateFlow("All")
    val selectedLeaningFilter = MutableStateFlow("All")
    val selectedWardFilter = MutableStateFlow("All")

    // Master stream of voters
    val allVoters: StateFlow<List<Voter>> = repository.allVoters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered stream of voters
    val filteredVoters: StateFlow<List<Voter>> = combine(
        allVoters,
        searchQuery,
        selectedPartyFilter,
        selectedStatusFilter,
        selectedLeaningFilter,
        selectedWardFilter
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val voters = array[0] as List<Voter>
        val query = array[1] as String
        val party = array[2] as String
        val status = array[3] as String
        val leaning = array[4] as String
        val ward = array[5] as String

        voters.filter { voter ->
            val matchesQuery = voter.fullName.contains(query, ignoreCase = true) ||
                    voter.address.contains(query, ignoreCase = true) ||
                    voter.phone.contains(query) ||
                    voter.notes.contains(query, ignoreCase = true)

            val matchesParty = party == "All" || voter.party == party
            val matchesStatus = status == "All" || voter.registrationStatus == status
            val matchesLeaning = leaning == "All" || voter.supportLeaning == leaning
            val matchesWard = ward == "All" || voter.ward.equals(ward, ignoreCase = true)

            matchesQuery && matchesParty && matchesStatus && matchesLeaning && matchesWard
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic list of unique Wards/Precincts collected, for filtering selection dropdowns
    val availableWards: StateFlow<List<String>> = allVoters
        .map { list ->
            list.map { it.ward.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Statistics state calculated directly from the master list of voters
    val statsState: StateFlow<VoterStats> = allVoters
        .map { list ->
            calculateStats(list)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VoterStats()
        )

    init {
        // Feed in beautiful, high-fidelity mock records on first run if database is empty!
        viewModelScope.launch {
            allVoters.collectLatest { list ->
                if (list.isEmpty()) {
                    prepopulateDatabase()
                }
            }
        }
    }

    private fun calculateStats(list: List<Voter>): VoterStats {
        if (list.isEmpty()) return VoterStats()

        val total = list.size
        var registeredCount = 0
        var pendingCount = 0
        var unregisteredCount = 0

        val partyCounts = mutableMapOf<String, Int>()
        val leaningCounts = mutableMapOf<String, Int>()
        val issueCounts = mutableMapOf<String, Int>()
        var ageSum = 0
        var votedLastGeneralCount = 0
        var votedLastPrimaryCount = 0

        list.forEach { voter ->
            // Registration status
            when (voter.registrationStatus) {
                "Registered" -> registeredCount++
                "Pending" -> pendingCount++
                "Unregistered" -> unregisteredCount++
            }

            // Party
            partyCounts[voter.party] = (partyCounts[voter.party] ?: 0) + 1

            // Leaning
            leaningCounts[voter.supportLeaning] = (leaningCounts[voter.supportLeaning] ?: 0) + 1

            // Key Issues
            if (voter.keyIssues.isNotEmpty()) {
                voter.keyIssues.split(",").forEach { issue ->
                    val cleanIssue = issue.trim()
                    if (cleanIssue.isNotEmpty()) {
                        issueCounts[cleanIssue] = (issueCounts[cleanIssue] ?: 0) + 1
                    }
                }
            }

            // Age & History
            ageSum += voter.age
            if (voter.votedLastGeneral) votedLastGeneralCount++
            if (voter.votedLastPrimary) votedLastPrimaryCount++
        }

        return VoterStats(
            totalVoters = total,
            registeredCount = registeredCount,
            pendingCount = pendingCount,
            unregisteredCount = unregisteredCount,
            partyDistribution = partyCounts,
            learningDistribution = leaningCounts,
            topIssues = issueCounts.entries.sortedByDescending { it.value }.map { it.key to it.value },
            averageAge = if (total > 0) (ageSum.toFloat() / total) else 0f,
            votedLastGeneralRate = if (total > 0) (votedLastGeneralCount.toFloat() / total) else 0f,
            votedLastPrimaryRate = if (total > 0) (votedLastPrimaryCount.toFloat() / total) else 0f
        )
    }

    private suspend fun prepopulateDatabase() {
        val sampleVoters = listOf(
            Voter(
                fullName = "Eleanor Vance",
                phone = "555-0198",
                email = "eleanor.v@civicmail.org",
                address = "1042 Birch Street, Ward 4",
                ward = "Ward 4",
                age = 64,
                gender = "Female",
                registrationStatus = "Registered",
                party = "Democrat",
                supportLeaning = "Strongly Support",
                keyIssues = "Healthcare,Environment,Public Safety",
                notes = "Retired nurse, highly active volunteer. Primary concerns are senior healthcare access and local parks maintenance.",
                votedLastGeneral = true,
                votedLastPrimary = true
            ),
            Voter(
                fullName = "Marcus Vance",
                phone = "555-0143",
                email = "mvance@civicmail.org",
                address = "1042 Birch Street, Ward 4",
                ward = "Ward 4",
                age = 65,
                gender = "Male",
                registrationStatus = "Registered",
                party = "Independent",
                supportLeaning = "Leaning Support",
                keyIssues = "Economy,Healthcare,Infrastructure",
                notes = "Eleanor's husband. Retired construction manager. Strongly interested in civic infrastructure and small business tax benefits.",
                votedLastGeneral = true,
                votedLastPrimary = false
            ),
            Voter(
                fullName = "Devon Miller",
                phone = "555-0211",
                email = "devon.miller@fastmail.com",
                address = "421 Oakwood Boulevard, Ward 2",
                ward = "Ward 2",
                age = 29,
                gender = "Non-binary",
                registrationStatus = "Pending",
                party = "Democratic", // Match party name standardizing or custom
                supportLeaning = "Undecided",
                keyIssues = "Education,Environment,Economy",
                notes = "High school science teacher. Recent transplant. Submitted voter registration form recently; currently pending review.",
                votedLastGeneral = false,
                votedLastPrimary = false
            ),
            Voter(
                fullName = "Siddharth Rao",
                phone = "555-0812",
                email = "sid.rao@techcorp.com",
                address = "11 Ritz Plaza, Ward 1",
                ward = "Ward 1",
                age = 42,
                gender = "Male",
                registrationStatus = "Registered",
                party = "Republican",
                supportLeaning = "Strongly Oppose",
                keyIssues = "Economy,Public Safety",
                notes = "Tech company executive. Strongly focused on fiscal conservatism, municipal budget transparency, and business regulation reductions.",
                votedLastGeneral = true,
                votedLastPrimary = true
            ),
            Voter(
                fullName = "Gabriela Torres",
                phone = "555-0724",
                email = "gab.torres@localbiz.net",
                address = "805 Cherry Lane, Ward 2",
                ward = "Ward 2",
                age = 35,
                gender = "Female",
                registrationStatus = "Registered",
                party = "Independent",
                supportLeaning = "Leaning Support",
                keyIssues = "Education,Healthcare,Economy",
                notes = "Baker and small business owner. Worried about inflation and looking for strong local school funding plans.",
                votedLastGeneral = true,
                votedLastPrimary = true
            ),
            Voter(
                fullName = "Arthur Pendelton",
                phone = "555-0309",
                email = "apendelton@legacy.com",
                address = "216 Pine Crest Plaza, Ward 3",
                ward = "Ward 3",
                age = 78,
                gender = "Male",
                registrationStatus = "Unregistered",
                party = "Other",
                supportLeaning = "Undecided",
                keyIssues = "Infrastructure,Public Safety",
                notes = "Veteran. Long-term resident, claims he hasn't voted in years because of transport difficulties. Needs a ballot-by-mail application form and registration help.",
                votedLastGeneral = false,
                votedLastPrimary = false
            )
        )

        sampleVoters.forEach {
            repository.insertVoter(it)
        }
    }

    fun upsertVoter(voter: Voter) {
        viewModelScope.launch {
            if (voter.id == 0) {
                repository.insertVoter(voter)
            } else {
                repository.updateVoter(voter.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deleteVoter(voter: Voter) {
        viewModelScope.launch {
            repository.deleteVoter(voter)
        }
    }

    fun resetFilters() {
        searchQuery.value = ""
        selectedPartyFilter.value = "All"
        selectedStatusFilter.value = "All"
        selectedLeaningFilter.value = "All"
        selectedWardFilter.value = "All"
    }

    fun clearAllVoters() {
        viewModelScope.launch {
            repository.deleteAllVoters()
        }
    }

    // Factory Class
    class Factory(
        private val application: Application,
        private val repository: VoterRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VoterViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return VoterViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

// Data class to wrap calculated metrics
data class VoterStats(
    val totalVoters: Int = 0,
    val registeredCount: Int = 0,
    val pendingCount: Int = 0,
    val unregisteredCount: Int = 0,
    val partyDistribution: Map<String, Int> = emptyMap(),
    val learningDistribution: Map<String, Int> = emptyMap(),
    val topIssues: List<Pair<String, Int>> = emptyList(),
    val averageAge: Float = 0f,
    val votedLastGeneralRate: Float = 0f,
    val votedLastPrimaryRate: Float = 0f
)
