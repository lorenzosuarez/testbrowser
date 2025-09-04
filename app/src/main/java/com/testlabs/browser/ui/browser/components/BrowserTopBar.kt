package com.testlabs.browser.ui.browser.components

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.testlabs.browser.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopBar(
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

    LaunchedEffect(shouldFocusUrlInput) {
        if (shouldFocusUrlInput) focusRequester.requestFocus()
    }

    val handleSubmit = {
        onSubmit()
        focusManager.clearFocus()
    }
    val handleClear = {
        onUrlChanged("")
        focusRequester.requestFocus()
    }

    TopAppBar(
        title = {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChanged,
                placeholder = {
                    androidx.compose.material3.Text(
                        text = stringResource(R.string.browser_url_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    if (url.isNotEmpty()) {
                        CompositionLocalProvider(value = LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            IconButton(
                                onClick = handleClear,
                                modifier = Modifier.size(dimensionResource(R.dimen.browser_clear_icon_size))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(R.string.browser_clear_search),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.browser_topbar_vertical_padding))
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { handleSubmit() }),
                shape = RoundedCornerShape(dimensionResource(R.dimen.browser_pill_radius)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            CompositionLocalProvider(value = LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(dimensionResource(R.dimen.browser_menu_icon_size)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.browser_menu),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}
