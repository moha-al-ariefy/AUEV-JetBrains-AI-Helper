package com.hackathon.aihelper.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AppSettingsComponent {

    val panel: JPanel
    private val apiKeyField = JBTextField()
    private val modelField = JBTextField()

    init {
        // Smart Auto-Detect Logic
        apiKeyField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { detect() }
            override fun removeUpdate(e: DocumentEvent?) { detect() }
            override fun changedUpdate(e: DocumentEvent?) { detect() }

            fun detect() {
                val key = apiKeyField.text.trim()
                if (key.isEmpty()) return

                // 1. Anthropic (Claude)
                if (key.startsWith("sk-ant-")) {
                    modelField.text = "claude-3-5-sonnet-20240620"
                }
                // 2. Groq (Llama/Mixtral - Insanely fast)
                else if (key.startsWith("gsk_")) {
                    modelField.text = "llama3-70b-8192"
                }
                // 3. OpenAI (Standard)
                else if (key.startsWith("sk-")) {
                    if (modelField.text.isBlank() || !modelField.text.startsWith("gpt")) {
                        modelField.text = "gpt-4o"
                    }
                }
            }
        })

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("API Key (OpenAI / Anthropic / Groq):"), apiKeyField, 1, false)
            .addLabeledComponent(JBLabel("Model Name (Auto-Detected):"), modelField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPreferredFocusedComponent() = apiKeyField

    var apiKey: String
        get() = apiKeyField.text
        set(newText) { apiKeyField.text = newText }

    var modelName: String
        get() = modelField.text
        set(newText) { modelField.text = newText }
}