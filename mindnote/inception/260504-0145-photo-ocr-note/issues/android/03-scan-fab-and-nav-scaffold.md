---
type: issue
feature: photo-ocr-note
lane: android
status: ready
wave: 0
estimate: 45m
blocked-by: []
tags:
  - inception/issue
  - lane/android
  - feature/photo-ocr-note
  - status/ready
  - wave/0
---

# [Android] Add Scan FAB on Home + new `Scan` nav route + empty `ScanCaptureScreen`

**Lane:** Android
**PRD section:** Goals — launch from Home FAB.
**API contract section:** N/A.

## Why

Gets the entry point on screen and reachable so subsequent issues can fill in behavior incrementally. After this lands, the user can already see the new affordance and the team can demo it as a stub.

## Implementation steps

1. Add a route constant in `app/src/main/java/com/mindnote/core/navigation/Destinations.kt`:
   `const val Scan = "scan"`.
2. Create `app/src/main/java/com/mindnote/features/scan/ScanContract.kt` with `ScanState`, `ScanIntent`, `ScanEffect` mirroring the MVI pattern in `features/capture/CaptureContract.kt`. For now `ScanState` carries only `val phase: ScanPhase = Idle` (placeholder; expanded in 04/05).
3. Create `app/src/main/java/com/mindnote/features/scan/ScanViewModel.kt` — a minimal `ViewModel` extending the base `Mvi` types in `core/mvi/Mvi.kt`. No real logic yet, just structure.
4. Create `app/src/main/java/com/mindnote/features/scan/ScanScreen.kt` — a Compose screen with a top bar ("Scan a photo"), a Cancel action that calls the navigation back-stack, and a placeholder body ("Pick or capture an image — coming soon").
5. Register `ScanViewModel` in Koin: `viewModel { ScanViewModel(get()) }` in `core/di/AppModule.kt`.
6. Wire navigation in `app/src/main/java/com/mindnote/core/navigation/MindNoteNavHost.kt`: add a `composable(Routes.Scan) { ScanScreen(onDismiss = { navController.popBackStack() }, onSaved = { id -> navController.navigate(Routes.noteDetail(id)) { popUpTo(Routes.Home) } }) }`.
7. In `app/src/main/java/com/mindnote/features/home/HomeScreen.kt`, add a **second** FAB labelled "Scan" using `Icons.Outlined.PhotoCamera` (or `DocumentScanner`), positioned to the **right** of the existing `+` Capture FAB inside `BottomTabBar`. Tapping it sends a new `HomeIntent.OpenScan` which the ViewModel maps to a new `HomeEffect.NavigateToScan`. Plumb the effect through `HomeScreen`'s callbacks to `MindNoteNavHost`.
8. Update `HomeContract.kt` and `HomeViewModel.kt` for the new intent/effect; keep changes minimal.
9. Update strings: add `R.string.tab_scan` ("Scan"), `R.string.scan_title` ("Scan a photo"), `R.string.scan_placeholder` ("Pick or capture an image to extract text.") in `app/src/main/res/values/strings.xml`.

## Files to touch

- `app/src/main/java/com/mindnote/core/navigation/Destinations.kt` — modify (add `Scan`).
- `app/src/main/java/com/mindnote/core/navigation/MindNoteNavHost.kt` — modify (register composable).
- `app/src/main/java/com/mindnote/features/scan/ScanContract.kt` — create.
- `app/src/main/java/com/mindnote/features/scan/ScanViewModel.kt` — create.
- `app/src/main/java/com/mindnote/features/scan/ScanScreen.kt` — create.
- `app/src/main/java/com/mindnote/core/di/AppModule.kt` — modify (register VM).
- `app/src/main/java/com/mindnote/features/home/HomeScreen.kt` — modify (Scan FAB).
- `app/src/main/java/com/mindnote/features/home/HomeContract.kt` — modify (intent/effect).
- `app/src/main/java/com/mindnote/features/home/HomeViewModel.kt` — modify (handle intent).
- `app/src/main/res/values/strings.xml` — modify.

## Acceptance criteria

- [ ] Tapping the Scan FAB on Home navigates to the new screen.
- [ ] The new screen shows the placeholder copy and a working Cancel that returns to Home.
- [ ] Existing Capture FAB still works (no regression).
- [ ] Unit test: extend `HomeViewModelTest` to assert that `HomeIntent.OpenScan` produces `HomeEffect.NavigateToScan`.
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` succeeds.

## Blocked by

- Nothing — independently grabbable.

## Notes

- Layout decision (Scan FAB **alongside** the existing Capture FAB rather than replacing it) is captured in [[../../decisions|D3]] for mob ratification.
- Keep `ScanContract` lean; the full state machine for the scan flow is added in 04/05.
