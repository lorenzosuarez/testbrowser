package com.testlabs.browser.ui.browser

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.testlabs.browser.R
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.presentation.browser.BrowserEffect
import com.testlabs.browser.presentation.browser.BrowserIntent
import com.testlabs.browser.presentation.browser.BrowserViewModel
import com.testlabs.browser.ui.browser.components.BrowserBottomBar
import com.testlabs.browser.ui.browser.components.BrowserProgressIndicator
import com.testlabs.browser.ui.browser.components.BrowserSettingsDialog
import com.testlabs.browser.ui.browser.components.BrowserTopBar
import com.testlabs.browser.ui.browser.components.StartPage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * High-level browser screen that hosts the material scaffold and the WebView container.
 *
 * This screen wires the MVI view model with the WebView host:
 * - Maps WebView callbacks to intents (title, progress, URL, navigation state, errors)
 * - Handles view-model effects (load/reload/back/forward/recreate/clear data)
 * - Bridges WebView scroll deltas into the TopAppBar nested scroll behavior
 * - Shows settings dialog and start page
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BrowserScreen(
    filePickerLauncher: ActivityResultLauncher<Intent>,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider = koinInject(),
    viewModel: BrowserViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val topScroll = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val userAgent = remember(uaProvider) { uaProvider.userAgent(desktop = false) }

    var webController by remember { mutableStateOf<WebViewController?>(null) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = state.canGoBack) {
        viewModel.handleIntent(BrowserIntent.GoBack)
    }

    LaunchedEffect(state.shouldFocusUrlInput) {
        if (state.shouldFocusUrlInput) topScroll.state.heightOffset = 0f
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BrowserEffect.LoadUrl -> webController?.loadUrl(effect.url.value)
                BrowserEffect.ReloadPage -> webController?.reload()
                BrowserEffect.NavigateBack -> webController?.goBack()
                BrowserEffect.NavigateForward -> webController?.goForward()
                is BrowserEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                BrowserEffect.RecreateWebView -> {
                    pendingUrl = state.url.value.ifBlank { null }
                    webController?.recreateWebView()
                    scope.launch { snackbarHostState.showSnackbar("WebView restarted with new settings") }
                }
                BrowserEffect.ClearBrowsingData -> {
                    val controller = webController
                    if (controller == null) {
                        snackbarHostState.showSnackbar("No WebView")
                    } else {
                        controller.clearBrowsingData {
                            scope.launch {
                                viewModel.handleIntent(BrowserIntent.CloseSettings)
                                viewModel.handleIntent(BrowserIntent.NewTab)
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_clear_browsing_data_done))
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topScroll.nestedScrollConnection).fillMaxSize(),
        topBar = {
            Column {
                BrowserTopBar(
                    url = state.inputUrl,
                    onUrlChanged = { viewModel.handleIntent(BrowserIntent.UpdateInputUrl(it)) },
                    onSubmit = { viewModel.submitUrl(state.inputUrl) },
                    onMenuClick = { viewModel.handleIntent(BrowserIntent.OpenSettings) },
                    scrollBehavior = topScroll,
                    shouldFocusUrlInput = state.shouldFocusUrlInput,
                )
                BrowserProgressIndicator(
                    progress = state.progress,
                    isVisible = state.isLoading,
                )
            }
        },
        bottomBar = {
            BrowserBottomBar(
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onBackClick = { viewModel.handleIntent(BrowserIntent.GoBack) },
                onForwardClick = { viewModel.handleIntent(BrowserIntent.GoForward) },
                onReloadClick = { viewModel.handleIntent(BrowserIntent.Reload) },
                onNewTabClick = { viewModel.handleIntent(BrowserIntent.NewTab) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            WebViewHost(
                modifier = Modifier.fillMaxSize(),
                onProgressChanged = { p -> viewModel.handleIntent(BrowserIntent.ProgressChanged(p)) },
                onPageStarted = { url -> viewModel.handleIntent(BrowserIntent.PageStarted(ValidatedUrl.fromValidUrl(url))) },
                onPageFinished = { url -> viewModel.handleIntent(BrowserIntent.PageFinished(ValidatedUrl.fromValidUrl(url))) },
                onTitleChanged = { title -> viewModel.handleIntent(BrowserIntent.TitleChanged(title)) },
                onNavigationStateChanged = { canBack, canFwd ->
                    viewModel.handleIntent(BrowserIntent.NavigationStateChanged(canBack, canFwd))
                },
                onError = { msg -> viewModel.handleIntent(BrowserIntent.NavigationError(msg)) },
                onUrlChanged = { url -> viewModel.handleIntent(BrowserIntent.UrlChanged(ValidatedUrl.fromValidUrl(url))) },
                filePickerLauncher = filePickerLauncher,
                uaProvider = uaProvider,
                jsCompat = jsCompat,
                config = state.settingsCurrent,
                onControllerReady = { controller ->
                    webController = controller
                    val initial = pendingUrl ?: state.url.value.ifBlank { null }
                    if (initial != null) controller.loadUrl(initial)
                    pendingUrl = null
                },
                onScrollDelta = { dyPx ->
                    val available = Offset(x = 0f, y = -dyPx.toFloat())
                    val pre = topScroll.nestedScrollConnection.onPreScroll(
                        available,
                        NestedScrollSource.UserInput
                    )
                    val remaining = available - pre
                    topScroll.nestedScrollConnection.onPostScroll(
                        Offset.Zero,
                        remaining,
                        NestedScrollSource.UserInput
                    )
                },
            )

            val showStart = (state.url.value.isBlank() || state.url.value == "about:blank") &&
                    !state.isLoading && state.errorMessage == null
            StartPage(
                visible = showStart,
                onOpenUrl = { target -> viewModel.submitUrl(target) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (state.isSettingsDialogVisible) {
            val mode = webController?.requestedWithHeaderMode() ?: RequestedWithHeaderMode.UNKNOWN
            val proxyStack = webController?.proxyStackName() ?: "Disabled"

            BrowserSettingsDialog(
                config = state.settingsDraft,
                onConfigChange = { viewModel.handleIntent(BrowserIntent.UpdateSettings(it)) },
                onDismiss = { viewModel.handleIntent(BrowserIntent.CloseSettings) },
                onConfirm = { viewModel.handleIntent(BrowserIntent.ApplySettings) },
                onApplyAndRestart = { viewModel.handleIntent(BrowserIntent.ApplySettingsAndRestart(state.settingsDraft)) },
                onClearBrowsingData = { viewModel.handleIntent(BrowserIntent.ClearBrowsingData) },
                userAgent = userAgent,
                acceptLanguages = state.settingsCurrent.acceptLanguages,
                headerMode = mode.name,
                jsCompatEnabled = state.settingsCurrent.jsCompatibilityMode,
                proxyStack = proxyStack,
                onCopyDiagnostics = {
                    val diagnostics =
                        """{"userAgent":"$userAgent","acceptLanguage":"${state.settingsCurrent.acceptLanguages}","proxyStack":"$proxyStack","xRequestedWith":"${mode.name}","jsCompat":${state.settingsCurrent.jsCompatibilityMode}}"""
                    scope.launch {
                        val clipData = ClipData.newPlainText("Diagnostics", diagnostics)
                        clipboard.setClipEntry(ClipEntry(clipData))
                        snackbarHostState.showSnackbar("Diagnostics copied to clipboard")
                    }
                },
            )
        }
    }
}
