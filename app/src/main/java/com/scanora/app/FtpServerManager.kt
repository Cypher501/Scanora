package com.scanora.app

import android.content.Context
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.FtpException
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File

class FtpServerManager(private val context: Context) {

    private var ftpServer: org.apache.ftpserver.FtpServer? = null
    val port = 2121
    val username = "scanora"
    val password = "1234"

    fun start(): Boolean {
        return try {
            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory()
            listenerFactory.port = port
            serverFactory.addListener("default", listenerFactory.createListener())

            val userManagerFactory = PropertiesUserManagerFactory()
            val userManager = userManagerFactory.createUserManager()

            val user = BaseUser()
            user.name = username
            user.password = password
            user.homeDirectory = getStudentFilesDir().absolutePath
            user.authorities = listOf(WritePermission())
            user.maxIdleTime = 0
            userManager.save(user)

            serverFactory.userManager = userManager

            ftpServer = serverFactory.createServer()
            ftpServer?.start()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stop() {
        ftpServer?.stop()
        ftpServer = null
    }

    fun isRunning() = ftpServer != null && !ftpServer!!.isStopped
    private fun getStudentFilesDir(): File {
        return android.os.Environment.getExternalStorageDirectory()
    }
}