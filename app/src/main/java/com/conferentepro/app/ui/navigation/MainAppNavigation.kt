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
fun MainAppNavigation(isDarkTheme: Boolean, onToggleTheme: () -> Unit) {
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
    val ocorrenciasAbertas by db.produtoDao().getTodasOcorrencias().collectAsState(initial = emptyList())
    val cobrancas by db.cobrancaDao().getTodas().collectAsState(initial = emptyList())
    val remanejos by db.remanejoDao().getTodos().collectAsState(initial = emptyList())

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
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Ativar tema claro" else "Ativar tema escuro",
                            tint = Color.White
                        )
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
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Remanejos", tint = if (telaSelecionada == 1) LaranjaUltra else Color.LightGray) },
                    label = { Text("Remanejos", color = if (telaSelecionada == 1) LaranjaUltra else Color.LightGray) })
                NavigationBarItem(selected = telaSelecionada == 2, onClick = { telaSelecionada = 2 },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Leitor EAN", tint = if (telaSelecionada == 2) LaranjaUltra else Color.LightGray) },
                    label = { Text("Leitor EAN", color = if (telaSelecionada == 2) LaranjaUltra else Color.LightGray) })
                NavigationBarItem(selected = telaSelecionada == 3, onClick = { telaSelecionada = 3 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Assistente", tint = if (telaSelecionada == 3) LaranjaUltra else Color.LightGray) },
                    label = { Text("Assistente", color = if (telaSelecionada == 3) LaranjaUltra else Color.LightGray) })
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
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
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val saudacao = when (java.time.LocalTime.now().hour) {
                        in 5..11 -> "Bom dia"
                        in 12..17 -> "Boa tarde"
                        else -> "Boa noite"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = AzulMarinho),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(saudacao + "!", color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
                                Text("Visão geral da operação", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(hoje.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", java.util.Locale("pt", "BR"))),
                                    color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                            }
                            Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.12f)) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = LaranjaUltra,
                                    modifier = Modifier.padding(13.dp).size(30.dp))
                            }
                        }
                    }
                    Text("CICLO DO CD", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                cicloCd.closed -> Color(0xFFEAF6EE)
                                cicloCd.overdue -> Color(0xFFFFF0EF)
                                else -> Color(0xFFEAF3F4)
                            }
                        ),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = when {
                                            cicloCd.closed -> Color(0xFF2E7D32)
                                            cicloCd.overdue -> VermelhoAtraso
                                            else -> AzulMarinho
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cicloCd.title, fontWeight = FontWeight.Bold, color = AzulMarinho, fontSize = 15.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(cicloCd.description, fontSize = 12.sp, color = AzulMarinho)
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    "Referência: produtos vencidos em ${cicloCd.month.format(DateTimeFormatter.ofPattern("MM/yyyy"))}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    "Data limite: 15/${cicloCd.deadline.format(DateTimeFormatter.ofPattern("MM/yyyy"))}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                                if (!cicloCd.closed) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showDialogFecharCd = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = LaranjaUltra),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Marcar envio concluído", fontSize = 12.sp)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("✓ Envio concluído", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = when {
                                    cicloCd.closed -> Color(0xFFD7EEDC)
                                    cicloCd.overdue -> Color(0xFFFFDAD6)
                                    cicloCd.daysRemaining <= 3 -> Color(0xFFFFE8C2)
                                    else -> Color(0xFFD6E9EB)
                                }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        when {
                                            cicloCd.closed -> "OK"
                                            cicloCd.overdue -> "ATRASO"
                                            else -> "FALTAM"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            cicloCd.closed -> Color(0xFF2E7D32)
                                            cicloCd.overdue -> VermelhoAtraso
                                            cicloCd.daysRemaining <= 3 -> Color(0xFF9A5B00)
                                            else -> AzulMarinho
                                        }
                                    )
                                    Text(
                                        if (cicloCd.closed) "✓" else if (cicloCd.overdue) "+${java.time.temporal.ChronoUnit.DAYS.between(cicloCd.deadline, hoje)}" else cicloCd.daysRemaining.toString(),
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when {
                                            cicloCd.closed -> Color(0xFF2E7D32)
                                            cicloCd.overdue -> VermelhoAtraso
                                            cicloCd.daysRemaining <= 3 -> Color(0xFF9A5B00)
                                            else -> AzulMarinho
                                        }
                                    )
                                    Text(
                                        if (cicloCd.overdue) "dias" else if (cicloCd.closed) "ENVIADO" else "DIAS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    Text("DEMANDAS OPERACIONAIS", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                    Text("ACOMPANHAMENTO", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                OutlinedButton(onClick = { telaSelecionada = 7 }, modifier = Modifier.weight(1f)) {
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
                    Text("PRODUTOS", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assessment, contentDescription = null, tint = AzulMarinho)
                                Spacer(Modifier.width(8.dp))
                                Text("CONTROLE DE PRODUTOS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text("Consulte avarias, produtos vencidos e próximos do vencimento.", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { telaSelecionada = 8 },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = LaranjaUltra)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Abrir controle de produtos")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    val semAssinatura = remanejos.filter { it.assinaturaBase64.isBlank() }.sortedBy { it.dataRegistro }
                    val ocorrenciasPendentes = ocorrenciasAbertas.filter { it.status.equals("PENDENTE", true) }
                    val cobrancasPendentes = cobrancas.filter { !it.status.equals("RESOLVIDA", true) && !it.status.equals("CONCLUIDA", true) }
                    Spacer(Modifier.height(12.dp))
                    Text("ATENÇÃO NECESSÁRIA", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("PAINEL DE PENDÊNCIAS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusResumoCard("SEM ASSINATURA", semAssinatura.size, Icons.Default.Draw, Color(0xFFB45309), Modifier.weight(1f))
                                StatusResumoCard("OCORRÊNCIAS", ocorrenciasPendentes.size, Icons.Default.WarningAmber, VermelhoAtraso, Modifier.weight(1f))
                            }
                            StatusResumoCard("COBRANÇAS PENDENTES", cobrancasPendentes.size, Icons.Default.NotificationsActive, Color(0xFF287A9B), Modifier.fillMaxWidth())
                            Text("REMANEJOS AGUARDANDO ASSINATURA", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (semAssinatura.isEmpty()) {
                                Text("Nenhum remanejo aguardando assinatura.", color = Color(0xFF388E3C), fontSize = 12.sp)
                            } else {
                                semAssinatura.take(10).forEach { item ->
                                    val dias = ((agora - item.dataRegistro).coerceAtLeast(0L) / 86400000L).toInt()
                                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(10.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("NF " + item.numeroNota, fontWeight = FontWeight.SemiBold, color = AzulMarinho)
                                            Text(if (dias == 0) "Hoje" else if (dias == 1) "1 dia" else "$dias dias", color = if (dias >= 3) VermelhoAtraso else Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                        }
                                        Text(item.filialSaida + " → " + item.filialDestino, fontSize = 11.sp)
                                        Text("Criado em " + java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date(item.dataRegistro)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (semAssinatura.size > 10) Text("E mais " + (semAssinatura.size - 10) + " remanejo(s).", fontSize = 11.sp, color = Color.Gray)
                            }
                            HorizontalDivider()
                            Text("COBRANÇAS PENDENTES", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            if (cobrancasPendentes.isEmpty()) Text("Nenhuma cobrança pendente.", fontSize = 12.sp)
                            else cobrancasPendentes.take(5).forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(item.destino + " • " + item.referencia, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Contato: " + item.pessoaContatada + " • Próxima cobrança: " + (item.proximaCobranca?.let { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("pt", "BR")).format(java.util.Date(it)) } ?: "não definida"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else if (telaSelecionada == 1) {
                RemanejosScreen(db = db)
            } else if (telaSelecionada == 7) {
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
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(item.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulMarinho)
                                    Text("Categoria: ${item.categoria.label} | Prioridade: ${item.prioridade.label}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            } else if (telaSelecionada == 2) {
                LeitorEanScreen()
            } else if (telaSelecionada == 3) {
                AssistenteOperacionalScreen(demandas = demandas, remanejos = remanejos, ocorrencias = ocorrenciasAbertas, cobrancas = cobrancas)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
