package cl.alercelab.centrointegral.domain

data class Lugar(
 var id: String = "",
 var nombre: String = "",
 var cupo: Int? = null
)

// Representa un lugar físico disponible para actividades, con su identificador, nombre y capacidad máxima (cupo).
