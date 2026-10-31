package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

val AzulMarinho = Color(0xFF0B192C)
val LaranjaUltra = Color(0xFFFF6500)
val VermelhoAtraso = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation() {
    val ctx = LocalContext.current
    val db = remember { AppDatabase.getInstance(ctx) }
    val scope = rememberCoroutineScope()
    var telaSelecionada by remember { mutableStateOf(0) }
    var showDialogCriar by remember { mutableStateOf(false) }
    var novoTitulo by remember { mutableStateOf("") }
    val demandas by db.demandaDao().getAllDemandas().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CONFERENTE PRO", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulMarinho)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AzulMarinho) {
                NavigationBarItem(
                    selected = telaSelecionada == 0,
                    onClick = { telaSelecionada = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Início", tint = if (telaSelecionada == 0) LaranjaUltra else Color.LightGray) },
                    label = { Text("Início", color = if (telaSelecionada == 0) LaranjaUltra else Color.LightGray) }
                )
                NavigationBarItem(
                    selected = telaSelecionada == 1,
                    onClick = { telaSelecionada = 1 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Demandas", tint = if (telaSelecionada == 1) LaranjaUltra else Color.LightGray) },
                    label = { Text("Demandas", color = if (telaSelecionada == 1) LaranjaUltra else Color.LightGray) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = LaranjaUltra,
                contentColor = Color.White,
                onClick = { showDialogCriar = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Demanda")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F7FA))
                .padding(16.dp)
        ) {
            if (telaSelecionada == 0) {
                // Card Atenção Necessária
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VermelhoAtraso.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = VermelhoAtraso)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ATENÇÃO NECESSÁRIA", fontWeight = FontWeight.Bold, color = VermelhoAtraso)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total de pendências registradas: ${demandas.size}", fontSize = 14.sp)
                    }
                }
            } else {
                // Lista de Demandas
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(demandas.size) { i ->
                        val item = demandas[i]
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(item.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulMarinho)
                                Text("Categoria: ${item.categoria.label} | Prioridade: ${item.prioridade.label}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialogCriar) {
        AlertDialog(
            onDismissRequest = { showDialogCriar = false },
            title = { Text("Nova Demanda Operacional") },
            text = {
                OutlinedTextField(
                    value = novoTitulo,
                    onValueChange = { novoTitulo = it },
                    label = { Text("O que precisa ser feito?") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                                        dataPrazoFinal = System.currentTimeMillis() + 86400000L
                                    )
                                )
                                novoTitulo = ""
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
