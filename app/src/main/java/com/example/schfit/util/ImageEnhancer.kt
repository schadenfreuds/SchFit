package com.example.schfit.util

import android.graphics.Bitmap
import kotlin.math.roundToInt

data class EnhanceParameters(
    val brightness: Float = 0.0f,     // -0.3f .. 0.3f (İnce pozlama ayarı)
    val contrast: Float = 0.0f,       // 0.0f .. 0.8f (Hermite S-Curve: Siyahları derinleştirir, kas hatlarını 3D yapar)
    val shadowLift: Float = 0.0f,     // 0.0f .. 0.4f (Yalnızca aşırı zifiri karanlık bölgeleri kurtarma)
    val warmth: Float = 0.0f,         // -0.3f .. 0.3f (Cilde hafif altın/bronz ton)
    val saturation: Float = 0.0f,     // -0.3f .. 0.4f (Renk doygunluğu)
    val sharpness: Float = 0.0f,      // 0.0f .. 0.8f (Kas lifleri ve damar keskinliği)
    val coachComment: String? = null
)

enum class LightingPreset(val displayName: String, val emoji: String, val params: EnhanceParameters) {
    ORIGINAL(
        displayName = "Orijinal",
        emoji = "📷",
        params = EnhanceParameters()
    ),
    GEMINI_AI(
        displayName = "Gemini AI",
        emoji = "✨",
        params = EnhanceParameters(
            brightness = 0.0f,
            contrast = 0.35f,
            shadowLift = 0.08f,
            warmth = 0.06f,
            saturation = 0.12f,
            sharpness = 0.35f
        )
    ),
    GYM_PUMP(
        displayName = "Gym Pump",
        emoji = "💪",
        params = EnhanceParameters(
            brightness = -0.02f,
            contrast = 0.50f,
            shadowLift = 0.05f,
            warmth = 0.08f,
            saturation = 0.15f,
            sharpness = 0.45f
        )
    ),
    MOODY_DARK(
        displayName = "Dramatik Karbon",
        emoji = "🌑",
        params = EnhanceParameters(
            brightness = -0.06f,
            contrast = 0.60f,
            shadowLift = 0.0f,
            warmth = 0.04f,
            saturation = -0.05f,
            sharpness = 0.40f
        )
    )
}


object ImageEnhancer {

    /**
     * Gerçek Fotoğrafçılık S-Curve & Ton Haritalama Algoritması:
     * - Fotoğrafı ABSÜRT ŞEKİLDE PARLATMAZ. Genel pozlama korunur.
     * - Gölgeleri hafif derinleştirip kas parlamalarını öne çıkararak 3D derinlik oluşturur.
     * - Tavan ışıklarındaki aşırı beyaz patlamaları (clipping) yumuşatır.
     * - Damar ve kas hatlarına mikro-kontrast (High-Pass Clarity) uygular.
     */
    fun applyParameters(source: Bitmap, params: EnhanceParameters): Bitmap {
        val width = source.width
        val height = source.height

        // 1. 256 Elemanlı Hermite S-Curve LUT Tabloları
        val rLut = IntArray(256)
        val gLut = IntArray(256)
        val bLut = IntArray(256)

        val contrast = params.contrast.coerceIn(0f, 0.85f)
        val brightness = params.brightness.coerceIn(-0.3f, 0.3f)
        val shadowLift = params.shadowLift.coerceIn(0f, 0.4f)
        val warmth = params.warmth.coerceIn(-0.3f, 0.3f)

        for (i in 0..255) {
            val x = i / 255.0f

            // A) Hermite S-Curve: Gölgeleri derinleştirir, parlak kas ışıklarını kaldırır, ortayı sabit tutar
            // f(x) = x + contrast * x * (1 - x) * (2x - 1)
            // x < 0.5 -> negatif (siyahlar zenginleşir), x > 0.5 -> pozitif (ışıklar parlar)
            val sCurveDelta = contrast * x * (1.0f - x) * (2.0f * x - 1.0f)
            
            // B) Zifiri karanlık gölgeleri yumuşatma (Yalnızca x < 0.35 için çok hafif lift)
            val shadowLiftDelta = if (x < 0.35f) {
                shadowLift * (0.35f - x) * 0.8f
            } else {
                0.0f
            }

            // C) Tavan Işığını Yumuşatma (Highlight Soft-Roll)
            val highlightTame = if (x > 0.75f) {
                (x - 0.75f) * 0.12f
            } else {
                0.0f
            }

            // D) Hafif Pozlama & Birleştirme
            val baseVal = (x + sCurveDelta + shadowLiftDelta + (brightness * 0.3f) - highlightTame).coerceIn(0.0f, 1.0f)

            // E) Sıcak Cilt / Bronz Ton (Maviyi hafif kısıp kırmızıyı dengeler)
            val rVal = (baseVal * (1.0f + warmth * 0.12f * (1.0f - baseVal))).coerceIn(0.0f, 1.0f)
            val gVal = (baseVal * (1.0f + warmth * 0.03f * (1.0f - baseVal))).coerceIn(0.0f, 1.0f)
            val bVal = (baseVal * (1.0f - warmth * 0.10f * (1.0f - baseVal))).coerceIn(0.0f, 1.0f)

            rLut[i] = (rVal * 255.0f).roundToInt().coerceIn(0, 255)
            gLut[i] = (gVal * 255.0f).roundToInt().coerceIn(0, 255)
            bLut[i] = (bVal * 255.0f).roundToInt().coerceIn(0, 255)
        }

        // 2. Piksel Dönüşümü ve Doygunluk
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val satFactor = (1.0f + params.saturation.coerceIn(-0.5f, 0.8f))

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel ushr 24) and 0xFF
            var r = rLut[(pixel ushr 16) and 0xFF]
            var g = gLut[(pixel ushr 8) and 0xFF]
            var b = bLut[pixel and 0xFF]

            if (satFactor != 1.0f) {
                val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                r = (gray + (r - gray) * satFactor).toInt().coerceIn(0, 255)
                g = (gray + (g - gray) * satFactor).toInt().coerceIn(0, 255)
                b = (gray + (b - gray) * satFactor).toInt().coerceIn(0, 255)
            }

            pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        // 3. High-Pass Clarity & Kenar Keskinleştirme (Görüntüyü beyazlatmadan sadece hatları keskinleştirir)
        val sharpness = params.sharpness.coerceIn(0f, 0.8f)
        val finalPixels = if (sharpness > 0.05f && width > 2 && height > 2) {
            applyControlledSharpening(pixels, width, height, sharpness)
        } else {
            pixels
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(finalPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Fotoğrafı parlatmadan yalnızca kas lifleri ve damar kenarlarını netleştiren kontrollü Laplacian filtresi
     */
    private fun applyControlledSharpening(srcPixels: IntArray, width: Int, height: Int, sharpness: Float): IntArray {
        val outPixels = IntArray(width * height)
        val amount = (sharpness * 0.6f).coerceIn(0.05f, 0.5f)

        for (y in 0 until height) {
            val yOffset = y * width
            val topOffset = if (y > 0) (y - 1) * width else yOffset
            val bottomOffset = if (y < height - 1) (y + 1) * width else yOffset

            for (x in 0 until width) {
                val left = if (x > 0) x - 1 else x
                val right = if (x < width - 1) x + 1 else x

                val current = srcPixels[yOffset + x]
                val top = srcPixels[topOffset + x]
                val bottom = srcPixels[bottomOffset + x]
                val leftPx = srcPixels[yOffset + left]
                val rightPx = srcPixels[yOffset + right]

                val a = (current ushr 24) and 0xFF

                val rC = (current ushr 16) and 0xFF
                val rLap = 4 * rC - ((top ushr 16) and 0xFF) - ((bottom ushr 16) and 0xFF) - ((leftPx ushr 16) and 0xFF) - ((rightPx ushr 16) and 0xFF)
                val newR = (rC + rLap * amount).roundToInt().coerceIn(0, 255)

                val gC = (current ushr 8) and 0xFF
                val gLap = 4 * gC - ((top ushr 8) and 0xFF) - ((bottom ushr 8) and 0xFF) - ((leftPx ushr 8) and 0xFF) - ((rightPx ushr 8) and 0xFF)
                val newG = (gC + gLap * amount).roundToInt().coerceIn(0, 255)

                val bC = current and 0xFF
                val bLap = 4 * bC - (top and 0xFF) - (bottom and 0xFF) - (leftPx and 0xFF) - (rightPx and 0xFF)
                val newB = (bC + bLap * amount).roundToInt().coerceIn(0, 255)

                outPixels[yOffset + x] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }
        return outPixels
    }

    /**
     * Önceden tanımlı hazır fitness modunu uygular
     */
    fun applyPreset(source: Bitmap, preset: LightingPreset): Bitmap {
        return if (preset == LightingPreset.ORIGINAL) {
            source
        } else {
            applyParameters(source, preset.params)
        }
    }

    /**
     * Dengeli ve estetik kas tonu varsayılanı
     */
    fun getLocalQuickParameters(): EnhanceParameters {
        return EnhanceParameters(
            brightness = 0.0f,
            contrast = 0.35f,
            shadowLift = 0.08f,
            warmth = 0.06f,
            saturation = 0.12f,
            sharpness = 0.35f,
            coachComment = "Doğal tonlama: Siyahlar korundu, kas hatları belirginleştirildi! 💪🔥"
        )
    }
}
