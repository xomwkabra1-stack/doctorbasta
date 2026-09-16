package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val sender: String, // "user" or "counselor"
    val timestamp: Long = System.currentTimeMillis(),
    val isCrisisWarning: Boolean = false
) {
    val isUser: Boolean get() = sender == "user"
    val isCounselor: Boolean get() = sender == "counselor"
}
