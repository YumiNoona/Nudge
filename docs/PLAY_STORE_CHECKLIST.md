# Google Play release checklist — Nudge 4.8.0

This is the operational checklist for the first Google Play release of `com.veilafk.nudge`. Complete it from top to bottom after the Play developer-account verification finishes.

## 1. Build identity and signing

- [x] Google Play package name is `com.veilafk.nudge`.
- [x] The GitHub distribution keeps `com.nudge.android` so existing sideloaded installations remain update-compatible.
- [x] Play candidate uses `versionName 4.8.0` and `versionCode 14`.
- [x] `compileSdk` and `targetSdk` are API 36.
- [x] Minimum Android version remains API 26 / Android 8.0.
- [x] Play and GitHub are separate Gradle product flavors.
- [x] The Play manifest does not request `REQUEST_INSTALL_PACKAGES` or silent-update permissions.
- [ ] Back up the existing Nudge release keystore and passwords in two secure, separate locations.
- [ ] In **Play Console → Test and release → App integrity**, enroll in Play App Signing.
- [ ] If GitHub-release users must migrate to the Play build without uninstalling, provide the existing Nudge release key as the Play app-signing key when Play Console offers that first-release choice. Do not let Play create an unrelated app-signing identity.
- [ ] After enrollment, create a separate upload key for future Play uploads and register its certificate with Play Console.

Build the candidate:

```powershell
.\gradlew.bat clean :shared:test :android:testPlayDebugUnitTest :android:lintPlayRelease :android:bundlePlayRelease
```

Upload this file to Play Console:

```text
android/build/outputs/bundle/playRelease/android-play-release.aab
```

Do not upload an APK to Google Play. Keep the GitHub APK workflow separate:

```powershell
.\gradlew.bat :android:assembleGithubRelease
```

## 2. Store setup

- [ ] App name: **Nudge: Expense Manager**.
- [ ] Default language: English (India) or English (United States).
- [ ] App category: **Finance**.
- [ ] App type: **App**.
- [ ] Pricing: **Free**.
- [ ] Add the support email you actively monitor.
- [ ] Privacy-policy URL: `https://github.com/YumiNoona/Nudge/blob/main/PRIVACY.md`.
- [ ] Prepare and upload the icon, feature graphic and truthful device screenshots using `docs/STORE_ASSETS.md`.
- [ ] Use the copy in `docs/PLAY_STORE_LISTING.md`.

## 3. App content declarations

- [ ] **Privacy policy:** enter the public URL above.
- [ ] **Ads:** select **No, my app does not contain ads**.
- [ ] **App access:** select **All functionality is available without special access**; Nudge has no login.
- [ ] **Target audience:** choose adult age groups only; do not include children.
- [ ] **Content rating:** complete the IARC questionnaire truthfully as a finance/utility app with no user-generated or social content.
- [ ] **News app:** No.
- [ ] **COVID-19 app:** No.
- [ ] **Data safety:** follow `docs/DATA_SAFETY_GUIDE.md` and confirm against the final binary.
- [ ] **Financial features:** declare personal expense/budget management. Select no lending, banking, brokerage, crypto, insurance, money-transfer or credit-repair features.
- [ ] **SMS/Call Log permissions:** submit the declaration described in `docs/SMS_PERMISSION_DECLARATION.md` and provide the requested demonstration video.
- [ ] Confirm the creator-tip screen states that 100% goes to the creator and unlocks no feature, content, badge, ad removal or benefit; confirm the Play build contains no in-app APK installer.

## 4. Testing and rollout

- [ ] Run the signed bundle through **Internal testing** first.
- [ ] Test onboarding with every restricted permission accepted and denied.
- [ ] Test manual entry without SMS, notification or camera permission.
- [ ] Test live SMS capture, historical scan, notification capture, receipt camera, PDF/image import, export/delete, widgets and light/dark themes.
- [ ] Review the **Pre-launch report**, accessibility report, Android vitals and policy warnings.
- [ ] If this is a new personal developer account, run a closed test with at least 12 opted-in testers continuously for 14 days, then apply for production access.
- [ ] Use staged production rollout rather than 100% on the first day.

## 5. Required external actions

Codex cannot complete these account-bound actions for you: identity verification, signing-key enrollment choices, tester opt-in, policy-form attestations, asset upload, or pressing **Start rollout**. Everything else in this repository is prepared for those steps.

## Official references

- [Target API requirements](https://developer.android.com/google/play/requirements/target-sdk)
- [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756)
- [SMS and Call Log permissions](https://support.google.com/googleplay/android-developer/answer/10208820)
- [New personal-account testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Prepare and roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348)
