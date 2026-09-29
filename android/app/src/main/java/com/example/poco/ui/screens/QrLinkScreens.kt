package com.example.poco.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.poco.ui.components.PocoTextField
import com.example.poco.ui.components.PocoTopBar
import com.example.poco.ui.components.PrimaryButton
import com.example.poco.ui.theme.POCOTheme
import com.example.poco.ui.theme.PocoCardBackground
import com.example.poco.ui.theme.PocoGreen
import com.example.poco.ui.theme.PocoTextMuted
import com.example.poco.ui.theme.PocoTextPrimary
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.Executors
import kotlin.random.Random

/** 사용자 기기에 표시되는 연동 코드 화면. 보호자가 이 코드를 스캔해 계정을 연결한다.
 *  code가 null이면 서버에서 아직 발급 중이라는 뜻이라 안내 문구만 보여준다. */
@Composable
fun QrShowScreen(
    code: String?,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "보호자 앱에서\n이 코드를 스캔해주세요",
                    color = PocoTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                val qrBitmap = code?.let { rememberQrBitmap(it) }
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "연동 QR 코드",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PocoCardBackground)
                            .padding(16.dp)
                    )
                } else {
                    QrPlaceholder(seed = 42)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = code?.let { "연동 코드: $it" } ?: "코드 발급 중...",
                    color = PocoTextMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(40.dp))
                PrimaryButton(text = "연동 완료", onClick = onDone)
            }
        }
    }
}

/** 보호자 기기의 스캔 화면. 초록 프레임 안에 실제 카메라 미리보기를 띄우고 ML Kit으로 QR을 읽는다.
 *  카메라 권한이 없거나 인식이 어려우면 코드를 직접 입력할 수도 있다. */
@Composable
fun QrScanScreen(
    onScanned: (code: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    Surface(modifier = modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "", onBack = onBack, contentColor = Color.White)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "사용자 기기에 뜬\nQR을 카메라에 비춰주세요",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, PocoGreen, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        QrCameraPreview(
                            onScanned = onScanned,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = PocoGreen,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                if (!hasCameraPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = "카메라 권한 허용하기",
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "또는 코드 직접 입력",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                PocoTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = "연동 코드",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryButton(text = "연동하기", onClick = { onScanned(code) }, enabled = code.isNotBlank())
            }
        }
    }
}

/** CameraX Preview + ML Kit 바코드 분석기로 QR을 실시간으로 읽는다.
 *  하나 인식되면 즉시 콜백을 부르고, 화면을 벗어나면 카메라 바인딩을 해제한다. */
@Composable
private fun QrCameraPreview(
    onScanned: (code: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasScanned by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = Executors.newSingleThreadExecutor()
            val scanner = BarcodeScanning.getClient()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = CameraPreview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null || hasScanned) {
                        imageProxy.close()
                    } else {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val value = barcodes.firstOrNull()?.rawValue
                                if (value != null && !hasScanned) {
                                    hasScanned = true
                                    onScanned(value)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }
}

/** userId에서 생성된 연동 코드를 실제 QR 이미지로 인코딩한다 (표시 전용, 스캔 쪽 인코딩은 ML Kit이 처리). */
@Composable
private fun rememberQrBitmap(content: String, sizePx: Int = 480) =
    remember(content) {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    }

/** 화면에 보여주는 한글 라벨 -> 서버가 받는 relationLabel(ENUM) 값. */
private val RELATION_OPTIONS = listOf(
    "가족" to "FAMILY",
    "요양보호사" to "CAREGIVER",
    "친구" to "FRIEND",
    "기타" to "OTHER"
)

/** QR 스캔 완료 직후, 보호자가 피보호자와의 관계를 선택하는 화면. */
@Composable
fun RelationSelectScreen(
    onComplete: (relationLabel: String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<String?>(null) }

    val isValid = selected != null

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            PocoTopBar(title = "관계 선택", onBack = onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "연동하는 분과 어떤 관계인가요?",
                    color = PocoTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                RELATION_OPTIONS.chunked(2).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowOptions.forEach { (label, value) ->
                            RelationOptionCard(
                                label = label,
                                isSelected = selected == value,
                                onClick = { selected = value },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowOptions.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                PrimaryButton(
                    text = "완료",
                    onClick = { onComplete(selected.orEmpty()) },
                    enabled = isValid
                )
            }
        }
    }
}

@Composable
private fun RelationOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) PocoGreen.copy(alpha = 0.12f) else PocoCardBackground)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) PocoGreen else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) PocoGreen else PocoTextPrimary,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun QrPlaceholder(seed: Int) {
    val random = remember(seed) { Random(seed) }
    val gridSize = 16
    val finderSize = 4
    val isFinderCell = { row: Int, col: Int ->
        (row < finderSize && col < finderSize) ||
            (row < finderSize && col >= gridSize - finderSize) ||
            (row >= gridSize - finderSize && col < finderSize)
    }
    val cells = remember(seed) {
        List(gridSize * gridSize) { index ->
            val row = index / gridSize
            val col = index % gridSize
            if (isFinderCell(row, col)) false else random.nextFloat() > 0.55f
        }
    }
    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PocoCardBackground)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.minDimension / gridSize

            fun drawFinderPattern(originRow: Int, originCol: Int) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(originCol * cellSize, originRow * cellSize),
                    size = Size(cellSize * finderSize, cellSize * finderSize)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset((originCol + 0.7f) * cellSize, (originRow + 0.7f) * cellSize),
                    size = Size(cellSize * (finderSize - 1.4f), cellSize * (finderSize - 1.4f))
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset((originCol + 1.3f) * cellSize, (originRow + 1.3f) * cellSize),
                    size = Size(cellSize * (finderSize - 2.6f), cellSize * (finderSize - 2.6f))
                )
            }

            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    if (cells[row * gridSize + col]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(col * cellSize, row * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f)
                        )
                    }
                }
            }

            drawFinderPattern(0, 0)
            drawFinderPattern(0, gridSize - finderSize)
            drawFinderPattern(gridSize - finderSize, 0)
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun QrShowScreenPreview() {
    POCOTheme { QrShowScreen(code = "7QK2-90LX", onDone = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun QrScanScreenPreview() {
    POCOTheme { QrScanScreen(onScanned = {}, onBack = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun RelationSelectScreenPreview() {
    POCOTheme { RelationSelectScreen(onComplete = {}, onBack = {}) }
}
