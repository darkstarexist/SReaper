# 📚 SReaper

**SReaper** is an Android app that lets users browse and download manhwa chapters directly from [ReaperScans](https://reaperscans.com). It parses chapter pages, extracts image URLs, and saves them for offline reading.

---

## ✨ Features

- 🔍 Fetch manhwa chapters from ReaperScans
- 📥 Download and save chapter images
- 🖼️ View high-quality pages offline
- ⏳ Foreground service with download progress in notifications

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **Libraries Used:**
  - [Jsoup](https://jsoup.org/) for HTML parsing
  - [OkHttp](https://square.github.io/okhttp/) for network requests
  - Android Notification system for foreground downloads
  - RecyclerView for displaying image lists

---

## 🚀 Getting Started

### Prerequisites

- Android Studio (Electric Eel or newer)
- Android SDK 24 or above
- Internet permission in `AndroidManifest.xml`

### Clone the Repository

```bash
git clone https://github.com/darkstarexist/SReaper.git
cd SReaper
