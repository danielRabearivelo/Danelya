package com.association.caisse

import com.association.caisse.data.backup.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitaires pour la sérialisation, la vérification d'intégrité
 * et le rejet des sauvegardes corrompues.
 */
class BackupIntegrityTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testBackupPayloadSerializationRoundTrip() {
        val payload = BackupPayload(
            schemaVersion = 1,
            appVersion = "1.0.0",
            exportedAt = 1770000000000L,
            associationName = "Association Test",
            currency = "Ar",
            defaultPrefix = "+261",
            reminderTemplate = "Bonjour {prenom}",
            members = listOf(
                MemberBackupDto(1L, "Ranaivo", "Jean", "+261341122233", "2026-01", true, null, "Test")
            ),
            feeRates = listOf(FeeRateBackupDto(1L, "2026-01", 10000L)),
            payments = listOf(PaymentBackupDto(1L, 1L, "2026-01-15", 10000L, "Espèces")),
            allocations = listOf(AllocationBackupDto(1L, 1L, 1L, "2026-01", 10000L)),
            categories = listOf(CategoryBackupDto(1L, "Dons", true)),
            otherIncomes = emptyList(),
            remittances = emptyList(),
            reminderLogs = emptyList()
        )

        val jsonStr = json.encodeToString(payload)
        assertTrue(jsonStr.contains("Association Test"))
        assertTrue(jsonStr.contains("Ranaivo"))

        val decoded = json.decodeFromString<BackupPayload>(jsonStr)
        assertEquals(1, decoded.schemaVersion)
        assertEquals("Association Test", decoded.associationName)
        assertEquals(1, decoded.members.size)
        assertEquals("Ranaivo", decoded.members[0].lastName)
    }

    @Test
    fun testCorruptedJsonFailsGracefully() {
        val corruptedJson = "{ \"schemaVersion\": 1, \"members\": [ { \"id\": 1, invalid_json "

        var failed = false
        try {
            json.decodeFromString<BackupPayload>(corruptedJson)
        } catch (e: Exception) {
            failed = true
        }

        assertTrue("Le parseur JSON doit rejeter le flux corrompu", failed)
    }

    @Test
    fun testPreviewSummaryCalculation() {
        val payload = BackupPayload(
            schemaVersion = 1,
            appVersion = "1.0.0",
            exportedAt = System.currentTimeMillis(),
            associationName = "Association Test",
            currency = "Ar",
            defaultPrefix = "+261",
            reminderTemplate = "",
            members = listOf(
                MemberBackupDto(1L, "Ranaivo", "Jean", "", "2026-01", true),
                MemberBackupDto(2L, "Andria", "Hanta", "", "2026-01", true)
            ),
            feeRates = emptyList(),
            payments = listOf(
                PaymentBackupDto(1L, 1L, "2026-01-10", 10000L),
                PaymentBackupDto(2L, 2L, "2026-02-14", 10000L)
            ),
            allocations = emptyList(),
            categories = emptyList(),
            otherIncomes = emptyList(),
            remittances = emptyList(),
            reminderLogs = emptyList()
        )

        assertEquals(2, payload.members.size)
        assertEquals(2, payload.payments.size)
        val latestDate = payload.payments.maxOf { it.date }
        assertEquals("2026-02-14", latestDate)
    }
}
