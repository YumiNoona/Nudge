# Detection quality

This pass improves capture and review without adding a network service or runtime permissions.

## Matching and evidence

- Capture writes run inside a Room transaction so simultaneous workers cannot insert the same source twice.
- Exact retries use a SHA-256 fingerprint of source family, sender, timestamp, and normalized message text. Different timestamps produce different fingerprints.
- Explicit UTR/RRN/reference IDs are hashed. Conflicting IDs preserve separate payments.
- Account matching uses bank and masked suffix evidence. Ambiguous bank names, conflicting details, multiple suffixes, and default-account fallbacks stay uncertain.
- Equal amount and direction are only candidate filters. Automatic merging requires an exact source identity or matching reference evidence with a strong account match. Close payments without references remain separate and may be flagged for comparison, even across sources.
- Worker identities use full timestamps rather than minute buckets. SMS source lookup chooses the closest unique received/sent timestamp instead of the latest identical body.
- Merged sources are retained as separate linked records. Optional bodies still follow the existing encrypted-message-saving and retention settings.
- Unambiguous partial refunds inherit the original expense category when account, merchant, date range and amount support a unique match. This does not reconcile a series of partial refunds or invent a full refund relationship.

## Review and receipts

Amount, direction, merchant, account, and category retain separate confidence. Strong direction/category evidence cannot raise a weak amount or account above review thresholds. These are heuristic scores, not measured probabilities.

Review shows reasons, lets the user edit details, and provides comparison/removal of a possible duplicate without teaching an unwanted merchant rejection. Learning a category does not automatically approve other uncertain fields. Settings exposes learned rules for category edits or forgetting.

Receipts retain identical product rows. Page stitching requires at least two strongly matching lines. Reconciliation discrepancies lower receipt confidence even when product rows agree with the printed subtotal.

Statement import matches account, currency, type, merchant and exact timestamp, consuming each prior match at most once. Repeated rows within a fresh import are preserved. Date-only exports remain intrinsically ambiguous: previewed matches/conflicts and reference-aware reconciliation are a separate follow-up.

## Local diagnostics

Settings → Capture health shows capture toggle, SMS access, actual notification-listener connection state, and up to 40 recent outcomes. The activity contains outcome/reason, source family and processing time only; no message bodies, merchants, account digits or payment references. Delete everything clears this activity too.

## Compatibility and checks

Room migration 6 → 7 adds optional source fingerprints, reference hashes and assessment JSON, and permits multiple sources per transaction. Existing transactions and linked messages are preserved; legacy records without field assessments are treated conservatively.

Run pure regressions:

```powershell
.\gradlew.bat :android:testPlayDebugUnitTest :shared:testDebugUnitTest
```

Build an isolated emulator test install:

```powershell
.\gradlew.bat :android:assembleGithubDebug :android:assembleGithubDebugAndroidTest -PcaptureScreenshots
adb install -r android/build/outputs/apk/github/debug/Nudge-github-v4.9.2-debug.apk
adb install -r android/build/outputs/apk/androidTest/github/debug/android-github-debug-androidTest.apk
adb shell am instrument -w -e class com.nudge.android.CapturePipelineTest com.nudge.android.screenshots.test/androidx.test.runner.AndroidJUnitRunner
```

Device tests use an in-memory database plus a separate migration-test database in the isolated screenshot package. They cover concurrent retries, repeat payments, separate accounts, merged sources, partial refunds, uncertainty, diagnostic privacy and preservation of legacy data through migration.

## Follow-up work

Benchmark larger OCR engines against a redacted receipt corpus before replacing the current recognizer. Image-region highlighting, full two-sided own-account transfer pairing, cumulative partial-refund reconciliation, statement conflict previews, calibrated field thresholds, richer budgets and recurring-payment alerts are not part of this pass.
