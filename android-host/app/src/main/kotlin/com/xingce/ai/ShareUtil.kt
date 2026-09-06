package com.xingce.ai

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtil {
    fun shareText(ctx: Context, text: String, title: String): Boolean {
        return try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title)
            }
            ctx.startActivity(Intent.createChooser(i, "分享到"))
            true
        } catch (e: Exception) { false }
    }
    fun shareUri(ctx: Context, uriString: String, mime: String, name: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = mime.ifBlank { "application/octet-stream" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TITLE, name)
            }
            ctx.startActivity(Intent.createChooser(i, "分享文件"))
            true
        } catch (e: Exception) { false }
    }
}