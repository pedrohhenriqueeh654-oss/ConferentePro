package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conferentepro.app.data.local.entity.DemandaEntity
import com.conferentepro.app.domain.model.StatusDemanda
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class MensagemAssistente(val texto: String, val doUsuario: Boolean)

@Composable
fun AssistenteOperacionalScreen(demandas: List<DemandaEntity>) {
    var pergunta by remember { mutableStateOf("") }
    var mensagens by remember {
        mutableStateOf(
            listOf(MensagemAssistente("Olá! Posso ajudar a analisar recebimentos, avarias, solicitações ao CD e trocas com fornecedores. Escolha um assunto ou escreva sua pergunta.", false))
        )
    }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AzulMarinho)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LaranjaUltra, modifier = Modifier.size(30.dp))
                Column {
                    Text("Assistente Operacional", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Apoio a recebimento, CD, avarias e fornecedores", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
        Text("ANÁLISE DAS SUAS DEMANDAS", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        val agora = System.currentTimeMillis()
        val abertas = demandas.filter { !it.concluida && it.status != StatusDemanda.CONCLUIDA && it.status != StatusDemanda.CANCELADA }
        val atrasadas = abertas.filter { it.dataPrazoFinal != Long.MAX_VALUE && it.dataPrazoFinal < agora }
        val aguardando = abertas.filter { it.status == StatusDemanda.AGUARDANDO_CD || it.status == StatusDemanda.AGUARDANDO_FORNECEDOR || it.status == StatusDemanda.AGUARDANDO_TERCEIROS }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${abertas.size} demandas em aberto • ${atrasadas.size} atrasadas", color = AzulMarinho, fontWeight = FontWeight.SemiBold)
                Text("${aguardando.size} aguardando CD, fornecedor ou terceiros", color = Color.DarkGray, fontSize = 13.sp)
                if (atrasadas.isNotEmpty()) {
                    Text("Prioridade de atenção: verifique os prazos vencidos e registre a cobrança ou atualização no histórico.", color = Color(0xFFB3261E), fontSize = 12.sp)
                } else {
                    Text("Não há demandas vencidas no momento. Continue registrando os retornos e movimentações.", color = Color(0xFF287A55), fontSize = 12.sp)
                }
            }
        }
        Text("CONVERSA", color = AzulMarinho, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mensagens) { mensagem ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (mensagem.doUsuario) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (mensagem.doUsuario) Color(0xFFDDF3EF) else Color.White,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Text(mensagem.texto, modifier = Modifier.padding(12.dp), color = AzulMarinho, fontSize = 13.sp)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pergunta,
                onValueChange = { pergunta = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ex.: o que está atrasado?") },
                singleLine = true
            )
            IconButton(
                onClick = {
                    val texto = pergunta.trim()
                    if (texto.isNotEmpty()) {
                        mensagens = mensagens + MensagemAssistente(texto, true) + MensagemAssistente(responderOperacionalmente(texto, demandas), false)
                        pergunta = ""
                    }
                },
                enabled = pergunta.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar pergunta", tint = AzulMarinho)
            }
        }
        Text("Esta etapa funciona localmente com regras operacionais. A conexão com IA generativa será adicionada depois, sem bloquear o uso offline.", color = Color.Gray, fontSize = 10.sp)
    }
}

private fun responderOperacionalmente(pergunta: String, demandas: List<DemandaEntity>): String {
    val q = pergunta.lowercase()
    val agora = System.currentTimeMillis()
    val abertas = demandas.filter { !it.concluida && it.status != StatusDemanda.CONCLUIDA && it.status != StatusDemanda.CANCELADA }
    val atrasadas = abertas.filter { it.dataPrazoFinal != Long.MAX_VALUE && it.dataPrazoFinal < agora }
    val doCd = abertas.filter { it.status == StatusDemanda.AGUARDANDO_CD }
    val fornecedor = abertas.filter { it.status == StatusDemanda.AGUARDANDO_FORNECEDOR }
    val terceiros = abertas.filter { it.status == StatusDemanda.AGUARDANDO_TERCEIROS }
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

    return when {
        listOf("atras", "vencid", "prazo").any { q.contains(it) } -> {
            if (atrasadas.isEmpty()) "Não encontrei demandas atrasadas. Confira se os prazos cadastrados estão corretos."
            else "Encontrei ${atrasadas.size} demanda(s) atrasada(s):\n" + atrasadas.take(8).joinToString("\n") {
                "• ${it.titulo} — prazo ${fmt.format(Instant.ofEpochMilli(it.dataPrazoFinal))}"
            } + "\nSugestão: confirme o último responsável, registre a cobrança e atualize a previsão. Não marque como concluída sem confirmação."
        }
        q.contains("fornecedor") || q.contains("troca") -> listarAguardando("fornecedor", fornecedor)
        q.contains("cd") || q.contains("centro de distribu") -> listarAguardando("CD", doCd)
        q.contains("terceir") -> listarAguardando("terceiros", terceiros)
        q.contains("resumo") || q.contains("situa") || q.contains("pend") -> {
            "Resumo operacional:\n• ${abertas.size} demanda(s) em aberto\n• ${atrasadas.size} atrasada(s)\n• ${doCd.size} aguardando CD\n• ${fornecedor.size} aguardando fornecedor\n• ${terceiros.size} aguardando terceiros\n\nSugestão: priorize primeiro as vencidas e depois as solicitações externas mais antigas."
        }
        q.contains("avaria") || q.contains("danific") -> "Para registrar uma avaria, descreva produto, código/EAN, quantidade, condição encontrada, data do recebimento e evidências fotográficas. Depois registre o protocolo e se a tratativa ficará com o CD ou fornecedor."
        q.contains("receb") || q.contains("confer") -> "Na conferência, compare produto, código/EAN e quantidade com o documento de entrada. Registre divergências separadamente, informe o tipo de ocorrência e mantenha evidências e protocolo vinculados à demanda."
        else -> "Posso consultar demandas atrasadas, resumir pendências e separar o que aguarda CD, fornecedor ou terceiros. Também posso orientar o registro de avarias e divergências. Experimente perguntar: ‘O que está atrasado?’ ou ‘O que aguarda o fornecedor?’"
    }
}

private fun listarAguardando(nome: String, lista: List<DemandaEntity>): String {
    if (lista.isEmpty()) return "Não há demandas abertas marcadas como aguardando ${nome}."
    return "${lista.size} demanda(s) aguardando ${nome}:\n" + lista.take(8).joinToString("\n") { "• ${it.titulo}" } +
        "\nSugestão: registre a data do último contato, protocolo e previsão de retorno. A demanda só deve ser encerrada após confirmação."
}
