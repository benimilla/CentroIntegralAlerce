package cl.alercelab.centrointegral.domain

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserProfile(
 var uid: String = "",
 var email: String = "",
 var nombre: String = "",
 var rol: String = "usuario",

 var telefono: String? = null,

 // Campos que Firestore SÍ guarda, pero tu clase NO tenía
 var aprobado: Boolean = false,
 var estado: String = "pendiente",
 var emailVerificado: Boolean = false,
 var fechaRegistro: Long = 0L,
)

// Representa el perfil de un usuario en la aplicación, incluyendo datos personales,
// rol, estado de aprobación y token de notificaciones FCM.