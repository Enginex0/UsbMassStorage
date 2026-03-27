package com.enginex0.usbmassstorage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.enginex0.usbmassstorage.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_how_to_use)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionTitle(stringResource(R.string.guide_what_title)) }
            item { Body(stringResource(R.string.guide_what_body)) }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionTitle(stringResource(R.string.guide_before_title)) }
            item { Bullet(stringResource(R.string.guide_before_1)) }
            item { Bullet(stringResource(R.string.guide_before_2)) }
            item { Bullet(stringResource(R.string.guide_before_3)) }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionTitle(stringResource(R.string.guide_mount_title)) }
            item { Step(1, stringResource(R.string.guide_mount_1)) }
            item { Step(2, stringResource(R.string.guide_mount_2)) }
            item { Step(3, stringResource(R.string.guide_mount_3)) }
            item { Step(4, stringResource(R.string.guide_mount_4)) }
            item { Step(5, stringResource(R.string.guide_mount_5)) }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionTitle(stringResource(R.string.guide_create_title)) }
            item { Step(1, stringResource(R.string.guide_create_1)) }
            item { Step(2, stringResource(R.string.guide_create_2)) }
            item { Step(3, stringResource(R.string.guide_create_3)) }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionTitle(stringResource(R.string.guide_multi_title)) }
            item { Body(stringResource(R.string.guide_multi_body)) }

            item { Spacer(Modifier.height(8.dp)) }
            item { SectionTitle(stringResource(R.string.guide_issues_title)) }
            item { TroubleshootItem(stringResource(R.string.guide_issue_daemon), stringResource(R.string.guide_issue_daemon_fix)) }
            item { TroubleshootItem(stringResource(R.string.guide_issue_root), stringResource(R.string.guide_issue_root_fix)) }
            item { TroubleshootItem(stringResource(R.string.guide_issue_usb), stringResource(R.string.guide_issue_usb_fix)) }
            item { TroubleshootItem(stringResource(R.string.guide_issue_appfuse), stringResource(R.string.guide_issue_appfuse_fix)) }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Bullet(text: String) {
    Text("\u2022  $text", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Step(number: Int, text: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$number. ") }
            append(text)
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun TroubleshootItem(problem: String, solution: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(problem) }
            append(": $solution")
        },
        style = MaterialTheme.typography.bodyMedium
    )
}
