package com.example.leananglesensor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import java.util.Locale

class LeanGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 18f * density
        strokeCap = Paint.Cap.ROUND
        color = Color.rgb(55, 64, 77)
    }
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.rgb(170, 181, 196)
        textSize = 12f * density
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.rgb(170, 181, 196)
        textSize = 20f * density
        letterSpacing = 0.12f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.WHITE
        textSize = 75f * density
    }

    var leanDegrees: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var maxLeftDegrees: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var maxRightDegrees: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2f
        val centerY = height * 0.80f
        val radius = min(width * 0.40f, height * 0.69f)
        if (radius <= 0f) return

        val arc = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        canvas.drawArc(arc, 180f, 180f, false, trackPaint)

        val boundedLean = leanDegrees.coerceIn(-GAUGE_LIMIT, GAUGE_LIMIT)
        activePaint.color = when {
            kotlin.math.abs(boundedLean) >= 50f -> Color.rgb(239, 83, 80)
            boundedLean < 0f -> Color.rgb(255, 183, 77)
            else -> Color.rgb(255, 183, 77)
        }
        if (boundedLean >= 0f) {
            canvas.drawArc(
                arc,
                270f,
                boundedLean / GAUGE_LIMIT * 90f,
                false,
                activePaint
            )
        } else {
            canvas.drawArc(
                arc,
                270f + boundedLean / GAUGE_LIMIT * 90f,
                -boundedLean / GAUGE_LIMIT * 90f,
                false,
                activePaint
            )
        }

        //val tickValues = intArrayOf(-60, -45, -30, -15, 0, 15, 30, 45, 60)
        //for (tickValue in tickValues) {
            //val angle = gaugeAngle(tickValue.toFloat())
           // val inner = point(centerX, centerY, radius - 16f * density, angle)
            //val outer = point(centerX, centerY, radius + 2f * density, angle)
           // canvas.drawLine(inner.first, inner.second, outer.first, outer.second, tickPaint)
       // }

        val labelValues = intArrayOf(-60, -30, 0, 30, 60)
        for (labelValue in labelValues) {
            val angle = gaugeAngle(labelValue.toFloat())
            val labelPoint = point(centerX, centerY, radius + 29f * density, angle)
            val label = when {
                labelValue > 0 -> "$labelValue°"
                else -> "$labelValue°"
            }
            canvas.drawText(label, labelPoint.first, labelPoint.second + 5f * density, labelPaint)
        }

        canvas.drawText("LEAN ANGLE", centerX, centerY - radius * 0.54f, titlePaint)
        canvas.drawText(
            String.format(Locale.US,"%.1f°", abs(leanDegrees)),
            centerX,
            centerY - radius * 0.025f,
            valuePaint
        )

    }

    private fun gaugeAngle(value: Float): Float {
        return 270f + (value / GAUGE_LIMIT) * 90f
    }

    private fun point(centerX: Float, centerY: Float, radius: Float, degrees: Float): Pair<Float, Float> {
        val radians = Math.toRadians(degrees.toDouble())
        return Pair(
            centerX + radius * cos(radians).toFloat(),
            centerY + radius * sin(radians).toFloat()
        )
    }

    companion object {
        private const val GAUGE_LIMIT = 60f
    }
}
