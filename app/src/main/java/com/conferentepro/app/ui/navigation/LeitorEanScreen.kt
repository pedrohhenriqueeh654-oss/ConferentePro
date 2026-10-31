package com.conferentepro.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun LeitorEanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current
    var ean by remember { mutableStateOf("") }
    var temPermissao by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var erro by remember { mutableStateOf<String?>(null) }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        temPermissao = concedida
        if (!concedida) erro = "A câmera precisa de permissão para ler o código de barras."
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("LEITOR DE CÓDIGO DE BARRAS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Aponte a câmera para o código EAN do produto. A leitura funciona offline.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }

        if (temPermissao) {
            CameraEanPreview(
                modifier = Modifier.fillMaxWidth().weight(1f),
                lifecycleOwner = lifecycleOwner,
                onCodeRead = { code ->
                    ean = code
                    erro = null
                },
                onError = { erro = it }
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AzulMarinho, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Permita o acesso à câmera", fontWeight = FontWeight.SemiBold, color = AzulMarinho)
                    Text("O aplicativo só usa a câmera durante a leitura.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { pedirPermissao.launch(Manifest.permission.CAMERA) }) { Text("Ativar câmera") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (ean.isNotBlank()) LaranjaUltra else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(if (ean.isBlank()) "Aguardando leitura" else "Código identificado", fontWeight = FontWeight.SemiBold, color = AzulMarinho)
                }
                OutlinedTextField(
                    value = ean,
                    onValueChange = { value -> ean = value.filter(Char::isDigit).take(14) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("EAN / código do produto") },
                    placeholder = { Text("Leia pela câmera ou digite") },
                    singleLine = true,
                    trailingIcon = {
                        if (ean.isNotBlank()) IconButton(onClick = { clipboard.setText(AnnotatedString(ean)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar EAN")
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ean = ""; erro = null }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Nova leitura")
                    }
                    Button(onClick = { clipboard.setText(AnnotatedString(ean)) }, enabled = ean.isNotBlank(), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Copiar EAN")
                    }
                }
            }
        }
        erro?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

@Composable
private fun CameraEanPreview(
    modifier: Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCodeRead: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val scannerOptions = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128
            )
            .build()
    }
    val scanner: BarcodeScanner = remember { BarcodeScanning.getClient(scannerOptions) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(previewView, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(cameraExecutor) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null) {
                        proxy.close()
                    } else {
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val code = barcodes.firstNotNullOfOrNull { barcode -> barcode.rawValue }
                                if (!code.isNullOrBlank()) onCodeRead(code)
                            }
                            .addOnFailureListener { onError("Não foi possível ler esse código. Tente ajustar a distância e a iluminação.") }
                            .addOnCompleteListener { proxy.close() }
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                onError("Não foi possível iniciar a câmera. Verifique a permissão e tente novamente.")
            }
        }
        providerFuture.addListener(listener, mainExecutor)
        onDispose {
            try {
                if (providerFuture.isDone) providerFuture.get().unbindAll()
            } catch (_: Exception) { }
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
