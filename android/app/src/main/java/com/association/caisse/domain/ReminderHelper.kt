package com.association.caisse.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ReminderHelper {

    /**
     * Normalise un numéro de téléphone selon les règles spécifiées :
     * 1. Nettoie les espaces, tirets, parenthèses et points.
     * 2. Si le numéro ne contient pas d'indicatif international (+ ou 00) :
     *    - applique l'indicatif par défaut configuré (ex. +261 pour Madagascar ou +33 pour la France)
     *    - supprime le zéro initial s'il existe (ex. "034 11 222 33" -> "+261341122233").
     */
    fun normalizePhoneNumber(rawPhone: String, defaultPrefix: String = "+261"): String {
        val cleaned = rawPhone.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
        if (cleaned.isBlank()) return ""

        if (cleaned.startsWith("+")) {
            return cleaned
        }
        if (cleaned.startsWith("00")) {
            return "+" + cleaned.substring(2)
        }

        val prefix = if (defaultPrefix.startsWith("+")) defaultPrefix else "+$defaultPrefix"
        val withoutLeadingZero = if (cleaned.startsWith("0")) cleaned.substring(1) else cleaned
        return "$prefix$withoutLeadingZero"
    }

    /**
     * Remplace dynamiquement les variables dans le modèle de relance :
     * {prenom}, {nom}, {montant_du}, {mois_impayes}, {association}, {devise}
     */
    fun buildReminderMessage(
        template: String,
        firstName: String,
        lastName: String,
        amountDueFormatted: String,
        unpaidMonthsFormatted: String,
        associationName: String,
        currency: String
    ): String {
        return template
            .replace("{prenom}", firstName)
            .replace("{nom}", lastName)
            .replace("{montant_du}", amountDueFormatted)
            .replace("{mois_impayes}", unpaidMonthsFormatted)
            .replace("{association}", associationName)
            .replace("{devise}", currency)
    }

    /**
     * Construit l'URL universelle pour ouvrir WhatsApp avec le message pré-rempli.
     */
    fun buildWhatsAppUrl(normalizedPhone: String, message: String): String {
        val phoneWithoutPlus = normalizedPhone.removePrefix("+")
        val encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        return "https://wa.me/$phoneWithoutPlus?text=$encodedMessage"
    }

    /**
     * Crée une intention Android pour ouvrir l'application SMS par défaut avec le message pré-rempli.
     * Utilise ACTION_SENDTO avec schéma "smsto:" sans nécessiter la permission dangereuse SEND_SMS.
     */
    fun createSmsIntent(normalizedPhone: String, message: String): Intent {
        val uri = Uri.parse("smsto:$normalizedPhone")
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Crée une intention pour ouvrir le composeur téléphonique (ACTION_DIAL).
     */
    fun createDialIntent(normalizedPhone: String): Intent {
        val uri = Uri.parse("tel:$normalizedPhone")
        return Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Vérifie la règle anti-harcèlement (délai minimal entre deux relances, 7 jours par défaut).
     * @return Pair(isTooSoon: Boolean, daysSince: Long?)
     */
    fun checkAntiHarassment(lastReminderTimestamp: Long?, minIntervalDays: Int = 7): Pair<Boolean, Long?> {
        if (lastReminderTimestamp == null) {
            return Pair(false, null)
        }
        val diffMs = System.currentTimeMillis() - lastReminderTimestamp
        val diffDays = diffMs / (1000L * 60 * 60 * 24)

        return if (diffDays < minIntervalDays) {
            Pair(true, diffDays)
        } else {
            Pair(false, diffDays)
        }
    }

    /**
     * Calcule le libellé textuel de la date de la dernière relance.
     * Exemples : "Jamais relancé", "Relancé aujourd'hui", "Relancé il y a 3 jours".
     */
    fun formatLastReminderStatus(lastReminderTimestamp: Long?): String {
        if (lastReminderTimestamp == null) return "Jamais relancé"
        val diffDays = (System.currentTimeMillis() - lastReminderTimestamp) / (1000L * 60 * 60 * 24)
        return when {
            diffDays == 0L -> "Relancé aujourd'hui"
            diffDays == 1L -> "Relancé hier"
            else -> "Relancé il y a $diffDays jours"
        }
    }
}
