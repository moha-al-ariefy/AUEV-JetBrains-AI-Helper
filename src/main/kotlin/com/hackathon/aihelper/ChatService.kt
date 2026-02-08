/*
 *    Copyright 2026 moha-al-ariefy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.hackathon.aihelper.ui

import com.hackathon.aihelper.settings.AppSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.net.HttpURLConnection
import java.net.URI // <--- Java 21 Friend
import java.nio.charset.StandardCharsets
import java.util.function.Consumer

object ChatService {

    private val apiKey: String
        get() = AppSettingsState.getInstance().apiKey

    private val model: String
        get() = AppSettingsState.getInstance().modelName // I removed the default here so I can handle it smarter later

    // I added this enum so the chat knows who it's talking to
    private enum class Provider { OPENAI, ANTHROPIC, GROQ }

    private fun getProvider(): Provider {
        return when {
            apiKey.startsWith("sk-ant-") -> Provider.ANTHROPIC
            apiKey.startsWith("gsk_") -> Provider.GROQ
            else -> Provider.OPENAI
        }
    }

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

                // Now I call the unified AI function
                val response = callAI(systemPrompt, fullMessage)

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

                // I changed this to callAI so it works with Claude too
                val response = callAI(prompt, currentCode)

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
    // I renamed this from callOpenAI to callAI because we are inclusive now
    private fun callAI(systemPrompt: String, userMessage: String): String {
        val provider = getProvider()

        // I define the endpoints for everyone
        val urlStr = when (provider) {
            Provider.ANTHROPIC -> "https://api.anthropic.com/v1/messages"
            Provider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            Provider.OPENAI -> "https://api.openai.com/v1/chat/completions"
        }

        // I added smart model selection because sending 'gpt-4o' to Groq is like asking for a Whopper at McDonald's
        val actualModel = when {
            // If it's Groq and the model is missing or set to the default OpenAI one, switch to Llama 3.3
            provider == Provider.GROQ && (model.isBlank() || model.startsWith("gpt")) -> "llama-3.3-70b-versatile"
            // If it's Anthropic, default to Claude 3.5
            provider == Provider.ANTHROPIC && (model.isBlank() || model.startsWith("gpt")) -> "claude-3-5-sonnet-20240620"
            // Default fallback
            else -> model.ifBlank { "gpt-4o" }
        }

        // I used URI.create().toURL() because URL(string) is deprecated in Java 20+
        val url = URI.create(urlStr).toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true

        // Headers vary by provider
        if (provider == Provider.ANTHROPIC) {
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.setRequestProperty("content-type", "application/json")
        } else {
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
        }

        // JSON Body Construction
        // Anthropic hates "system" inside messages, so I have to treat it differently
        val jsonInput = if (provider == Provider.ANTHROPIC) {
            """
            {
                "model": "$actualModel",
                "max_tokens": 2000,
                "system": "${escapeJson(systemPrompt)}",
                "messages": [
                    {"role": "user", "content": "${escapeJson(userMessage)}"}
                ]
            }
            """.trimIndent()
        } else {
            """
            {
                "model": "$actualModel",
                "messages": [
                    {"role": "system", "content": "${escapeJson(systemPrompt)}"},
                    {"role": "user", "content": "${escapeJson(userMessage)}"}
                ],
                "max_tokens": 2000
            }
            """.trimIndent()
        }

        conn.outputStream.use { os -> os.write(jsonInput.toByteArray(StandardCharsets.UTF_8)) }

        if (conn.responseCode != 200) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw RuntimeException("API Error (${conn.responseCode}): $err")
        }

        val rawResponse = conn.inputStream.bufferedReader().use { it.readText() }
        return extractContent(rawResponse, provider)
    }

    private fun escapeJson(text: String) = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t")

    private fun extractContent(json: String, provider: Provider): String {
        // Anthropic returns "text": "...", OpenAI/Groq return "content": "..."
        val startMarker = if (provider == Provider.ANTHROPIC) "\"text\": \"" else "\"content\": \""
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