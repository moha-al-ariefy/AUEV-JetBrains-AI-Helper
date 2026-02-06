package com.hackathon.aihelper.ui

import com.hackathon.aihelper.settings.AppSettingsConfigurable
import com.hackathon.aihelper.settings.AppSettingsState
import com.intellij.openapi.options.ShowSettingsUtil
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

    // Main container. If this panel could talk, it would scream.
    private val mainPanel = JPanel(CardLayout())
    private val chatView = JPanel(BorderLayout())
    private val setupView = JPanel(GridBagLayout()) // For when the user inevitably forgets the API key

    // Chat components
    private val chatHistoryPanel = JPanel(GridBagLayout()) // GridBag is pain, but it works
    private val conversationHistory = StringBuilder()
    private val inputArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        border = JBUI.Borders.empty(5)
        emptyText.text = "Ask the ghost in the machine..."
    }

    private lateinit var scrollPane: JBScrollPane

    init {
        setupSetupView() // "Setup Setup" - naming things is hard
        setupChatView()

        mainPanel.add(setupView, "SETUP")
        mainPanel.add(chatView, "CHAT")

        // Check if we actually have a key, or if we are just pretending.
        checkApiKey()

        // Swing requires a blood sacrifice to resize correctly.
        mainPanel.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                chatHistoryPanel.revalidate()
                chatHistoryPanel.repaint()
            }
        })
    }

    private fun checkApiKey() {
        val settings = AppSettingsState.getInstance()
        val layout = mainPanel.layout as CardLayout
        if (settings.apiKey.isBlank()) {
            layout.show(mainPanel, "SETUP")
        } else {
            layout.show(mainPanel, "CHAT")
        }
    }

    private fun setupSetupView() {
        val gbc = GridBagConstraints().apply {
            gridx = 0; gridy = 0; insets = JBUI.insets(10); anchor = GridBagConstraints.CENTER
        }

        val title = JLabel("⚠️ Missing API Key").apply {
            font = Font("JetBrains Mono", Font.BOLD, 16)
            foreground = JBColor.RED
        }
        setupView.add(title, gbc)

        gbc.gridy++
        setupView.add(JLabel("I can't help you if I can't think."), gbc)

        gbc.gridy++
        val configBtn = JButton("Open Settings").apply {
            addActionListener {
                // Opens the nice settings page we built earlier
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable::class.java)
                checkApiKey() // Re-check after they close the dialog
            }
        }
        setupView.add(configBtn, gbc)
    }

    private fun setupChatView() {
        // 1. TOP BAR
        val topBar = JPanel(BorderLayout())
        topBar.border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)

        val titleLabel = JLabel(" AI Assistant").apply {
            font = Font("JetBrains Mono", Font.BOLD, 12)
        }

        val settingsBtn = JButton("⚙️").apply {
            toolTipText = "Configure AI"
            isBorderPainted = false
            isContentAreaFilled = false
            addActionListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable::class.java)
                checkApiKey()
            }
        }

        topBar.add(titleLabel, BorderLayout.WEST)
        topBar.add(settingsBtn, BorderLayout.EAST)
        chatView.add(topBar, BorderLayout.NORTH)

        // 2. CHAT HISTORY (The Scroll Zone)
        val verticalWrapper = JPanel(BorderLayout())
        verticalWrapper.add(chatHistoryPanel, BorderLayout.NORTH)

        scrollPane = JBScrollPane(verticalWrapper).apply {
            border = null
            verticalScrollBar.unitIncrement = 16 // Make scrolling feel less like moving through molasses
        }
        chatView.add(scrollPane, BorderLayout.CENTER)

        // 3. BOTTOM CONTROLS
        val bottomPanel = JPanel(BorderLayout())
        bottomPanel.border = JBUI.Borders.empty(10)

        val inputScroll = JBScrollPane(inputArea).apply {
            preferredSize = Dimension(0, 80)
            border = BorderFactory.createLineBorder(JBColor.border())
        }

        val btnPanel = JPanel(FlowLayout(FlowLayout.RIGHT))

        val auditBtn = JButton("🛡️ Audit Code").apply {
            toolTipText = "Judge my code securely"
            addActionListener { runAudit() }
        }

        val sendBtn = JButton("Send").apply {
            addActionListener { sendMessage() }
        }

        btnPanel.add(auditBtn)
        btnPanel.add(sendBtn)

        bottomPanel.add(inputScroll, BorderLayout.CENTER)
        bottomPanel.add(btnPanel, BorderLayout.SOUTH)
        chatView.add(bottomPanel, BorderLayout.SOUTH)

        addAiMessage("I'm awake. What did I miss?")
    }

    private fun sendMessage() {
        val text = inputArea.text.trim()
        if (text.isEmpty()) return

        addUserMessage(text)
        inputArea.text = ""
        val thinkingPanel = addAiMessage("Thinking... (or buffering)...")

        // Include history so it doesn't forget who I am every 5 seconds
        val fullPromptWithMemory = "$conversationHistory\nUser: $text".trimIndent()
        conversationHistory.append("\nUser: $text")

        ChatService.sendMessage(project, fullPromptWithMemory) { response ->
            chatHistoryPanel.remove(thinkingPanel)
            conversationHistory.append("\nAI: $response")

            // If it looks like code, treat it like code.
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
        val thinking = addAiMessage("🔍 Analyzing for bugs and bad life choices...")
        ChatService.runAudit(project) { response ->
            chatHistoryPanel.remove(thinking)
            addAiMessage("🛡️ **Security Report:**\n$response")
            refreshUI()
        }
    }

    // --- UI HELPERS (Don't look too closely) ---

    private fun addUserMessage(text: String) {
        // User bubbles stick to the Right
        val bubble = createBubbleTextArea(text, JBColor(Color(220, 240, 255), Color(60, 90, 120)))
        val constraints = GridBagConstraints().apply {
            gridx = 0; gridwidth = GridBagConstraints.REMAINDER; weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL; anchor = GridBagConstraints.EAST
            insets = JBUI.insets(5, 50, 5, 5)
        }
        chatHistoryPanel.add(bubble, constraints)
        refreshUI()
    }

    private fun addAiMessage(text: String): JPanel {
        // AI bubbles stick to the Left
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
            // Fake rounded corners using borders because Graphics2D is scary
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
            background = JBColor(Color(250, 250, 250), Color(45, 45, 45))
        }

        val codePreview = JTextArea(displayCode.take(400) + "... (click apply for full)").apply {
            isEditable = false; font = Font("JetBrains Mono", Font.PLAIN, 11)
            background = JBColor(Color(255, 255, 255), Color(30, 30, 30))
            border = JBUI.Borders.empty(5)
        }

        val applyBtn = JButton("✅ Apply Code").apply {
            addActionListener {
                ChatService.applyCodeToCurrentFile(project, rawCode)
                cardPanel.removeAll()
                cardPanel.add(JLabel("✅ Applied. Good luck."), BorderLayout.CENTER)
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
        // Auto-scroll to bottom, hopefully.
        SwingUtilities.invokeLater {
            if (::scrollPane.isInitialized) {
                scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
            }
        }
    }

    fun getContent() = mainPanel
}