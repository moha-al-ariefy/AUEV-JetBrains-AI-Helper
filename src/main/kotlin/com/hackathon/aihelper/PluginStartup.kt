package com.hackathon.aihelper

import com.hackathon.aihelper.settings.AppSettingsState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class PluginStartup : StartupActivity {

    private val LOG = Logger.getInstance(PluginStartup::class.java)

    override fun runActivity(project: Project) {
        LOG.warn("🧨 [AutoDev] Plugin Starting up...")

        val settings = AppSettingsState.getInstance()

        // I check the settings file. If the ghost is allowed, I unleash it.
        if (settings.enableGhostText) {
            LOG.warn("👻 [AutoDev] Ghost Mode is ENABLED. Starting Manager...")
            AutoDevManager.start()
        } else {
            LOG.warn("💤 [AutoDev] Ghost Mode is DISABLED. Manager staying asleep.")
        }
    }
}