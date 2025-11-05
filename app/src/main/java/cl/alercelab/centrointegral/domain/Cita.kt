package cl.alercelab.centrointegral.domain

import java.io.Serializable

data class Cita(
 var id: String = "",
 var actividadId: String = "",
 var fechaInicioMillis: Long = 0,
 var fechaFinMillis: Long = 0,
 var lugar: String = "",
 var observaciones: String? = null,
 var asistentes: List<String> = emptyList(),
 var estado: String = "programada",
 var duracionMin: Int? = null,
 var fechaCreacion: Long = System.currentTimeMillis(),
 var ultimaActualizacion: Long? = null
) : Serializable

// Representa una cita o evento programado con información sobre la actividad asociada,
// horarios, lugar, asistentes, estado y fechas de creación o actualización.