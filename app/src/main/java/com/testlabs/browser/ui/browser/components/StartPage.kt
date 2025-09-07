/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.ui.browser.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.testlabs.browser.R

private data class RecommendedTarget(
    val title: String,
    val description: String,
    val url: String,
    val icon: ImageVector,
)

@Composable
internal fun StartPage(
    visible: Boolean,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = dimensionResource(R.dimen.start_page_horizontal_padding),
                        vertical = dimensionResource(R.dimen.start_page_vertical_padding)
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = dimensionResource(R.dimen.start_page_content_max_width))
                        .padding(bottom = dimensionResource(R.dimen.start_page_title_bottom_margin)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.start_page_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(id = R.string.start_page_subtitle),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.spacing_medium),
                            bottom = dimensionResource(R.dimen.spacing_large)
                        ),
                        textAlign = TextAlign.Center,
                    )
                }

                val items = listOf(
                    RecommendedTarget(
                        title = stringResource(id = R.string.start_page_scan_title),
                        description = stringResource(id = R.string.start_page_scan_desc),
                        url = "https://www.browserscan.net",
                        icon = Icons.Outlined.Search,
                    ),
                    RecommendedTarget(
                        title = stringResource(id = R.string.start_page_fingerprint_title),
                        description = stringResource(id = R.string.start_page_fingerprint_desc),
                        url = "https://fingerprintjs.github.io/fingerprintjs/",
                        icon = Icons.Outlined.Settings,
                    ),
                    RecommendedTarget(
                        title = stringResource(id = R.string.start_page_tls_title),
                        description = stringResource(id = R.string.start_page_tls_desc),
                        url = "https://tls.peet.ws/api/all",
                        icon = Icons.Outlined.Lock,
                    ),
                    RecommendedTarget(
                        title = stringResource(id = R.string.start_page_headers_title),
                        description = stringResource(id = R.string.start_page_headers_desc),
                        url = "https://httpbin.org/headers",
                        icon = Icons.Outlined.CheckCircle,
                    ),
                )

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = dimensionResource(R.dimen.start_page_content_max_width))
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.start_page_card_spacing)),
                    contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.start_page_list_content_padding)),
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
    val isDarkTheme = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "start-recommendation-${target.title}" }
            .clickable { onOpenUrl(target.url) },
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f)
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = dimensionResource(R.dimen.start_page_card_elevation)
        ),
        shape = RoundedCornerShape(dimensionResource(R.dimen.start_page_card_corner_radius)),
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.start_page_card_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
            ) {
                Icon(
                    imageVector = target.icon,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_size_large)),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = target.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = target.description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_small))
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.start_page_divider_margin)),
                color = if (isDarkTheme) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = target.url,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { onOpenUrl(target.url) },
                    modifier = Modifier.padding(start = dimensionResource(R.dimen.start_page_button_margin_top)),
                ) {
                    Text(
                        text = stringResource(id = R.string.start_page_open),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
