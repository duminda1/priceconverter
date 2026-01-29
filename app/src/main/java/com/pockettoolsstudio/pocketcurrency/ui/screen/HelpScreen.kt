package com.pockettoolsstudio.pocketcurrency.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.pockettoolsstudio.pocketcurrency.R

private val orderedListRegex = Regex("^\\d+\\.\\s+")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavHostController) {
    val context = LocalContext.current
    val helpText = remember { loadHelpText(context) }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HelpContent(helpText)
        }
    }
}

@Composable
private fun HelpContent(helpText: String) {
    val lines = remember(helpText) { helpText.lines() }
    val bodyStyle = MaterialTheme.typography.bodyLarge

    lines.forEach { rawLine ->
        val line = rawLine.trimEnd()
        when {
            line.isBlank() -> {
                Spacer(modifier = Modifier.height(6.dp))
            }
            line.startsWith("# ") -> {
                Text(
                    cleanInlineMarkdown(line.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            line.startsWith("## ") -> {
                Text(
                    cleanInlineMarkdown(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            line.startsWith("### ") -> {
                Text(
                    cleanInlineMarkdown(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            line.startsWith("---") -> {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            line.startsWith("- ") -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(stringResource(R.string.list_bullet), style = bodyStyle)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(cleanInlineMarkdown(line.removePrefix("- ")), style = bodyStyle)
                }
            }
            orderedListRegex.containsMatchIn(line) -> {
                val match = orderedListRegex.find(line)
                val number = match?.value?.trim().orEmpty()
                val rest = line.removePrefix(match?.value.orEmpty()).trimStart()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(number, style = bodyStyle)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(cleanInlineMarkdown(rest), style = bodyStyle)
                }
            }
            else -> {
                Text(cleanInlineMarkdown(line), style = bodyStyle)
            }
        }
    }
}

private fun cleanInlineMarkdown(text: String): String {
    return text.replace("**", "")
}

private fun loadHelpText(context: Context): String {
    return context.resources.openRawResource(R.raw.help_pocketcurrency)
        .bufferedReader()
        .use { it.readText() }
}
