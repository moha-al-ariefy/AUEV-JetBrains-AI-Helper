# Changelog

All notable changes to the **AUEV** project will be documented in this file.

## [Beta 0.5b] - 2026-02-08

### 🚀 New Features (The "I Did It" Update)
* **Jetpack Compose UI:** Completely rewrote the Tool Window using **Compose for Desktop**.
  * **Modern Look:** Matte dark theme, flat buttons, and rounded message bubbles.
  * **Dynamic Layout:**  chat and input area now resize perfectly.
  * **Settings Panel:** Integrated settings directly into the tool window (no more modal popups).
* **Model Upgrade:** Updated Groq configuration to use **Llama 3.3 (70b Versatile)** by default.

### 🐛 Critical Bug Fixes
* **The "Ghost" Fix:** Refactored `AutoDevManager` from a `class` to a `singleton object`.
  * *Fixed:* The plugin no longer ignores files that were already open on startup.
  * *Fixed:* Listeners now attach/detach correctly when toggling the feature.
* **Enter Key:** Fixed the input field ignoring the `Enter` key. It now sends the message correctly (Shift+Enter for new line).

---

## [Beta 0.5] - 2026-02-07

### 🚀 New Features
* **UI Refresh (Legacy Swing):**
  * Redesigned the **Chat Tool Window** with proper message bubbles.
  * Added **"One-Click Apply"** buttons to code blocks.
  * Added a "Setup Required" screen for missing API keys.
* **Feature Flags:** Added the ability to toggle **Ghost Text** on/off in settings.

### 🛠 Improvements
* **Build System:** Fully migrated to **IntelliJ Platform 2025.3.2**.
* **Native Settings:** Moved configuration to `Settings > Tools > AUEV`.
* **Build Stability:** Disabled `buildSearchableOptions` to fix compilation crashes.

### 🐛 Bug Fixes
* Fixed `ClassNotFoundException` in `plugin.xml`.
* Fixed the "Zombie Plugin" issue where settings wouldn't load.
* Resolved build artifact issues.

---

## [Beta 0.4] - 2026-02-07

### 🚀 New Features
* **Multi-Provider Support:** Added native support for **Anthropic (Claude 3.5)** and **Groq (Llama 3)**.
* **Smart Ghost Text:**
  * **Dual Agent System:** Separated Writer and Sanitizer logic.
  * **Zipper Algorithm:** Prevents the AI from repeating existing code.
  * **Auto-Expansion:** `sysout` -> `System.out.println`.

### 🛠 Improvements
* **UX:** Mapped `Backspace` to reject ghost text.
* **UX:** Mapped `Tab` to accept ghost text.
* **Performance:** Optimized API calls with token checks.

### 🐛 Bug Fixes
* Fixed duplicate code generation.
* Fixed "silent failure" on missing API keys.