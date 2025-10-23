package com.gchat.presentation.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gchat.domain.model.CulturalContext
import com.gchat.domain.model.CulturalContextResult
import com.gchat.presentation.theme.GChatTheme

/**
 * Bottom sheet displaying cultural context (idioms, slang, cultural references) for a message.
 * 
 * Shows explanations for expressions that may not translate literally, including:
 * - The phrase/expression
 * - Literal translation (if applicable)
 * - Actual meaning
 * - Cultural context and usage notes
 * - Example usages
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CulturalContextBottomSheet(
    culturalContextResult: CulturalContextResult,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexts = culturalContextResult.contexts
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Cultural Context",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cultural Context",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Subtitle with language
        Text(
            text = "${contexts.size} expression${if (contexts.size == 1) "" else "s"} found in ${culturalContextResult.language}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // List of cultural contexts
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(contexts) { context ->
                CulturalContextItem(context = context)
            }
        }
    }
}

/**
 * Individual cultural context item card
 */
@Composable
fun CulturalContextItem(
    context: CulturalContext,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Phrase/Expression
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "\"${context.phrase}\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Literal Translation (if available)
            context.literalTranslation?.let { literal ->
                Column {
                    Text(
                        text = "Literal:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "\"$literal\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Actual Meaning
            Column {
                Text(
                    text = "Meaning:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = context.actualMeaning,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Cultural Context
            Column {
                Text(
                    text = "Context:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = context.culturalContext,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Examples (if available)
            if (context.examples.isNotEmpty()) {
                Column {
                    Text(
                        text = "Examples:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    context.examples.forEach { example ->
                        Text(
                            text = "• $example",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewCulturalContextBottomSheet() {
    GChatTheme {
        CulturalContextBottomSheet(
            culturalContextResult = CulturalContextResult(
                messageId = "msg123",
                language = "English",
                contexts = listOf(
                    CulturalContext(
                        phrase = "break a leg",
                        literalTranslation = "break your leg",
                        actualMeaning = "good luck",
                        culturalContext = "Common English idiom used before performances. Despite sounding negative, it's a traditional way to wish someone success.",
                        examples = listOf(
                            "Break a leg on your interview!",
                            "Break a leg at the concert tonight!"
                        )
                    ),
                    CulturalContext(
                        phrase = "piece of cake",
                        literalTranslation = "a slice of cake",
                        actualMeaning = "something very easy",
                        culturalContext = "Informal English expression suggesting that a task is as easy as eating cake.",
                        examples = listOf(
                            "This test was a piece of cake.",
                            "Don't worry, the installation is a piece of cake."
                        )
                    )
                ),
                cached = false
            ),
            onDismiss = {}
        )
    }
}

