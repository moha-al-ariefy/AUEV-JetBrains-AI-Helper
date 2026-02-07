package com.hackathon.aihelper.ui

import com.hackathon.aihelper.settings.AppSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

object ChatService {

    private val apiKey: String
        get() = AppSettingsState.getInstance().apiKey

    private val model: String
        get() = AppSettingsState.getInstance().modelName.ifBlank { "gpt-4o" }

    // --- CHAT LOGIC ---
    fun sendMessage(project: Project, userPrompt: String, onResponse: Consumer<String>) {
        if (apiKey.isBlank()) {
            onResponse.accept("⚠️ Please configure your API Key in Settings.")
            return
        }

        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val currentCode = editor?.document?.text ?: "No file open."
        val fileExtension = editor?.virtualFile?.extension ?: "unknown"

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // FIXED: Professional, Senior-Level System Prompt
                val systemPrompt = """
                    You are AUEV, an expert coding assistant integrated into IntelliJ IDEA.
                    
                    RULES FOR CHAT:
                    - Be professional, concise, and helpful. 
                    - Focus on high-quality, maintainable solutions.
                    
                    RULES FOR CODE GENERATION:
                    - ALWAYS return the FULL, executable code. No placeholders.
                    - Code must follow best practices (SOLID principles, Clean Code).
                    - COMMENTS MUST BE PROFESSIONAL:
                      - Use standard Javadoc/KDoc formatting where appropriate.
                      - Explain *WHY*, not just *what*.
                      - Use imperative voice (e.g., "Calculates the hash..." not "I calculate...").
                      - Do not use first-person pronouns ("I", "We").
                    
                    Current Context: File Type ($fileExtension).
                """.trimIndent()

                val fullMessage = "Context:\n$currentCode\n\nUser Question: $userPrompt"

                val response = callOpenAI(systemPrompt, fullMessage)

                ApplicationManager.getApplication().invokeLater {
                    onResponse.accept(response)
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    onResponse.accept("Error: ${e.message}")
                }
            }
        }
    }

    // --- AUDIT LOGIC ---
    fun runAudit(project: Project, onResponse: Consumer<String>) {
        if (apiKey.isBlank()) {
            onResponse.accept("⚠️ Set your API Key first.")
            return
        }

        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val currentCode = editor.document.text

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Security Audit Prompt remains strict
                val prompt = "You are a Senior Security Engineer. Analyze this code for vulnerabilities (OWASP Top 10). Return a concise, prioritized list of issues and recommended fixes."
                val response = callOpenAI(prompt, currentCode)

                ApplicationManager.getApplication().invokeLater {
                    onResponse.accept(response)
                }
            } catch (e: Exception) {
                onResponse.accept("Error: ${e.message}")
            }
        }
    }

    // --- EDITOR MANIPULATION ---

    fun applyCodeToCurrentFile(project: Project, code: String) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val cleanCode = extractCodeBlock(code)

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project, "Apply AI Code", "AUEV", {
                editor.document.setText(cleanCode)
            })
        }
    }

    fun undoLastAction(project: Project) {
        val fileEditor = FileEditorManager.getInstance(project).selectedEditor
        if (fileEditor != null) {
            ApplicationManager.getApplication().invokeLater {
                val undoManager = UndoManager.getInstance(project)
                if (undoManager.isUndoAvailable(fileEditor)) {
                    undoManager.undo(fileEditor)
                }
            }
        }
    }

    // --- TEXT PARSING ---

    fun cleanMarkdown(text: String): String {
        return extractCodeBlock(text)
    }

    private fun extractCodeBlock(text: String): String {
        // Robust Regex to extract code between ```backticks```
        val pattern = Regex("```(?:[a-zA-Z]*)?\\n([\\s\\S]*?)```")
        val match = pattern.find(text)

        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            // Fallback for when the AI skips markdown (rare)
            text.replace("```", "").trim()
        }
    }

    // --- NETWORK ---
    private fun callOpenAI(systemPrompt: String, userMessage: String): String {
        val url = URL("[https://api.openai.com/v1/chat/completions](https://api.openai.com/v1/chat/completions)")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val jsonInput = """
            {
                "model": "$model",
                "messages": [
                    {"role": "system", "content": "${escapeJson(systemPrompt)}"},
                    {"role": "user", "content": "${escapeJson(userMessage)}"}
                ],
                "max_tokens": 2000
            }
        """.trimIndent()

        conn.outputStream.use { os -> os.write(jsonInput.toByteArray(StandardCharsets.UTF_8)) }

        if (conn.responseCode != 200) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw RuntimeException("API Error: $err")
        }

        return extractContent(conn.inputStream.bufferedReader().use { it.readText() })
    }

    private fun escapeJson(text: String) = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t")

    private fun extractContent(json: String): String {
        val startMarker = "\"content\": \""
        val start = json.indexOf(startMarker)
        if (start == -1) return "Error parsing response."

        val actualStart = start + startMarker.length
        val sb = StringBuilder()
        var i = actualStart
        var escaped = false

        while (i < json.length) {
            val c = json[i]
            if (escaped) {
                when(c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(c)
                }
                escaped = false
            } else {
                if (c == '\\') escaped = true
                else if (c == '"') break
                else sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}