package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conferentepro.app.data.local.AppDatabase
import com.conferentepro.app.data.local.entity.DemandaEntity
import com.conferentepro.app.domain.model.CategoriaDemanda
import com.conferentepro.app.domain.model.Prioridade
import com.conferentepro.app.domain.model.StatusDemanda
import com.conferentepro.app.worker.MonthlyCdCycle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val AzulMarinho = Color(0xFF123B46)
val LaranjaUltra = Color(0xFF39B8A5)
val VermelhoAtraso = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation() {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.getInstance(ctx) }
    val scope = rememberCoroutineScope()
    var telaSelecionada by remember { mutableStateOf(0) }
    var showDialogCriar by remember { mutableStateOf(false) }
    var showDialogFecharCd by remember { mutableStateOf(false) }
    var novoTitulo by remember { mutableStateOf("") }
    var prazoIndeterminado by remember { mutableStateOf(false) }
    var mostrarSeletorData by remember { mutableStateOf(false) }
    var dataPrazoSelecionada by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    val seletorDataState = rememberDatePickerState(initialSelectedDateMillis = dataPrazoSelecionada.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
    var filtroDemandas by remember { mutableStateOf("") }
    var hoje by remember { mutableStateOf(LocalDate.now()) }
    var cicloCd by remember { mutableStateOf(MonthlyCdCycle.currentState(ctx, hoje)) }
    val demandas by db.demandaDao().getAllDemandas().collectAsState(initial = emptyList())

    fun atualizarCiclo() {
        hoje = LocalDate.now()
        cicloCd = MonthlyCdCycle.currentState(ctx, hoje)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.Center) {
                        Text("CONFERENTE PRO", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Desenvolvido por @eanpdro e GPT", color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, letterSpacing = 0.25.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulMarinho)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AzulMarinho) {
                NavigationBarItem(selected = telaSelecionada == 0, onClick = { telaSelecionada = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Início", tint = if (telaSelecionada == 0) LaranjaUltra else Color.LightGray) },
                    label = { Text("Início", color = if (telaSelecionada == 0) LaranjaUltra else Color.LightGray) })
                NavigationBarItem(selected = telaSelecionada == 1, onClick = { telaSelecionada = 1 },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Remanejos", tint = if (telaSelecionada == 1) LaranjaUltra else Color.LightGray) },
                    label = { Text("Remanejos", color = if (telaSelecionada == 1) LaranjaUltra else Color.LightGray) })
                NavigationBarItem(selected = telaSelecionada == 2, onClick = { telaSelecionada = 2 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Demandas", tint = if (telaSelecionada == 2) LaranjaUltra else Color.LightGray) },
                    label = { Text("Demandas", color = if (telaSelecionada == 2) LaranjaUltra else Color.LightGray) })
                NavigationBarItem(selected = telaSelecionada == 3, onClick = { telaSelecionada = 3 },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Leitor EAN", tint = if (telaSelecionada == 3) LaranjaUltra else Color.LightGray) },
                    label = { Text("Leitor EAN", color = if (telaSelecionada == 3) LaranjaUltra else Color.LightGray) })
                NavigationBarItem(selected = telaSelecionada == 4, onClick = { telaSelecionada = 4 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Assistente", tint = if (telaSelecionada == 4) LaranjaUltra else Color.LightGray) },
                    label = { Text("Assistente", color = if (telaSelecionada == 4) LaranjaUltra else Color.LightGray) })
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF4F7F6))
                .padding(16.dp)
        ) {
            if (telaSelecionada == 0) {
                val agora = System.currentTimeMillis()
                val demandasAtivas = demandas.filter {
                    !it.concluida && it.status != StatusDemanda.CONCLUIDA &&
                        it.status != StatusDemanda.CANCELADA
                }
                val totalAtrasadas = demandasAtivas.count { it.dataPrazoFinal != Long.MAX_VALUE && it.dataPrazoFinal < agora }
                val totalPendentes = demandasAtivas.count {
                    it.status == StatusDemanda.PENDENTE && (it.dataPrazoFinal == Long.MAX_VALUE || it.dataPrazoFinal >= agora)
                }
                val totalEmAndamento = demandasAtivas.count {
                    it.status != StatusDemanda.PENDENTE && (it.dataPrazoFinal == Long.MAX_VALUE || it.dataPrazoFinal >= agora)
                }
                val totalConcluidas = demandas.count {
                    it.concluida || it.status == StatusDemanda.CONCLUIDA
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusResumoCard(
                            titulo = "PENDENTES",
                            quantidade = totalPendentes,
                            icone = Icons.Default.Assignment,
                            cor = Color(0xFF287A9B),
                            modifier = Modifier.weight(1f)
                        )
                        StatusResumoCard(
                            titulo = "EM ANDAMENTO",
                            quantidade = totalEmAndamento,
                            icone = Icons.Default.Autorenew,
                            cor = Color(0xFF087E83),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusResumoCard(
                            titulo = "ATRASADAS",
                            quantidade = totalAtrasadas,
                            icone = Icons.Default.WarningAmber,
                            cor = VermelhoAtraso,
                            modifier = Modifier.weight(1f)
                        )
                        StatusResumoCard(
                            titulo = "CONCLUÍDAS",
                            quantidade = totalConcluidas,
                            icone = Icons.Default.CheckCircle,
                            cor = Color(0xFF388E3C),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Checklist, contentDescription = null, tint = AzulMarinho)
                                Spacer(Modifier.width(8.dp))
                                Text("DEMANDAS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${demandasAtivas.size} pendência(s) em aberto • ${totalAtrasadas} atrasada(s)",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { telaSelecionada = 2 }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.FormatListBulleted, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ver demandas")
                                }
                                Button(
                                    onClick = { showDialogCriar = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = LaranjaUltra)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Adicionar")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = AzulMarinho)
                                Spacer(Modifier.width(8.dp))
                                Text("PAINEL DE PRODUTIVIDADE", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text("Avarias, vencidos e produtos próximos do vencimento.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { telaSelecionada = 5 },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LaranjaUltra)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Abrir relatórios")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            cicloCd.closed -> Color(0xFFE8F5E9)
                            cicloCd.overdue -> Color(0xFFFFEBEE)
                            else -> Color(0xFFFFFFFF)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = if (cicloCd.overdue && !cicloCd.closed) VermelhoAtraso else AzulMarinho
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cicloCd.title, fontWeight = FontWeight.Bold, color = AzulMarinho)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(cicloCd.description, fontSize = 14.sp, color = AzulMarinho)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ciclo referente a ${cicloCd.month.format(DateTimeFormatter.ofPattern("MMMM/yyyy"))}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        if (!cicloCd.closed) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showDialogFecharCd = true },
                                colors = ButtonDefaults.buttonColors(containerColor = LaranjaUltra),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Marcar envio como concluído")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ciclo encerrado manualmente.", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                }
            } else if (telaSelecionada == 1) {
                RemanejosScreen(db = db)
            } else if (telaSelecionada == 2) {
                OutlinedTextField(
                    value = filtroDemandas,
                    onValueChange = { filtroDemandas = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar demanda") },
                    placeholder = { Text("Digite parte do título ou descrição") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (filtroDemandas.isNotBlank()) {
                            IconButton(onClick = { filtroDemandas = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                val demandasFiltradas = demandas.filter {
                    it.titulo.contains(filtroDemandas, ignoreCase = true) ||
                        it.descricao.contains(filtroDemandas, ignoreCase = true)
                }
                if (demandasFiltradas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(if (demandas.isEmpty()) "Nenhuma demanda cadastrada ainda." else "Nenhuma demanda encontrada.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(demandasFiltradas.size) { i ->
                            val item = demandasFiltradas[i]
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(item.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulMarinho)
                                    Text("Categoria: ${item.categoria.label} | Prioridade: ${item.prioridade.label}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            } else if (telaSelecionada == 3) {
                LeitorEanScreen()
            } else if (telaSelecionada == 4) {
                AssistenteOperacionalScreen(demandas = demandas)
            } else if (telaSelecionada == 5) {
                CentralCobrancasScreen(db = db, demandas = demandas)
            } else {
                RelatoriosScreen(db = db)
            }
        }
    }


    if (mostrarSeletorData) {
        DatePickerDialog(
            onDismissRequest = { mostrarSeletorData = false },
            confirmButton = {
                TextButton(onClick = {
                    seletorDataState.selectedDateMillis?.let { millis ->
                        dataPrazoSelecionada = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    mostrarSeletorData = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSeletorData = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = seletorDataState)
        }
    }

    if (showDialogFecharCd) {
        AlertDialog(
            onDismissRequest = { showDialogFecharCd = false },
            title = { Text("Confirmar envio ao CD") },
            text = { Text("Confirma que os produtos vencidos do mês de referência foram enviados ao CD? O histórico do ciclo será preservado.") },
            confirmButton = {
                TextButton(onClick = {
                    MonthlyCdCycle.closeCurrentCycle(ctx, hoje)
                    atualizarCiclo()
                    showDialogFecharCd = false
                }) { Text("Confirmar conclusão") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogFecharCd = false }) { Text("Voltar") }
            }
        )
    }

    if (showDialogCriar) {
        AlertDialog(
            onDismissRequest = { showDialogCriar = false },
            title = { Text("Nova Demanda Operacional") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = novoTitulo,
                        onValueChange = { novoTitulo = it },
                        label = { Text("O que precisa ser feito?") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text("Prazo para realização", fontWeight = FontWeight.SemiBold, color = AzulMarinho)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !prazoIndeterminado, onClick = { prazoIndeterminado = false })
                        Text("Com prazo")
                        Spacer(modifier = Modifier.width(12.dp))
                        RadioButton(selected = prazoIndeterminado, onClick = { prazoIndeterminado = true })
                        Text("Indeterminado")
                    }
                    if (!prazoIndeterminado) {
                        OutlinedButton(
                            onClick = { mostrarSeletorData = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Prazo: ${dataPrazoSelecionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}")
                        }
                    } else {
                        Text("Essa demanda não vencerá automaticamente.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = LaranjaUltra),
                    onClick = {
                        if (novoTitulo.isNotBlank()) {
                            scope.launch {
                                db.demandaDao().insertDemanda(
                                    DemandaEntity(
                                        titulo = novoTitulo,
                                        categoria = CategoriaDemanda.RECEBIMENTO,
                                        prioridade = Prioridade.ALTA,
                                        dataPrazoFinal = if (prazoIndeterminado) Long.MAX_VALUE else dataPrazoSelecionada.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1L
                                    )
                                )
                                novoTitulo = ""
                                prazoIndeterminado = false
                                dataPrazoSelecionada = LocalDate.now().plusDays(1)
                                showDialogCriar = false
                            }
                        }
                    }
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogCriar = false }) { Text("Cancelar") }
            }
        )
    }
}


@Composable
private fun StatusResumoCard(
    titulo: String,
    quantidade: Int,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = quantidade.toString(),
                color = AzulMarinho,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = titulo,
                color = cor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.35.sp
            )
        }
    }
}
