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

package com.hackathon.aihelper

import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.awt.Rectangle

class GhostInlayRenderer(private val text: String) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val fontMetrics = editor.contentComponent.getFontMetrics(
            editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
        )

        val lines = text.split("\n")

        // Prevent "Positive width" crash if AI returns immediate newline
        if (lines.isEmpty()) return 1

        val firstLineWidth = fontMetrics.stringWidth(lines[0])
        return if (firstLineWidth < 1) 1 else firstLineWidth
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        val color = JBColor.GRAY
        val font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)

        g.color = color
        g.font = font

        val lines = text.split("\n")
        val lineHeight = editor.lineHeight

        // Calculate indentation X offset
        val lineStartOffset = editor.document.getLineStartOffset(editor.caretModel.logicalPosition.line)
        val indentX = editor.offsetToXY(lineStartOffset).x

        lines.forEachIndexed { index, line ->
            val x = if (index == 0) targetRegion.x else indentX
            val y = targetRegion.y + (index * lineHeight) + (targetRegion.height - 4)

            if (line.isNotEmpty()) {
                g.drawString(line, x, y)
            }
        }
    }
}