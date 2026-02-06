package com.hackathon.aihelper.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import org.jetbrains.annotations.Nls

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
        return settingsComponent?.apiKey != settings.apiKey ||
                settingsComponent?.modelName != settings.modelName
    }

    override fun apply() {
        val settings = AppSettingsState.getInstance()
        settings.apiKey = settingsComponent?.apiKey ?: ""
        settings.modelName = settingsComponent?.modelName ?: "gpt-4o"
    }

    override fun reset() {
        val settings = AppSettingsState.getInstance()
        settingsComponent?.apiKey = settings.apiKey
        settingsComponent?.modelName = settings.modelName
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}