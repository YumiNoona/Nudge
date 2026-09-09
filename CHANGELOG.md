# Changelog

## 4.9.1 — Sharper receipt review

- Fix **Add as one expense** from the Android share-sheet receipt review by removing a conflicting import action layer.
- Show save failures directly in the receipt review instead of leaving the action unresponsive.
- Improve the receipt summary with private-processing, confidence, and detected-item indicators.
- Scale the transaction date picker down further in both width and height.

## 4.9.0 — Bring your history with you

- Import transaction history from common Money Manager, Wallet, Spendee, Monefy-style, and Bluecoins CSV layouts.
- Accept comma-, tab-, and semicolon-separated exports with flexible dates, signed amounts, income/expense columns, and quoted notes.
- Appear in Android's share sheet as **Add to Nudge** for supported text, images, PDFs, and tabular files.
- Scan shared content on-device with a focused animation, identify its source, and show a clear preview before anything is saved.
- Open shared receipt images in the full receipt editor with merchant, printed total, subtotal, taxes, fees, line items, account, category, and itemized-save controls.
- Let the user explicitly choose **Not now** or **Add** while retaining existing duplicate protection and account selection.
- Use a more compact transaction calendar that leaves more of the surrounding entry screen visible.

## 4.8.0 — Smarter capture, richer imports, calmer review

Nudge 4.8.0 is a substantial local-first upgrade focused on getting transactions into the app accurately and making them easier to review.

### Highlights

- Smarter bank, card, wallet, refund, and UPI classification with improved merchant normalization and duplicate protection.
- Multi-page receipt scanning with on-device OCR, item extraction, GST and printed-total reconciliation, and one-expense or itemized saving.
- Local PDF, CSV, text, and image statement imports with a review step before anything is saved.
- Shared expenses with equal, exact, or percentage splits, flexible payer selection, balances, and settlement.
- Weekly, monthly, and yearly recurring entries, plus historical dates for manual transactions.
- Clearer onboarding, safer deletion, improved backups, refined transaction entry, and broader category icon support.
- Separate Google Play and GitHub distributions, with secure signed APK updates for GitHub users.
- A new responsive public landing page and privacy-policy route, isolated from the Android build and ready for Vercel.

### Privacy

Nudge remains account-free and local-first. Financial parsing, receipt recognition, categorization, and learning happen on the device. Automatic SMS, notification, camera, and reminder capabilities remain optional.

### Installation

Download `Nudge-github-v4.8.0.apk` from the GitHub release assets. Android 8.0 or newer is required. Existing release-signed GitHub installations can update in place; users of older debug builds or the separate Google Play edition should export their data before switching editions.
