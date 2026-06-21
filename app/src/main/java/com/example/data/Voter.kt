package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voters")
data class Voter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val ward: String = "", // Precinct / Ward / Voting District
    val age: Int = 18,
    val gender: String = "Undeclared", // "Male", "Female", "Non-binary", "Undeclared"
    val registrationStatus: String = "Registered", // "Registered", "Unregistered", "Pending"
    val party: String = "Independent", // "Democrat", "Republican", "Independent", "Green", "Libertarian", "Other"
    val supportLeaning: String = "Undecided", // "Strongly Support", "Leaning Support", "Undecided", "Leaning Oppose", "Strongly Oppose"
    val keyIssues: String = "", // Comma-separated list of values (e.g. "Economy,Healthcare,Education")
    val notes: String = "",
    val votedLastGeneral: Boolean = false,
    val votedLastPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
