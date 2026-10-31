package com.conferentepro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Petrol = Color(0xFF123B46)
private val Teal = Color(0xFF087E83)
private val Turquoise = Color(0xFF39B8A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Teal,
                secondary = Turquoise,
                background = Color(0xFFF4F7F6),
                surface = Color.White
            )) {
                LaunchedEffect(Unit) {
                    delay(1100)
                    showSplash = false
                }
                if (showSplash) {
                    ConferenteSplash()
                } else {
                    com.conferentepro.app.ui.navigation.MainAppNavigation()
                }
            }
        }
    }
}

@Composable
private fun ConferenteSplash() {
    Box(
        modifier = Modifier.fillMaxSize().background(Petrol),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(id = R.drawable.ic_conferente_logo),
                contentDescription = "Logo Conferente Pro",
                modifier = Modifier.size(136.dp)
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text("CONFERENTE", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("PRO", color = Turquoise, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = 6.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Organização e controle operacional", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
        }
        Text(
            "Desenvolvido por @eanpdro e GPT",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 11.sp,
            letterSpacing = 0.3.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        )
    }
}
