package com.scanora.app

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class PhotoServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri

        return when {
            uri == "/" || uri == "/index.html" -> serveIndexPage()
            uri.startsWith("/photos/") -> servePhoto(uri)
            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Not found"
            )
        }
    }

    private fun serveIndexPage(): Response {
        val photoDir = File(context.getExternalFilesDir(null), "StudentPhotos")
        val photos = photoDir.listFiles()
            ?.filter { it.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val html = buildString {
            append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Scanora — Student Photos</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body { 
                            font-family: sans-serif; 
                            background: #0f0f0f; 
                            color: #fff;
                            padding: 20px;
                        }
                        h1 { 
                            text-align: center; 
                            margin-bottom: 8px;
                            font-size: 24px;
                            color: #4fc3f7;
                        }
                        p.count {
                            text-align: center;
                            color: #888;
                            margin-bottom: 24px;
                            font-size: 14px;
                        }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
                            gap: 16px;
                        }
                        .card {
                            background: #1e1e1e;
                            border-radius: 12px;
                            overflow: hidden;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.4);
                        }
                        .card img {
                            width: 100%;
                            height: 200px;
                            object-fit: cover;
                            display: block;
                        }
                        .card .info {
                            padding: 10px;
                        }
                        .card .name {
                            font-size: 13px;
                            font-weight: bold;
                            color: #4fc3f7;
                            white-space: nowrap;
                            overflow: hidden;
                            text-overflow: ellipsis;
                        }
                        .card .time {
                            font-size: 11px;
                            color: #666;
                            margin-top: 4px;
                        }
                        .empty {
                            text-align: center;
                            color: #555;
                            margin-top: 80px;
                            font-size: 16px;
                        }
                    </style>
                </head>
                <body>
                    <h1>Scanora Student Photos</h1>
                    <p class="count">${photos.size} photo(s) captured</p>
            """.trimIndent())

            if (photos.isEmpty()) {
                append("""<div class="empty">No photos yet.<br>Scan a student ID to capture.</div>""")
            } else {
                append("""<div class="grid">""")
                for (photo in photos) {
                    val name = photo.nameWithoutExtension.replace("_", "/")
                    val time = java.text.SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(photo.lastModified()))

                    append("""
                        <div class="card">
                            <a href="/photos/${photo.name}" target="_blank">
                                <img src="/photos/${photo.name}" alt="$name"/>
                            </a>
                            <div class="info">
                                <div class="name">$name</div>
                                <div class="time">$time</div>
                            </div>
                        </div>
                    """.trimIndent())
                }
                append("""</div>""")
            }

            append("""
                </body>
                </html>
            """.trimIndent())
        }

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun servePhoto(uri: String): Response {
        val fileName = uri.removePrefix("/photos/")
        val photoDir = File(context.getExternalFilesDir(null), "StudentPhotos")
        val file = File(photoDir, fileName)

        return if (file.exists()) {
            val fis = FileInputStream(file)
            newFixedLengthResponse(
                Response.Status.OK, "image/jpeg", fis, file.length()
            )
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Photo not found")
        }
    }
}