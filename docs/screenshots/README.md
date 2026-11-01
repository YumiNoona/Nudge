# App screenshots

These PNGs are actual Android emulator screenshots of Transactions, Analytics, and Accounts in dark mode. All account names, merchant names, balances, and transactions are fictional. The fixture uses the previous calendar month to show a complete month's analytics.

The opt-in instrumentation test launches the real app, checks the redesigned stats and navigation, and captures the rendered screen. It runs in the separate `com.nudge.android.screenshots` package. Sample data is test-only and is never seeded in a normal Play/GitHub build.

With a 1080 × 2400 Android emulator connected, run from the repository root in PowerShell:

```powershell
.\gradlew.bat :android:assembleGithubDebug :android:assembleGithubDebugAndroidTest -PcaptureScreenshots
adb install -r android/build/outputs/apk/github/debug/Nudge-github-v4.9.2-debug.apk
adb install -r android/build/outputs/apk/androidTest/github/debug/android-github-debug-androidTest.apk
adb shell am instrument -w -e readmeScreenshots true -e class com.nudge.android.ReadmeScreenshotsTest com.nudge.android.screenshots.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.nudge.android.screenshots/files/readme-screenshots/. docs/screenshots/
```

Use `adb -s <serial>` when multiple devices are connected. The fixture resets only the isolated screenshot app's demo database on each run. It is skipped in ordinary instrumentation runs and refuses to seed a normal app package.

Keep the original screen captures; do not replace them with recreated interface mockups. Review each PNG after UI changes before committing.
