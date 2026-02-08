# Notes (Android MVP)

Notes is a minimal Android app for creating, viewing, and organizing plain-text notes.
Notes are stored as `.txt` files, with metadata tracked in a `notes_index.json` file alongside
those notes.

## Features
- Create, edit, view, and delete notes.
- Optional labels per note with filtering by label.
- Notes list and detail preview screens with quick navigation.
- Configure the notes storage directory (defaults to the app's `getExternalFilesDir("notes")`).
- Import notes from:
  - A ZIP archive containing `.txt` files and `notes_index.json`.
  - A directory of `.txt` files (file name becomes the note title).
- Export notes to a ZIP archive.
- Encrypted backup export to Google Drive (AES-GCM with PBKDF2 key derivation).

## Data storage model
- **Notes content:** individual `.txt` files.
- **Metadata:** `notes_index.json` stores note IDs, titles, labels, and timestamps.
- **Location:** a writable directory chosen in the app settings (defaults to the app's external files directory).

## Backup and Google Drive integration
- The encrypted backup process packages notes into a ZIP file and encrypts it using AES-GCM.
- Encryption keys are derived using PBKDF2 (HMAC-SHA256, 120,000 iterations, 256-bit key).
- The app calls a `DriveUploader` abstraction; the current implementation uses a placeholder uploader
  that must be replaced with a real Google Drive API integration (e.g., Google Sign-In + Drive REST API).

## Tech stack
- Kotlin + Jetpack Compose
- Coroutines + Kotlinx Serialization
- AndroidX Lifecycle, DocumentFile, and Material 3
- Google Sign-In for Drive authorization flow

## Build requirements
- Android Studio or Gradle with JDK 17
- Android SDK (compileSdk/targetSdk 34, minSdk 26)

## Build locally
```bash
./gradlew assembleDebug
```

The APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

## CI: GitHub Actions APK build
A GitHub Actions workflow is included to build the debug APK on every push or pull request.
It installs the Android SDK, sets up JDK 17, and runs `gradle assembleDebug`.
The resulting APK is uploaded as a workflow artifact.

## Future improvements
- Implement a production `DriveUploader` to upload encrypted backups to Google Drive.
- Add additional backup providers (e.g., S3, Dropbox).
- Add a settings screen for backup providers and schedules.
