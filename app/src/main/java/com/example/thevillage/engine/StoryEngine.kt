package com.example.thevillage.engine

import com.example.thevillage.GameState

interface StoryEngine {
    suspend fun generateStoryAndAudio(
        prompt: String,
        gameState: GameState,
        onSuccess: (audioFilePath: String) -> Unit,
        onError: (Exception) -> Unit
    )
}
