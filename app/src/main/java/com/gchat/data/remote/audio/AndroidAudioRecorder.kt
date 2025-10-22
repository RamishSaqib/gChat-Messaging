package com.gchat.data.remote.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.gchat.domain.repository.AudioRecordingResult
import com.gchat.domain.repository.RecordingState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10

/**
 * AndroidAudioRecorder - Record audio using MediaRecorder
 * 
 * Records audio in M4A format (Android native, good compression)
 * Generates waveform data during recording
 */
@Singleton
class AndroidAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartTime: Long = 0
    private val waveformData = mutableListOf<Float>()
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: Flow<RecordingState> = _recordingState.asStateFlow()
    
    /**
     * Start recording audio
     */
    fun startRecording(): Result<File> {
        return try {
            // Create output file
            val outputDir = File(context.cacheDir, "voice_messages")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFile = File(outputDir, "recording_${System.currentTimeMillis()}.m4a")
            recordingFile = outputFile
            recordingStartTime = System.currentTimeMillis()
            waveformData.clear()
            
            // Create and configure MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000) // 128 kbps
                setAudioSamplingRate(44100) // 44.1 kHz
                setOutputFile(outputFile.absolutePath)
                
                prepare()
                start()
            }
            
            _recordingState.value = RecordingState.Recording(0, 0f)
            
            Log.d("AndroidAudioRecorder", "Recording started: ${outputFile.absolutePath}")
            
            Result.success(outputFile)
        } catch (e: IOException) {
            Log.e("AndroidAudioRecorder", "Failed to start recording", e)
            _recordingState.value = RecordingState.Error("Failed to start recording: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AndroidAudioRecorder", "Unexpected error starting recording", e)
            _recordingState.value = RecordingState.Error("Unexpected error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update recording state (call periodically from ViewModel)
     */
    fun updateRecordingState() {
        if (mediaRecorder == null) return
        
        try {
            val duration = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
            
            // Get amplitude for waveform visualization
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            val normalizedAmplitude = normalizeAmplitude(amplitude)
            
            // Add to waveform data (sample every update)
            waveformData.add(normalizedAmplitude)
            
            _recordingState.value = RecordingState.Recording(duration, normalizedAmplitude)
        } catch (e: Exception) {
            Log.e("AndroidAudioRecorder", "Error updating recording state", e)
        }
    }
    
    /**
     * Stop recording and return result
     */
    fun stopRecording(): Result<AudioRecordingResult> {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val file = recordingFile ?: return Result.failure(Exception("No recording file"))
            val duration = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
            
            // Smooth waveform data (reduce resolution for storage)
            val smoothedWaveform = smoothWaveform(waveformData, targetSize = 100)
            
            val result = AudioRecordingResult(
                file = file,
                durationSeconds = duration,
                waveformData = smoothedWaveform
            )
            
            _recordingState.value = RecordingState.Idle
            
            Log.d("AndroidAudioRecorder", "Recording stopped: ${file.absolutePath}, duration: ${duration}s")
            
            Result.success(result)
        } catch (e: Exception) {
            Log.e("AndroidAudioRecorder", "Failed to stop recording", e)
            _recordingState.value = RecordingState.Error("Failed to stop recording: ${e.message}")
            cancelRecording() // Clean up
            Result.failure(e)
        }
    }
    
    /**
     * Cancel recording and clean up
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AndroidAudioRecorder", "Error stopping recorder", e)
        }
        
        mediaRecorder = null
        
        // Delete recording file
        recordingFile?.delete()
        recordingFile = null
        
        waveformData.clear()
        _recordingState.value = RecordingState.Idle
        
        Log.d("AndroidAudioRecorder", "Recording cancelled")
    }
    
    /**
     * Normalize amplitude to 0.0 - 1.0 range
     */
    private fun normalizeAmplitude(amplitude: Int): Float {
        if (amplitude == 0) return 0f
        
        // Convert to decibels (dB)
        val db = 20 * log10(amplitude.toDouble() / 32767.0)
        
        // Normalize to 0.0 - 1.0 (assuming -60 dB to 0 dB range)
        val normalized = ((db + 60) / 60).coerceIn(0.0, 1.0)
        
        return normalized.toFloat()
    }
    
    /**
     * Smooth waveform data by averaging samples
     */
    private fun smoothWaveform(waveform: List<Float>, targetSize: Int): List<Float> {
        if (waveform.size <= targetSize) return waveform
        
        val chunkSize = waveform.size / targetSize
        val smoothed = mutableListOf<Float>()
        
        for (i in 0 until targetSize) {
            val start = i * chunkSize
            val end = ((i + 1) * chunkSize).coerceAtMost(waveform.size)
            
            val chunk = waveform.subList(start, end)
            val average = chunk.average().toFloat()
            smoothed.add(average)
        }
        
        return smoothed
    }
}

