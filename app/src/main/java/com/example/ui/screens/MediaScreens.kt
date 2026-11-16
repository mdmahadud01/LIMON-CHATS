package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import android.view.WindowManager
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.media.LimonMediaResolver
import com.example.ui.theme.LocalLiquidGlassColors
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.util.UUID

@Composable
fun CameraCaptureScreen(
    onPhotoCaptured: (File) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glass = LocalLiquidGlassColors.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is needed to capture photos.", color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Camera Permission")
                }
            }
        } else {
            AndroidView(
                factory = { ctx -> PreviewView(ctx) },
                update = { previewView ->
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().apply {
                            surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder().build()
                        imageCapture = capture
                        val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        runCatching {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                        }.onFailure { errorMsg = it.localizedMessage }
                    }, ContextCompat.getMainExecutor(context))
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            IconButton(
                onClick = { useFrontCamera = !useFrontCamera },
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera", tint = Color.White)
            }
        }

        errorMsg?.let {
            Text(it, color = Color.Red, modifier = Modifier.align(Alignment.Center))
        }

        if (hasCameraPermission) {
            IconButton(
                onClick = {
                    val outFile = File(context.cacheDir, "cam_${UUID.randomUUID()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onPhotoCaptured(outFile)
                            }
                            override fun onError(exception: ImageCaptureException) {
                                errorMsg = exception.localizedMessage
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(glass.accent)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture photo", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
fun ImageViewerScreen(
    url: String,
    title: String,
    secureMode: Boolean = false,
    onConsumed: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var consumedTriggered by remember { mutableStateOf(false) }

    fun finishViewer() {
        if (secureMode && !consumedTriggered) {
            consumedTriggered = true
            onConsumed?.invoke()
        }
        onBack()
    }

    DisposableEffect(secureMode) {
        val window = (context as? Activity)?.window
        if (secureMode) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (secureMode) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (!consumedTriggered) {
                    consumedTriggered = true
                    onConsumed?.invoke()
                }
            }
        }
    }

    BackHandler { finishViewer() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = url,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 4f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { finishViewer() },
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
            if (secureMode) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xCC0A84FF),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "(1) View Once",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    url: String,
    title: String,
    firestore: FirebaseFirestore? = null,
    secureMode: Boolean = false,
    onConsumed: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val requiresResolution = remember(url) {
        url.startsWith("rtdb://") || url.startsWith("limonmedia://")
    }
    var resolvedVideoUri by remember(url) {
        mutableStateOf<Uri?>(if (requiresResolution) null else Uri.parse(url))
    }
    var loadError by remember(url) { mutableStateOf<String?>(null) }
    var consumedTriggered by remember { mutableStateOf(false) }

    fun finishPlayer() {
        if (secureMode && !consumedTriggered) {
            consumedTriggered = true
            onConsumed?.invoke()
        }
        onBack()
    }

    DisposableEffect(secureMode) {
        val window = (context as? Activity)?.window
        if (secureMode) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (secureMode) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if (!consumedTriggered) {
                    consumedTriggered = true
                    onConsumed?.invoke()
                }
            }
        }
    }

    BackHandler { finishPlayer() }

    LaunchedEffect(url, firestore) {
        if (requiresResolution) {
            val db = firestore ?: runCatching {
                FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
            }.getOrNull()
            val file = LimonMediaResolver.resolveToLocalFile(context, db, url, "video.mp4")
            if (file != null && file.exists()) {
                resolvedVideoUri = Uri.fromFile(file)
            } else {
                loadError = "Unable to stream or cache video."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val playUri = resolvedVideoUri
        if (playUri != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setVideoURI(playUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            loadError = "Video playback error"
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (loadError == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(10.dp))
                Text("Loading video...", color = Color.White.copy(alpha = 0.85f))
            }
        } else {
            Text(
                text = loadError ?: "Video unavailable",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x991E293B),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = { finishPlayer() }, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f).padding(start = 12.dp)
            )
            if (secureMode) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xCC0A84FF),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "(1) View Once",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
