package com.spidey.ai

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class SpideyFaceView(context: Context) : View(context) {

    enum class State { IDLE, LISTENING, SPEAKING }

    var state: State = State.IDLE

    private var t = 0f
    private val red = Color.parseColor("#FF1E2D")

    private val bmp: Bitmap =
        BitmapFactory.decodeStream(resources.openRawResource(R.raw.spidey_mask))

    private val softOff = IntArray(2)
    private val tightOff = IntArray(2)
    private val glowSoft: Bitmap = bmp.extractAlpha(
        Paint().apply { maskFilter = BlurMaskFilter(80f, BlurMaskFilter.Blur.NORMAL) }, softOff
    )
    private val glowTight: Bitmap = bmp.extractAlpha(
        Paint().apply { maskFilter = BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL) }, tightOff
    )

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = red }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = red; strokeWidth = 3f
    }

    private var bgRadius = 0f
    private var bgCx = 0f
    private var bgCy = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); animator.start() }
    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bgCx = w / 2f
        bgCy = h * 0.38f
        bgRadius = w * 0.85f
        bgPaint.shader = RadialGradient(
            bgCx, bgCy, bgRadius,
            intArrayOf(Color.argb(150, 255, 30, 45), Color.argb(0, 255, 30, 45)),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height * 0.38f
        val fit = min(width * 0.78f / bmp.width, height * 0.48f / bmp.height)
        val two = (2.0 * PI).toFloat() * t

        val breathe = sin(two)
        val talk = when (state) {
            State.SPEAKING -> (sin(two * 7f) * 0.5f + 0.5f) * 0.6f + (sin(two * 11f + 1f) * 0.5f + 0.5f) * 0.4f
            State.LISTENING -> sin(two * 2f) * 0.5f + 0.5f
            State.IDLE -> 0f
        }
        val scale = fit * (1f + 0.012f * breathe + (if (state == State.SPEAKING) 0.035f * talk else 0f))
        val rot = when (state) {
            State.SPEAKING -> 2.2f * sin(two * 3f)
            State.LISTENING -> 1.2f * sin(two * 2f)
            State.IDLE -> 0.6f * sin(two)
        }
        val bob = height * 0.006f * sin(two * 2f)
        val strength = when (state) {
            State.IDLE -> 0.55f + 0.15f * breathe
            State.LISTENING -> 0.75f + 0.2f * talk
            State.SPEAKING -> 0.8f + 0.2f * talk
        }.coerceIn(0f, 1f)

        bgPaint.alpha = (255 * strength).toInt()
        c.drawCircle(bgCx, bgCy, bgRadius, bgPaint)

        if (state != State.IDLE) {
            val base = bmp.height * fit * 0.5f
            val speed = if (state == State.LISTENING) 1f else 3f
            for (i in 0..2) {
                val ph = (t * speed + i / 3f) % 1f
                ring.alpha = ((1f - ph) * 150).toInt()
                c.drawCircle(cx, cy, base * (1.1f + ph * 0.8f), ring)
            }
        }

        c.save()
        c.translate(cx, cy + bob)
        c.rotate(rot)
        c.scale(scale, scale)
        c.translate(-bmp.width / 2f, -bmp.height / 2f)
        glowPaint.alpha = (210 * strength).toInt()
        c.drawBitmap(glowSoft, softOff[0].toFloat(), softOff[1].toFloat(), glowPaint)
        glowPaint.alpha = (255 * strength).toInt()
        c.drawBitmap(glowTight, tightOff[0].toFloat(), tightOff[1].toFloat(), glowPaint)
        c.drawBitmap(bmp, 0f, 0f, bmpPaint)
        c.restore()
    }
}
