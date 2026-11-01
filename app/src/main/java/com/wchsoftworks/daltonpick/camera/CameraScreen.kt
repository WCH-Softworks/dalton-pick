package com.wchsoftworks.daltonpick.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.colorengine.Lang
import com.wchsoftworks.daltonpick.colorengine.Srgb
import com.wchsoftworks.daltonpick.settings.Settings
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import com.wchsoftworks.daltonpick.ui.SystemActions
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun CameraScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        askedOnce = true
    }
    // The user may grant it in App info and come back.
    LifecycleResumeEffect(Unit) {
        granted = hasCameraPermission(context)
        onPauseOrDispose { }
    }

    if (granted) {
        CameraContent(onClose)
    } else {
        // After a denial with "don't ask again", Android won't show the dialog: send people to App info.
        val canAsk = !askedOnce || (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true
        PermissionRationale(
            canAsk = canAsk,
            onAllow = { launcher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = { SystemActions.openAppInfo(context) },
            onClose = onClose,
        )
    }
}

private fun hasCameraPermission(context: android.content.Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
private fun PermissionRationale(canAsk: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit, onClose: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.camera_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.camera_permission_body), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.camera_permission_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (canAsk) {
                Button(onClick = onAllow) { Text(stringResource(R.string.camera_allow)) }
            } else {
                Text(stringResource(R.string.camera_permission_denied), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onOpenSettings) { Text(stringResource(R.string.btn_app_info)) }
            }
            OutlinedButton(onClick = onClose) { Text(stringResource(R.string.camera_close)) }
        }
    }
}

@Composable
private fun CameraContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val lang = Lang.fromTag(LocalConfiguration.current.locales[0].toLanguageTag())
    val settings by remember { SettingsRepository.get(context).flow }.collectAsStateWithLifecycle(initialValue = Settings())

    val lens = remember { CameraLens(lang) }
    LaunchedEffect(lang) {
        lens.lang = lang
        lens.reset()
    }
    val reading by lens.reading.collectAsStateWithLifecycle()

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView-backed, so the frame can be read back when frozen.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<Bitmap?>(null) }
    var crosshair by remember { mutableStateOf<Offset?>(null) } // null = center of the view
    var zoom by remember { mutableFloatStateOf(1f) }

    // Bind preview + analysis, sharing the preview's viewport so the analysis crop matches what's on screen.
    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            val p = future.get()
            provider = p
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, lens) }
            val group = UseCaseGroup.Builder().addUseCase(preview).addUseCase(analysis)
            previewView.viewPort?.let { group.setViewPort(it) }
            p.unbindAll()
            camera = try {
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, group.build())
            } catch (e: IllegalArgumentException) {
                // No back camera (some tablets): fall back to whatever is there.
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, group.build())
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            provider?.unbindAll()
            executor.shutdown()
        }
    }

    // Tap the label to hear the name (spec §4.4). The system speech engine, in the app's language.
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) tts = engine }
        onDispose { engine.shutdown() }
    }
    val speak: () -> Unit = {
        reading?.let { r ->
            tts?.language = Locale.forLanguageTag(lang.tag)
            tts?.speak(r.label.name, TextToSpeech.QUEUE_FLUSH, null, "name")
        }
    }

    // A tick when the name changes, like the overlay.
    var lastName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(reading?.label?.name) {
        val name = reading?.label?.name ?: return@LaunchedEffect
        if (lastName != null && name != lastName && settings.haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        lastName = name
    }

    // TalkBack hears the name only once it has held still for a moment, not on every frame.
    var settledName by remember { mutableStateOf("") }
    LaunchedEffect(reading?.label) {
        val label = reading?.label ?: return@LaunchedEffect
        delay(SETTLE_MS)
        settledName = if (label.detail.isEmpty()) label.name else "${label.name}, ${label.detail}"
    }

    fun freeze() {
        val bitmap = previewView.bitmap ?: return
        lens.frozen = true
        frozen = bitmap
        crosshair = null
        lens.sampleFrozen(bitmap, bitmap.width / 2, bitmap.height / 2)
    }

    fun resume() {
        lens.frozen = false
        lens.reset()
        frozen = null
        crosshair = null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        frozen?.let { bitmap ->
            Image(
                bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Gestures: pinch to zoom while live; drag the crosshair over the held picture.
        val ringRadius = with(density) { (settings.lensSizeDp / 2).dp.toPx() }
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(frozen) {
                    if (frozen == null) {
                        detectTransformGestures { _, _, gestureZoom, _ ->
                            val state = camera?.cameraInfo?.zoomState?.value ?: return@detectTransformGestures
                            zoom = (zoom * gestureZoom).coerceIn(state.minZoomRatio, state.maxZoomRatio)
                            camera?.cameraControl?.setZoomRatio(zoom)
                        }
                    } else {
                        detectDragGestures { _, drag ->
                            val bitmap = frozen ?: return@detectDragGestures
                            val start = crosshair ?: Offset(size.width / 2f, size.height / 2f)
                            val next = Offset(
                                (start.x + drag.x).coerceIn(0f, size.width - 1f),
                                (start.y + drag.y).coerceIn(0f, size.height - 1f),
                            )
                            crosshair = next
                            lens.sampleFrozen(
                                bitmap,
                                (next.x * bitmap.width / size.width).toInt(),
                                (next.y * bitmap.height / size.height).toInt(),
                            )
                        }
                    }
                },
        ) {
            val center = crosshair ?: Offset(size.width / 2f, size.height / 2f)
            drawLensRing(center, ringRadius)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x99000000), contentColor = Color.White),
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                }
                Spacer(Modifier.weight(1f))
                if (zoom > 1.05f) ZoomBadge(zoom)
            }
            if (settings.cameraOpens <= LIGHTING_HINT_OPENS) {
                Hint(stringResource(if (frozen == null) R.string.camera_lighting_hint else R.string.camera_frozen_hint))
            } else if (frozen != null) {
                Hint(stringResource(R.string.camera_frozen_hint))
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NameLabel(reading, settings.showHex, settledName, speak)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (frozen == null) {
                    Button(onClick = ::freeze) { Text(stringResource(R.string.camera_freeze)) }
                } else {
                    Button(onClick = ::resume) { Text(stringResource(R.string.camera_resume)) }
                }
                if (camera?.cameraInfo?.hasFlashUnit() == true && frozen == null) {
                    FilledTonalButton(onClick = {
                        torchOn = !torchOn
                        camera?.cameraControl?.enableTorch(torchOn)
                    }) {
                        Text(stringResource(if (torchOn) R.string.camera_torch_off else R.string.camera_torch_on))
                    }
                }
            }
        }
    }
}

/** The overlay's look: dark-and-light ring, gapped crosshair, a box around the sampled spot. */
private fun DrawScope.drawLensRing(center: Offset, radius: Float) {
    val d = density
    for ((color, width) in listOf(Color(0xE6000000) to 4f * d, Color.White to 2f * d)) {
        drawCircle(color, radius, center, style = Stroke(width))
    }
    val box = 6f * d
    val gap = box + 3f * d
    val arm = 12f * d
    for ((color, width) in listOf(Color(0xCC000000) to 3f * d, Color.White to 1.25f * d)) {
        val stroke = Stroke(width)
        drawRect(color, Offset(center.x - box, center.y - box), Size(box * 2, box * 2), style = stroke)
        drawLine(color, Offset(center.x - gap - arm, center.y), Offset(center.x - gap, center.y), width)
        drawLine(color, Offset(center.x + gap, center.y), Offset(center.x + gap + arm, center.y), width)
        drawLine(color, Offset(center.x, center.y - gap - arm), Offset(center.x, center.y - gap), width)
        drawLine(color, Offset(center.x, center.y + gap), Offset(center.x, center.y + gap + arm), width)
    }
}

/** Same pill as the overlay label: swatch, name, family and tone, optional hex. Tap to hear it. */
@Composable
private fun NameLabel(reading: Reading?, showHex: Boolean, settledName: String, onSpeak: () -> Unit) {
    val hint = stringResource(R.string.camera_tap_to_speak)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF0202020),
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClickLabel = hint, onClick = onSpeak)
            .semantics {
                contentDescription = settledName
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .background(reading?.let { Color(0xFF000000 or it.rgb.toLong()) } ?: Color.DarkGray, CircleShape),
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    reading?.label?.name ?: stringResource(R.string.camera_starting),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                reading?.label?.detail?.takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE0E0E0))
                }
                if (showHex && reading != null) {
                    val rgb = reading.rgb
                    Text(
                        "${Srgb.toHex(rgb)} · ${rgb shr 16 and 0xFF}, ${rgb shr 8 and 0xFF}, ${rgb and 0xFF}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFB8B8B8),
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC000000),
        contentColor = Color.White,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun ZoomBadge(zoom: Float) {
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0x99000000), contentColor = Color.White) {
        Text("%.1f×".format(zoom), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

/** How long the name must hold before TalkBack reads it. Matches the overlay. */
private const val SETTLE_MS = 700L

/** The lighting hint shows on the first few opens only (spec §4.4). */
private const val LIGHTING_HINT_OPENS = 3
