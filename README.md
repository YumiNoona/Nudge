<p align="center">
  <img src="web/public/favicon.svg" alt="Nudge" width="80" />
</p>

<p align="center">
  <strong>A local-first, privacy-first personal finance tracker that turns budgeting into a game.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/platform-Android%20%7C%20Web-indigo?style=flat-square" alt="Platform" />
  <img src="https://img.shields.io/badge/kotlin-1.9%2B-purple?style=flat-square" alt="Kotlin" />
  <img src="https://img.shields.io/badge/typescript-5.3%2B-blue?style=flat-square" alt="TypeScript" />
  <img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" alt="License" />
  <img src="https://img.shields.io/badge/status-active-brightgreen?style=flat-square" alt="Status" />
</p>

---

Nudge automatically detects expenses from bank SMS and notifications, requires almost zero manual entry, and makes budgeting fun with XP, streaks, badges, and challenges — all wrapped in a tactile, animated UI.

> **Privacy by default.** SMS parsing happens 100% on-device. Nothing is uploaded. No account required. Your data lives where it belongs — with you.

---

## Features

### Capture
- **Auto-detect transactions** from bank SMS and UPI app notifications (Android)
- **Smart parsing engine** — 30+ regex templates across 20+ banks (IN/US/UK), merchant normalization, deduplication
- **Swipe-to-categorize** — Tinder-style card deck for pending reviews
- **Manual entry** with amount-first numeric pad and category grid
- **CSV import** with drag-and-drop, auto-column detection, and preview

### Organize
- **Custom categories & subcategories** with icons and colors
- **Tags** for cross-cutting labels (e.g. "trip:goa", "reimbursable")
- **Merchant aliases** — teach the app once, it remembers forever
- **Multi-account support** — bank accounts, credit cards, UPI, wallets, cash

### Gamification
- **XP & Levels** — earn XP for reviewing, logging, and staying under budget
- **42 achievement badges** — trophy-shelf grid with locked silhouettes and unlock animations
- **Weekly challenges** — auto-generated from your spending patterns
- **Savings goals** — 4 visual metaphors (growing plant, filling jar, building house, launching rocket)
- **Streak system** — daily check-in streaks with freeze tokens (no guilt — just encouragement)
- **Level-up celebrations** with confetti bursts and haptic feedback

### Budgets & Analytics
- **Category budgets** with rollover support
- **Envelope budgeting mode** — fill-level bars with animated progress
- **Spending trends** — area charts with month-over-month comparisons
- **Category breakdown** — donut charts with color-coded slices
- **Cash-flow calendar heatmap** — GitHub-contribution-style intensity grid
- **Merchant leaderboard** — your top 10 spending destinations

### Security & Privacy
- **Local-first** — all data stored on-device by default
- **SQLCipher** encrypted database on Android (AES-256)
- **WebCrypto** field-level encryption on Web (AES-256-GCM + PBKDF2)
- **Biometric/PIN app lock** on Android
- **E2E encrypted sync** — sync relay server stores only encrypted blobs it cannot read
- **Full data export/import** with encrypted backup support
- **Anti-dark-pattern guardrails** — no shame-based messaging, no fake urgency

### Design
- **Electric indigo accent** — not generic fintech blue
- **True OLED dark mode** — near-black `#0A0A0F` surfaces
- **Semantic design tokens** — 16-color categorical palette, type scale, spacing scale
- **Micro-animations** — card slide-ins, amount count-ups, budget ring interpolations, streak flame breathing
- **Haptic feedback** — semantic events (confirm, warning, error, celebration) mapped to Android rich haptics
- **Reduced motion** support for accessibility
- **Tabular figures** everywhere money is shown

---

## Architecture

```
nudge/
├── shared/          Kotlin Multiplatform — models, engines, sync protocol
│   ├── model/       Transaction, Account, Category, Budget, Gamification
│   ├── engine/      Categorization, Budget math, Recurring detection, SMS parser, Gamification math
│   ├── sync/        E2E sync protocol, merge engine
│   └── util/        Design tokens, ID generator, anti-guilt messages
│
├── android/         Android app — Jetpack Compose + Room + SQLCipher
│   ├── data/        10 Room entities, 5 DAOs, encrypted database
│   ├── ui/          15+ Compose screens, animations, themes
│   └── service/     SMS receiver, notification listener, sync worker
│
├── web/             Web dashboard — React + TypeScript + Tailwind + Dexie.js
│   ├── components/  14 screens (dashboard, analytics, needs-review, badges, etc.)
│   ├── lib/         DB, crypto, categorization, sync, animations, seed data
│   └── styles/      CSS custom properties (semantic tokens)
│
└── server/          Sync relay — Node.js + Express + sql.js
    └── src/         E2E encrypted blob storage (zero plaintext)
```

---

## Tech Stack

| Layer | Android | Web | Server |
|-------|---------|-----|--------|
| **UI** | Jetpack Compose (Material 3) | React + TypeScript + Tailwind | — |
| **DB** | Room + SQLCipher (AES-256) | Dexie.js (IndexedDB) | sql.js (SQLite WASM) |
| **Logic** | Kotlin (shared via KMP) | TypeScript (mirrored engines) | — |
| **Charts** | Canvas/Compose custom | Recharts | — |
| **Animation** | `animateXAsState` + Spring | Framer Motion | — |
| **Encryption** | Android Keystore + SQLCipher | WebCrypto AES-GCM + PBKDF2 | — |
| **Sync** | WorkManager + AES-GCM | fetch + WebCrypto | Express |

---

## Getting Started

### Web Dashboard

```bash
cd web
npm install
npm run dev        # → http://localhost:3000
npm run build      # production build → web/dist/
```

### Android App

Open `android/` in Android Studio. The project uses standard Gradle with Kotlin Multiplatform support.

```bash
cd android
./gradlew assembleDebug
```

### Sync Relay Server

```bash
cd server
npm install
npm run dev        # → http://localhost:3741
```

Environment variables:
- `PORT` — server port (default: `3741`)
- `DB_PATH` — SQLite database path (default: `./nudge-sync.db`)

---

## Design Principles

1. **Local-first, not local-only.** All data on-device by default. Sync is optional and E2E encrypted.
2. **Zero-friction capture.** SMS and notification parsing does the heavy lifting.
3. **Privacy by default.** On-device processing. Nothing uploaded. Granular permission explainers.
4. **Delight in every interaction.** Haptics, micro-animations, and motion are core feedback mechanisms.
5. **One codebase philosophy.** Shared business logic in KMP. TypeScript mirrors for web.

---

## License

MIT © Nudge contributors

---

<p align="center">
  <sub>Built with • care • and • a • lot • of • animations</sub>
</p>
