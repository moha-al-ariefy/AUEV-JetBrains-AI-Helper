/*
 * Copyright 2026 moha-al-ariefy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hackathon.aihelper

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil

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
                var deletedPrefix = "" // I save this just in case the AI messes up and we need to roll back

                if (fix.charsToDelete > 0) {
                    val startDel = (currentOffset - fix.charsToDelete).coerceAtLeast(0)
                    deletedPrefix = document.getText(TextRange(startDel, currentOffset))
                    document.deleteString(startDel, currentOffset)
                    insertOffset = startDel // Update insertion point to where we deleted
                }

                // 3. EXECUTE INSERTION
                document.insertString(insertOffset, fix.textToInsert)

                // 4. THE SYNTAX VALIDATION LAYER (The Spaghetti Filter)
                // I added this to back up our "Strict Type Safety" marketing claim.
                // We commit the document to the PSI tree so IntelliJ can analyze the syntax.
                PsiDocumentManager.getInstance(project).commitDocument(document)
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
                var syntaxValid = true

                if (psiFile != null) {
                    val endOffset = insertOffset + fix.textToInsert.length

                    // I scan the tree for actual syntax errors in the newly injected block.
                    val errorElements = PsiTreeUtil.findChildrenOfType(psiFile, PsiErrorElement::class.java)

                    for (error in errorElements) {
                        // If the error overlaps with the AI's garbage, we flag it.
                        if (error.textRange.intersects(insertOffset, endOffset)) {
                            syntaxValid = false
                            break
                        }
                    }

                    if (!syntaxValid) {
                        // REJECT THE SPAGHETTI!
                        // I delete the garbage AI text and restore the user's original prefix.
                        document.deleteString(insertOffset, endOffset)
                        if (deletedPrefix.isNotEmpty()) {
                            document.insertString(insertOffset, deletedPrefix)
                        }

                        // I show a cool little popup so the user knows we saved their codebase
                        HintManager.getInstance().showErrorHint(editor, "🛡️ AUEV Blocked Invalid Syntax")

                        // I reset the suggestion and kill the rest of the execution
                        AutoDevManager.resetSuggestion(editor)
                        return@runWriteCommandAction
                    }

                    // 5. AUTO-FORMATTING (The "Premium" Polish)
                    // This fixes indentation automatically for the new code block, but ONLY if the syntax was valid.
                    try {
                        CodeStyleManager.getInstance(project).reformatText(psiFile, insertOffset, endOffset)
                    } catch (ignored: Exception) {
                        // Fail silently if PSI formatting fails (rare), code is still inserted
                    }
                }

                // 6. UPDATE CARET & SCROLL
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