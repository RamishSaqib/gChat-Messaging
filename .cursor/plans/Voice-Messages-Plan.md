<!-- d2e60e5a-6b5c-484f-a957-307d75c0bb54 e2bbf947-2634-4a2d-8999-266fff42c29e -->
# Voice Messages with AI Transcription

## Overview

Add voice message support with recording, playback controls, waveform visualization, speed adjustment, and AI-powered transcription via OpenAI Whisper API.

## Implementation Steps

### Phase 1: Backend (Firebase Cloud Functions)

**1.1 Create Whisper Transcription Function** (`firebase/functions/src/ai/transcription.ts`)

- Add OpenAI Whisper API integration
- Accept audio file URL from Firebase Storage
- Download audio, send to Whisper API
- Return transcription text with language detection
- Handle errors and rate limiting (50 transcriptions/hour per user)
- Support multiple audio formats (M4A, MP3, WAV, OGG)

**1.2 Update Storage Rules** (`firebase/storage.rules`)

- Add `voice_messages/{userId}/{fileName}` path
- Allow audio MIME types: audio/m4a, audio/mp3, audio/wav, audio/ogg
- Max size: 10MB (approx 10 minutes of audio)
- Read: any authenticated user
- Write: owner only

**1.3 Export Function** (`firebase/functions/src/index.ts`)

- Export `transcribeVoiceMessage` function

---

### Phase 2: Domain Layer (Android)

**2.1 Update Message Model** (`app/src/main/java/com/gchat/domain/model/Message.kt`)

- Add `AUDIO` to `MessageType` enum
- Add optional fields: `audioDuration`, `audioWaveform`, `transcription`

**2.2 Create Use Cases**

- `RecordVoiceMessageUseCase.kt` - Handle recording lifecycle
- `SendVoiceMessageUseCase.kt` - Upload audio + send message
- `PlayVoiceMessageUseCase.kt` - Play audio with controls
- `TranscribeVoiceMessageUseCase.kt` - Request transcription from backend

**2.3 Create Repository Interface** (`app/src/main/java/com/gchat/domain/repository/AudioRepository.kt`)

- `startRecording()`: Start audio recording
- `stopRecording()`: Stop and return audio file
- `playAudio(url, speed)`: Play audio with speed control
- `pauseAudio()`: Pause playback
- `getWaveformData(file)`: Extract waveform for visualization
- `transcribeAudio(audioUrl)`: Call transcription Cloud Function

---

### Phase 3: Data Layer (Android)

**3.1 Update Database Schema**

- Increment `AppDatabase` version to 10
- Add to `MessageEntity`: `audioDuration`, `audioWaveform`, `transcription`
- Create migration 9→10

**3.2 Create Audio Data Sources**

- `AndroidAudioRecorder.kt` - MediaRecorder wrapper
- Record to M4A format (Android native, good compression)
- Generate waveform data during recording
- Return File object when done

- `AndroidAudioPlayer.kt` - MediaPlayer wrapper
- Play audio from URL
- Support playback speed (0.5x, 1x, 1.5x, 2x)
- Emit playback progress
- Handle pause/resume

**3.3 Update Firebase Data Sources**

- `FirestoreMessageDataSource.kt` - Handle AUDIO message type
- `FirebaseStorageDataSource.kt` - Upload audio files to `voice_messages/{userId}/` path
- `FirebaseTranscriptionDataSource.kt` - Call Cloud Function for transcription

**3.4 Implement Repository** (`AudioRepositoryImpl.kt`)

- Wire up all data sources
- Handle audio recording/playback lifecycle
- Upload audio to Firebase Storage
- Request transcription from backend

**3.5 Update Message Mappers**

- `MessageMapper.kt` - Map audio fields to/from Entity and Firestore

---

### Phase 4: Presentation Layer (Android)

**4.1 Add Permissions** (`AndroidManifest.xml`)

- `android.permission.RECORD_AUDIO`
- `android.permission.MODIFY_AUDIO_SETTINGS`

**4.2 Create Voice Recording UI** (`VoiceRecordingSheet.kt`)

- Bottom sheet for recording
- Animated waveform during recording
- Duration counter (00:00)
- Cancel button
- Send button
- Request RECORD_AUDIO permission

**4.3 Create Voice Message Bubble** (`VoiceMessageBubble.kt` composable)

- Play/pause button
- Waveform visualization (static from recorded data)
- Duration / current time display
- Playback speed button (0.5x, 1x, 1.5x, 2x)
- Progress slider
- Transcription text (collapsed/expandable)
- "Transcribing..." loading state

**4.4 Update ChatScreen** (`ChatScreen.kt`)

- Add microphone icon to input bar (hold to record)
- Show `VoiceRecordingSheet` when recording
- Display `VoiceMessageBubble` for AUDIO messages

**4.5 Update ChatViewModel** (`ChatViewModel.kt`)

- Add recording state management
- Add playback state management
- `startRecording()`, `stopRecording()`, `sendVoiceMessage()`
- `playVoiceMessage()`, `pauseVoiceMessage()`, `setPlaybackSpeed()`
- `requestTranscription()` - Call backend for transcription
- Track currently playing message (only one at a time)

**4.6 Add Waveform Composable** (`WaveformView.kt`)

- Custom Canvas drawing
- Animated during recording
- Static with playback progress for playback

---

### Phase 5: Hilt Integration

**5.1 Update RepositoryModule** (`di/RepositoryModule.kt`)

- Bind `AudioRepository` to `AudioRepositoryImpl`

**5.2 Create DataSourceModule** (`di/DataSourceModule.kt`)

- Provide `AndroidAudioRecorder`
- Provide `AndroidAudioPlayer`
- Provide `FirebaseTranscriptionDataSource`

---

### Phase 6: Testing & Deployment

**6.1 Deploy Cloud Functions**

- Build and deploy `transcribeVoiceMessage` function
- Update Firebase Storage rules

**6.2 Test Flow**

1. Open chat
2. Long-press microphone icon
3. Record voice message (see waveform)
4. Release to send
5. See voice message bubble with waveform
6. Tap play button - audio plays
7. Wait for transcription to appear
8. Test speed controls (0.5x, 1x, 1.5x, 2x)
9. Test on sender and receiver

---

## Key Files to Create/Modify

### Backend (3 files)

- `firebase/functions/src/ai/transcription.ts` (new)
- `firebase/functions/src/index.ts` (modify)
- `firebase/storage.rules` (modify)

### Domain (7 files)

- `Message.kt` (modify - add AUDIO type)
- `AudioRepository.kt` (new interface)
- `RecordVoiceMessageUseCase.kt` (new)
- `SendVoiceMessageUseCase.kt` (new)
- `PlayVoiceMessageUseCase.kt` (new)
- `TranscribeVoiceMessageUseCase.kt` (new)
- `AudioPlaybackState.kt` (new - sealed class for playback states)

### Data (8 files)

- `AppDatabase.kt` (modify - version 10)
- `MessageEntity.kt` (modify - add audio fields)
- `AndroidAudioRecorder.kt` (new)
- `AndroidAudioPlayer.kt` (new)
- `FirebaseTranscriptionDataSource.kt` (new)
- `FirebaseStorageDataSource.kt` (modify - add audio upload)
- `AudioRepositoryImpl.kt` (new)
- `MessageMapper.kt` (modify)

### Presentation (6 files)

- `AndroidManifest.xml` (modify - add permissions)
- `VoiceRecordingSheet.kt` (new)
- `VoiceMessageBubble.kt` (new)
- `WaveformView.kt` (new)
- `ChatScreen.kt` (modify - add voice UI)
- `ChatViewModel.kt` (modify - add voice logic)

### DI (2 files)

- `RepositoryModule.kt` (modify)
- `DataSourceModule.kt` (modify)

---

## Total: ~30 files (11 new, 19 modified)

## Technical Decisions

1. **Audio Format**: M4A (Android native, good compression, ~1MB per minute)
2. **Recording Library**: Android MediaRecorder (native, reliable)
3. **Playback Library**: Android MediaPlayer (native, supports speed control)
4. **Transcription**: OpenAI Whisper API (accurate, supports 50+ languages)
5. **Waveform**: Generate during recording, store as JSON array
6. **Storage**: Firebase Storage (`voice_messages/{userId}/timestamp.m4a`)
7. **Max Duration**: 10 minutes (configurable)
8. **Playback Speed**: 0.5x, 1x, 1.5x, 2x

---

## User Experience Flow

1. User long-presses microphone icon
2. Permission requested if not granted
3. Recording starts, waveform animates
4. User releases or taps send → audio uploaded
5. Message appears with static waveform
6. Transcription happens in background
7. Transcription appears below waveform when ready
8. Recipient can play, adjust speed, read transcription

### To-dos

- [ ] Create Whisper transcription Cloud Function
- [ ] Update Firebase Storage rules for audio files
- [ ] Deploy Cloud Functions
- [ ] Update Message domain model with AUDIO type and audio fields
- [ ] Create audio-related use cases (Record, Send, Play, Transcribe)
- [ ] Create AudioRepository interface
- [ ] Update MessageEntity and create DB migration (v9→v10)
- [ ] Create AndroidAudioRecorder data source
- [ ] Create AndroidAudioPlayer data source
- [ ] Create FirebaseTranscriptionDataSource
- [ ] Update FirebaseStorageDataSource for audio uploads
- [ ] Implement AudioRepositoryImpl
- [ ] Update MessageMapper for audio fields
- [ ] Add RECORD_AUDIO permission to AndroidManifest
- [ ] Create VoiceRecordingSheet composable
- [ ] Create VoiceMessageBubble composable
- [ ] Create WaveformView composable
- [ ] Update ChatScreen with voice recording UI
- [ ] Update ChatViewModel with voice message logic
- [ ] Update Hilt modules for dependency injection
- [ ] Test voice recording and playback in emulator
- [ ] Test transcription feature end-to-end