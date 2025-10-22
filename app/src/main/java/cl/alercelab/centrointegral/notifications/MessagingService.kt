package cl.alercelab.centrointegral.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = auth.currentUser?.uid ?: return
        db.collection("usuarios").document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener { Log.d("FCM", "Token actualizado correctamente") }
            .addOnFailureListener { Log.w("FCM", "Error al guardar token", it) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Centro Integral Alerce"
        val body = message.notification?.body ?: "Tienes una nueva notificación"
        NotificationHelper.showSimpleNotification(applicationContext, title, body)
    }
}
