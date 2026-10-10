package com.scanora.app

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue

class CameraStreamServer(context: Context, port: Int) : NanoHTTPD(port) {

    val frameQueue = LinkedBlockingQueue<ByteArray>(2)

    override fun serve(session: IHTTPSession): Response {
        val mimeType = "multipart/x-mixed-replace; boundary=--frame"
        val response = newChunkedResponse(Response.Status.OK, mimeType, MJPEGStream())
        response.addHeader("Cache-Control", "no-cache, private, max-age=0, no-store, must-revalidate")
        response.addHeader("Pragma", "no-cache")
        response.addHeader("Expires", "0")
        return response
    }

    private inner class MJPEGStream : InputStream() {
        private var currentData: ByteArray? = null
        private var offset = 0

        override fun read(): Int {
            if (currentData == null || offset >= currentData!!.size) {
                if (!getNextFrame()) return -1
            }
            return currentData!![offset++].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (currentData == null || offset >= currentData!!.size) {
                if (!getNextFrame()) return -1
            }
            val available = currentData!!.size - offset
            val toCopy = Math.min(len, available)
            System.arraycopy(currentData!!, offset, b, off, toCopy)
            offset += toCopy
            return toCopy
        }

        private fun getNextFrame(): Boolean {
            return try {
                val frame = frameQueue.take()
                val header = "\r\n--frame\r\nContent-Type: image/jpeg\r\nContent-Length: ${frame.size}\r\n\r\n"
                val headerBytes = header.toByteArray()
                val combined = ByteArray(headerBytes.size + frame.size)
                System.arraycopy(headerBytes, 0, combined, 0, headerBytes.size)
                System.arraycopy(frame, 0, combined, headerBytes.size, frame.size)
                currentData = combined
                offset = 0
                true
            } catch (e: InterruptedException) {
                false
            }
        }
    }
}
