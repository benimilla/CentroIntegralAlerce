package cl.alercelab.centrointegral.domain

data class Cita(
 var id: String = "",                      // ID único de la cita
 var actividadId: String = "",             // ID de la actividad relacionada
 var fechaInicioMillis: Long = 0,          // Timestamp de inicio (en milisegundos)
 var fechaFinMillis: Long = 0,             // Timestamp de finalización (en milisegundos)
 var lugar: String = "",                   // Lugar donde se realizará la cita
 var observaciones: String? = null,        // Comentarios, notas o motivo
 var asistentes: List<String> = emptyList(), // Lista de asistentes o beneficiarios
 var estado: String = "programada",        // Estado actual: programada, completada, cancelada
 var duracionMin: Int? = null,             // Duración real o planificada en minutos (opcional)
 var fechaCreacion: Long = System.currentTimeMillis(), // Fecha de registro
 var ultimaActualizacion: Long? = null     // Última modificación (si la cita se edita)
)
