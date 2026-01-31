# AI Auto-Dev (JetBrains Hackathon Submission)

A complete AI development suite for IntelliJ IDEA. It combines the "Ghost Text" experience of GitHub Copilot with a context-aware Chat Assistant and a One-Click Security Auditor—all powered by OpenAI's GPT-4o.

![Status](https://img.shields.io/badge/Status-Prototype-green) ![Platform](https://img.shields.io/badge/Platform-IntelliJ-blue)

## 🚀 Overview
**AI Auto-Dev** reduces developer friction by predicting your next move and securing your code. It works in two powerful modes:
1.  **Editor Mode:** Real-time ghost text completion as you type.
2.  **Assistant Mode:** A conversational sidebar to refactor code, fix bugs, and audit security vulnerabilities.

## ✨ Features

### 1. 👻 Ghost Text (Auto-Complete)
* **Real-Time Suggestions:** Analyzes your file context and suggests logical next steps.
* **Non-Intrusive:** Suggestions appear in gray.
* **Smart Triggers:** Press **`Tab`** to accept, keep typing to ignore.
* **Debounced:** Saves API credits by waiting for you to pause typing.

### 2. 💬 AI Chat Sidebar
* **Context-Aware:** The AI knows which file you have open and "reads" your code automatically.
* **Memory:** Keeps track of your conversation history so you can ask follow-up questions (e.g., *"Make that function thread-safe"*).
* **One-Click Apply:** Generate code in the chat and click **`✅ Apply`** to instantly inject it into your editor. No copy-pasting required!
* **Dynamic UI:** Resizable chat bubbles and professional code highlighting.

### 3. 🛡️ Security Auto-Audit
* **One-Click Scan:** detailed analysis of your current file against **OWASP Top 10** vulnerabilities.
* **Instant Report:** Finds SQL Injections, hardcoded secrets, and weak cryptography, offering immediate fixes.

## 🛠️ How It Works
1.  **Context Capture:** The plugin reads the currently active editor file (extension, language, and content).
2.  **GPT-4o Integration:** Sends optimized prompts to OpenAI with strict system instructions (e.g., "You are a Security Auditor").
3.  **Inlay & Tool Window:** Uses IntelliJ's native Inlay Model for ghost text and a custom Swing/GridBagLayout UI for the sidebar.

## 📦 Installation & Setup
This is a hackathon prototype. To run it locally, you must provide your own API key.

1.  **Clone this repository.**
2.  **Open in IntelliJ IDEA.**
3.  **Create the Config File:**
    * Navigate to `src/main/kotlin/com/hackathon/aihelper/`
    * Create a new file named **`PluginConfig.kt`**
    * Paste the following code (this file is ignored by Git to keep your key safe):
    ```kotlin
    package com.hackathon.aihelper

    object PluginConfig {
        const val API_KEY = "sk-YOUR_OPENAI_API_KEY_HERE"
    }
    ```
4.  **Run the Plugin:**
    * Open the Gradle tool window on the right.
    * Go to `Tasks` > `intellij` > `runIde`.

## 🎥 Usage
### Ghost Text
* **Start Typing:** Write code as usual.
* **Wait:** Pause for 600ms.
* **Press Tab:** Use the `Tab` key to accept the gray suggestion.

### AI Assistant Sidebar
* **Open Chat:** Click **"AI Assistant"** on the right sidebar or press a shortcut (if configured).
* **Ask Questions:** Type *"Explain this code"* or *"Refactor for readability"*.
* **Apply Code:** If the AI generates code, click the green **`✅ Apply`** button to update your file instantly.
* **Run Audit:** Click the **`🛡️ Audit`** button to scan for security flaws.

## 🏗️ Tech Stack
* **Language:** Kotlin
* **Framework:** IntelliJ Platform SDK (Swing UI)
* **AI Model:** OpenAI GPT-4o API

---
*Built for the JetBrains UOBD_GDG Hackathon 2026.*