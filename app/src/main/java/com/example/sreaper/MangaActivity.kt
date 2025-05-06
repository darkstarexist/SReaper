package com.example.sreaper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class MangaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        // Set up the RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.RecycleView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Fetch manga folder names
        val folderNames = getAllMangaFolders()

        // Set up the adapter with original names (with underscores)
        val adapter = MangaAdapter(folderNames) { selectedFolder ->
            // On folder click, open the ChapterListActivity with the original folder name (with underscores)
            val intent = Intent(this, ChapterListActivity::class.java).apply {
                putExtra("mangaName", selectedFolder) // Pass the original folder name (with underscores)
            }
            startActivity(intent)
        }

        recyclerView.adapter = adapter
    }

    // Fetch all manga folders and clean their names by replacing underscores with spaces only for display
    private fun getAllMangaFolders(): List<String> {
        val parentDir = getExternalFilesDir(null) ?: return emptyList()

        // Get all directories, map their names, and replace underscores with spaces for display purposes only
        return parentDir.listFiles()?.filter { it.isDirectory }
            ?.map { it.name } // Replace underscores with spaces for display
            ?: emptyList()
    }
}
