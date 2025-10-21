package cl.alercelab.centrointegral

import android.app.Application
import cl.alercelab.centrointegral.notifications.NotificationHelper
import com.google.firebase.FirebaseApp

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificationHelper.createChannel(this)
    }
}
