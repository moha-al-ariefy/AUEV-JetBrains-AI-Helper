package com.hackathon.aihelper.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*

class AIChatToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = AIChatPanel(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(myToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class AIChatPanel(private val project: Project) {

    private val mainPanel = JPanel(BorderLayout())
    private val chatHistoryPanel = JPanel(GridBagLayout()) // Changed to GridBag for dynamic sizing
    private val scrollPane: JBScrollPane
    private val conversationHistory = StringBuilder() // Memory Storage

    // Input Area
    private val inputArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(5)
        emptyText.text = "Ask AI to modify code..."
    }

    init {
        // --- 1. CHAT HISTORY (Vertical List) ---
        // Wrapper to align messages to the top
        val verticalWrapper = JPanel(BorderLayout())
        verticalWrapper.add(chatHistoryPanel, BorderLayout.NORTH)

        scrollPane = JBScrollPane(verticalWrapper)
        scrollPane.border = null
        scrollPane.verticalScrollBar.unitIncrement = 16
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // --- 2. BOTTOM CONTROLS ---
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = JBUI.Borders.empty(10)

        val inputScroll = JBScrollPane(inputArea).apply {
            preferredSize = Dimension(0, 60)
            border = BorderFactory.createLineBorder(JBColor.border())
        }

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))

        val auditBtn = JButton("🛡️ Audit").apply {
            toolTipText = "Scan file for vulnerabilities"
            addActionListener { runAudit() }
        }

        val sendBtn = JButton("Send").apply {
            toolTipText = "Send prompt"
            addActionListener { sendMessage() }
        }

        btnPanel.add(auditBtn)
        btnPanel.add(sendBtn)

        bottomPanel.add(inputScroll, BorderLayout.CENTER)
        bottomPanel.add(btnPanel, BorderLayout.SOUTH)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        // Initial Message
        addAiMessage("Ready! I remember our conversation context now.")

        // Force re-layout on resize to adjust bubble widths
        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                chatHistoryPanel.revalidate()
                chatHistoryPanel.repaint()
            }
        })
    }

    private fun runAudit() {
        val thinking = addAiMessage("🔍 Running Security Audit...")
        // Audit doesn't need conversation history, it's a fresh check
        ChatService.runAudit(project) { response ->
            chatHistoryPanel.remove(thinking)
            addAiMessage("🛡️ **Audit Report:**\n$response")
            refreshUI()
        }
    }

    private fun sendMessage() {
        val text = inputArea.text.trim()
        if (text.isEmpty()) return

        addUserMessage(text)
        inputArea.text = "" // Clear input

        val thinkingPanel = addAiMessage("Thinking...")

        // --- MEMORY LOGIC ---
        // We pack the history into the prompt so the AI knows what happened before.
        // We keep the history strictly limited to text to save tokens.
        val fullPromptWithMemory = """
            $conversationHistory
            User: $text
        """.trimIndent()

        // Update local memory
        conversationHistory.append("\nUser: $text")

        ChatService.sendMessage(project, fullPromptWithMemory) { response ->
            chatHistoryPanel.remove(thinkingPanel)

            // Update local memory with AI response
            conversationHistory.append("\nAI: $response")

            // Heuristic detection for code blocks
            if (response.contains("class ") || response.contains("fun ") ||
                response.contains("def ") || response.contains("public ") ||
                response.contains("#include") || response.contains("import ")) {
                addCodeProposal(response)
            } else {
                addAiMessage(response)
            }
            refreshUI()
        }
    }

    // --- DYNAMIC BUBBLE CREATION ---

    private fun addUserMessage(text: String) {
        val bubble = createBubbleTextArea(text, JBColor(Color(220, 240, 255), Color(50, 80, 120)))
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridwidth = GridBagConstraints.REMAINDER
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.EAST // Right align
            insets = JBUI.insets(5, 50, 5, 5) // Left padding 50 (push to right)
        }
        chatHistoryPanel.add(bubble, constraints)
        refreshUI()
    }

    private fun addAiMessage(text: String): JPanel {
        val bubble = createBubbleTextArea(text, JBColor(Color(240, 240, 240), Color(60, 63, 65)))
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridwidth = GridBagConstraints.REMAINDER
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST // Left align
            insets = JBUI.insets(5, 5, 5, 50) // Right padding 50 (push to left)
        }
        chatHistoryPanel.add(bubble, constraints)
        refreshUI()
        return bubble // Return so we can remove it (for "Thinking...")
    }

    /** * Creates a text area that acts like a dynamic bubble.
     * It uses a JPanel wrapper to handle the background color and border correctly.
     */
    /** * Creates a text area that acts like a dynamic bubble.
     */
    private fun createBubbleTextArea(text: String, bgColor: Color): JPanel {
        val wrapper = JPanel(BorderLayout())
        wrapper.isOpaque = false // Transparent wrapper

        val textArea = JTextArea(text).apply {
            isEditable = false
            isOpaque = true
            background = bgColor
            // REMOVED THE BAD LINE HERE. The default theme color is perfect.
            font = Font("JetBrains Mono", Font.PLAIN, 12)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1, true),
                JBUI.Borders.empty(10)
            )
            lineWrap = true
            wrapStyleWord = true
        }

        wrapper.add(textArea, BorderLayout.CENTER)
        return wrapper
    }

    private fun addCodeProposal(rawCode: String) {
        val displayCode = ChatService.cleanMarkdown(rawCode)

        val cardPanel = JPanel(BorderLayout())
        cardPanel.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.empty(10, 5, 10, 5),
            BorderFactory.createLineBorder(JBColor.border(), 1, true)
        )
        cardPanel.background = JBColor(Color(250, 250, 250), Color(43, 43, 43))

        val headerLabel = JLabel("Suggested Change:")
        headerLabel.border = JBUI.Borders.empty(5)
        headerLabel.font = Font("JetBrains Mono", Font.BOLD, 12)
        cardPanel.add(headerLabel, BorderLayout.NORTH)

        val codePreview = JTextArea(displayCode.take(500) + "...").apply {
            isEditable = false
            font = Font("JetBrains Mono", Font.PLAIN, 11)
            background = JBColor(Color(255, 255, 255), Color(30, 30, 30))
            border = JBUI.Borders.empty(5)
            lineWrap = false // Code shouldn't wrap weirdly in preview
        }
        cardPanel.add(codePreview, BorderLayout.CENTER)

        val btnPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val applyBtn = JButton("✅ Apply").apply {
            background = Color(100, 180, 100)
            addActionListener {
                ChatService.applyCodeToCurrentFile(project, rawCode)

                // Minimize logic
                cardPanel.removeAll()
                val successLabel = JLabel("✅ Code Applied")
                successLabel.border = JBUI.Borders.empty(10)
                cardPanel.add(successLabel, BorderLayout.CENTER)
                cardPanel.revalidate()
                cardPanel.repaint()
            }
        }

        val rejectBtn = JButton("❌ Reject").apply {
            addActionListener {
                chatHistoryPanel.remove(cardPanel)
                refreshUI()
            }
        }

        btnPanel.add(applyBtn)
        btnPanel.add(rejectBtn)
        cardPanel.add(btnPanel, BorderLayout.SOUTH)

        // Add to list with GridBag constraints
        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridwidth = GridBagConstraints.REMAINDER
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(5, 5, 5, 5)
        }
        chatHistoryPanel.add(cardPanel, constraints)
        refreshUI()
    }

    private fun refreshUI() {
        chatHistoryPanel.revalidate()
        chatHistoryPanel.repaint()

        SwingUtilities.invokeLater {
            val vertical = scrollPane.verticalScrollBar
            vertical.value = vertical.maximum
        }
    }

    fun getContent() = mainPanel
}