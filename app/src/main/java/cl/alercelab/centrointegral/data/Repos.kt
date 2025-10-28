package cl.alercelab.centrointegral.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import cl.alercelab.centrointegral.domain.*
import cl.alercelab.centrointegral.utils.FcmSender

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
        val snap = db.collection("usuarios")
            .whereEqualTo("aprobado", false)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun approveUser(uid: String) {
        db.collection("usuarios").document(uid)
            .update(mapOf("aprobado" to true, "estado" to "activo"))
            .await()
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
        val updates = mapOf("nombre" to nuevoNombre, "email" to nuevoEmail)
        db.collection("usuarios").document(uid).update(updates).await()
    }

    // -----------------------------------------------------------
    // 🔹 ACTIVIDADES
    // -----------------------------------------------------------

    suspend fun crearActividadConCitas(actividad: Actividad, citas: List<Cita>) {
        val ref = db.collection("actividades").document()
        val newAct = actividad.copy(id = ref.id)
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
                .update(mapOf("estado" to "cancelada", "motivoCancelacion" to motivo))
                .await()
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
        return snap.documents.mapNotNull { it.toObject(Actividad::class.java) }
    }

    suspend fun listUserActividades(uid: String): List<Actividad> {
        return try {
            val snap = db.collection("actividades")
                .whereArrayContains("beneficiarios", uid)
                .get().await()
            snap.documents.mapNotNull { it.toObject(Actividad::class.java) }
        } catch (e: Exception) {
            Log.e("LIST_USER_ACT", "Error: ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------
    // 🔹 ACTIVIDADES EN RANGO (para calendario semanal)
    // -----------------------------------------------------------

    suspend fun actividadesEnRango(
        desde: Long,
        hasta: Long,
        tipo: String? = null,
        query: String? = null,
        onlyMine: Boolean = false,
        rol: String? = null,
        uid: String? = null
    ): List<Actividad> {
        return try {
            var ref = db.collection("actividades")
                .whereGreaterThanOrEqualTo("fechaInicio", desde)
                .whereLessThanOrEqualTo("fechaFin", hasta)

            if (!tipo.isNullOrBlank()) ref = ref.whereEqualTo("tipo", tipo)
            if (!query.isNullOrBlank()) ref = ref.whereEqualTo("nombre", query)
            if (onlyMine && !uid.isNullOrBlank()) ref = ref.whereEqualTo("responsableUid", uid)

            val snapshot = ref.get().await()
            snapshot.documents.mapNotNull { it.toObject(Actividad::class.java) }
        } catch (e: Exception) {
            Log.e("ACTIVIDADES_RANGO", "Error: ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------
    // 🔹 CITAS EN RANGO (para el calendario)
    // -----------------------------------------------------------

    suspend fun citasEnRango(
        desde: Long,
        hasta: Long,
        tipo: String? = null,
        query: String? = null,
        onlyMine: Boolean = false,
        rol: String? = null,
        uid: String? = null
    ): List<Pair<Cita, Actividad?>> {
        return try {
            val citasSnap = db.collection("citas")
                .whereGreaterThanOrEqualTo("fechaInicioMillis", desde)
                .whereLessThanOrEqualTo("fechaFinMillis", hasta)
                .get().await()

            val citas = citasSnap.documents.mapNotNull { it.toObject(Cita::class.java) }
            if (citas.isEmpty()) return emptyList()

            val actividadesSnap = db.collection("actividades").get().await()
            val actividades = actividadesSnap.documents.mapNotNull { it.toObject(Actividad::class.java) }
                .associateBy { it.id }

            citas.mapNotNull { cita ->
                val act = actividades[cita.actividadId]
                if (act != null) cita to act else null
            }
        } catch (e: Exception) {
            Log.e("CITAS_RANGO", "Error: ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------
    // 🔹 CITAS
    // -----------------------------------------------------------

    suspend fun reagendarCita(
        citaId: String,
        nuevaFecha: Long,
        nuevoLugar: String,
        motivo: String? = null
    ): Boolean {
        return try {
            db.collection("citas").document(citaId)
                .update(
                    mapOf(
                        "fechaInicioMillis" to nuevaFecha,
                        "lugar" to nuevoLugar,
                        "motivo" to motivo
                    )
                ).await()
            true
        } catch (e: Exception) {
            Log.e("REAGENDAR_CITA", "Error: ${e.message}")
            false
        }
    }

    suspend fun updateCitaTiempoYLugar(
        citaId: String,
        nuevaFecha: Long,
        nuevoLugar: String
    ): Boolean {
        return try {
            db.collection("citas").document(citaId)
                .update(
                    mapOf(
                        "fechaInicioMillis" to nuevaFecha,
                        "lugar" to nuevoLugar
                    )
                ).await()
            true
        } catch (e: Exception) {
            Log.e("UPDATE_CITA", "Error al actualizar cita: ${e.message}")
            false
        }
    }

    // -----------------------------------------------------------
    // 🔹 NOTIFICACIONES (FCM)
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

    // -----------------------------------------------------------
    // 🔹 MANTENEDORES CRUD
    // -----------------------------------------------------------

    suspend fun crearLugar(lugar: Lugar) {
        val doc = db.collection("lugares").document()
        val nuevo = lugar.copy(id = doc.id)
        doc.set(nuevo).await()
    }

    suspend fun obtenerLugares(): List<Lugar> {
        val snap = db.collection("lugares").get().await()
        return snap.documents.mapNotNull { it.toObject(Lugar::class.java) }
    }

    suspend fun actualizarLugar(lugar: Lugar) {
        db.collection("lugares").document(lugar.id).set(lugar).await()
    }

    suspend fun eliminarLugar(id: String) {
        db.collection("lugares").document(id).delete().await()
    }

    suspend fun crearOferente(oferente: Oferente) {
        val doc = db.collection("oferentes").document()
        val nuevo = oferente.copy(id = doc.id)
        doc.set(nuevo).await()
    }

    suspend fun obtenerOferentes(): List<Oferente> {
        val snap = db.collection("oferentes").get().await()
        return snap.documents.mapNotNull { it.toObject(Oferente::class.java) }
    }

    suspend fun actualizarOferente(oferente: Oferente) {
        db.collection("oferentes").document(oferente.id).set(oferente).await()
    }

    suspend fun eliminarOferente(id: String) {
        db.collection("oferentes").document(id).delete().await()
    }

    suspend fun crearSocioComunitario(socio: SocioComunitario) {
        val doc = db.collection("sociosComunitarios").document()
        val nuevo = socio.copy(id = doc.id)
        doc.set(nuevo).await()
    }

    suspend fun obtenerSociosComunitarios(): List<SocioComunitario> {
        val snap = db.collection("sociosComunitarios").get().await()
        return snap.documents.mapNotNull { it.toObject(SocioComunitario::class.java) }
    }

    suspend fun actualizarSocioComunitario(socio: SocioComunitario) {
        db.collection("sociosComunitarios").document(socio.id).set(socio).await()
    }

    suspend fun eliminarSocioComunitario(id: String) {
        db.collection("sociosComunitarios").document(id).delete().await()
    }

    suspend fun crearProyecto(proyecto: Proyecto) {
        val doc = db.collection("proyectos").document()
        val nuevo = proyecto.copy(id = doc.id)
        doc.set(nuevo).await()
    }

    suspend fun obtenerProyectos(): List<Proyecto> {
        val snap = db.collection("proyectos").get().await()
        return snap.documents.mapNotNull { it.toObject(Proyecto::class.java) }
    }

    suspend fun actualizarProyecto(proyecto: Proyecto) {
        db.collection("proyectos").document(proyecto.id).set(proyecto).await()
    }

    suspend fun eliminarProyecto(id: String) {
        db.collection("proyectos").document(id).delete().await()
    }

    suspend fun crearTipoActividad(tipo: TipoActividad) {
        val doc = db.collection("tiposActividad").document()
        val nuevo = tipo.copy(nombre = tipo.nombre)
        doc.set(nuevo).await()
    }

    suspend fun obtenerTiposActividad(): List<TipoActividad> {
        val snap = db.collection("tiposActividad").get().await()
        return snap.documents.mapNotNull { it.toObject(TipoActividad::class.java) }
    }

    suspend fun actualizarTipoActividad(tipo: TipoActividad) {
        db.collection("tiposActividad").document(tipo.nombre).set(tipo).await()
    }

    suspend fun eliminarTipoActividad(nombre: String) {
        db.collection("tiposActividad").document(nombre).delete().await()
    }
}
