package cl.alercelab.centrointegral.domain

data class Auditoria(
    var id: String = "",
    var usuarioId: String = "",
    var usuarioNombre: String = "",
    var fecha: Long = System.currentTimeMillis(),
    var modulo: String = "", // Ej: "Actividades", "Citas", "GestorUsuarios"
    var accion: String = "", // Ej: "Creación", "Edición", "Eliminación", "Aprobación", "Rechazo"
    var entidadId: String = "", // ID del objeto afectado
    var descripcion: String = "", // texto descriptivo
    var cambios: Map<String, String> = emptyMap() // detalle de campos modificados
)

// Representa un registro de auditoría que almacena la acción realizada por un usuario,
// el módulo afectado, la descripción del cambio y los detalles específicos de los campos modificados.