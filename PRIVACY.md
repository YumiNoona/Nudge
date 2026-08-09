# Nudge Privacy Policy

**Effective date:** 9 August 2026  
**Developer:** Veil  
**App:** Nudge (`com.nudge.android`)

Nudge is a local-first Android expense manager. It does not require an account and does not operate a cloud financial database. This policy explains what the app can access, what remains on your device, and the controls available to you.

## Information Nudge processes

Nudge may process the following information only when you enable or use the corresponding feature:

- transaction details you enter manually, including amount, merchant, date, category and account;
- bank, card and UPI SMS/MMS content used to identify financial transactions;
- banking and payment-app notification text used to identify financial transactions;
- statements, receipts and images that you explicitly select or capture for import;
- a display name and profile picture that you choose to save locally;
- categories, accounts, correction rules and rejection patterns created through your use of the app.

## How processing works

Transaction parsing, merchant normalization, duplicate detection, OCR, categorization, learning and analytics run on your Android device. Nudge does not upload your transaction database, messages, notifications, statements, receipts, profile photo or learned rules to the developer.

Nudge has no advertising SDK, analytics SDK, account service or cloud synchronization service. The developer does not sell personal or financial information.

## SMS and notification access

If you grant SMS access, Nudge can scan SMS/MMS stored on the device and receive future messages. The app looks for financial transaction alerts. Unrelated messages are ignored and are not stored by Nudge.

If you grant notification access, Android allows Nudge to inspect notifications on the device. Nudge uses this access to recognize transaction alerts from banks and payment apps. Non-financial notifications are ignored and are not retained.

By default, transaction source bodies are not retained after parsing. If you enable **Save transaction messages**, source text linked to a captured transaction is stored in app-private storage and encrypted using an Android Keystore-backed key. You can review or delete saved sources at any time.

## Camera, images and files

Camera access is requested only when you start card or receipt scanning. Files and gallery images are chosen through Android's system picker; Nudge does not request broad storage access. OCR and parsing happen locally. Captured card, receipt and rendered PDF images are discarded after recognition unless you independently keep the original outside Nudge.

Portable JSON exports are not encrypted. You control where exported backups are stored and shared.

## Network access

The Google Play edition uses network access for Google Play delivery and update services. The GitHub edition may contact GitHub's public Releases API to check for an app update and download a release APK. No financial records or message content are included in an update request.

## Data storage, retention and deletion

Nudge stores app data locally in app-private storage. Android system backup is disabled for the app. Data remains until you edit or delete it, clear the app's storage, or uninstall Nudge.

You can:

- disable automatic capture in **Settings**;
- revoke SMS, notification or camera access in Android Settings;
- delete individual transactions or saved source messages;
- export or import a portable backup in **Settings → Backup & data**;
- erase all Nudge data in **Settings → Backup & data → Delete all data**.

## Sharing and third parties

Nudge does not share personal or financial data with the developer, advertisers, data brokers or analytics providers. Android, Google Play, your device manufacturer, and apps you intentionally share an export with may process data under their own policies.

If you choose **Tip the creator**, Nudge opens your selected UPI app or lets you scan/copy the displayed UPI details. Nudge does not process the payment or receive your payment credentials. The UPI provider processes that transaction under its own privacy policy. A tip is voluntary, goes directly to the creator, and unlocks no app feature or content.

## Children

Nudge is a personal financial utility intended for adults. It is not directed to children and should not be marketed to children.

## Security

Nudge uses Android app-private storage and Android Keystore encryption for optionally retained transaction-message sources. No security measure is perfect; keep the device protected and store exported backup files securely.

## Changes

Material changes will be published in this file and reflected by a new effective date. The current policy is always available from the app's Settings screen.

## Contact

For privacy questions or deletion assistance, open an issue at [github.com/YumiNoona/Nudge/issues](https://github.com/YumiNoona/Nudge/issues). A public support email must also be supplied in the Google Play store listing before release.
