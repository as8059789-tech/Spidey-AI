package com.spidey.ai

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var face: SpideyFaceView
    private lateinit var status: TextView
    private lateinit var subtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        face = SpideyFaceView(this)
        root.addView(face, FrameLayout.LayoutParams(-1, -1))

        status = TextView(this).apply {
            setTextColor(Color.parseColor("#FF3B30"))
            textSize = 13f
            letterSpacing = 0.35f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
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
        }
        root.addView(subtitle, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            bottomMargin = dp(70)
        })

        setContentView(root)
        setState(SpideyFaceView.State.IDLE)

        // Stage 1 preview only: tap the face to cycle states.
        // This will be removed once real voice input arrives in Stage 3.
        face.setOnClickListener { cycleState() }
    }

    private var preview = 0
    private fun cycleState() {
        preview = (preview + 1) % 3
        setState(
            when (preview) {
                0 -> SpideyFaceView.State.IDLE
                1 -> SpideyFaceView.State.LISTENING
                else -> SpideyFaceView.State.SPEAKING
            }
        )
    }

    private fun setState(s: SpideyFaceView.State) {
        face.state = s
        when (s) {
            SpideyFaceView.State.IDLE -> {
                status.text = "STANDING BY"
                subtitle.text = ""
            }
            SpideyFaceView.State.LISTENING -> {
                status.text = "LISTENING"
                subtitle.text = "Hey Spidey, kya haal hai?"
            }
            SpideyFaceView.State.SPEAKING -> {
                status.text = "SPEAKING"
                subtitle.text = "All good, Boss! What are we doing today?"
            }
        }
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
