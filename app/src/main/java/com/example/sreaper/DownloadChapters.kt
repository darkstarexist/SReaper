package com.example.sreaper

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import kotlin.jvm.java

class ChapterDownloader(private val context: Context) {

    private val client = OkHttpClient()

    suspend fun downloadAllChapters(seriesUrl: String, progressBar: ProgressBar, progressText: TextView) {
        withContext(Dispatchers.IO) {
            try {
                val doc = Jsoup.connect(seriesUrl).get()

                // Find the total number of chapters
                val totalChaptersDiv = doc.select("div.flex.justify-between:contains(Total chapters)").firstOrNull()
                val spans = totalChaptersDiv?.select("span")
                val totalChaptersText = spans?.getOrNull(1)?.text()
                val totalChapters = totalChaptersText?.toIntOrNull()

                if (totalChapters == null) {
                    showToast("Couldn't find total chapters.")
                    return@withContext
                }

                // Loop through all chapters and download each one
                for (chapter in 1..totalChapters) {
                    val chapterUrl = "$seriesUrl/chapter-$chapter"
                    val chapterHtml = fetchChapterHtml(chapterUrl)

                    if (chapterHtml != null) {
                        // Call the method to download chapter
                        downloadChapter(chapterUrl, progressBar, progressText, chapterHtml)
                    } else {
                        showToast("Failed to fetch chapter $chapter HTML.")
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun fetchChapterHtml(url: String): String? {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                return response.body?.string()
            } else {
                return null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    private suspend fun downloadChapter(
        chapterUrl: String,
        progressBar: ProgressBar,
        progressText: TextView,
        html: String
    ) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = ProgressBar.VISIBLE
            progressText.visibility = TextView.VISIBLE
            progressBar.progress = 0
            progressText.text = "Progress: 0%"
        }

        try {
            val doc = Jsoup.parse(html)
            val imgTags = doc.select("img")

            // Extract manga name from the page title or URL (before 'chapter')
            val fullTitle = doc.select("title").text().ifEmpty { "chapter" }
            val mangaName = fullTitle.substringBefore(" Chapter").trim().replace("[^a-zA-Z0-9\\s]".toRegex(), "").replace(" ", "_")
            val chapterName = fullTitle.substringAfter("Chapter").trim().replace("[^a-zA-Z0-9\\s]".toRegex(), "")

            // Define the folder name, use mangaName and chapterName to avoid conflicts
            val folderName = "${mangaName}_Chapter_${chapterName}"

            val validImages = imgTags.mapNotNull {
                val src = it.absUrl("src")
                if (src.endsWith(".jpg") || src.endsWith(".png") || src.endsWith(".jpeg")) src else null
            }.distinct()

            if (validImages.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ No images found. Check the URL or site layout.", Toast.LENGTH_SHORT).show()
                    progressText.text = "❌ No images"
                }
                return
            }

            // Create the folder with the corrected name
            val folder = File(context.getExternalFilesDir(null), folderName)
            if (!folder.exists()) folder.mkdir()

            for ((index, src) in validImages.withIndex()) {
                try {
                    val outputFile = File(folder, "%02d.jpg".format(index + 1))
                    if (!outputFile.exists()) {
                        val inputStream = URL(src).openStream()
                        FileOutputStream(outputFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val progressPercent = ((index + 1) * 100) / validImages.size
                withContext(Dispatchers.Main) {
                    progressBar.progress = progressPercent
                    progressText.text = "Progress: $progressPercent%"
                }
            }

            // Prepare the paths for the downloaded images
            val imagePaths = validImages.mapIndexed { index, _ ->
                File(folder, "%02d.jpg".format(index + 1)).absolutePath
            }

            // Launch ShowActivity with the downloaded images
            val intent = Intent(context, ShowActivity::class.java).apply {
                putStringArrayListExtra("imagePaths", ArrayList(imagePaths))
            }

            withContext(Dispatchers.Main) {
                context.startActivity(intent)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("Error", e.message.toString())
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "❌ Download failed", Toast.LENGTH_SHORT).show()
                progressText.text = "❌ Failed"
            }
        }
    }

    private fun showToast(message: String) {
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
