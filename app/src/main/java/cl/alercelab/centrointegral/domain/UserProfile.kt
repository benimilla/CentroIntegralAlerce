package cl.alercelab.centrointegral.domain

data class UserProfile(
 val uid: String = "",
 val email: String = "",
 val nombre: String = "",
 val rol: String = "usuario",
 val telefono: String? = null,
 val aprobado: Boolean = false,
 val estado: String = "pendiente",
 val fcmToken: String? = null
)