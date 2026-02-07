package com.hackathon.aihelper

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.codeStyle.CodeStyleManager

class ApplyGhostAction : AnAction() {

    // I added this so IntelliJ stops screaming "SEVERE" in the logs.
    // We need to run on the EDT (UI thread) because we are looking at the Editor object.
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return

        // 1. Get instructions (Directly from the Singleton now!)
        val fix = AutoDevManager.currentMergeFix[editor] ?: return

        if (fix.textToInsert.isNotEmpty()) {
            // "Accept AI Suggestion" = The name you see in the Edit -> Undo menu
            WriteCommandAction.runWriteCommandAction(project, "Accept AI Suggestion", "AI", {
                val document = editor.document
                val caretModel = editor.caretModel
                val currentOffset = caretModel.offset

                // 2. EXECUTE DELETION (The "sysout" Fix)
                var insertOffset = currentOffset
                if (fix.charsToDelete > 0) {
                    val startDel = (currentOffset - fix.charsToDelete).coerceAtLeast(0)
                    document.deleteString(startDel, currentOffset)
                    insertOffset = startDel // Update insertion point to where we deleted
                }

                // 3. EXECUTE INSERTION
                document.insertString(insertOffset, fix.textToInsert)

                // 4. AUTO-FORMATTING (The "Premium" Polish)
                // This fixes indentation automatically for the new code block
                PsiDocumentManager.getInstance(project).commitDocument(document)
                try {
                    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
                    if (psiFile != null) {
                        val endOffset = insertOffset + fix.textToInsert.length
                        CodeStyleManager.getInstance(project).reformatText(psiFile, insertOffset, endOffset)
                    }
                } catch (ignored: Exception) {
                    // Fail silently if PSI parsing fails (rare), code is still inserted
                }

                // 5. UPDATE CARET & SCROLL
                // Recalculate end offset in case formatting shifted things slightly
                val finalOffset = (insertOffset + fix.textToInsert.length).coerceAtMost(document.textLength)
                caretModel.moveToOffset(finalOffset)
                editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
            })

            // I reset the suggestion so it doesn't get stuck
            AutoDevManager.resetSuggestion(editor)
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        // I fixed the reference here too
        e.presentation.isEnabled = editor != null && AutoDevManager.currentMergeFix.containsKey(editor)
    }
}