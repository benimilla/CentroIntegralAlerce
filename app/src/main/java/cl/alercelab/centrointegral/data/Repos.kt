package cl.alercelab.centrointegral.data

import android.util.Log
import cl.alercelab.centrointegral.domain.*
import cl.alercelab.centrointegral.utils.FcmSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class Repos {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // -----------------------------------------------------------
    // 🔹 AUTENTICACIÓN
    // -----------------------------------------------------------

    suspend fun register(nombre: String, email: String, password: String, rol: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return false
            val profile = UserProfile(
                uid = uid,
                nombre = nombre,
                email = email,
                rol = rol,
                aprobado = false,
                estado = "pendiente"
            )
            db.collection("usuarios").document(uid).set(profile).await()
            true
        } catch (e: Exception) {
            Log.e("REGISTER", "Error al registrar: ${e.message}")
            false
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            Log.e("LOGIN", "Error al iniciar sesión: ${e.message}")
            false
        }
    }

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            Log.e("RESET_PASSWORD", "Error: ${e.message}")
            false
        }
    }

    fun logout() = auth.signOut()

    // -----------------------------------------------------------
    // 🔹 USUARIOS
    // -----------------------------------------------------------

    suspend fun currentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("usuarios").document(uid).get().await()
        return doc.toObject<UserProfile>()
    }

    suspend fun listAllUsers(): List<UserProfile> {
        val snap = db.collection("usuarios").get().await()
        return snap.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun listPendingUsers(): List<UserProfile> {
        val snap = db.collection("usuarios").whereEqualTo("aprobado", false).get().await()
        return snap.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun approveUser(uid: String) {
        db.collection("usuarios").document(uid)
            .update(mapOf("aprobado" to true, "estado" to "activo")).await()
    }

    suspend fun rejectUser(uid: String) {
        db.collection("usuarios").document(uid).delete().await()
    }

    suspend fun updateUserRole(uid: String, newRole: String) {
        db.collection("usuarios").document(uid).update("rol", newRole).await()
    }

    suspend fun deleteUser(uid: String) {
        db.collection("usuarios").document(uid).delete().await()
    }

    suspend fun updateUserData(uid: String, nuevoNombre: String, nuevoEmail: String) {
        db.collection("usuarios").document(uid)
            .update(mapOf("nombre" to nuevoNombre, "email" to nuevoEmail)).await()
    }

    // -----------------------------------------------------------
    // 🔹 ACTIVIDADES Y CITAS
    // -----------------------------------------------------------

    suspend fun crearActividadConCitas(actividad: Actividad, citas: List<Cita>) {
        // ⬇️ Calcular fechaInicio/fechaFin de la actividad a partir de las citas (si existen)
        val (minInicio, maxFin) = if (citas.isNotEmpty()) {
            val minI = citas.minOf { it.fechaInicioMillis }
            val maxF = citas.maxOf { it.fechaFinMillis }
            minI to maxF
        } else {
            // si no hay citas, conserva lo que venga en actividad (por compatibilidad)
            actividad.fechaInicio to actividad.fechaFin
        }

        val ref = db.collection("actividades").document()
        val newAct = actividad.copy(
            id = ref.id,
            fechaInicio = minInicio,
            fechaFin = maxFin
        )
        ref.set(newAct).await()

        val citasRef = db.collection("citas")
        for (cita in citas) {
            citasRef.add(cita.copy(actividadId = ref.id)).await()
        }
    }

    suspend fun modificarActividad(actividad: Actividad) {
        db.collection("actividades").document(actividad.id).set(actividad).await()
    }

    suspend fun cancelarActividad(id: String, motivo: String? = null): Boolean {
        return try {
            db.collection("actividades").document(id)
                .update(mapOf("estado" to "cancelada", "motivoCancelacion" to motivo)).await()
            true
        } catch (e: Exception) {
            Log.e("CANCELAR_ACTIVIDAD", "Error: ${e.message}")
            false
        }
    }

    suspend fun deleteActividad(id: String) {
        db.collection("actividades").document(id).delete().await()
    }

    suspend fun listAllActividades(): List<Actividad> {
        val snap = db.collection("actividades").get().await()
        return snap.documents.mapNotNull { it.toObject(Actividad::class.java)?.apply { id = it.id } }
    }

    suspend fun listUserActividades(uid: String): List<Actividad> {
        val snap = db.collection("actividades")
            .whereEqualTo("creadorUid", uid)
            .get().await()
        return snap.documents.mapNotNull {
            it.toObject(Actividad::class.java)?.apply { id = it.id }
        }
    }

    suspend fun citasEnRango(inicio: Long, fin: Long): List<Cita> {
        // ✅ Rango por UN solo campo (fechaInicioMillis) + orderBy para rendimiento
        val snap = db.collection("citas")
            .whereGreaterThanOrEqualTo("fechaInicioMillis", inicio)
            .whereLessThan("fechaInicioMillis", fin)   // < fin (exclusivo) para evitar duplicar bordes
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Cita::class.java)?.apply { id = it.id } }
            .sortedBy { it.fechaInicioMillis }
    }

    suspend fun reagendarCita(citaId: String, nuevoInicio: Long, nuevoFin: Long, nuevoLugar: String) {
        db.collection("citas").document(citaId)
            .update(
                mapOf(
                    "fechaInicioMillis" to nuevoInicio,
                    "fechaFinMillis" to nuevoFin,
                    "lugar" to nuevoLugar
                )
            ).await()
    }

    suspend fun updateCitaTiempoYLugar(
        citaId: String,
        nuevaFechaInicio: Long,
        nuevaFechaFin: Long,
        nuevoLugar: String
    ) {
        reagendarCita(citaId, nuevaFechaInicio, nuevaFechaFin, nuevoLugar)
    }

    // -----------------------------------------------------------
    // 🔹 CRUD MANTENEDORES
    // -----------------------------------------------------------

    suspend fun crearLugar(lugar: Lugar) {
        val doc = db.collection("lugares").document()
        doc.set(lugar.copy(id = doc.id)).await()
    }

    suspend fun obtenerLugares(): List<Lugar> {
        val snap = db.collection("lugares").get().await()
        return snap.documents.mapNotNull { it.toObject(Lugar::class.java)?.apply { id = it.id } }
    }

    suspend fun actualizarLugar(lugar: Lugar) {
        db.collection("lugares").document(lugar.id).set(lugar).await()
    }

    suspend fun eliminarLugar(id: String) {
        db.collection("lugares").document(id).delete().await()
    }

    suspend fun crearTipoActividad(tipo: TipoActividad) {
        val doc = db.collection("tiposActividad").document()
        doc.set(tipo.copy(id = doc.id)).await()
    }

    suspend fun obtenerTiposActividad(): List<TipoActividad> {
        val snap = db.collection("tiposActividad").get().await()
        return snap.documents.mapNotNull { it.toObject(TipoActividad::class.java)?.apply { id = it.id } }
    }

    suspend fun actualizarTipoActividad(tipo: TipoActividad) {
        db.collection("tiposActividad").document(tipo.id).set(tipo).await()
    }

    suspend fun eliminarTipoActividad(id: String) {
        db.collection("tiposActividad").document(id).delete().await()
    }

    suspend fun crearOferente(oferente: Oferente) {
        val doc = db.collection("oferentes").document()
        doc.set(oferente.copy(id = doc.id)).await()
    }

    suspend fun obtenerOferentes(): List<Oferente> {
        val snap = db.collection("oferentes").get().await()
        return snap.documents.mapNotNull { it.toObject(Oferente::class.java)?.apply { id = it.id } }
    }

    suspend fun actualizarOferente(oferente: Oferente) {
        db.collection("oferentes").document(oferente.id).set(oferente).await()
    }

    suspend fun eliminarOferente(id: String) {
        db.collection("oferentes").document(id).delete().await()
    }

    suspend fun crearProyecto(proyecto: Proyecto) {
        val doc = db.collection("proyectos").document()
        doc.set(proyecto.copy(id = doc.id)).await()
    }

    suspend fun obtenerProyectos(): List<Proyecto> {
        val snap = db.collection("proyectos").get().await()
        return snap.documents.mapNotNull { it.toObject(Proyecto::class.java)?.apply { id = it.id } }
    }

    suspend fun actualizarProyecto(proyecto: Proyecto) {
        db.collection("proyectos").document(proyecto.id).set(proyecto).await()
    }

    suspend fun eliminarProyecto(id: String) {
        db.collection("proyectos").document(id).delete().await()
    }

    // -----------------------------------------------------------
    // 🔹 SOCIOS COMUNITARIOS
    // -----------------------------------------------------------

    suspend fun crearSocioComunitario(socio: SocioComunitario) {
        val doc = db.collection("sociosComunitarios").document()
        doc.set(socio.copy(id = doc.id)).await()
    }

    suspend fun obtenerSociosComunitarios(): List<SocioComunitario> {
        val snap = db.collection("sociosComunitarios").get().await()
        return snap.documents.mapNotNull { it.toObject(SocioComunitario::class.java)?.apply { id = it.id } }
    }

    suspend fun actualizarSocioComunitario(socio: SocioComunitario) {
        db.collection("sociosComunitarios").document(socio.id).set(socio).await()
    }

    suspend fun eliminarSocioComunitario(id: String) {
        db.collection("sociosComunitarios").document(id).delete().await()
    }

    // -----------------------------------------------------------
    // 🔹 ACTIVIDADES EN RANGO
    // -----------------------------------------------------------

    suspend fun actividadesEnRango(inicio: Long, fin: Long): List<Actividad> {
        // Si usas este método, asegúrate de que la actividad tenga fechaInicio/fechaFin seteados.
        val snap = db.collection("actividades")
            .whereGreaterThanOrEqualTo("fechaInicio", inicio)
            .whereLessThanOrEqualTo("fechaFin", fin)
            .get().await()
        return snap.documents.mapNotNull {
            it.toObject(Actividad::class.java)?.apply { id = it.id }
        }
    }

    // -----------------------------------------------------------
    // 🔹 OPCIONES PARA FORMULARIO DE ACTIVIDAD (ADMIN)
    // -----------------------------------------------------------

    suspend fun obtenerOpcionesParaFormularioActividad():
            Quadruple<List<Lugar>, List<TipoActividad>, List<Oferente>, List<SocioComunitario>> {
        val lugares = obtenerLugares()
        val tipos = obtenerTiposActividad()
        val oferentes = obtenerOferentes()
        val socios = obtenerSociosComunitarios()
        return Quadruple(lugares, tipos, oferentes, socios)
    }

    // -----------------------------------------------------------
    // 🔹 NOTIFICACIONES FCM
    // -----------------------------------------------------------

    fun saveDeviceToken() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            db.collection("usuarios").document(uid).update("fcmToken", token)
        }
    }

    suspend fun sendNotificationToUser(uid: String, title: String, body: String) {
        try {
            val userDoc = db.collection("usuarios").document(uid).get().await()
            val token = userDoc.getString("fcmToken")
            if (!token.isNullOrEmpty()) {
                FcmSender.sendPushNotification(token, title, body)
            }
        } catch (e: Exception) {
            Log.e("FCM_SEND", "Error: ${e.message}")
        }
    }

    suspend fun obtenerActividades(): List<Actividad> {
        val snapshot = db.collection("actividades").get().await()
        return snapshot.documents.mapNotNull {
            it.toObject(Actividad::class.java)?.apply { id = it.id }
        }
    }

    suspend fun obtenerActividadPorId(id: String): Actividad? {
        val doc = db.collection("actividades").document(id).get().await()
        return doc.toObject(Actividad::class.java)?.apply { this.id = doc.id }
    }

    suspend fun obtenerCitasPorActividad(actividadId: String): List<Cita> {
        val snap = db.collection("citas")
            .whereEqualTo("actividadId", actividadId)
            .get().await()
        return snap.documents.mapNotNull {
            it.toObject(Cita::class.java)?.apply { id = it.id }
        }
    }
}

/** 🔸 Auxiliar para devolver 4 listas */
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
