package com.carlens.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carlens.domain.calc.CostSummarizer
import com.carlens.domain.calc.Units
import com.carlens.domain.model.OdometerReading
import com.carlens.domain.model.OdometerSource

@Composable
fun OdometerHistoryDialog(
    readings: List<OdometerReading>,
    onDeleteManualReading: (OdometerReading) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSource by remember { mutableStateOf<OdometerSource?>(null) }

    val deltaByReadingId = remember(readings) {
        val chronological = readings.sortedWith(
            compareBy<OdometerReading> { it.time.epochMillis }.thenBy { it.km }
        )
        val map = mutableMapOf<String, Long>()
        for (i in 1 until chronological.size) {
            val curr = chronological[i]
            val prev = chronological[i - 1]
            if (!curr.startsNewSegment && curr.km >= prev.km) {
                map[curr.id] = curr.km - prev.km
            }
        }
        map
    }

    val totalTrackedKm = remember(readings) {
        CostSummarizer.totalTrackedDistanceKm(readings)
    }

    val filteredReadings = remember(readings, selectedSource) {
        if (selectedSource == null) readings
        else readings.filter { it.source == selectedSource }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Odometer Log (${readings.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (totalTrackedKm != null && totalTrackedKm > 0L) {
                    Text(
                        text = "Tracked distance: ${Units.formatKm(totalTrackedKm)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            if (readings.isEmpty()) {
                Text(
                    text = "No odometer readings recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSource == null,
                            onClick = { selectedSource = null },
                            label = { Text("All (${readings.size})") }
                        )
                        OdometerSource.values().forEach { src ->
                            val count = readings.count { it.source == src }
                            if (count > 0) {
                                FilterChip(
                                    selected = selectedSource == src,
                                    onClick = {
                                        selectedSource = if (selectedSource == src) null else src
                                    },
                                    label = {
                                        Text("${src.name.lowercase().replaceFirstChar { it.uppercase() }} ($count)")
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredReadings, key = { it.id }) { r ->
                            val deltaKm = deltaByReadingId[r.id]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = Units.formatKm(r.km),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = r.source.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                            if (r.startsNewSegment) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "NEW SEGMENT",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                }
                                            }
                                        }
                                        val deltaText = if (deltaKm != null && deltaKm > 0L) " · +${Units.formatKm(deltaKm)}" else ""
                                        Text(
                                            text = "${r.time.localDate}$deltaText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (r.source == OdometerSource.MANUAL) {
                                        IconButton(
                                            onClick = { onDeleteManualReading(r) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete manual reading",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
