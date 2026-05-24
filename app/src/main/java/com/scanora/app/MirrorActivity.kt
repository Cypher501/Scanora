package com.scanora.app

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

class MirrorActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private val mainHandler = Handler(Looper.getMainLooper())
    private val AUTO_CLOSE_MS = 2 * 60 * 1000L
    private val autoCloseRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mirror)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = findViewById(R.id.mirrorSurfaceView)

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                openCamera(holder.surface)
            }
            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int, height: Int
            ) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                captureSession?.close()
                cameraDevice?.close()
            }
        })

        mainHandler.postDelayed(autoCloseRunnable, AUTO_CLOSE_MS)
    }

    private fun openCamera(surface: Surface) {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = manager.cameraIdList.firstOrNull() ?: return
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startPreview(camera, surface)
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    finish()
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPreview(camera: CameraDevice, surface: Surface) {
        try {
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)

            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        session.setRepeatingRequest(
                            builder.build(), null, backgroundHandler
                        )
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        finish()
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundThread = HandlerThread("MirrorBg").also { it.start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    override fun onPause() {
        super.onPause()
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        backgroundThread.quitSafely()
        mainHandler.removeCallbacks(autoCloseRunnable)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(autoCloseRunnable)
    }
}