package cl.alercelab.centrointegral

import android.app.Application
import com.google.firebase.FirebaseApp
import cl.alercelab.centrointegral.notifications.NotificationHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // --- Inicialización de Firebase ---
        FirebaseApp.initializeApp(this)

        // --- Creación del canal de notificaciones ---
        NotificationHelper.createChannel(this)
    }
}
