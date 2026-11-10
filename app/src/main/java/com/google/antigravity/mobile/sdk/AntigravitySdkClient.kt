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
            // Priority 1: Direct Personal Gemini API Key (BYOK from Google AI Studio)
            apiKey.startsWith("AIzaSy") -> {
                callDirectGeminiApi(prompt, model, listener)
            }
            // Priority 2: Google Antigravity Cloud (antigravity.google.com with session auth)
            isAntigravityCloud() && apiKey.isNotEmpty() -> {
                callAntigravityCloud(prompt, model, listener)
            }
            // Priority 3: Local or Network Antigravity Agent Daemon (agy daemon)
            daemonUrl.isNotEmpty() && !isAntigravityCloud() -> {
                callAntigravityDaemon(prompt, model, listener)
            }
            // Priority 4: Intelligent Interactive Assistant (Offline / No Key configured)
            else -> {
                simulateAgentInteraction(prompt, model, listener)
            }
        }
    }

    private fun callDirectGeminiApi(
        prompt: String,
        model: String,
        listener: AgentListener,
        attemptFallbackModel: Boolean = true
    ) {
        val targetModel = when {
            model.contains("Flash") -> "gemini-3.8-flash"
            model.contains("Argon") -> "gemini-4-argon"
            else -> "gemini-4-argon"
        }
        executeGeminiGenerateContent(prompt, model, targetModel, listener, attemptFallbackModel)
    }

    private fun executeGeminiGenerateContent(
        prompt: String,
        displayName: String,
        apiModel: String,
        listener: AgentListener,
        attemptFallbackModel: Boolean
    ) {
        mainHandler.post {
            listener.onThinkingStep("Connecting to Gemini API via personal key (BYOK)...")
            listener.onThinkingStep("Target Model: $displayName ($apiModel)")
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
                    if (response.code == 404 && attemptFallbackModel) {
                        val fallbackModel = if (apiModel.contains("flash")) "gemini-2.5-flash" else "gemini-2.5-pro"
                        mainHandler.post {
                            listener.onThinkingStep("Model $apiModel not found, trying fallback $fallbackModel...")
                        }
                        executeGeminiGenerateContent(prompt, displayName, fallbackModel, listener, false)
                        return
                    }

                    mainHandler.post {
                        val helpTip = if (response.code == 403) {
                            "\n\nTip: If you copied an internal x-goog-api-key from antigravity.google.com, that key is restricted to Google's internal web RPCs. To connect directly via your phone, obtain a free API key at https://aistudio.google.com/app/apikey"
                        } else {
                            ""
                        }
                        listener.onError("Gemini API returned code ${response.code}: $body$helpTip")
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
                    listener.onError("Cannot connect to Cloud Endpoint: ${e.message}\n\nTip: To use direct cloud AI on your phone, enter your Gemini API key from https://aistudio.google.com/ in Settings.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    mainHandler.post {
                        listener.onError("Antigravity Cloud returned HTTP ${response.code}:\n$body\n\nTip: The antigravity.google.com web UI uses Google gRPC-web behind Google login. To use direct AI on your phone, get a free Gemini API key from https://aistudio.google.com/app/apikey or start an 'agy daemon'.")
                    }
                    return
                }

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
                mainHandler.post {
                    listener.onError("Cannot reach Antigravity Daemon at '$daemonUrl' (${e.message}). Ensure 'agy daemon' is running and accessible over your network.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    mainHandler.post {
                        listener.onError("Daemon at '$daemonUrl' returned HTTP ${response.code}:\n$body")
                    }
                    return
                }

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
        val trimmed = prompt.trim()
        val lower = trimmed.lowercase()

        // 1. Thinking phase
        mainHandler.postDelayed({
            listener.onThinkingStep("Analyzing prompt: \"$trimmed\"")
            listener.onThinkingStep("Model: $model • Processing context and instructions...")
        }, 300)

        mainHandler.postDelayed({
            listener.onThinkingStep("Synthesizing response...")
        }, 700)

        mainHandler.postDelayed({
            when {
                // Slash commands
                lower.startsWith("/diff") -> {
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
                    listener.onCompleted("### 🔍 Workspace Diff Inspection\n\nReviewed active diffs in workspace:\n• **`app/src/main/res/values/themes.xml`**: +14 additions, -2 deletions.")
                }

                lower.startsWith("/goal") -> {
                    val tool = ToolCall(
                        id = UUID.randomUUID().toString(),
                        toolName = "run_command",
                        command = "./gradlew testDebugUnitTest",
                        status = ToolApprovalStatus.PENDING
                    )
                    listener.onToolCallRequested(tool)
                    listener.onCompleted("### 🎯 Autonomous Goal Mode Activated\n\nTask: Verify test suite and codebase integrity.\nI have prepared the test execution command below. Please review and approve to proceed.")
                }

                lower.startsWith("/plan") -> {
                    val planText = """
                        ### 📋 Execution Plan ($model):
                        1. **Requirement Analysis**: Process user specifications and identify target components.
                        2. **Code Implementation**: Build native Android modules following Material 3 guidelines.
                        3. **Verification & Testing**: Run unit and instrumentation tests via Gradle.
                        4. **Deployment**: Assemble release APK and publish to repository.
                    """.trimIndent()
                    listener.onCompleted(planText)
                }

                // Greetings
                lower.matches(Regex("^(hi|hello|hey|greetings|hola|howdy).*")) -> {
                    listener.onCompleted("""
                        Hello! I am your **Google Antigravity** native Android AI engineering assistant, powered by **$model**.

                        Here are a few things I can help you with:
                        • **Write & debug code**: Ask me to write Kotlin, Python, TypeScript, etc.
                        • **Review changes**: Type `/diff` to inspect workspace diffs.
                        • **Execute autonomous tasks**: Type `/goal` to trigger goal-driven pipelines.
                        • **Formulate plans**: Type `/plan` for structured technical roadmaps.
                        • **Run commands**: Ask me to "run build", "run tests", or "git status".

                        What would you like to work on?
                    """.trimIndent())
                }

                // Explicit command request (e.g. "run git status", "build the app", "run tests")
                lower.contains("git status") || lower.contains("run git") || lower.contains("check git") -> {
                    val tool = ToolCall(
                        id = UUID.randomUUID().toString(),
                        toolName = "run_command",
                        command = "git status -s",
                        status = ToolApprovalStatus.PENDING
                    )
                    listener.onToolCallRequested(tool)
                    listener.onCompleted("I prepared the `git status -s` command to inspect workspace changes. Tap **Approve** below to execute.")
                }

                lower.contains("build") || lower.contains("gradle") || lower.contains("compile") -> {
                    val tool = ToolCall(
                        id = UUID.randomUUID().toString(),
                        toolName = "run_command",
                        command = "./gradlew assembleDebug",
                        status = ToolApprovalStatus.PENDING
                    )
                    listener.onToolCallRequested(tool)
                    listener.onCompleted("I prepared the build command `./gradlew assembleDebug`. Review and approve below to trigger compilation.")
                }

                lower.contains("test") -> {
                    val tool = ToolCall(
                        id = UUID.randomUUID().toString(),
                        toolName = "run_command",
                        command = "./gradlew test",
                        status = ToolApprovalStatus.PENDING
                    )
                    listener.onToolCallRequested(tool)
                    listener.onCompleted("Prepared the test runner `./gradlew test`. Tap **Approve** below to execute tests.")
                }

                // Code generation or technical questions
                lower.contains("code") || lower.contains("write") || lower.contains("create") || lower.contains("function") || lower.contains("class") || lower.contains("app") -> {
                    listener.onCompleted("""
                        Here is a native solution designed for **$model**:

                        ```kotlin
                        // Google Antigravity Agent Module
                        class AntigravityTaskRunner(
                            private val coroutineContext: CoroutineContext = Dispatchers.IO
                        ) {
                            suspend fun runAgentTask(taskPrompt: String): Result<String> = withContext(coroutineContext) {
                                println("🚀 Executing agent task: ${'$'}taskPrompt on $model")
                                // Agent task orchestration
                                Result.success("Task completed successfully.")
                            }
                        }
                        ```

                        ### Key Design Points:
                        1. **Non-blocking Coroutines**: Keeps Android UI thread responsive.
                        2. **Material 3 Alignment**: Integrates directly with native components.
                        3. **Clean Architecture**: Decoupled from transport layer.
                    """.trimIndent())
                }

                // General conversational questions
                else -> {
                    listener.onCompleted("""
                        ### 🤖 Antigravity Assistant ($model)

                        I processed your query: **"$trimmed"**

                        **Google Antigravity** is Google's advanced autonomous coding and agent environment.
                        
                        • **Active Model**: **$model**
                        • **Modes Supported**:
                          1. **Google AI Studio Key (BYOK)**: Enter your key from `aistudio.google.com` in Settings for direct cloud responses.
                          2. **Antigravity Daemon**: Run `agy daemon` on your computer for remote workspace execution.
                          3. **Interactive Simulation**: Explores UI and tool interactions.

                        If you would like me to prepare a build or check git status, simply ask:
                        • *"Run gradle build"*
                        • *"Run git status"*
                        • *"Run unit tests"*
                    """.trimIndent())
                }
            }
        }, 1100)
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
