package com.conferentepro.app.ui.navigation

import android.Manifest
import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
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
    var eanDigitado by remember { mutableStateOf("") }
    var produto by remember { mutableStateOf<ProdutoEntity?>(null) }
    var verificando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var baseTratativas by remember { mutableStateOf<TratativasRoRepository?>(null) }
    var erroBaseTratativas by remember { mutableStateOf<String?>(null) }
    var carregandoBaseTratativas by remember { mutableStateOf(true) }
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

    LaunchedEffect(Unit) {
        carregandoBaseTratativas = true
        val resultado = runCatching { TratativasRoRepository.carregar(context) }
        resultado.onSuccess { baseTratativas = it; erroBaseTratativas = null }
            .onFailure { falha ->
                val detalhe = falha.message?.takeIf { it.isNotBlank() } ?: falha.javaClass.simpleName
                erroBaseTratativas = "Base de tratativas RO indisponível. Detalhe: $detalhe"
            }
        carregandoBaseTratativas = false
    }

    val resultadosTratativas = remember(ean, baseTratativas) {
        if (ean.isBlank()) emptyList() else baseTratativas?.consultar(ean) ?: emptyList()
    }

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

    Column(Modifier.fillMaxSize().then(if (ean.isNotBlank() && produto == null) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
        if (ean.isBlank()) {
            CabecalhoTela("Leitor de produtos", "Leia o EAN para consultar a ficha e registrar ocorrências", "SCANNER")
        }
        // O campo manual permanece visível mesmo após uma consulta ou leitura pela câmera.
        Spacer(Modifier.height(10.dp))
        Text("CONSULTA MANUAL DE EAN", fontWeight = FontWeight.Bold, color = AzulMarinho, fontSize = 14.sp)
        OutlinedTextField(
            value = eanDigitado,
            onValueChange = { eanDigitado = it.filter(Char::isDigit).take(14) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Digite ou cole o código EAN") },
            placeholder = { Text("Ex.: 7891234567890") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = { ean = eanDigitado; erro = null },
            enabled = eanDigitado.length in 8..14,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("CONSULTAR EAN")
        }
        Spacer(Modifier.height(8.dp))
        if (ean.isBlank()) {
            // A câmera fica ativa somente enquanto aguardamos uma leitura.
            // Ao capturar um EAN, removemos o Preview da composição e encerramos a câmera.
            if (temPermissao) {
                CameraEanPreview(
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    lifecycleOwner = lifecycleOwner,
                    onCodeRead = { code -> if (code != ean) { ean = code; eanDigitado = code; erro = null } },
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
        }
        if (ean.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("FICHA DO PRODUTO", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("EAN: $ean", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            OutlinedButton(
                onClick = {
                    ean = ""
                    eanDigitado = ""
                    erro = null
                    tipo = "AVARIA"
                    destino = "CD"
                    quantidade = "1"
                    unidade = "unidade(s)"
                    lote = ""
                    validade = ""
                    protocolo = ""
                    observacoes = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Ler próximo EAN")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (ean.isBlank()) {
            EstadoVazioVisual("Aguardando leitura", "Aponte a câmera para o código de barras ou digite o EAN manualmente.")
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
            CardTratativasRo(resultadosTratativas, carregandoBaseTratativas, erroBaseTratativas)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                item { CardTratativasRo(resultadosTratativas, carregandoBaseTratativas, erroBaseTratativas) }
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


private fun normalizarIdFabricante(valor: String): String {
    val bruto = valor.trim()
    val semDecimal = if (bruto.endsWith(".0")) bruto.dropLast(2) else bruto
    return semDecimal.filter { it.isLetterOrDigit() }.uppercase(java.util.Locale.ROOT)
}

private data class ProdutoTratativaRo(val ean: String, val codigo: String, val descricao: String, val grupo: String, val idFabricante: String, val fabricante: String)
private data class DadosTratativaRo(val codigoFabricante: String, val fabricante: String, val comprador: String, val politicaTrocaFornecedor: String, val tipoNegociacao: String, val tipoSaida: String, val acao: String, val lojaCd: String)
private data class ProdutoComTratativasRo(val produto: ProdutoTratativaRo, val tratativas: List<DadosTratativaRo>, val quantidadeRegistrosEan: Int)

private class TratativasRoRepository private constructor(private val produtosPorEan: Map<String, List<ProdutoComTratativasRo>>) {
    fun consultar(ean: String): List<ProdutoComTratativasRo> = produtosPorEan[ean.filter(Char::isDigit)].orEmpty()

    companion object {
        @Volatile
        private var instancia: TratativasRoRepository? = null

        suspend fun carregar(context: Context): TratativasRoRepository {
            instancia?.let { return it }
            return withContext(Dispatchers.IO) {
                synchronized(this@Companion) {
                    instancia?.let { return@synchronized it }

                    val nomes = listOf("Tratativas_RO.json.gz", "Tratativas_RO.json")
                    var ultimaFalha: Exception? = null
                    var json: com.google.gson.JsonObject? = null
                    for (nomeArquivo in nomes) {
                        try {
                            context.assets.open(nomeArquivo).use { bruto ->
                                val bytes = bruto.readBytes()
                                val ehGzip = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
                                val conteudo = if (ehGzip) GZIPInputStream(bytes.inputStream()).use { it.readBytes() } else bytes
                                val candidato = JsonParser.parseString(conteudo.toString(Charsets.UTF_8)).asJsonObject
                                require(candidato.has("eanRows") && candidato.has("tratRows")) {
                                    "$nomeArquivo não contém eanRows e tratRows"
                                }
                                json = candidato
                            }
                            if (json != null) break
                        } catch (e: Exception) {
                            ultimaFalha = e
                        }
                    }
                    val dados = json ?: throw IllegalStateException(
                        "Falha ao abrir a base JSON/GZIP: " + (ultimaFalha?.message ?: "arquivos ausentes")
                    )

                    // Leitura, descompactação, parsing e indexação ocorrem fora da thread da interface.
                    val eanRows = dados.getAsJsonArray("eanRows").map { row ->
                        row.asJsonArray.map { cell -> if (cell.isJsonNull) "" else cell.asString.trim() }
                    }
                    val tratRows = dados.getAsJsonArray("tratRows").map { row ->
                        row.asJsonArray.map { cell -> if (cell.isJsonNull) "" else cell.asString.trim() }
                    }
                    val tratamentosPorFabricante = tratRows
                        .filter { it.getOrElse(0) { "" }.isNotBlank() }
                        .groupBy { normalizarIdFabricante(it.getOrElse(0) { "" }) }

                    val agrupados = eanRows
                        .filter { it.getOrElse(0) { "" }.isNotBlank() }
                        .groupBy { it[0].filter(Char::isDigit) }

                    val resultado = agrupados.mapValues { (_, linhas) ->
                        linhas.distinct().map { linha ->
                            val produto = ProdutoTratativaRo(
                                linha.getOrElse(0) { "" }, linha.getOrElse(1) { "" },
                                linha.getOrElse(2) { "" }, linha.getOrElse(3) { "" },
                                linha.getOrElse(4) { "" }, linha.getOrElse(5) { "" }
                            )
                            val chaveFabricante = normalizarIdFabricante(produto.idFabricante)
                            // O índice já contém todos os fabricantes normalizados; não é necessário
                            // percorrer novamente a planilha inteira para cada produto.
                            val tratativas = tratamentosPorFabricante[chaveFabricante].orEmpty()
                                .distinct()
                                .map { t ->
                                    DadosTratativaRo(
                                        t.getOrElse(0) { "" }, t.getOrElse(1) { "" },
                                        t.getOrElse(2) { "" }, t.getOrElse(3) { "" },
                                        t.getOrElse(4) { "" }, t.getOrElse(5) { "" },
                                        t.getOrElse(6) { "" }, t.getOrElse(7) { "" }
                                    )
                                }
                            ProdutoComTratativasRo(produto, tratativas, linhas.size)
                        }
                    }
                    TratativasRoRepository(resultado).also { instancia = it }
                }
            }
        }
    }
}

@Composable
private fun CardTratativasRo(resultados: List<ProdutoComTratativasRo>, carregando: Boolean, erro: String?) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TRATATIVA DO PRODUTO", fontWeight = FontWeight.Bold, color = AzulMarinho, fontSize = 17.sp)
            when {
                carregando -> LinearProgressIndicator(Modifier.fillMaxWidth())
                erro != null -> Text(erro, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                resultados.isEmpty() -> Text("Este EAN não foi localizado na base de tratativas RO.", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    if (resultados.size > 1) Text("Atenção: existem ${resultados.size} registros distintos para este EAN. Confira todas as alternativas.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    resultados.forEachIndexed { index, item ->
                        if (resultados.size > 1) Text("REGISTRO ${index + 1}", fontWeight = FontWeight.Bold, color = AzulMarinho, fontSize = 15.sp)
                        val p = item.produto
                        TextoCampoTratativaRo("Código", p.codigo)
                        TextoCampoTratativaRo("Descrição", p.descricao)
                        TextoCampoTratativaRo("Grupo", p.grupo)
                        TextoCampoTratativaRo("ID fabricante", p.idFabricante)
                        if (item.quantidadeRegistrosEan > 1) Text("Planilha contém ${item.quantidadeRegistrosEan} linhas para este EAN.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.tratativas.isEmpty()) Text("Não foi encontrada tratativa para este ID de fabricante.", fontSize = 14.sp)
                        else {
                            if (item.tratativas.size > 1) Text("Há ${item.tratativas.size} registros distintos na Base Trat RO para este fabricante.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            item.tratativas.forEachIndexed { tratIndex, t ->
                                if (item.tratativas.size > 1) Text("TRATATIVA ${tratIndex + 1}", fontWeight = FontWeight.Bold, color = AzulMarinho, fontSize = 15.sp)
                                TextoCampoTratativaRo("Fabricante", t.fabricante)
                                TextoCampoTratativaRo("Comprador", t.comprador)
                                TextoCampoTratativaRo("Política de troca", t.politicaTrocaFornecedor.ifBlank { "Não informado na planilha" })
                                TextoCampoTratativaRo("Tipo de negociação", t.tipoNegociacao.ifBlank { "Não informado na planilha" })
                                TextoCampoTratativaRo("Tipo de saída", t.tipoSaida.ifBlank { "Não informado na planilha" })
                                val decisao = listOf(t.politicaTrocaFornecedor, t.tipoNegociacao, t.tipoSaida, t.acao).joinToString(" ").uppercase(java.util.Locale.ROOT)
                                val semNegociacao = decisao.contains("SEM NEGOCIAÇÃO") || decisao.contains("SEM NEGOCIACAO")
                                val envioCd = decisao.contains("ENVIAR PARA O CD")
                                val recolhimento = decisao.contains("FORNECEDOR FARÁ RECOLHIMENTO NA LOJA") || decisao.contains("FORNECEDOR FARA RECOLHIMENTO NA LOJA")
                                val destaque = when { semNegociacao -> MaterialTheme.colorScheme.error; envioCd || recolhimento -> Color(0xFF16834A); else -> AzulMarinho }
                                val fundo = when { semNegociacao -> MaterialTheme.colorScheme.errorContainer; envioCd || recolhimento -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.surfaceVariant }
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = fundo)) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text("DECISÃO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = destaque)
                                        val tituloDecisao = when {
                                            semNegociacao -> "SEM NEGOCIAÇÃO"
                                            envioCd -> "ENVIAR PARA O CD"
                                            recolhimento -> "FORNECEDOR FARÁ RECOLHIMENTO NA LOJA"
                                            else -> t.politicaTrocaFornecedor.ifBlank { t.tipoNegociacao }
                                        }
                                        Text(tituloDecisao, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = destaque)
                                        Text("AÇÃO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(t.acao.ifBlank { "Não informada na planilha" }, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                        if (index < resultados.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TextoCampoTratativaRo(rotulo: String, valor: String) {
    if (valor.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(rotulo.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
