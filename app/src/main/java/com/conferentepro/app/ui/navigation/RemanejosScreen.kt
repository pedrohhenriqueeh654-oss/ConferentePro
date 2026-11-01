package com.conferentepro.app.ui.navigation

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.conferentepro.app.data.local.AppDatabase
import com.conferentepro.app.data.local.entity.RemanejoEntity
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private val RemanejoBg = Color(0xFFF4F7F6)
private val RemanejoNavy = Color(0xFF123B46)
private val RemanejoAccent = Color(0xFF087E83)

@Composable
fun RemanejosScreen(db: AppDatabase) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val registros by db.remanejoDao().getTodos().collectAsState(initial = emptyList())
    var busca by remember { mutableStateOf("") }
    var filtroFilial by remember { mutableStateOf("") }
    var telaCadastro by remember { mutableStateOf(false) }
    var selecionado by remember { mutableStateOf<RemanejoEntity?>(null) }
    var modoSelecao by remember { mutableStateOf(false) }
    val idsSelecionados = remember { mutableStateListOf<Long>() }
    var mostrarAssinaturaLote by remember { mutableStateOf(false) }
    var mostrarAssinaturaIndividual by remember { mutableStateOf(false) }
    var remanejoDuplicado by remember { mutableStateOf<RemanejoEntity?>(null) }

    val calendarioAtual = Calendar.getInstance()
    val anoAtual = calendarioAtual.get(Calendar.YEAR)
    val mesAtual = calendarioAtual.get(Calendar.MONTH)
    val totalRemanejosMes = registros.count { registro ->
        val data = Calendar.getInstance().apply { timeInMillis = registro.dataRegistro }
        data.get(Calendar.YEAR) == anoAtual && data.get(Calendar.MONTH) == mesAtual
    }

    val filtrados = registros.filter { item ->
        val texto = busca.trim()
        (texto.isBlank() || item.numeroNota.contains(texto, true) ||
            item.filialSaida.contains(texto, true) ||
            item.filialDestino.contains(texto, true) ||
            item.retiradoPor.contains(texto, true)) &&
            (filtroFilial.isBlank() || item.filialSaida.contains(filtroFilial, true) ||
                item.filialDestino.contains(filtroFilial, true))
    }

    Column(Modifier.fillMaxSize().background(RemanejoBg)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("REMANEJOS", color = RemanejoNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${registros.size} comprovante(s) registrado(s)", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ESTE MÊS", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Text("$totalRemanejosMes", color = RemanejoAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Button(
                    onClick = { selecionado = null; telaCadastro = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RemanejoAccent),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("＋ Novo") }
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            telaCadastro -> RemanejoForm(
                onCancel = { telaCadastro = false },
                onSave = { numeros, origem, destino, pessoa, assinatura, observacoes ->
                    scope.launch {
                        val duplicado = numeros.firstNotNullOfOrNull { numero ->
                            db.remanejoDao().buscarPorNumeroNota(numero)
                        }
                        if (duplicado != null) {
                            remanejoDuplicado = duplicado
                            return@launch
                        }
                        val dataRegistro = System.currentTimeMillis()
                        numeros.forEach { numero ->
                            db.remanejoDao().inserir(
                                RemanejoEntity(
                                    numeroNota = numero,
                                    filialSaida = origem.trim(),
                                    filialDestino = destino.trim(),
                                    retiradoPor = pessoa.trim(),
                                    assinaturaBase64 = assinatura,
                                    observacoes = observacoes.trim(),
                                    dataRegistro = dataRegistro
                                )
                            )
                        }
                        telaCadastro = false
                    }
                }
            )
            selecionado != null -> RemanejoDetalhe(
                item = selecionado!!,
                onBack = { selecionado = null },
                onShare = { compartilharComprovante(context, selecionado!!) },
                onSign = { mostrarAssinaturaIndividual = true }
            )
            else -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { modoSelecao = !modoSelecao; idsSelecionados.clear() }, modifier = Modifier.weight(1f)) {
                        Text(if (modoSelecao) "Cancelar seleção" else "Selecionar para assinar")
                    }
                    if (modoSelecao) {
                        Button(onClick = { mostrarAssinaturaLote = true }, enabled = idsSelecionados.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = RemanejoAccent)) {
                            Text("Assinar (${idsSelecionados.size})")
                        }
                    }
                }
                if (modoSelecao) {
                    Text("Marque os remanejos existentes que deseja assinar. A mesma assinatura será aplicada a todos.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        idsSelecionados.clear()
                        filtrados.filter { it.assinaturaBase64.isBlank() }.forEach { idsSelecionados.add(it.id) }
                    }) { Text("Selecionar todos não assinados") }
                }
                OutlinedTextField(
                    value = busca, onValueChange = { busca = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Buscar NF, filial ou responsável") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = filtroFilial, onValueChange = { filtroFilial = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Filtrar por filial (origem ou destino)") },
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { if (filtroFilial.isNotBlank()) TextButton(onClick = { filtroFilial = "" }) { Text("Limpar") } }
                )
                Spacer(Modifier.height(10.dp))
                if (filtrados.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (registros.isEmpty()) "Nenhum remanejo cadastrado. Toque em + Novo para registrar." else "Nenhum resultado para os filtros.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                        items(filtrados, key = { it.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                onClick = {
                                    if (modoSelecao) {
                                        if (idsSelecionados.contains(item.id)) idsSelecionados.remove(item.id) else idsSelecionados.add(item.id)
                                    } else selecionado = item
                                }
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            if (modoSelecao) Checkbox(checked = idsSelecionados.contains(item.id), onCheckedChange = { checked ->
                                                if (checked) idsSelecionados.add(item.id) else idsSelecionados.remove(item.id)
                                            })
                                            Text("NF ${item.numeroNota}", color = RemanejoNavy, fontWeight = FontWeight.Bold)
                                        }
                                        Text(formatarData(item.dataRegistro), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(Modifier.height(5.dp))
                                    Text("${item.filialSaida}  →  ${item.filialDestino}", color = RemanejoNavy)
                                    Text("Retirado por: ${item.retiradoPor}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        if (item.assinaturaBase64.isBlank()) "○ NÃO ASSINADO" else "✓ ASSINADO",
                                        color = if (item.assinaturaBase64.isBlank()) Color(0xFFB45309) else Color(0xFF16834A),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text("Toque para abrir comprovante", color = RemanejoAccent, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (remanejoDuplicado != null) {
        val duplicado = remanejoDuplicado!!
        AlertDialog(
            onDismissRequest = { remanejoDuplicado = null },
            title = { Text("Nota fiscal já cadastrada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("A NF ${duplicado.numeroNota} já existe nos remanejos. O novo cadastro foi bloqueado para evitar duplicidade.")
                    Text("Origem: ${duplicado.filialSaida}")
                    Text("Destino: ${duplicado.filialDestino}")
                    Text("Registrada em: ${formatarData(duplicado.dataRegistro)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { remanejoDuplicado = null }) { Text("Entendi") }
            }
        )
    }
    if (mostrarAssinaturaIndividual && selecionado != null) {
        AssinaturaIndividualDialog(
            onDismiss = { mostrarAssinaturaIndividual = false },
            onConfirm = { assinatura ->
                scope.launch {
                    val itemAtual = selecionado ?: return@launch
                    db.remanejoDao().atualizarAssinatura(itemAtual.id, assinatura)
                    selecionado = itemAtual.copy(assinaturaBase64 = assinatura)
                    mostrarAssinaturaIndividual = false
                }
            }
        )
    }
    if (mostrarAssinaturaLote) {
        AssinaturaLoteDialog(quantidade = idsSelecionados.size, onDismiss = { mostrarAssinaturaLote = false }, onConfirm = { assinatura ->
            scope.launch {
                idsSelecionados.toList().forEach { id -> db.remanejoDao().atualizarAssinatura(id, assinatura) }
                idsSelecionados.clear()
                modoSelecao = false
                mostrarAssinaturaLote = false
            }
        })
    }
}

@Composable
private fun AssinaturaLoteDialog(quantidade: Int, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val pontos = remember { mutableStateListOf<Offset>() }
    var largura by remember { mutableFloatStateOf(1f) }
    var altura by remember { mutableFloatStateOf(1f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assinar $quantidade remanejo(s)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Desenhe sua assinatura uma única vez. Ela será aplicada aos registros selecionados.")
                Canvas(Modifier.fillMaxWidth().height(130.dp).background(Color.White, RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(onDragStart = { p -> pontos.add(p); largura = size.width.toFloat(); altura = size.height.toFloat() },
                            onDrag = { change, _ -> pontos.add(change.position); change.consume() })
                    }
                    .pointerInput(Unit) { detectTapGestures { p -> pontos.add(p); largura = size.width.toFloat(); altura = size.height.toFloat() } }
                ) {
                    drawLine(Color.LightGray, Offset(8f, size.height - 14f), Offset(size.width - 8f, size.height - 14f), 1.dp.toPx())
                    if (pontos.size > 1) {
                        val path = Path().apply { moveTo(pontos.first().x, pontos.first().y); pontos.drop(1).forEach { lineTo(it.x, it.y) } }
                        drawPath(path, RemanejoNavy, style = Stroke(width = 2.8.dp.toPx()))
                    }
                }
                TextButton(onClick = { pontos.clear() }) { Text("Limpar assinatura") }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(criarAssinaturaBase64(pontos.toList(), largura, altura)) }, enabled = pontos.size >= 2, colors = ButtonDefaults.buttonColors(containerColor = RemanejoAccent)) { Text("Aplicar assinatura") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RemanejoForm(
    onCancel: () -> Unit,
    onSave: (List<String>, String, String, String, String, String) -> Unit
) {
    var numerosTexto by remember { mutableStateOf("") }
    var origem by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var pessoa by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var assinarAgora by remember { mutableStateOf(false) }
    val pontos = remember { mutableStateListOf<Offset>() }
    var largura by remember { mutableFloatStateOf(1f) }
    var altura by remember { mutableFloatStateOf(1f) }
    val assinaturaValida = pontos.size >= 2

    Column(Modifier.fillMaxSize()) {
        Text("NOVO COMPROVANTE", color = RemanejoNavy, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = numerosTexto,
            onValueChange = { numerosTexto = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Número da NF *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        val numerosValidos = listOf(numerosTexto.trim()).filter { it.isNotBlank() }
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(origem, { origem = it }, Modifier.fillMaxWidth(), label = { Text("Filial de origem *") }, singleLine = true)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(destino, { destino = it }, Modifier.fillMaxWidth(), label = { Text("Filial de destino *") }, singleLine = true)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(pessoa, { pessoa = it }, Modifier.fillMaxWidth(), label = { Text("Quem retirou *") }, singleLine = true)
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(observacoes, { observacoes = it }, Modifier.fillMaxWidth(), label = { Text("Observações (opcional)") }, minLines = 2, maxLines = 3)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = assinarAgora, onCheckedChange = { assinarAgora = it; if (!it) pontos.clear() })
            Column(Modifier.weight(1f)) {
                Text("Assinar agora", color = RemanejoNavy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("Desmarcado: o remanejo será salvo como NÃO ASSINADO.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            if (assinarAgora) TextButton(onClick = { pontos.clear() }) { Text("Limpar") }
        }
        if (assinarAgora) {
            Canvas(
                Modifier.fillMaxWidth().height(112.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { p -> pontos.add(p); largura = size.width.toFloat(); altura = size.height.toFloat() },
                            onDrag = { change, _ -> pontos.add(change.position); change.consume() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { p -> pontos.add(p); largura = size.width.toFloat(); altura = size.height.toFloat() }
                    }
            ) {
                drawLine(Color.LightGray, Offset(8f, size.height - 14f), Offset(size.width - 8f, size.height - 14f), 1.dp.toPx())
                if (pontos.size > 1) {
                    val path = Path().apply {
                        moveTo(pontos.first().x, pontos.first().y)
                        pontos.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, RemanejoNavy, style = Stroke(width = 2.8.dp.toPx()))
                }
            }
            Text("Assine com o dedo no campo acima.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(
                onClick = {
                    val assinatura = if (assinarAgora && assinaturaValida) criarAssinaturaBase64(pontos.toList(), largura, altura) else ""
                    onSave(numerosValidos, origem, destino, pessoa, assinatura, observacoes)
                },
                enabled = numerosValidos.isNotEmpty() && origem.isNotBlank() && destino.isNotBlank() && pessoa.isNotBlank() && (!assinarAgora || assinaturaValida),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = RemanejoAccent)
            ) { Text("Salvar") }
        }
    }
}

@Composable
private fun AssinaturaIndividualDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val pontos = remember { mutableStateListOf<Offset>() }
    var largura by remember { mutableFloatStateOf(1f) }
    var altura by remember { mutableFloatStateOf(1f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assinar remanejo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Desenhe sua assinatura para este comprovante.")
                Canvas(
                    Modifier.fillMaxWidth().height(130.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { p -> pontos.add(p); largura = size.width.toFloat(); altura = size.height.toFloat() },
                                onDrag = { change, _ -> pontos.add(change.position); change.consume() }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { p -> pontos.add(p); largura = size.width.toFloat(); altura = size.height.toFloat() }
                        }
                ) {
                    drawLine(Color.LightGray, Offset(8f, size.height - 14f), Offset(size.width - 8f, size.height - 14f), 1.dp.toPx())
                    if (pontos.size > 1) {
                        val path = Path().apply {
                            moveTo(pontos.first().x, pontos.first().y)
                            pontos.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, RemanejoNavy, style = Stroke(width = 2.8.dp.toPx()))
                    }
                }
                TextButton(onClick = { pontos.clear() }) { Text("Limpar assinatura") }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(criarAssinaturaBase64(pontos.toList(), largura, altura)) },
                enabled = pontos.size >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = RemanejoAccent)
            ) { Text("Salvar assinatura") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RemanejoDetalhe(
    item: RemanejoEntity,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSign: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("COMPROVANTE", color = RemanejoNavy, fontWeight = FontWeight.Bold)
            TextButton(onClick = onBack) { Text("Voltar") }
        }
        Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("REMANEJO DE MERCADORIAS", color = RemanejoNavy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                CampoComprovante("NOTA FISCAL", item.numeroNota)
                CampoComprovante("FILIAL DE ORIGEM", item.filialSaida)
                CampoComprovante("FILIAL DE DESTINO", item.filialDestino)
                CampoComprovante("RETIRADO POR", item.retiradoPor)
                CampoComprovante("DATA E HORA", formatarData(item.dataRegistro))
                if (item.observacoes.isNotBlank()) CampoComprovante("OBSERVAÇÕES", item.observacoes)
                Text(
                    if (item.assinaturaBase64.isBlank()) "STATUS: NÃO ASSINADO" else "STATUS: ASSINADO",
                    color = if (item.assinaturaBase64.isBlank()) Color(0xFFB45309) else Color(0xFF16834A),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                if (item.assinaturaBase64.isNotBlank()) {
                    Text("ASSINATURA", color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    val bytes = remember(item.assinaturaBase64) { Base64.decode(item.assinaturaBase64, Base64.DEFAULT) }
                    val bitmap = remember(bytes) { android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Assinatura de quem retirou",
                        modifier = Modifier.fillMaxWidth().height(76.dp)
                    )
                    }
                } else {
                    Text("Este remanejo foi registrado sem assinatura digital.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.weight(1f))
                Text("Comprovante gerado pelo Conferente PRO", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (item.assinaturaBase64.isBlank()) {
            Button(
                onClick = onSign,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RemanejoAccent)
            ) {
                Text("Adicionar assinatura")
            }
            Spacer(Modifier.height(8.dp))
        }
        Button(onClick = onShare, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RemanejoAccent)) {
            Text("Compartilhar comprovante em PDF")
        }
    }
}

@Composable
private fun CampoComprovante(rotulo: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(rotulo, color = Color.Gray, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(valor, color = RemanejoNavy, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun criarAssinaturaBase64(pontos: List<Offset>, largura: Float, altura: Float): String {
    val w = 900
    val h = 260
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(18, 59, 70)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val path = android.graphics.Path()
    val sx = w / largura.coerceAtLeast(1f)
    val sy = h / altura.coerceAtLeast(1f)
    pontos.forEachIndexed { index, p ->
        val x = p.x * sx
        val y = p.y * sy
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    canvas.drawPath(path, paint)
    val stream = java.io.ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    bitmap.recycle()
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

private fun formatarData(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(millis))

private fun compartilharComprovante(context: android.content.Context, item: RemanejoEntity) {
    val pageWidth = 595
    val pageHeight = 842
    val document = PdfDocument()
    val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
    val canvas = page.canvas
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(18, 59, 70); textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.GRAY; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(18, 59, 70); textSize = 16f }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
    canvas.drawText("CONFERENTE PRO", 40f, 52f, titlePaint)
    canvas.drawText("COMPROVANTE DE REMANEJO", 40f, 82f, valuePaint)
    canvas.drawLine(40f, 100f, 555f, 100f, linePaint)
    var y = 135f
    fun field(label: String, value: String) {
        canvas.drawText(label.uppercase(Locale("pt", "BR")), 40f, y, labelPaint)
        y += 22f
        canvas.drawText(value.take(75), 40f, y, valuePaint)
        y += 42f
    }
    field("Nota fiscal", item.numeroNota)
    field("Filial de origem", item.filialSaida)
    field("Filial de destino", item.filialDestino)
    field("Retirado por", item.retiradoPor)
    field("Data e hora", formatarData(item.dataRegistro))
    if (item.observacoes.isNotBlank()) field("Observações", item.observacoes)
    if (item.assinaturaBase64.isBlank()) {
        canvas.drawText("STATUS: NAO ASSINADO", 40f, y, labelPaint)
        y += 35f
    } else {
        canvas.drawText("ASSINATURA", 40f, y, labelPaint)
        y += 12f
        val signatureBytes = Base64.decode(item.assinaturaBase64, Base64.DEFAULT)
        val signature = android.graphics.BitmapFactory.decodeByteArray(signatureBytes, 0, signatureBytes.size)
        if (signature != null) {
            val dest = android.graphics.Rect(40, y.toInt(), 330, (y + 95f).toInt())
            canvas.drawBitmap(signature, null, dest, Paint(Paint.ANTI_ALIAS_FLAG))
            signature.recycle()
        }
        y += 125f
    }
    canvas.drawLine(40f, y, 555f, y, linePaint)
    canvas.drawText("Documento emitido pelo aplicativo Conferente PRO.", 40f, y + 24f, labelPaint)
    document.finishPage(page)
    val folder = File(context.cacheDir, "remanejos").apply { mkdirs() }
    val file = File(folder, "Comprovante_NF_${item.numeroNota}.pdf")
    FileOutputStream(file).use { document.writeTo(it) }
    document.close()
    val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Comprovante de remanejo - NF ${item.numeroNota}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(context.contentResolver, "Comprovante PDF", uri)
    }
    context.startActivity(Intent.createChooser(intent, "Compartilhar comprovante"))
}
