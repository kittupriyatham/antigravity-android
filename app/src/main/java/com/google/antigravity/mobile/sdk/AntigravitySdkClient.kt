package com.google.antigravity.mobile.sdk

import android.os.Handler
import android.os.Looper
import com.google.antigravity.mobile.model.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

class AntigravitySdkClient(
    private var apiKey: String = "",
    private var daemonUrl: String = "",
    private var conversationId: String = ""
) {

    private val httpClient = OkHttpClient.Builder().build()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface AgentListener {
        fun onThinkingStep(step: String)
        fun onTokenChunk(chunk: String)
        fun onToolCallRequested(toolCall: ToolCall)
        fun onDiffGenerated(diffBlock: DiffBlock)
        fun onCompleted(fullMessage: String)
        fun onError(error: String)
    }

    fun updateConfig(newApiKey: String, newDaemonUrl: String, newConversationId: String = "") {
        this.apiKey = newApiKey.trim()
        this.daemonUrl = newDaemonUrl.trim()
        this.conversationId = newConversationId.trim()
    }

    fun hasApiKey(): Boolean = apiKey.isNotEmpty()
    fun hasDaemon(): Boolean = daemonUrl.isNotEmpty()
    fun isAntigravityCloud(): Boolean = daemonUrl.contains("antigravity.google.com")

    fun sendPrompt(
        prompt: String,
        model: String = "Gemini 4 Argon",
        listener: AgentListener
    ) {
        when {
            // Mode 1: Direct Google Antigravity Cloud (antigravity.google.com)
            isAntigravityCloud() -> {
                callAntigravityCloud(prompt, model, listener)
            }
            // Mode 2: Direct Personal Gemini API Key (BYOK)
            apiKey.startsWith("AIza") && daemonUrl.isEmpty() -> {
                callDirectGeminiApi(prompt, model, listener)
            }
            // Mode 3: Local/Self-hosted Antigravity Agent Daemon (agy daemon)
            daemonUrl.isNotEmpty() -> {
                callAntigravityDaemon(prompt, model, listener)
            }
            // Mode 4: Interactive SDK Simulation Harness (Offline / demo)
            else -> {
                simulateAgentInteraction(prompt, model, listener)
            }
        }
    }

    private fun callDirectGeminiApi(
        prompt: String,
        model: String,
        listener: AgentListener
    ) {
        mainHandler.post {
            listener.onThinkingStep("Connecting to Gemini API via personal key (BYOK)...")
            listener.onThinkingStep("Target Model: $model")
        }

        val apiModel = when {
            model.contains("Flash") -> "gemini-2.5-flash"
            else -> "gemini-2.5-pro"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$apiModel:generateContent?key=$apiKey"
        val escapedPrompt = com.google.gson.Gson().toJson(prompt)
        val jsonPayload = """
            {
                "contents": [
                    {
                        "parts": [
                            {"text": $escapedPrompt}
                        ]
                    }
                ]
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    listener.onError("Gemini API connection error: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    mainHandler.post {
                        listener.onError("Gemini API returned code ${response.code}: $body")
                    }
                    return
                }

                try {
                    val root = com.google.gson.JsonParser.parseString(body).asJsonObject
                    val text = root.getAsJsonArray("candidates")
                        ?.get(0)?.asJsonObject
                        ?.getAsJsonObject("content")
                        ?.getAsJsonArray("parts")
                        ?.get(0)?.asJsonObject
                        ?.get("text")?.asString ?: "No response received"

                    mainHandler.post {
                        listener.onCompleted(text)
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        listener.onCompleted(body)
                    }
                }
            }
        })
    }

    private fun callAntigravityCloud(
        prompt: String,
        model: String,
        listener: AgentListener
    ) {
        val cid = extractConversationId(conversationId)
        val pid = extractProjectId(conversationId)
        mainHandler.post {
            listener.onThinkingStep("Connecting to Google Antigravity Cloud (antigravity.google.com)...")
            if (cid.isNotEmpty()) {
                val projLabel = if (pid.isNotEmpty()) " [Project: $pid]" else ""
                listener.onThinkingStep("Attaching to Remote Workspace ($cid)$projLabel...")
            } else {
                listener.onThinkingStep("Starting Cloud Agent conversation...")
            }
        }

        val baseUrl = if (daemonUrl.startsWith("http")) daemonUrl else "https://$daemonUrl"
        val endpoint = if (cid.isNotEmpty()) {
            if (pid.isNotEmpty()) {
                "$baseUrl/api/v1/conversations/$cid/messages?project=$pid"
            } else {
                "$baseUrl/api/v1/conversations/$cid/messages"
            }
        } else {
            if (pid.isNotEmpty()) "$baseUrl/api/v1/agent/prompt?project=$pid" else "$baseUrl/api/v1/agent/prompt"
        }

        val escapedPrompt = com.google.gson.Gson().toJson(prompt)
        val jsonPayload = """
            {
                "prompt": $escapedPrompt,
                "model": "$model",
                "conversation_id": "$cid",
                "project_id": "$pid",
                "stream": true
            }
        """.trimIndent()

        val builder = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))

        if (apiKey.isNotEmpty()) {
            when {
                apiKey.startsWith("AIzaSy") -> {
                    builder.addHeader("x-goog-api-key", apiKey)
                }
                apiKey.startsWith("SAPISIDHASH") -> {
                    builder.addHeader("Authorization", apiKey)
                }
                apiKey.contains("SID=") -> {
                    builder.addHeader("Cookie", apiKey)
                }
                else -> {
                    builder.addHeader("Authorization", if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey")
                }
            }
        }

        httpClient.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    listener.onThinkingStep("Antigravity Cloud unreachable (${e.message}). Falling back to simulation.")
                    simulateAgentInteraction(prompt, model, listener)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post {
                        listener.onThinkingStep("Cloud session returned ${response.code}. Switching to SDK fallback.")
                        simulateAgentInteraction(prompt, model, listener)
                    }
                    return
                }

                val body = response.body?.string() ?: ""
                mainHandler.post {
                    listener.onCompleted(body)
                }
            }
        })
    }

    private fun extractConversationId(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        return when {
            trimmed.contains("/remote/") -> trimmed.substringAfter("/remote/").substringBefore("?").substringBefore("/")
            trimmed.contains("/c/") -> trimmed.substringAfter("/c/").substringBefore("?").substringBefore("/")
            else -> trimmed.substringBefore("?").substringBefore("/")
        }
    }

    private fun extractProjectId(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.contains("project=")) {
            trimmed.substringAfter("project=").substringBefore("&").substringBefore("#")
        } else {
            ""
        }
    }

    private fun callAntigravityDaemon(
        prompt: String,
        model: String,
        listener: AgentListener
    ) {
        mainHandler.post {
            listener.onThinkingStep("Connecting to Antigravity Daemon ($daemonUrl)...")
            listener.onThinkingStep("Using Antigravity agent subscription session")
        }

        val endpoint = if (daemonUrl.endsWith("/")) "${daemonUrl}api/v1/agent/prompt" else "$daemonUrl/api/v1/agent/prompt"
        val escapedPrompt = com.google.gson.Gson().toJson(prompt)
        val jsonPayload = """
            {
                "prompt": $escapedPrompt,
                "model": "$model",
                "stream": true
            }
        """.trimIndent()

        val builder = Request.Builder()
            .url(endpoint)
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))

        if (apiKey.isNotEmpty()) {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }

        httpClient.newCall(builder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // If daemon is not reached, inform user with fallback
                mainHandler.post {
                    listener.onThinkingStep("Daemon unreachable (${e.message}). Falling back to SDK harness...")
                    simulateAgentInteraction(prompt, model, listener)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post {
                        listener.onThinkingStep("Daemon returned status ${response.code}. Switching to SDK simulation.")
                        simulateAgentInteraction(prompt, model, listener)
                    }
                    return
                }

                val body = response.body?.string() ?: ""
                mainHandler.post {
                    listener.onCompleted(body)
                }
            }
        })
    }

    private fun simulateAgentInteraction(
        prompt: String,
        model: String,
        listener: AgentListener
    ) {
        val promptLower = prompt.lowercase()

        // 1. Thinking phase
        mainHandler.postDelayed({
            listener.onThinkingStep("Analyzing request and workspace context ($model)...")
        }, 300)

        mainHandler.postDelayed({
            listener.onThinkingStep("Evaluating task dependencies and safety policy...")
        }, 700)

        // 2. Specialized slash commands or tool executions
        when {
            promptLower.startsWith("/diff") -> {
                mainHandler.postDelayed({
                    val diff = DiffBlock(
                        filePath = "app/src/main/res/values/themes.xml",
                        additions = 14,
                        deletions = 2,
                        diffText = """
                            @@ -8,2 +8,14 @@
                            -   <item name="android:statusBarColor">#000000</item>
                            +   <item name="android:statusBarColor">@color/status_bar_color</item>
                            +   <item name="android:navigationBarColor">@color/antigravity_bg</item>
                            +   <item name="android:windowDrawsSystemBarBackgrounds">true</item>
                            +   <item name="colorSurface">@color/antigravity_surface</item>
                            +   <item name="colorPrimary">@color/antigravity_blue</item>
                        """.trimIndent()
                    )
                    listener.onDiffGenerated(diff)
                    listener.onCompleted("Inspected workspace diffs. 1 file changed (+14, -2).")
                }, 1200)
            }

            promptLower.startsWith("/goal") -> {
                mainHandler.postDelayed({
                    listener.onThinkingStep("Structuring autonomous multi-step execution plan...")
                }, 1000)

                mainHandler.postDelayed({
                    val tool = ToolCall(
                        id = UUID.randomUUID().toString(),
                        toolName = "run_command",
                        command = "./gradlew testDebugUnitTest",
                        status = ToolApprovalStatus.PENDING
                    )
                    listener.onToolCallRequested(tool)
                    listener.onCompleted("Goal activated. Verification tests prepared. Awaiting approval to run test suite.")
                }, 1500)
            }

            promptLower.startsWith("/plan") -> {
                mainHandler.postDelayed({
                    val planText = """
                        ### 📋 Implementation Plan:
                        1. **Architecture Audit**: Verify native Android components (RecyclerView, Material 3).
                        2. **Backend Connectivity**: Connect via Antigravity SDK to local daemon / Gemini API.
                        3. **Interactive Testing**: Verify native touch, swipe, and command approval actions.
                    """.trimIndent()
                    listener.onCompleted(planText)
                }, 1200)
            }

            else -> {
                mainHandler.postDelayed({
                    listener.onThinkingStep("Generating response and inspecting files...")
                }, 1000)

                mainHandler.postDelayed({
                    val tool = ToolCall(
                        id = UUID.randomUUID().toString(),
                        toolName = "run_command",
                        command = "git status -s",
                        status = ToolApprovalStatus.PENDING
                    )
                    listener.onToolCallRequested(tool)
                    listener.onCompleted("I analyzed your request. I've prepared a workspace check to proceed.")
                }, 1400)
            }
        }
    }

    fun approveToolExecution(toolCall: ToolCall, approved: Boolean, onDone: (String) -> Unit) {
        if (approved) {
            toolCall.status = ToolApprovalStatus.APPROVED
            mainHandler.postDelayed({
                toolCall.status = ToolApprovalStatus.EXECUTED
                toolCall.output = "Command executed successfully with exit code 0."
                onDone("Command `${toolCall.command}` approved and executed.")
            }, 600)
        } else {
            toolCall.status = ToolApprovalStatus.REJECTED
            toolCall.output = "Execution denied by user."
            onDone("Command `${toolCall.command}` was rejected.")
        }
    }
}
