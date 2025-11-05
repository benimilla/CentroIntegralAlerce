package cl.alercelab.centrointegral.domain

data class UserProfile(
 var uid: String = "",
 var email: String = "",
 var nombre: String = "",
 var rol: String = "usuario",
 var telefono: String? = null,
 var aprobado: Boolean = false,
 var estado: String = "pendiente",
 var fcmToken: String? = null
)

// Representa el perfil de un usuario en la aplicación, incluyendo datos personales,
// rol, estado de aprobación y token de notificaciones FCM.