# BisonNotes AI - Android

Jetpack Compose Android app for recording audio, transcribing it with local or cloud engines, and generating summaries, tasks, and reminders. Room database powers persistence; WorkManager handles background processing.

**Android Port Status**: 🚧 In Development - Phase 3 (Advanced Transcription)
**Original iOS App**: https://apps.apple.com/us/app/bisonnotes-ai-voice-notes/id6749189425

Quick links: [Android Port Planning](ANDROID_PORT_README.md) • [Implementation Roadmap](ANDROID_IMPLEMENTATION_ROADMAP.md) • [Build & Test](#build-and-test) • [Architecture](#architecture)

## Architecture
- **Data**: Room database stores recordings, transcripts, summaries, and processing jobs with automatic migrations.
- **Transcription Engines**:
  - **Android SpeechRecognizer** - On-device, privacy-focused, free
  - **OpenAI Whisper API** - Cloud, high-quality, $0.006/min
  - **AWS Transcribe** - Enterprise cloud, speaker diarization, $0.024/min
  - **Local Whisper Server** - Self-hosted, complete privacy, unlimited free ⭐ **NEW**
- **Background Processing**: WorkManager coordinates queued transcription and AI jobs with retry logic and constraints.
- **UI**: Jetpack Compose with Material 3 Design. MVVM architecture with Hilt dependency injection. View models manage state with Kotlin Flow and StateFlow.
- **Clean Architecture**: Domain, Data, and Presentation layers with clear separation of concerns.

## Project Structure
```
app/src/main/java/com/bisonnotesai/android/
├── data/
│   ├── db/           # Room database entities and DAOs
│   ├── preferences/  # DataStore for settings
│   ├── repository/   # Repository implementations
│   └── transcription/
│       ├── aws/      # AWS Transcribe engine
│       ├── openai/   # OpenAI Whisper API engine
│       └── whisper/  # Local Whisper server engine ⭐ NEW
├── domain/
│   ├── model/        # Domain models
│   └── repository/   # Repository interfaces
├── di/              # Hilt dependency injection modules
├── ui/
│   ├── screen/      # Compose screens
│   ├── viewmodel/   # MVVM view models
│   └── theme/       # Material 3 theming
├── audio/           # Audio recording and playback
└── transcription/   # Transcription service interfaces
```

## Build and Test
- **Open in Android Studio**: File → Open → Select `BisonNotes-AI-Android`
- **Build**: `./gradlew build` or Build → Make Project (Ctrl+F9 / ⌘F9)
- **Run**: `./gradlew installDebug` or Run → Run 'app' (Shift+F10 / ⌃R)
- **Test**: `./gradlew test` for unit tests, `./gradlew connectedAndroidTest` for instrumented tests
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)

## Dependencies

The project uses Gradle for dependency management. Major dependencies include:

### **Core Android**
- **Jetpack Compose**: Modern declarative UI with Material 3
- **Room**: Local database with compile-time SQL verification
- **Hilt**: Dependency injection framework
- **WorkManager**: Background job processing
- **DataStore**: Type-safe data storage

### **Networking & APIs**
- **Retrofit 2.9.0**: REST API client
- **OkHttp 4.12.0**: HTTP client with interceptors
- **Gson**: JSON serialization/deserialization

### **Cloud Services**
- **AWS SDK for Kotlin 1.0.0**: Cloud transcription and AI
  - `aws.sdk.kotlin:transcribe`: AWS Transcribe service
  - `aws.sdk.kotlin:s3`: S3 file storage
  - Authentication with static credentials

### **Audio**
- **MediaRecorder**: Native audio recording
- **MediaPlayer**: Audio playback
- **SpeechRecognizer**: On-device speech recognition

All dependencies are resolved automatically via Gradle when building in Android Studio.

## Local Dev Setup
- **Requirements**:
  - Android Studio Hedgehog (2023.1.1) or newer
  - JDK 17 or higher
  - Android SDK with API 26+ and API 34
- **Setup**:
  1. Clone the repo: `git clone https://github.com/bisonbet/BisonNotes-AI-Android.git`
  2. Open in Android Studio: File → Open → Select `BisonNotes-AI-Android`
  3. Wait for Gradle sync to complete
  4. Select a device/emulator and click Run
- **Branch/PR**: Create feature branch, push changes, open PR. Include build/test results and screenshots for UI changes.

## Key Modules
- **Recording**: `AudioRecorder`, `AudioPlayer`, `RecordingFileManager`, `AudioRecorderViewModel`
- **Transcription Engines**:
  - `AndroidSpeechRecognizer` - On-device Android SpeechRecognizer
  - `OpenAIWhisperEngine` - Cloud OpenAI Whisper API
  - `AWSTranscribeEngine` - Cloud AWS Transcribe with S3 upload
  - `LocalWhisperEngine` - Privacy-focused local Whisper server ⭐ **NEW**
- **UI**:
  - Screens: `RecordingsScreen`, `TranscriptsScreen`, `TranscriptDetailScreen`
  - Settings: `OpenAISettingsScreen`, `AWSSettingsScreen`, `WhisperSettingsScreen` ⭐
  - ViewModels: `AudioRecorderViewModel`, `TranscriptsViewModel`, `WhisperSettingsViewModel` ⭐
- **Data**: `RecordingEntity`, `TranscriptEntity`, `SummaryEntity`, `ProcessingJobEntity`
- **Repositories**: `RecordingRepository`, `TranscriptRepository`, `SummaryRepository`, `ProcessingJobRepository`
- **Background**: `TranscriptionWorker` with WorkManager

## Configuration
- **API Keys**: Enter credentials in-app via settings screens:
  - OpenAI Settings: API key for Whisper API
  - AWS Settings: Access Key ID, Secret Key, S3 bucket, region
  - Local Whisper Settings: Server URL, port, protocol (REST/Wyoming)
- **Permissions**: Required permissions in `AndroidManifest.xml`:
  - `RECORD_AUDIO` - Audio recording
  - `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` - File storage (API < 29)
  - `INTERNET` - Cloud transcription services
- **Security**: Never commit API keys or credentials. Use `.gitignore` for local config files.

## Contributing
See AGENTS.md for repository guidelines (style, structure, commands, testing, PRs). Follow the Local Dev Setup above to run and validate changes before opening a PR.

## License
See LICENSE.
