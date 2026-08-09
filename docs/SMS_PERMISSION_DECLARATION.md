# SMS permission declaration

Nudge requests `READ_SMS` and `RECEIVE_SMS` because SMS-based money management is the app's central user-facing purpose. Google Play lists **SMS-based money management** as an eligible exception to the restricted SMS permissions policy, subject to review.

## Suggested declaration text

> Nudge is a local-first personal expense manager. With the user's explicit opt-in, it reads bank, card and UPI transaction SMS messages to identify debits, credits and refunds, create an editable expense timeline, prevent duplicate imports, and generate local spending analytics. It receives future financial SMS alerts so the same core expense-management function continues automatically. Parsing and classification occur on-device. Unrelated messages are ignored and are not retained. Manual expense entry remains available when permission is denied.

## Permission-to-feature mapping

| Permission | Core use |
|---|---|
| `READ_SMS` | User-triggered historical scan of bank, card and UPI transaction alerts. |
| `RECEIVE_SMS` | Automatic on-device capture of new financial transaction alerts. |

## Review-video script

Record a short, unedited video on a test device:

1. Launch Nudge from a clean install.
2. Show the prominent disclosure explaining on-device SMS use.
3. Tap **Bank & UPI messages** and accept the Android SMS prompt.
4. Open Settings and tap **Scan message history**.
5. Show matched financial messages appearing as editable transactions.
6. Show that the app remains usable for manual expenses when SMS access is revoked.
7. Show **Saved messages** disabled by default and the local deletion controls.

Do not show real account numbers, phone numbers or personal message bodies in the review video. Use a test SIM/device or redact the recording.

## Reviewer notes

- Restricted access is requested only after a dedicated prominent disclosure and affirmative tap.
- Permission denial does not block onboarding or manual expense tracking.
- Camera and reminder notifications are requested contextually, not in the SMS disclosure.
- The privacy policy URL is available before consent and in Settings.

Official policy: [Use of SMS or Call Log permission groups](https://support.google.com/googleplay/android-developer/answer/10208820)

