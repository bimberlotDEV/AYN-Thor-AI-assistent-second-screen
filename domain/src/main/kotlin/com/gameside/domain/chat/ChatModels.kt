package com.gameside.domain.chat

import java.time.Instant

data class ChatSession(
    val id: String,
    val gameProfileId: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val createdAt: Instant,
)

enum class ChatRole { USER, ASSISTANT }

data class ChatThread(
    val session: ChatSession,
    val messages: List<ChatMessage>,
)
