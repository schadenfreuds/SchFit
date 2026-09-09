package com.example.schfit.util

import android.graphics.Bitmap
import android.graphics.Matrix
import com.example.schfit.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GeminiLightingService {

    private const val MODEL_NAME = "gemini-1.5-flash"

    /**
     * Gemini 1.5 Flash ile fotoğrafın ton ve kas derinliği analizini yapar
     */
    suspend fun analyzeAndGetLightingParams(
        sourceBitmap: Bitmap,
        customApiKey: String? = null
    ): Result<EnhanceParameters> = withContext(Dispatchers.IO) {
        try {
            val apiKey = customApiKey?.takeIf { it.isNotBlank() }
                ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("Gemini API Anahtarı bulunamadı."))

            val generativeModel = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey
            )

            // API'ye göndermeden önce görseli optimize et (max 720px)
            val scaledBitmap = scaleBitmapForAi(sourceBitmap, maxDimension = 720)

            val prompt = """
                You are a world-class bodybuilding and fitness physique colorist (style: David Laid, Gymshark, Raw Aesthetics, Olympus stage photography).
                Analyze this workout photo for aesthetic S-curve contrast, 3D muscle depth, and natural golden skin tones.

                RULES:
                1. DO NOT OVEREXPOSE OR BLOW OUT THE PHOTO: Keep brightness neutral (brightness between -0.08 and 0.05).
                2. 3D MUSCLE DEPTH (HERMITE S-CURVE): Deepen shadows and pop muscle highlights (contrast between 0.25 and 0.50).
                3. TAME CEILING LIGHTS: Soften harsh fluorescent glare on shoulders/face.
                4. NATURAL GOLDEN PUMP: Neutralize sickly gym greens/yellows with healthy skin tone (warmth between 0.04 and 0.12).
                5. SHARP MUSCLE DEFINITION: Enhance veins and striations tastefully (sharpness between 0.25 and 0.45).

                Return ONLY a valid JSON object matching this schema (no markdown, no backticks):
                {
                  "brightness": 0.0,
                  "contrast": 0.35,
                  "shadowLift": 0.08,
                  "warmth": 0.06,
                  "saturation": 0.12,
                  "sharpness": 0.35,
                  "coachComment": "Derin siyahlar ve 3D kas definasyonu ile pump netleştirildi! 🔥💪"
                }
                Constraints:
                - brightness: float between -0.10 and 0.08
                - contrast: float between 0.20 and 0.55
                - shadowLift: float between 0.0 and 0.20
                - warmth: float between 0.02 and 0.15
                - saturation: float between 0.05 and 0.22
                - sharpness: float between 0.20 and 0.50
                - coachComment: 1 concise motivating sentence in Turkish with 1-2 emojis.
            """.trimIndent()

            val response = generativeModel.generateContent(
                content {
                    image(scaledBitmap)
                    text(prompt)
                }
            )

            val responseText = response.text?.trim() ?: ""
            val jsonString = extractJsonFromResponse(responseText)

            val jsonObject = JSONObject(jsonString)
            val brightness = jsonObject.optDouble("brightness", 0.0).toFloat()
            val contrast = jsonObject.optDouble("contrast", 0.35).toFloat()
            val shadowLift = jsonObject.optDouble("shadowLift", 0.08).toFloat()
            val warmth = jsonObject.optDouble("warmth", 0.06).toFloat()
            val saturation = jsonObject.optDouble("saturation", 0.12).toFloat()
            val sharpness = jsonObject.optDouble("sharpness", 0.35).toFloat()
            val coachComment = jsonObject.optString("coachComment", "Gemini AI: 3D kas derinliği ve zengin tonlama optimize edildi! ✨💪")

            val params = EnhanceParameters(
                brightness = brightness,
                contrast = contrast,
                shadowLift = shadowLift,
                warmth = warmth,
                saturation = saturation,
                sharpness = sharpness,
                coachComment = coachComment
            )

            Result.success(params)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scaleBitmapForAi(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxDim = maxOf(width, height)
        if (maxDim <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxDim
        val matrix = Matrix().apply { postScale(scale, scale) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun extractJsonFromResponse(text: String): String {
        val startIdx = text.indexOf('{')
        val endIdx = text.lastIndexOf('}')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx + 1)
        }
        return text
    }
}
