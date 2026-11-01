package com.conferentepro.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.conferentepro.app.data.local.AppDatabase

@Composable
fun RemanejosScreen(db: AppDatabase) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("REMANEJOS", style = MaterialTheme.typography.titleLarge, color = AzulMarinho)
        Spacer(Modifier.height(8.dp))
        Text(
            "A área de remanejos foi adicionada à navegação. O cadastro com NF, filiais, responsável, assinatura e histórico está em desenvolvimento.",
            color = Color.Gray
        )
    }
}