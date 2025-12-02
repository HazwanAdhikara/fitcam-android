package com.example.fitcam.ui.screens.community

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat
import com.example.fitcam.R

fun drawStatsOnBitmap(context: Context, originalBitmap: Bitmap, statsText: String): Bitmap {
    // 1. Create a mutable copy of the bitmap to draw on
    val result = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    // 2. Setup Paint for the Box Background
    val boxPaint = Paint().apply {
        color = Color.parseColor("#99000000") // Black with Alpha
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // 3. Setup Paint for the Border
    val borderPaint = Paint().apply {
        color = Color.parseColor("#FEB21A") // FitCamYellow
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    // 4. Setup Paint for Text
    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 80f // Besar teks tergantung resolusi foto (biasanya foto kamera besar)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val titlePaint = Paint().apply {
        color = Color.parseColor("#FEB21A")
        textSize = 50f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    // 5. Coordinates (Top Left)
    val startX = 50f
    val startY = 150f
    val padding = 50f
    
    // Calculate Box Height based on lines
    val lines = statsText.uppercase().split("\n")
    val lineHeight = 90f
    val boxHeight = (lines.size * lineHeight) + 150f
    val boxWidth = 900f // Approximate width

    // 6. Draw Box & Border
    val rect = RectF(startX, startY, startX + boxWidth, startY + boxHeight)
    canvas.drawRoundRect(rect, 40f, 40f, boxPaint)
    canvas.drawRoundRect(rect, 40f, 40f, borderPaint)

    // 7. Draw Text
    canvas.drawText("FITCAM WORKOUT", startX + padding, startY + padding + 20f, titlePaint)
    
    var textY = startY + padding + 120f
    lines.forEach { line ->
        canvas.drawText(line, startX + padding, textY, textPaint)
        textY += lineHeight
    }

    return result
}