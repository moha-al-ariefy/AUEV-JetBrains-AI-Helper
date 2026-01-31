package com.hackathon.aihelper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.util.Alarm
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class AutoDevManager : EditorFactoryListener {


    private val API_KEY = PluginConfig.API_KEY

    private val alarm = Alarm()

    companion object {
        var currentInlay: Inlay<*>? = null
        var currentSuggestion: String = ""
    }

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                // 1. ALWAYS clear suggestion on any change (Typing or Backspace)
                resetSuggestion(editor)
                alarm.cancelAllRequests()

                // 2. BACKSPACE LOGIC:
                // If text length decreased (old > new) or new length is 0, it's a deletion.
                // We return immediately so we DO NOT ask AI after a backspace.
                if (event.oldLength > event.newLength || event.newFragment.isEmpty()) {
                    return
                }

                // 3. Ignore large pastes (more than 20 chars)
                if (event.newFragment.length > 20) return

                // 4. Debounce: Wait 600ms before asking AI to save credits/speed
                alarm.addRequest({ fetchSuggestion(editor) }, 600)
            }
        })
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        resetSuggestion(event.editor)
    }

    private fun fetchSuggestion(editor: Editor) {
        ApplicationManager.getApplication().executeOnPooledThread {
            var textContext = ""
            var fileExtension = "java"

            // 1. Safe Read Action (Prevents "Read Access" Crashes)
            val shouldProceed = ApplicationManager.getApplication().runReadAction<Boolean> {
                if (editor.isDisposed) return@runReadAction false

                val file = editor.virtualFile
                if (file != null) fileExtension = file.extension ?: "txt"

                val offset = editor.caretModel.offset
                // Capture last 1000 chars for context
                textContext = editor.document.text.substring(0, offset).takeLast(1000)
                return@runReadAction true
            }

            if (!shouldProceed || textContext.isBlank()) return@executeOnPooledThread

            // Optimization: Don't ask AI if we are in the middle of a word (e.g. typing "Syst")
            // This prevents spamming the API while you are still typing a keyword.
            if (!textContext.last().isWhitespace() && !textContext.last().isLetterOrDigit() && textContext.last() != '.') {
                // You can uncomment this if you want strict "end of word" triggering only
                // return@executeOnPooledThread
            }

            println("DEBUG: Asking AI ($fileExtension)...")

            try {
                val suggestion = callOpenAI(textContext, fileExtension)
                println("DEBUG: Raw AI: '$suggestion'")

                val cleanSuggestion = cleanAIResponse(suggestion, textContext)

                if (cleanSuggestion.isNotEmpty()) {
                    // 2. Render on UI Thread
                    ApplicationManager.getApplication().invokeLater {
                        renderGhostText(editor, cleanSuggestion)
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: ERROR! ${e.message}")
            }
        }
    }

    private fun renderGhostText(editor: Editor, text: String) {
        if (editor.isDisposed) return
        resetSuggestion(editor)

        currentSuggestion = text
        val offset = editor.caretModel.offset

        // Draw the gray text using our Renderer class
        currentInlay = editor.inlayModel.addInlineElement(offset, GhostInlayRenderer(text))
    }

    fun resetSuggestion(editor: Editor) {
        currentInlay?.dispose()
        currentInlay = null
        currentSuggestion = ""
    }

    // --- SMART CLEANING (Fixes the "System.out" duplication) ---
    private fun cleanAIResponse(response: String, originalContext: String): String {
        var clean = response.trimEnd()

        // 1. Remove Markdown trash
        clean = clean.replace("```java", "").replace("```", "").replace("`", "").trim()

        // 2. Remove "Chatty" prefixes
        if (clean.startsWith("It seems") || clean.startsWith("Here is")) return ""

        // 3. Smart Overlap Removal:
        // If the user typed "System.out", and AI returned "System.out.println", we want only ".println"
        val contextTail = originalContext.takeLast(30)
        for (i in contextTail.indices) {
            val suffix = contextTail.substring(i)
            if (clean.startsWith(suffix)) {
                clean = clean.substring(suffix.length)
                break
            }
        }

        return clean
    }

    private fun callOpenAI(contextCode: String, language: String): String {
        val url = URL("https://api.openai.com/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $API_KEY")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        // PROMPT UPGRADE: Strict JSON mode instructions
        val systemPrompt = "You are a $language code completion engine. Return ONLY the remaining code for the current line. Do NOT start with markdown. Do NOT repeat the user's existing code. If the code is complete, return empty string."

        val jsonInput = """
            {
                "model": "gpt-4o", 
                "messages": [
                    {"role": "system", "content": "$systemPrompt"},
                    {"role": "user", "content": "${escapeJson(contextCode)}"}
                ], 
                "max_tokens": 60, 
                "temperature": 0.1,
                "stop": ["\n"]
            }
        """.trimIndent()

        conn.outputStream.use { os -> os.write(jsonInput.toByteArray(StandardCharsets.UTF_8)) }

        val code = conn.responseCode
        if (code != 200) throw RuntimeException("API Error $code")

        val response = conn.inputStream.bufferedReader().use { it.readText() }
        return extractContent(response)
    }

    private fun escapeJson(text: String) = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

    private fun extractContent(json: String): String {
        val startMarker = "\"content\": \""
        val start = json.indexOf(startMarker)
        if (start == -1) return ""
        val actualStart = start + startMarker.length

        var i = actualStart
        while (i < json.length) {
            if (json[i] == '"' && json[i-1] != '\\') break
            i++
        }
        return json.substring(actualStart, i).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").replace("\\t", "\t")
    }
}