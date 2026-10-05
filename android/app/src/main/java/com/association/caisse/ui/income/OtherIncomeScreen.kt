package com.association.caisse.ui.income

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.association.caisse.data.local.dao.OtherIncomeWithCategory
import com.association.caisse.data.local.entity.IncomeCategoryEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherIncomeScreen(
    viewModel: OtherIncomeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var incomeToDelete by remember { mutableStateOf<OtherIncomeWithCategory?>(null) }

    val totalFiltered = remember(state.filteredIncomes) {
        state.filteredIncomes.sumOf { it.income.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Autres Recettes") },
                actions = {
                    TextButton(onClick = { showCategoryDialog = true }) {
                        Text("Catégories")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle recette")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barre de filtres de période
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedPeriodFilter == "ALL",
                        onClick = { viewModel.onPeriodFilterChanged("ALL") },
                        label = { Text("Toutes périodes") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedPeriodFilter == "CURRENT_MONTH",
                        onClick = { viewModel.onPeriodFilterChanged("CURRENT_MONTH") },
                        label = { Text("Ce mois-ci") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedPeriodFilter == "CURRENT_YEAR",
                        onClick = { viewModel.onPeriodFilterChanged("CURRENT_YEAR") },
                        label = { Text("Cette année") }
                    )
                }
            }

            // Barre de filtres de catégorie
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategoryFilter == null,
                        onClick = { viewModel.onCategoryFilterChanged(null) },
                        label = { Text("Toutes catégories") }
                    )
                }
                items(state.categories) { cat ->
                    FilterChip(
                        selected = state.selectedCategoryFilter == cat.id,
                        onClick = { viewModel.onCategoryFilterChanged(cat.id) },
                        label = { Text(cat.name) }
                    )
                }
            }

            // Bandeau de total de la sélection
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total de la sélection :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$totalFiltered (Espèces)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Liste chronologique des recettes
            if (state.filteredIncomes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune recette pour ces critères.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredIncomes) { item ->
                        val income = item.income
                        val catName = item.category?.name ?: "Autre"
                        val member = state.members.find { it.id == income.memberId }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (income.description.isNotBlank()) {
                                        Text(
                                            text = income.description,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${income.date} · ${member?.let { "${it.firstName} ${it.lastName}" } ?: "Association"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+ ${income.amount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(onClick = { incomeToDelete = item }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Supprimer",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue d'ajout d'une recette
    if (showAddDialog) {
        AddIncomeDialog(
            categories = state.categories.filter { it.isActive },
            members = state.members,
            onDismiss = { showAddDialog = false },
            onConfirm = { catId, memberId, amount, date, desc ->
                viewModel.addOtherIncome(catId, memberId, amount, date, desc)
                showAddDialog = false
            }
        )
    }

    // Dialogue de confirmation de suppression
    if (incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Voulez-vous vraiment supprimer cette recette de ${incomeToDelete?.income?.amount} en espèces ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        incomeToDelete?.let { viewModel.deleteOtherIncome(it.income) }
                        incomeToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue de gestion des catégories
    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = state.categories,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = { viewModel.addCategory(it) },
            onToggleCategory = { viewModel.updateCategory(it.copy(isActive = !it.isActive)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    categories: List<IncomeCategoryEntity>,
    members: List<com.association.caisse.data.local.entity.MemberEntity>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long, memberId: Long?, amount: Long, date: String, desc: String) -> Unit
) {
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    var selectedMemberId by remember { mutableStateOf<Long?>(null) }
    var amountText by remember { mutableStateOf("5000") }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var descText by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Recette (Espèces)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Catégorie
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCatId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCatId = cat.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // Montant
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Montant en espèces *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (aaaa-mm-jj) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("Description / Motif") },
                    placeholder = { Text("ex. Don anonyme, amende réunion...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (selectedCatId > 0 && amount > 0) {
                        onConfirm(selectedCatId, selectedMemberId, amount, dateText, descText)
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun CategoryManagerDialog(
    categories: List<IncomeCategoryEntity>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onToggleCategory: (IncomeCategoryEntity) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gérer les catégories") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Nouvelle catégorie") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newCatName.isNotBlank()) {
                                onAddCategory(newCatName)
                                newCatName = ""
                            }
                        }
                    ) {
                        Text("Ajouter")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (cat.isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (cat.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = cat.isActive,
                                onCheckedChange = { onToggleCategory(cat) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
