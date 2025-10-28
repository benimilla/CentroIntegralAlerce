package cl.alercelab.centrointegral.domain

data class Cita(
 var id: String = "",
 var actividadId: String = "",
 var fechaInicioMillis: Long = 0L,
 var fechaFinMillis: Long = 0L,
 var lugar: String = "",
 var motivo: String? = null
) {
 val duracionMin: Int
  get() = ((fechaFinMillis - fechaInicioMillis) / 60000).toInt()
}


