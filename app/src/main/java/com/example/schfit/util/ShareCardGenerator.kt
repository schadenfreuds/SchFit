package com.example.schfit.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.schfit.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StoryPrItem(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int
)

object ShareCardGenerator {

    fun generateWorkoutStoryBitmap(
        context: Context,
        routineName: String,
        durationMinutes: Int,
        totalVolume: Double,
        totalSets: Int,
        prList: List<StoryPrItem>,
        exerciseNames: List<String>,
        userPhotoBitmap: Bitmap? = null
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Arka Plan Degrade (Ultra Koyu Karbon & Stealth Titanyum)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.parseColor("#06080E"), Color.parseColor("#0B0E17"), Color.parseColor("#020306")),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Atletik Micro Dot-Matrix Izgara Deseni (Teknoloji & Sporcu Dokusu)
        val dotPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        val dotSpacing = 48f
        var dy = 24f
        while (dy < height) {
            var dx = 24f
            while (dx < width) {
                val distCenterY = Math.abs(dy - height / 2f) / (height / 2f)
                val distCenterX = Math.abs(dx - width / 2f) / (width / 2f)
                val alpha = ((1f - (distCenterX * 0.35f + distCenterY * 0.35f)) * 0.08f).coerceIn(0.015f, 0.08f)
                dotPaint.color = Color.argb((alpha * 255).toInt(), 255, 255, 255)
                canvas.drawCircle(dx, dy, 1.6f, dotPaint)
                dx += dotSpacing
            }
            dy += dotSpacing
        }

        // 3. Köşe Atletik Çapraz Çizgiler (Subtle Speed Lines)
        val stripePaint = Paint().apply {
            isAntiAlias = true
            color = Color.argb(12, 255, 255, 255) // %5 Opaklık
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        for (i in 0..7) {
            val offset = i * 45f
            canvas.drawLine(width - 360f + offset, 0f, width.toFloat() + offset, 360f, stripePaint)
            canvas.drawLine(-offset, height - 360f, 360f - offset, height.toFloat(), stripePaint)
        }

        // Text & Card Paints
        val paint = Paint().apply { isAntiAlias = true }

        val activeLocale = Locale.getDefault()
        val dateFormat = SimpleDateFormat("d MMMM EEEE, HH:mm", activeLocale)
        val currentDateFormatted = dateFormat.format(Date()).uppercase(activeLocale)

        val linePaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        // 4. TEK BİRLEŞİK DİKDÖRTGEN KART (Fotoğraf 1 ile Birebir Uyumlu Hero Kartı)
        val cardLeft = 70f
        val cardRight = width - 70f
        val cardTop = 120f
        val cardBottom = 1285f

        val cardRadius = 40f
        val mainCardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

        // Kart Arkası Derinlik Gölgesi
        val shadowPaint = Paint().apply {
            color = Color.parseColor("#80000000")
            isAntiAlias = true
        }
        val shadowRect = RectF(cardLeft + 4f, cardTop + 8f, cardRight + 4f, cardBottom + 8f)
        canvas.drawRoundRect(shadowRect, cardRadius, cardRadius, shadowPaint)

        // Kart Arka Planı (Koyu Mat Titanyum)
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            isAntiAlias = true
        }
        canvas.drawRoundRect(mainCardRect, cardRadius, cardRadius, cardBgPaint)

        // Kart Dış Çerçevesi
        val cardBorderPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(mainCardRect, cardRadius, cardRadius, cardBorderPaint)


        // A) BÜYÜTÜLMÜŞ FOTOĞRAF ALANI (Y: 120f -> 925f | Yükseklik: 805px)
        val photoRect = RectF(cardLeft, cardTop, cardRight, 925f)
        if (userPhotoBitmap != null) {
            drawBitmapWithTopRoundedCorners(canvas, userPhotoBitmap, photoRect, cardRadius)
        } else {
            // Varsayılan Fitness Banner'ı
            val defaultPhotoBg = Paint().apply {
                shader = LinearGradient(
                    cardLeft, cardTop, cardRight, 925f,
                    intArrayOf(Color.parseColor("#1E293B"), Color.parseColor("#0F172A"), Color.parseColor("#020617")),
                    null, Shader.TileMode.CLAMP
                )
            }
            val photoPath = Path().apply {
                addRoundRect(
                    photoRect,
                    floatArrayOf(cardRadius, cardRadius, cardRadius, cardRadius, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            }
            canvas.save()
            canvas.clipPath(photoPath)
            canvas.drawRect(photoRect, defaultPhotoBg)
            canvas.restore()
        }

        // Fotoğraf Üzeri Sol Üst Şeffaf "⚡ SCHFIT" Rozeti
        val badgeRect = RectF(cardLeft + 25f, cardTop + 25f, cardLeft + 210f, cardTop + 75f)
        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#B3000000") // %70 Şeffaf Siyah
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 16f, 16f, badgeBgPaint)
        paint.color = Color.WHITE
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("⚡ SCHFIT", cardLeft + 45f, cardTop + 60f, paint)

        // B) KART İÇİ BİLGİLER & İSTATİSTİKLER (Y: 925f -> 1285f)
        // 1. Antrenman Başlığı (Ortalı, Büyük, Kalın Beyaz)
        paint.color = Color.WHITE
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleText = if (routineName.isNotBlank()) routineName else context.getString(R.string.workout_summary_default_name)
        val titleWidth = paint.measureText(titleText)
        canvas.drawText(titleText, width / 2f - titleWidth / 2f, 995f, paint)

        // 2. Antrenman Tarih & Saat Metni (Ortalı, Gri)
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val dateSubWidth = paint.measureText(currentDateFormatted)
        canvas.drawText(currentDateFormatted, width / 2f - dateSubWidth / 2f, 1042f, paint)

        // 3. Birleşik 3'lü İstatistik Kutusu: HACİM (KG) | SÜRE (DK) | SET
        val innerStatsRect = RectF(cardLeft + 25f, 1080f, cardRight - 25f, 1250f)
        val innerStatsBg = Paint().apply {
            color = Color.parseColor("#080C16")
            isAntiAlias = true
        }
        canvas.drawRoundRect(innerStatsRect, 24f, 24f, innerStatsBg)

        val innerStatsBorder = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRoundRect(innerStatsRect, 24f, 24f, innerStatsBorder)


        val colWidth = innerStatsRect.width() / 3f

        // 1. Sütun: Hacim
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val volLabel = "${context.getString(R.string.volume).uppercase(activeLocale)} (${context.getString(R.string.kg).uppercase(activeLocale)})"
        var labelW = paint.measureText(volLabel)
        canvas.drawText(volLabel, innerStatsRect.left + colWidth * 0.5f - labelW / 2f, 1130f, paint)

        paint.color = Color.parseColor("#E53935") // Canlı Kırmızı
        paint.textSize = 50f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val volStr = "%.0f".format(totalVolume)
        var valW = paint.measureText(volStr)
        canvas.drawText(volStr, innerStatsRect.left + colWidth * 0.5f - valW / 2f, 1205f, paint)

        // Dikey Ayraç 1
        canvas.drawLine(innerStatsRect.left + colWidth, 1110f, innerStatsRect.left + colWidth, 1220f, linePaint)

        // 2. Sütun: Süre
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val durLabel = "${context.getString(R.string.duration).uppercase(activeLocale)} (${context.getString(R.string.min).uppercase(activeLocale)})"
        labelW = paint.measureText(durLabel)
        canvas.drawText(durLabel, innerStatsRect.left + colWidth * 1.5f - labelW / 2f, 1130f, paint)

        paint.color = Color.parseColor("#E53935") // Canlı Kırmızı
        paint.textSize = 50f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val durStr = "$durationMinutes"
        valW = paint.measureText(durStr)
        canvas.drawText(durStr, innerStatsRect.left + colWidth * 1.5f - valW / 2f, 1205f, paint)

        // Dikey Ayraç 2
        canvas.drawLine(innerStatsRect.left + colWidth * 2f, 1110f, innerStatsRect.left + colWidth * 2f, 1220f, linePaint)

        // 3. Sütun: Set
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val setLabel = context.getString(R.string.sets).uppercase(activeLocale)
        labelW = paint.measureText(setLabel)
        canvas.drawText(setLabel, innerStatsRect.left + colWidth * 2.5f - labelW / 2f, 1130f, paint)

        paint.color = Color.parseColor("#E53935") // Canlı Kırmızı
        paint.textSize = 50f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val setStr = "$totalSets"
        valW = paint.measureText(setStr)
        canvas.drawText(setStr, innerStatsRect.left + colWidth * 2.5f - valW / 2f, 1205f, paint)

        // 5. KARTIN ALTI: PR'lar VEYA SEANS TAMAMLAMA BİLGİSİ
        var nextY = 1320f


        if (prList.isNotEmpty()) {
            val prCardHeight = 90f + (prList.size * 65f)
            val prCardRect = RectF(cardLeft, nextY, cardRight, nextY + prCardHeight)

            val prBgPaint = Paint().apply {
                color = Color.parseColor("#422006")
                isAntiAlias = true
            }
            canvas.drawRoundRect(prCardRect, 28f, 28f, prBgPaint)

            val prBorderPaint = Paint().apply {
                color = Color.parseColor("#F59E0B")
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            canvas.drawRoundRect(prCardRect, 28f, 28f, prBorderPaint)

            paint.color = Color.parseColor("#FBBF24")
            paint.textSize = 34f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val prTitle = context.getString(R.string.share_card_new_prs, prList.size)
            canvas.drawText(prTitle, cardLeft + 35f, nextY + 55f, paint)

            var prItemY = nextY + 115f
            prList.forEach { pr ->
                paint.color = Color.WHITE
                paint.textSize = 32f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("• ${pr.exerciseName}:", cardLeft + 35f, prItemY, paint)

                val weightTag = "${pr.weightKg} ${context.getString(R.string.kg)} (${pr.reps} ${context.getString(R.string.reps)})"
                paint.color = Color.parseColor("#FDE68A")
                paint.textSize = 32f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val wWidth = paint.measureText(weightTag)
                canvas.drawText(weightTag, cardRight - 35f - wWidth, prItemY, paint)

                prItemY += 65f
            }

            nextY += prCardHeight + 35f
        } else {
            // Harika Seans Rozet Kartı (Fotoğraf 1 ile Birebir Aynı)
            val finishCardRect = RectF(cardLeft, nextY, cardRight, nextY + 130f)
            val finishBgPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                isAntiAlias = true
            }
            canvas.drawRoundRect(finishCardRect, 24f, 24f, finishBgPaint)
            canvas.drawRoundRect(finishCardRect, 24f, 24f, cardBorderPaint)

            paint.textSize = 40f
            canvas.drawText("💪", cardLeft + 35f, nextY + 78f, paint)

            paint.color = Color.parseColor("#F1F5F9")
            paint.textSize = 28f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val sessionMsg = context.getString(R.string.share_card_great_session)
            if (sessionMsg.length > 45) {
                val mid = sessionMsg.indexOf(' ', 35).let { if (it > 0) it else 40 }
                canvas.drawText(sessionMsg.take(mid), cardLeft + 95f, nextY + 58f, paint)
                canvas.drawText(sessionMsg.substring(mid).trim(), cardLeft + 95f, nextY + 98f, paint)
            } else {
                canvas.drawText(sessionMsg, cardLeft + 95f, nextY + 78f, paint)
            }

            nextY += 165f
        }

        // 6. Tamamlanan Egzersizler (Opsiyonel / Kompakt)
        if (exerciseNames.isNotEmpty() && nextY < 1720f) {
            val maxItems = if (prList.isEmpty()) 4 else 2
            val displayExercises = exerciseNames.take(maxItems)
            val exCardHeight = 75f + (displayExercises.size * 52f)
            if (nextY + exCardHeight < 1760f) {
                val exCardRect = RectF(cardLeft, nextY, cardRight, nextY + exCardHeight)
                canvas.drawRoundRect(exCardRect, 24f, 24f, cardBgPaint)
                canvas.drawRoundRect(exCardRect, 24f, 24f, cardBorderPaint)

                paint.color = Color.parseColor("#94A3B8")
                paint.textSize = 26f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val exHeader = context.getString(R.string.share_card_completed_exercises)
                canvas.drawText(exHeader, cardLeft + 35f, nextY + 48f, paint)

                var exY = nextY + 95f
                displayExercises.forEach { name ->
                    paint.color = Color.parseColor("#10B981")
                    paint.textSize = 28f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("✓", cardLeft + 35f, exY, paint)

                    paint.color = Color.parseColor("#F1F5F9")
                    paint.textSize = 28f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(name, cardLeft + 75f, exY, paint)

                    exY += 52f
                }
            }
        }

        // 7. Alt Footer & Marka
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footer1 = "SchFit Gym & Workout Tracker"
        val f1W = paint.measureText(footer1)
        canvas.drawText(footer1, width / 2f - f1W / 2f, height - 100f, paint)

        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val footer2 = "#SchFit #GymProgress #Fitness"
        val f2W = paint.measureText(footer2)
        canvas.drawText(footer2, width / 2f - f2W / 2f, height - 55f, paint)

        return bitmap
    }

    private fun drawBitmapWithTopRoundedCorners(canvas: Canvas, bitmap: Bitmap, destRect: RectF, radius: Float) {
        val path = Path().apply {
            addRoundRect(
                destRect,
                floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
                Path.Direction.CW
            )
        }

        val scale = maxOf(destRect.width() / bitmap.width.toFloat(), destRect.height() / bitmap.height.toFloat())
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val left = destRect.left + (destRect.width() - scaledWidth) / 2f
        val top = destRect.top + (destRect.height() - scaledHeight) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(left, top)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, matrix, paint)

        // Fotoğrafın alt kenarına hafif geçiş gölgesi
        val overlayPaint = Paint().apply {
            shader = LinearGradient(
                0f, destRect.bottom - 160f, 0f, destRect.bottom,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#990B0F19")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(destRect, overlayPaint)

        canvas.restore()
    }

    fun shareToStory(context: Context, bitmap: Bitmap) {
        shareBitmap(context, bitmap)
    }

    fun shareBitmap(context: Context, bitmap: Bitmap) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "schfit_workout_story.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra("com.instagram.share.ADD_TO_STORY", true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.share_card_share_error, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
        }
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap): Boolean {
        val filename = "SchFit_Workout_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "SchFit")
                }
                val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = context.contentResolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "SchFit"
                val file = File(imagesDir)
                if (!file.exists()) {
                    file.mkdirs()
                }
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
                Toast.makeText(context, context.getString(R.string.share_card_saved_gallery), Toast.LENGTH_SHORT).show()
                return true
            }
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.share_card_save_error, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
        }
        return false
    }
}
