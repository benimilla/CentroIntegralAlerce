package cl.alercelab.centrointegral.notifications

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.alercelab.centrointegral.R

class AlertWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val actividadId = inputData.getString("actividadId") ?: return Result.failure()
        val citaId = inputData.getString("citaId") ?: return Result.failure()

        val builder = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Recordatorio de actividad")
            .setContentText("Actividad $actividadId, cita $citaId pronto")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        NotificationManagerCompat.from(applicationContext)
            .notify((System.currentTimeMillis() % 100000).toInt(), builder.build())

        return Result.success()
    }
}
