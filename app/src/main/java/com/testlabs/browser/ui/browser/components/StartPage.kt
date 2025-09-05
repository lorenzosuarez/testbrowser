package com.testlabs.browser.ui.browser.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.testlabs.browser.R

private data class RecommendedTarget(
    val title: String,
    val description: String,
    val url: String,
)

@Composable
internal fun StartPage(
    visible: Boolean,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.start_page_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(id = R.string.start_page_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
                val items = listOf(
                    RecommendedTarget(
                        title = stringResource(id = R.string.start_page_scan_title),
                        description = stringResource(id = R.string.start_page_scan_desc),
                        url = "https://www.browserscan.net",
                    ),
                    RecommendedTarget(
                        title = stringResource(id = R.string.start_page_headers_title),
                        description = stringResource(id = R.string.start_page_headers_desc),
                        url = "https://httpbin.org/headers",
                    ),
                )
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(items) { target ->
                        RecommendationCard(target = target, onOpenUrl = onOpenUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    target: RecommendedTarget,
    onOpenUrl: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "start-recommendation-${target.title}" }
            .clickable { onOpenUrl(target.url) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = target.description,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = target.url,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary),
            )
            Button(
                onClick = { onOpenUrl(target.url) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(text = stringResource(id = R.string.start_page_open))
            }
        }
    }
}
