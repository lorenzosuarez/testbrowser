/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.testlabs.browser.R
import com.testlabs.browser.ui.theme.BrowserThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BrowserTopBar(
    modifier: Modifier = Modifier,
    url: String,
    onUrlChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onMenuClick: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
    shouldFocusUrlInput: Boolean = false,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var shouldRequestFocus by remember { mutableStateOf(false) }
    val barColors = BrowserThemeTokens.barColors()

    LaunchedEffect(key1 = shouldFocusUrlInput) {
        if (shouldFocusUrlInput) focusRequester.requestFocus()
    }

    LaunchedEffect(key1 = shouldRequestFocus) {
        if (shouldRequestFocus) {
            focusRequester.requestFocus()
            shouldRequestFocus = false
        }
    }

    val handleSubmit = {
        onSubmit()
        focusManager.clearFocus()
    }
    val handleClear: () -> Unit = {
        onUrlChanged("")
        shouldRequestFocus = true
    }


    TopAppBar(
        title = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = (-6).dp)
                    .padding(end = dimensionResource(id = R.dimen.browser_topbar_horizontal_padding))
                    .focusRequester(focusRequester),
                value = url,
                onValueChange = onUrlChanged,
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.browser_url_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                },
                leadingIcon = {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.size(dimensionResource(id = R.dimen.browser_menu_icon_size)),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(id = R.string.browser_menu),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            IconButton(
                                onClick = handleClear,
                                modifier = Modifier.size(dimensionResource(id = R.dimen.browser_clear_icon_size)),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(id = R.string.browser_clear_search),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { handleSubmit() }),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.browser_pill_radius)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barColors.container,
            scrolledContainerColor = barColors.container,
            navigationIconContentColor = barColors.content,
            titleContentColor = barColors.content,
            actionIconContentColor = barColors.content,
        ),
    )
}
