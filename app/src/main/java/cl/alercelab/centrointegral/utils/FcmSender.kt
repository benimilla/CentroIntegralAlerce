package cl.alercelab.centrointegral.utils

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

object FcmSender {
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    private const val SERVER_KEY = "TU_CLAVE_FCM_AQUI" // reemplaza por tu clave

    fun sendPushNotification(token: String, title: String, body: String) {
        try {
            val client = OkHttpClient()
            val json = JSONObject()
            val notif = JSONObject()

            notif.put("title", title)
            notif.put("body", body)
            json.put("to", token)
            json.put("notification", notif)

            val bodyRequest = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url(FCM_URL)
                .addHeader("Authorization", "key=$SERVER_KEY")
                .post(bodyRequest)
                .build()

            client.newCall(request).execute().use { res ->
                println("FCM Response: ${res.body?.string()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
