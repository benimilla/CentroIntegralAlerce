package cl.alercelab.centrointegral.domain

data class Actividad(
 var id: String = "",
 var nombre: String = "",
 var descripcion: String = "",
 var tipo: String = "",
 var periodicidad: String = "",                 // Única / Semanal / Mensual
 var frecuencia: String? = null,                // Ej: "Cada semana", "Cada 2 meses"
 var cupo: Int? = null,                         // Capacidad máxima de participantes
 var oferente: String? = null,                  // Nombre del oferente o institución
 var socioComunitario: String? = null,          // Aliado o colaborador
 var beneficiarios: List<String> = emptyList(), // Grupos objetivo (adultos mayores, jóvenes, etc.)
 var diasAvisoPrevio: Int = 0,                  // Días mínimos de anticipación para citas
 var lugar: String = "",                        // Ubicación o sala
 var duracionMin: Int? = null,                  // Duración máxima por cita en minutos
 var fechaInicio: Long = 0,                     // Fecha de inicio de la actividad (timestamp)
 var estado: String = "activa",                 // Estado actual: activa / inactiva / cancelada
 var motivoCancelacion: String? = null,         // Razón si fue cancelada
 var citas: List<Cita> = emptyList()            // Citas asociadas a la actividad
)
