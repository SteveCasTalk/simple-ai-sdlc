package com.mindnote.features.scan

import com.mindnote.data.remote.OcrResponseDto
import com.mindnote.util.FakeNotesRepository
import com.mindnote.util.MainDispatcherRule
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun newVm(
        ocrCall: suspend (String, ByteArray, String) -> OcrResponseDto = { _, _, _ ->
            OcrResponseDto("hello", "en")
        },
        notes: FakeNotesRepository = FakeNotesRepository(),
        readBytes: suspend (String) -> ByteArray = { ByteArray(4) { 0xAB.toByte() } },
        contentTypeOf: suspend (String) -> String = { "image/jpeg" },
        saveImage: suspend (String, ByteArray) -> String = { id, _ -> "/data/ocr/$id.jpg" },
        ocrTimeoutMs: Long = 60_000L,
        newNoteId: () -> String = { "n-fixed" },
        today: () -> LocalDate = { LocalDate.of(2026, 5, 4) },
    ) = ScanViewModel(
        ocrCall = ocrCall,
        notesRepository = notes,
        deviceId = "device-1",
        readBytes = readBytes,
        contentTypeOf = contentTypeOf,
        saveImage = saveImage,
        ocrTimeoutMs = ocrTimeoutMs,
        newNoteId = newNoteId,
        today = today,
    )

    @Test
    fun `Cancel from Idle emits Dismiss`() = runTest {
        val vm = newVm()
        vm.send(ScanIntent.Cancel)
        assertEquals(ScanEffect.Dismiss, vm.effects.first())
    }

    @Test
    fun `PickFromGallery emits LaunchGalleryPicker`() = runTest {
        val vm = newVm()
        vm.send(ScanIntent.PickFromGallery)
        assertEquals(ScanEffect.LaunchGalleryPicker, vm.effects.first())
    }

    @Test
    fun `ImageReceived under cap transitions Idle to Picked`() = runTest {
        val vm = newVm()
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 5_000_000L))
        val phase = vm.state.value.phase
        assertTrue(phase is ScanPhase.Picked)
        assertEquals("content://photo/1", (phase as ScanPhase.Picked).uri)
    }

    @Test
    fun `ImageReceived above cap emits ShowError and stays in Idle`() = runTest {
        val vm = newVm()
        vm.send(
            ScanIntent.ImageReceived(
                uri = "content://photo/big",
                sizeBytes = SCAN_MAX_IMAGE_BYTES + 1,
            )
        )
        val effect = vm.effects.first()
        assertTrue(effect is ScanEffect.ShowError)
        assertEquals(ScanPhase.Idle, vm.state.value.phase)
    }

    @Test
    fun `RunOcr happy path transitions Picked to Review with OCR text`() = runTest {
        val vm = newVm(
            ocrCall = { _, _, _ -> OcrResponseDto("extracted body\nline 2", "vi") }
        )
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 100L))
        vm.send(ScanIntent.RunOcr)
        // Wait for the launched coroutine to settle.
        // Since we're on UnconfinedTestDispatcher the coroutine runs eagerly.
        val phase = vm.state.value.phase
        assertTrue("expected Review, was $phase", phase is ScanPhase.Review)
        val review = phase as ScanPhase.Review
        assertEquals("extracted body\nline 2", review.text)
        assertEquals("vi", review.languageHint)
    }

    @Test
    fun `RunOcr empty text returns to Picked and emits ShowError`() = runTest {
        val vm = newVm(
            ocrCall = { _, _, _ -> OcrResponseDto("   \n", "") }
        )
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 100L))
        vm.send(ScanIntent.RunOcr)
        val effect = vm.effects.first()
        assertTrue(effect is ScanEffect.ShowError)
        val phase = vm.state.value.phase
        assertTrue("expected Picked, was $phase", phase is ScanPhase.Picked)
    }

    @Test
    fun `Cancel during Loading aborts the OCR job and returns to Picked`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val vm = newVm(
            ocrCall = { _, _, _ ->
                gate.await()
                OcrResponseDto("never", "")
            },
        )
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 100L))
        vm.send(ScanIntent.RunOcr)
        assertTrue(vm.state.value.phase is ScanPhase.Loading)
        vm.send(ScanIntent.Cancel)
        gate.complete(Unit)
        val phase = vm.state.value.phase
        assertTrue("expected Picked after cancel, was $phase", phase is ScanPhase.Picked)
    }

    @Test
    fun `RunOcr timeout produces ShowError and returns to Picked`() = runTest {
        val vm = newVm(
            ocrCall = { _, _, _ ->
                delay(10_000) // far longer than the timeout below
                OcrResponseDto("never", "")
            },
            ocrTimeoutMs = 10L,
        )
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 100L))
        vm.send(ScanIntent.RunOcr)
        val effect = vm.effects.first()
        assertTrue(effect is ScanEffect.ShowError)
        val phase = vm.state.value.phase
        assertTrue("expected Picked after timeout, was $phase", phase is ScanPhase.Picked)
    }

    @Test
    fun `Save persists Note via repository with imagePath and emits Saved`() = runTest {
        val notes = FakeNotesRepository()
        val savedPaths = mutableListOf<Pair<String, ByteArray>>()
        val vm = newVm(
            ocrCall = { _, _, _ -> OcrResponseDto("Title line\nbody", "en") },
            notes = notes,
            saveImage = { id, bytes ->
                savedPaths += id to bytes
                "/data/ocr/$id.jpg"
            },
            newNoteId = { "n-42" },
            today = { LocalDate.of(2026, 5, 4) },
        )
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 100L))
        vm.send(ScanIntent.RunOcr)
        assertTrue(vm.state.value.phase is ScanPhase.Review)
        vm.send(ScanIntent.Save)

        val effect = vm.effects.first()
        assertTrue(effect is ScanEffect.Saved && effect.noteId == "n-42")
        assertEquals(1, notes.created.size)
        val note = notes.created.single()
        assertEquals("n-42", note.id)
        assertEquals("Title line", note.title)
        assertEquals("Title line\nbody", note.body)
        assertEquals("/data/ocr/n-42.jpg", note.imagePath)
        assertEquals(LocalDate.of(2026, 5, 4), note.date)
        assertEquals(1, savedPaths.size)
        assertEquals("n-42", savedPaths.single().first)
    }

    @Test
    fun `EditText updates only when in Review`() = runTest {
        val vm = newVm()
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 100L))
        vm.send(ScanIntent.RunOcr) // → Review("hello")
        vm.send(ScanIntent.EditText("edited"))
        val phase = vm.state.value.phase
        assertTrue(phase is ScanPhase.Review)
        assertEquals("edited", (phase as ScanPhase.Review).text)
    }

    @Test
    fun `Cancel from Review returns to Picked`() = runTest {
        val vm = newVm()
        vm.send(ScanIntent.ImageReceived(uri = "content://photo/1", sizeBytes = 100L))
        vm.send(ScanIntent.RunOcr)
        assertTrue(vm.state.value.phase is ScanPhase.Review)
        vm.send(ScanIntent.Cancel)
        val phase = vm.state.value.phase
        assertTrue(phase is ScanPhase.Picked)
        assertEquals("content://photo/1", (phase as ScanPhase.Picked).uri)
    }
}
