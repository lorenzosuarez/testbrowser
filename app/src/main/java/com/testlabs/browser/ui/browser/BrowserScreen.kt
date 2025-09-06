/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.focus.FocusRequester
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
import com.testlabs.browser.presentation.browser.BrowserMode
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

    var webController by remember { mutableStateOf<WebViewController?>(null) }
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = state.canGoBack) {
        viewModel.handleIntent(BrowserIntent.GoBack)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BrowserEffect.LoadUrl -> webController?.loadUrl(effect.url.value)
                BrowserEffect.ReloadPage -> webController?.reload()
                BrowserEffect.NavigateBack -> webController?.goBack()
                BrowserEffect.NavigateForward -> webController?.goForward()
                is BrowserEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                BrowserEffect.FocusUrlEditor -> {
                    focusRequester.requestFocus()
                    topScroll.state.heightOffset = 0f
                }
                BrowserEffect.ClearBrowsingData -> {
                    val controller = webController
                    if (controller == null) {
                        snackbarHostState.showSnackbar("No WebView")
                    } else {
                        controller.clearBrowsingData {
                            scope.launch {
                                viewModel.handleIntent(BrowserIntent.CloseSettings)
                                viewModel.handleIntent(BrowserIntent.EditUrlRequested)
                                snackbarHostState.showSnackbar(context.getString(R.string.settings_clear_browsing_data_done))
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(state.mode) {
        if (state.mode == BrowserMode.StartPage) {
            webController = null
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(topScroll.nestedScrollConnection).fillMaxSize(),
        topBar = {
            Column {
                BrowserTopBar(
                    url = if (state.isUrlInputEditing) state.inputUrl else state.url.value,
                    onUrlChanged = { viewModel.handleIntent(BrowserIntent.UpdateInputUrl(it)) },
                    onSubmit = { viewModel.submitUrl(state.inputUrl) },
                    onMenuClick = { viewModel.handleIntent(BrowserIntent.OpenSettings) },
                    scrollBehavior = topScroll,
                    focusRequester = focusRequester,
                    onEditingChange = { editing -> viewModel.handleIntent(BrowserIntent.UrlInputEditing(editing)) }
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
                onHomeClick = { viewModel.handleIntent(BrowserIntent.NavigateHome) },
                onEditUrlClick = { viewModel.handleIntent(BrowserIntent.EditUrlRequested) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (state.mode == BrowserMode.Web) {
                androidx.compose.runtime.key(state.webViewInstanceKey) {
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
                            val initial = state.url.value.ifBlank { null }
                            if (initial != null) controller.loadUrl(initial)
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
                }
            }

            StartPage(
                visible = state.mode == BrowserMode.StartPage,
                onOpenUrl = { target -> viewModel.submitUrl(target) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (state.isSettingsDialogVisible) {
            val mode = webController?.requestedWithHeaderMode() ?: RequestedWithHeaderMode.UNKNOWN
            val proxyStack = webController?.proxyStackName() ?: "Disabled"


            val currentUserAgent = state.settingsDraft.customUserAgent
                ?: uaProvider.userAgent(desktop = state.settingsDraft.desktopMode)

            BrowserSettingsDialog(
                config = state.settingsDraft,
                onConfigChange = { viewModel.handleIntent(BrowserIntent.UpdateSettings(it)) },
                onDismiss = { viewModel.handleIntent(BrowserIntent.CloseSettings) },
                onConfirm = { viewModel.handleIntent(BrowserIntent.ApplySettings) },
                onApplyAndRestart = { viewModel.handleIntent(BrowserIntent.ApplySettingsAndRestartWebView(state.settingsDraft)) },
                onClearBrowsingData = { viewModel.handleIntent(BrowserIntent.ClearBrowsingData) },
                userAgent = currentUserAgent,
                acceptLanguages = state.settingsDraft.acceptLanguages,
                headerMode = mode.name,
                jsCompatEnabled = state.settingsDraft.jsCompatibilityMode,
                proxyStack = proxyStack,
                onCopyDiagnostics = {
                    val diagnostics =
                        """{"userAgent":"$currentUserAgent","acceptLanguage":"${state.settingsDraft.acceptLanguages}","proxyStack":"$proxyStack","xRequestedWith":"${mode.name}","jsCompat":${state.settingsDraft.jsCompatibilityMode},"desktopMode":${state.settingsDraft.desktopMode},"thirdPartyCookies":${state.settingsDraft.enableThirdPartyCookies},"proxyEnabled":${state.settingsDraft.proxyEnabled},"proxyInterceptEnabled":${state.settingsDraft.proxyInterceptEnabled}}"""
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
