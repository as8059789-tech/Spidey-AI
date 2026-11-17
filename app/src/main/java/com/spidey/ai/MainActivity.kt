package com.spidey.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.VideoView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener, RecognitionListener {

    private lateinit var face: SpideyFaceView
    private lateinit var status: TextView
    private lateinit var subtitle: TextView
    private lateinit var video: VideoView

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var ttsDone = false
    private var videoReady = false
    private var started = false
    private var introFinished = false
    private var greetingDone = false

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var speaking = false
    private var idleMode = false
    private var inForeground = false
    private var silentCount = 0
    private var pendingStore: String? = null

    private val handler = Handler(Looper.getMainLooper())
    private val doneCallbacks = HashMap<String, () -> Unit>()
    private var uttCounter = 0
    private val greeting = listOf("Yes Boss!", "Yes Aadil!", "I'm here, Boss!").random()

    companion object {
        val OPEN_WORDS = setOf(
            "open", "kholo", "khol", "kholna", "launch", "start",
            "chalu", "chalao", "chala", "run", "dikhao"
        )
        val FILLER = setOf(
            "karo", "kar", "do", "de", "dena", "dijiye", "please", "pls", "plz", "mujhe",
            "mere", "liye", "ko", "ka", "ki", "app", "application", "the", "a", "hey", "hi",
            "spidey", "boss", "aur", "zara", "jara", "ek", "bhai", "yaar", "can", "you",
            "could", "me", "my", "to", "now", "jaldi", "kijiye", "kijie", "ho", "hai", "hain"
        )
        val YES = setOf("yes", "haan", "han", "ha", "ok", "okay", "kholo", "sure", "yeah", "yep")
        val DEV = listOf(
            "यू ट्यूब" to "youtube", "यूट्यूब" to "youtube", "यूटयूब" to "youtube",
            "इंस्टाग्राम" to "instagram", "इंस्टा" to "instagram",
            "व्हाट्सएप" to "whatsapp", "वॉट्सऐप" to "whatsapp", "व्हाट्सऐप" to "whatsapp",
            "वाट्सएप" to "whatsapp", "व्हाट्सअप" to "whatsapp", "व्हाट्स ऐप" to "whatsapp",
            "क्रोम" to "chrome", "गूगल" to "google", "कैमरा" to "camera", "कैमरे" to "camera",
            "सेटिंग्स" to "settings", "सेटिंग" to "settings",
            "फेसबुक" to "facebook", "स्नैपचैट" to "snapchat", "टेलीग्राम" to "telegram",
            "मैप्स" to "maps", "मैप" to "maps", "जीमेल" to "gmail", "स्पॉटिफाई" to "spotify",
            "प्ले स्टोर" to "play store", "कैलकुलेटर" to "calculator", "गैलरी" to "gallery",
            "ट्विटर" to "twitter", "नेटफ्लिक्स" to "netflix", "अमेज़न" to "amazon",
            "फ्लिपकार्ट" to "flipkart", "पेटीएम" to "paytm", "फोनपे" to "phonepe",
            "खोलिए" to "open", "खोलना" to "open", "खोलो" to "open", "खोल" to "open",
            "ओपन" to "open", "चालू" to "open", "चलाओ" to "open", "चला" to "open",
            "शुरू" to "open", "स्टार्ट" to "open",
            "वापस" to "back", "पीछे" to "back", "बैक" to "back", "होम" to "home",
            "हाँ" to "yes", "हां" to "yes",
            "क्या हाल" to "kya haal", "कैसे हो" to "kaise ho", "तुम कौन" to "tum kaun",
            "कौन हो" to "kaun ho", "समय" to "samay", "टाइम" to "time", "तारीख" to "tarikh",
            "धन्यवाद" to "dhanyavad", "शुक्रिया" to "shukriya", "बंद करो" to "band karo",
            "चुप" to "chup"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        face = SpideyFaceView(this)
        face.visibility = View.INVISIBLE
        root.addView(face, FrameLayout.LayoutParams(-1, -1))

        status = TextView(this).apply {
            setTextColor(Color.parseColor("#FF3B30"))
            textSize = 13f
            letterSpacing = 0.35f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            visibility = View.INVISIBLE
        }
        root.addView(status, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            bottomMargin = dp(150)
        })

        subtitle = TextView(this).apply {
            setTextColor(Color.parseColor("#E8E8E8"))
            textSize = 18f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dp(32), 0, dp(32), 0)
            visibility = View.INVISIBLE
        }
        root.addView(subtitle, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            bottomMargin = dp(60)
        })

        video = VideoView(this)
        root.addView(video, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        setContentView(root)

        face.setOnClickListener {
            if (introFinished && !listening) {
                idleMode = false
                silentCount = 0
                tts?.stop()
                speaking = false
                startListening()
            }
        }

        video.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.intro))
        video.setOnPreparedListener { mp ->
            mp.setVolume(0.6f, 0.6f)
            videoReady = true
            tryStart()
        }
        video.setOnCompletionListener { showInterface() }
        video.setOnErrorListener { _, _, _ -> showInterface(); true }

        tts = TextToSpeech(this, this)
        handler.postDelayed({ ttsDone = true; tryStart() }, 2000)
    }

    // ---------- Text to speech ----------

    override fun onInit(statusCode: Int) {
        val t = tts
        if (statusCode == TextToSpeech.SUCCESS && t != null) {
            var r = t.setLanguage(Locale("en", "IN"))
            if (r < TextToSpeech.LANG_AVAILABLE) r = t.setLanguage(Locale.US)
            ttsReady = r >= TextToSpeech.LANG_AVAILABLE
            t.setPitch(0.9f)
            t.setSpeechRate(0.95f)
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    runOnUiThread { finishUtterance(utteranceId) }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    runOnUiThread { finishUtterance(utteranceId) }
                }
            })
        }
        ttsDone = true
        tryStart()
    }

    private fun finishUtterance(id: String?) {
        speaking = false
        if (introFinished) setState(SpideyFaceView.State.IDLE)
        val cb = if (id != null) doneCallbacks.remove(id) else null
        cb?.invoke()
    }

    private fun say(text: String, onDone: (() -> Unit)? = null) {
        subtitle.text = text
        setState(SpideyFaceView.State.SPEAKING)
        speaking = true
        val t = tts
        if (t == null || !ttsReady) {
            handler.postDelayed({
                speaking = false
                setState(SpideyFaceView.State.IDLE)
                onDone?.invoke()
            }, 1800)
            return
        }
        val id = "u" + (uttCounter++)
        if (onDone != null) doneCallbacks[id] = onDone
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    // ---------- Intro ----------

    private fun tryStart() {
        if (started || !videoReady || !ttsDone) return
        started = true
        video.start()
        say(greeting) { greetingDone = true; maybeBegin() }
    }

    private fun showInterface() {
        if (introFinished) return
        introFinished = true
        video.stopPlayback()
        video.visibility = View.GONE
        face.visibility = View.VISIBLE
        status.visibility = View.VISIBLE
        subtitle.visibility = View.VISIBLE
        if (!speaking) setState(SpideyFaceView.State.IDLE)
        subtitle.text = if (ttsReady) greeting
        else "Awaaz nahi mili. Phone Settings > Text-to-speech mein Google engine chuno."
        if (!started) greetingDone = true
        maybeBegin()
    }

    private fun maybeBegin() {
        if (introFinished && greetingDone && inForeground && !listening && !speaking && !idleMode) {
            startListening()
        }
    }

    private fun setState(s: SpideyFaceView.State) {
        face.state = s
        status.text = when (s) {
            SpideyFaceView.State.IDLE -> "STANDING BY"
            SpideyFaceView.State.LISTENING -> "LISTENING"
            SpideyFaceView.State.SPEAKING -> "SPEAKING"
        }
    }

    private fun goIdle(msg: String) {
        idleMode = true
        listening = false
        silentCount = 0
        setState(SpideyFaceView.State.IDLE)
        subtitle.text = msg
    }

    private fun next() {
        handler.postDelayed({
            if (inForeground && introFinished && !listening && !speaking && !idleMode) startListening()
        }, 300)
    }

    // ---------- Listening ----------

    private fun startListening() {
        if (listening) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 11)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            goIdle("Speech service nahi mila. Google app update/install karo.")
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(this)
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        listening = true
        setState(SpideyFaceView.State.LISTENING)
        recognizer?.startListening(i)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 11) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                goIdle("Mic permission chahiye, Boss. Settings mein allow karo.")
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onPartialResults(partialResults: Bundle?) {
        val l = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!l.isNullOrEmpty()) subtitle.text = l[0]
    }

    override fun onResults(results: Bundle?) {
        listening = false
        val l = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (l.isNullOrEmpty()) {
            onError(SpeechRecognizer.ERROR_NO_MATCH)
            return
        }
        silentCount = 0
        handle(l[0])
    }

    override fun onError(error: Int) {
        listening = false
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                silentCount++
                if (silentCount >= 3) {
                    goIdle("Standing by. Mask pe tap karo jab bulana ho.")
                } else {
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) recognizer?.cancel()
                    handler.postDelayed({
                        if (inForeground && !listening && !speaking && !idleMode) startListening()
                    }, 500)
                }
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 11)
            }
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER -> {
                say("Internet nahi chal raha, Boss. Samajhne ke liye net chahiye.") {
                    goIdle("Internet check karo, phir mask pe tap karo.")
                }
            }
            else -> goIdle("Voice service mein dikkat aayi (code $error). Mask pe tap karo.")
        }
    }

    // ---------- Understanding commands ----------

    private fun normalize(s: String): String {
        var x = s.lowercase(Locale.ROOT)
        for ((k, v) in DEV) x = x.replace(k, " $v ")
        return x
    }

    private fun handle(raw: String) {
        subtitle.text = raw
        val n = normalize(raw)
        val words = n.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

        val ps = pendingStore
        if (ps != null) {
            pendingStore = null
            if (words.any { it in YES }) {
                if (openStore(ps)) say("Play Store khol raha hoon, Boss.") { next() }
                else say("Boss, Play Store nahi khul paya.") { next() }
                return
            }
        }

        if (words.any { it == "stop" || it == "chup" || it == "bye" } || n.contains("band karo")) {
            say("Okay Boss, standing by. Jab bulana ho mask pe tap karo.") {
                goIdle("Standing by. Mask pe tap karo.")
            }
            return
        }

        if (words.any { it == "back" || it == "wapas" || it == "piche" }) {
            say("Back dabane ke liye Accessibility permission chahiye, wo abhi nahi jodi hai, Boss. Home jaane ke liye bolo home.") { next() }
            return
        }

        if (words.contains("home") && !words.any { it in OPEN_WORDS && it != "start" }) {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            if (launch(i)) say("Home screen pe ja raha hoon, Boss.") { next() }
            else say("Boss, home screen nahi khul paya.") { next() }
            return
        }

        if (words.any { it in OPEN_WORDS }) {
            openApp(words)
            return
        }

        if (words.any { it == "time" || it == "samay" || it == "baje" }) {
            val t = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date())
            say("Boss, abhi $t ho rahe hain.") { next() }
            return
        }
        if (words.any { it == "date" || it == "tarikh" }) {
            val d = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH).format(Date())
            say("Aaj $d hai, Boss.") { next() }
            return
        }
        if (n.contains("kya haal") || n.contains("kaise ho") || n.contains("how are you")) {
            say("All good, Boss! What are we doing today?") { next() }
            return
        }
        if (n.contains("who are you") || n.contains("tum kaun") || n.contains("tu kaun") ||
            n.contains("kaun ho") || n.contains("your name") || n.contains("tera naam") || n.contains("tumhara naam")
        ) {
            say("Main SPIDEY hoon, Boss. Aapka personal AI assistant.") { next() }
            return
        }
        if (words.any { it == "thanks" || it == "thank" || it == "shukriya" || it == "dhanyavad" }) {
            say("Anytime, Boss!") { next() }
            return
        }

        say("Boss, is sawaal ke liye mujhe AI dimaag chahiye, jo abhi connect nahi hua. Abhi main apps khol sakta hoon, time aur date bata sakta hoon.") { next() }
    }

    private fun openApp(words: List<String>) {
        val name = words.filter { it !in OPEN_WORDS && it !in FILLER }.joinToString(" ")
        if (name.isEmpty()) {
            say("Kaunsa app kholna hai, Boss?") { next() }
            return
        }
        var ok = false
        var label = name
        var installed = true
        when (name) {
            "camera" -> { label = "Camera"; ok = launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }
            "settings", "setting" -> { label = "Settings"; ok = launch(Intent(Settings.ACTION_SETTINGS)) }
            "google" -> {
                label = "Google"
                val pi = packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox")
                ok = launch(pi ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
            }
            else -> {
                val found = findApp(name)
                if (found == null) installed = false
                else { label = found.first; ok = launch(found.second) }
            }
        }
        if (!installed) {
            pendingStore = name
            say("$name mere paas install nahi mila, Boss. Play Store pe dhundu? Haan ya na bolo.") { next() }
        } else if (ok) {
            say("$label khol raha hoon, Boss.") { next() }
        } else {
            say("Boss, $label khol nahi paya.") { next() }
        }
    }

    private fun findApp(query: String): Pair<String, Intent>? {
        val pm = packageManager
        val q = query.replace(" ", "")
        if (q.length < 2) return null
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        var bestScore = 0
        var best: Pair<String, Intent>? = null
        for (ri in pm.queryIntentActivities(main, 0)) {
            val label = ri.loadLabel(pm).toString()
            val l = label.lowercase(Locale.ROOT).replace(" ", "")
            val score = when {
                l == q -> 3
                l.startsWith(q) -> 2
                l.contains(q) -> 1
                l.length >= 4 && q.contains(l) -> 1
                else -> 0
            }
            if (score > bestScore) {
                bestScore = score
                val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
                best = Pair(label, i)
            }
        }
        return best
    }

    private fun openStore(name: String): Boolean {
        val q = Uri.encode(name)
        return launch(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$q&c=apps")))
                || launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=$q&c=apps")))
    }

    private fun launch(i: Intent?): Boolean {
        if (i == null) return false
        return try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ---------- Lifecycle ----------

    override fun onResume() {
        super.onResume()
        inForeground = true
        hideSystemBars()
        if (started && !introFinished && !video.isPlaying) video.start()
        handler.postDelayed({
            if (inForeground && introFinished && greetingDone && !listening && !speaking && !idleMode) {
                startListening()
            }
        }, 700)
    }

    override fun onPause() {
        inForeground = false
        recognizer?.cancel()
        listening = false
        if (!introFinished && started) video.pause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.let {
                it.hide(android.view.WindowInsets.Type.systemBars())
                it.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
