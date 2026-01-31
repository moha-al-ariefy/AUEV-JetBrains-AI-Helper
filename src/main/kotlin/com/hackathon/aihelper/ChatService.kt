package com.hackathon.aihelper.ui

import com.hackathon.aihelper.PluginConfig
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

object ChatService {

    private val API_KEY = PluginConfig.API_KEY

    // --- CHAT LOGIC ---
    fun sendMessage(project: Project, userPrompt: String, onResponse: Consumer<String>) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor

        // 1. Detect File Type
        val currentCode = editor?.document?.text ?: "No file open."
        val fileExtension = editor?.virtualFile?.extension ?: "unknown"

        // Map extension to language name for better AI context
        val language = when (fileExtension) {
            "kt" -> "Kotlin"
            "java" -> "Java"
            "py" -> "Python"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "cpp", "c", "h" -> "C/C++"
            else -> fileExtension.uppercase()
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 2. Add Language Context to System Prompt
                val systemPrompt = "You are an expert $language coding assistant. " +
                        "If the user asks for code, output ONLY the code block. " +
                        "If they ask a question, answer normally. " +
                        "Do not use markdown blocks like ```java or ```c in the final output unless requested."

                val fullMessage = "Current File ($language):\n$currentCode\n\nRequest: $userPrompt"

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
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) {
            onResponse.accept("No file open to audit.")
            return
        }
        val currentCode = editor.document.text
        val fileExtension = editor.virtualFile?.extension ?: "code"

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val response = callOpenAI(
                    systemPrompt = "You are a Security Auditor. Scan this .$fileExtension file for vulnerabilities. Be concise. List issues and fixes.",
                    userMessage = currentCode
                )
                ApplicationManager.getApplication().invokeLater {
                    onResponse.accept(response)
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    onResponse.accept("Audit Error: ${e.message}")
                }
            }
        }
    }

    // --- FILE MODIFICATION LOGIC ---
    fun applyCodeToCurrentFile(project: Project, code: String) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val cleanCode = cleanMarkdown(code)

        ApplicationManager.getApplication().invokeLater {
            WriteCommandAction.runWriteCommandAction(project) {
                editor.document.setText(cleanCode)
            }
        }
    }

    // Helper to strip ```java, ```c, etc.
    fun cleanMarkdown(text: String): String {
        return text.replace(Regex("```[a-zA-Z]*"), "")
            .replace("```", "")
            .trim()
    }

    // --- NETWORKING ---
    private fun callOpenAI(systemPrompt: String, userMessage: String): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $API_KEY")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val jsonInput = """
            {
                "model": "gpt-4o",
                "messages": [
                    {"role": "system", "content": "${escapeJson(systemPrompt)}"},
                    {"role": "user", "content": "${escapeJson(userMessage)}"}
                ],
                "max_tokens": 1500
            }
        """.trimIndent()

        conn.outputStream.use { os -> os.write(jsonInput.toByteArray(StandardCharsets.UTF_8)) }

        if (conn.responseCode != 200) {
            val errorStream = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown Error"
            throw RuntimeException("API Error ${conn.responseCode}: $errorStream")
        }

        return extractContent(conn.inputStream.bufferedReader().use { it.readText() })
    }

    private fun escapeJson(text: String) = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

    private fun extractContent(json: String): String {
        val startMarker = "\"content\": \""
        val start = json.indexOf(startMarker)
        if (start == -1) return "Error parsing response."

        val actualStart = start + startMarker.length
        var i = actualStart
        while (i < json.length) {
            if (json[i] == '"' && json[i-1] != '\\') break
            i++
        }
        return json.substring(actualStart, i)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\t", "\t")
    }
}