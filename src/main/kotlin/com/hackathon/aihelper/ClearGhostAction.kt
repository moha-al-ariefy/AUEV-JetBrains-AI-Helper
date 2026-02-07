package com.hackathon.aihelper

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class ClearGhostAction : AnAction() {

    // I added this to keep the logs clean and green.
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        // I killed the constructor call here. Long live the Singleton.
        AutoDevManager.resetSuggestion(editor)
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        // CRITICAL: Only enable this action if we actually have a suggestion to clear.
        // This allows the standard "Backspace" behavior to work when there is no ghost text.
        val hasSuggestion = editor != null && AutoDevManager.currentMergeFix.containsKey(editor)

        e.presentation.isEnabled = hasSuggestion
        e.presentation.isVisible = hasSuggestion
    }
}