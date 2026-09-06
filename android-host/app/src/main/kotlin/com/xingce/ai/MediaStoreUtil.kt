package com.xingce.ai

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

/**
 * 把 JS 传过来的文件/图片写到【用户可见】的公共位置（Android 10+ 用 MediaStore，<=9 用公共目录文件）。
 * 不申请任何存储权限（Android 10+ 写自己的媒体/下载无需权限；<=9 走 WRITE_EXTERNAL_STORAGE，已在清单声明 maxSdk28）。
 */
object MediaStoreUtil {
    private const val DIR = "行测AI导出"

    fun saveImageDataUrl(ctx: Context, dataUrl: String, name: String): String {
        return try {
            val comma = dataUrl.indexOf(',')
            val mime = if (comma > 0) dataUrl.substring(5, comma).substringBefore(';').ifEmpty { "image/png" } else "image/png"
            val b64 = if (comma > 0) dataUrl.substring(comma + 1) else dataUrl
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val finalName = name.ifBlank { "截图_" + System.currentTimeMillis() + ".png" }
            val uri = insertMedia(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, finalName, mime, "Pictures/" + DIR, bytes)
            jsonOk("image", finalName, uri ?: "")
        } catch (e: Exception) { jsonErr(e) }
    }

    fun saveText(ctx: Context, name: String, text: String): String {
        return try {
            val bytes = text.toByteArray(Charsets.UTF_8)
            val finalName = name.ifBlank { "导出_" + System.currentTimeMillis() + ".txt" }
            val uri = insertMedia(ctx, MediaStore.Downloads.EXTERNAL_CONTENT_URI, finalName, "text/plain", "Download/" + DIR, bytes)
            jsonOk("file", finalName, uri ?: "")
        } catch (e: Exception) { jsonErr(e) }
    }

    fun saveBinary(ctx: Context, name: String, base64: String, mime: String): String {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val finalName = name.ifBlank { "导出_" + System.currentTimeMillis() }
            val mm = mime.ifBlank { "application/octet-stream" }
            val uri = insertMedia(ctx, MediaStore.Downloads.EXTERNAL_CONTENT_URI, finalName, mm, "Download/" + DIR, bytes)
            jsonOk("file", finalName, uri ?: "")
        } catch (e: Exception) { jsonErr(e) }
    }

    /** 返回 MediaStore content uri 字符串（用于分享）；失败返回 null */
    fun insertMedia(ctx: Context, collection: Uri, name: String, mime: String, relPath: String, bytes: ByteArray): String? {
        return try {
            val resolver = ctx.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(collection, values) ?: return null
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: run { resolver.delete(uri, null, null); return null }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri.toString()
            } else {
                val baseDir = if (collection == MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                else
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dir = File(baseDir, DIR).apply { mkdirs() }
                val f = File(dir, name)
                FileOutputStream(f).use { it.write(bytes) }
                Uri.fromFile(f).toString()
            }
        } catch (e: Exception) { null }
    }

    private fun jsonOk(kind: String, name: String, uri: String): String {
        val where = when { kind == "image" -> "相册/" + DIR + "/" + name; uri.startsWith("file:") -> uri; else -> "Download/" + DIR + "/" + name }
        return "{\"ok\":true,\"kind\":\"" + kind + "\",\"name\":\"" + name + "\",\"where\":\"" + where + "\",\"uri\":\"" + uri + "\"}"
    }
    private fun jsonErr(e: Exception): String {
        val m = (e.message ?: "unknown").replace("\"", "'")
        return "{\"ok\":false,\"error\":\"" + m + "\"}"
    }
}