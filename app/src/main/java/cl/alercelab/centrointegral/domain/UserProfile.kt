package cl.alercelab.centrointegral.domain

data class UserProfile(
 val uid: String = "",
 val nombre: String = "",
 val email: String = "",
 val rol: String = "usuario",
 val estado: String = "pendiente",
 val creadoEl: Long = System.currentTimeMillis()
)
