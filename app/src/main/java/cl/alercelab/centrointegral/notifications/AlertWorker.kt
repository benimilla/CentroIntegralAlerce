package cl.alercelab.centrointegral.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import cl.alercelab.centrointegral.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking

class AlertWorker(
    private val ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {

    override fun doWork(): Result = runBlocking {
        val actividadId = inputData.getString("actividadId") ?: return@runBlocking Result.failure()
        val citaId = inputData.getString("citaId") ?: return@runBlocking Result.failure()

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        val actSnap = db.collection("actividades").document(actividadId).get().await()
        val actividad = actSnap.getString("nombre") ?: "Actividad"
        val tipo = actSnap.getString("tipo") ?: "-"
        val beneficiarios = actSnap.get("beneficiarios") as? List<String> ?: emptyList()
        val oferente = actSnap.getString("oferente")
        val socio = actSnap.getString("socioComunitario")

        // --- Filtrar según el rol del usuario actual ---
        val perfilSnap = uid?.let { db.collection("usuarios").document(it).get().await() }
        val rol = perfilSnap?.getString("rol") ?: "usuario"

        val permitido = when (rol) {
            "admin", "gestor" -> true
            else -> beneficiarios.contains(uid) || oferente == uid || socio == uid
        }

        if (!permitido) return@runBlocking Result.success()

        // --- Mostrar notificación ---
        showNotification(actividad, tipo)
        Result.success()
    }

    private fun showNotification(nombre: String, tipo: String) {
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "alertas_actividades"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Actividades",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.drawable.ic_notification) // asegúrate de tener un ícono válido
            .setContentTitle("Recordatorio de actividad")
            .setContentText("Se aproxima: $nombre ($tipo)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify((0..999999).random(), notif)
    }
}
