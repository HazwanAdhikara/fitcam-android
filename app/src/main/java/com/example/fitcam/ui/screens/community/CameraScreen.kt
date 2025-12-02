package com.example.fitcam.ui.screens.community

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitcam.ui.theme.FitCamBlue
import com.example.fitcam.ui.theme.FitCamRed
import com.example.fitcam.ui.theme.FitCamYellow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import android.graphics.BitmapFactory
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.FileOutputStream
import androidx.compose.ui.graphics.asImageBitmap


@Composable
fun CameraScreen(
    viewModel: CommunityViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        CameraContent(viewModel)
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required.", color = FitCamRed)
        }
    }
}

@Composable
fun CameraContent(viewModel: CommunityViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lastSession by viewModel.lastSession.collectAsState()
    val uploadStatus by viewModel.uploadStatus.collectAsState()

    // STATE BARU: Menyimpan foto yang baru diambil
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Jika sudah ada foto captured -> Tampilkan PREVIEW MODE
    if (capturedBitmap != null) {
        PostPreviewScreen(
            bitmap = capturedBitmap!!,
            uploadStatus = uploadStatus,
            onRetake = { 
                capturedBitmap = null
                viewModel.resetStatus()
            },
            onPost = { 
                // TIDAK PERLU drawStatsOnBitmap lagi, karena capturedBitmap sudah ada watermarknya
                viewModel.uploadPost(capturedBitmap!!, lastSession ?: com.example.fitcam.data.WorkoutSession("Unknown",0,0,0,0))
            },
            onShare = {
                // Langsung share bitmap yang ada
                shareImage(context, capturedBitmap!!)
            }
        )

    } else {
        CameraMode(viewModel, lifecycleOwner, lastSession) { bitmap ->
            capturedBitmap = bitmap
        }
    }
}

@Composable
fun PostPreviewScreen(
    bitmap: android.graphics.Bitmap,
    uploadStatus: String,
    onRetake: () -> Unit,
    onPost: () -> Unit,
    onShare: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // Tampilkan Gambar
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        // Overlay Loading / Success
        if (uploadStatus == "Uploading") {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.7f)), Alignment.Center) {
                CircularProgressIndicator(color = FitCamYellow)
            }
        } else if (uploadStatus == "Success") {
            AlertDialog(
                onDismissRequest = onRetake,
                confirmButton = { Button(onClick = onRetake) { Text("Back to Camera") } },
                title = { Text("Posted!") },
                text = { Text("Your workout is now on the community feed.") }
            )
        }

        // Tombol Aksi (Bawah)
        Column(
            Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onRetake,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Retake")
                }
                
                Button(
                    onClick = onShare, // SOCIAL MEDIA SHARE
                    colors = ButtonDefaults.buttonColors(containerColor = FitCamBlue)
                ) {
                    Text("Share Externally")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onPost, // FIREBASE UPLOAD
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FitCamRed)
            ) {
                Text("POST TO COMMUNITY")
            }
        }
    }
}

// --- REFACTOR: CAMERA MODE ---
@Composable
fun CameraMode(
    viewModel: CommunityViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    lastSession: com.example.fitcam.data.WorkoutSession?,
    onImageCaptured: (android.graphics.Bitmap) -> Unit
) {
    val context = LocalContext.current
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (e: Exception) { Log.e("FitCam", "Bind failed", e) }
        }, cameraExecutor)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, Modifier.fillMaxSize())
        
        // OVERLAY (Hanya Visual)
        if (lastSession != null) {
            val statsText = viewModel.getOverlayText(lastSession)
            Box(
                modifier = Modifier
                    .padding(top = 48.dp, start = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .border(1.dp, FitCamYellow, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(statsText.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // BUTTONS
        Row(
            Modifier.align(Alignment.BottomCenter).padding(32.dp), 
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             IconButton(
                onClick = {
                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                },
                modifier = Modifier.background(Color.White.copy(0.2f), CircleShape)
            ) { Icon(Icons.Default.Cameraswitch, null, tint = Color.White) }

            Button(
                onClick = {
                    captureImage(context, imageCapture, cameraExecutor) { file ->
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        
                        // 1. Fix Rotation
                        val matrix = android.graphics.Matrix()
                        matrix.postRotate(90f) 
                        val rotatedBitmap = android.graphics.Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )

                        // 2. LANGSUNG TEMPEL WATERMARK DI SINI
                        // Kita cek apakah ada lastSession, jika ada, gambar ulang bitmapnya
                        val finalBitmap = if (lastSession != null) {
                            // Panggil fungsi drawStatsOnBitmap yang sudah kita buat di ImageUtils.kt
                            drawStatsOnBitmap(context, rotatedBitmap, viewModel.getOverlayText(lastSession))
                        } else {
                            rotatedBitmap
                        }
                        
                        // 3. Kirim Bitmap yang SUDAH ada watermarknya ke Preview
                        onImageCaptured(finalBitmap) 
                    }
                },
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = FitCamRed),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color.White)
            ) {}
            
            Spacer(Modifier.size(48.dp))
        }
    }
}

// Fungsi Helper untuk Capture Foto Sementara
private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onSuccess: (File) -> Unit // New Callback
) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val file = File(context.cacheDir, "fitcam_$name.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) { Log.e("FitCam", "Error", exc) }
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                onSuccess(file)
            }
        }
    })
}

fun shareImage(context: Context, bitmap: android.graphics.Bitmap) {
    try {
        // 1. Simpan bitmap ke file cache
        val cachePath = File(context.cacheDir, "images") // Buat subfolder biar rapi
        cachePath.mkdirs() // Wajib buat foldernya dulu
        
        val file = File(cachePath, "share_image.png") // Gunakan PNG biar bening
        val stream = FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        // 2. Dapatkan URI lewat FileProvider
        // PERHATIKAN STRUKTUR INI: "com.example.fitcam.provider"
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        // 3. Buat Intent Share
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        // 4. Jalankan Chooser
        context.startActivity(Intent.createChooser(intent, "Share your workout!"))
        
    } catch (e: Exception) {
        e.printStackTrace()
        // Log error biar tau salahnya dimana
        Log.e("ShareError", "Gagal share: ${e.message}")
        Toast.makeText(context, "Error sharing image", Toast.LENGTH_SHORT).show()
    }
}
