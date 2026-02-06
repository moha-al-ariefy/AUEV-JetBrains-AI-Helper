# AUEV (AI Unified Editor Vision) - Beta 0.4

**AUEV** is a next-generation AI development suite for IntelliJ IDEA. It combines the "Ghost Text" experience of Copilot with a context-aware Chat Assistant and a One-Click Security Auditor.

Now featuring **Multi-Provider Support** (OpenAI, Anthropic, Groq) and a **Dual-Agent Sanitizer** for perfect code insertions.

![Status](https://img.shields.io/badge/Status-Beta_0.4-orange) ![Platform](https://img.shields.io/badge/Platform-IntelliJ-blue)

## 🚀 Key Features

### 1. 👻 Smart Ghost Text (Dual-Agent Engine)
* **Predicts Your Next Move:** Uses a "Writer" agent to generate code and a "Cleaner" agent to fix typos (e.g., `sysout` → `System.out.println`).
* **Smart Zipper:** Aggressively prevents code repetition and infinite loops.
* **Non-Intrusive:** Suggestions appear in gray. Press **`Tab`** to accept, **`Backspace`** to reject.
* **Undo Support:** Accepted code is auto-formatted and respects your Undo stack.

### 2. 🧠 Multi-Provider Intelligence
* **Bring Your Own Key:** The plugin automatically detects your API key type.
    * **OpenAI (`sk-...`):** Uses GPT-4o for high-quality logic.
    * **Anthropic (`sk-ant-...`):** Uses Claude 3.5 Sonnet for complex refactoring.
    * **Groq (`gsk_...`):** Uses Llama 3 / Mixtral for **insanely fast** (low-latency) completions.

### 3. 💬 Context-Aware Chat
* **Reads Your Mind:** The chat knows which file is open and includes it in the context automatically.
* **One-Click Apply:** Click **`✅ Apply`** on any code block to instantly inject it into your editor without copy-pasting.

### 4. 🛡️ Security Auto-Audit
* **One-Click Scan:** detailed analysis of your current file against **OWASP Top 10** vulnerabilities.
* **Instant Report:** Finds SQL Injections, hardcoded secrets, and weak cryptography, offering immediate fixes.

## 📦 Installation & Setup

1.  **Clone & Run:**
    * Clone this repository.
    * Open in IntelliJ IDEA.
    * Run the `runIde` Gradle task.

2.  **Configure API Key:**
    * Go to **Settings (Ctrl+Alt+S)** > **Tools** > **AI Auto-Dev**.
    * Paste your API Key (OpenAI, Anthropic, or Groq).
    * The plugin will **auto-detect** the provider and set the correct model.

## 🎥 Usage
### Ghost Text
* **Type Code:** Start typing (e.g., `fun main`).
* **Wait:** Pause for 600ms.
* **Accept:** Press **`Tab`** to insert the code.
* **Reject:** Press **`Backspace`** to clear the suggestion.

### AI Assistant Sidebar
* **Open Chat:** Click **"AI Assistant"** on the right sidebar.
* **Ask Questions:** *"Explain this class"* or *"Fix this bug"*.
* **Apply Fixes:** Click the `✅ Apply` button on generated code blocks.

## 🏗️ Tech Stack
* **Language:** Kotlin
* **UI:** IntelliJ Platform SDK (Swing, Inlays, ToolWindows)
* **AI Backend:** Universal Provider Logic (Supports OpenAI, Anthropic, Groq APIs)

---
*Built for the JetBrains UOBD_GDG Hackathon 2026.*