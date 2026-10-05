package com.association.caisse.ui.remittances

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.association.caisse.data.local.entity.RemittanceEntity
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemittancesScreen(
    viewModel: RemittancesViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewRemittanceDialog by remember { mutableStateOf(false) }
    var remittanceToDelete by remember { mutableStateOf<RemittanceEntity?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Historique des versements, 1 = État périodique

    // SAF Document Launchers
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportPdf(context, uri, "Association Solidarité")
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            writeCsvToUri(context, uri, viewModel.generateCsvContent())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Versements à la Caisse") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val fileName = "etat_versements_${LocalDate.now()}.pdf"
                        exportPdfLauncher.launch(fileName)
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exporter PDF")
                    }
                    IconButton(onClick = {
                        val fileName = "versements_${LocalDate.now()}.csv"
                        exportCsvLauncher.launch(fileName)
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Exporter CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewRemittanceDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau versement")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cartes récapitulatives (Total Encaissé, Total Versé, Reste à Verser)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Encaissé", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${state.totalCollectedAllTime}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Versé", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${state.totalRemittedAllTime}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Reste dû", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(
                            text = "${state.remainingToRemit}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Onglets : Historique vs État Périodique
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Historique (${state.remittances.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("État de versement") }
                )
            }

            if (selectedTab == 0) {
                // Liste chronologique des versements
                if (state.remittances.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Aucun versement enregistré.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.remittances) { r ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${r.amount} ${state.currency}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Reçu par : ${r.recipient}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (!r.coveredPeriod.isNullOrBlank()) {
                                            Text(
                                                text = "Période : ${r.coveredPeriod}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = r.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { remittanceToDelete = r }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tableau de l'état de versement par mois
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mois", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text("Encaissé", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text("Versé", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text("Cumul dû", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        }
                    }

                    items(state.monthlyStatement) { row ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(row.month, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
                                Text("${row.totalCollected}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f))
                                Text("${row.remitted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.2f))
                                Text("${row.cumulativeRemaining}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Nouveau Versement
    if (showNewRemittanceDialog) {
        NewRemittanceDialog(
            remainingToRemit = state.remainingToRemit,
            defaultRecipient = state.lastRecipient,
            onDismiss = { showNewRemittanceDialog = false },
            onConfirm = { amount, recipient, period, date, note ->
                viewModel.addRemittance(amount, recipient, period, date, note)
                showNewRemittanceDialog = false
            }
        )
    }

    // Modal Confirmation Suppression
    if (remittanceToDelete != null) {
        val target = remittanceToDelete!!
        AlertDialog(
            onDismissRequest = { remittanceToDelete = null },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer ce versement de ${target.amount} ${state.currency} effectué à ${target.recipient} ?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRemittance(target)
                    remittanceToDelete = null
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { remittanceToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun NewRemittanceDialog(
    remainingToRemit: Long,
    defaultRecipient: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, recipient: String, period: String?, date: String, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(if (remainingToRemit > 0) remainingToRemit.toString() else "0") }
    var recipientText by remember { mutableStateOf(defaultRecipient) }
    var periodText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var noteText by remember { mutableStateOf("") }
    var showWarningExceeded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Versement en Caisse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reste calculé en caisse : $remainingToRemit",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Montant versé (espèces) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = recipientText,
                    onValueChange = { recipientText = it },
                    label = { Text("Destinataire (Caisse Centrale) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = periodText,
                    onValueChange = { periodText = it },
                    label = { Text("Période couverte (optionnel)") },
                    placeholder = { Text("ex. Janvier - Février 2026") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (aaaa-mm-jj) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note optionnelle") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (showWarningExceeded) {
                    Text(
                        text = "Attention : ce montant dépasse le reste à verser théorique en caisse.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > remainingToRemit && !showWarningExceeded) {
                        showWarningExceeded = true
                    } else if (amount > 0 && recipientText.isNotBlank()) {
                        onConfirm(amount, recipientText, periodText.ifBlank { null }, dateText, noteText)
                    }
                }
            ) {
                Text(if (showWarningExceeded) "Confirmer quand même" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun writeCsvToUri(context: Context, uri: Uri, content: String) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(content)
                writer.flush()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
