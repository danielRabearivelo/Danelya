package com.association.caisse.ui.members

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.association.caisse.domain.MonthFeeStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailScreen(
    viewModel: MemberDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNewPayment: (memberId: Long) -> Unit,
    onNavigateToEditMember: (memberId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteBlockedDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.member?.let { "${it.lastName.uppercase()} ${it.firstName}" }
                            ?: "Fiche Membre"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    state.member?.let { mem ->
                        IconButton(onClick = { onNavigateToEditMember(mem.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            state.member?.let { mem ->
                FloatingActionButton(
                    onClick = { onNavigateToNewPayment(mem.id) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Régler cotisation")
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val member = state.member ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Carte Récapitulative du Membre
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${member.lastName.uppercase()} ${member.firstName}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (member.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (member.isActive) "Actif" else "Archivé",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (member.phone.isNotBlank()) "Tél : ${member.phone}" else "Téléphone non renseigné",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Membre depuis : ${member.joinMonth}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (member.note.isNotBlank()) {
                            Text(
                                text = "Note : ${member.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Soldes comptables
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Dû", style = MaterialTheme.typography.labelSmall)
                                Text("${state.totalDue} Ar", fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Total Payé", style = MaterialTheme.typography.labelSmall)
                                Text("${state.totalPaid} Ar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Reste à régler", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = "${state.balanceDue} Ar",
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Grille des 12 Mois (Code couleur Material 3)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Situation de l'année ${state.selectedYear}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grille 4 colonnes x 3 lignes = 12 mois
                        val months = listOf(
                            "01" to "Janv.", "02" to "Févr.", "03" to "Mars", "04" to "Avr.",
                            "05" to "Mai", "06" to "Juin", "07" to "Juil.", "08" to "Août",
                            "09" to "Sept.", "10" to "Oct.", "11" to "Nov.", "12" to "Déc."
                        )

                        for (row in 0 until 3) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (col in 0 until 4) {
                                    val idx = row * 4 + col
                                    val (mNum, mLabel) = months[idx]
                                    val ym = "${state.selectedYear}-$mNum"
                                    val status = state.yearBalances[ym]

                                    val (badgeBg, badgeText, statusLabel) = when (status) {
                                        MonthFeeStatus.PAID -> Triple(Color(0xFF2E7D32), Color.White, "Payé")
                                        MonthFeeStatus.PARTIAL -> Triple(Color(0xFFED6C02), Color.White, "Partiel")
                                        MonthFeeStatus.UNPAID -> Triple(Color(0xFFD32F2F), Color.White, "Impayé")
                                        MonthFeeStatus.ADVANCE -> Triple(Color(0xFF0288D1), Color.White, "Avance")
                                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Non échu")
                                    }

                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = mLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeText
                                            )
                                            Text(
                                                text = statusLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = badgeText.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Historique des Paiements
            item {
                Text(
                    text = "Historique des paiements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (state.payments.isEmpty()) {
                item {
                    Text(
                        text = "Aucun paiement enregistré pour ce membre.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(state.payments) { item ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.payment.date,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "+ ${item.payment.amount} Ar",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (item.allocations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ventilation : " + item.allocations.joinToString(", ") { "${it.month} (${it.amount} Ar)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Boutons d'administration du membre
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.toggleMemberActive() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (member.isActive) "Archiver" else "Réactiver")
                    }

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Supprimer")
                    }
                }
            }
        }
    }

    // Confirmation de suppression
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Supprimer le membre") },
            text = { Text("Confirmez-vous la suppression définitive de ce membre ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        scope.launch {
                            val success = viewModel.deleteMemberSafely()
                            if (success) {
                                onNavigateBack()
                            } else {
                                showDeleteBlockedDialog = true
                            }
                        }
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Message explicatif si suppression bloquée
    if (showDeleteBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBlockedDialog = false },
            title = { Text("Suppression impossible") },
            text = {
                Text(
                    "Ce membre possède un historique de paiements ou de recettes. Pour garantir l'exactitude de la comptabilité, il ne peut pas être supprimé. Vous pouvez l'archiver à la place."
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteBlockedDialog = false }) {
                    Text("Compris")
                }
            }
        )
    }
}
