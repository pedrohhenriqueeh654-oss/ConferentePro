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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.conferentepro.app.data.local.AppDatabase
import com.conferentepro.app.data.local.entity.OcorrenciaProdutoEntity
import com.conferentepro.app.data.local.entity.ProdutoEntity
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeitorEanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    var ean by remember { mutableStateOf("") }
    var produto by remember { mutableStateOf<ProdutoEntity?>(null) }
    var verificando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var nome by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("AVARIA") }
    var destino by remember { mutableStateOf("CD") }
    var quantidade by remember { mutableStateOf("1") }
    var unidade by remember { mutableStateOf("unidade(s)") }
    var lote by remember { mutableStateOf("") }
    var validade by remember { mutableStateOf("") }
    var protocolo by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var temPermissao by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { temPermissao = it }
    val historico by if (ean.isNotBlank() && produto != null) db.produtoDao().getOcorrencias(ean).collectAsState(initial = emptyList()) else remember { mutableStateOf(emptyList()) }

    LaunchedEffect(ean) {
        if (ean.isNotBlank()) {
            verificando = true
            produto = db.produtoDao().getProduto(ean)
            nome = produto?.nome.orEmpty()
            sku = produto?.sku.orEmpty()
            descricao = produto?.descricao.orEmpty()
            verificando = false
        } else {
            produto = null
            nome = ""; sku = ""; descricao = ""
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("FICHA DE PRODUTO", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Leia o EAN para abrir o cadastro e registrar avarias, vencimentos, divergências e destino.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = ean, onValueChange = { ean = it.filter(Char::isDigit).take(14) },
            modifier = Modifier.fillMaxWidth(), label = { Text("EAN / código de barras") },
            singleLine = true, trailingIcon = { IconButton(onClick = { ean = "" }) { Icon(Icons.Default.Close, "Limpar") } }
        )
        Spacer(Modifier.height(8.dp))
        if (temPermissao) {
            CameraEanPreview(
                modifier = Modifier.fillMaxWidth().height(190.dp),
                lifecycleOwner = lifecycleOwner,
                onCodeRead = { code -> if (code != ean) { ean = code; erro = null } },
                onError = { erro = it }
            )
        } else {
            Card(Modifier.fillMaxWidth().height(150.dp)) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = AzulMarinho, modifier = Modifier.size(36.dp))
                    Text("Permita o acesso à câmera")
                    Button(onClick = { pedirPermissao.launch(Manifest.permission.CAMERA) }) { Text("Ativar câmera") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (ean.isBlank()) {
            Card(Modifier.fillMaxWidth()) { Text("Aguardando leitura. Aponte a câmera para o código de barras do produto.", Modifier.padding(16.dp), color = AzulMarinho) }
        } else if (verificando) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else if (produto == null) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null)
                        Spacer(Modifier.width(8.dp))
                        Text("PRODUTO NÃO CADASTRADO", fontWeight = FontWeight.Bold)
                    }
                    Text("Este EAN ainda não possui uma ficha. Cadastre o produto uma única vez para começar a registrar o histórico.")
                    OutlinedTextField(nome, { nome = it }, Modifier.fillMaxWidth(), label = { Text("Nome do produto *") }, singleLine = true)
                    OutlinedTextField(sku, { sku = it }, Modifier.fillMaxWidth(), label = { Text("SKU / código interno (opcional)") }, singleLine = true)
                    OutlinedTextField(descricao, { descricao = it }, Modifier.fillMaxWidth(), label = { Text("Descrição / apresentação (opcional)") })
                    Button(
                        onClick = {
                            if (nome.isNotBlank()) scope.launch {
                                val novo = ProdutoEntity(ean = ean, nome = nome.trim(), sku = sku.trim(), descricao = descricao.trim())
                                db.produtoDao().salvarProduto(novo)
                                produto = novo
                            }
                        },
                        enabled = nome.isNotBlank(), modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Criar cadastro do produto") }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, null, tint = LaranjaUltra)
                                Spacer(Modifier.width(8.dp))
                                Text("PRODUTO CADASTRADO", fontWeight = FontWeight.Bold, color = AzulMarinho)
                            }
                            Text(produto!!.nome, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AzulMarinho)
                            Text("EAN: $ean", fontSize = 12.sp)
                            if (produto!!.sku.isNotBlank()) Text("SKU: ${produto!!.sku}", fontSize = 12.sp)
                            if (produto!!.descricao.isNotBlank()) Text(produto!!.descricao, fontSize = 12.sp)
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("REGISTRAR NOVA OCORRÊNCIA", fontWeight = FontWeight.Bold, color = AzulMarinho)
                            Text("Tipo de ocorrência")
                            var tipoExpandido by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = tipoExpandido, onExpandedChange = { tipoExpandido = it }) {
                                OutlinedTextField(tipo, { }, Modifier.fillMaxWidth().menuAnchor(), readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tipoExpandido) })
                                ExposedDropdownMenu(expanded = tipoExpandido, onDismissRequest = { tipoExpandido = false }) {
                                    listOf("AVARIA", "VENCIMENTO", "DIVERGÊNCIA", "FALTA", "SOBRA", "TROCA", "DEFEITO", "PRODUTO INCORRETO", "OUTRO").forEach { op ->
                                        DropdownMenuItem(text = { Text(op) }, onClick = { tipo = op; tipoExpandido = false })
                                    }
                                }
                            }
                            Text("Destino desta ocorrência")
                            var destinoExpandido by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = destinoExpandido, onExpandedChange = { destinoExpandido = it }) {
                                OutlinedTextField(destino, { }, Modifier.fillMaxWidth().menuAnchor(), readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(destinoExpandido) })
                                ExposedDropdownMenu(expanded = destinoExpandido, onDismissRequest = { destinoExpandido = false }) {
                                    listOf("CD", "FORNECEDOR", "AVARIA", "LOJA", "VENCIDO").forEach { op ->
                                        DropdownMenuItem(text = { Text(op) }, onClick = { destino = op; destinoExpandido = false })
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(quantidade, { quantidade = it }, Modifier.weight(1f), label = { Text("Quantidade") }, singleLine = true)
                                OutlinedTextField(unidade, { unidade = it }, Modifier.weight(1f), label = { Text("Unidade") }, singleLine = true)
                            }
                            OutlinedTextField(lote, { lote = it }, Modifier.fillMaxWidth(), label = { Text("Lote (opcional)") }, singleLine = true)
                            OutlinedTextField(validade, { validade = it }, Modifier.fillMaxWidth(), label = { Text("Validade (dd/MM/aaaa, opcional)") }, singleLine = true)
                            OutlinedTextField(protocolo, { protocolo = it }, Modifier.fillMaxWidth(), label = { Text("Protocolo (opcional)") }, singleLine = true)
                            OutlinedTextField(observacoes, { observacoes = it }, Modifier.fillMaxWidth(), label = { Text("Observações") }, minLines = 2)
                            Button(
                                onClick = {
                                    val qtd = quantidade.replace(",", ".").toDoubleOrNull()
                                    val validadeMillis = try { if (validade.isBlank()) null else SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply { isLenient = false }.parse(validade)?.time } catch (_: Exception) { null }
                                    if (qtd != null && qtd > 0 && (validade.isBlank() || validadeMillis != null)) scope.launch {
                                        db.produtoDao().inserirOcorrencia(OcorrenciaProdutoEntity(
                                            produtoEan = ean, tipo = tipo, destino = destino, quantidade = qtd,
                                            unidade = unidade.ifBlank { "unidade(s)" }, lote = lote.trim(), validade = validadeMillis,
                                            protocolo = protocolo.trim(), observacoes = observacoes.trim()
                                        ))
                                        quantidade = "1"; lote = ""; validade = ""; protocolo = ""; observacoes = ""
                                        erro = null
                                    } else erro = "Informe uma quantidade válida e confira a data de validade."
                                }, modifier = Modifier.fillMaxWidth()
                            ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Salvar ocorrência") }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("HISTÓRICO COMPLETO DO EAN", fontWeight = FontWeight.Bold, color = AzulMarinho)
                            Text("${historico.size} registro(s) • ${historico.count { it.status.equals("PENDENTE", true) }} pendente(s) • ${historico.count { it.status.equals("RESOLVIDO", true) }} resolvido(s)", fontSize = 12.sp)
                            Text("Os registros são exibidos do mais recente para o mais antigo e permanecem vinculados a este EAN.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (historico.isEmpty()) item { Text("Nenhuma ocorrência registrada para este produto.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(historico) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.tipo, fontWeight = FontWeight.Bold, color = AzulMarinho)
                            Text("${item.quantidade} ${item.unidade} • Destino: ${item.destino}")
                            if (item.lote.isNotBlank()) Text("Lote: ${item.lote}", fontSize = 12.sp)
                            if (item.validade != null) Text("Validade: ${SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(item.validade)}", fontSize = 12.sp)
                            if (item.protocolo.isNotBlank()) Text("Protocolo: ${item.protocolo}", fontSize = 12.sp)
                            if (item.observacoes.isNotBlank()) Text(item.observacoes, fontSize = 12.sp)
                            Text("Registrado em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(item.dataRegistro)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = if (item.status.equals("RESOLVIDO", true)) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(item.status, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                item.dataResolucao?.let { resolucao ->
                                    Text("Resolvido em ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(resolucao)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
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
    lifecycleOwner: LifecycleOwner,
    onCodeRead: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val scannerOptions = remember {
        BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E, Barcode.FORMAT_CODE_128
        ).build()
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
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(cameraExecutor) { proxy ->
                    val mediaImage = proxy.image
                    if (mediaImage == null) proxy.close() else {
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(image).addOnSuccessListener { barcodes ->
                            barcodes.firstNotNullOfOrNull { it.rawValue }?.let { onCodeRead(it) }
                        }.addOnFailureListener {
                            onError("Não foi possível ler esse código. Ajuste a distância e a iluminação.")
                        }.addOnCompleteListener { proxy.close() }
                    }
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (_: Exception) {
                onError("Não foi possível iniciar a câmera. Verifique a permissão.")
            }
        }
        providerFuture.addListener(listener, mainExecutor)
        onDispose {
            try { if (providerFuture.isDone) providerFuture.get().unbindAll() } catch (_: Exception) { }
            scanner.close()
            cameraExecutor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
