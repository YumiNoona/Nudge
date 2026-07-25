<p align="center">
  <img src="web/public/favicon.png" alt="Nudge" width="72" />
</p>

<p align="center">
  <strong>A local-first, privacy-first personal finance tracker.<br>Auto-detects expenses from SMS, turns budgeting into a game.</strong>
</p>

<p align="center">
  <a href="https://github.com/YumiNoona/Nudge/blob/main/LICENSE"><img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" alt="License" /></a>
  <a href="#"><img src="https://img.shields.io/badge/kotlin-1.9%2B-7C6FF0?style=flat-square&logo=kotlin" alt="Kotlin" /></a>
  <a href="#"><img src="https://img.shields.io/badge/typescript-5.3%2B-3178C6?style=flat-square&logo=typescript" alt="TypeScript" /></a>
  <a href="#"><img src="https://img.shields.io/badge/android-API%2026%2B-3DDC84?style=flat-square&logo=android" alt="Android" /></a>
  <a href="#"><img src="https://img.shields.io/badge/react-18-61DAFB?style=flat-square&logo=react" alt="React" /></a>
  <a href="#"><img src="https://img.shields.io/badge/platform-Android%20%7C%20Web-7C6FF0?style=flat-square" alt="Platform" /></a>
  <a href="#"><img src="https://img.shields.io/badge/status-active-F0574B?style=flat-square" alt="Status" /></a>
</p>

---

## What is Nudge

Nudge automatically detects expenses from bank SMS and UPI notifications — on-device, never uploaded. It requires almost zero manual entry and wraps budgeting in a warm, playful layer of XP, streaks, badges, and goals. **Your data stays yours.**

> **Privacy by default.** SMS parsing happens 100% on-device. No account required. Nothing leaves your phone.

---

## Features

| Capability | Detail |
|---|---|
| **Auto-capture** | Bank SMS + UPI notification parsing (50+ bank regex templates, IN/US/UK) |
| **Swipe-to-categorize** | Tinder-style card deck — swipe right to tag, left to skip |
| **Smart budgets** | Category budgets with rollover, animated progress rings, envelope mode |
| **Gamification** | 42 badges, XP/level system, weekly challenges, streak freezes |
| **Analytics** | Spend trends, category donut, cash-flow calendar heatmap, merchant leaderboard |
| **CSV import** | Drag-and-drop with auto-column detection + preview |
| **E2E sync** | Encrypted cross-device sync (server stores only unreadable blobs) |
| **Encryption** | AES-256 on-device DB + field-level IndexedDB encryption on Web |
| **Dark mode** | OLED-friendly near-black surfaces |
| **Anti-guilt design** | No shame-based messaging — everything framed as encouragement |

---

## Architecture

```
nudge/
├── shared/          Kotlin Multiplatform — models, engines, sync protocol
│   ├── model/       Transaction, Account, Category, Budget, Gamification
│   ├── engine/      Categorization, Budget math, SMS parser, Gamification math
│   ├── sync/        E2E sync protocol, merge engine
│   └── util/        Design tokens, ID generator, anti-guilt messages
│
├── android/         Android app — Jetpack Compose + Room
│   ├── data/        10 Room entities, 5 DAOs
│   ├── ui/          16 Compose screens, shared components, animations, themes
│   └── service/     SMS receiver, notification listener, sync worker
│
├── web/             Web dashboard — React + TypeScript + Tailwind + Dexie.js
│   ├── components/  14 screens + 8 shared UI components
│   ├── lib/         DB, crypto, categorization, sync, animations, seed data
│   └── styles/      CSS custom properties (semantic tokens)
│
└── server/          Sync relay — Node.js + Express + sql.js (WASM)
    └── src/         E2E encrypted blob storage (zero plaintext)
```

---

## Tech Stack

| Layer | Android | Web | Server |
|---|---|---|---|
| **UI** | Jetpack Compose (Material 3) | React 18 + Tailwind CSS | — |
| **DB** | Room (SQLite) | Dexie.js (IndexedDB) | sql.js (SQLite WASM) |
| **Shared logic** | Kotlin Multiplatform (KMP) | TypeScript (mirrored engines) | — |
| **Charts** | Canvas / Compose custom | Recharts | — |
| **Animation** | `animateXAsState` + Spring | Framer Motion | — |
| **Encryption** | EncryptedSharedPreferences | WebCrypto AES-GCM + PBKDF2 | — |
| **Sync** | WorkManager + AES-GCM | fetch + WebCrypto | Express REST |

---

## Quick Start

### Web Dashboard

```bash
cd web
npm install
npm run dev        # → http://localhost:3000
npm run build      # → web/dist/
```

### Android

Open `android/` in Android Studio, or:

```bash
cd android
./gradlew assembleDebug   # → android/build/outputs/apk/debug/
```

### Sync Relay Server

```bash
cd server
npm install
npm run dev        # → http://localhost:3741
```

Set `PORT` and `DB_PATH` env vars to customize.

---

## Design Principles

1. **Local-first, not local-only** — all data on-device, sync is optional and E2E encrypted
2. **Zero-friction capture** — SMS and notification parsing does the heavy lifting
3. **Privacy by default** — on-device processing, nothing uploaded, granular permissions
4. **Delight in every interaction** — haptics, micro-animations, spring physics
5. **One codebase philosophy** — shared business logic in KMP, mirrored in TypeScript

---

## License

MIT © Nudge contributors

Built with 💙 Made by Veil 
