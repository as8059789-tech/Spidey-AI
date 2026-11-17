package com.spidey.ai

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SpideyFaceView(context: Context) : View(context) {

    enum class State { IDLE, LISTENING, SPEAKING }

    var state: State = State.IDLE
        set(v) { field = v; invalidate() }

    private var t = 0f // 0..1 looping clock
    private val red = Color.parseColor("#FF1E2D")

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = red; strokeWidth = 6f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#1A0004")
    }
    private val web = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#80FF1E2D"); strokeWidth = 1.6f
    }
    private val eyeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#F4F4F4")
    }
    private val eyeGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = red
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = red; strokeWidth = 3f
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    init { setLayerType(LAYER_TYPE_SOFTWARE, null) } // needed for blur glow

    override fun onAttachedToWindow() { super.onAttachedToWindow(); animator.start() }
    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    private fun pulse(): Float {
        val speed = when (state) { State.IDLE -> 1f; State.LISTENING -> 2f; State.SPEAKING -> 4f }
        val amp = when (state) { State.IDLE -> 0.25f; State.LISTENING -> 0.45f; State.SPEAKING -> 0.7f }
        return 0.65f + amp * 0.5f * (1f + sin(2.0 * PI * t * speed).toFloat()) * 0.7f
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height * 0.40f
        val r = min(width, height) * 0.30f
        val p = pulse()

        // voice ring(s)
        if (state != State.IDLE) {
            for (i in 0..2) {
                val ph = (t * (if (state == State.LISTENING) 1f else 3f) + i / 3f) % 1f
                ring.alpha = ((1f - ph) * 140).toInt()
                ring.maskFilter = null
                c.drawCircle(cx, cy, r * (1.35f + ph * 0.9f), ring)
            }
        }

        val head = headPath(cx, cy, r)

        // outer glow
        glow.maskFilter = BlurMaskFilter(r * 0.22f * p + 4f, BlurMaskFilter.Blur.NORMAL)
        glow.alpha = (255 * min(1f, p)).toInt()
        glow.strokeWidth = r * 0.06f
        c.drawPath(head, glow)

        // body + webbing
        c.drawPath(head, fillPaint)
        c.save()
        c.clipPath(head)
        web.maskFilter = null
        for (i in 0 until 14) {
            val a = (i / 14f) * 2f * PI.toFloat()
            c.drawLine(cx, cy - 0.1f * r, cx + cos(a) * r * 2f, cy - 0.1f * r + sin(a) * r * 2f, web)
        }
        for (k in 1..5) c.drawCircle(cx, cy - 0.1f * r, r * 0.26f * k, web)
        c.restore()

        // crisp edge
        glow.maskFilter = null
        glow.alpha = 255
        glow.strokeWidth = r * 0.03f
        c.drawPath(head, glow)

        // eyes
        for (side in intArrayOf(-1, 1)) {
            val eye = eyePath(cx, cy, r, side)
            eyeGlow.maskFilter = BlurMaskFilter(r * 0.10f, BlurMaskFilter.Blur.NORMAL)
            eyeGlow.alpha = (255 * min(1f, p)).toInt()
            c.drawPath(eye, eyeGlow)
            c.drawPath(eye, eyeFill)
        }
    }

    private fun headPath(cx: Float, cy: Float, r: Float) = Path().apply {
        moveTo(cx, cy - 1.25f * r)
        cubicTo(cx + 0.95f * r, cy - 1.25f * r, cx + 1.0f * r, cy - 0.2f * r, cx + 0.8f * r, cy + 0.5f * r)
        cubicTo(cx + 0.6f * r, cy + 1.15f * r, cx + 0.25f * r, cy + 1.3f * r, cx, cy + 1.3f * r)
        cubicTo(cx - 0.25f * r, cy + 1.3f * r, cx - 0.6f * r, cy + 1.15f * r, cx - 0.8f * r, cy + 0.5f * r)
        cubicTo(cx - 1.0f * r, cy - 0.2f * r, cx - 0.95f * r, cy - 1.25f * r, cx, cy - 1.25f * r)
        close()
    }

    private fun eyePath(cx: Float, cy: Float, r: Float, s: Int) = Path().apply {
        moveTo(cx + s * 0.08f * r, cy + 0.25f * r)
        quadTo(cx + s * 0.35f * r, cy - 0.45f * r, cx + s * 0.85f * r, cy - 0.35f * r)
        quadTo(cx + s * 0.75f * r, cy + 0.15f * r, cx + s * 0.08f * r, cy + 0.25f * r)
        close()
    }
}
