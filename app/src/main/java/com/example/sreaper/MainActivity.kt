package com.example.sreaper

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private val sitename = "https://reaperscans.com/series/"
    private var isDownloading = false
    private lateinit var downloadBtn: Button

    private val downloadFinishedReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context?, intent: Intent?) {
            // Update download status and button text when download finishes
            isDownloading = false
            updateDownloadButton()
            saveDownloadState(isDownloading)
        }
    }

    @SuppressLint("SetTextI18n", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadBtn = findViewById(R.id.downloadBtn)
        val urlEdit = findViewById<EditText>(R.id.urlEdit)

        // Load previous URL and download state
        val previousUrl = getSavedUrl()
        urlEdit.setText(previousUrl)
        isDownloading = getDownloadState()

        // Set button text based on download state
        updateDownloadButton()

        downloadBtn.setOnClickListener {
            val urlEditValue = urlEdit.text.toString()
            if (isDownloading) {
                stopDownload()
            } else {
                if (validateUrl(urlEditValue)) {
                    startDownload(urlEditValue)
                }
            }
        }

        // Register receiver for download finished event
        val filter = IntentFilter("com.example.sreaper.DOWNLOAD_FINISHED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadFinishedReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(downloadFinishedReceiver, filter)
        }

        // Navigate to the view downloads activity
        findViewById<Button>(R.id.viewDownloadsBtn).setOnClickListener {
            startActivity(Intent(this, MangaActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(downloadFinishedReceiver)
    }

    // Start the download service
    private fun startDownload(url: String) {
        saveUrl(url)
        val intent = Intent(this, DownloadService::class.java).apply {
            putExtra("seriesUrl", url)
        }

        // Start service based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Update download state and button text
        isDownloading = true
        saveDownloadState(isDownloading)
        updateDownloadButton()
    }

    // Stop the download service
    private fun stopDownload() {
        val intent = Intent(this, DownloadService::class.java).apply {
            action = "ACTION_STOP_DOWNLOAD"
        }
        startService(intent)

        // Update download state and button text
        isDownloading = false
        saveDownloadState(isDownloading)
        updateDownloadButton()
    }

    // Validate the entered URL
    private fun validateUrl(url: String): Boolean {
        return when {
            url.isEmpty() -> {
                showToast("URL cannot be empty!")
                false
            }
            !url.startsWith(sitename) -> {
                showToast("URL should start with $sitename{Series_name}")
                false
            }
            url == sitename -> {
                showToast("Please enter the series name at the end of the URL!")
                false
            }
            else -> true
        }
    }

    // Update the download button text based on the state
    private fun updateDownloadButton() {
        downloadBtn.text = if (isDownloading) "Cancel Download" else "Start Download"
        downloadBtn.isEnabled = !isDownloading // Disable button when downloading
    }

    // Show a Toast message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Save the download state to SharedPreferences
    private fun saveDownloadState(isDownloading: Boolean) {
        getSharedPreferences("app_preferences", MODE_PRIVATE).edit {
            putBoolean("isDownloading", isDownloading)
        }
    }

    // Retrieve the download state from SharedPreferences
    private fun getDownloadState(): Boolean {
        return getSharedPreferences("app_preferences", MODE_PRIVATE)
            .getBoolean("isDownloading", false)
    }

    // Save the entered URL to SharedPreferences
    private fun saveUrl(url: String) {
        getSharedPreferences("app_preferences", MODE_PRIVATE).edit {
            putString("savedUrl", url)
        }
    }

    // Retrieve the saved URL from SharedPreferences
    private fun getSavedUrl(): String {
        return getSharedPreferences("app_preferences", MODE_PRIVATE)
            .getString("savedUrl", sitename) ?: sitename
    }
}
