package com.scanora.app

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import java.io.File

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var pdfView: PDFView
    private lateinit var studentCodeText: TextView

    private var countDownTimer: CountDownTimer? = null
    private var autoScrollHandler: Handler? = null
    private var autoScrollRunnable: Runnable? = null

    private var totalPages = 0
    private var currentPage = 0

    companion object {
        private const val DISPLAY_DURATION_MS = 30 * 1000L
        private const val AUTO_SCROLL_INTERVAL_MS = 3 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        pdfView = findViewById(R.id.pdfView)
        studentCodeText = findViewById(R.id.studentCodeText)

        val pdfPath = intent.getStringExtra("PDF_PATH") ?: run { finish(); return }
        val studentCode = intent.getStringExtra("STUDENT_CODE") ?: ""

        studentCodeText.text = studentCode

        loadPdf(pdfPath)
        startAutoCloseTimer()
    }

    private fun loadPdf(path: String) {
        val sourceFile = File(path)
        if (!sourceFile.exists()) {
            Toast.makeText(this, "PDF not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val cacheFile = File(cacheDir, "temp_student.pdf")
        try {
            sourceFile.copyTo(cacheFile, overwrite = true)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot read PDF: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        pdfView.fromFile(cacheFile)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .enableAnnotationRendering(true)
            .password(null)
            .enableAntialiasing(true)
            .spacing(10)
            .scrollHandle(DefaultScrollHandle(this))
            .onLoad(OnLoadCompleteListener { pageCount ->
                totalPages = pageCount
                currentPage = 0
                startAutoScroll()
            })
            .onError(OnErrorListener { error ->
                Toast.makeText(this, "PDF Error: ${error.message}", Toast.LENGTH_LONG).show()
                finish()
            })
            .onPageError(OnPageErrorListener { page, error ->
                Toast.makeText(this, "Page $page error: ${error?.message}", Toast.LENGTH_SHORT).show()
            })
            .load()
    }

    private fun startAutoScroll() {
        autoScrollHandler = Handler(Looper.getMainLooper())
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (currentPage < totalPages - 1) {
                    currentPage++
                    pdfView.jumpTo(currentPage, true)
                    autoScrollHandler?.postDelayed(this, AUTO_SCROLL_INTERVAL_MS)
                }
            }
        }
        autoScrollHandler?.postDelayed(autoScrollRunnable!!, AUTO_SCROLL_INTERVAL_MS)
    }

    private fun startAutoCloseTimer() {
        countDownTimer = object : CountDownTimer(DISPLAY_DURATION_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                goToHome()
            }
        }.start()
    }

    private fun goToHome() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        autoScrollHandler?.removeCallbacks(autoScrollRunnable!!)
    }
}
