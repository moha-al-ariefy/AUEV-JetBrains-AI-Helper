package com.hackathon.aihelper.ui

import com.hackathon.aihelper.settings.AppSettingsState
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
    private val cardLayout = CardLayout()
    private val contentPanel = JPanel(cardLayout) // Swaps between Chat and Config

    // Chat View Components
    private val chatView = JPanel(BorderLayout())
    private val chatHistoryPanel = JPanel(GridBagLayout())
    private val conversationHistory = StringBuilder()
    private val modelSelector = JComboBox(arrayOf("gpt-4o", "gpt-3.5-turbo", "o1-mini"))
    private val inputArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(5)
        emptyText.text = "Ask AI to modify code..."
    }

    // Config View Components
    private val configView = JPanel(GridBagLayout())
    private val apiKeyField = JTextField(25)

    private lateinit var scrollPane: JBScrollPane

    init {
        setupChatUI()
        setupConfigUI()

        contentPanel.add(chatView, "CHAT")
        contentPanel.add(configView, "CONFIG")
        mainPanel.add(contentPanel, BorderLayout.CENTER)

        // --- INITIAL STATE CHECK ---
        val settings = AppSettingsState.getInstance()
        if (settings.apiKey.isBlank()) {
            cardLayout.show(contentPanel, "CONFIG")
        } else {
            modelSelector.selectedItem = settings.modelName
            cardLayout.show(contentPanel, "CHAT")
        }

        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                chatHistoryPanel.revalidate()
                chatHistoryPanel.repaint()
            }
        })
    }

    private fun setupChatUI() {
        // 1. TOP BAR (Model Selector + Settings Icon)
        val topBar = JPanel(BorderLayout())
        topBar.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)

        val modelPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        modelPanel.add(JLabel("Model:"))
        modelSelector.addActionListener {
            AppSettingsState.getInstance().modelName = modelSelector.selectedItem as String
        }
        modelPanel.add(modelSelector)

        val settingsBtn = JButton("⚙️").apply {
            toolTipText = "API Settings"
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener {
                apiKeyField.text = AppSettingsState.getInstance().apiKey
                cardLayout.show(contentPanel, "CONFIG")
            }
        }

        topBar.add(modelPanel, BorderLayout.WEST)
        topBar.add(settingsBtn, BorderLayout.EAST)
        chatView.add(topBar, BorderLayout.NORTH)

        // 2. CHAT HISTORY
        val verticalWrapper = JPanel(BorderLayout())
        verticalWrapper.add(chatHistoryPanel, BorderLayout.NORTH)
        scrollPane = JBScrollPane(verticalWrapper)
        scrollPane.border = null
        scrollPane.verticalScrollBar.unitIncrement = 16
        chatView.add(scrollPane, BorderLayout.CENTER)

        // 3. BOTTOM CONTROLS
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = JBUI.Borders.empty(10)

        val inputScroll = JBScrollPane(inputArea).apply {
            preferredSize = Dimension(0, 80)
            border = BorderFactory.createLineBorder(JBColor.border())
        }

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val auditBtn = JButton("🛡️ Audit").apply { addActionListener { runAudit() } }
        val sendBtn = JButton("Send").apply { addActionListener { sendMessage() } }

        btnPanel.add(auditBtn)
        btnPanel.add(sendBtn)
        bottomPanel.add(inputScroll, BorderLayout.CENTER)
        bottomPanel.add(btnPanel, BorderLayout.SOUTH)
        chatView.add(bottomPanel, BorderLayout.SOUTH)

        addAiMessage("Hello! I am ready to help.")
    }

    private fun setupConfigUI() {
        configView.border = JBUI.Borders.empty(20)
        val gbc = GridBagConstraints().apply {
            gridx = 0; gridy = 0; insets = JBUI.insets(10); anchor = GridBagConstraints.CENTER
        }

        val title = JLabel("AI Configuration").apply {
            font = Font("JetBrains Mono", Font.BOLD, 16)
        }
        configView.add(title, gbc)

        gbc.gridy++
        configView.add(JLabel("OpenAI API Key:"), gbc)

        gbc.gridy++
        configView.add(apiKeyField, gbc)

        gbc.gridy++
        val saveBtn = JButton("Save & Open Chat").apply {
            addActionListener {
                val key = apiKeyField.text.trim()
                if (key.isNotEmpty()) {
                    val state = AppSettingsState.getInstance()
                    state.apiKey = key
                    if (state.modelName.isBlank()) state.modelName = "gpt-4o"
                    cardLayout.show(contentPanel, "CHAT")
                }
            }
        }
        configView.add(saveBtn, gbc)
    }

    // --- LOGIC METHODS (Preserved from your code) ---

    private fun sendMessage() {
        val text = inputArea.text.trim()
        if (text.isEmpty()) return

        addUserMessage(text)
        inputArea.text = ""
        val thinkingPanel = addAiMessage("Thinking...")

        val fullPromptWithMemory = "$conversationHistory\nUser: $text".trimIndent()
        conversationHistory.append("\nUser: $text")

        ChatService.sendMessage(project, fullPromptWithMemory) { response ->
            chatHistoryPanel.remove(thinkingPanel)
            conversationHistory.append("\nAI: $response")

            if (response.contains("class ") || response.contains("fun ") ||
                response.contains("def ") || response.contains("public ")) {
                addCodeProposal(response)
            } else {
                addAiMessage(response)
            }
            refreshUI()
        }
    }

    private fun runAudit() {
        val thinking = addAiMessage("🔍 Running Security Audit...")
        ChatService.runAudit(project) { response ->
            chatHistoryPanel.remove(thinking)
            addAiMessage("🛡️ **Audit Report:**\n$response")
            refreshUI()
        }
    }

    private fun addUserMessage(text: String) {
        val bubble = createBubbleTextArea(text, JBColor(Color(220, 240, 255), Color(50, 80, 120)))
        val constraints = GridBagConstraints().apply {
            gridx = 0; gridwidth = GridBagConstraints.REMAINDER; weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL; anchor = GridBagConstraints.EAST
            insets = JBUI.insets(5, 50, 5, 5)
        }
        chatHistoryPanel.add(bubble, constraints)
        refreshUI()
    }

    private fun addAiMessage(text: String): JPanel {
        val bubble = createBubbleTextArea(text, JBColor(Color(240, 240, 240), Color(60, 63, 65)))
        val constraints = GridBagConstraints().apply {
            gridx = 0; gridwidth = GridBagConstraints.REMAINDER; weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL; anchor = GridBagConstraints.WEST
            insets = JBUI.insets(5, 5, 5, 50)
        }
        chatHistoryPanel.add(bubble, constraints)
        refreshUI()
        return bubble
    }

    private fun createBubbleTextArea(text: String, bgColor: Color): JPanel {
        val wrapper = JPanel(BorderLayout()).apply { isOpaque = false }
        val textArea = JTextArea(text).apply {
            isEditable = false; isOpaque = true; background = bgColor
            font = Font("JetBrains Mono", Font.PLAIN, 12)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1, true),
                JBUI.Borders.empty(10)
            )
            lineWrap = true; wrapStyleWord = true
        }
        wrapper.add(textArea, BorderLayout.CENTER)
        return wrapper
    }

    private fun addCodeProposal(rawCode: String) {
        val displayCode = ChatService.cleanMarkdown(rawCode)
        val cardPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.empty(10, 5, 10, 5),
                BorderFactory.createLineBorder(JBColor.border(), 1, true)
            )
            background = JBColor(Color(250, 250, 250), Color(43, 43, 43))
        }

        val codePreview = JTextArea(displayCode.take(500) + "...").apply {
            isEditable = false; font = Font("JetBrains Mono", Font.PLAIN, 11)
            background = JBColor(Color(255, 255, 255), Color(30, 30, 30))
            border = JBUI.Borders.empty(5)
        }

        val applyBtn = JButton("✅ Apply").apply {
            addActionListener {
                ChatService.applyCodeToCurrentFile(project, rawCode)
                cardPanel.removeAll()
                cardPanel.add(JLabel("✅ Code Applied"), BorderLayout.CENTER)
                refreshUI()
            }
        }

        cardPanel.add(codePreview, BorderLayout.CENTER)
        cardPanel.add(applyBtn, BorderLayout.SOUTH)

        val constraints = GridBagConstraints().apply {
            gridx = 0; gridwidth = GridBagConstraints.REMAINDER; weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL; insets = JBUI.insets(5)
        }
        chatHistoryPanel.add(cardPanel, constraints)
        refreshUI()
    }

    private fun refreshUI() {
        chatHistoryPanel.revalidate()
        chatHistoryPanel.repaint()
        SwingUtilities.invokeLater {
            if (::scrollPane.isInitialized) {
                scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
            }
        }
    }

    fun getContent() = mainPanel
}