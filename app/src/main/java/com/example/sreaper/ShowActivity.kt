package com.example.sreaper

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.collections.isNotEmpty

class ShowActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var imagePaths: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.show_content)  // Reference to your show_content.xml

        //SetonClickListener
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        //Start


        // Get image paths from Intent
        imagePaths = intent.getStringArrayListExtra("imagePaths") ?: emptyList()

        if (imagePaths.isNotEmpty()) {
            imageAdapter = ImageAdapter(imagePaths)
            recyclerView.adapter = imageAdapter
        } else {
            Toast.makeText(this, "❌ No images found", Toast.LENGTH_SHORT).show()
        }
    }
}
