# AUEV (AI Unified Editor Vision) - Beta 0.5b

**AUEV** is a **Security-First** AI development suite for IntelliJ IDEA (2025.3+).

While other plugins just generate code, AUEV audits it. We combine the speed of "Ghost Text" with a **Paranoid Security Architecture** designed to stop vulnerabilities *before* you commit them.

**"Code fast, don't get hacked."**

![Status](https://img.shields.io/badge/Status-Beta_0.5b-orange) ![Focus](https://img.shields.io/badge/Focus-Security_First-red) ![License](https://img.shields.io/badge/License-Apache_2.0-green)

## The Security Pivot
We have shifted our core focus. AUEV is no longer just a coding assistant; it is a **guardian**.
* **Paranoid Mode:** A dedicated setting that injects strict OWASP security guidelines into every AI prompt.
* **Auto-Sanitization:** The "Ghost" engine actively strips potential secrets or malicious patterns from suggestions.
* **One-Click Audit:** Instantly grade your file against common vulnerabilities (SQLi, XSS, Weak Crypto).

---

## Key Features

### 1. Security Auto-Audit
* **Instant Risk Report:** Click **Audit** in the sidebar to scan your open file.
* **OWASP Top 10 Focus:** Detects hardcoded secrets, unchecked inputs, and dangerous SQL patterns.
* **Fix It For Me:** The audit generates patched code ready for one-click application.

### 2. Toggleable Smart Ghost Text
* **Dual-Agent Engine:** One agent writes the code, the second agent (The "Sanitizer") cleans it.
* **Kill Switch:** Ghost text can be **toggled ON/OFF** in settings. If you're working on sensitive core logic, you can silence the ghost.
* **Smart Zipper:** Prevents the AI from repeating code you've already written.

### 3. Multi-Provider Intelligence (BYOK)
* **Bring Your Own Key:** We don't act as a middleman. Your keys, your data.
    * **OpenAI:** Best for logic and security auditing.
    * **Anthropic:** Superior for large-scale refactoring.
    * **Groq (Llama 3.3):** Ultra-low latency for instant Ghost Text.

### 4. Modern Native UI
* **Jetpack Compose:** Completely rewritten UI for a seamless, modern desktop experience.
* **Context-Aware:** The chat knows your current file and context.
* **One-Click Apply:** Found a fix in chat? Click **Insert Code** to inject it directly into the editor. No copy-pasting required.

---

## Installation & Setup

1.  **Install**
    * Clone repo -> Run `gradlew buildPlugin` -> Install the ZIP from `build/distributions`.

2.  **Configure**
    * Open the **AUEV** Tool Window.
    * Click the **Settings (⚙)** icon in the top header.
    * **API Key:** Paste your key (Auto-detected).
    * **Paranoid Mode:** Enable for strict security checks.
    * **Ghost Text:** Enable/Disable based on your preference.

## Tech Stack
* **Language:** Kotlin (JVM 17)
* **UI Framework:** Jetpack Compose for Desktop
* **Platform:** IntelliJ SDK 2025.3.2
* **Architecture:** Event-Driven (EditorFactoryListener) with Asynchronous AI Execution.

## License
This project is licensed under the **Apache License 2.0**. See the `LICENSE` file for details.

---
*Built for the JetBrains UOBD_GDG Hackathon 2026.*