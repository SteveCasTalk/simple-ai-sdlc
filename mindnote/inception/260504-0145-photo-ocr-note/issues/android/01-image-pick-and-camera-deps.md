---
type: issue
feature: photo-ocr-note
lane: android
status: ready
wave: 0
estimate: 30m
blocked-by: []
tags:
  - inception/issue
  - lane/android
  - feature/photo-ocr-note
  - status/ready
  - wave/0
---

# [Android] Add image-pick / camera capture infrastructure (deps + permissions + FileProvider)

**Lane:** Android
**PRD section:** Goals — pick from gallery or take a photo.
**API contract section:** N/A.

## Why

Foundation for the Scan flow. Establishes the dependencies and the manifest entries needed for camera capture and gallery picking, so the screen issues that follow can call `ActivityResultContracts` without setup churn.

## Implementation steps

1. In `app/build.gradle.kts`, ensure (and add if missing):
   - `androidx.activity:activity-compose` (already present — verify).
   - `io.coil-kt:coil-compose:2.6.0` for previewing the local image in the review step.
2. In `app/src/main/AndroidManifest.xml`:
   - Add `<uses-feature android:name="android.hardware.camera" android:required="false" />`.
   - Inside `<application>`, register a `FileProvider` for camera output:
     ```xml
     <provider
         android:name="androidx.core.content.FileProvider"
         android:authorities="${applicationId}.fileprovider"
         android:exported="false"
         android:grantUriPermissions="true">
         <meta-data android:name="android.support.FILE_PROVIDER_PATHS"
             android:resource="@xml/file_paths" />
     </provider>
     ```
3. Create `app/src/main/res/xml/file_paths.xml` declaring `<files-path name="ocr_scans" path="ocr/" />` and `<cache-path name="camera_temp" path="camera/" />`.
4. (No explicit `CAMERA` permission needed when using `ActivityResultContracts.TakePicture` with a content URI — verify on a device. If a runtime prompt is needed, add `<uses-permission android:name="android.permission.CAMERA" />` and the runtime request is handled in issue 04.)

## Files to touch

- `app/build.gradle.kts` — modify (add Coil if missing).
- `app/src/main/AndroidManifest.xml` — modify (camera feature, FileProvider).
- `app/src/main/res/xml/file_paths.xml` — create.

## Acceptance criteria

- [ ] `./gradlew :app:assembleDebug` succeeds.
- [ ] `FileProvider` authority `${applicationId}.fileprovider` is reachable (manual smoke: log `FileProvider.getUriForFile(...)` for a stub file in a small unit test or via a temporary dev call).
- [ ] No regression on existing screens (Home, Capture, Notes load and render).

## Blocked by

- Nothing — independently grabbable.

## Notes

- Don't add picker / camera UI here. That's issue 04. This issue is purely infrastructure.
- Coil is also reusable later for note detail thumbnails.
