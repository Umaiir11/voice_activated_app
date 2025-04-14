package com.example.voice_activated_app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

class VoiceService : Service() {
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private lateinit var textToSpeech: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wakeLock: PowerManager.WakeLock
    private val TAG = "VoiceService"
    private var isProcessingHotword = false
    private var ttsInitialized = false

    // Watchdog to ensure listening is always active
    private val restartWatchdog = object : Runnable {
        override fun run() {
            if (!isProcessingHotword) {
                Log.d(TAG, "Watchdog: Ensuring speech recognition is active")
                ensureSpeechRecognizerActive()
            }
            handler.postDelayed(this, 10000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        initWakeLock()
        initTextToSpeech()
        setupSpeechRecognizer()
        handler.postDelayed(restartWatchdog, 10000)
    }

    private fun initWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VoiceActivatedApp:VoiceWakeLock"
        )
        wakeLock.acquire(10*60*1000L)
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                ttsInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                Log.d(TAG, "TTS initialized: $ttsInitialized")
            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "voice_activation_channel"
        val channelName = "Voice Activation Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(chan)
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Voice Activation Running")
            .setContentText("Listening for 'Hey My App'")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            startListening()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup recognizer", e)
            handler.postDelayed({ setupSpeechRecognizer() }, 3000)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
            }

            override fun onError(error: Int) {
                Log.e(TAG, "SpeechRecognizer error: $error")
                restartListeningWithDelay()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                processSpeechResults(matches)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                processSpeechResults(matches)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun processSpeechResults(matches: ArrayList<String>?) {
        if (isProcessingHotword) return

        matches?.forEach { match ->
            Log.d(TAG, "Heard: $match")
            if (match.lowercase().contains("hey my app") || match.lowercase().contains("my app")) {
                isProcessingHotword = true

                if (ttsInitialized) {
                    textToSpeech.speak("Hi broooo", TextToSpeech.QUEUE_FLUSH, null, null)
                }

                val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    action = "HOTWORD_DETECTED"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                try {
                    startActivity(launchIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start activity", e)
                }

                handler.postDelayed({
                    isProcessingHotword = false
                    startListening()
                }, 3000)

                return
            }
        }

        if (!isProcessingHotword) {
            startListening()
        }
    }

    private fun startListening() {
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            handler.postDelayed({ startListening() }, 2000)
        }
    }

    private fun restartListeningWithDelay() {
        handler.postDelayed({ startListening() }, 1500)
    }

    private fun ensureSpeechRecognizerActive() {
        if (speechRecognizer == null) {
            setupSpeechRecognizer()
        } else {
            startListening()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech.shutdown()
        handler.removeCallbacksAndMessages(null)
        wakeLock.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
