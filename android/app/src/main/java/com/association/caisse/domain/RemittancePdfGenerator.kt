package com.association.caisse.domain

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.association.caisse.data.local.entity.RemittanceEntity
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Générateur d'état de versement PDF 100% natif Android (PdfDocument).
 * Aucune dépendance tierce requise.
 */
object RemittancePdfGenerator {

    fun generateRemittanceStatementPdf(
        context: Context,
        outputUri: Uri,
        associationName: String,
        periodLabel: String,
        totalFees: Long,
        totalOtherIncomes: Long,
        remittances: List<RemittanceEntity>,
        currency: String = "Ar"
    ) {
        val pdfDocument = PdfDocument()

        // Page A4 standard : 595 x 842 points (72 DPI)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var y = 50f

        // 1. En-tête
        canvas.drawText(associationName.uppercase(), 40f, y, titlePaint)
        y += 20f
        canvas.drawText("ÉTAT DE VERSEMENT DE LA CAISSE EN ESPÈCES", 40f, y, boldPaint)
        y += 16f

        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        canvas.drawText("Période : $periodLabel  |  Édité le : $todayStr", 40f, y, textPaint)
        y += 15f
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
        y += 25f

        // 2. Récapitulatif des encaissements
        canvas.drawText("1. SITUATION DES ENCAISSEMENTS (ESPÈCES)", 40f, y, boldPaint)
        y += 18f

        val totalCollected = totalFees + totalOtherIncomes

        canvas.drawText("• Total des cotisations des membres :", 50f, y, textPaint)
        canvas.drawText("$totalFees $currency", 380f, y, textPaint)
        y += 16f

        canvas.drawText("• Total des autres recettes (dons, amendes, etc.) :", 50f, y, textPaint)
        canvas.drawText("$totalOtherIncomes $currency", 380f, y, textPaint)
        y += 18f

        canvas.drawLine(50f, y, 480f, y, linePaint)
        y += 16f
        canvas.drawText("TOTAL GÉNÉRAL ENCAISSÉ :", 50f, y, boldPaint)
        canvas.drawText("$totalCollected $currency", 380f, y, boldPaint)
        y += 35f

        // 3. Versements effectués
        canvas.drawText("2. VERSEMENTS EFFECTUÉS À LA CAISSE PRINCIPALE", 40f, y, boldPaint)
        y += 18f

        // En-tête du tableau des versements
        canvas.drawText("Date", 50f, y, boldPaint)
        canvas.drawText("Destinataire", 130f, y, boldPaint)
        canvas.drawText("Période couverte", 290f, y, boldPaint)
        canvas.drawText("Montant versé", 420f, y, boldPaint)
        y += 6f
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
        y += 16f

        var totalRemitted = 0L

        if (remittances.isEmpty()) {
            canvas.drawText("Aucun versement enregistré sur cette période.", 50f, y, textPaint)
            y += 20f
        } else {
            for (r in remittances) {
                totalRemitted += r.amount
                canvas.drawText(r.date, 50f, y, textPaint)
                canvas.drawText(r.recipient.take(22), 130f, y, textPaint)
                canvas.drawText((r.coveredPeriod ?: "-").take(18), 290f, y, textPaint)
                canvas.drawText("${r.amount} $currency", 420f, y, boldPaint)
                y += 18f

                if (y > pageHeight - 160) break // Évite le débordement
            }
        }

        y += 10f
        canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
        y += 18f

        canvas.drawText("TOTAL DES FONDS VERSÉS :", 50f, y, boldPaint)
        canvas.drawText("$totalRemitted $currency", 420f, y, boldPaint)
        y += 30f

        // 4. Solde restant à verser
        val remainingToRemit = totalCollected - totalRemitted
        val boxPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRect(40f, y, (pageWidth - 40).toFloat(), y + 42f, boxPaint)
        y += 26f

        canvas.drawText("SOLDE RESTANT EN CAISSE DU TRÉSORIER :", 55f, y, boldPaint)
        canvas.drawText("$remainingToRemit $currency", 400f, y, titlePaint)
        y += 50f

        // 5. Cadres de signatures
        val signatureBoxWidth = 220f
        val signatureBoxHeight = 80f

        // Signature Trésorier
        canvas.drawRect(50f, y, 50f + signatureBoxWidth, y + signatureBoxHeight, boxPaint)
        canvas.drawText("Visa & Signature du Trésorier :", 55f, y + 15f, boldPaint)
        canvas.drawText("(Atteste l'exactitude des espèces remises)", 55f, y + 30f, textPaint)

        // Signature Destinataire
        val destX = (pageWidth - 50 - signatureBoxWidth).toFloat()
        canvas.drawRect(destX, y, destX + signatureBoxWidth, y + signatureBoxHeight, boxPaint)
        canvas.drawText("Visa & Signature du Destinataire :", destX + 5f, y + 15f, boldPaint)
        canvas.drawText("(Atteste la bonne réception des fonds)", destX + 5f, y + 30f, textPaint)

        pdfDocument.finishPage(page)

        try {
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream: OutputStream ->
                pdfDocument.writeTo(outputStream)
            }
        } finally {
            pdfDocument.close()
        }
    }
}
