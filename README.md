# JARVIS — Next-Generation Personal AI Assistant for Android

A premium-quality, futuristic personal AI assistant application built with **Jetpack Compose**, **Material 3**, and a signature **Warm Sunset Glassmorphism** design aesthetic.

---

## 🌟 Visual Identity & Design System

- **Design Language**: Soft futuristic glassmorphism combined with a warm sunset aesthetic.
- **Color Palette**:
  - Primary Background: `#FFF0E9`
  - Secondary Background: `#FCEAE5`
  - Card Background: `#FFF9F6`
  - Primary Text: `#32182B`
  - Secondary Text: `#98758D`
  - Accent Coral: `#F16D43`
  - Accent Purple: `#A66C91`
  - Accent Blue: `#8295AD`
  - Success Green: `#28AE86`
  - Border: `#D9C7C4`
- **Cards**: Large rounded corners (26–32dp), thin translucent borders, soft elevation shadows.
- **Centerpiece 3D AI Orb**: Dynamic particle sphere projected on 3D coordinate space with real-time state reactions (Offline, Idle, Listening, Thinking, Processing, Speaking, Executing Task, Error).

---

## 🚀 Key Features

### 1. Dashboard & AI Orb
- Dynamic temporal greeting (`Good morning, Master`, `Good afternoon, Master`, `Good evening, Master`).
- Interactive 3D particle sphere with mathematical perspective scaling and counter-rotating orbital rings.
- Power control button with live status indicator (`JARVIS OFF • TAP TO START` ➔ `JARVIS ONLINE • READY`).
- Live atmospheric weather card powered by real-time Open-Meteo REST API with cached telemetry and refresh animation.
- Quick Access Cards: **Screen** (Screen sharing / visual inspection), **Memories** (Recall Core), **Language** (Hinglish/English/Hindi/Auto).

### 2. Full Conversation Engine (Chat)
- Spacious message interface with glassmorphic bubbles and timestamps.
- Mode Selectors: **Jarvis Mode** (daily automation) & **Code Mode** (programming & algorithms).
- Streaming response animations and thinking indicator.
- Action dropdowns: Copy, Share, Speak Aloud (TTS), Regenerate, Delete.
- Multiline floating composer with circular coral send button, attachment sheet, and voice input.

### 3. Persistent Floating Navigation
- Translucent pill bottom bar with 5 working destinations:
  1. **Home**
  2. **Chat**
  3. **Central Microphone**: Prominent circular button with grey-blue border and active audio pulsation.
  4. **Settings**
  5. **Updates**

### 4. Settings & Intelligence Core
- **Master Profile Card**: Online status, role (`JARVIS OPERATOR • PRIMARY USER`), Voice Enrolled badge, JARVIS PRO badge, interactive profile editor.
- **Voice & Neural Intelligence**: Owner Voice Biometrics (spectral footprint test & threshold tuning), Language selector, AI Persona & Voice configuration (with live TTS speech sample preview), Silent Execution mode toggle.
- **System & Data Engine**: Gemini Live API configuration with model selection and WebSocket latency testing, Neural Memory Bank (search, add, edit, delete, JSON backup/restore).
- **System Permissions**: Real-time permission checker auditing 12 Android permissions with direct settings deep-links.
- **Overlay Avatars**: Floating assistant window service (`JarvisOverlayService`) with double-tap HUD, drag gesture, and dock edge snapping.
- **App Control & Hands-Free Automation**: WhatsApp contact alias resolution and message dispatch, YouTube video search, Spotify audio playback, permission-aware phone calls, and battery/network telemetry.
- **Appearance & Security**: Theme presets (Sunset, Dark, Light, AMOLED), Privacy & Data Shield (TLS status, memory export, history wipe), App Identity & Integrity (SHA-256 cert fingerprint).

### 5. Task Execution Interface (Task HUD)
- Autonomous 4-step progress line: `Preparing` ➔ `Observing` ➔ `Executing` ➔ `Verifying`.
- Verification Alert: Halts completion until accessibility confirmation is verified.

### 6. Updates & Announcements Feed
- Scrollable release feed featuring all 12 system update cards with category tags, bold headers, and detailed telemetry notes.

---

## 🛠️ Technology Stack

- **Platform**: Native Android (API 26+)
- **Language**: Kotlin 1.9.23
- **UI Toolkit**: Jetpack Compose (BOM 2024.04.01), Material 3
- **Architecture**: MVVM + Clean Architecture
- **Concurrency**: Kotlin Coroutines & StateFlow
- **Storage**: Jetpack DataStore Preferences & JSON Persistence
- **Networking**: OkHttp 4.12.0
- **Audio & Voice**: Android TextToSpeech & Synthesized Harmonic Earcon AudioTrack
- **Build System**: Gradle 8.7 & AGP 8.3.2

---

## 📦 Automated GitHub Actions CI/CD

Automatic debug APK compilation and GitHub Release publishing via `.github/workflows/android-apk-build.yml`.
Artifacts are automatically uploaded to GitHub Actions and downloadable on device.
