# Fix Build Errors in Nudge Android

The project has several build errors:
1. **Unresolved references** in `AddTransactionSheet.kt` due to missing imports.
2. **Invalid Composable invocations** in several files where `@Composable` properties (from `Nc`) or functions (like `Canvas`) are called from non-composable contexts (like `Canvas` draw blocks or regular functions).

## Proposed Changes

### [android]

#### [MODIFY] [AddTransactionSheet.kt](file:///E:/AI/Nudge/android/src/main/kotlin/com/nudge/android/ui/AddTransactionSheet.kt)
- Add missing imports for `KeypadGrid`, `applyKeypadInput`, and `ScrollableChipRow`.
- Remove unused import `androidx.compose.foundation.horizontalScroll`.

#### [MODIFY] [KeypadGrid.kt](file:///E:/AI/Nudge/android/src/main/kotlin/com/nudge/android/ui/components/KeypadGrid.kt)
- Move `LocalContext.current` out of `remember` block to fix invalid Composable invocation.

#### [MODIFY] [ChartsScreen.kt](file:///E:/AI/Nudge/android/src/main/kotlin/com/nudge/android/ui/ChartsScreen.kt)
- Capture `Nc` colors in local variables within the `@Composable` scope before passing them to the `Canvas` draw block.

#### [MODIFY] [SavingsGoalsScreen.kt](file:///E:/AI/Nudge/android/src/main/kotlin/com/nudge/android/ui/SavingsGoalsScreen.kt)
- Mark `JarIllustration` as `@Composable`.
- Capture `Nc` colors in local variables within the `@Composable` scope before passing them to the `Canvas` draw block.

#### [MODIFY] [BadgeTile.kt](file:///E:/AI/Nudge/android/src/main/kotlin/com/nudge/android/ui/components/BadgeTile.kt)
- Capture `Nc` colors in local variables within the `@Composable` scope before passing them to the `drawBehind` block.

#### [MODIFY] [RingStatCard.kt](file:///E:/AI/Nudge/android/src/main/kotlin/com/nudge/android/ui/components/RingStatCard.kt)
- Capture `Nc` colors in local variables within the `@Composable` scope before passing them to the `Canvas` draw block.

## Verification Plan

### Automated Tests
- Run `./gradlew :android:compileDebugKotlin` to verify the build passes.
- Use `analyze_file` on modified files to check for any remaining issues.

