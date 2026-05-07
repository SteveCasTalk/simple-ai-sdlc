---
type: issue
feature: photo-ocr-note
lane: android
status: ready
wave: 1
estimate: 60m
blocked-by:
  - "[[01-image-pick-and-camera-deps]]"
  - "[[03-scan-fab-and-nav-scaffold]]"
tags:
  - inception/issue
  - lane/android
  - feature/photo-ocr-note
  - status/ready
  - wave/1
---

# [Android] Implement image source selection (camera + gallery) in `ScanScreen`

**Lane:** Android
**PRD section:** Story 1 — pick from gallery or take a photo.
**API contract section:** N/A.

## Why

Lets the user actually choose an image. Produces a `Uri` pointing to a local image file the OCR step (issue 05) will upload. Once this lands, the app reaches a meaningful stub state: user picks → image preview shows.

## Implementation steps

1. In `ScanContract.kt`, expand `ScanState` to include `val phase: ScanPhase` where `ScanPhase` is sealed: `Idle`, `Picking`, `Picked(val uri: Uri)`. Add intents: `PickFromGallery`, `TakePhoto`, `ImageReceived(uri: Uri)`, `ImageCleared`. Add effect `LaunchGalleryPicker`, `LaunchCamera(targetUri: Uri)`, `ShowError(message: String)`.
2. In `ScanViewModel.kt`, create a `targetCameraUri()` helper that builds a content URI under the FileProvider authority `${applicationId}.fileprovider` for a new file at `cacheDir/camera/scan-<timestamp>.jpg`. Emit `LaunchCamera(uri)` on `TakePhoto` and `LaunchGalleryPicker` on `PickFromGallery`.
3. In `ScanScreen.kt`:
   - Replace the placeholder body with two buttons: "Take photo" and "Pick from gallery".
   - Use `rememberLauncherForActivityResult(ActivityResultContracts.TakePicture())` — on success, call `vm.send(ScanIntent.ImageReceived(targetUri))`. On failure or null, no-op.
   - Use `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())` constrained to `ImageOnly` — on result `uri != null`, call `vm.send(ScanIntent.ImageReceived(uri))`.
   - Listen to `ScanEffect.LaunchCamera` and `LaunchGalleryPicker` from a `LaunchedEffect(Unit) { vm.effects.collect { ... } }` block and trigger the matching launcher.
   - When `state.phase is Picked`, render an image preview using Coil (`AsyncImage(model = phase.uri, ...)`) and a "Remove" button that sends `ImageCleared`.
4. Handle the camera permission runtime request only if the manifest in issue 01 added `CAMERA`. Use `rememberLauncherForActivityResult(RequestPermission())` and gate the camera launcher behind it. If denied, send `ScanIntent.ShowError` with copy "Camera permission required to scan."
5. Validate the picked image's size before transitioning to `Picked`: query `contentResolver.openAssetFileDescriptor(uri, "r")?.length`. If > 25 MB, send `ShowError("Image too large. Max 25 MB.")` and stay in `Idle`.

## Files to touch

- `app/src/main/java/com/mindnote/features/scan/ScanContract.kt` — modify (expand state/intents/effects).
- `app/src/main/java/com/mindnote/features/scan/ScanViewModel.kt` — modify.
- `app/src/main/java/com/mindnote/features/scan/ScanScreen.kt` — modify (picker UI + launchers).
- `app/src/main/res/values/strings.xml` — modify (`scan_take_photo`, `scan_pick_gallery`, `scan_remove`, error strings).

## Acceptance criteria

- [ ] Tapping "Take photo" launches the camera; on capture, the screen shows the captured image as a preview.
- [ ] Tapping "Pick from gallery" launches the system photo picker; on selection, the screen shows the picked image.
- [ ] Picking an image > 25 MB shows the size-error toast and does not transition to `Picked`.
- [ ] "Remove" returns the screen to `Idle`.
- [ ] Unit test: `ScanViewModelTest` covers (a) `ImageReceived` transitions `Idle → Picked`, (b) `ImageCleared` transitions back to `Idle`, (c) oversized URI emits `ShowError`. Use a fake `ContentResolver` adapter or inject the size-lookup as a function for testability.
- [ ] `./gradlew :app:testDebugUnitTest :app:assembleDebug` succeeds.

## Blocked by

- [[01-image-pick-and-camera-deps]]
- [[03-scan-fab-and-nav-scaffold]]

## Notes

- `PickVisualMedia` is the modern, permission-free gallery picker (Android 13+) with backwards-compat polyfill — preferred over a custom intent.
- Keep file-size validation client-side so we don't waste a server round-trip on rejects.
