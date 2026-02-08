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

import com.hackathon.aihelper.settings.AppSettingsState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity // <--- The new Sheriff in town

// I changed this from StartupActivity to ProjectActivity because 2025 killed the old one
class PluginStartup : ProjectActivity {

    private val LOG = Logger.getInstance(PluginStartup::class.java)

    // This is now a 'suspend' function because we are living in the future
    override suspend fun execute(project: Project) {
        LOG.warn("🧨 [AutoDev] Plugin Starting up (2025 Edition)...")

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