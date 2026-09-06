package com.xingce.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Base64

/** SAF：让用户选一个文件夹并持久授权，之后可把导出文件写进该文件夹（任意位置，国产 ROM 也通用）。 */
object FolderUtil {
    private const val PREFS = "xc_folder"
    private const val KEY_TREE = "tree_uri"
    private const val KEY_NAME = "folder_name"

    fun launchPicker(activity: Activity, requestCode: Int) {
        try {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) i.putExtra("android.provider.extra.INITIAL_URI", Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"))
            activity.startActivityForResult(i, requestCode)
        } catch (e: Exception) {
            try { activity.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)) } catch (e2: Exception) {}
        }
    }

    fun remember(activity: Activity, uri: Uri): String {
        try {
            activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (e: Exception) {}
        val name = folderName(activity, uri)
        val sp: SharedPreferences = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_TREE, uri.toString()).putString(KEY_NAME, name).apply()
        return name
    }

    fun lastFolder(activity: Activity): Pair<String, String>? {
        val sp = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tree = sp.getString(KEY_TREE, null) ?: return null
        return tree to (sp.getString(KEY_NAME, null) ?: "")
    }

    fun folderName(ctx: Context, treeUri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            ctx.contentResolver.query(docUri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) ?: "" else ""
            } ?: ""
        } catch (e: Exception) { treeUri.lastPathSegment ?: "" }
    }

    /** 在已授权文件夹下创建/写入文件；返回 "ok:name" 或 "err:msg" */
    fun writeIntoFolder(ctx: Context, treeUriString: String, name: String, base64: String): String {
        return try {
            val treeUri = Uri.parse(treeUriString)
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDoc = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val mime = when { name.endsWith(".json") -> "application/json"; name.endsWith(".md") -> "text/markdown"; name.endsWith(".png") -> "image/png"; name.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; else -> "application/octet-stream" }
            // 同名已存在 → 先删再建（覆盖），避免自动备份累积 (1)(2)… 副本
            try {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                val sel = DocumentsContract.Document.COLUMN_DISPLAY_NAME + "=?"
                ctx.contentResolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), sel, arrayOf(name), null)?.use { c ->
                    if (c.moveToFirst()) {
                        val oldUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, c.getString(0))
                        try { DocumentsContract.deleteDocument(ctx.contentResolver, oldUri) } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
            val fileUri = DocumentsContract.createDocument(ctx.contentResolver, parentDoc, mime, name) ?: return "err:create"
            ctx.contentResolver.openOutputStream(fileUri)?.use { it.write(bytes) } ?: return "err:stream"
            "ok:" + name
        } catch (e: Exception) { "err:" + (e.message ?: "write") }
    }
}