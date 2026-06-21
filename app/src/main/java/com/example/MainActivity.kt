package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.Voter
import com.example.data.VoterRepository
import com.example.ui.VoterStats
import com.example.ui.VoterViewModel
import com.example.ui.theme.*

// Civic theme color fallbacks aligned with Geometric Balance Specs
val CivicTeal = Color(0xFF14B8A6)
val CivicAccentGold = Color(0xFFF59E0B)
val CivicRed = Color(0xFFEF4444)
val CivicBlueSecondary = Color(0xFF3B82F6)
val CivicCoral = Color(0xFFF97316)
val CivicSlateLight = Color(0xFF64748B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup persistent database and repo using Singleton model helper
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = VoterRepository(database.voterDao())
        val viewModel: VoterViewModel by viewModels {
            VoterViewModel.Factory(application, repository)
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        VoterAppScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoterAppScreen(viewModel: VoterViewModel) {
    val context = LocalContext.current
    
    // Core voter flows
    val voters by viewModel.filteredVoters.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val availableWards by viewModel.availableWards.collectAsStateWithLifecycle()
    
    // UI state
    var showAddEditSheet by remember { mutableStateOf(false) }
    var selectedVoterForEdit by remember { mutableStateOf<Voter?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Voter?>(null) }
    var showResetDatabaseDialog by remember { mutableStateOf(false) }
    var isAnalyticsExpanded by remember { mutableStateOf(false) }
    
    // Bottom sheet state helper
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "DATABASE ALPHA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Voter Collection",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            letterSpacing = (-0.3).sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showResetDatabaseDialog = true },
                        modifier = Modifier.testTag("reset_db_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Database",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp, start = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedVoterForEdit = null
                    showAddEditSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .testTag("add_voter_fab")
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Voter")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Voter", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            
            // 1. COLLAPSIBLE CIVIC STATISTICS SCREEN
            AnalyticsSection(
                stats = stats,
                isExpanded = isAnalyticsExpanded,
                onToggleExpand = { isAnalyticsExpanded = !isAnalyticsExpanded }
            )
            
            // 2. SEARCH & DYNAMIC FILTER BAR
            FilterAndSearchSection(viewModel = viewModel, availableWards = availableWards)
            
            // 3. VOTER LIST SECTION
            if (voters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "No Voters Found",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Voter Records Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Adjust your filter tags or register a new voter utilizing the action button below.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(voters, key = { it.id }) { voter ->
                        VoterCard(
                            voter = voter,
                            onEdit = {
                                selectedVoterForEdit = voter
                                showAddEditSheet = true
                            },
                            onDelete = {
                                showDeleteConfirmDialog = voter
                            }
                        )
                    }
                }
            }
        }
        
        // 4. ADD/EDIT VOTER MODAL BOTTOM SHEET
        if (showAddEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddEditSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                VoterFormSheetContent(
                    voter = selectedVoterForEdit,
                    onSave = { updatedVoter ->
                        viewModel.upsertVoter(updatedVoter)
                        showAddEditSheet = false
                        Toast.makeText(
                            context,
                            if (selectedVoterForEdit == null) "Registered voter successfully" else "Updated voter record successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onCancel = { showAddEditSheet = false }
                )
            }
        }
        
        // 5. DELETE CONFIRMATION DIALOG
        showDeleteConfirmDialog?.let { voter ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Delete Voter Record?", fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently erase ${voter.fullName}'s registration profile and historic outreach concerns from the local voter archive.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteVoter(voter)
                            showDeleteConfirmDialog = null
                            Toast.makeText(context, "Deleted voter record", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete permanently", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // 6. CLEAR/RESET DATABASE DIALOG
        if (showResetDatabaseDialog) {
            AlertDialog(
                onDismissRequest = { showResetDatabaseDialog = false },
                title = { Text("Reset Voter Local DB?", fontWeight = FontWeight.Bold) },
                text = { Text("This operation is irreversible and will delete all user-added voter profiles, before reloaded the secure default civic sample dataset.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllVoters()
                            showResetDatabaseDialog = false
                            Toast.makeText(context, "Database wiped and catalog reset", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Wipe & Reload")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDatabaseDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AnalyticsSection(
    stats: VoterStats,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Civic Insights",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Voter Database Metrics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "${stats.totalVoters} Total",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Expand Metrics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Collapsed quick-view panel
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Card: Registered (mapped from stats.registeredCount)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Registered",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format("%,d", stats.registeredCount),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Right Card: Pending Outreach / Verification (mapped from stats.pendingCount)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pending Sync",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = String.format("%,d", stats.pendingCount),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
            
            // Rich expanded dashboard panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    
                    // Registration Progress bar
                    val regRate = if (stats.totalVoters > 0) stats.registeredCount.toFloat() / stats.totalVoters else 0f
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Registration Verification Rate",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "${(regRate * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CivicTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { regRate },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CivicTeal,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Two-Column Grid of Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left: Party Distribution counts
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "Party Affiliation",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                val parties = listOf("Democrat", "Republican", "Independent", "Other")
                                parties.forEach { party ->
                                    val count = stats.partyDistribution[party] ?: 0
                                    val pct = if (stats.totalVoters > 0) count.toFloat() / stats.totalVoters else 0f
                                    
                                    val barColor = when(party) {
                                        "Democrat" -> CivicBlueSecondary
                                        "Republican" -> CivicRed
                                        "Independent" -> CivicTeal
                                        else -> CivicAccentGold
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(party, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("$count (${(pct*100).toInt()}%)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = barColor,
                                        trackColor = Color.LightGray.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                        
                        // Right: Leaning Distribution counts
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "Outreach Leaning",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                val leanings = listOf("Strongly Support", "Leaning Support", "Undecided")
                                leanings.forEach { leaning ->
                                    val count = stats.learningDistribution[leaning] ?: 0
                                    val pct = if (stats.totalVoters > 0) count.toFloat() / stats.totalVoters else 0f
                                    
                                    val barColor = when(leaning) {
                                        "Strongly Support" -> CivicTeal
                                        "Leaning Support" -> CivicTeal.copy(alpha = 0.6f)
                                        else -> CivicAccentGold
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(leaning.replace(" Support", ""), fontSize = 10.sp, maxLines = 1)
                                        Text("$count (${(pct*100).toInt()}%)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = barColor,
                                        trackColor = Color.LightGray.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Demographic quick insight footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Average age: ${stats.averageAge.toInt()} yrs old",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Voted Gen'24: ${(stats.votedLastGeneralRate * 100).toInt()}%  |  Prim'24: ${(stats.votedLastPrimaryRate * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatBadge(label: String, count: Int, baseColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(baseColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(baseColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: ",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterAndSearchSection(
    viewModel: VoterViewModel,
    availableWards: List<String>
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val partyFilter by viewModel.selectedPartyFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val leaningFilter by viewModel.selectedLeaningFilter.collectAsStateWithLifecycle()
    val wardFilter by viewModel.selectedWardFilter.collectAsStateWithLifecycle()
    
    var isFilterPopupOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Search text-field
        TextField(
            value = query,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search voter ID, name, address or ward...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search icon", tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input")
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Fast Filtering row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick reset badge
                if (partyFilter != "All" || statusFilter != "All" || leaningFilter != "All" || wardFilter != "All") {
                    InputChip(
                        selected = true,
                        onClick = { viewModel.resetFilters() },
                        label = { Text("Reset Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        trailingIcon = { Icon(imageVector = Icons.Default.Close, contentDescription = "ResetFiltersBtn", modifier = Modifier.size(14.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                
                // Show current active badges
                FilterChipBadge("Party: $partyFilter", partyFilter != "All") { viewModel.selectedPartyFilter.value = "All" }
                FilterChipBadge("Status: $statusFilter", statusFilter != "All") { viewModel.selectedStatusFilter.value = "All" }
                FilterChipBadge("Support: ${leaningFilter.replace(" Support", "")}", leaningFilter != "All") { viewModel.selectedLeaningFilter.value = "All" }
                FilterChipBadge("Ward: $wardFilter", wardFilter != "All") { viewModel.selectedWardFilter.value = "All" }
            }
            
            // Expand advanced filters dial button
            IconButton(
                onClick = { isFilterPopupOpen = !isFilterPopupOpen },
                modifier = Modifier.testTag("open_filters_popup_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu, 
                    contentDescription = "Configure Filters",
                    tint = if (isFilterPopupOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Advanced Filters Collapsible Section
        AnimatedVisibility(
            visible = isFilterPopupOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Outreach Parameters Criteria", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Wrap-layout for dropdowns
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Party Filter Selector
                        FilterDropdown(
                            label = "Party Affiliation",
                            selectedValue = partyFilter,
                            options = listOf("All", "Democrat", "Republican", "Independent", "Libertarian", "Green", "Other"),
                            onSelected = { viewModel.selectedPartyFilter.value = it }
                        )
                        
                        // 2. Status Filter Selector
                        FilterDropdown(
                            label = "Reg. Status",
                            selectedValue = statusFilter,
                            options = listOf("All", "Registered", "Pending", "Unregistered"),
                            onSelected = { viewModel.selectedStatusFilter.value = it }
                        )
                        
                        // 3. Support Leaning Selector
                        FilterDropdown(
                            label = "Support Preference",
                            selectedValue = leaningFilter,
                            options = listOf("All", "Strongly Support", "Leaning Support", "Undecided", "Leaning Oppose", "Strongly Oppose"),
                            onSelected = { viewModel.selectedLeaningFilter.value = it }
                        )
                        
                        // 4. District Ward Selector
                        FilterDropdown(
                            label = "Voting Precinct",
                            selectedValue = wardFilter,
                            options = listOf("All") + availableWards,
                            onSelected = { viewModel.selectedWardFilter.value = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipBadge(text: String, isActive: Boolean, onClear: () -> Unit) {
    if (isActive) {
        InputChip(
            selected = true,
            onClick = onClear,
            label = { Text(text, fontSize = 11.sp) },
            trailingIcon = { Icon(imageVector = Icons.Default.Close, contentDescription = "Clear tag icon", modifier = Modifier.size(12.dp)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .width(160.dp)
                .menuAnchor(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp)
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp) },
                    onClick = {
                        onSelected(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun VoterCard(
    voter: Voter,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Set colors according to political party & outreach preference
    val partyColor = when(voter.party) {
        "Democrat" -> CivicBlueSecondary
        "Republican" -> CivicRed
        "Independent" -> CivicTeal
        else -> CivicSlateLight
    }
    
    val leaningColor = when(voter.supportLeaning) {
        "Strongly Support" -> CivicTeal
        "Leaning Support" -> CivicTeal.copy(alpha = 0.65f)
        "Undecided" -> CivicAccentGold
        "Leaning Oppose" -> CivicCoral
        else -> CivicRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voter_card_${voter.id}")
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Rounded-Square Avatar placeholder indicating party
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(partyColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = voter.fullName.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString()?.uppercase() }.joinToString(""),
                        color = partyColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = voter.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Action badges
                        Surface(
                            color = partyColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, partyColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = voter.party,
                                color = partyColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ward District Information Badge
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Precinct Icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (voter.ward.isNotEmpty()) voter.ward else "Unassigned Ward",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Registration Status Badge
                        val statusBorderColor = when(voter.registrationStatus) {
                            "Registered" -> CivicTeal
                            "Pending" -> CivicAccentGold
                            else -> CivicRed
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusBorderColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = voter.registrationStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusBorderColor
                        )
                    }
                }
            }

            // Quick display of support status & summary of keys
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Outreach: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = voter.supportLeaning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = leaningColor
                    )
                }
                
                if (voter.keyIssues.isNotEmpty()) {
                    Text(
                        text = voter.keyIssues.split(",").take(2).joinToString(" • "),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Expanded profile detail card section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Divider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // Comprehensive demographics & details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Demographics: ${voter.age} yrs old • Gender: ${voter.gender}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (voter.address.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                // Real Action Integration: Open Google Maps for canvassing coordinate routing!
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(voter.address)}"))
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Navigation Address",
                                tint = CivicBlueSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = voter.address,
                                fontSize = 12.sp,
                                color = CivicBlueSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Display Concerns Checkbox Tags
                    if (voter.keyIssues.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Key Voter Concerns:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            voter.keyIssues.split(",").forEach { issue ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = issue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // Call records/notes text area
                    if (voter.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Canvassing Outreach Notes",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = voter.notes,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Ballot turnout history indicators
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Turnout History: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        HistoryIndicator("General '24", voter.votedLastGeneral)
                        Spacer(modifier = Modifier.width(6.dp))
                        HistoryIndicator("Primary '24", voter.votedLastPrimary)
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // Bottom Buttons: Edit profile, Contact, Dial phone, Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Dial mobile action button
                            if (voter.phone.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${voter.phone}"))
                                        context.startActivity(intent)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = "Dial Voter Phone", modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            // Send email action button
                            if (voter.email.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${voter.email}"))
                                        context.startActivity(intent)
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = "Email Voter Email", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = onEdit,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile icon", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Profile", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            
                            TextButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Profile icon", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Erase", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryIndicator(label: String, voted: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                if (voted) CivicTeal.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(6.dp)
            )
            .border(
                0.5.dp,
                if (voted) CivicTeal.copy(alpha = 0.3F) else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (voted) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (voted) CivicTeal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (voted) CivicTeal else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoterFormSheetContent(
    voter: Voter?,
    onSave: (Voter) -> Unit,
    onCancel: () -> Unit
) {
    // Form fields input local state
    var fullName by remember { mutableStateOf(voter?.fullName ?: "") }
    var phone by remember { mutableStateOf(voter?.phone ?: "") }
    var email by remember { mutableStateOf(voter?.email ?: "") }
    var address by remember { mutableStateOf(voter?.address ?: "") }
    var ward by remember { mutableStateOf(voter?.ward ?: "") }
    var ageString by remember { mutableStateOf(voter?.age?.toString() ?: "18") }
    var gender by remember { mutableStateOf(voter?.gender ?: "Undeclared") }
    var registrationStatus by remember { mutableStateOf(voter?.registrationStatus ?: "Registered") }
    var party by remember { mutableStateOf(voter?.party ?: "Independent") }
    var supportLeaning by remember { mutableStateOf(voter?.supportLeaning ?: "Undecided") }
    var notes by remember { mutableStateOf(voter?.notes ?: "") }
    var votedLastGeneral by remember { mutableStateOf(voter?.votedLastGeneral ?: false) }
    var votedLastPrimary by remember { mutableStateOf(voter?.votedLastPrimary ?: false) }

    // Issues choosing list mapping
    val standardIssuesList = listOf("Economy", "Healthcare", "Education", "Environment", "Public Safety", "Infrastructure")
    val selectedIssues = remember { 
        mutableStateListOf<String>().apply {
            if (voter?.keyIssues?.isNotEmpty() == true) {
                addAll(voter.keyIssues.split(",").map { it.trim() })
            }
        }
    }

    // Validation alerts
    var nameError by remember { mutableStateOf(false) }
    var ageError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = if (voter == null) "Register New Voter" else "Edit Voter Profile",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Store registration criteria, affiliation, and outreach contact notes locally.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // 1. Full name input
        OutlinedTextField(
            value = fullName,
            onValueChange = {
                fullName = it
                nameError = it.trim().isEmpty()
            },
            label = { Text("Full Name *") },
            isError = nameError,
            supportingText = { if (nameError) Text("Political registry names must cannot be blank.", color = MaterialTheme.colorScheme.error) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("form_name_input")
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 2. Demographic row (Age & Gender)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = ageString,
                onValueChange = {
                    ageString = it
                    val parsed = it.toIntOrNull()
                    ageError = parsed == null || parsed < 18 || parsed > 120
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Age *") },
                isError = ageError,
                supportingText = { if (ageError) Text("Must be 18+", color = MaterialTheme.colorScheme.error) },
                singleLine = true,
                modifier = Modifier
                    .weight(0.4f)
                    .testTag("form_age_input")
            )
            
            // Gender Dropdown button inline
            Box(modifier = Modifier.weight(0.6f)) {
                var genderExpanded by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Toggle") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { genderExpanded = true }
                )
                DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                    listOf("Female", "Male", "Non-binary", "Undeclared").forEach { genVal ->
                        DropdownMenuItem(
                            text = { Text(genVal) },
                            onClick = {
                                gender = genVal
                                genderExpanded = false
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 3. Contact information fields
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            label = { Text("Phone Number") },
            placeholder = { Text("e.g. 555-0100") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            label = { Text("Email Address") },
            placeholder = { Text("voter@civicmail.org") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 4. Address & Precinct Info
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Physical Street Address") },
            placeholder = { Text("e.g. 1042 Birch Street") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = ward,
            onValueChange = { ward = it },
            label = { Text("Voting Precinct / Ward") },
            placeholder = { Text("e.g. Ward 4") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("form_ward_input")
        )
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Divider()
        Spacer(modifier = Modifier.height(10.dp))
        
        // 5. REGISTRATION STATUS CHOICE
        Text("Registration Integrity", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Registered", "Pending", "Unregistered").forEach { statusVal ->
                val active = registrationStatus == statusVal
                val statusColor = when(statusVal) {
                    "Registered" -> CivicTeal
                    "Pending" -> CivicAccentGold
                    else -> CivicRed
                }
                
                FilterChip(
                    selected = active,
                    onClick = { registrationStatus = statusVal },
                    label = { Text(statusVal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = statusColor.copy(alpha = 0.15f),
                        selectedLabelColor = statusColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = active,
                        selectedBorderColor = statusColor,
                        selectedBorderWidth = 1.5.dp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 6. PARTY AFFILIATION SELECTOR INDEX
        Text("Party Affiliation Registry", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Democrat", "Republican", "Independent", "Libertarian", "Green", "Other").forEach { partyVal ->
                val active = party == partyVal
                val partyThemeColor = when(partyVal) {
                    "Democrat" -> CivicBlueSecondary
                    "Republican" -> CivicRed
                    "Independent" -> CivicTeal
                    else -> CivicSlateLight
                }
                
                FilterChip(
                    selected = active,
                    onClick = { party = partyVal },
                    label = { Text(partyVal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = partyThemeColor.copy(alpha = 0.15f),
                        selectedLabelColor = partyThemeColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = active,
                        selectedBorderColor = partyThemeColor,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 7. OUTREACH LEANING SELECTOR
        Text("Campaign Outreach Leaning", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Strongly Support", "Leaning Support", "Undecided", "Leaning Oppose", "Strongly Oppose").forEach { leanVal ->
                val active = supportLeaning == leanVal
                val supportThemeColor = when(leanVal) {
                    "Strongly Support" -> CivicTeal
                    "Leaning Support" -> CivicTeal.copy(alpha = 0.6f)
                    "Undecided" -> CivicAccentGold
                    "Leaning Oppose" -> CivicCoral
                    else -> CivicRed
                }
                
                FilterChip(
                    selected = active,
                    onClick = { supportLeaning = leanVal },
                    label = { Text(leanVal.replace(" Support", " Support").replace(" Oppose", " Oppose")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = supportThemeColor.copy(alpha = 0.15f),
                        selectedLabelColor = supportThemeColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = active,
                        selectedBorderColor = supportThemeColor,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        Divider()
        Spacer(modifier = Modifier.height(10.dp))
        
        // 8. KEY CONCERNS SELECTION
        Text("Core Civic Concerns Checklist", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            standardIssuesList.forEach { issue ->
                val isSelected = selectedIssues.contains(issue)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            selectedIssues.remove(issue)
                        } else {
                            selectedIssues.add(issue)
                        }
                    },
                    label = { Text(issue) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 9. OUTREACH DIALOG NOTES
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Canvassing Conversation Notes") },
            placeholder = { Text("Enter detail regarding municipal concerns, candidate support comments or accessibility requirements...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 10. PAST VOTING HISTORY
        Text("Voting Turnout History", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { votedLastGeneral = !votedLastGeneral }
            ) {
                Checkbox(
                    checked = votedLastGeneral,
                    onCheckedChange = { votedLastGeneral = it }
                )
                Text("Voted General '24", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { votedLastPrimary = !votedLastPrimary }
            ) {
                Checkbox(
                    checked = votedLastPrimary,
                    onCheckedChange = { votedLastPrimary = it }
                )
                Text("Voted Primary '24", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 11. BUTTON ACTIONS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    val nameTrimmed = fullName.trim()
                    val ageParsed = ageString.toIntOrNull()
                    
                    if (nameTrimmed.isEmpty()) {
                        nameError = true
                        return@Button
                    }
                    if (ageParsed == null || ageParsed < 18 || ageParsed > 120) {
                        ageError = true
                        return@Button
                    }
                    
                    val compiledVoter = Voter(
                        id = voter?.id ?: 0,
                        fullName = nameTrimmed,
                        phone = phone.trim(),
                        email = email.trim(),
                        address = address.trim(),
                        ward = ward.trim(),
                        age = ageParsed,
                        gender = gender,
                        registrationStatus = registrationStatus,
                        party = party,
                        supportLeaning = supportLeaning,
                        keyIssues = selectedIssues.joinToString(","),
                        notes = notes.trim(),
                        votedLastGeneral = votedLastGeneral,
                        votedLastPrimary = votedLastPrimary,
                        createdAt = voter?.createdAt ?: System.currentTimeMillis()
                    )
                    
                    onSave(compiledVoter)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_voter_button")
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
