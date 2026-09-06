package com.xingce.ai

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var bridge: XcBridge? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE) // 去掉系统 ActionBar（应用名标题条）
        // 全屏沉浸式（同原 5+App 全屏体验）：隐藏系统状态栏/导航栏，内容铺满全屏；
        // 刘海/挖孔区域由 WebView 上报 env(safe-area-inset-*)，前端 CSS .app 已用 safe-area 自动避让，顶栏按钮不会被遮挡。
        hideSystemBars()
        web = WebView(this)
        setContentView(web)
        val s: WebSettings = web.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.allowFileAccess = true
        s.setAllowFileAccessFromFileURLs(true)
        s.setAllowUniversalAccessFromFileURLs(true)
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        s.cacheMode = WebSettings.LOAD_DEFAULT

        android.webkit.WebView.setWebContentsDebuggingEnabled(true) // 调试用：adb/chrome devtools 可连
        val b = XcBridge(this, web)
        bridge = b
        web.addJavascriptInterface(b, "xcnative")

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return if (url.startsWith("file://") || url.startsWith("about:")) false else { view?.loadUrl(url); false }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback
                try {
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.type = fileChooserParams?.acceptTypes?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "*/*"
                    if (fileChooserParams?.isCaptureEnabled == true && i.type == "image/*") {
                        // 仅相册/文件选择（保守）；如需拍照再补 CAMERA 运行时权限流程
                    }
                    startActivityForResult(Intent.createChooser(i, "选择文件"), RC_CHOOSER)
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = null
                }
                return true
            }
        }

        web.loadUrl("file:///android_asset/www/index.html")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                // API30+：只用新版 InsetsController 隐藏（不产生“Viewing full screen/Got it”教育浮层）
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.apply {
                    hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                    systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    (android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            }
        } catch (e: Exception) {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_CHOOSER) {
            val cb = filePathCallback
            filePathCallback = null
            cb?.onReceiveValue(when {
                resultCode == RESULT_OK && data?.data != null -> arrayOf(data.data!!)
                resultCode == RESULT_OK && data?.clipData != null -> (0 until data.clipData!!.itemCount).map { data.clipData!!.getItemAt(it).uri }.toTypedArray()
                else -> null
            })
        } else if (requestCode == XcBridge.RC_FOLDER) {
            val b = bridge
            if (resultCode == RESULT_OK) b?.onFolderPicked(data?.data, null) else b?.onFolderPicked(null, "cancelled")
        }
    }

    override fun onBackPressed() {
        val b = bridge
        if (b == null) { finish(); return }
        b.askConsumeBack { consumed ->
            if (!consumed) {
                if (web.canGoBack()) web.goBack() else finish()
            }
        }
    }

    override fun onDestroy() {
        try { filePathCallback?.onReceiveValue(null) } catch (e: Exception) {}
        filePathCallback = null
        web.destroy()
        super.onDestroy()
    }

    companion object { private const val RC_CHOOSER = 1001 }
}