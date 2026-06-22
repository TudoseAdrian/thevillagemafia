package com.example.thevillage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thevillage.ui.theme.TheVillageTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheVillageTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF111827) // Dark gray/blue background
                ) {
                    val gameId by viewModel.currentGameId.collectAsState()
                    val gameState by viewModel.gameState.collectAsState()

                    var currentScreen by remember { mutableStateOf("home") }

                    LaunchedEffect(gameId) {
                        if (gameId != null && currentScreen == "home") {
                            currentScreen = "qr"
                        }
                    }

                    when (currentScreen) {
                        "home" -> HomeScreen(
                            onCreateGame = { viewModel.createNewGame() }
                        )
                        "qr" -> QrScreen(
                            gameId = gameId ?: "",
                            gameState = gameState,
                            viewModel = viewModel,
                            onNext = { currentScreen = "roles" }
                        )
                        "roles" -> RoleSelectionScreen(
                            gameState = gameState,
                            onConfirm = { 
                                viewModel.startGame()
                                currentScreen = "game" 
                            }
                        )
                        "game" -> OngoingGameScreen(
                            gameState = gameState
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onCreateGame: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "The Village",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 64.dp)
        )
        
        Button(
            onClick = onCreateGame,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("NEW GAME", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QrScreen(gameId: String, gameState: GameState?, viewModel: GameViewModel, onNext: () -> Unit) {
    // Generate QR Code bitmap
    val joinUrl = "https://thevillagemafia.web.app/game/$gameId" 
    val qrBitmap = remember(joinUrl) { viewModel.generateQrCode(joinUrl) }
    
    val playerCount = gameState?.players?.size ?: 0

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text("Scan to Join", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Session ID: $gameId", fontSize = 20.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
        
        Box(modifier = Modifier.size(250.dp).background(Color.White).padding(16.dp)) {
            qrBitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.fillMaxSize())
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text("Players joined: $playerCount", fontSize = 24.sp, color = Color.White, modifier = Modifier.padding(bottom = 24.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            enabled = playerCount > 0
        ) {
            Text("SELECT ROLES", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        if (playerCount == 0) {
            Text("Waiting for players...", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun RoleSelectionScreen(gameState: GameState?, onConfirm: () -> Unit) {
    val playerCount = gameState?.players?.size ?: 0
    // Simple placeholder for now. Later we will add + / - buttons for Peasants and Killers
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Roles", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Total Players: $playerCount", fontSize = 20.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 64.dp))
        
        // TODO: Interactive role selectors
        Text("1x Killer", color = Color.White, fontSize = 24.sp)
        Text("${maxOf(0, playerCount - 1)}x Peasant", color = Color.White, fontSize = 24.sp)
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
        ) {
            Text("CONFIRM ROLES & START", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OngoingGameScreen(gameState: GameState?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text("Village of Frowningtown", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Text("Phase: ${gameState?.state?.uppercase()}", fontSize = 20.sp, color = Color.Red, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
        
        // TODO: Display players and Narrator Controls
        Text("Narrator Controls will go here...", color = Color.Gray)
    }
}