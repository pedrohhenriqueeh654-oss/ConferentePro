package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val VisualNavy = Color(0xFF123B46)
private val VisualTeal = Color(0xFF087E83)
private val VisualMint = Color(0xFF39B8A5)

@Composable
fun CabecalhoTela(
    titulo: String,
    descricao: String,
    etiqueta: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VisualNavy),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(descricao, color = Color.White.copy(alpha = .76f), fontSize = 11.sp, lineHeight = 15.sp)
            }
            if (!etiqueta.isNullOrBlank()) {
                Surface(color = VisualTeal, shape = RoundedCornerShape(30.dp)) {
                    Text(etiqueta.uppercase(), color = Color.White, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
fun TituloSecaoVisual(titulo: String, detalhe: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo.uppercase(), color = VisualNavy, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            if (!detalhe.isNullOrBlank()) Text(detalhe, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
fun IndicadorVisual(titulo: String, valor: String, detalhe: String? = null, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(valor, color = VisualTeal, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(titulo, color = VisualNavy, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            if (!detalhe.isNullOrBlank()) Text(detalhe, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
        }
    }
}

@Composable
fun EstadoVazioVisual(titulo: String, descricao: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(titulo, color = VisualNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(descricao, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
