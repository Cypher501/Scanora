package com.scanora.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private var isProcessing = false
    private lateinit var cameraExecutor: ExecutorService
    private var frameListener: StreamForegroundService.FrameListener? = null
    private lateinit var viewFinder: PreviewView

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestAllPermissions()
    }

    private fun requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(
                    this,
                    "Please allow 'All files access' for Scanora",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            }
        }

        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
            return
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                return
            }
        }

        startCameraPreview()
        subscribeToFrames()
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // ── Subscribe to frames from the always-running service ──
    private fun subscribeToFrames() {
        frameListener = object : StreamForegroundService.FrameListener {
            override fun onFrame(imageProxy: ImageProxy) {
                processImageProxy(imageProxy)
            }
        }
        StreamForegroundService.addFrameListener(frameListener!!)
    }

    override fun onResume() {
        super.onResume()
        isProcessing = false
        // Re-subscribe when returning to this screen
        if (frameListener != null) {
            StreamForegroundService.addFrameListener(frameListener!!)
        } else if (hasCameraPermission()) {
            subscribeToFrames()
        }
    }

    override fun onPause() {
        super.onPause()
        // Unsubscribe when leaving — stop processing frames
        frameListener?.let {
            StreamForegroundService.removeFrameListener(it)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestAllPermissions()
        } else {
            Toast.makeText(this, "Permission required!", Toast.LENGTH_LONG).show()
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()

        val scanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    for (barcode in barcodes) {
                        val rawCode = barcode.rawValue ?: continue
                        handleBarcodeDetected(rawCode)
                        break
                    }
                }
            }
            .addOnFailureListener {
                // Silent fail
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleBarcodeDetected(rawCode: String) {
        isProcessing = true

        val safeCode = rawCode.trim().replace("/", "_")

        val pdfFile = File(
            Environment.getExternalStorageDirectory(),
            "Documents/StudentFiles/$safeCode.pdf"
        )

        runOnUiThread {
            if (pdfFile.exists()) {
                val intent = Intent(this, PdfViewerActivity::class.java)
                intent.putExtra("PDF_PATH", pdfFile.absolutePath)
                intent.putExtra("STUDENT_CODE", rawCode)
                startActivity(intent)
            } else {
                Toast.makeText(
                    this,
                    "No PDF found for: $rawCode",
                    Toast.LENGTH_SHORT
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    isProcessing = false
                }, 2000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        frameListener?.let {
            StreamForegroundService.removeFrameListener(it)
        }
        cameraExecutor.shutdown()
    }
}
