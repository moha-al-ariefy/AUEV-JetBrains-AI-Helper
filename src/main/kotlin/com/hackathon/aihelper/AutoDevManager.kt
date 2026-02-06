package com.hackathon.aihelper

import com.hackathon.aihelper.settings.AppSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.util.Alarm
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

class AutoDevManager : EditorFactoryListener {

    private val alarm = Alarm()

    companion object {
        val currentMergeFix = java.util.WeakHashMap<Editor, GhostSanitizer.MergeResult>()
        val currentInlays = java.util.WeakHashMap<Editor, Inlay<*>>()
    }

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                // If they turned off the ghost, don't haunt them.
                if (!AppSettingsState.getInstance().enableGhostText) return

                resetSuggestion(editor)
                alarm.cancelAllRequests()
                if (event.newFragment.length > 100) return
                // 600ms debounce: enough time to sip coffee, not enough to lose focus.
                alarm.addRequest({ fetchSuggestion(editor) }, 600)
            }
        })
    }

    override fun editorReleased(event: EditorFactoryEvent) = resetSuggestion(event.editor)

    private fun fetchSuggestion(editor: Editor) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val settings = AppSettingsState.getInstance()

            // Double check: if they disabled it or forgot the key, abort mission.
            if (!settings.enableGhostText || settings.apiKey.isBlank()) return@executeOnPooledThread

            var fullContext = ""
            var currentLinePrefix = ""
            var suffix = ""

            val shouldProceed = ApplicationManager.getApplication().runReadAction<Boolean> {
                if (editor.isDisposed) return@runReadAction false
                val offset = editor.caretModel.offset
                val text = editor.document.text

                // 1. Context
                val start = (offset - 2500).coerceAtLeast(0)
                fullContext = text.substring(start, offset)

                // 2. Prefix (Trimmed line is safest for Sanitizer)
                val lineStart = editor.document.getLineStartOffset(editor.caretModel.logicalPosition.line)
                currentLinePrefix = text.substring(lineStart, offset).trim()

                // 3. Suffix
                val suffixEnd = (offset + 1000).coerceAtMost(text.length)
                suffix = text.substring(offset, suffixEnd)

                return@runReadAction true
            }

            if (!shouldProceed || fullContext.isBlank()) return@executeOnPooledThread

            try {
                val provider = when {
                    settings.apiKey.startsWith("sk-ant-") -> Provider.ANTHROPIC
                    settings.apiKey.startsWith("gsk_") -> Provider.GROQ
                    else -> Provider.OPENAI
                }

                // Log acts as my only debugger at this hour.
                println("[AutoDev] 🚀 Requesting completion from ${provider.name}...")

                val rawSuggestion = callAI(provider, settings.apiKey, settings.modelName, fullContext, suffix, settings.enableSecurityFocus)

                if (rawSuggestion.isNotBlank()) {
                    val fix = GhostSanitizer.sanitize(currentLinePrefix, suffix, rawSuggestion)

                    if (fix.textToInsert.isNotBlank()) {
                        println("[AutoDev] ✅ Ghost Text Ready: '${fix.textToInsert}'")
                        ApplicationManager.getApplication().invokeLater {
                            renderGhostText(editor, fix)
                        }
                    }
                }
            } catch (e: Exception) {
                println("[AutoDev] 💥 API Error: ${e.message}")
            }
        }
    }

    private fun renderGhostText(editor: Editor, fix: GhostSanitizer.MergeResult) {
        if (editor.isDisposed) return
        resetSuggestion(editor)
        currentMergeFix[editor] = fix
        val offset = editor.caretModel.offset
        val inlay = editor.inlayModel.addInlineElement(offset, GhostInlayRenderer(fix.textToInsert))
        if (inlay != null) currentInlays[editor] = inlay
    }

    fun resetSuggestion(editor: Editor) {
        currentInlays[editor]?.dispose()
        currentInlays.remove(editor)
        currentMergeFix.remove(editor)
    }

    enum class Provider { OPENAI, ANTHROPIC, GROQ }

    // Added 'securityMode' param here
    private fun callAI(provider: Provider, apiKey: String, model: String, prefix: String, suffix: String, securityMode: Boolean): String {

        // If security mode is on, we ask the AI to be extra paranoid.
        val securityInstruction = if (securityMode) "CRITICAL: PRIORITIZE SECURITY. No hardcoded secrets. No SQL injection." else ""

        val sysPrompt = """
            You are a low-latency code completion engine. $securityInstruction
            Complete the code at the [CURSOR] position.
            - Output ONLY the missing code. 
            - No markdown.
            - No repetition of code found in the SUFFIX.
            - Maintain indentation.
            - If user typed a shortcut (e.g. 'sysout'), expand it fully.
        """.trimIndent()

        val userContent = "PREFIX:\n$prefix\n\n[CURSOR]\n\nSUFFIX:\n$suffix"

        val urlStr = when (provider) {
            Provider.ANTHROPIC -> "https://api.anthropic.com/v1/messages"
            Provider.GROQ -> "https://api.groq.com/openai/v1/chat/completions"
            Provider.OPENAI -> "https://api.openai.com/v1/chat/completions"
        }

        val url = URI.create(urlStr).toURL()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 3000
        conn.readTimeout = 5000

        if (provider == Provider.ANTHROPIC) {
            conn.setRequestProperty("x-api-key", apiKey)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.setRequestProperty("content-type", "application/json")
        } else {
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
        }

        val jsonInput = if (provider == Provider.ANTHROPIC) {
            """
            {
                "model": "${model.ifBlank { "claude-3-5-sonnet-20240620" }}",
                "max_tokens": 128,
                "system": "${escapeJson(sysPrompt)}",
                "messages": [
                    {"role": "user", "content": "${escapeJson(userContent)}"}
                ],
                "temperature": 0.1
            }
            """.trimIndent()
        } else {
            """
            {
                "model": "${model.ifBlank { "gpt-4o" }}",
                "messages": [
                    {"role": "system", "content": "${escapeJson(sysPrompt)}"},
                    {"role": "user", "content": "${escapeJson(userContent)}"}
                ],
                "max_tokens": 128,
                "temperature": 0.1,
                "stop": ["SUFFIX"] 
            }
            """.trimIndent()
        }

        conn.outputStream.use { os -> os.write(jsonInput.toByteArray(StandardCharsets.UTF_8)) }

        if (conn.responseCode != 200) {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown"
            println("[AutoDev] API Error ${conn.responseCode}: $error")
            return ""
        }

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        return extractContent(response, provider)
    }

    private fun escapeJson(text: String) = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\t", "\\t")

    private fun extractContent(json: String, provider: Provider): String {
        val searchMarker = if (provider == Provider.ANTHROPIC) "\"text\": \"" else "\"content\": \""
        val start = json.indexOf(searchMarker)
        if (start == -1) return ""
        val actualStart = start + searchMarker.length

        val sb = StringBuilder()
        var i = actualStart
        var escaped = false

        while (i < json.length) {
            val c = json[i]
            if (escaped) {
                when (c) {
                    'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t'); '\\' -> sb.append('\\'); '"' -> sb.append('"'); else -> sb.append(c)
                }
                escaped = false
            } else {
                if (c == '\\') escaped = true else if (c == '"') break else sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}