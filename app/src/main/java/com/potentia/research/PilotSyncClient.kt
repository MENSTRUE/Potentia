package com.potentia.research

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object PilotSyncClient {

    sealed interface Result {
        data object Success : Result
        data class Retryable(val message: String) : Result
        data class PermanentFailure(val message: String) : Result
    }

    fun upload(record: PilotSessionRecord): Result {
        if (!PilotSyncConfig.isConfigured) {
            return Result.PermanentFailure("Endpoint sinkronisasi pilot belum dikonfigurasi.")
        }

        val connection = runCatching {
            (URL(PilotSyncConfig.endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 25_000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
        }.getOrElse { error ->
            return Result.Retryable(error.message ?: "Tidak dapat membuka koneksi sinkronisasi.")
        }

        return try {
            val payload = PilotSyncPayload.create(
                record = record,
                uploadToken = PilotSyncConfig.uploadToken
            )

            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val body = runCatching {
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            }.getOrDefault("")

            when {
                code in 200..299 -> {
                    val response = runCatching { JSONObject(body) }.getOrNull()
                    when {
                        response?.optBoolean("ok", false) == true -> Result.Success
                        response != null -> Result.PermanentFailure(
                            response.optString("error", "Server menolak payload sinkronisasi.")
                        )
                        else -> Result.Retryable("Respons server sinkronisasi tidak dapat dibaca.")
                    }
                }
                code == 408 || code == 425 || code == 429 || code >= 500 ->
                    Result.Retryable("Server sync HTTP $code ${body.take(160)}".trim())
                else ->
                    Result.PermanentFailure("Server sync HTTP $code ${body.take(160)}".trim())
            }
        } catch (error: IOException) {
            Result.Retryable(error.message ?: "Gangguan jaringan saat sinkronisasi.")
        } catch (error: Exception) {
            Result.PermanentFailure(error.message ?: "Sinkronisasi gagal.")
        } finally {
            connection.disconnect()
        }
    }
}
