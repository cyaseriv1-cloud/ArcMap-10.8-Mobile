package com.terraaxis.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingSaveBytes: ByteArray? = null
    private var pendingSaveName: String = "arcmap_export.bin"
    private var pendingSaveMime: String = "application/octet-stream"

    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val bytes = pendingSaveBytes
        val name = pendingSaveName
        val mime = pendingSaveMime
        pendingSaveBytes = null

        if (bytes == null) return@registerForActivityResult

        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    val out = contentResolver.openOutputStream(uri)
                        ?: throw IOException("No se pudo abrir flujo de escritura")
                    out.use {
                        it.write(bytes)
                        it.flush()
                    }
                    val displayName = resolveUriDisplayName(uri) ?: name
                    Toast.makeText(
                        this@MainActivity,
                        "Archivo guardado: $displayName",
                        Toast.LENGTH_LONG
                    ).show()
                    notifyJsExportStatus("saved", "✓ Archivo guardado en carpeta elegida: $displayName")
                } catch (e: Exception) {
                    saveToDownloadsFallback(name, mime, bytes, e)
                }
            } else {
                saveToDownloadsFallback(name, mime, bytes)
            }
        } else {
            Toast.makeText(
                this@MainActivity,
                "Guardado cancelado",
                Toast.LENGTH_SHORT
            ).show()
            notifyJsExportStatus("cancelled", "Guardado cancelado")
        }
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback
        filePathCallback = null
        if (callback == null) return@registerForActivityResult

        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val clipData = data.clipData
            if (clipData != null && clipData.itemCount > 0) {
                val uris = Array(clipData.itemCount) { idx -> clipData.getItemAt(idx).uri }
                callback.onReceiveValue(uris)
            } else if (data.data != null) {
                callback.onReceiveValue(arrayOf(data.data!!))
            } else {
                callback.onReceiveValue(null)
            }
        } else {
            callback.onReceiveValue(null)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        val callback = pendingGeoCallback
        val origin = pendingGeoOrigin
        pendingGeoCallback = null
        pendingGeoOrigin = null
        callback?.invoke(origin, granted, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.setBackgroundColor(0xFF161B22.toInt())
        setContentView(webView)

        ViewCompat.setOnApplyWindowInsetsListener(webView) { v, insets ->
            val sysBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.setGeolocationEnabled(true)
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.userAgentString = settings.userAgentString
            .replace("; wv", "")
            .plus(" ArcMap10.8Mobile/1.2.0")

        webView.addJavascriptInterface(AndroidFileBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url
                if (url.host == APP_ASSET_HOST) {
                    val rawPath = url.path?.trimStart('/') ?: ""
                    val assetRel = if (rawPath.isEmpty()) "index.html" else rawPath
                    return openAssetResponse("www/$assetRel")
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (_: Exception) {
                    this@MainActivity.filePathCallback = null
                    false
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val hasFine = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "(function(){return window.__arcmapHandleBack ? window.__arcmapHandleBack() : false;})()"
                ) { result ->
                    if (result != "true") {
                        if (webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            moveTaskToBack(true)
                        }
                    }
                }
            }
        })

        webView.loadUrl("https://$APP_ASSET_HOST/index.html")
    }

    private fun openAssetResponse(assetPath: String): WebResourceResponse {
        return try {
            val stream = assets.open(assetPath)
            val mime = guessMimeType(assetPath)
            WebResourceResponse(mime, "UTF-8", stream)
        } catch (_: IOException) {
            WebResourceResponse(
                "text/plain",
                "UTF-8",
                404,
                "Not Found",
                emptyMap(),
                ByteArrayInputStream(ByteArray(0))
            )
        }
    }

    private fun guessMimeType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".html") -> "text/html"
            lower.endsWith(".js") || lower.endsWith(".mjs") -> "application/javascript"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".wasm") -> "application/wasm"
            else -> "application/octet-stream"
        }
    }

    private fun resolveUriDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun notifyJsExportStatus(status: String, message: String) {
        if (!::webView.isInitialized) return
        val qStatus = JSONObject.quote(status)
        val qMessage = JSONObject.quote(message)
        webView.post {
            webView.evaluateJavascript(
                "window.__onAndroidExportStatus && window.__onAndroidExportStatus($qStatus, $qMessage);",
                null
            )
        }
    }

    private fun saveToDownloadsFallback(
        safeName: String,
        mimeType: String,
        bytes: ByteArray,
        cause: Exception? = null
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("No se pudo crear archivo en Descargas")
                contentResolver.openOutputStream(uri)?.use {
                    it.write(bytes)
                    it.flush()
                } ?: throw IOException("No se pudo escribir en Descargas")
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                FileOutputStream(File(dir, safeName)).use {
                    it.write(bytes)
                    it.flush()
                }
            }
            val msg = if (cause != null) {
                "✓ Guardado en Descargas (respaldo): $safeName"
            } else {
                "✓ Guardado en Descargas: $safeName"
            }
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            notifyJsExportStatus("saved", msg)
        } catch (e: Exception) {
            val detail = cause?.message ?: e.message ?: "Error desconocido"
            val errMsg = "Error al guardar archivo: $detail"
            Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
            notifyJsExportStatus("error", errMsg)
        }
    }

    private fun resolveMime(filename: String, mimeType: String): String {
        val trimmed = mimeType.trim()
        if (trimmed.isNotEmpty() && trimmed != "application/geo+json") {
            return trimmed
        }
        return when {
            filename.endsWith(".zip", ignoreCase = true) -> "application/zip"
            filename.endsWith(".geojson", ignoreCase = true) ||
                filename.endsWith(".json", ignoreCase = true) -> "application/json"
            filename.endsWith(".png", ignoreCase = true) -> "image/png"
            else -> "application/octet-stream"
        }
    }

    inner class AndroidFileBridge {
        @JavascriptInterface
        fun getAppVersion(): String = "1.2.0"

        @JavascriptInterface
        fun saveBase64File(filename: String, mimeType: String, base64Data: String) {
            saveBase64FileWithPicker(filename, mimeType, base64Data)
        }

        @JavascriptInterface
        fun saveBase64FileWithPicker(filename: String, mimeType: String, base64Data: String) {
            runOnUiThread {
                try {
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    if (bytes.isEmpty()) {
                        notifyJsExportStatus("error", "El archivo generado está vacío.")
                        return@runOnUiThread
                    }
                    val safeName = filename.ifBlank { "arcmap_export.zip" }
                    val safeMime = resolveMime(safeName, mimeType)

                    pendingSaveBytes = bytes
                    pendingSaveName = safeName
                    pendingSaveMime = safeMime

                    val mimeCandidates = listOf(safeMime, "application/octet-stream", "*/*").distinct()
                    var launched = false
                    var lastError: Exception? = null

                    for (candidateMime in mimeCandidates) {
                        val createIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = candidateMime
                            putExtra(Intent.EXTRA_TITLE, safeName)
                        }
                        if (createIntent.resolveActivity(packageManager) != null || candidateMime == "*/*") {
                            try {
                                notifyJsExportStatus(
                                    "picker_opened",
                                    "Abriendo selector de carpeta para '$safeName'..."
                                )
                                saveFileLauncher.launch(createIntent)
                                launched = true
                                break
                            } catch (e: Exception) {
                                lastError = e
                            }
                        }
                    }

                    if (!launched) {
                        pendingSaveBytes = null
                        saveToDownloadsFallback(safeName, safeMime, bytes, lastError)
                    }
                } catch (e: Exception) {
                    pendingSaveBytes = null
                    val errMsg = "Error al preparar archivo: ${e.message}"
                    Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                    notifyJsExportStatus("error", errMsg)
                }
            }
        }

        @JavascriptInterface
        fun saveBase64FileToDownloads(filename: String, mimeType: String, base64Data: String) {
            runOnUiThread {
                try {
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val safeName = filename.ifBlank { "arcmap_export.zip" }
                    val safeMime = resolveMime(safeName, mimeType)
                    saveToDownloadsFallback(safeName, safeMime, bytes)
                } catch (e: Exception) {
                    val errMsg = "Error al guardar en Descargas: ${e.message}"
                    Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                    notifyJsExportStatus("error", errMsg)
                }
            }
        }
    }

    companion object {
        private const val APP_ASSET_HOST = "appassets.androidplatform.net"
    }
}
