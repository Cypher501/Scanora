package com.scanora.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class AdActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var dotContainer: LinearLayout
    private lateinit var adCountText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0
    private var adImages: List<File> = emptyList()

    private val AUTO_CLOSE_MS = 5 * 60 * 1000L
    private val SLIDE_INTERVAL_MS = 4 * 1000L

    private val autoCloseRunnable = Runnable { finish() }

    private val slideRunnable = object : Runnable {
        override fun run() {
            if (adImages.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % adImages.size
                showImage(currentIndex)
                handler.postDelayed(this, SLIDE_INTERVAL_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ad)

        imageView    = findViewById(R.id.adImageView)
        dotContainer = findViewById(R.id.dotContainer)
        adCountText  = findViewById(R.id.adCountText)

        loadAdImages()

        if (adImages.isNotEmpty()) {
            showImage(0)
            setupDots()
            handler.postDelayed(slideRunnable, SLIDE_INTERVAL_MS)
        } else {
            adCountText.text = "No ads found\nPlace JPG files in:\nDocuments/Ads/"
        }

        handler.postDelayed(autoCloseRunnable, AUTO_CLOSE_MS)
    }

    private fun loadAdImages() {
        val adsDir = File(
            android.os.Environment.getExternalStorageDirectory(),
            "Documents/Ads"
        )
        if (!adsDir.exists()) adsDir.mkdirs()

        adImages = adsDir.listFiles()
            ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "mp4") }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun showImage(index: Int) {
        val file = adImages[index]
        val ext  = file.extension.lowercase()

        val slideIn = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        imageView.startAnimation(slideIn)

        if (ext == "mp4") {
            // Show video thumbnail or just a placeholder
            imageView.setImageResource(android.R.drawable.ic_media_play)
        } else {
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
        }

        updateDots(index)
        adCountText.text = "${index + 1} / ${adImages.size}"
    }

    private fun setupDots() {
        dotContainer.removeAllViews()
        for (i in adImages.indices) {
            val dot = TextView(this)
            dot.text = "●"
            dot.textSize = 10f
            dot.setPadding(6, 0, 6, 0)
            dot.setTextColor(
                if (i == 0) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#88FFFFFF")
            )
            dotContainer.addView(dot)
        }
    }

    private fun updateDots(activeIndex: Int) {
        for (i in 0 until dotContainer.childCount) {
            val dot = dotContainer.getChildAt(i) as TextView
            dot.setTextColor(
                if (i == activeIndex) android.graphics.Color.WHITE
                else android.graphics.Color.parseColor("#88FFFFFF")
            )
        }
    }

    // Tap anywhere to exit
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(autoCloseRunnable)
        handler.removeCallbacks(slideRunnable)
    }
}