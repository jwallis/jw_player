package com.joshuawallis.jwplayer

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshuawallis.jwplayer.data.SettingsRepository
import com.joshuawallis.jwplayer.playback.PlaybackViewModel
import com.joshuawallis.jwplayer.ui.navigation.AppNavHost
import com.joshuawallis.jwplayer.ui.theme.Mp3playerTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.BLACK),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.BLACK)
        )
        setContent {
            Mp3playerTheme {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1500)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen(modifier = Modifier.fillMaxSize())
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val playbackViewModel: PlaybackViewModel = viewModel()
                        val settingsRepository = remember { SettingsRepository(applicationContext) }
                        AppNavHost(
                            playbackViewModel = playbackViewModel,
                            settingsRepository = settingsRepository
                        )
                    }
                }
            }
        }
    }
}

private val rainbowColors = listOf(
    Color(0xFFFF0000),
    Color(0xFFFF7F00),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF0000FF),
    Color(0xFF8B00FF)
)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(LocalTime.now().format(timeFormatter)) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now().format(timeFormatter)
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "jw player",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    brush = Brush.linearGradient(colors = rainbowColors),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = currentTime,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { contentDescription = "Current time: $currentTime" }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    Mp3playerTheme {
        SplashScreen()
    }
}
