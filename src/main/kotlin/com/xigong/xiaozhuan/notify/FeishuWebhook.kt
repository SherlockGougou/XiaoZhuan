package com.xigong.xiaozhuan.notify

import com.xigong.xiaozhuan.log.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FeishuWebhook {

    suspend fun sendText(webhookUrl: String, text: String) = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(webhookUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            val payload = buildJson(text)
            BufferedWriter(OutputStreamWriter(conn.outputStream, Charsets.UTF_8)).use { out ->
                out.write(payload)
            }
            val code = conn.responseCode
            AppLogger.info("Feishu", "Webhook响应码: $code")
            conn.disconnect()
        }.onFailure { e ->
            AppLogger.error("Feishu", "Webhook发送失败", e)
        }
    }

    private fun buildJson(text: String): String {
        val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
        return "{" +
                "\"msg_type\":\"text\"," +
                "\"content\":{\"text\":\"$escaped\"}" +
                "}"
    }
}

