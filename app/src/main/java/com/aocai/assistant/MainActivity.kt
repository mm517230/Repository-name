package com.aocai.assistant

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var usingLocalFallback = false

    companion object {
        // GitHub Pages 地址：仓库 mm517230/Repository-name
        private const val ONLINE_URL = "https://mm517230.github.io/Repository-name/"
        private const val LOCAL_URL = "file:///android_asset/index.html"
    }

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_NO_CACHE
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame && !usingLocalFallback) loadLocalFallback()
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request.isForMainFrame && errorResponse.statusCode >= 400 && !usingLocalFallback) {
                    loadLocalFallback()
                }
            }
        }
        webView.webChromeClient = WebChromeClient()
        webView.setDownloadListener(DownloadListener { url, userAgent, _, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "aocai_export.json")
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "已开始下载", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this, "当前导出方式暂不支持", Toast.LENGTH_LONG).show()
            }
        })
        // 每次启动优先获取 GitHub Pages 上的最新网页；网络不可用时自动使用 APK 内置版本。
        webView.loadUrl(ONLINE_URL)
    }

    private fun loadLocalFallback() {
        if (usingLocalFallback) return
        usingLocalFallback = true
        webView.post { webView.loadUrl(LOCAL_URL) }
        Toast.makeText(this, "联网更新暂不可用，已切换到本地版本", Toast.LENGTH_LONG).show()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}
