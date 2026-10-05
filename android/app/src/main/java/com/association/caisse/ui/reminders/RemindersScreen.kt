package com.association.caisse.ui.reminders

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.association.caisse.data.local.entity.ReminderChannel
import com.association.caisse.domain.ReminderHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (memberId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var memberToRemind by remember { mutableStateOf<LateMemberItem?>(null) }
    var editedMessage by remember { mutableStateOf("") }
    var pendingChannel by remember { mutableStateOf<ReminderChannel?>(null) }
    var antiHarassmentWarningMember by remember { mutableStateOf<LateMemberItem?>(null) }

    // Déclenche l'intention système et journalise l'action
    val launchReminderIntent = { item: LateMemberItem, channel: ReminderChannel, message: String ->
        val normalized = ReminderHelper.normalizePhoneNumber(item.member.phone, state.defaultPrefix)
        viewModel.logReminderAction(item.member.id, channel, item.totalDue)

        when (channel) {
            ReminderChannel.WHATSAPP -> {
                val url = ReminderHelper.buildWhatsAppUrl(normalized, message)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Si WhatsApp n'est pas installé, basculer sur l'application SMS
                    context.startActivity(ReminderHelper.createSmsIntent(normalized, message))
                }
            }
            ReminderChannel.SMS -> {
                context.startActivity(ReminderHelper.createSmsIntent(normalized, message))
            }
            ReminderChannel.CALL -> {
                context.startActivity(ReminderHelper.createDialIntent(normalized))
            }
        }

        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Relance enregistrée pour ${item.member.firstName}",
                actionLabel = "Annuler",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastReminderLog()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impayés & Relances") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (state.lateMembers.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.startBatchQueue() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("En série")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Barre de tri
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.sortOrder == RemindersSortOrder.AMOUNT_DESC,
                        onClick = { viewModel.onSortOrderChanged(RemindersSortOrder.AMOUNT_DESC) },
                        label = { Text("Trier par montant dû") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.sortOrder == RemindersSortOrder.SENIORITY_DESC,
                        onClick = { viewModel.onSortOrderChanged(RemindersSortOrder.SENIORITY_DESC) },
                        label = { Text("Trier par ancienneté") }
                    )
                }
            }

            // Nombre total et montant
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.lateMembers.size} membre(s) en retard",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Total : ${state.lateMembers.sumOf { it.totalDue }} ${state.currency}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Liste des membres en retard
            if (state.lateMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun membre en retard. Toutes les cotisations sont à jour !",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.lateMembers) { item ->
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
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${item.member.lastName.uppercase()} ${item.member.firstName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.unpaidMonths.size} mois : " + item.unpaidMonths.joinToString(", ") { it.month },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = ReminderHelper.formatLastReminderStatus(item.lastReminderTimestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${item.totalDue} ${state.currency}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        if (item.isPhoneMissing) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Tél manquant",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Boutons d'action : WhatsApp, SMS, Appel, Régler
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // WhatsApp
                                    Button(
                                        onClick = {
                                            if (item.isPhoneMissing) return@Button
                                            val (isTooSoon, _) = ReminderHelper.checkAntiHarassment(
                                                item.lastReminderTimestamp,
                                                state.minReminderIntervalDays
                                            )
                                            if (isTooSoon) {
                                                pendingChannel = ReminderChannel.WHATSAPP
                                                antiHarassmentWarningMember = item
                                            } else {
                                                memberToRemind = item
                                                editedMessage = viewModel.buildMessageForMember(item)
                                                pendingChannel = ReminderChannel.WHATSAPP
                                            }
                                        },
                                        enabled = !item.isPhoneMissing,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                                    ) {
                                        Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                                    }

                                    // SMS
                                    Button(
                                        onClick = {
                                            if (item.isPhoneMissing) return@Button
                                            val (isTooSoon, _) = ReminderHelper.checkAntiHarassment(
                                                item.lastReminderTimestamp,
                                                state.minReminderIntervalDays
                                            )
                                            if (isTooSoon) {
                                                pendingChannel = ReminderChannel.SMS
                                                antiHarassmentWarningMember = item
                                            } else {
                                                memberToRemind = item
                                                editedMessage = viewModel.buildMessageForMember(item)
                                                pendingChannel = ReminderChannel.SMS
                                            }
                                        },
                                        enabled = !item.isPhoneMissing,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("SMS", style = MaterialTheme.typography.labelSmall)
                                    }

                                    // Appel
                                    IconButton(
                                        onClick = {
                                            if (!item.isPhoneMissing) {
                                                launchReminderIntent(item, ReminderChannel.CALL, "")
                                            }
                                        },
                                        enabled = !item.isPhoneMissing
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Appeler")
                                    }

                                    // Régler
                                    IconButton(onClick = { onNavigateToPayment(item.member.id) }) {
                                        Icon(Icons.Default.Payment, contentDescription = "Régler")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Avertissement anti-harcèlement (relance < 7 jours)
    if (antiHarassmentWarningMember != null) {
        val target = antiHarassmentWarningMember!!
        val (_, days) = ReminderHelper.checkAntiHarassment(target.lastReminderTimestamp, state.minReminderIntervalDays)

        AlertDialog(
            onDismissRequest = { antiHarassmentWarningMember = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Relance rapprochée") },
            text = {
                Text(
                    "Ce membre a déjà été relancé il y a seulement $days jour(s). Le délai minimal recommandé est de ${state.minReminderIntervalDays} jours. Souhaitez-vous quand même poursuivre ?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = target
                        antiHarassmentWarningMember = null
                        memberToRemind = item
                        editedMessage = viewModel.buildMessageForMember(item)
                    }
                ) {
                    Text("Continuer")
                }
            },
            dismissButton = {
                TextButton(onClick = { antiHarassmentWarningMember = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Boîte de dialogue d'aperçu du message avant ouverture de l'application cible
    if (memberToRemind != null && pendingChannel != null) {
        val target = memberToRemind!!
        val channel = pendingChannel!!

        AlertDialog(
            onDismissRequest = {
                memberToRemind = null
                pendingChannel = null
            },
            title = { Text("Message de relance (${channel.name})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Destinataire : ${target.member.firstName} ${target.member.lastName} (${target.member.phone})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = editedMessage,
                        onValueChange = { editedMessage = it },
                        label = { Text("Texte du message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        launchReminderIntent(target, channel, editedMessage)
                        memberToRemind = null
                        pendingChannel = null
                    }
                ) {
                    Text("Ouvrir ${if (channel == ReminderChannel.WHATSAPP) "WhatsApp" else "SMS"}")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        memberToRemind = null
                        pendingChannel = null
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal de relance en série séquentielle
    if (state.isBatchQueueActive && state.lateMembers.isNotEmpty()) {
        val currentIndex = state.batchQueueIndex
        val currentItem = state.lateMembers.getOrNull(currentIndex)

        if (currentItem != null) {
            val message = remember(currentIndex) { viewModel.buildMessageForMember(currentItem) }

            AlertDialog(
                onDismissRequest = { viewModel.cancelBatchQueue() },
                title = { Text("Relance en série (${currentIndex + 1} / ${state.lateMembers.size})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${currentItem.member.lastName.uppercase()} ${currentItem.member.firstName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Montant dû : ${currentItem.totalDue} ${state.currency} · ${currentItem.member.phone.ifBlank { "Tél manquant" }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!currentItem.isPhoneMissing) {
                                launchReminderIntent(currentItem, ReminderChannel.WHATSAPP, message)
                            }
                            viewModel.nextBatchItem()
                        }
                    ) {
                        Text("Envoyer puis suivant")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { viewModel.nextBatchItem() }) {
                            Text("Passer")
                        }
                        TextButton(onClick = { viewModel.cancelBatchQueue() }) {
                            Text("Arrêter")
                        }
                    }
                }
            )
        }
    }
}
