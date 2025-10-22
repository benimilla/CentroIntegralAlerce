const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// 🔔 Notificar cuando se crea una nueva actividad
exports.notifyActivityCreated = functions.firestore
  .document("actividades/{actividadId}")
  .onCreate(async (snap, context) => {
    const actividad = snap.data();
    const actividadId = context.params.actividadId;

    const usersSnap = await admin.firestore().collection("usuarios").get();
    const tokens = [];

    usersSnap.forEach((doc) => {
      const user = doc.data();

      // Admins y gestores reciben todas las notificaciones
      if (user.rol === "admin" || user.rol === "gestor") {
        if (user.fcmToken) tokens.push(user.fcmToken);
      }
      // Usuarios normales solo si están involucrados en la actividad
      else if (
        actividad.beneficiarios?.includes(user.uid) ||
        actividad.oferente === user.uid ||
        actividad.socioComunitario === user.uid
      ) {
        if (user.fcmToken) tokens.push(user.fcmToken);
      }
    });

    if (tokens.length > 0) {
      const payload = {
        notification: {
          title: "Nueva actividad programada",
          body: `${actividad.nombre} (${actividad.tipo}) ha sido creada.`,
        },
        data: { actividadId },
      };

      await admin.messaging().sendEachForMulticast({ tokens, ...payload });
      console.log(`🔔 Notificación enviada a ${tokens.length} usuarios`);
    }
  });

// 🔄 Notificar cuando una actividad es actualizada
exports.notifyActivityUpdated = functions.firestore
  .document("actividades/{actividadId}")
  .onUpdate(async (change, context) => {
    const actividad = change.after.data();
    const actividadId = context.params.actividadId;

    const usersSnap = await admin.firestore().collection("usuarios").get();
    const tokens = [];

    usersSnap.forEach((doc) => {
      const user = doc.data();

      if (user.rol === "admin" || user.rol === "gestor") {
        if (user.fcmToken) tokens.push(user.fcmToken);
      } else if (
        actividad.beneficiarios?.includes(user.uid) ||
        actividad.oferente === user.uid ||
        actividad.socioComunitario === user.uid
      ) {
        if (user.fcmToken) tokens.push(user.fcmToken);
      }
    });

    if (tokens.length > 0) {
      const payload = {
        notification: {
          title: "Actividad actualizada",
          body: `${actividad.nombre} ha sido modificada.`,
        },
        data: { actividadId },
      };

      await admin.messaging().sendEachForMulticast({ tokens, ...payload });
      console.log(`🔔 Notificación enviada a ${tokens.length} usuarios`);
    }
  });
