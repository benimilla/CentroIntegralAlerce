# Centro Integral Alerce 
# Descripción general

Aplicación Android en Kotlin que gestiona las actividades, usuarios y recursos del Centro Integral Alerce.
Está pensada para facilitar el trabajo tanto de administradores como de gestores y usuarios comunes, con un flujo claro, roles definidos y una estructura modular simple.

# El sistema está dividido por áreas:
  -auth/ → login, registro y recuperación de contraseña.

  -admin/ → panel de administración, gestión y aprobación de usuarios, mantenedores.

  -adapters/ → adaptadores para RecyclerView.

  -domain/ → modelos de datos.

  -data/ → acceso a Firestore (repositorio central Repos).

  -ui/ → calendario, actividades y configuración.

  -Usa Firebase para autenticación, base de datos y notificaciones.

 # Roles y accesos

El control de acceso está totalmente implementado.

  -Administrador: accede al panel admin, mantenedores, usuarios, actividades y configuración.

  -Gestor: accede a actividades, calendario y configuración.

  -Usuario común: solo calendario y configuración.

La navegación se adapta según el rol del usuario al iniciar sesión, evitando accesos indebidos.

# Funcionalidades principales

  -Login y registro con validación y aprobación de nuevos usuarios.

  -Panel administrativo con gestión de usuarios, cambio de roles, edición y eliminación.

  -Sección de pendientes para aprobar o rechazar registros.

  -Mantenedores completos para lugares, oferentes, socios, proyectos y tipos de actividad.

  -Calendario con vista semanal y actividades asignadas.

  -Notificaciones configuradas con canal local (listo para FCM).

# Capa de datos

  -Repos.kt centraliza toda la comunicación con Firestore:

  -Maneja usuarios, roles y aprobación.

  -Implementa CRUD de todas las entidades.

  -Usa coroutines para un flujo asíncrono limpio y seguro.
