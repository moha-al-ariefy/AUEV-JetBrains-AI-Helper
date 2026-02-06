package com.hackathon.aihelper.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AppSettingsComponent {

    val panel: JPanel
    private val apiKeyField = JBTextField()
    private val modelField = JBTextField()

    // New toys for the user to play with
    private val ghostTextCheckbox = JBCheckBox("Enable Ghost Text (The invisible pair programmer)")
    private val securityModeCheckbox = JBCheckBox("Paranoid Security Mode (OWASP Focus)")

    init {
        // If they paste a key, try to guess the model so they don't have to think.
        // Thinking is hard after midnight.
        apiKeyField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { detect() }
            override fun removeUpdate(e: DocumentEvent?) { detect() }
            override fun changedUpdate(e: DocumentEvent?) { detect() }

            fun detect() {
                val key = apiKeyField.text.trim()
                if (key.isEmpty()) return

                // 1. Anthropic (Claude is smart but expensive)
                if (key.startsWith("sk-ant-")) {
                    modelField.text = "claude-3-5-sonnet-20240620"
                }
                // 2. Groq (Go brrr)
                else if (key.startsWith("gsk_")) {
                    modelField.text = "llama3-70b-8192"
                }
                // 3. OpenAI (Old reliable)
                else if (key.startsWith("sk-")) {
                    if (modelField.text.isBlank() || !modelField.text.startsWith("gpt")) {
                        modelField.text = "gpt-4o"
                    }
                }
            }
        })

        // Layout: Trying to make it look like we actually know Swing.
        panel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("API Configuration"))
            .addSeparator()
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField, 1, false)
            .addTooltip("Supports OpenAI (sk-), Anthropic (sk-ant-), and Groq (gsk_)")
            .addLabeledComponent(JBLabel("Model Name:"), modelField, 1, false)

            .addVerticalGap(10)
            .addComponent(JBLabel("Features"))
            .addSeparator()
            .addComponent(ghostTextCheckbox)
            .addComponent(securityModeCheckbox)
            .addTooltip("If enabled, the AI will scream at you about SQL injection.")

            .addComponentFillVertically(JPanel(), 0)
            .panel
            .apply {
                border = JBUI.Borders.empty(10) // Breathing room is important
            }
    }

    fun getPreferredFocusedComponent() = apiKeyField

    // Getters and Setters (Boilerplate city)

    var apiKey: String
        get() = apiKeyField.text
        set(newText) { apiKeyField.text = newText }

    var modelName: String
        get() = modelField.text
        set(newText) { modelField.text = newText }

    var isGhostTextEnabled: Boolean
        get() = ghostTextCheckbox.isSelected
        set(newStatus) { ghostTextCheckbox.isSelected = newStatus }

    var isSecurityModeEnabled: Boolean
        get() = securityModeCheckbox.isSelected
        set(newStatus) { securityModeCheckbox.isSelected = newStatus }
}