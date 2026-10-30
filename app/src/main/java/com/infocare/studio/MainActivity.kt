package com.infocare.studio

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import android.util.Base64

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val pickFile = 4102
    private val askStorage = 4103
    private val io = Executors.newSingleThreadExecutor()
    private var pendingName = "infocare-output"
    private var pendingMime = "application/octet-stream"
    private var pendingBytes: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.settings.setSupportMultipleWindows(false)
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(v: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
                }
                startActivityForResult(Intent.createChooser(intent, "Choose a file"), pickFile)
                return true
            }
        }
        web.addJavascriptInterface(NativeDownloads(), "InfoCareNative")
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class NativeDownloads {
        @JavascriptInterface
        fun saveDataUrl(dataUrl: String, filename: String): Boolean {
            return try {
                val comma = dataUrl.indexOf(',')
                if (comma < 0) return false
                val header = dataUrl.substring(0, comma)
                val mime = Regex("data:([^;]+)").find(header)?.groupValues?.get(1) ?: "application/octet-stream"
                val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
                if (bytes.isEmpty()) return false
                pendingName = filename.replace(Regex("[^A-Za-z0-9._-]"), "_").take(100).ifBlank { "infocare-output" }
                pendingMime = mime
                if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    pendingBytes = bytes
                    requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), askStorage)
                } else {
                    saveBytes(bytes, pendingName, pendingMime)
                }
                true
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Could not save file: ${e.message}", Toast.LENGTH_LONG).show() }
                false
            }
        }
    }

    private fun saveBytes(bytes: ByteArray, name: String, mime: String) {
        io.execute {
            try {
                val isImage = mime.startsWith("image/")
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, mime)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, if (isImage) "Pictures/InfoCare Studio" else "Download/InfoCare Studio")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                    val collection = if (isImage) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    val uri = contentResolver.insert(collection, values) ?: throw Exception("Storage provider did not create a file")
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw Exception("Cannot open output file")
                    val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    contentResolver.update(uri, done, null, null)
                } else {
                    val folder = File(Environment.getExternalStoragePublicDirectory(if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS), "InfoCare Studio")
                    if (!folder.exists()) folder.mkdirs()
                    val file = File(folder, name)
                    FileOutputStream(file).use { it.write(bytes) }
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                }
                runOnUiThread { Toast.makeText(this@MainActivity, "Saved: $name", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickFile) {
            val results = if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                if (uri != null) arrayOf(uri) else null
            } else null
            fileCallback?.onReceiveValue(results)
            fileCallback = null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == askStorage) {
            val bytes = pendingBytes
            pendingBytes = null
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && bytes != null) saveBytes(bytes, pendingName, pendingMime)
            else Toast.makeText(this, "Storage permission was not granted", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
