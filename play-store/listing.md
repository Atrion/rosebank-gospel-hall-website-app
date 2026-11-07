# Google Play listing

Text and answers for the Play Console. Nothing here is used by the app itself.

## Store listing

**App name** (30 characters or fewer)
Rosebank Gospel Hall App

**Short description** (80 characters or fewer)
Read, watch and listen to messages from Rosebank Gospel Hall, Rosebank PEI.

**Full description**
Rosebank Gospel Hall App brings the messages, videos and audio from Rosebank Gospel Hall, a Gospel Hall Assembly in Rosebank, Prince Edward Island, into one easy-to-use app.

WHAT YOU CAN DO
• Browse every post, with filters for each category and tag, search and sorting
• Listen to audio messages, even with the screen off, with lock-screen controls, speed and skip buttons
• Watch videos full screen inside the app
• Save posts to read again later, even without a connection
• Read What We Believe, the Free Food Hamper page and Resources
• Call, email or get directions to the hall
• Choose a light or dark theme and make the text larger
• Optionally get a notification when a new post is published

PRIVATE AND SIMPLE
No accounts, no ads and no tracking. Saved posts and settings stay on your device.

ABOUT
Messages and articles come from rosebankgospelhall.ca and belong to Rosebank Gospel Hall and their authors. The app is built and maintained by From Gravel Hill Enterprises (fromgravelhill.ca). The source code is open under the Apache License 2.0.

**Category:** Lifestyle  (Books & Reference is also reasonable)
**Contact website:** https://fromgravelhill.ca/
**Contact email:** the address you want shown publicly (required)
**Privacy policy URL:** https://fromgravelhill.ca/rosebank-gospel-hall-app/privacy/

## Graphics

- App icon, 512 x 512 PNG: downloaded from each build run (Actions, then the run, then the `play-store-icon` artifact)
- Feature graphic, 1024 x 500: `play-store/feature-graphic-1024x500.png`
- Phone screenshots (at least 2, up to 8): take them on your phone. Suggested: Home, Posts with filters, a post with the audio player, the video player, Saved posts, and Settings.

## App content answers

- **App access:** all functionality is available without logging in
- **Ads:** no ads
- **Target audience:** 18 and over (or 13 and over). Avoid choosing under 13, which triggers the Families policy.
- **Content rating questionnaire:** religious content; no violence, no user-generated content, no purchases
- **News app / government app / financial features / health features:** none
- **Data safety:** no data collected and no data shared. Data is encrypted in transit (the app uses HTTPS only). Videos play in the YouTube, Vimeo or Rumble player; those services' own policies apply and are mentioned in the privacy policy.
- **Foreground service (media playback):** audio messages keep playing with the screen off, with notification and lock-screen controls. If asked for a video, record a short screen capture of audio playing in the background.
- **Permissions:** internet; notifications (optional, asked inside the app); foreground service for audio playback

## Release steps

1. Download the `.aab` file from the latest GitHub release.
2. Play Console, Testing, Internal testing, Create new release. On first upload, accept Play App Signing.
3. Upload the `.aab`, add testers by email, and roll out.
4. Test the Play version. Then use Closed testing and Production when ready.
5. Personal developer accounts created recently must run a closed test with at least 12 testers for 14 days before applying for production. Organization accounts do not.
6. Every upload needs a higher version number than the last. The release number in `app/build.gradle.kts` provides this.
