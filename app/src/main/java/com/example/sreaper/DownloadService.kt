package com.example.sreaper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import kotlin.system.measureTimeMillis

class DownloadService : Service() {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val client = OkHttpClient()
    private val channelId = "download_channel"

    private val downloadedUrls = mutableSetOf<String>()
    private val semaphore = Semaphore(permits = 4) // Limit parallel downloads

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP_DOWNLOAD") {
            job.cancel()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        val seriesUrl = intent?.getStringExtra("seriesUrl") ?: return START_NOT_STICKY
        startForeground(1, createNotification("Starting download..."))

        scope.launch {
            downloadAllChapters(seriesUrl)
            sendBroadcast(Intent("com.example.sreaper.DOWNLOAD_FINISHED"))
            stopForeground(true)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val cancelIntent = Intent(this, DownloadService::class.java).apply {
            action = "ACTION_STOP_DOWNLOAD"
        }
        val cancelPendingIntent = PendingIntent.getService(this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SneakReaper")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.reaper)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, false)
            .build()
    }

    private fun updateNotification(progress: Int, speedKBps: Int, chapter: Int) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Downloading Chapter $chapter")
            .setContentText("Progress: $progress% | Speed: ${speedKBps}KB/s")
            .setSmallIcon(R.drawable.reaper)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(channelId, "Download Service Channel", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun fetchHtml(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val stateFile: File
        get() = File(getExternalFilesDir(null), "downloads_state.json")

    private data class DownloadState(
        val completedChapters: MutableSet<Int> = mutableSetOf(),
        val downloadedUrls: MutableSet<String> = mutableSetOf()
    )

    private fun saveDownloadState(state: DownloadState) {
        try {
            val json = JSONObject().apply {
                put("completedChapters", JSONArray(state.completedChapters.toList()))
                put("downloadedUrls", JSONArray(state.downloadedUrls.toList()))
            }
            stateFile.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadDownloadState(): DownloadState {
        return try {
            if (!stateFile.exists()) return DownloadState()
            val json = JSONObject(stateFile.readText())
            val chapters = json.optJSONArray("completedChapters") ?: JSONArray()
            val urls = json.optJSONArray("downloadedUrls") ?: JSONArray()

            val chapterSet = mutableSetOf<Int>()
            val urlSet = mutableSetOf<String>()
            for (i in 0 until chapters.length()) chapterSet.add(chapters.getInt(i))
            for (i in 0 until urls.length()) urlSet.add(urls.getString(i))

            DownloadState(chapterSet, urlSet)
        } catch (e: Exception) {
            e.printStackTrace()
            DownloadState()
        }
    }

    private suspend fun downloadAllChapters(seriesUrl: String) {
        val state = loadDownloadState()
        downloadedUrls.addAll(state.downloadedUrls)

        try {
            val doc = Jsoup.connect(seriesUrl).get()
            val totalChaptersDiv = doc.select("div.flex.justify-between:contains(Total chapters)").firstOrNull()
            val spans = totalChaptersDiv?.select("span")
            val totalChaptersText = spans?.getOrNull(1)?.text()
            val totalChapters = totalChaptersText?.toIntOrNull() ?: return

            for (chapter in 1..totalChapters) {
                if (!scope.isActive) return
                if (state.completedChapters.contains(chapter)) continue

                val chapterUrl = "$seriesUrl/chapter-$chapter"
                val chapterHtml = fetchHtml(chapterUrl) ?: continue
                downloadChapterParallel(chapter, chapterHtml)

                state.completedChapters.add(chapter)
                state.downloadedUrls.addAll(downloadedUrls)
                saveDownloadState(state)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun downloadChapterParallel(chapter: Int, html: String) {
        val doc = Jsoup.parse(html)
        val title = doc.select("title").text().ifEmpty { "chapter" }
        val mangaName = title.substringBefore(" Chapter").trim().replace("[^a-zA-Z0-9\\s]".toRegex(), "").replace(" ", "_")

        val mangaFolder = File(getExternalFilesDir(null), mangaName)
        if (!mangaFolder.exists()) mangaFolder.mkdirs()

        val chapterFolder = File(mangaFolder, "Chapter_$chapter")
        if (!chapterFolder.exists()) chapterFolder.mkdirs()

        val images = doc.select("img").mapNotNull {
            val src = it.absUrl("src")
            if (src.endsWith(".jpg") || src.endsWith(".png") || src.endsWith(".jpeg")) src else null
        }.distinct()

        val totalImages = images.size
        var completed = 0

        coroutineScope {
            images.mapIndexed { i, url ->
                launch {
                    semaphore.withPermit {
                        val file = File(chapterFolder, "%02d.jpg".format(i + 1))
                        if (downloadedUrls.contains(url) || file.exists()) {
                            completed++
                            return@withPermit
                        }

                        var retryCount = 3
                        while (retryCount > 0) {
                            try {
                                var downloadedBytes = 0
                                val timeTaken = measureTimeMillis {
                                    val req = Request.Builder().url(url).build()
                                    val res = client.newCall(req).execute()
                                    if (!res.isSuccessful) throw Exception("Failed to download")

                                    val input = res.body?.byteStream()
                                    val output = FileOutputStream(file)
                                    val buffer = ByteArray(4096)
                                    var read: Int

                                    while (input?.read(buffer).also { read = it ?: -1 } != -1) {
                                        output.write(buffer, 0, read)
                                        downloadedBytes += read
                                    }

                                    output.flush()
                                    output.close()
                                    input?.close()
                                }

                                downloadedUrls.add(url)
                                val speedKBps = if (timeTaken > 0) (downloadedBytes / 1024 / (timeTaken / 1000.0)).toInt() else 0
                                val progress = (++completed * 100) / totalImages
                                updateNotification(progress, speedKBps, chapter)
                                break
                            } catch (e: Exception) {
                                retryCount--
                                if (retryCount == 0) e.printStackTrace()
                                delay(500)
                            }
                        }
                    }
                }
            }.joinAll()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
