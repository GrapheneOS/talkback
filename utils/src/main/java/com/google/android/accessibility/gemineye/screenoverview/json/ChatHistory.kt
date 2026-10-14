package com.google.android.accessibility.gemineye.screenoverview.json

/** The original sender of a message in a chat history item */
enum class MessageType {
  USER,
  MODEL,
}

/** A single message in a chat history with an AI model */
data class ChatHistoryItem(val message: String, val messageType: MessageType)
