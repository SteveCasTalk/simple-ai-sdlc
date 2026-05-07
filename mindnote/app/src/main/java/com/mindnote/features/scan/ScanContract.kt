package com.mindnote.features.scan

import com.mindnote.core.mvi.UiEffect
import com.mindnote.core.mvi.UiIntent
import com.mindnote.core.mvi.UiState

const val SCAN_MAX_IMAGE_BYTES: Long = 25L * 1024L * 1024L
const val SCAN_DEFAULT_OCR_TIMEOUT_MS: Long = 60_000L

sealed interface ScanIntent : UiIntent {
    data object Cancel : ScanIntent
    data object PickFromGallery : ScanIntent
    data object TakePhoto : ScanIntent
    data class ImageReceived(val uri: String, val sizeBytes: Long?) : ScanIntent
    data object ImageCleared : ScanIntent
    data object RunOcr : ScanIntent
    data class EditText(val value: String) : ScanIntent
    data object Save : ScanIntent
}

sealed interface ScanPhase {
    data object Idle : ScanPhase
    data class Picked(val uri: String) : ScanPhase
    data class Loading(val uri: String) : ScanPhase
    data class Review(val uri: String, val text: String, val languageHint: String) : ScanPhase
    data class Saving(val uri: String, val text: String) : ScanPhase
}

data class ScanState(
    val phase: ScanPhase = ScanPhase.Idle,
) : UiState

sealed interface ScanEffect : UiEffect {
    data object Dismiss : ScanEffect
    data object LaunchGalleryPicker : ScanEffect
    data object LaunchCamera : ScanEffect
    data class Saved(val noteId: String) : ScanEffect
    data class ShowError(val message: String) : ScanEffect
}
