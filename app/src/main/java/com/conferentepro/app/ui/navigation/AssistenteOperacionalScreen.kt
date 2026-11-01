package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.background
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
import com.conferentepro.app.data.local.entity.*
import com.conferentepro.app.domain.model.StatusDemanda
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

private data class MensagemAssistente(val texto: String, val doUsuario: Boolean)
private data class PerguntaRapida(val titulo: String, val pergunta: String)

@Composable
fun AssistenteOperacionalScreen(
    demandas: List<DemandaEntity>,
    remanejos: List<RemanejoEntity>,
    ocorrencias: List<OcorrenciaProdutoEntity>,
    cobrancas: List<CobrancaEntity>
) {
    var pergunta by remember { mutableStateOf("") }
    var mensagens by remember {
        mutableStateOf(listOf(MensagemAssistente(
            "Olá! Sou o assistente local do Conferente Pro. Posso consultar os registros salvos neste aparelho: remanejos e NFs, assinaturas, demandas, ocorrências, EANs e cobranças.\n\nFaça uma pergunta completa ou toque em uma sugestão abaixo. Minhas respostas são baseadas nos dados cadastrados no aplicativo.",
            false
        )))
    }
    val perguntas = listOf(
        PerguntaRapida("NFs sem assinatura", "Quais remanejos estão sem assinatura?"),
        PerguntaRapida("Consultar uma NF", "Localize a NF "),
        PerguntaRapida("Remanejos recentes", "Mostre os remanejos mais recentes"),
        PerguntaRapida("Demandas atrasadas", "Quais demandas estão atrasadas?"),
        PerguntaRapida("Ocorrências pendentes", "Quais ocorrências estão pendentes?"),
        PerguntaRapida("Cobranças", "Quais cobranças precisam de retorno?"),
        PerguntaRapida("Resumo geral", "Faça um resumo geral da operação")
    )
    val abertas = demandas.filter { !it.concluida && it.status != StatusDemanda.CONCLUIDA && it.status != StatusDemanda.CANCELADA }
    val atrasadas = abertas.filter { it.dataPrazoFinal != Long.MAX_VALUE && it.dataPrazoFinal < System.currentTimeMillis() }
    val semAssinatura = remanejos.count { it.assinaturaBase64.isBlank() }
    val ocorrenciasPendentes = ocorrencias.count { it.status.equals("PENDENTE", true) }
    val cobrancasPendentes = cobrancas.count { !it.status.equals("RESOLVIDA", true) && !it.status.equals("CONCLUIDA", true) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = AzulMarinho)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.12f)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = LaranjaUltra, modifier = Modifier.padding(11.dp).size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Assistente Operacional", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Consulta inteligente dos dados deste aparelho", color = Color.White.copy(alpha = .78f), fontSize = 11.sp)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF1F665F)) {
                    Text("OFFLINE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MiniIndicadorAssistente("Remanejos", remanejos.size.toString(), Modifier.weight(1f))
            MiniIndicadorAssistente("Sem assinatura", semAssinatura.toString(), Modifier.weight(1f))
            MiniIndicadorAssistente("Demandas atrasadas", atrasadas.size.toString(), Modifier.weight(1f))
        }
        Text("PERGUNTAS RÁPIDAS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(perguntas) { item ->
                SuggestionChip(
                    onClick = { pergunta = item.pergunta },
                    label = { Text(item.titulo, fontSize = 11.sp) }
                )
            }
        }
        Text("CONVERSA", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mensagens) { mensagem ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mensagem.doUsuario) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (mensagem.doUsuario) Color(0xFFDDF3EF) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(if (mensagem.doUsuario) 20.dp else 8.dp, 20.dp, 20.dp, 20.dp),
                        modifier = Modifier.widthIn(max = 330.dp),
                        tonalElevation = if (mensagem.doUsuario) 0.dp else 1.dp
                    ) {
                        Text(mensagem.texto, modifier = Modifier.padding(13.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = pergunta,
                onValueChange = { pergunta = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pergunte sobre NF, EAN, prazo...") },
                maxLines = 3,
                shape = RoundedCornerShape(18.dp)
            )
            IconButton(
                onClick = {
                    val texto = pergunta.trim()
                    if (texto.isNotEmpty()) {
                        mensagens = mensagens + MensagemAssistente(texto, true) +
                            MensagemAssistente(responderOperacionalmente(texto, demandas, remanejos, ocorrencias, cobrancas), false)
                        pergunta = ""
                    }
                },
                enabled = pergunta.isNotBlank(),
                modifier = Modifier.size(50.dp).background(AzulMarinho, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar pergunta", tint = Color.White)
            }
        }
        Text("Consulta local: o assistente não inventa registros. Se uma informação não estiver cadastrada, ele avisará.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
    }
}

@Composable
private fun MiniIndicadorAssistente(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(valor, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AzulMarinho)
            Text(titulo, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 11.sp)
        }
    }
}

private fun responderOperacionalmente(
    pergunta: String,
    demandas: List<DemandaEntity>,
    remanejos: List<RemanejoEntity>,
    ocorrencias: List<OcorrenciaProdutoEntity>,
    cobrancas: List<CobrancaEntity>
): String {
    val q = normalizarAssistente(pergunta)
    val agora = System.currentTimeMillis()
    val abertas = demandas.filter { !it.concluida && it.status != StatusDemanda.CONCLUIDA && it.status != StatusDemanda.CANCELADA }
    val atrasadas = abertas.filter { it.dataPrazoFinal != Long.MAX_VALUE && it.dataPrazoFinal < agora }
    val semAssinatura = remanejos.filter { it.assinaturaBase64.isBlank() }.sortedBy { it.dataRegistro }
    val ocorrenciasPendentes = ocorrencias.filter { it.status.equals("PENDENTE", true) }
    val cobrancasPendentes = cobrancas.filter { !it.status.equals("RESOLVIDA", true) && !it.status.equals("CONCLUIDA", true) }
    val data = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    val dataCurta = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    val nfInformada = Regex("(?:nf|nota(?: fiscal)?|remanejo)\\s*(?:n[º°.]?\\s*)?([a-z0-9./-]+)", RegexOption.IGNORE_CASE)
        .find(pergunta)?.groupValues?.getOrNull(1)?.trim()
    if (nfInformada != null) {
        val encontrados = remanejos.filter { it.numeroNota.trim().equals(nfInformada, true) || it.numeroNota.contains(nfInformada, true) }
        if (encontrados.isEmpty()) return "Não encontrei remanejo com a NF '$nfInformada' nos registros locais. Confira se o número foi digitado corretamente ou se o remanejo já foi cadastrado."
        return "CONSULTA DA NF $nfInformada\n" + encontrados.joinToString("\n\n") {
            "• NF: ${it.numeroNota}\n• Origem: ${it.filialSaida}\n• Destino: ${it.filialDestino}\n• Retirado por: ${it.retiradoPor.ifBlank { "não informado" }}\n• Assinatura: ${if (it.assinaturaBase64.isBlank()) "NÃO ASSINADO" else "ASSINADO"}\n• Registro: ${data.format(Date(it.dataRegistro))}\n• Observações: ${it.observacoes.ifBlank { "nenhuma" }}"
        }
    }

    when {
        listOf("sem assinatura", "nao assinado", "nao assinados", "pendente de assinatura", "falta assinar").any { q.contains(it) } ->
            return if (semAssinatura.isEmpty()) "Ótimo: não há remanejos sem assinatura cadastrados."
            else "REMANEJOS SEM ASSINATURA — ${semAssinatura.size}\n" + semAssinatura.joinToString("\n\n") {
                "NF ${it.numeroNota} • ${it.filialSaida} → ${it.filialDestino}\nRegistrado: ${data.format(Date(it.dataRegistro))}\nRetirado por: ${it.retiradoPor.ifBlank { "não informado" }}"
            }
        listOf("remanejo", "remanejos", "nota fiscal", "notas fiscais", "nf").any { q.contains(it) } ->
            return if (remanejos.isEmpty()) "Ainda não há remanejos cadastrados."
            else {
                val lista = remanejos.sortedByDescending { it.dataRegistro }.take(15)
                "REMANEJOS MAIS RECENTES — mostrando ${lista.size} de ${remanejos.size}\n" + lista.joinToString("\n\n") {
                    "NF ${it.numeroNota} • ${it.filialSaida} → ${it.filialDestino}\n${if (it.assinaturaBase64.isBlank()) "NÃO ASSINADO" else "ASSINADO"} • ${data.format(Date(it.dataRegistro))}"
                }
            }
        listOf("ocorrencia", "ocorrencias", "avaria", "avarias", "vencid", "validade", "ean", "codigo de barras", "produto").any { q.contains(it) } -> {
            val termo = Regex("\\b\\d{8,14}\\b").find(pergunta)?.value
            val lista = if (termo != null) ocorrencias.filter { it.produtoEan.contains(termo) } else if (q.contains("pend")) ocorrenciasPendentes else ocorrencias
            return if (lista.isEmpty()) "Não encontrei ocorrências ${if (termo != null) "para o EAN $termo" else "com esse critério"} nos registros locais."
            else "OCORRÊNCIAS — ${lista.size} registro(s)\n" + lista.sortedByDescending { it.dataRegistro }.take(20).joinToString("\n\n") {
                "EAN ${it.produtoEan} • ${it.tipo}\nDestino: ${it.destino} • Quantidade: ${it.quantidade} ${it.unidade}\nLote: ${it.lote.ifBlank { "não informado" }} • Validade: ${it.validade?.let { v -> dataCurta.format(Date(v)) } ?: "não informada"}\nStatus: ${it.status} • Protocolo: ${it.protocolo.ifBlank { "não informado" }}\nRegistro: ${data.format(Date(it.dataRegistro))}\nObservações: ${it.observacoes.ifBlank { "nenhuma" }}"
            }
        }
        listOf("cobranca", "cobrancas", "cobrar", "retorno", "fornecedor", "contato").any { q.contains(it) } -> {
            val lista = if (q.contains("resolvid") || q.contains("concluid")) cobrancas.filter { it.status.equals("RESOLVIDA", true) || it.status.equals("CONCLUIDA", true) } else cobrancasPendentes
            return if (lista.isEmpty()) "Não há cobranças ${if (lista === cobrancasPendentes) "pendentes" else "resolvidas"} cadastradas para esse filtro."
            else "COBRANÇAS — ${lista.size} registro(s)\n" + lista.sortedBy { it.proximaCobranca ?: Long.MAX_VALUE }.take(20).joinToString("\n\n") {
                "${it.destino} • Referência: ${it.referencia}\nContato: ${it.pessoaContatada}\nStatus: ${it.status}\nPróxima cobrança: ${it.proximaCobranca?.let { v -> dataCurta.format(Date(v)) } ?: "não definida"}\nÚltimo contato: ${data.format(Date(it.dataContato))}\nResposta: ${it.resposta.ifBlank { "sem retorno registrado" }}\nObservações: ${it.observacoes.ifBlank { "nenhuma" }}"
            }
        }
        listOf("atras", "vencid", "prazo", "urgente", "prioridade").any { q.contains(it) } -> {
            return if (atrasadas.isEmpty()) "Não encontrei demandas atrasadas. Existem ${abertas.size} demanda(s) em aberto."
            else "DEMANDAS ATRASADAS — ${atrasadas.size}\n" + atrasadas.sortedBy { it.dataPrazoFinal }.take(20).joinToString("\n\n") {
                "• ${it.titulo}\nPrazo: ${dataCurta.format(Date(it.dataPrazoFinal))}\nPrioridade: ${it.prioridade.label}\nStatus: ${it.status}\nResponsável externo: ${it.responsavelExterno ?: "não informado"}\nDescrição: ${it.descricao.ifBlank { "sem descrição" }}"
            }
        }
        listOf("cd", "centro de distribu").any { q.contains(it) } ->
            return listarDemandasPorStatus("aguardando CD", abertas.filter { it.status == StatusDemanda.AGUARDANDO_CD }, dataCurta)
        listOf("fornecedor", "troca").any { q.contains(it) } ->
            return listarDemandasPorStatus("aguardando fornecedor", abertas.filter { it.status == StatusDemanda.AGUARDANDO_FORNECEDOR }, dataCurta)
        q.contains("terceir") ->
            return listarDemandasPorStatus("aguardando terceiros", abertas.filter { it.status == StatusDemanda.AGUARDANDO_TERCEIROS }, dataCurta)
        listOf("demanda", "demandas", "pendencia", "pendencias", "tarefa", "tarefas").any { q.contains(it) } ->
            return if (abertas.isEmpty()) "Não há demandas abertas."
            else "DEMANDAS EM ABERTO — ${abertas.size}\n" + abertas.sortedBy { it.dataPrazoFinal }.take(20).joinToString("\n\n") {
                "• ${it.titulo}\nCategoria: ${it.categoria.label} • Prioridade: ${it.prioridade.label}\nStatus: ${it.status}\nPrazo: ${if (it.dataPrazoFinal == Long.MAX_VALUE) "indeterminado" else dataCurta.format(Date(it.dataPrazoFinal))}\nResponsabilidade: ${it.responsabilidade}\nDescrição: ${it.descricao.ifBlank { "sem descrição" }}"
            }
        listOf("resumo", "situacao", "geral", "painel", "operacao", "operacional").any { q.contains(it) } ->
            return "RESUMO GERAL DA OPERAÇÃO\n\n• Remanejos cadastrados: ${remanejos.size}\n• Remanejos sem assinatura: ${semAssinatura.size}\n• Demandas em aberto: ${abertas.size}\n• Demandas atrasadas: ${atrasadas.size}\n• Ocorrências registradas: ${ocorrencias.size}\n• Ocorrências pendentes: ${ocorrenciasPendentes.size}\n• Cobranças cadastradas: ${cobrancas.size}\n• Cobranças pendentes: ${cobrancasPendentes.size}\n\nAtenção: os números representam somente os registros existentes neste aparelho."
    }
    return "Ainda não identifiquei exatamente o que você quer consultar. Posso ajudar com:\n\n• Remanejos: "Quais remanejos foram feitos?", "Quais estão sem assinatura?" ou "Consulte a NF 12345".\n• Demandas: "O que está atrasado?", "O que aguarda o CD?", "Mostre as demandas por prioridade".\n• Ocorrências: "Quais avarias estão pendentes?", "Consulte ocorrências do EAN 789...".\n• Cobranças: "Quais fornecedores precisam de retorno?", "Qual a próxima cobrança?".\n• Geral: "Faça um resumo da operação".\n\nDica: informe o número da NF ou o EAN na pergunta para localizar um registro específico."
}

private fun listarDemandasPorStatus(nome: String, lista: List<DemandaEntity>, formato: SimpleDateFormat): String {
    if (lista.isEmpty()) return "Não há demandas abertas marcadas como $nome."
    return "${lista.size} demanda(s) $nome:\n" + lista.sortedBy { it.dataPrazoFinal }.take(20).joinToString("\n\n") {
        "• ${it.titulo}\nPrazo: ${if (it.dataPrazoFinal == Long.MAX_VALUE) "indeterminado" else formato.format(Date(it.dataPrazoFinal))}\nPrioridade: ${it.prioridade.label}\nDescrição: ${it.descricao.ifBlank { "sem descrição" }}"
    }
}

private fun normalizarAssistente(texto: String): String {
    val base = texto.lowercase(Locale("pt", "BR")).replace(Regex("[áàãâä]"), "a")
        .replace(Regex("[éèêë]"), "e").replace(Regex("[íìîï]"), "i")
        .replace(Regex("[óòõôö]"), "o").replace(Regex("[úùûü]"), "u")
        .replace("ç", "c")
    return base.replace(Regex("\\s+"), " ").trim()
}
