package com.scanora.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.util.concurrent.Executors

class StreamForegroundService : LifecycleService() {

    interface FrameListener {
        fun onFrame(imageProxy: ImageProxy)
    }

    companion object {
        private val listeners = mutableListOf<FrameListener>()

        fun addFrameListener(listener: FrameListener) {
            synchronized(listeners) {
                if (!listeners.contains(listener)) {
                    listeners.add(listener)
                }
            }
        }

        fun removeFrameListener(listener: FrameListener) {
            synchronized(listeners) {
                listeners.remove(listener)
            }
        }
    }

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(1, createNotification())
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val currentListeners = synchronized(listeners) {
                    listeners.toList()
                }

                if (currentListeners.isEmpty()) {
                    imageProxy.close()
                } else {
                    currentListeners.forEach { it.onFrame(imageProxy) }
                    // Note: imageProxy should be closed by the service or listener.
                    // If multiple listeners, it's tricky. 
                    // For now, let's assume the listeners close it or we close it here after notification.
                    // Actually, ImageProxy must be closed to receive next frames.
                    // If we close it here, listeners might lose access.
                    // Since MainActivity is the only one, we can let it close it.
                    // But if there are NO listeners, we closed it above.
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun createNotification(): Notification {
        val channelId = "stream_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Camera Stream Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Scanora Active")
            .setContentText("Camera is scanning for barcodes")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
