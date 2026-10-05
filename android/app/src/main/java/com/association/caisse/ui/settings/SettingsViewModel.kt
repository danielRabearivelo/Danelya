package com.association.caisse.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.association.caisse.data.backup.BackupManager
import com.association.caisse.data.backup.BackupPayload
import com.association.caisse.data.backup.BackupPreviewSummary
import com.association.caisse.data.local.dao.FeeRateDao
import com.association.caisse.data.local.entity.MonthlyFeeRateEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val associationName: String = "Notre Association",
    val currency: String = "Ar",
    val defaultCountryPrefix: String = "+261",
    val reminderMessageTemplate: String = "Bonjour {prenom}, sauf erreur de notre part, il vous reste {montant_du} de cotisation à régler pour : {mois_impayes}. Merci ! – {association}",
    val feeRates: List<MonthlyFeeRateEntity> = emptyList(),
    val lastBackupDate: String? = null,
    val isBackupOverdue: Boolean = false,
    val backupPreview: BackupPreviewSummary? = null,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val feeRateDao: FeeRateDao,
    private val backupManager: BackupManager
) : ViewModel() {

    private val prefs = context.getSharedPreferences("caisse_settings", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val name = prefs.getString("association_name", "Notre Association") ?: "Notre Association"
        val currency = prefs.getString("currency", "Ar") ?: "Ar"
        val prefix = prefs.getString("default_prefix", "+261") ?: "+261"
        val template = prefs.getString(
            "reminder_template",
            "Bonjour {prenom}, sauf erreur de notre part, il vous reste {montant_du} de cotisation à régler pour : {mois_impayes}. Merci ! – {association}"
        ) ?: ""
        val lastBackupTime = prefs.getLong("last_manual_backup_time", 0L)

        val lastBackupStr = if (lastBackupTime > 0) {
            Instant.ofEpochMilli(lastBackupTime)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } else {
            null
        }

        val isOverdue = lastBackupTime == 0L || (System.currentTimeMillis() - lastBackupTime) > (30L * 24 * 3600 * 1000)

        _uiState.update {
            it.copy(
                associationName = name,
                currency = currency,
                defaultCountryPrefix = prefix,
                reminderMessageTemplate = template,
                lastBackupDate = lastBackupStr,
                isBackupOverdue = isOverdue
            )
        }

        viewModelScope.launch {
            feeRateDao.getAllRates().collect { rates ->
                _uiState.update { it.copy(feeRates = rates) }
            }
        }
    }

    fun saveGeneralSettings(name: String, currency: String, prefix: String, template: String) {
        prefs.edit()
            .putString("association_name", name.trim())
            .putString("currency", currency.trim())
            .putString("default_prefix", prefix.trim())
            .putString("reminder_template", template.trim())
            .apply()

        _uiState.update {
            it.copy(
                associationName = name.trim(),
                currency = currency.trim(),
                defaultCountryPrefix = prefix.trim(),
                reminderMessageTemplate = template.trim(),
                feedbackMessage = "Paramètres enregistrés avec succès."
            )
        }
    }

    fun addFeeRate(effectiveMonth: String, amount: Long) {
        viewModelScope.launch {
            feeRateDao.insertRate(MonthlyFeeRateEntity(effectiveFromMonth = effectiveMonth, amount = amount))
            _uiState.update { it.copy(feedbackMessage = "Nouveau tarif mensuel enregistré.") }
        }
    }

    suspend fun prepareBackupPayload(): BackupPayload {
        return backupManager.createBackupPayload(
            associationName = _uiState.value.associationName,
            currency = _uiState.value.currency,
            defaultPrefix = _uiState.value.defaultCountryPrefix,
            reminderTemplate = _uiState.value.reminderMessageTemplate
        )
    }

    fun exportBackupJson(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, errorMessage = null) }
            val payload = prepareBackupPayload()
            val success = backupManager.exportJsonToUri(context, uri, payload)
            if (success) {
                recordSuccessfulBackup()
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        feedbackMessage = "Sauvegarde JSON exportée avec succès !"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        errorMessage = "Échec de l'exportation du fichier de sauvegarde."
                    )
                }
            }
        }
    }

    fun exportDatabaseFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, errorMessage = null) }
            val success = backupManager.copyDatabaseFileToUri(context, uri)
            if (success) {
                recordSuccessfulBackup()
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        feedbackMessage = "Copie de la base SQLite exportée avec succès !"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBackingUp = false,
                        errorMessage = "Impossible de copier la base de données."
                    )
                }
            }
        }
    }

    fun inspectBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, errorMessage = null) }
            val summary = backupManager.inspectBackupFile(context, uri)
            _uiState.update { it.copy(isRestoring = false, backupPreview = summary) }
        }
    }

    fun dismissPreview() {
        _uiState.update { it.copy(backupPreview = null) }
    }

    fun confirmRestore() {
        val payload = _uiState.value.backupPreview?.rawPayload ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true, backupPreview = null) }
            val success = backupManager.restoreFromPayload(payload)
            if (success) {
                // Mettre à jour les paramètres importés
                saveGeneralSettings(
                    payload.associationName,
                    payload.currency,
                    payload.defaultPrefix,
                    payload.reminderTemplate
                )
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        feedbackMessage = "Restauration terminée ! Données restaurées et statuts recalculés."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = "Erreur critique lors de la restauration des données."
                    )
                }
            }
        }
    }

    private fun recordSuccessfulBackup() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("last_manual_backup_time", now).apply()
        val nowStr = Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        _uiState.update { it.copy(lastBackupDate = nowStr, isBackupOverdue = false) }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }
}
