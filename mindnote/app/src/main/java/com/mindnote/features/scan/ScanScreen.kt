package com.mindnote.features.scan

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mindnote.R
import com.mindnote.design.HSpace
import com.mindnote.design.HairlineDivider
import com.mindnote.design.MindNoteTheme
import com.mindnote.design.VSpace
import org.koin.androidx.compose.koinViewModel
import java.io.File

@Composable
fun ScanScreen(
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit,
    vm: ScanViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = MindNoteTheme.snackbar

    val cameraTargetUri = remember { Holder<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            vm.send(
                ScanIntent.ImageReceived(
                    uri = uri.toString(),
                    sizeBytes = querySizeBytes(context, uri),
                )
            )
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = cameraTargetUri.value
        if (success && uri != null) {
            vm.send(
                ScanIntent.ImageReceived(
                    uri = uri.toString(),
                    sizeBytes = querySizeBytes(context, uri),
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        vm.effects.collect { effect ->
            when (effect) {
                ScanEffect.Dismiss -> onDismiss()
                ScanEffect.LaunchGalleryPicker -> galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                ScanEffect.LaunchCamera -> {
                    val target = newCameraTargetUri(context)
                    cameraTargetUri.value = target
                    cameraLauncher.launch(target)
                }
                is ScanEffect.Saved -> onSaved(effect.noteId)
                is ScanEffect.ShowError -> snackbar.showSnackbar(effect.message)
            }
        }
    }

    val phase = state.phase

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MindNoteTheme.colors.bg)
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VSpace(44)
            TopBar(
                phase = phase,
                onCancel = { vm.send(ScanIntent.Cancel) },
                onSave = { vm.send(ScanIntent.Save) },
            )
            HairlineDivider()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                VSpace(24)
                when (phase) {
                    ScanPhase.Idle -> IdleBody(
                        onTakePhoto = { vm.send(ScanIntent.TakePhoto) },
                        onPickGallery = { vm.send(ScanIntent.PickFromGallery) },
                    )

                    is ScanPhase.Picked -> PickedBody(
                        uri = phase.uri,
                        onExtract = { vm.send(ScanIntent.RunOcr) },
                        onRemove = { vm.send(ScanIntent.ImageCleared) },
                    )

                    is ScanPhase.Loading -> LoadingBody(uri = phase.uri)

                    is ScanPhase.Review -> ReviewBody(
                        uri = phase.uri,
                        text = phase.text,
                        onChange = { vm.send(ScanIntent.EditText(it)) },
                    )

                    is ScanPhase.Saving -> ReviewBody(
                        uri = phase.uri,
                        text = phase.text,
                        onChange = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(phase: ScanPhase, onCancel: () -> Unit, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.action_cancel),
            style = MindNoteTheme.typography.label,
            color = MindNoteTheme.colors.textMuted,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(enabled = phase !is ScanPhase.Saving, onClick = onCancel),
        )
        Text(
            text = stringResource(R.string.scan_title),
            style = MindNoteTheme.typography.titleSmall,
            color = MindNoteTheme.colors.text,
            modifier = Modifier.align(Alignment.Center),
        )
        when (phase) {
            is ScanPhase.Review -> {
                val canSave = phase.text.isNotBlank()
                Text(
                    text = stringResource(R.string.action_save),
                    style = MindNoteTheme.typography.label,
                    color = if (canSave) MindNoteTheme.colors.accent
                    else MindNoteTheme.colors.accent.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable(enabled = canSave, onClick = onSave),
                )
            }
            is ScanPhase.Saving -> CircularProgressIndicator(
                color = MindNoteTheme.colors.accent,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(18.dp),
            )
            else -> Unit
        }
    }
}

@Composable
private fun IdleBody(onTakePhoto: () -> Unit, onPickGallery: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Top) {
        Text(
            text = stringResource(R.string.scan_placeholder),
            style = MindNoteTheme.typography.body,
            color = MindNoteTheme.colors.textMuted,
        )
        VSpace(20)
        SourceButton(
            label = stringResource(R.string.scan_take_photo),
            icon = Icons.Outlined.PhotoCamera,
            onClick = onTakePhoto,
        )
        VSpace(12)
        SourceButton(
            label = stringResource(R.string.scan_pick_gallery),
            icon = Icons.Outlined.PhotoLibrary,
            onClick = onPickGallery,
        )
    }
}

@Composable
private fun PickedBody(uri: String, onExtract: () -> Unit, onRemove: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MindNoteTheme.colors.surface),
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        VSpace(16)
        SourceButton(
            label = stringResource(R.string.scan_extract_text),
            icon = Icons.Outlined.TextFields,
            onClick = onExtract,
        )
        VSpace(12)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onRemove)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = MindNoteTheme.colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
            HSpace(8)
            Text(
                text = stringResource(R.string.scan_remove),
                style = MindNoteTheme.typography.label,
                color = MindNoteTheme.colors.textMuted,
            )
        }
    }
}

@Composable
private fun LoadingBody(uri: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MindNoteTheme.colors.surface),
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        VSpace(20)
        CircularProgressIndicator(color = MindNoteTheme.colors.accent)
        VSpace(12)
        Text(
            text = stringResource(R.string.scan_loading),
            style = MindNoteTheme.typography.body,
            color = MindNoteTheme.colors.textMuted,
        )
    }
}

@Composable
private fun ReviewBody(uri: String, text: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MindNoteTheme.colors.surface),
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        VSpace(20)
        BasicTextField(
            value = text,
            onValueChange = onChange,
            textStyle = MindNoteTheme.typography.body.copy(
                color = MindNoteTheme.colors.text,
                lineHeight = 24.sp,
            ),
            cursorBrush = SolidColor(MindNoteTheme.colors.accent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.scan_review_placeholder),
                        style = MindNoteTheme.typography.body.copy(lineHeight = 24.sp),
                        color = MindNoteTheme.colors.textSubtle,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun SourceButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MindNoteTheme.colors.surface)
            .border(1.dp, MindNoteTheme.colors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MindNoteTheme.colors.accent,
            modifier = Modifier.size(20.dp),
        )
        HSpace(12)
        Text(label, style = MindNoteTheme.typography.body, color = MindNoteTheme.colors.text)
    }
}

private class Holder<T>(var value: T)

private fun newCameraTargetUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "scan-${System.currentTimeMillis()}.jpg")
    if (!file.exists()) file.createNewFile()
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

private fun querySizeBytes(context: Context, uri: Uri): Long? = runCatching {
    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
}.getOrNull()
