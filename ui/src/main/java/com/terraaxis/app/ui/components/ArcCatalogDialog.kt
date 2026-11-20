package com.terraaxis.app.ui.components

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.terraaxis.app.viewmodel.MainViewModel
import java.io.File

/**
 * Diálogo Add Data / ArcCatalog de ArcMap:
 * Permite explorar carpetas del teléfono para cargar archivos .shp y .geojson,
 * o cargar paquetes de muestra de análisis espacial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArcCatalogDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    // Directorio inicial preferido
    val defaultDir = remember {
        val ext = Environment.getExternalStorageDirectory()
        val downloadDir = File(ext, "Download")
        if (downloadDir.exists() && downloadDir.canRead()) downloadDir else ext
    }

    var currentDir by remember { mutableStateOf(defaultDir) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var filterType by remember { mutableStateOf("Todos los formatos (*.shp, *.geojson)") }

    // Listar archivos del directorio actual
    val filesList = remember(currentDir) {
        try {
            val list = currentDir.listFiles()?.filter { file ->
                file.isDirectory || file.extension.equals("shp", ignoreCase = true) || file.extension.equals("geojson", ignoreCase = true) || file.extension.equals("json", ignoreCase = true)
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icono clásico Add Data (Rombo amarillo)
                Surface(
                    color = Color(0xFFFFD54F),
                    shape = RoundedCornerShape(3.dp),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Add Data (ArcCatalog)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Look in: ${currentDir.name.ifBlank { "Root" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Barra de navegación de ruta
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEBE9E4), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        currentDir.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentDir.parentFile != null && currentDir.parentFile?.canRead() == true) {
                        IconButton(
                            onClick = {
                                selectedFile = null
                                currentDir = currentDir.parentFile!!
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Subir nivel", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Acceso rápido a ubicaciones comunes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val dl = File(Environment.getExternalStorageDirectory(), "Download")
                            if (dl.exists()) currentDir = dl
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Descargas", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = {
                            val docs = File(Environment.getExternalStorageDirectory(), "Documents")
                            if (docs.exists()) currentDir = docs
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Documentos", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = {
                            viewModel.loadSampleData()
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Muestra Esri", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider()

                // Explorador de archivos
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp))
                        .background(Color.White)
                ) {
                    if (filesList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No hay archivos Shapefile (.shp) en esta carpeta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filesList) { file ->
                                val isSelected = selectedFile?.absolutePath == file.absolutePath
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFFCCE8FF) else Color.Transparent)
                                        .clickable {
                                            if (file.isDirectory) {
                                                selectedFile = null
                                                currentDir = file
                                            } else {
                                                selectedFile = file
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (file.isDirectory) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        // Icono ESRI Shapefile
                                        when (file.extension.lowercase()) {
                                            "shp" -> {
                                                Surface(
                                                    color = Color(0xFF4CAF50),
                                                    shape = RoundedCornerShape(2.dp),
                                                    modifier = Modifier.size(18.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("S", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                            else -> {
                                                Icon(
                                                    Icons.Default.Map,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1976D2),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.Black,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (!file.isDirectory) {
                                        Text(
                                            "${(file.length() / 1024).coerceAtLeast(1)} KB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                            }
                        }
                    }
                }

                // Nombre del archivo seleccionado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Name:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(60.dp))
                    OutlinedTextField(
                        value = selectedFile?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Selecciona un archivo .shp") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).heightIn(max = 50.dp)
                    )
                }

                // Tipo de archivo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Type:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(60.dp))
                    Text(
                        filterType,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedFile?.let { file ->
                        if (file.extension.equals("shp", ignoreCase = true)) {
                            viewModel.loadShapefile(file)
                        } else if (file.extension.equals("geojson", ignoreCase = true) || file.extension.equals("json", ignoreCase = true)) {
                            viewModel.loadGeoJsonFile(file)
                        }
                    }
                    onDismiss()
                },
                enabled = selectedFile != null && !selectedFile!!.isDirectory,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0079C1)) // ESRI Blue
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
