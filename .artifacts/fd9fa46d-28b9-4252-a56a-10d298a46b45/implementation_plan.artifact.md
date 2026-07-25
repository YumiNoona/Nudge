# Fix SQLCipher Dependency Resolution Issue

The project is failing to sync because it attempts to resolve `net.zetetic:android-database-sqlcipher:4.5.6`. This specific version (4.5.6) was released under a new artifact coordinate, `net.zetetic:sqlcipher-android`, which replaces the now-deprecated legacy artifact.

## Proposed Changes

### Android Module

#### [MODIFY] [android/build.gradle.kts](file:///E:/AI/Nudge/android/build.gradle.kts)
- Update the SQLCipher dependency coordinate from `net.zetetic:android-database-sqlcipher:4.5.6` to `net.zetetic:sqlcipher-android:4.5.6`.

#### [MODIFY] [NudgeDatabase.kt](file:///E:/AI/Nudge/android/src/main/kotlin/com/nudge/android/data/NudgeDatabase.kt)
- Update the import and usage of the SQLCipher Room factory to match the new library's package structure and class names.
- Change `import net.sqlcipher.database.SupportFactory` to `import net.zetetic.database.sqlcipher.SupportOpenHelperFactory`.
- Update the `getInstance` method to use `SupportOpenHelperFactory`.

#### [MODIFY] [proguard-rules.pro](file:///E:/AI/Nudge/android/proguard-rules.pro)
- Update ProGuard rules to reflect the new package name `net.zetetic.database.sqlcipher`.

## Verification Plan

### Automated Tests
- Run Gradle sync to verify that the dependency now resolves.
- Perform a build to ensure that the code compiles with the new library.
- (Optional) Run unit tests for the database if available.

### Manual Verification
- Verify that the app can still open and interact with the encrypted database (requires running on a device/emulator).
