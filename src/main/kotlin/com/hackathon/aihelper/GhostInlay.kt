package com.hackathon.aihelper

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Graphics
import java.awt.Rectangle

class GhostInlayRenderer(private val text: String) : EditorCustomElementRenderer {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val fontMetrics = inlay.editor.contentComponent.getFontMetrics(inlay.editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN))
        return fontMetrics.stringWidth(text)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        g.color = JBColor.GRAY // Make it look like "Ghost" text
        g.font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
        g.drawString(text, targetRegion.x, targetRegion.y + targetRegion.height - 4) // Align baseline
    }
}