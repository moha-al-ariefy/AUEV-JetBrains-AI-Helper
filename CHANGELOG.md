# Changelog

All notable changes to the **AUEV** project will be documented in this file.

## [Beta 0.4] - 2026-02-07

### 🚀 New Features
* **Multi-Provider Support:** Added native support for **Anthropic (Claude 3.5)** and **Groq (Llama 3)**. The engine now auto-detects the provider based on the API key prefix (`sk-ant-`, `gsk_`).
* **Settings UI:** Replaced hardcoded `PluginConfig.kt` with a native IntelliJ Settings page (**Settings > Tools > AI Auto-Dev**).
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