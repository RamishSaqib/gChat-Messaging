package com.gchat.presentation.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gchat.domain.model.ExtractedEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Smart chip for displaying extracted entity data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartChip(
    entity: ExtractedEntity,
    onActionClick: (ExtractedEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label, actionText) = when (entity) {
        is ExtractedEntity.ActionItem -> Triple(
            Icons.Default.CheckCircle,
            entity.task,
            "Add to Tasks"
        )
        is ExtractedEntity.DateTime -> Triple(
            Icons.Default.Event,
            formatDateTime(entity.dateTime),
            "Add to Calendar"
        )
        is ExtractedEntity.Contact -> Triple(
            Icons.Default.Person,
            entity.name ?: entity.email ?: entity.phone ?: "Contact",
            "Save Contact"
        )
        is ExtractedEntity.Location -> Triple(
            Icons.Default.Place,
            entity.placeName ?: entity.address,
            "Open in Maps"
        )
    }
    
    AssistChip(
        onClick = { onActionClick(entity) },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = actionText,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            leadingIconContentColor = when (entity) {
                is ExtractedEntity.ActionItem -> MaterialTheme.colorScheme.primary
                is ExtractedEntity.DateTime -> MaterialTheme.colorScheme.secondary
                is ExtractedEntity.Contact -> MaterialTheme.colorScheme.tertiary
                is ExtractedEntity.Location -> MaterialTheme.colorScheme.error
            }
        ),
        modifier = modifier
    )
}

/**
 * Display multiple smart chips in a flow layout
 */
@Composable
fun SmartChipGroup(
    entities: List<ExtractedEntity>,
    onActionClick: (ExtractedEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entities.isEmpty()) return
    
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Group chips by type
        val actionItems = entities.filterIsInstance<ExtractedEntity.ActionItem>()
        val dateTimes = entities.filterIsInstance<ExtractedEntity.DateTime>()
        val contacts = entities.filterIsInstance<ExtractedEntity.Contact>()
        val locations = entities.filterIsInstance<ExtractedEntity.Location>()
        
        // Display each type
        if (actionItems.isNotEmpty()) {
            ChipRow(entities = actionItems, onActionClick = onActionClick)
        }
        if (dateTimes.isNotEmpty()) {
            ChipRow(entities = dateTimes, onActionClick = onActionClick)
        }
        if (contacts.isNotEmpty()) {
            ChipRow(entities = contacts, onActionClick = onActionClick)
        }
        if (locations.isNotEmpty()) {
            ChipRow(entities = locations, onActionClick = onActionClick)
        }
    }
}

@Composable
private fun ChipRow(
    entities: List<ExtractedEntity>,
    onActionClick: (ExtractedEntity) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        entities.take(2).forEach { entity ->
            SmartChip(
                entity = entity,
                onActionClick = onActionClick
            )
        }
        if (entities.size > 2) {
            Text(
                text = "+${entities.size - 2}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Format Unix timestamp to readable date/time
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Smart chip with more detailed information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedSmartChip(
    entity: ExtractedEntity,
    onActionClick: (ExtractedEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    when (entity) {
        is ExtractedEntity.ActionItem -> ActionItemChip(entity, onActionClick, modifier)
        is ExtractedEntity.DateTime -> DateTimeChip(entity, onActionClick, modifier)
        is ExtractedEntity.Contact -> ContactChip(entity, onActionClick, modifier)
        is ExtractedEntity.Location -> LocationChip(entity, onActionClick, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionItemChip(
    entity: ExtractedEntity.ActionItem,
    onActionClick: (ExtractedEntity) -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = { onActionClick(entity) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entity.task,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entity.priority != ExtractedEntity.ActionItem.Priority.MEDIUM) {
                        Text(
                            text = entity.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (entity.priority) {
                                ExtractedEntity.ActionItem.Priority.HIGH -> MaterialTheme.colorScheme.error
                                ExtractedEntity.ActionItem.Priority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    entity.dueDate?.let { dueDate ->
                        Text(
                            text = "Due: ${formatDateTime(dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Add to Tasks",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeChip(
    entity: ExtractedEntity.DateTime,
    onActionClick: (ExtractedEntity) -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = { onActionClick(entity) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDateTime(entity.dateTime),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (entity.isRange && entity.endDateTime != null) {
                    Text(
                        text = "to ${formatDateTime(entity.endDateTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                entity.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Add to Calendar",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactChip(
    entity: ExtractedEntity.Contact,
    onActionClick: (ExtractedEntity) -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = { onActionClick(entity) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                entity.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                entity.email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                entity.phone?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Save Contact",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationChip(
    entity: ExtractedEntity.Location,
    onActionClick: (ExtractedEntity) -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = { onActionClick(entity) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                entity.placeName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = entity.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open in Maps",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

