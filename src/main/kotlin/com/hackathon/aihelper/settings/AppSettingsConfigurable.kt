package com.hackathon.aihelper.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import org.jetbrains.annotations.Nls

/**
 * Connects the UI to the Data.
 * Essentially the middle-man that makes the "Apply" button work.
 */
class AppSettingsConfigurable : Configurable {

    private var settingsComponent: AppSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "AI Auto-Dev"
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return settingsComponent?.getPreferredFocusedComponent()
    }

    override fun createComponent(): JComponent? {
        settingsComponent = AppSettingsComponent()
        return settingsComponent?.panel
    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.getInstance()
        // If any of these don't match, the "Apply" button lights up.
        return settingsComponent?.apiKey != settings.apiKey ||
                settingsComponent?.modelName != settings.modelName ||
                settingsComponent?.isGhostTextEnabled != settings.enableGhostText ||
                settingsComponent?.isSecurityModeEnabled != settings.enableSecurityFocus
    }

    override fun apply() {
        val settings = AppSettingsState.getInstance()
        settings.apiKey = settingsComponent?.apiKey ?: ""
        settings.modelName = settingsComponent?.modelName ?: "gpt-4o"
        settings.enableGhostText = settingsComponent?.isGhostTextEnabled ?: true
        settings.enableSecurityFocus = settingsComponent?.isSecurityModeEnabled ?: false
    }

    override fun reset() {
        val settings = AppSettingsState.getInstance()
        // Revert to saved state if user hits Cancel or opens the menu
        settingsComponent?.apiKey = settings.apiKey
        settingsComponent?.modelName = settings.modelName
        settingsComponent?.isGhostTextEnabled = settings.enableGhostText
        settingsComponent?.isSecurityModeEnabled = settings.enableSecurityFocus
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}