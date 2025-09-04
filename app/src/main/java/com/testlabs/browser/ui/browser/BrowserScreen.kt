package com.testlabs.browser.ui.browser

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.presentation.browser.BrowserEffect
import com.testlabs.browser.presentation.browser.BrowserIntent
import com.testlabs.browser.presentation.browser.BrowserState
import com.testlabs.browser.presentation.browser.BrowserViewModel
import com.testlabs.browser.ui.browser.components.BrowserBottomBar
import com.testlabs.browser.ui.browser.components.BrowserProgressIndicator
import com.testlabs.browser.ui.browser.components.BrowserTopBar
import com.testlabs.browser.ui.browser.components.BrowserSettingsDialog
import org.koin.androidx.compose.koinViewModel

/**
 * Browser screen using Material3 Scaffold with custom TopBar and BottomBar components.
 * The TopBar includes URL input and collapses/expands with scroll while the BottomBar
 * remains fixed with navigation controls and FAB for new tabs.
 *
 * Since WebView does not participate in Compose nested scroll, this bridges
 * WebView pixel deltas into the TopAppBar scroll behavior only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BrowserScreen(
    filePickerLauncher: ActivityResultLauncher<Intent>,
    uaProvider: UAProvider,
    viewModel: BrowserViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val topScroll = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    var webController by remember { mutableStateOf<WebViewController?>(value = null) }

    LaunchedEffect(state.shouldFocusUrlInput) {
        if (state.shouldFocusUrlInput) {
            topScroll.state.heightOffset = 0f
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BrowserEffect.LoadUrl -> webController?.loadUrl(effect.url.value)
                BrowserEffect.ReloadPage -> webController?.reload()
                BrowserEffect.NavigateBack -> webController?.goBack()
                BrowserEffect.NavigateForward -> webController?.goForward()
                is BrowserEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val callbacks = remember(viewModel) {
        object {
            val onProgressChanged: (Float) -> Unit = { viewModel.handleIntent(BrowserIntent.ProgressChanged(it)) }
            val onPageStarted: (String) -> Unit = { viewModel.handleIntent(BrowserIntent.PageStarted(ValidatedUrl.fromValidUrl(it))) }
            val onPageFinished: (String) -> Unit = { viewModel.handleIntent(BrowserIntent.PageFinished(ValidatedUrl.fromValidUrl(it))) }
            val onTitleChanged: (String) -> Unit = { viewModel.handleIntent(BrowserIntent.TitleChanged(it)) }
            val onNavigationStateChanged: (Boolean, Boolean) -> Unit = { b, f -> viewModel.handleIntent(BrowserIntent.NavigationStateChanged(b, f)) }
            val onError: (String) -> Unit = { viewModel.handleIntent(BrowserIntent.NavigationError(it)) }
            val onControllerReady: (WebViewController) -> Unit = { webController = it }
            val onBack: () -> Unit = { viewModel.handleIntent(BrowserIntent.GoBack) }
            val onForward: () -> Unit = { viewModel.handleIntent(BrowserIntent.GoForward) }
            val onReload: () -> Unit = { viewModel.handleIntent(BrowserIntent.Reload) }
            val onNewTab: () -> Unit = { viewModel.handleIntent(BrowserIntent.NewTab) }
            val onUrlChanged: (String) -> Unit = { viewModel.handleIntent(BrowserIntent.UpdateInputUrl(it)) }
            val onUrlSubmit: () -> Unit = { viewModel.submitUrl(state.inputUrl) }
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(topScroll.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            Column {
                BrowserTopBar(
                    url = state.inputUrl,
                    onUrlChanged = callbacks.onUrlChanged,
                    onSubmit = callbacks.onUrlSubmit,
                    onMenuClick = { viewModel.handleIntent(BrowserIntent.OpenSettings) },
                    scrollBehavior = topScroll,
                    shouldFocusUrlInput = state.shouldFocusUrlInput
                )
                BrowserProgressIndicator(
                    progress = state.progress,
                    isVisible = state.isLoading
                )
            }
        },
        bottomBar = {
            BrowserBottomBar(
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onBackClick = callbacks.onBack,
                onForwardClick = callbacks.onForward,
                onReloadClick = callbacks.onReload,
                onNewTabClick = callbacks.onNewTab
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        WebViewHost(
            onProgressChanged = callbacks.onProgressChanged,
            onPageStarted = callbacks.onPageStarted,
            onPageFinished = callbacks.onPageFinished,
            onTitleChanged = callbacks.onTitleChanged,
            onNavigationStateChanged = callbacks.onNavigationStateChanged,
            onError = callbacks.onError,
            filePickerLauncher = filePickerLauncher,
            uaProvider = uaProvider,
            config = state.settingsCurrent,
            onControllerReady = callbacks.onControllerReady,
            onScrollDelta = { dyPx ->
                val available = Offset(x = 0f, y = -dyPx.toFloat())
                val pre = topScroll.nestedScrollConnection.onPreScroll(available,
                    NestedScrollSource.UserInput
                )
                val remaining = available - pre
                topScroll.nestedScrollConnection.onPostScroll(Offset.Zero, remaining,
                    NestedScrollSource.UserInput
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
        if (state.isSettingsDialogVisible) {
            BrowserSettingsDialog(
                config = state.settingsDraft,
                onConfigChange = { viewModel.handleIntent(BrowserIntent.SettingsUpdated(it)) },
                onDismiss = { viewModel.handleIntent(BrowserIntent.CloseSettings) },
                onConfirm = { viewModel.handleIntent(BrowserIntent.ApplySettings) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
public fun BrowserScreenPreview(@PreviewParameter(BrowserStateProvider::class) state: BrowserState) {
    MaterialTheme {
        BrowserScreenPreviewContent(state)
    }
}

@Composable
public fun BrowserScreenPreviewContent(state: BrowserState) {
    val mockViewModel = remember {
        object {
            fun handleIntent() {}
            fun submitUrl() {}
        }
    }
    val topScroll = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier
            .nestedScroll(topScroll.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            Column {
                BrowserTopBar(
                    url = state.inputUrl,
                    onUrlChanged = { mockViewModel.handleIntent() },
                    onSubmit = { mockViewModel.submitUrl() },
                    onMenuClick = { /* Preview - no action */ },
                    scrollBehavior = topScroll,
                    shouldFocusUrlInput = state.shouldFocusUrlInput
                )
                BrowserProgressIndicator(
                    progress = state.progress,
                    isVisible = state.isLoading
                )
            }
        },
        bottomBar = {
            BrowserBottomBar(
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onBackClick = { mockViewModel.handleIntent() },
                onForwardClick = { mockViewModel.handleIntent() },
                onReloadClick = { mockViewModel.handleIntent() },
                onNewTabClick = { mockViewModel.handleIntent() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WebView Preview\n${state.inputUrl}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

public class BrowserStateProvider : PreviewParameterProvider<BrowserState> {
    override val values: Sequence<BrowserState> = sequenceOf(
        BrowserState(
            inputUrl = "https://www.example.com",
            title = "Example Website",
            canGoBack = true,
            canGoForward = false,
            shouldFocusUrlInput = false,
            isLoading = false,
            progress = 0.75f
        ),
    )
}
