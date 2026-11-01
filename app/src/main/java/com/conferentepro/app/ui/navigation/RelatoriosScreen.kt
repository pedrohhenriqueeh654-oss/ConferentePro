package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conferentepro.app.data.local.AppDatabase
import com.conferentepro.app.data.local.entity.OcorrenciaProdutoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RelatoriosScreen(db: AppDatabase) {
    val ocorrencias by db.produtoDao().getTodasOcorrencias().collectAsState(initial = emptyList())
    val produtos by db.produtoDao().getProdutos().collectAsState(initial = emptyList())
    val nomesProdutos = remember(produtos) { produtos.associateBy { it.ean } }
    val inicioHoje = remember {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }
    val limite30Dias = inicioHoje + TimeUnit.DAYS.toMillis(30)
    val avarias = ocorrencias.filter { it.tipo.equals("AVARIA", ignoreCase = true) }
    // Um item marcado explicitamente como VENCIMENTO/VENCIDO deve aparecer mesmo sem data de validade preenchida.
    val vencidos = ocorrencias.filter {
        it.tipo.equals("VENCIMENTO", ignoreCase = true) ||
            it.destino.equals("VENCIDO", ignoreCase = true) ||
            (it.validade != null && it.validade < inicioHoje)
    }
    val proximos = ocorrencias.filter { it !in vencidos && it.validade != null && it.validade >= inicioHoje && it.validade <= limite30Dias }
        .sortedBy { it.validade }
    val vencidosEProximos = vencidos + proximos
    val formato = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }

    Column(Modifier.fillMaxSize()) {
        Text("PAINEL DE PRODUTIVIDADE", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Indicadores calculados automaticamente a partir das fichas de produto.", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RelatorioCard("AVARIAS", avarias.size, avarias.sumOf { it.quantidade }, Icons.Default.WarningAmber, Color(0xFFD32F2F), Modifier.weight(1f))
                    RelatorioCard("VENCIDOS", vencidos.size, vencidos.sumOf { it.quantidade }, Icons.Default.EventBusy, Color(0xFF9C332D), Modifier.weight(1f))
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = Color(0xFF087E83))
                            Spacer(Modifier.width(8.dp))
                            Text("VENCIDOS OU PRÓXIMOS DO VENCIMENTO", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(vencidosEProximos.size.toString(), color = AzulMarinho, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                        Text("${vencidosEProximos.map { it.produtoEan }.distinct().size} produto(s) • ${vencidosEProximos.sumOf { it.quantidade }} unidade(s)", color = Color.Gray, fontSize = 12.sp)
                        Text("Alerta de vencimento configurado para os próximos 30 dias.", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
            item {
                Text("DETALHAMENTO DE VALIDADES", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (vencidosEProximos.isEmpty()) {
                item { Text("Nenhum produto vencido ou próximo do vencimento foi encontrado nas ocorrências registradas.", color = Color.Gray, fontSize = 13.sp) }
            } else {
                items(vencidosEProximos, key = { it.id }) { item ->
                    ValidadeRelatorioItem(
                        item = item,
                        vencido = item in vencidos,
                        nomeProduto = nomesProdutos[item.produtoEan]?.nome ?: "Produto não identificado",
                        dataFormatada = item.validade?.let { formato.format(Date(it)) } ?: "Não informada"
                    )
                }
            }
            item {
                Text("RESUMO DOS REGISTROS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Avarias: ${avarias.size} ocorrência(s), ${avarias.sumOf { it.quantidade }} unidade(s).", fontSize = 12.sp, color = Color.Gray)
                Text("Vencimentos passados: ${vencidos.size} ocorrência(s), ${vencidos.sumOf { it.quantidade }} unidade(s).", fontSize = 12.sp, color = Color.Gray)
                Text("Próximos 30 dias: ${proximos.size} ocorrência(s), ${proximos.sumOf { it.quantidade }} unidade(s).", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun RelatorioCard(
    titulo: String,
    ocorrencias: Int,
    unidades: Double,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(ocorrencias.toString(), color = AzulMarinho, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(titulo, color = cor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("$unidades unidade(s)", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ValidadeRelatorioItem(item: OcorrenciaProdutoEntity, vencido: Boolean, nomeProduto: String, dataFormatada: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (vencido) "VENCIDO" else "PRÓXIMO DO VENCIMENTO", color = if (vencido) Color(0xFFD32F2F) else Color(0xFF087E83), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(nomeProduto, color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("EAN: ${item.produtoEan}", color = AzulMarinho, fontWeight = FontWeight.SemiBold)
            Text("Validade: $dataFormatada • Quantidade: ${item.quantidade} ${item.unidade}", fontSize = 12.sp)
            Text("Registrado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(item.dataRegistro))}", fontSize = 11.sp, color = Color.Gray)
            Text("Tipo: ${item.tipo} • Destino: ${item.destino}", color = Color.Gray, fontSize = 11.sp)
            if (item.lote.isNotBlank()) Text("Lote: ${item.lote}", color = Color.Gray, fontSize = 11.sp)
        }
    }
}
