package cl.alercelab.centrointegral.domain

data class Cita(
 var id: String = "",
 var actividadId: String = "",     // 🔹 ID de la actividad a la que pertenece
 var fechaInicioMillis: Long = 0,  // 🔹 Inicio en milisegundos (timestamp)
 var fechaFinMillis: Long = 0,     // 🔹 Fin en milisegundos (timestamp)
 var lugar: String = "",           // 🔹 Lugar donde se realiza la cita
 var observaciones: String? = null,// 🔹 Comentarios u observaciones
 var asistentes: List<String> = emptyList(), // 🔹 Participantes registrados
 var estado: String = "programada" // 🔹 Estados: programada, completada, cancelada
)
