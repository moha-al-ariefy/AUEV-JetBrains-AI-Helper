# AUEV (AI Unified Editor Vision) - Beta 0.5

**AUEV** is a **Security-First** AI development suite for IntelliJ IDEA (2025.3+).

While other plugins just generate code, AUEV audits it. We combine the speed of "Ghost Text" with a **Paranoid Security Architecture** designed to stop vulnerabilities *before* you commit them.

**"Code fast, don't get hacked."**

![Status](https://img.shields.io/badge/Status-Beta_0.5-orange) ![Focus](https://img.shields.io/badge/Focus-Security_First-red) ![Platform](https://img.shields.io/badge/Platform-IntelliJ_2025.3-blue)

## 🛡️ The Security Pivot
We have shifted our core focus. AUEV is no longer just a coding assistant; it is a **guardian**.
* **Paranoid Mode:** A dedicated setting that injects strict OWASP security guidelines into every AI prompt.
* **Auto-Sanitization:** The "Ghost" engine actively strips potential secrets or malicious patterns from suggestions.
* **One-Click Audit:** Instantly grade your file against common vulnerabilities (SQLi, XSS, Weak Crypto).

---

## 🚀 Key Features

### 1. 👮 Security Auto-Audit (Primary Module)
* **Instant Risk Report:** Click **`🛡️ Audit Code`** in the sidebar to scan your open file.
* **OWASP Top 10 Focus:** detects hardcoded secrets, unchecked inputs, and dangerous SQL patterns.
* **Fix It For Me:** The audit doesn't just complain; it generates the patched code ready for one-click application.

### 2. 👻 Toggleable Smart Ghost Text
* **Dual-Agent Engine:** One agent writes the code, the second agent (The "Sanitizer") cleans it.
* **Kill Switch:** Ghost text can now be **toggled ON/OFF** in settings. If you're working on sensitive core logic, you can silence the ghost.
* **Smart Zipper:** Prevents the AI from repeating code you've already written.

### 3. 🧠 Multi-Provider Intelligence (BYOK)
* **Bring Your Own Key:** We don't act as a middleman. Your keys, your data.
    * **OpenAI (`sk-...`):** Best for logic and security auditing.
    * **Anthropic (`sk-ant-...`):** Superior for large-scale refactoring.
    * **Groq (`gsk_...`):** Ultra-low latency for instant Ghost Text.

### 4. 💬 Context-Aware Chat & UI Overhaul
* **New UI:** Clean message bubbles, native look-and-feel, and a "Setup" screen that alerts you if keys are missing.
* **One-Click Apply:** Found a fix in chat? Click **`✅ Apply`** to inject it directly into the editor. No copy-pasting required.

---

## 📦 Installation & Setup

1.  **Install:**
    * Clone repo -> Run `gradlew buildPlugin` -> Install the ZIP from `build/distributions`.
    * *Or use the pre-built Beta release.*

2.  **Configure (The Security Check):**
    * Go to **Settings (Ctrl+Alt+S)** > **Tools** > **AUEV**.
    * **API Key:** Paste your key (Auto-detected).
    * **Paranoid Security Mode:** [Check] (Recommended for production code).
    * **Enable Ghost Text:** [Check/Uncheck] depending on your anxiety levels.

## 🎥 Usage Flow

1.  **Draft:** Start typing. The **Ghost Text** suggests the line.
2.  **Refine:** Press **`Tab`** to accept.
3.  **Secure:** Click **`🛡️ Audit`** in the sidebar to ensure you didn't just introduce a vulnerability.
4.  **Fix:** If the audit screams at you, click **`✅ Apply`** on the fix.

## 🏗️ Tech Stack
* **Language:** Kotlin (JVM 21)
* **Platform:** IntelliJ SDK 2025.3.2
* **Architecture:** Event-Driven (EditorFactoryListener) with Asynchronous AI Execution.

---
*Built for the JetBrains UOBD_GDG Hackathon 2026. Powered by caffeine, anxiety, and a healthy fear of CVEs.*