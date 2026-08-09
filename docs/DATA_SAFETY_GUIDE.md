# Data safety form guide

Use this guide for the Google Play **Data safety** form, then re-check every answer against the exact Play bundle you upload. Google defines data as “collected” when it is transmitted off the device; Nudge's financial processing is local.

## Recommended answers for the current Play build

| Question | Nudge answer | Reason |
|---|---|---|
| Does the app collect or share required user-data types? | **No** | Transaction, message, notification, image, statement and profile data are not transmitted to Nudge or a third party by the app. |
| Is all user data encrypted in transit? | **Not applicable** to local financial data | Financial data is not transmitted. Google Play update traffic is platform-managed. |
| Can users request deletion? | **Yes / deletion is available in-app** | Settings → Backup & data → Delete all data, or Android clear storage/uninstall. |
| Is an account required? | **No** | Nudge has no account or cloud profile. |
| Does the app contain ads? | **No** | No ad SDK is included. |

## Important distinctions

- Local access is still sensitive and must be disclosed prominently even when it is not “collection” in the Data safety definition.
- Do not mark data as “ephemeral processing” merely to avoid disclosure if a future build uploads or retains it on a server.
- If you later add crash reporting, analytics, cloud sync, remote OCR, email APIs, authentication, or support-message uploads, update this form and the privacy policy before releasing that build.
- Google Play and Android platform services may process installation/update diagnostics under Google's policies; do not incorrectly attribute platform-only collection to Nudge.
- The optional creator-tip action is handed to a separate UPI app. Nudge does not receive payment credentials or transaction data; the selected UPI provider's disclosures apply.

## Final binary audit

Before submitting, inspect **Play Console → App content → Data safety** and the SDK list shown by Play Console. If any dependency reports data collection that is not described above, stop and reconcile it before publishing.
