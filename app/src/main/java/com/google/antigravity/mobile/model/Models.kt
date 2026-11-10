package com.google.antigravity.mobile.model

enum class MessageRole {
    USER,
    AGENT,
    SYSTEM
}

enum class ToolApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED,
    EXECUTED
}

data class ToolCall(
    val id: String,
    val toolName: String,
    val command: String,
    var status: ToolApprovalStatus = ToolApprovalStatus.PENDING,
    var output: String? = null
)

data class DiffBlock(
    val filePath: String,
    val additions: Int,
    val deletions: Int,
    val diffText: String
)

data class ArtifactItem(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // e.g. "CODE", "DIFF", "HTML", "CONFIG"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AgentMessage(
    val id: String,
    val role: MessageRole,
    var content: String,
    var model: String = "Gemini 4 Argon",
    val thinkingSteps: MutableList<String> = mutableListOf(),
    var isThinkingExpanded: Boolean = false,
    var toolCall: ToolCall? = null,
    var diffBlock: DiffBlock? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class InstanceConfig(
    val id: String,
    val name: String,
    val type: String, // "Gemini Antigravity Agent", "agy Daemon (Local)", "Custom Host"
    val endpoint: String,
    val isConnected: Boolean = true
)
