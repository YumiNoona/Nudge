# Fix jlink.exe Exit Value 1 Build Error

The build is failing because the `JdkImageTransform` task in Android Gradle Plugin (AGP) 8.2.0 is incompatible with the JDK version (likely JDK 26) provided by the Android Studio 2026 environment. The `jlink.exe` tool fails with exit value 1 when processing the Android SDK's `core-for-system-modules.jar`.

## User Review Required

> [!IMPORTANT]
> This plan proposes adding a compatibility flag to `gradle.properties` to bypass the failing transform. This is a low-risk workaround that avoids a major upgrade of AGP and Kotlin versions, which might be necessary for full compatibility with 2026 tools.

## Proposed Changes

### [Component] Build Configuration

#### [MODIFY] [gradle.properties](file:///E:/AI/Nudge/gradle.properties)
- Add `android.jdk.image.legacy.behavior=true` to revert to the legacy JDK discovery behavior, bypassing the modular JDK image transform that is failing.

#### [MODIFY] [android/build.gradle.kts](file:///E:/AI/Nudge/android/build.gradle.kts)
- Explicitly configure the Java toolchain to version 17. This ensures that even if the IDE's bundled JDK is "too new" (e.g., JDK 26), Gradle will use a compatible JDK 17 for compilation tasks.

## Verification Plan

### Automated Tests
- Run `./gradlew :android:assembleDebug` to verify that the build succeeds without the `jlink` error.
