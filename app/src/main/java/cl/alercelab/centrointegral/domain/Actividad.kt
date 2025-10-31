package cl.alercelab.centrointegral.domain

data class Actividad(
 var id: String = "",
 var nombre: String = "",
 var descripcion: String = "",
 var tipo: String = "",
 var periodicidad: String = "",
 var cupo: Int? = null,
 var oferente: String? = null,
 var socioComunitario: String? = null,
 var beneficiarios: List<String> = emptyList(),
 var diasAvisoPrevio: Int = 0,
 var lugar: String = "",
 var fechaInicio: Long = 0,
 var estado: String = "activa",
 var motivoCancelacion: String? = null,
 var citas: List<Cita> = emptyList()
)
