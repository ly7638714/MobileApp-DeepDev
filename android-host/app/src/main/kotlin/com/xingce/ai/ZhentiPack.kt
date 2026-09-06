package com.xingce.ai

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/** 真题卷包：App 内一键下载(zip)→解压到应用外部目录→离线阅读；不占安装包体积 */
object ZhentiPack {
    private const val TOTAL_LIMIT = 1024L * 1024 * 1024 // 1GB 上限防呆
    private fun dir(ctx: Context): File = File(ctx.getExternalFilesDir(null), "zhenti").apply { mkdirs() }

    fun install(ctx: Context, url: String): String {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 20000; conn.readTimeout = 60000; conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "xingce-ai")
            val code = conn.responseCode
            if (code !in 200..299) return "err:HTTP " + code
            val zipFile = File(ctx.cacheDir, "zhenti_pack.zip")
            zipFile.outputStream().use { os -> conn.inputStream.copyTo(os) }
            conn.disconnect()
            val root = dir(ctx)
            root.listFiles()?.forEach { it.deleteRecursively() }
            var count = 0; var total = 0L
            ZipInputStream(zipFile.inputStream().buffered()).use { z ->
                var e = z.nextEntry
                while (e != null) {
                    val name = e.name
                    if (!name.contains("..") && !name.startsWith("/")) {
                        val out = File(root, name)
                        if (e.isDirectory) out.mkdirs()
                        else {
                            out.parentFile?.mkdirs()
                            total += e.size
                            if (total > TOTAL_LIMIT) return "err:包过大"
                            FileOutputStream(out).use { fos -> z.copyTo(fos) }
                            count++
                        }
                    }
                    z.closeEntry(); e = z.nextEntry
                }
            }
            zipFile.delete()
            "ok:" + count
        } catch (e: Exception) { "err:" + (e.message ?: "下载/解压失败") }
    }

    fun list(ctx: Context): String {
        val sb = StringBuilder("["); var first = true
        fun walk(f: File, rel: String) {
            val kids = f.listFiles()?.sortedBy { it.name } ?: return
            for (k in kids) {
                val r = if (rel.isEmpty()) k.name else rel + "/" + k.name
                if (!first) sb.append(','); first = false
                sb.append('{').append("\"name\":\"").append(js(k.name)).append("\",\"path\":\"").append(js(r)).append("\",\"dir\":").append(k.isDirectory).append('}')
                if (k.isDirectory) walk(k, r)
            }
        }
        walk(dir(ctx), "")
        sb.append(']')
        return sb.toString()
    }
    private fun js(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")

    fun read(ctx: Context, rel: String): String {
        return try {
            val f = File(dir(ctx), rel)
            if (!f.exists() || f.isDirectory) return "ERR:文件不存在"
            val bytes = f.readBytes()
            if (bytes.size > 140 * 1024 * 1024) return "ERR:文件过大"
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) { "ERR:" + (e.message ?: "读取失败") }
    }
}