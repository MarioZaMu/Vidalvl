package com.vidalvl.app.ui.screens.missions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vidalvl.app.R
import com.vidalvl.app.domain.verification.FailureKind
import com.vidalvl.app.domain.verification.VerificationLimits
import com.vidalvl.app.domain.verification.VerificationResult
import com.vidalvl.app.ui.components.PrimaryButton
import com.vidalvl.app.ui.components.SecondaryButton
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

private sealed interface Step {
    data object Camera : Step
    data class Review(val photo: Bitmap) : Step
    data object Checking : Step
    data class Problem(val message: String, val canSkip: Boolean) : Step
}

/**
 * Full-screen flow to verify a mission with a photo: permission, camera, review, check.
 * The photo lives only in memory and is dropped as soon as the flow ends.
 * [verify] returns the verdict; [onVerified] runs once when it is positive.
 */
@Composable
fun PhotoVerificationDialog(
    missionTitle: String,
    needsConsent: Boolean,
    onConsent: () -> Unit,
    verify: suspend (ByteArray) -> VerificationResult,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            // The privacy notice comes before anything else, so no camera permission is asked without it.
            if (needsConsent) {
                ConsentStep(onAccept = onConsent, onDecline = onDismiss)
            } else {
                PhotoFlow(missionTitle, verify, onVerified, onDismiss)
            }
        }
    }
}

@Composable
private fun ConsentStep(onAccept: () -> Unit, onDecline: () -> Unit) {
    CenteredMessage(
        title = stringResource(R.string.photo_consent_title),
        body = stringResource(R.string.photo_consent_text)
    ) {
        PrimaryButton(
            text = stringResource(R.string.photo_consent_accept),
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth()
        )
        SecondaryButton(
            text = stringResource(R.string.photo_consent_decline),
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PhotoFlow(
    missionTitle: String,
    verify: suspend (ByteArray) -> VerificationResult,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionAsked by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        permissionAsked = true
    }
    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var step by remember { mutableStateOf<Step>(Step.Camera) }

    val networkMessage = stringResource(R.string.photo_failed_network)
    val serviceMessage = stringResource(R.string.photo_failed_service)
    val unreadableMessage = stringResource(R.string.photo_failed_unreadable)
    val limitMessage = stringResource(R.string.photo_failed_limit)
    val notConfiguredMessage = stringResource(R.string.photo_failed_not_configured)
    val rejectedDefault = stringResource(R.string.photo_rejected_default)

    // After a couple of misses the user may finish without a check: a wrong "no" must never block them.
    var failedAttempts by remember { mutableIntStateOf(0) }

    fun check(photo: Bitmap) {
        step = Step.Checking
        scope.launch {
            step = when (val result = verify(photo.toJpegBytes())) {
                VerificationResult.Verified -> {
                    onVerified()
                    Step.Checking
                }

                is VerificationResult.Rejected -> {
                    failedAttempts++
                    Step.Problem(
                        message = result.reason.ifBlank { rejectedDefault },
                        canSkip = failedAttempts >= VerificationLimits.ATTEMPTS_BEFORE_SKIP
                    )
                }

                is VerificationResult.Failed -> {
                    failedAttempts++
                    // Retrying cannot help when the daily cap is reached or the key is missing.
                    val hopeless = result.kind == FailureKind.DAILY_LIMIT || result.kind == FailureKind.NOT_CONFIGURED
                    Step.Problem(
                        message = when (result.kind) {
                            FailureKind.NETWORK -> networkMessage
                            FailureKind.SERVICE -> serviceMessage
                            FailureKind.UNREADABLE_ANSWER -> unreadableMessage
                            FailureKind.DAILY_LIMIT -> limitMessage
                            FailureKind.NOT_CONFIGURED -> notConfiguredMessage
                        },
                        canSkip = hopeless || failedAttempts >= VerificationLimits.ATTEMPTS_BEFORE_SKIP
                    )
                }
            }
        }
    }

    when {
        !granted -> PermissionRequest(
            showSettings = permissionAsked,
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onDismiss = onDismiss
        )

        else -> when (val current = step) {
            Step.Camera -> CameraStep(
                missionTitle = missionTitle,
                onPhoto = { step = Step.Review(it) },
                onDismiss = onDismiss
            )

            is Step.Review -> ReviewStep(
                photo = current.photo,
                onRetake = { step = Step.Camera },
                onUse = { check(current.photo) }
            )

            Step.Checking -> CheckingStep()

            is Step.Problem -> ProblemStep(
                message = current.message,
                canSkip = current.canSkip,
                onRetry = { step = Step.Camera },
                onSkip = onVerified,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun PermissionRequest(showSettings: Boolean, onAllow: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    CenteredMessage(
        title = stringResource(R.string.photo_permission_title),
        body = stringResource(R.string.photo_permission_text)
    ) {
        PrimaryButton(
            text = stringResource(R.string.photo_permission_retry),
            onClick = onAllow,
            modifier = Modifier.fillMaxWidth()
        )
        if (showSettings) {
            SecondaryButton(
                text = stringResource(R.string.photo_permission_settings),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        SecondaryButton(
            text = stringResource(R.string.action_cancel),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CameraStep(missionTitle: String, onPhoto: (Bitmap) -> Unit, onDismiss: () -> Unit) {
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val workExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { workExecutor.shutdown() } }

    var cameraFailed by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var captureFailed by remember { mutableStateOf(false) }

    if (cameraFailed) {
        CenteredMessage(title = stringResource(R.string.photo_camera_error), body = null) {
            SecondaryButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(imageCapture, onFailure = { cameraFailed = true }, modifier = Modifier.fillMaxSize())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.photo_close), tint = Color.White)
            }
            Text(
                text = stringResource(R.string.photo_title_hint, missionTitle),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (captureFailed) {
                Text(
                    text = stringResource(R.string.photo_capture_error),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f)).padding(8.dp)
                )
            }
            ShutterButton(enabled = !capturing) {
                capturing = true
                captureFailed = false
                imageCapture.takeScaledPhoto(workExecutor) { bitmap ->
                    capturing = false
                    if (bitmap != null) onPhoto(bitmap) else captureFailed = true
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture, onFailure: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    LaunchedEffect(lifecycleOwner) {
        try {
            val provider = context.awaitCameraProvider()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
        } catch (e: Exception) {
            onFailure()
        }
    }
    DisposableEffect(Unit) {
        onDispose { runCatching { androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context).get().unbindAll() } }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.photo_take)
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color.Gray)
        )
    }
}

@Composable
private fun ReviewStep(photo: Bitmap, onRetake: () -> Unit, onUse: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryButton(
                text = stringResource(R.string.photo_retake),
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = stringResource(R.string.photo_use),
                onClick = onUse,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CheckingStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.photo_checking),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun ProblemStep(
    message: String,
    canSkip: Boolean,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    CenteredMessage(title = stringResource(R.string.photo_problem_title), body = message) {
        PrimaryButton(
            text = stringResource(R.string.photo_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        )
        if (canSkip) {
            Text(
                text = stringResource(R.string.photo_skip_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            SecondaryButton(
                text = stringResource(R.string.photo_skip),
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
        }
        SecondaryButton(
            text = stringResource(R.string.action_cancel),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CenteredMessage(title: String, body: String?, actions: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
        if (body != null) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
        }
        actions()
    }
}
