# Rosebank Gospel Hall for Android

A native Android app for [Rosebank Gospel Hall](https://rosebankgospelhall.ca/) in Rosebank, Prince Edward Island.
It brings the church website's messages, videos, audio and key pages into a fast, easy-to-read app.

## Features

- **Home** with the latest posts, quick links to each kind of content, and visit details
- **Posts** with filters for every category and tag on the site, search, and sorting (newest, oldest, A to Z, Z to A)
- **Listen** and **Watch** tabs with their own filters and sorting
- **Audio player** that keeps playing in the background with lock-screen controls, speed control and skip buttons
- **Video player** that plays videos inside the app, including full screen
- **Saved posts** for reading again later, available offline
- **Info pages**: What We Believe, Resources, Free Food Hamper and Contact (call, email, directions)
- **Reading comfort**: light, dark or system theme and adjustable text size
- **Optional notifications** when a new post is published
- Links between posts and pages open inside the app

## How content is loaded

The app reads from the public website, so anything published there appears in the app without an update:

- Posts come from the site's WordPress REST API, with the RSS feed as a backup.
- Pages are read from the API, or from the live page if the API returns nothing.
- Posts and pages are kept on the phone for offline reading.
- The launcher icon is made from the website's favicon. To use a different image, add a square `icon/logo.png`.

## Privacy

- No accounts, no ads, no analytics and no tracking.
- The app only talks to rosebankgospelhall.ca and the video hosts used by posts (such as YouTube).
- Saved posts and settings stay on the device.
- Permissions: internet access, notifications (optional) and background audio playback.

## Project layout

```
app/src/main/java/ca/rosebankgospelhall/app/
  data/   Models, website loading, HTML-to-native content parsing, preferences
  ui/     Jetpack Compose screens, theme and reusable components
  AudioController.kt, PlaybackService.kt   Background audio
  NewPostWorker.kt                          New-post notifications
```

Built with Kotlin, Jetpack Compose (Material 3), Navigation Compose, Media3, Coil, OkHttp, Jsoup and WorkManager.
Minimum Android version: 8.0 (API 26).

## Content and credit

All sermons, articles, images and text belong to Rosebank Gospel Hall and their respective authors.
For questions about the church or its content, visit [rosebankgospelhall.ca](https://rosebankgospelhall.ca/).
