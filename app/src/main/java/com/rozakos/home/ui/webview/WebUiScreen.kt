package com.rozakos.home.ui.webview

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebUiScreen(
    title: String,
    url: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var webView: WebView? by remember { mutableStateOf(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    val requestBack: () -> Unit = { if (webView?.canGoBack() == true) webView?.goBack() else onBack() }
    BackHandler(enabled = canGoBack, onBack = requestBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = requestBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            error = null
                            loading = true
                            webView?.reload()
                        },
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                    ) {
                        Icon(Icons.Outlined.OpenInBrowser, contentDescription = "Open in browser")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                error = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                webError: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
                                    error = "Check that the device is powered on and your phone is on the same Wi-Fi network."
                                }
                            }
                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
                                    error = "The device couldn't open this page (HTTP ${response?.statusCode}). Try again or return to device controls."
                                }
                            }
                        }
                        webView = this
                        loadUrl(url)
                    }
                },
                onRelease = { it.stopLoading(); it.destroy(); webView = null },
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            error?.let { message ->
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Can't reach device settings",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Button(
                        onClick = {
                            error = null
                            loading = true
                            webView?.loadUrl(url)
                        },
                    ) {
                        Text("Retry")
                    }
                }
                }
            }
        }
    }
}
