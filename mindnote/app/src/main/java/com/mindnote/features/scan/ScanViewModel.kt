package com.mindnote.features.scan

import androidx.lifecycle.viewModelScope
import com.mindnote.core.mvi.MviViewModel
import com.mindnote.data.remote.OcrResponseDto
import com.mindnote.domain.model.Note
import com.mindnote.domain.repository.NotesRepository
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ScanViewModel(
    private val ocrCall: suspend (deviceId: String, bytes: ByteArray, contentType: String) -> OcrResponseDto,
    private val notesRepository: NotesRepository,
    private val deviceId: String,
    private val readBytes: suspend (String) -> ByteArray,
    private val contentTypeOf: suspend (String) -> String,
    private val saveImage: suspend (noteId: String, bytes: ByteArray) -> String,
    private val ocrTimeoutMs: Long = SCAN_DEFAULT_OCR_TIMEOUT_MS,
    private val newNoteId: () -> String = { "n-${System.currentTimeMillis()}" },
    private val today: () -> LocalDate = { LocalDate.now() },
) : MviViewModel<ScanIntent, ScanState, ScanEffect>(initial = ScanState()) {

    private var ocrJob: Job? = null

    override suspend fun handle(intent: ScanIntent) {
        when (intent) {
            ScanIntent.Cancel -> handleCancel()
            ScanIntent.PickFromGallery -> emit(ScanEffect.LaunchGalleryPicker)
            ScanIntent.TakePhoto -> emit(ScanEffect.LaunchCamera)
            is ScanIntent.ImageReceived -> handleImageReceived(intent)
            ScanIntent.ImageCleared -> setState { copy(phase = ScanPhase.Idle) }
            ScanIntent.RunOcr -> handleRunOcr()
            is ScanIntent.EditText -> handleEditText(intent.value)
            ScanIntent.Save -> handleSave()
        }
    }

    private suspend fun handleCancel() {
        when (val phase = currentState.phase) {
            ScanPhase.Idle, is ScanPhase.Picked -> emit(ScanEffect.Dismiss)
            is ScanPhase.Loading -> {
                ocrJob?.cancel()
                ocrJob = null
                setState { copy(phase = ScanPhase.Picked(phase.uri)) }
            }
            is ScanPhase.Review -> setState { copy(phase = ScanPhase.Picked(phase.uri)) }
            is ScanPhase.Saving -> Unit
        }
    }

    private suspend fun handleImageReceived(intent: ScanIntent.ImageReceived) {
        val size = intent.sizeBytes
        if (size != null && size > SCAN_MAX_IMAGE_BYTES) {
            emit(ScanEffect.ShowError("Image too large. Max 25 MB."))
            return
        }
        setState { copy(phase = ScanPhase.Picked(intent.uri)) }
    }

    private fun handleRunOcr() {
        val phase = currentState.phase
        if (phase !is ScanPhase.Picked) return
        val uri = phase.uri
        setState { copy(phase = ScanPhase.Loading(uri)) }
        ocrJob = viewModelScope.launch {
            val outcome = runCatching {
                withTimeout(ocrTimeoutMs) {
                    val bytes = readBytes(uri)
                    val ct = contentTypeOf(uri)
                    ocrCall(deviceId, bytes, ct)
                }
            }
            outcome.fold(
                onSuccess = { result ->
                    if (result.text.isBlank()) {
                        setState { copy(phase = ScanPhase.Picked(uri)) }
                        emit(ScanEffect.ShowError("OCR found no text. Try a clearer photo."))
                    } else {
                        setState {
                            copy(
                                phase = ScanPhase.Review(
                                    uri = uri,
                                    text = result.text,
                                    languageHint = result.languageHint,
                                ),
                            )
                        }
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException && e !is TimeoutCancellationException) {
                        return@fold
                    }
                    setState { copy(phase = ScanPhase.Picked(uri)) }
                    val message = if (e is TimeoutCancellationException) {
                        "OCR timed out. Try again."
                    } else {
                        e.message?.takeIf { it.isNotBlank() } ?: "Couldn't extract text."
                    }
                    emit(ScanEffect.ShowError(message))
                },
            )
        }
    }

    private fun handleEditText(value: String) {
        val phase = currentState.phase
        if (phase is ScanPhase.Review) {
            setState { copy(phase = phase.copy(text = value)) }
        }
    }

    private suspend fun handleSave() {
        val phase = currentState.phase
        if (phase !is ScanPhase.Review) return
        val text = phase.text
        if (text.isBlank()) {
            emit(ScanEffect.ShowError("Nothing to save."))
            return
        }
        val uri = phase.uri
        setState { copy(phase = ScanPhase.Saving(uri = uri, text = text)) }
        val outcome = runCatching {
            val noteId = newNoteId()
            val bytes = readBytes(uri)
            val imagePath = saveImage(noteId, bytes)
            val title = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Scanned note"
            val note = Note(
                id = noteId,
                title = title,
                preview = text.take(120),
                body = text,
                tags = emptyList(),
                date = today(),
                imagePath = imagePath,
            )
            notesRepository.create(note)
            noteId
        }
        outcome.fold(
            onSuccess = { id -> emit(ScanEffect.Saved(id)) },
            onFailure = { e ->
                setState {
                    copy(phase = ScanPhase.Review(uri = uri, text = text, languageHint = ""))
                }
                emit(ScanEffect.ShowError(e.message?.takeIf { it.isNotBlank() } ?: "Couldn't save scan."))
            },
        )
    }
}
