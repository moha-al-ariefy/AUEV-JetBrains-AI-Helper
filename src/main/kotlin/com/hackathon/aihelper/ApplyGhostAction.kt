package com.hackathon.aihelper

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.actionSystem.CommonDataKeys

class ApplyGhostAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val suggestion = AutoDevManager.currentSuggestion

        if (suggestion.isNotEmpty() && AutoDevManager.currentInlay != null) {
            val offset = editor.caretModel.offset

            // Insert the text permanently
            WriteCommandAction.runWriteCommandAction(e.project) {
                editor.document.insertString(offset, suggestion)
                editor.caretModel.moveToOffset(offset + suggestion.length)
            }
            // Clear the ghost
            AutoDevManager().resetSuggestion(editor)
        }
    }

    // Only enable "Tab" if there is a suggestion visible
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = AutoDevManager.currentSuggestion.isNotEmpty()
    }
}