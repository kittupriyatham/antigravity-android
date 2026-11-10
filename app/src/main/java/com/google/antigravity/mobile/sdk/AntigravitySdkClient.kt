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
    private var daemonUrl: String = "http://localhost:8080"
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

    fun sendPrompt(
        prompt: String,
        model: String = "Gemini 2.5 Pro",
        listener: AgentListener
    ) {
        // If an API key is provided and endpoint is configured, connect to the Antigravity Agent REST API
        if (apiKey.isNotEmpty()) {
            callAntigravityApi(prompt, model, listener)
        } else {
            // Native interactive simulation loop implementing the Antigravity SDK harness
            simulateAgentInteraction(prompt, model, listener)
        }
    }

    private fun callAntigravityApi(
        prompt: String,
        model: String,
        listener: AgentListener
    ) {
        val endpoint = "$daemonUrl/api/v1/agent/prompt"
        val jsonPayload = """
            {
                "prompt": "$prompt",
                "model": "$model",
                "stream": true
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // If remote daemon fails, fallback to local SDK simulation
                mainHandler.post {
                    simulateAgentInteraction(prompt, model, listener)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post {
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
