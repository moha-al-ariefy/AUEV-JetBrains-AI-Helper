package com.hackathon.aihelper.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * This class saves your settings to disk.
 * It survives IDE restarts, unlike my will to live after 48 hours of coding.
 */
@State(
    name = "com.hackathon.aihelper.settings.AppSettingsState",
    storages = [Storage("AIHelperSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {

    // The keys to the kingdom. Don't commit these to GitHub, please.
    var apiKey: String = ""
    var modelName: String = "gpt-4o" // The default expensive one

    // --- Feature Flags (aka "Kill Switches") ---

    // Toggle this off if the ghost text starts suggesting "rm -rf /"
    var enableGhostText: Boolean = true

    // If true, the AI becomes a paranoid security officer (OWASP mode)
    var enableSecurityFocus: Boolean = false

    companion object {
        fun getInstance(): AppSettingsState {
            return ApplicationManager.getApplication().getService(AppSettingsState::class.java)
        }
    }

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}