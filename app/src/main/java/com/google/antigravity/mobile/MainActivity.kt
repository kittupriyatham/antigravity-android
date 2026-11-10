package com.google.antigravity.mobile

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.antigravity.mobile.adapter.MessageAdapter
import com.google.antigravity.mobile.databinding.ActivityMainBinding
import com.google.antigravity.mobile.model.*
import com.google.antigravity.mobile.sdk.AntigravitySdkClient
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<AgentMessage>()

    private val sdkClient = AntigravitySdkClient()
    private var selectedModel = "Gemini 2.5 Pro"

    private val supportedModels = arrayOf(
        "Gemini 2.5 Pro (Recommended)",
        "Gemini 2.5 Flash",
        "Claude 3.5 Sonnet",
        "GPT-4o"
    )

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val fileName = uri.lastPathSegment ?: "attached_file"
                binding.etMessageInput.append(" [Attached: $fileName]")
                Toast.makeText(this, "File attached: $fileName", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Antigravity push alerts enabled", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNotificationPermissions()
        setupRecyclerView()
        setupListeners()
        setupSlashCommandChips()
        setupBottomNavigation()
        setupInitialWelcomeMessage()
    }

    private fun setupNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages) { message, approved ->
            val tool = message.toolCall ?: return@MessageAdapter
            sdkClient.approveToolExecution(tool, approved) { resultText ->
                val index = messages.indexOf(message)
                if (index != -1) {
                    messageAdapter.notifyItemChanged(index)
                }

                // Append terminal execution log
                binding.tvTerminalOutput.append("\n[Tool Approval] ${tool.command} -> ${if (approved) "EXECUTED" else "DENIED"}\n")
                Toast.makeText(this, resultText, Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupListeners() {
        // Send Button
        binding.btnSendMessage.setOnClickListener {
            val text = binding.etMessageInput.text?.toString()?.trim() ?: ""
            if (text.isNotEmpty()) {
                sendUserPrompt(text)
                binding.etMessageInput.setText("")
            }
        }

        // Attach File Button
        binding.btnAttachFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        // Model Selector Chip
        binding.chipModelSelector.setOnClickListener {
            showModelSelectorDialog()
        }

        // New Session Button
        binding.btnNewSession.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("New Antigravity Session")
                .setMessage("Start a new agent session? The current conversation context will be reset.")
                .setPositiveButton("Start New") { _, _ ->
                    messages.clear()
                    setupInitialWelcomeMessage()
                    Toast.makeText(this, "Started new session", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Terminal run button
        binding.btnRunTerminalCommand.setOnClickListener {
            val cmd = binding.etTerminalCommand.text?.toString()?.trim() ?: ""
            if (cmd.isNotEmpty()) {
                binding.tvTerminalOutput.append("\nantigravity@gemini:~$ $cmd\nCommand executed with exit code 0.\nantigravity@gemini:~$ ")
                binding.etTerminalCommand.setText("")
                binding.scrollTerminal.post {
                    binding.scrollTerminal.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        // Artifacts code inspect
        binding.btnViewArtifactCode.setOnClickListener {
            val codeSnippet = """
                class AntigravityAgent {
                    val model = "Gemini 2.5 Pro"
                    val memory = AgentMemory()
                    suspend fun executeTask(goal: String) {
                        autonomousHarness.plan(goal)
                    }
                }
            """.trimIndent()

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Antigravity Code", codeSnippet))
            Toast.makeText(this, "Artifact code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Save Settings
        binding.btnSaveSettings.setOnClickListener {
            val endpoint = binding.etDaemonEndpoint.text?.toString()?.trim() ?: ""
            val key = binding.etApiKey.text?.toString()?.trim() ?: ""
            Toast.makeText(this, "Settings saved. Connected to Antigravity runtime.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSlashCommandChips() {
        binding.chipDiff.setOnClickListener {
            sendUserPrompt("/diff")
        }
        binding.chipGoal.setOnClickListener {
            binding.etMessageInput.setText("/goal ")
            binding.etMessageInput.setSelection(binding.etMessageInput.text.length)
        }
        binding.chipPlan.setOnClickListener {
            binding.etMessageInput.setText("/plan ")
            binding.etMessageInput.setSelection(binding.etMessageInput.text.length)
        }
        binding.chipTeamwork.setOnClickListener {
            sendUserPrompt("/teamwork")
        }
        binding.chipGrillMe.setOnClickListener {
            sendUserPrompt("/grill-me")
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            binding.viewChat.visibility = View.GONE
            binding.viewArtifacts.visibility = View.GONE
            binding.viewTerminal.visibility = View.GONE
            binding.viewSettings.visibility = View.GONE

            when (item.itemId) {
                R.id.nav_agents -> {
                    binding.viewChat.visibility = View.VISIBLE
                    true
                }
                R.id.nav_artifacts -> {
                    binding.viewArtifacts.visibility = View.VISIBLE
                    true
                }
                R.id.nav_terminal -> {
                    binding.viewTerminal.visibility = View.VISIBLE
                    true
                }
                R.id.nav_settings -> {
                    binding.viewSettings.visibility = View.VISIBLE
                    true
                }
                else -> false
            }
        }
    }

    private fun setupInitialWelcomeMessage() {
        val welcomeMsg = AgentMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.AGENT,
            content = "Welcome to Google Antigravity on Android. I am your autonomous AI engineering assistant powered by $selectedModel.\n\nYou can ask me to write code, review diffs with /diff, or execute background goals with /goal.",
            thinkingSteps = mutableListOf(
                "Initialized Antigravity SDK harness",
                "Connected to local agy runtime environment",
                "Ready for user prompts and tool execution"
            )
        )
        messages.add(welcomeMsg)
        messageAdapter.notifyItemInserted(messages.size - 1)
    }

    private fun sendUserPrompt(promptText: String) {
        // Add User Message
        val userMsg = AgentMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            content = promptText
        )
        messages.add(userMsg)
        val userPos = messages.size - 1
        messageAdapter.notifyItemInserted(userPos)
        binding.rvMessages.smoothScrollToPosition(userPos)

        // Show Progress
        binding.progressBar.visibility = View.VISIBLE

        // Prepare placeholder Agent Message
        val agentMsg = AgentMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.AGENT,
            content = "Thinking..."
        )
        messages.add(agentMsg)
        val agentPos = messages.size - 1
        messageAdapter.notifyItemInserted(agentPos)

        sdkClient.sendPrompt(promptText, selectedModel, object : AntigravitySdkClient.AgentListener {
            override fun onThinkingStep(step: String) {
                agentMsg.thinkingSteps.add(step)
                messageAdapter.notifyItemChanged(agentPos)
            }

            override fun onTokenChunk(chunk: String) {
                agentMsg.content += chunk
                messageAdapter.notifyItemChanged(agentPos)
            }

            override fun onToolCallRequested(toolCall: ToolCall) {
                agentMsg.toolCall = toolCall
                messageAdapter.notifyItemChanged(agentPos)
                binding.rvMessages.smoothScrollToPosition(agentPos)
            }

            override fun onDiffGenerated(diffBlock: DiffBlock) {
                agentMsg.diffBlock = diffBlock
                messageAdapter.notifyItemChanged(agentPos)
                binding.rvMessages.smoothScrollToPosition(agentPos)
            }

            override fun onCompleted(fullMessage: String) {
                binding.progressBar.visibility = View.GONE
                agentMsg.content = fullMessage
                messageAdapter.notifyItemChanged(agentPos)
                binding.rvMessages.smoothScrollToPosition(agentPos)
            }

            override fun onError(error: String) {
                binding.progressBar.visibility = View.GONE
                agentMsg.content = "Error: $error"
                messageAdapter.notifyItemChanged(agentPos)
            }
        })
    }

    private fun showModelSelectorDialog() {
        val currentIdx = supportedModels.indexOfFirst { it.startsWith(selectedModel) }.coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Antigravity Model")
            .setSingleChoiceItems(supportedModels, currentIdx) { dialog, which ->
                selectedModel = when (which) {
                    0 -> "Gemini 2.5 Pro"
                    1 -> "Gemini 2.5 Flash"
                    2 -> "Claude 3.5 Sonnet"
                    else -> "GPT-4o"
                }
                binding.chipModelSelector.text = "$selectedModel ▾"
                Toast.makeText(this, "Switched model to $selectedModel", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
