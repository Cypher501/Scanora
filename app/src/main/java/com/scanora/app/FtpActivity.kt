package com.scanora.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FtpActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var ipText: TextView
    private lateinit var infoText: TextView
    private lateinit var toggleButton: Button

    private lateinit var ftpManager: FtpServerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ftp)

        statusText = findViewById(R.id.ftpStatusText)
        ipText = findViewById(R.id.ftpIpText)
        infoText = findViewById(R.id.ftpInfoText)
        toggleButton = findViewById(R.id.ftpToggleButton)

        ftpManager = FtpServerManager(this)

        updateUI(false)

        toggleButton.setOnClickListener {
            if (ftpManager.isRunning()) {
                stopServer()
            } else {
                startServer()
            }
        }
    }

    private fun startServer() {
        val success = ftpManager.start()
        if (success) {
            val ip = getDeviceIp()
            updateUI(true, ip)
            Toast.makeText(this, "FTP Server started!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to start FTP server", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopServer() {
        ftpManager.stop()
        updateUI(false)
        Toast.makeText(this, "FTP Server stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(running: Boolean, ip: String = "") {
        if (running) {
            statusText.text = "● Server Running"
            statusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            ipText.text = "ftp://$ip:${ftpManager.port}"
            infoText.text = """
                Username: ${ftpManager.username}
                Password: ${ftpManager.password}
                
                Open any FTP client on your PC:
                • Windows: File Explorer → type address
                • Mac: Finder → Go → Connect to Server
                • Android: ES File Explorer / Solid Explorer
                
                You can add, delete and modify
                student PDF files directly!
            """.trimIndent()
            toggleButton.text = "Stop FTP Server"
            toggleButton.setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
        } else {
            statusText.text = "● Server Stopped"
            statusText.setTextColor(android.graphics.Color.parseColor("#F44336"))
            ipText.text = "Not running"
            infoText.text = "Start the server to manage student PDF files from your PC or phone on the same WiFi."
            toggleButton.text = "Start FTP Server"
            toggleButton.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        }
    }

    private fun getDeviceIp(): String {
        val wifiManager = applicationContext
            .getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
        return android.text.format.Formatter.formatIpAddress(
            wifiManager.connectionInfo.ipAddress
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        ftpManager.stop()
    }
}