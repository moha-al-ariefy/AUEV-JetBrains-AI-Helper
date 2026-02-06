# Changelog

All notable changes to the **AUEV** project will be documented in this file.

## [Beta 0.5] - 2026-02-07

### 🚀 New Features
* **Total UI Overhaul:**
  * Redesigned the **Chat Tool Window** with proper message bubbles (Blue for you, Gray for the ghost).
  * Added **"One-Click Apply"** buttons to code blocks in chat. No more copy-pasting like a caveman.
  * Added a "Setup Required" screen that politely screams at you if your API key is missing.
* **Security Mode:** Added a **"Paranoid Security Mode"** toggle in settings. When enabled, the AI prompts are injected with strict OWASP security guidelines (no hardcoded secrets, no SQL injection).
* **Feature Flags:** Added the ability to toggle **Ghost Text** on/off in settings, in case the AI gets too enthusiastic.

### 🛠 Improvements
* **Build System:** fully migrated to **IntelliJ Platform 2025.3.2**. We are now compiling for the future.
* **Native Settings:** Moved configuration from the side panel to the official IDE Settings (`Settings > Tools > AUEV`).
* **Developer Experience:** Added internal comments and UI tooltips that reflect the true mental state of a developer at 3 AM.
* **Build Stability:** Disabled `buildSearchableOptions` to prevent the headless IDE from crashing on "Kubernetes" errors during compilation.

### 🐛 Bug Fixes
* Fixed `ClassNotFoundException` caused by incorrect package paths in `plugin.xml`.
* Fixed the "Zombie Plugin" issue where the settings panel wouldn't load after a fresh install.
* Resolved build artifacts missing from the `distributions` folder by cleaning the gradle cache.

---

## [Beta 0.4] - 2026-02-07

### 🚀 New Features
* **Multi-Provider Support:** Added native support for **Anthropic (Claude 3.5)** and **Groq (Llama 3)**. The engine now auto-detects the provider based on the API key prefix (`sk-ant-`, `gsk_`).
* **Settings UI:** Replaced hardcoded `PluginConfig.kt` with a native IntelliJ Settings page.
* **Smart Ghost Text:**
  * Added **Dual Agent System** (Writer + Sanitizer) to handle typos and shortcuts.
  * Implemented `sysout` -> `System.out.println` expansion logic.
  * Added **"Zipper" Algorithm** to prevent the AI from repeating code that already exists in the file.

### 🛠 Improvements
* **UX:** Mapped `Backspace` to reject/clear ghost text.
* **UX:** Mapped `Tab` to accept ghost text with auto-formatting.
* **Performance:** Optimized API calls with a "Last Token" check to reduce token usage.
* **Stability:** Fixed `URI` deprecation warnings and improved thread safety in `AutoDevManager`.

### 🐛 Bug Fixes
* Fixed an issue where the AI would delete the user's line before completing it.
* Fixed duplicate code generation when typing inside existing blocks.
* Fixed "silent failure" when API keys were missing (added robust error logging).