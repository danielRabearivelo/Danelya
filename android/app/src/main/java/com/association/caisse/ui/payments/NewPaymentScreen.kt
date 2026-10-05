package com.association.caisse.ui.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPaymentScreen(
    viewModel: PaymentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var memberMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paiement de Cotisation (Espèces)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Sélection du membre
                ExposedDropdownMenuBox(
                    expanded = memberMenuExpanded,
                    onExpandedChange = { memberMenuExpanded = !memberMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = state.selectedMember?.let { "${it.lastName.uppercase()} ${it.firstName}" }
                            ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Membre concerné *") },
                        placeholder = { Text("Choisir un membre...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = memberMenuExpanded,
                        onDismissRequest = { memberMenuExpanded = false }
                    ) {
                        state.members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text("${member.lastName.uppercase()} ${member.firstName}") },
                                onClick = {
                                    viewModel.onSelectMember(member)
                                    memberMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Saisie du montant et de la date
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = { viewModel.onAmountChanged(it) },
                        label = { Text("Montant reçu (espèces) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = state.dateInput,
                        onValueChange = { viewModel.onDateChanged(it) },
                        label = { Text("Date (aaaa-mm-jj) *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            // Note optionnelle
            item {
                OutlinedTextField(
                    value = state.noteInput,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    label = { Text("Note optionnelle") },
                    placeholder = { Text("ex. Remise en main propre...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // En-tête de ventilation
            if (state.autoProposals.isNotEmpty()) {
                item {
                    Text(
                        text = "Aperçu de la répartition (modifiable manuellement) :",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Liste des propositions de ventilation
                items(state.autoProposals) { proposal ->
                    val manualAmount = state.manualAllocations[proposal.month] ?: 0L

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proposal.monthLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Dû : ${proposal.expectedFee} | Déjà payé : ${proposal.previouslyPaid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = manualAmount.toString(),
                                onValueChange = { newVal ->
                                    val parsed = newVal.toLongOrNull() ?: 0L
                                    viewModel.onUpdateManualAllocation(proposal.month, parsed)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("Affecté") },
                                singleLine = true,
                                modifier = Modifier.width(130.dp)
                            )
                        }
                    }
                }
            }

            // Affichage des erreurs éventuelles
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Bouton de validation
            item {
                Button(
                    onClick = { viewModel.savePayment() },
                    enabled = !state.isSaving && state.selectedMember != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Valider l'encaissement en espèces")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
