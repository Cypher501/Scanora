package com.scanora.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.io.File

class ExamCircularsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var countText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exam_circulars)

        recyclerView = findViewById(R.id.circularsRecyclerView)
        emptyText    = findViewById(R.id.emptyText)
        countText    = findViewById(R.id.countText)

        loadCirculars()
    }

    override fun onResume() {
        super.onResume()
        loadCirculars()
    }

    private fun loadCirculars() {
        val examsDir = File(
            Environment.getExternalStorageDirectory(),
            "Documents/Exams"
        )
        if (!examsDir.exists()) examsDir.mkdirs()

        val pdfs = examsDir.listFiles()
            ?.filter { it.extension.lowercase() == "pdf" }
            ?.sortedBy { it.name }
            ?: emptyList()

        countText.text = "${pdfs.size} circular(s) found"

        if (pdfs.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            recyclerView.layoutManager = GridLayoutManager(this, 2)
            recyclerView.adapter = CircularAdapter(pdfs) { file ->
                val intent = Intent(this, PdfViewerActivity::class.java)
                intent.putExtra("PDF_PATH", file.absolutePath)
                intent.putExtra("STUDENT_CODE", file.nameWithoutExtension)
                startActivity(intent)
            }
        }
    }
}

class CircularAdapter(
    private val files: List<File>,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<CircularAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.circularName)
        val iconText: TextView = view.findViewById(R.id.circularIcon)
        val sizeText: TextView = view.findViewById(R.id.circularSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_circular, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.nameText.text = file.nameWithoutExtension
        holder.sizeText.text = "%.1f MB".format(file.length() / 1024.0 / 1024.0)

        // Pick emoji based on name keywords
        holder.iconText.text = when {
            file.name.contains("exam", ignoreCase = true)      -> "📝"
            file.name.contains("place", ignoreCase = true)     -> "📍"
            file.name.contains("room", ignoreCase = true)      -> "🚪"
            file.name.contains("dept", ignoreCase = true)      -> "🏫"
            file.name.contains("result", ignoreCase = true)    -> "📊"
            file.name.contains("schedule", ignoreCase = true)  -> "📅"
            file.name.contains("notice", ignoreCase = true)    -> "📢"
            file.name.contains("circular", ignoreCase = true)  -> "📋"
            else                                                -> "📄"
        }

        holder.itemView.setOnClickListener { onClick(file) }
    }

    override fun getItemCount() = files.size
}