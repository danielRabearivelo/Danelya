package com.association.caisse.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Graphique en barres 100% natif en Compose Canvas.
 * Affiche la distribution des encaissements sur les 12 mois de l'année
 * sans aucune dépendance tierce.
 */
@Composable
fun SimpleMonthlyBarChart(
    monthlyAmounts: List<Long>, // 12 valeurs correspondant aux mois 1 à 12
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    emptyBarColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val maxAmount = (monthlyAmounts.maxOrNull() ?: 1L).coerceAtLeast(1L)
    val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = 12
            val spacing = 8.dp.toPx()
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = (canvasWidth - totalSpacing) / barCount

            for (i in 0 until barCount) {
                val amount = monthlyAmounts.getOrElse(i) { 0L }
                val heightRatio = (amount.toFloat() / maxAmount.toFloat()).coerceIn(0.04f, 1f)
                val barHeight = canvasHeight * heightRatio
                val x = spacing + i * (barWidth + spacing)
                val y = canvasHeight - barHeight

                val color = if (amount > 0) barColor else emptyBarColor

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Étiquettes des mois sous les barres
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            monthLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
