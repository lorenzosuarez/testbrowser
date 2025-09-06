package com.testlabs.browser.ui.browser.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.testlabs.browser.R
import com.testlabs.browser.ui.theme.BrowserThemeTokens
import com.testlabs.browser.ui.theme.TestBrowserTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BrowserTopBar(
    modifier: Modifier = Modifier,
    url: String,
    onUrlChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onMenuClick: () -> Unit = {},
    focusRequester: FocusRequester,
    onEditingChange: (Boolean) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(url)) }
    var lastFocusState by remember { mutableStateOf(false) }
    val barColors = BrowserThemeTokens.barColors()
    val omni = BrowserThemeTokens.omniboxColors()

    LaunchedEffect(url) {
        if (textFieldValue.text != url) {
            textFieldValue = TextFieldValue(url)
        }
    }

    val handleSubmit = {
        onSubmit()
        onEditingChange(false)
        focusManager.clearFocus()
    }

    val handleClear: () -> Unit = {
        textFieldValue = TextFieldValue("")
        onUrlChanged("")
        focusRequester.requestFocus()
    }

    val handleValueChange = { newValue: TextFieldValue ->
        textFieldValue = newValue
        onUrlChanged(newValue.text)
    }

    TopAppBar(
        modifier = modifier,
        title = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = handleValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.browser_topbar_height))
                    .offset(x = (-6).dp)
                    .padding(
                        top = dimensionResource(id = R.dimen.browser_topbar_vertical_padding),
                        bottom = dimensionResource(id = R.dimen.browser_topbar_vertical_padding),
                        end = dimensionResource(id = R.dimen.browser_topbar_horizontal_padding)
                    )
                    .focusRequester(focusRequester)
                    .onFocusChanged { f ->
                        if (f.isFocused != lastFocusState) {
                            lastFocusState = f.isFocused
                            onEditingChange(f.isFocused)
                            if (f.isFocused) {
                                textFieldValue = TextFieldValue(textFieldValue.text, selection = TextRange(0, textFieldValue.text.length))
                            }
                        }
                    },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.browser_url_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.size(dimensionResource(id = R.dimen.browser_menu_icon_size)),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = stringResource(id = R.string.browser_menu)
                            )
                        }
                    }
                },
                trailingIcon = {
                    if (textFieldValue.text.isNotEmpty()) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                            IconButton(
                                onClick = handleClear,
                                modifier = Modifier.size(dimensionResource(id = R.dimen.browser_clear_icon_size)),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = stringResource(id = R.string.browser_clear_search)
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
                    focusedContainerColor = omni.container,
                    unfocusedContainerColor = omni.container,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = omni.content,
                    unfocusedTextColor = omni.content,
                    focusedPlaceholderColor = omni.placeholder,
                    unfocusedPlaceholderColor = omni.placeholder,
                    focusedLeadingIconColor = omni.content,
                    unfocusedLeadingIconColor = omni.content,
                    focusedTrailingIconColor = omni.trailingIcon,
                    unfocusedTrailingIconColor = omni.trailingIcon,
                    cursorColor = omni.content
                ),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = barColors.container,
            scrolledContainerColor = barColors.container,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun BrowserTopBarPreview() {
    TestBrowserTheme {
        val focus = remember { FocusRequester() }
        BrowserTopBar(
            url = "browserscan.net",
            onUrlChanged = {},
            onSubmit = {},
            onMenuClick = {},
            focusRequester = focus,
            onEditingChange = {}
        )
    }
}