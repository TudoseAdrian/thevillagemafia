package com.example.thevillage

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import java.util.UUID

data class Player(
    val id: String = "",
    val name: String = "",
    val role: String = "unassigned",
    val alive: Boolean = true,
    val votedFor: String? = null,
    val nightActionTarget: String? = null,
    val readyToVote: Boolean = false
)

data class GameState(
    val state: String = "lobby",
    val villageName: String = "",
    val dayNumber: Int = 1,
    val winner: String? = null,
    val recentDeath: String? = null,
    val deliberationEndTime: Long? = null,
    val players: Map<String, Player> = emptyMap()
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val narrator = NarratorManager(application)
    private val database = Firebase.database.reference

    private val _currentGameId = MutableStateFlow<String?>(null)
    val currentGameId: StateFlow<String?> = _currentGameId.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private var gameListener: ValueEventListener? = null

    fun createNewGame() {
        // Generate a simple 6-character alphanumeric ID
        val newId = UUID.randomUUID().toString().substring(0, 6).uppercase()
        
        val hostingUrl = "https://thevillagemafia.web.app/game/$newId"

        val initialGameState = GameState(
            state = "lobby",
            villageName = "Frowningtown",
            players = emptyMap()
        )

        database.child("games").child(newId).setValue(initialGameState)
            .addOnSuccessListener {
                _currentGameId.value = newId
                listenToGame(newId)
            }
    }

    fun startGame() {
        val currentState = _gameState.value ?: return
        val gameId = _currentGameId.value ?: return
        
        val players = currentState.players.values.toList()
        if (players.isEmpty()) return

        // Assign Roles
        val shuffledPlayers = players.shuffled()
        val updatedPlayers = mutableMapOf<String, Player>()
        
        shuffledPlayers.forEachIndexed { index, player ->
            val assignedRole = if (index == 0) "killer" else "peasant"
            updatedPlayers[player.id] = player.copy(role = assignedRole)
        }

        val newState = currentState.copy(
            state = "night",
            players = updatedPlayers
        )

        database.child("games").child(gameId).setValue(newState).addOnSuccessListener {
            // Trigger Narrator
            narrator.speak("In the village of Frowningtown, the people were all happy and cheerful... until they went to sleep, not knowing what the night ahead hides.")
            narrator.speak("Everybody goes to sleep.", flush = false)
            
            triggerNightPhase(gameId)
        }
    }

    private fun triggerNightPhase(gameId: String) {
        viewModelScope.launch {
            delay(10000) // 10 seconds of suspense
            
            val currentState = _gameState.value ?: return@launch
            val newState = currentState.copy(state = "night_killer_awake")
            
            database.child("games").child(gameId).setValue(newState).addOnSuccessListener {
                narrator.speak("During the night, the killer wakes up.", flush = true)
                narrator.speak("Killer, choose your victim.", flush = false)
            }
        }
    }

    private fun listenToGame(gameId: String) {
        // Remove old listener if exists
        gameListener?.let { database.child("games").child(currentGameId.value ?: "").removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(GameState::class.java) ?: return
                val oldState = _gameState.value
                _gameState.value = state
                
                checkGameLogic(state, gameId)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        
        database.child("games").child(gameId).addValueEventListener(listener)
        gameListener = listener
    }

    private fun checkGameLogic(state: GameState, gameId: String) {
        if (state.state == "night_killer_awake") {
            // Check if killer has acted
            val killers = state.players.values.filter { it.role == "killer" && it.alive }
            val allKillersActed = killers.isNotEmpty() && killers.all { it.nightActionTarget != null }
            
            if (allKillersActed) {
                // Resolve night
                // For simplicity with 1 killer, we just take the first target
                val targetId = killers.first().nightActionTarget
                
                val updatedPlayers = state.players.toMutableMap()
                var victimName = "nobody"
                
                if (targetId != null && updatedPlayers.containsKey(targetId)) {
                    val victim = updatedPlayers[targetId]!!
                    updatedPlayers[targetId] = victim.copy(alive = false)
                    victimName = victim.name
                }
                
                // Clear night action targets
                updatedPlayers.forEach { (id, player) ->
                    updatedPlayers[id] = player.copy(nightActionTarget = null)
                }
                
                val newState = state.copy(
                    state = "morning",
                    recentDeath = targetId,
                    deliberationEndTime = System.currentTimeMillis() + 5 * 60 * 1000,
                    players = updatedPlayers
                )
                
                database.child("games").child(gameId).setValue(newState).addOnSuccessListener {
                    narrator.speak("In the morning, all the people in the village woke up cheerful...", flush = true)
                    if (targetId != null) {
                        narrator.speak("Except $victimName, who was found with a knife in their back.", flush = false)
                    } else {
                        narrator.speak("And surprisingly, everyone was still alive!", flush = false)
                    }
                    narrator.speak("The people are angry and start deliberating who might the killer be.", flush = false)
                    narrator.speak("You have five minutes to deliberate.", flush = false)
                    
                    // We'll set a timer in coroutines in a real app, but for now we rely on the readyToVote logic 
                    // or a frontend trigger to end deliberation.
                }
            }
        } else if (state.state == "morning") {
            val alivePlayers = state.players.values.filter { it.alive }
            val allReady = alivePlayers.isNotEmpty() && alivePlayers.all { it.readyToVote }
            
            if (allReady) {
                // Clear ready flag and go to voting
                val updatedPlayers = state.players.toMutableMap()
                updatedPlayers.forEach { (id, player) ->
                    updatedPlayers[id] = player.copy(readyToVote = false)
                }
                
                val newState = state.copy(
                    state = "voting",
                    // 60 seconds to vote? No, we didn't add votingEndTime yet. Let's just set state to voting.
                    players = updatedPlayers
                )
                
                database.child("games").child(gameId).setValue(newState).addOnSuccessListener {
                    narrator.speak("The village has spoken. It is time to vote for who you think the killer is.", flush = true)
                    narrator.speak("You have one minute. Choose wisely.", flush = false)
                }
            }
        } else if (state.state == "voting") {
            val alivePlayers = state.players.values.filter { it.alive }
            val allVoted = alivePlayers.isNotEmpty() && alivePlayers.all { it.votedFor != null }

            if (allVoted) {
                val voteCounts = mutableMapOf<String, Int>()
                alivePlayers.forEach { player ->
                    val vote = player.votedFor
                    if (vote != null && vote != "skip") {
                        voteCounts[vote] = voteCounts.getOrDefault(vote, 0) + 1
                    }
                }

                var maxVotes = 0
                var eliminatedId: String? = null
                var tie = false

                voteCounts.forEach { (id, count) ->
                    if (count > maxVotes) {
                        maxVotes = count
                        eliminatedId = id
                        tie = false
                    } else if (count == maxVotes) {
                        tie = true
                    }
                }

                if (tie) eliminatedId = null

                val updatedPlayers = state.players.toMutableMap()
                var victimName = "nobody"

                if (eliminatedId != null && updatedPlayers.containsKey(eliminatedId)) {
                    val victim = updatedPlayers[eliminatedId]!!
                    updatedPlayers[eliminatedId] = victim.copy(alive = false)
                    victimName = victim.name
                }

                // Clear votes
                updatedPlayers.forEach { (id, player) ->
                    updatedPlayers[id] = player.copy(votedFor = null)
                }

                // Check win condition
                val remainingAlive = updatedPlayers.values.filter { it.alive }
                val remainingKillers = remainingAlive.count { it.role == "killer" }
                val remainingPeasants = remainingAlive.count { it.role == "peasant" }

                var winner: String? = null
                var nextState = "night"

                if (remainingKillers == 0) {
                    winner = "peasants"
                    nextState = "game_over"
                } else if (remainingKillers >= remainingPeasants) {
                    winner = "killers"
                    nextState = "game_over"
                }

                val newState = state.copy(
                    state = nextState,
                    winner = winner,
                    dayNumber = state.dayNumber + 1,
                    recentDeath = eliminatedId,
                    players = updatedPlayers
                )

                database.child("games").child(gameId).setValue(newState).addOnSuccessListener {
                    narrator.speak("The votes are in.", flush = true)
                    if (eliminatedId != null) {
                        narrator.speak("The village has decided to execute $victimName.", flush = false)
                    } else {
                        narrator.speak("The village could not agree on who to execute. Nobody dies today.", flush = false)
                    }

                    if (winner == "peasants") {
                        narrator.speak("With the killer dead, the village is finally safe. The peasants win!", flush = false)
                    } else if (winner == "killers") {
                        narrator.speak("The killers have outnumbered the peasants. The village falls to darkness. Killers win!", flush = false)
                    } else {
                        narrator.speak("As night falls again, everybody goes to sleep.", flush = false)
                        triggerNightPhase(gameId)
                    }
                }
            }
        }
    }

    fun generateQrCode(content: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameListener?.let { 
            currentGameId.value?.let { id ->
                database.child("games").child(id).removeEventListener(it)
            }
        }
    }
}
