package com.google.antigravity.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.antigravity.mobile.databinding.ItemMessageAgentBinding
import com.google.antigravity.mobile.databinding.ItemMessageUserBinding
import com.google.antigravity.mobile.model.AgentMessage
import com.google.antigravity.mobile.model.MessageRole
import com.google.antigravity.mobile.model.ToolApprovalStatus
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: MutableList<AgentMessage>,
    private val onToolApprovalClicked: (message: AgentMessage, approved: Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AGENT = 2
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].role) {
            MessageRole.USER -> TYPE_USER
            MessageRole.AGENT, MessageRole.SYSTEM -> TYPE_AGENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserViewHolder(ItemMessageUserBinding.inflate(inflater, parent, false))
        } else {
            AgentViewHolder(ItemMessageAgentBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is UserViewHolder) {
            holder.bind(msg)
        } else if (holder is AgentViewHolder) {
            holder.bind(msg)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class UserViewHolder(private val binding: ItemMessageUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: AgentMessage) {
            binding.tvUserContent.text = message.content
            binding.tvUserTimestamp.text = timeFormat.format(Date(message.timestamp))
        }
    }

    inner class AgentViewHolder(private val binding: ItemMessageAgentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: AgentMessage) {
            binding.tvAgentContent.text = message.content
            binding.tvAgentTimestamp.text = timeFormat.format(Date(message.timestamp))
            binding.tvModelBadge.text = message.model

            // 1. Thinking Process
            if (message.thinkingSteps.isNotEmpty()) {
                binding.cardThinking.visibility = View.VISIBLE
                binding.tvThinkingContent.text = message.thinkingSteps.joinToString("\n• ", prefix = "• ")
                binding.tvThinkingContent.visibility =
                    if (message.isThinkingExpanded) View.VISIBLE else View.GONE
                binding.tvThinkingToggle.text = if (message.isThinkingExpanded) "▲" else "▼"

                binding.layoutThinkingHeader.setOnClickListener {
                    message.isThinkingExpanded = !message.isThinkingExpanded
                    binding.tvThinkingContent.visibility =
                        if (message.isThinkingExpanded) View.VISIBLE else View.GONE
                    binding.tvThinkingToggle.text = if (message.isThinkingExpanded) "▲" else "▼"
                }
            } else {
                binding.cardThinking.visibility = View.GONE
            }

            // 2. Tool Call & Native Approvals
            val tool = message.toolCall
            if (tool != null) {
                binding.cardToolCall.visibility = View.VISIBLE
                binding.tvToolName.text = "⚡ ${tool.toolName}"
                binding.tvToolCommand.text = tool.command
                binding.tvToolStatusBadge.text = tool.status.name

                when (tool.status) {
                    ToolApprovalStatus.PENDING -> {
                        binding.layoutToolButtons.visibility = View.VISIBLE
                        binding.tvToolOutput.visibility = View.GONE
                        binding.tvToolStatusBadge.setTextColor(0xFFFBBC05.toInt())

                        binding.btnApproveTool.setOnClickListener {
                            onToolApprovalClicked(message, true)
                        }
                        binding.btnRejectTool.setOnClickListener {
                            onToolApprovalClicked(message, false)
                        }
                    }
                    ToolApprovalStatus.APPROVED, ToolApprovalStatus.EXECUTED -> {
                        binding.layoutToolButtons.visibility = View.GONE
                        binding.tvToolOutput.visibility = View.VISIBLE
                        binding.tvToolOutput.text = tool.output ?: "Execution finished with status 0."
                        binding.tvToolStatusBadge.setTextColor(0xFF34A853.toInt())
                    }
                    ToolApprovalStatus.REJECTED -> {
                        binding.layoutToolButtons.visibility = View.GONE
                        binding.tvToolOutput.visibility = View.VISIBLE
                        binding.tvToolOutput.text = "Execution rejected by user."
                        binding.tvToolStatusBadge.setTextColor(0xFFFF6B6B.toInt())
                    }
                }
            } else {
                binding.cardToolCall.visibility = View.GONE
            }

            // 3. Diff Block
            val diff = message.diffBlock
            if (diff != null) {
                binding.cardDiff.visibility = View.VISIBLE
                binding.tvDiffFilePath.text = diff.filePath
                binding.tvDiffStats.text = "+${diff.additions} / -${diff.deletions}"
                binding.tvDiffSnippet.text = diff.diffText
            } else {
                binding.cardDiff.visibility = View.GONE
            }
        }
    }
}
