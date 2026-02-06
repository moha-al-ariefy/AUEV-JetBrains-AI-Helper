package com.hackathon.aihelper.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.hackathon.aihelper.settings.AppSettingsState",
    storages = [Storage("AIHelperSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {

    var apiKey: String = ""
    var modelName: String = "gpt-4o" // Default

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