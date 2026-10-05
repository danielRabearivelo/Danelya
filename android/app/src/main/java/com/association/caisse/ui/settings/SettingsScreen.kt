package com.association.caisse.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var associationName by remember(state.associationName) { mutableStateOf(state.associationName) }
    var currency by remember(state.currency) { mutableStateOf(state.currency) }
    var defaultPrefix by remember(state.defaultCountryPrefix) { mutableStateOf(state.defaultCountryPrefix) }
    var reminderTemplate by remember(state.reminderMessageTemplate) { mutableStateOf(state.reminderMessageTemplate) }

    var showAddRateDialog by remember { mutableStateOf(false) }
    var showDoubleConfirmRestore by remember { mutableStateOf(false) }
    var restoreKeywordInput by remember { mutableStateOf("") }

    // SAF Launchers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupJson(uri)
        }
    }

    val exportSqliteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDatabaseFile(uri)
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectBackup(uri)
        }
    }

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres & Sauvegarde") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avertissement de sauvegarde si > 30 jours
            if (state.isBackupOverdue) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sauvegarde recommandée",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Aucune sauvegarde manuelle n'a été exportée depuis plus de 30 jours.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Section 1 : Informations Générales
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Paramètres de l'Association",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = associationName,
                            onValueChange = { associationName = it },
                            label = { Text("Nom de l'association") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = currency,
                                onValueChange = { currency = it },
                                label = { Text("Devise affichée") },
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = defaultPrefix,
                                onValueChange = { defaultPrefix = it },
                                label = { Text("Indicatif pays par défaut") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.saveGeneralSettings(associationName, currency, defaultPrefix, reminderTemplate)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }

            // Section 2 : Cotisation Mensuelle & Historique des Tarifs
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cotisation Mensuelle Générale",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Un changement s'applique à partir du mois choisi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { showAddRateDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Changer tarif")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        state.feeRates.sortedByDescending { it.effectiveFromMonth }.forEach { rate ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("À partir de ${rate.effectiveFromMonth} :", style = MaterialTheme.typography.bodyMedium)
                                Text("${rate.amount} ${state.currency} / mois", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Section 3 : Modèle de Message de Relance
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Modèle de Message de Relance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Variables disponibles : {prenom}, {nom}, {montant_du}, {mois_impayes}, {association}, {devise}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = reminderTemplate,
                            onValueChange = { reminderTemplate = it },
                            label = { Text("Texte du modèle") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.saveGeneralSettings(associationName, currency, defaultPrefix, reminderTemplate)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Enregistrer le modèle")
                        }
                    }
                }
            }

            // Section 4 : Sauvegarde & Restauration (SAF)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sauvegarde & Restauration (SAF)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Dernière sauvegarde manuelle : ${state.lastBackupDate ?: "Aucune"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.isBackupOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val fileName = "sauvegarde_caisse_${LocalDate.now()}.json"
                                    exportJsonLauncher.launch(fileName)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export JSON")
                            }

                            OutlinedButton(
                                onClick = {
                                    val fileName = "caisse_association_${LocalDate.now()}.db"
                                    exportSqliteLauncher.launch(fileName)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copie .db")
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Button(
                            onClick = { openBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurer une sauvegarde...")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal d'ajout d'un nouveau tarif
    if (showAddRateDialog) {
        var rateMonth by remember { mutableStateOf(LocalDate.now().toString().take(7)) }
        var rateAmount by remember { mutableStateOf("10000") }

        AlertDialog(
            onDismissRequest = { showAddRateDialog = false },
            title = { Text("Nouveau tarif de cotisation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = rateMonth,
                        onValueChange = { rateMonth = it },
                        label = { Text("Mois d'effet (aaaa-mm) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rateAmount,
                        onValueChange = { rateAmount = it },
                        label = { Text("Montant mensuel *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = rateAmount.toLongOrNull() ?: 0L
                        if (rateMonth.isNotBlank() && amount > 0) {
                            viewModel.addFeeRate(rateMonth, amount)
                            showAddRateDialog = false
                        }
                    }
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRateDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal de prévisualisation avant restauration
    if (state.backupPreview != null) {
        val preview = state.backupPreview!!

        AlertDialog(
            onDismissRequest = { viewModel.dismissPreview() },
            title = { Text(if (preview.isValid) "Prévisualisation de la sauvegarde" else "Fichier non valide") },
            text = {
                if (!preview.isValid) {
                    Text(preview.errorMessage ?: "Erreur inconnue de lecture du fichier.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Exporté le : ${preview.exportedAtDate}", fontWeight = FontWeight.Bold)
                        Text("• Nombre de membres : ${preview.membersCount}")
                        Text("• Nombre de paiements : ${preview.paymentsCount}")
                        Text("• Autres recettes : ${preview.otherIncomesCount}")
                        Text("• Versements : ${preview.remittancesCount}")
                        Text("• Dernière opération : ${preview.lastOperationDate}")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Attention : La restauration remplacera l'ensemble des données actuelles par le contenu de ce fichier.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                if (preview.isValid) {
                    Button(
                        onClick = {
                            viewModel.dismissPreview()
                            showDoubleConfirmRestore = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Continuer vers confirmation")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPreview() }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal de double confirmation explicite
    if (showDoubleConfirmRestore) {
        AlertDialog(
            onDismissRequest = {
                showDoubleConfirmRestore = false
                restoreKeywordInput = ""
            },
            title = { Text("Confirmation définitive") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pour éviter tout accident d'écrasement, veuillez taper le mot RESTAURER ci-dessous :",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = restoreKeywordInput,
                        onValueChange = { restoreKeywordInput = it },
                        label = { Text("Mot-clé de confirmation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreKeywordInput.trim() == "RESTAURER") {
                            showDoubleConfirmRestore = false
                            restoreKeywordInput = ""
                            viewModel.confirmRestore()
                        }
                    },
                    enabled = restoreKeywordInput.trim() == "RESTAURER",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmer l'écrasement")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDoubleConfirmRestore = false
                        restoreKeywordInput = ""
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}
