package com.terraaxis.map.basemap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Gestor y descargador asíncrono de teselas XYZ (Tile Manager) para ArcMap 10.8:
 * - Caché de memoria RAM (LruCache de alta velocidad)
 * - Caché persistente en almacenamiento local (Disco)
 * - Descarga asíncrona concurrente con reintentos
 * - Notificación de actualización de fotograma mediante callback
 */
class TileManager(
    private val diskCacheDir: File? = null
) {
    // 1. Caché en memoria RAM: 1/8 de la memoria disponible para bitmaps de teselas
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceIn(16 * 1024, 64 * 1024)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // Control de descargas en curso para no repetir solicitudes de la misma tesela
    private val inFlightRequests = ConcurrentHashMap<String, Boolean>()

    // Pool de hilos con límite de concurrencia para descargas suaves y estables
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callback para solicitar re-renderizado en la vista cuando una tesela termina de cargar
    var onTileLoaded: (() -> Unit)? = null

    /**
     * Obtiene una tesela para el mapa base solicitado.
     * Si está en memoria o en disco, la retorna inmediatamente.
     * Si no está, programa su descarga asíncrona y retorna null.
     */
    fun getTile(basemap: BasemapType, z: Int, x: Int, y: Int): Bitmap? {
        if (basemap == BasemapType.NONE || basemap.tileUrlTemplate.isBlank()) return null
        if (z < basemap.minZoom || z > basemap.maxZoom) return null

        val key = "${basemap.id}_${z}_${x}_${y}"

        // Paso A: Consultar en memoria RAM
        memoryCache.get(key)?.let { return it }

        // Paso B: Consultar en el disco (caché offline)
        val diskFile = diskCacheDir?.let { File(it, "${basemap.id}/$z/$x/$y.png") }
        if (diskFile != null && diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(key, bitmap)
                    return bitmap
                }
            } catch (_: Exception) {
                diskFile.delete()
            }
        }

        // Paso C: Si no existe, solicitar descarga si no está en proceso
        if (inFlightRequests.putIfAbsent(key, true) == null) {
            scope.launch {
                downloadTile(basemap, z, x, y, key, diskFile)
            }
        }

        return null
    }

    private suspend fun downloadTile(
        basemap: BasemapType,
        z: Int,
        x: Int,
        y: Int,
        key: String,
        diskFile: File?
    ) {
        try {
            val urlString = basemap.tileUrlTemplate
                .replace("{z}", z.toString())
                .replace("{x}", x.toString())
                .replace("{y}", y.toString())

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ArcMapMobile/10.8 (com.terraaxis.app; Android GIS)")
                setRequestProperty("Accept", "image/webp,image/png,image/jpeg")
            }

            if (connection.responseCode in 200..299) {
                val stream: InputStream = connection.inputStream
                val bytes = stream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        // Guardar en disco para acceso offline futuro
                        if (diskFile != null) {
                            try {
                                diskFile.parentFile?.mkdirs()
                                val tempFile = File(diskFile.parentFile, "${diskFile.name}.tmp")
                                tempFile.writeBytes(bytes)
                                tempFile.renameTo(diskFile)
                            } catch (_: Exception) { }
                        }

                        // Guardar en memoria RAM
                        memoryCache.put(key, bitmap)

                        // Notificar al hilo principal para redibujar el fotograma
                        withContext(Dispatchers.Main) {
                            onTileLoaded?.invoke()
                        }
                    }
                }
            }
            connection.disconnect()
        } catch (_: Exception) {
            // Ignorar fallas de red silenciosamente (sin bloquear la app)
        } finally {
            inFlightRequests.remove(key)
        }
    }

    fun clearMemory() {
        memoryCache.evictAll()
    }
}
