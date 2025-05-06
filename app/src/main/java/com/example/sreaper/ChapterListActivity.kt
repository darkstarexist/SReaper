package com.example.sreaper

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ChapterListActivity : AppCompatActivity() {

    private lateinit var mangaName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapters)

        mangaName = intent.getStringExtra("mangaName") ?: return

        val recyclerView = findViewById<RecyclerView>(R.id.Rv_Chapters)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val chapterList = getChapterFolders(mangaName)

        // Provide onItemClick action when creating the adapter
        val adapter = ChapterAdapter(chapterList) { chapter ->
            // Get all image files in the selected chapter folder
            val chapterDir = File(chapter.path)
            val imageFiles = chapterDir.listFiles()?.filter {
                it.isFile && (it.name.endsWith(".jpg") || it.name.endsWith(".png") || it.name.endsWith(".jpeg"))
            }?.sortedBy {
                // Extract the numeric part from the filename for sorting
                it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE  // Fallback to Int.MAX_VALUE for non-numeric filenames
            }?.map { it.absolutePath } ?: emptyList()

            // Launch ShowActivity with image paths
            val intent = Intent(this, ShowActivity::class.java).apply {
                putStringArrayListExtra("imagePaths", ArrayList(imageFiles))
            }
            startActivity(intent)
        }

        recyclerView.adapter = adapter
    }

    private fun getChapterFolders(mangaName: String): List<Chapter> {
        val mangaDir = File(getExternalFilesDir(null), mangaName)

        // Sorting the directories by the numeric value after "Chapter_"
        return mangaDir.listFiles()?.filter { it.isDirectory }
            ?.sortedBy {
                // Extract the number after "Chapter_" and parse it as an integer
                it.name.substringAfter("Chapter_").toIntOrNull() ?: Int.MAX_VALUE
            }
            ?.map {
                // Replace underscores with spaces in the chapter name
                val chapterName = it.name.replace("_", " ")
                Chapter(chapterName, it.absolutePath)  // Mapping directories to Chapter objects
            } ?: emptyList()
    }

}
