package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.conferentepro.app.data.local.AppDatabase
import com.conferentepro.app.data.local.entity.CobrancaEntity
import com.conferentepro.app.data.local.entity.DemandaEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dataFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralCobrancasScreen(db: AppDatabase, demandas: List<DemandaEntity>) {
    val scope = rememberCoroutineScope()
    val cobrancas by db.cobrancaDao().getTodas().collectAsState(initial = emptyList())
    val ocorrencias by db.produtoDao().getTodasOcorrencias().collectAsState(initial = emptyList())
    var mostrarFormulario by remember { mutableStateOf(false) }
    var destino by remember { mutableStateOf("CD") }
    var referencia by remember { mutableStateOf("") }
    var pessoa by remember { mutableStateOf("") }
    var resposta by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var proximaData by remember { mutableStateOf<Long?>(null) }
    var abrirData by remember { mutableStateOf(false) }
    val dataState = rememberDatePickerState()

    val agora = System.currentTimeMillis()
    val atrasadas = demandas.count { !it.concluida && it.status.name != "CONCLUIDA" && it.status.name != "CANCELADA" && it.dataPrazoFinal != Long.MAX_VALUE && it.dataPrazoFinal < agora }
    val aguardando = demandas.count { !it.concluida && it.status.name.startsWith("AGUARDANDO") }
    val ocorrenciasAbertas = ocorrencias.count { it.status != "RESOLVIDO" && it.status != "CONCLUIDO" }
    val cobrancasVencidas = cobrancas.count { it.status == "PENDENTE" && it.proximaCobranca != null && it.proximaCobranca!! <= agora }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("CENTRAL DE NOTIFICAÇÕES E COBRANÇAS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ResumoCentral("Atrasadas", atrasadas, Color(0xFFD32F2F), Modifier.weight(1f))
            ResumoCentral("Aguardando", aguardando, Color(0xFF287A9B), Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ResumoCentral("Ocorrências", ocorrenciasAbertas, Color(0xFF087E83), Modifier.weight(1f))
            ResumoCentral("Cobrar hoje", cobrancasVencidas, Color(0xFFE07A22), Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { mostrarFormulario = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LaranjaUltra)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Registrar contato / cobrança")
        }
        Spacer(Modifier.height(8.dp))
        Text("HISTÓRICO DE ACOMPANHAMENTOS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (cobrancas.isEmpty()) {
            Text("Nenhuma cobrança registrada. Registre contatos com o CD ou fornecedor para acompanhar os retornos.", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(cobrancas, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(item.destino, color = AzulMarinho, fontWeight = FontWeight.Bold)
                                Text(item.status, color = if (item.status == "PENDENTE") Color(0xFFE07A22) else Color(0xFF388E3C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(item.referencia, fontWeight = FontWeight.SemiBold)
                            Text("Contato: ${formatarData(item.dataContato)} • ${item.pessoaContatada}", fontSize = 12.sp, color = Color.Gray)
                            if (item.resposta.isNotBlank()) Text("Retorno: ${item.resposta}", fontSize = 13.sp)
                            if (item.proximaCobranca != null && item.status == "PENDENTE") {
                                Text("Próxima cobrança: ${formatarData(item.proximaCobranca)}", color = if (item.proximaCobranca <= agora) Color(0xFFD32F2F) else AzulMarinho, fontSize = 12.sp)
                            }
                            if (item.observacoes.isNotBlank()) Text(item.observacoes, color = Color.Gray, fontSize = 12.sp)
                            if (item.status == "PENDENTE") {
                                TextButton(onClick = { scope.launch { db.cobrancaDao().concluir(item.id) } }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Marcar como resolvida")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        AlertDialog(
            onDismissRequest = { mostrarFormulario = false },
            title = { Text("Registrar cobrança") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 520.dp)) {
                    OutlinedTextField(destino, { destino = it }, label = { Text("Destino (CD ou fornecedor)") }, singleLine = true)
                    OutlinedTextField(referencia, { referencia = it }, label = { Text("Produto, demanda ou assunto") })
                    OutlinedTextField(pessoa, { pessoa = it }, label = { Text("Pessoa contatada") }, singleLine = true)
                    OutlinedTextField(resposta, { resposta = it }, label = { Text("Resposta recebida") })
                    OutlinedButton(onClick = { abrirData = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (proximaData == null) "Definir próxima cobrança (opcional)" else "Próxima: ${formatarData(proximaData!!)}")
                    }
                    if (proximaData != null) TextButton(onClick = { proximaData = null }) { Text("Remover próxima data") }
                    OutlinedTextField(observacoes, { observacoes = it }, label = { Text("Observações") })
                    Text("O contato fica registrado com data e hora automaticamente.", color = Color.Gray, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = destino.isNotBlank() && referencia.isNotBlank() && pessoa.isNotBlank(), onClick = {
                    scope.launch {
                        db.cobrancaDao().inserir(CobrancaEntity(destino = destino.trim(), referencia = referencia.trim(), pessoaContatada = pessoa.trim(), resposta = resposta.trim(), proximaCobranca = proximaData, observacoes = observacoes.trim()))
                        destino = "CD"; referencia = ""; pessoa = ""; resposta = ""; observacoes = ""; proximaData = null
                        mostrarFormulario = false
                    }
                }) { Text("Salvar contato") }
            },
            dismissButton = { TextButton(onClick = { mostrarFormulario = false }) { Text("Cancelar") } }
        )
    }
    if (abrirData) {
        DatePickerDialog(
            onDismissRequest = { abrirData = false },
            confirmButton = { TextButton(onClick = {
                proximaData = dataState.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
                abrirData = false
            }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { abrirData = false }) { Text("Cancelar") } }
        ) { DatePicker(state = dataState) }
    }
}

@Composable
private fun ResumoCentral(titulo: String, valor: Int, cor: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(76.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(valor.toString(), color = cor, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(titulo, color = AzulMarinho, fontSize = 11.sp)
        }
    }
}

private fun formatarData(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dataFmt)
