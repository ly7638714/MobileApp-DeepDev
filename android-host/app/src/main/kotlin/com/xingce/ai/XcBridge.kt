package com.xingce.ai

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.JavascriptInterface
import android.webkit.WebView

/** 暴露给网页的桥对象 window.xcnative（配合 platform.js 使用）。 */
class XcBridge(private val activity: Activity, private val web: WebView) {
    companion object { const val RC_FOLDER = 2001; const val RC_WRITE = 2002 }

    @JavascriptInterface fun appInfo(): String {
        val ver = try { activity.packageManager.getPackageInfo(activity.packageName, 0) } catch (e: Exception) { null }
        return "{\"versionName\":\"" + (ver?.versionName ?: "") + "\",\"versionCode\":" + (ver?.versionCode ?: 0) + 
            "\",\"sdk\":" + Build.VERSION.SDK_INT + "\",\"manufacturer\":\"" + Build.MANUFACTURER + "\",\"model\":\"" + Build.MODEL + "\"}"
    }
    @JavascriptInterface fun toast(msg: String) {
        try { android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_SHORT).show() } catch (e: Exception) {}
    }

    // ---- 本地通知（学习提醒用）；Android 13+ 首次申请 POST_NOTIFICATIONS ----
    @JavascriptInterface fun notify(title: String, text: String): Boolean {
        return try {
            ensureNotifyPermission()
            if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            val mgr = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                val ch = NotificationChannel("xc_default", "行测提醒", NotificationManager.IMPORTANCE_DEFAULT)
                mgr.createNotificationChannel(ch)
            }
            val id = System.currentTimeMillis().toInt()
            val pi = PendingIntent.getActivity(activity, id, Intent(activity, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val n = if (Build.VERSION.SDK_INT >= 26) {
                android.app.Notification.Builder(activity, "xc_default")
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_notify_chat)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                android.app.Notification.Builder(activity).setContentTitle(title).setContentText(text)
                    .setSmallIcon(android.R.drawable.stat_notify_chat).setContentIntent(pi).setAutoCancel(true).build()
            }
            mgr.notify(id, n)
            true
        } catch (e: Exception) { false }
    }
    private fun ensureNotifyPermission() {
        if (Build.VERSION.SDK_INT >= 33 && activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            try { activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101) } catch (e: Exception) {}
        }
    }

    // ---- 剪贴板 ----
    @JavascriptInterface fun clipboardSet(text: String): Boolean {
        return try {
            val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("xc", text))
            true
        } catch (e: Exception) { false }
    }
    @JavascriptInterface fun clipboardGet(): String {
        return try {
            val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val d = cm.primaryClip ?: return ""
            if (d.itemCount > 0) (d.getItemAt(0).coerceToText(activity) ?: "").toString() else ""
        } catch (e: Exception) { "" }
    }

    // ---- 分享 ----
    @JavascriptInterface fun shareText(text: String, title: String): Boolean = ShareUtil.shareText(activity, text, title)
    @JavascriptInterface fun shareFile(name: String, base64: String, mime: String): Boolean {
        val uri = MediaStoreUtil.insertMedia(activity, android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, name.ifBlank { "share_" + System.currentTimeMillis() }, mime.ifBlank { "application/octet-stream" }, "Download/" + "行测AI导出", android.util.Base64.decode(base64, android.util.Base64.DEFAULT)) ?: return false
        return ShareUtil.shareUri(activity, uri, mime, name)
    }

    // ---- 文件保存（公共可见）----
    @JavascriptInterface fun saveImage(dataUrl: String, name: String): String = MediaStoreUtil.saveImageDataUrl(activity, dataUrl, name)
    @JavascriptInterface fun saveText(name: String, text: String): String = MediaStoreUtil.saveText(activity, name, text)
    @JavascriptInterface fun saveBinary(name: String, base64: String, mime: String): String = MediaStoreUtil.saveBinary(activity, name, base64, mime)

    // ---- SAF 文件夹 ----
    @JavascriptInterface fun pickFolder(requestId: Int) {
        pendingPick = requestId
        FolderUtil.launchPicker(activity, RC_FOLDER)
    }
    @JavascriptInterface fun writeIntoFolder(treeUri: String, name: String, base64: String, requestId: Int) {
        Thread {
            val r = FolderUtil.writeIntoFolder(activity, treeUri, name, base64)
            web.post { eval("window.__xcOnWrite && window.__xcOnWrite($requestId, " + jsStr(r) + ")") }
        }.start()
    }

    var pendingPick: Int = -1
    fun onFolderPicked(uri: android.net.Uri?, err: String?) {
        val id = pendingPick
        pendingPick = -1
        if (uri == null) { eval("window.__xcOnPick && window.__xcOnPick($id, null, null, " + jsStr(err ?: "cancelled") + ")") ; return }
        val name = FolderUtil.remember(activity, uri)
        eval("window.__xcOnPick && window.__xcOnPick($id, '" + uri.toString() + "', " + jsStr(name) + ", null)")
    }
    fun lastFolder(): String {
        val p = FolderUtil.lastFolder(activity) ?: return ""
        return "{\"treeUri\":\"" + p.first + "\",\"name\":\"" + p.second + "\"}"
    }

    private fun jsStr(s: String): String {
        val esc = s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").replace("\r", " ")
        return "'" + esc + "'"
    }
    private fun eval(js: String) { try { web.evaluateJavascript(js, null) } catch (e: Exception) {} }

    // 返回键回调钩子（JS 决定是否消费）
    @JavascriptInterface fun consumeBack(): Boolean = false
    fun askConsumeBack(onDone: (Boolean) -> Unit) {
        try { web.evaluateJavascript("(window.__xcConsumeBack ? (window.__xcConsumeBack() ? '1' : '0') : '0')") { v -> onDone(v == "\"1\"") } } catch (e: Exception) { onDone(false) }
    }
}