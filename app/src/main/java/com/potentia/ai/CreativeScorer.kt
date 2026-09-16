package com.potentia.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.ln
import kotlin.math.sqrt

data class CreativeScoreResult(
    val rawScore: Double,
    val uiScore: Double,
    val outOfDomain: Boolean,
    val experimental: Boolean = true,
    val warning: String? = null
)

class CreativeScorer private constructor(
    private val model: ModelBundle
) {

    companion object {
        const val DEFAULT_ASSET_PATH =
            "potentia_ai/creative_tfidf_ridge_v1.json"

        fun fromAssets(
            context: Context,
            assetPath: String = DEFAULT_ASSET_PATH
        ): CreativeScorer {
            val json = context.assets
                .open(assetPath)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            return fromJson(json)
        }

        fun fromJson(json: String): CreativeScorer {
            return CreativeScorer(
                ModelBundle.parse(JSONObject(json))
            )
        }
    }

    fun score(
        task: String,
        text: String
    ): CreativeScoreResult {

        val normalizedTask = task.lowercase(Locale.ROOT)
        val modelInput =
            "__TASK_${normalizedTask.uppercase(Locale.ROOT)}__ $text"

        val wordVector = sparseTfidf(
            features = wordNgrams(
                text = preprocess(modelInput),
                minN = model.word.ngramMin,
                maxN = model.word.ngramMax
            ),
            spec = model.word
        )

        val charVector = sparseTfidf(
            features = charWbNgrams(
                input = preprocess(modelInput),
                minN = model.char.ngramMin,
                maxN = model.char.ngramMax
            ),
            spec = model.char
        )

        var prediction = model.intercept

        for ((index, value) in wordVector) {
            prediction += model.word.coef[index] * value
        }

        for ((index, value) in charVector) {
            prediction += model.char.coef[index] * value
        }

        val clipped = prediction.coerceIn(
            model.targetMin,
            model.targetMax
        )

        val uiScore = if (model.targetMax > model.targetMin) {
            ((clipped - model.targetMin) /
                (model.targetMax - model.targetMin)) * 100.0
        } else {
            0.0
        }

        val outOfDomain =
            !model.trainingTasks.contains(normalizedTask)

        return CreativeScoreResult(
            rawScore = clipped,
            uiScore = uiScore,
            outOfDomain = outOfDomain,
            experimental = true,
            warning = if (outOfDomain) {
                "Task '$normalizedTask' was not present in V5 training data. " +
                    "Treat this score as experimental only."
            } else {
                null
            }
        )
    }

    private fun preprocess(input: String): String {
        val lowered = input.lowercase(Locale.ROOT)
        val normalized = Normalizer.normalize(
            lowered,
            Normalizer.Form.NFKD
        )

        val output = StringBuilder()

        for (ch in normalized) {
            val type = Character.getType(ch)

            if (
                type != Character.NON_SPACING_MARK.toInt() &&
                type != Character.COMBINING_SPACING_MARK.toInt() &&
                type != Character.ENCLOSING_MARK.toInt()
            ) {
                output.append(ch)
            }
        }

        return output.toString()
    }

    private fun wordNgrams(
        text: String,
        minN: Int,
        maxN: Int
    ): List<String> {
        val matcher = WORD_PATTERN.matcher(text)
        val tokens = ArrayList<String>()

        while (matcher.find()) {
            tokens.add(matcher.group())
        }

        val result = ArrayList<String>()

        for (n in minN..maxN) {
            if (n > tokens.size) break

            for (start in 0..tokens.size - n) {
                result.add(
                    tokens.subList(start, start + n)
                        .joinToString(" ")
                )
            }
        }

        return result
    }

    private fun charWbNgrams(
        input: String,
        minN: Int,
        maxN: Int
    ): List<String> {
        val text = MULTI_WS_PATTERN
            .matcher(input)
            .replaceAll(" ")

        val result = ArrayList<String>()

        for (rawWord in text.trim().split(WHITESPACE_REGEX)) {
            if (rawWord.isEmpty()) continue

            val word = " $rawWord "
            val wordLength = word.length

            for (n in minN..maxN) {
                var offset = 0

                result.add(
                    word.substring(
                        offset,
                        minOf(offset + n, wordLength)
                    )
                )

                while (offset + n < wordLength) {
                    offset += 1
                    result.add(
                        word.substring(
                            offset,
                            minOf(offset + n, wordLength)
                        )
                    )
                }

                // Mirrors sklearn's _char_wb_ngrams behavior for short words.
                if (offset == 0) break
            }
        }

        return result
    }

    private fun sparseTfidf(
        features: List<String>,
        spec: VectorizerSpec
    ): Map<Int, Double> {
        val counts = HashMap<Int, Int>()

        for (feature in features) {
            val index = spec.vocabulary[feature] ?: continue
            counts[index] = (counts[index] ?: 0) + 1
        }

        if (counts.isEmpty()) return emptyMap()

        val values = HashMap<Int, Double>()
        var normSquared = 0.0

        for ((index, count) in counts) {
            val tf = if (spec.sublinearTf) {
                1.0 + ln(count.toDouble())
            } else {
                count.toDouble()
            }

            val value = tf * spec.idf[index]
            values[index] = value
            normSquared += value * value
        }

        if (spec.norm == "l2") {
            val norm = sqrt(normSquared)

            if (norm > 0.0) {
                for ((index, value) in values.toMap()) {
                    values[index] =
                        (value / norm) * spec.featureWeight
                }
            }
        } else if (spec.featureWeight != 1.0) {
            for ((index, value) in values.toMap()) {
                values[index] = value * spec.featureWeight
            }
        }

        return values
    }
}

private data class VectorizerSpec(
    val ngramMin: Int,
    val ngramMax: Int,
    val sublinearTf: Boolean,
    val norm: String?,
    val featureWeight: Double,
    val vocabulary: Map<String, Int>,
    val idf: DoubleArray,
    val coef: DoubleArray
) {
    companion object {
        fun parse(json: JSONObject): VectorizerSpec {
            val vocabJson = json.getJSONObject("vocabulary")
            val vocabulary = HashMap<String, Int>(vocabJson.length())
            val keys = vocabJson.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                vocabulary[key] = vocabJson.getInt(key)
            }

            return VectorizerSpec(
                ngramMin = json.getInt("ngram_min"),
                ngramMax = json.getInt("ngram_max"),
                sublinearTf = json.getBoolean("sublinear_tf"),
                norm = if (json.isNull("norm")) {
                    null
                } else {
                    json.getString("norm")
                },
                featureWeight = json.optDouble("feature_weight", 1.0),
                vocabulary = vocabulary,
                idf = json.getJSONArray("idf").toDoubleArrayCompat(),
                coef = json.getJSONArray("ridge_coef").toDoubleArrayCompat()
            )
        }
    }
}

private data class ModelBundle(
    val targetMin: Double,
    val targetMax: Double,
    val trainingTasks: Set<String>,
    val word: VectorizerSpec,
    val char: VectorizerSpec,
    val intercept: Double
) {
    companion object {
        fun parse(root: JSONObject): ModelBundle {
            return ModelBundle(
                targetMin = root.getDouble("target_min"),
                targetMax = root.getDouble("target_max"),
                trainingTasks = root
                    .getJSONArray("training_tasks")
                    .toStringSetCompat(),
                word = VectorizerSpec.parse(
                    root.getJSONObject("word_vectorizer")
                ),
                char = VectorizerSpec.parse(
                    root.getJSONObject("char_vectorizer")
                ),
                intercept = root
                    .getJSONObject("ridge")
                    .getDouble("intercept")
            )
        }
    }
}

private val WORD_PATTERN: Pattern =
    Pattern.compile("(?U)\\b\\w\\w+\\b")

private val MULTI_WS_PATTERN: Pattern =
    Pattern.compile("\\s\\s+")

private val WHITESPACE_REGEX =
    Regex("\\s+")

private fun JSONArray.toDoubleArrayCompat(): DoubleArray {
    return DoubleArray(length()) { index ->
        getDouble(index)
    }
}

private fun JSONArray.toStringSetCompat(): Set<String> {
    val result = LinkedHashSet<String>()

    for (index in 0 until length()) {
        result.add(
            getString(index).lowercase(Locale.ROOT)
        )
    }

    return result
}
