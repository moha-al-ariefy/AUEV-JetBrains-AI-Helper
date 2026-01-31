# AI Auto-Dev (JetBrains Hackathon Submission)

A lightweight, intelligent code completion plugin for IntelliJ IDEA that brings the "Ghost Text" experience of GitHub Copilot to your IDE, powered by OpenAI's GPT-4o.

![Status](https://img.shields.io/badge/Status-Prototype-green) ![Platform](https://img.shields.io/badge/Platform-IntelliJ-blue)

## 🚀 Overview
**AI Auto-Dev** reduces developer friction by predicting your next move. Unlike standard autocomplete, it understands the full context of your file and suggests entire lines or blocks of logic in real-time.

It features a **"Ghost Text" UX**: suggestions appear in gray as you type, and you can simply press `Tab` to accept them.

## ✨ Features
* **Real-Time Suggestions:** As you type, the AI analyzes your code context and suggests the logical next step.
* **Ghost Text UX:** Non-intrusive gray text that doesn't break your flow.
* **Smart Triggers:**
    * **Accept:** Press `Tab` to insert the code.
    * **Dismiss:** Press `Backspace` or keep typing to ignore.
* **Language Aware:** Automatically detects if you are writing Java, Python, Kotlin, etc., and adjusts the prompt accordingly.
* **Anti-Hallucination:** Strictly engineered prompts to prevent markdown formatting or "chatty" responses—just pure code.

## 🛠️ How It Works
1.  **Context Capture:** The plugin reads the last 1000 characters of your current file.
2.  **Smart Debounce:** It waits for you to stop typing for 600ms before triggering (saving API credits).
3.  **GPT-4o Integration:** Sends the context to OpenAI with a strict "Code Completion" system prompt.
4.  **Inlay Rendering:** Draws the suggestion directly into the editor canvas using IntelliJ's Inlay Model.

## 📦 Installation & Setup
This is a hackathon prototype. To run it locally:

1.  Clone this repository.
2.  Open in **IntelliJ IDEA**.
3.  Open `src/main/kotlin/com/hackathon/aihelper/AutoDevManager.kt`.
4.  Replace the `API_KEY` variable with your own OpenAI API Key:
    ```kotlin
    private val API_KEY = "sk-..."
    ```
5.  Run the **`runIde`** Gradle task.

## 🎥 Usage
* **Start Typing:** Just write code as usual.
* **Wait:** Pause for a split second.
* **See Ghost:** The suggestion appears in gray.
* **Tab:** Press Tab to lock it in.

## 🏗️ Tech Stack
* **Language:** Kotlin
* **Framework:** IntelliJ Platform SDK
* **AI Model:** OpenAI GPT-4o API

---
*Built for the JetBrains UOBD_GDG Hackathon 2026.*