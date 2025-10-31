package cl.alercelab.centrointegral

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import cl.alercelab.centrointegral.notifications.NotificationHelper

/**
 * Clase de aplicación principal del Centro Integral.
 * Inicializa Firebase, configura Firestore y crea el canal de notificaciones.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // --- 🔹 Inicialización de Firebase ---
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        // --- 🔹 Configuración global de Firestore ---
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Permite acceso sin conexión
            .build()

        val db = FirebaseFirestore.getInstance()
        db.firestoreSettings = settings

        // --- 🔹 Creación del canal de notificaciones ---
        NotificationHelper.createChannel(this)
    }
}
