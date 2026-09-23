# TaskFlow

Aplicación Android de tareas con Kotlin, Jetpack Compose, Room, Hilt y Firebase.

## Funciones

- Registro, inicio y cierre de sesión con Firebase Auth.
- Crear, editar, completar y eliminar tareas; prioridades, categorías y búsqueda.
- Persistencia local con Room y tema claro/oscuro según el sistema.
- Sincronización manual de tareas con Firestore mediante el botón **Sincronizar**.

## Sincronización de tareas

La pantalla siempre lee de Room. `TaskSynchronizer` sube los cambios pendientes
y descarga los documentos de `users/{uid}/tasks/{taskId}` a la base local.

- Cada subida usa una transacción de Firestore: gana el mayor `updatedAt`.
  Si las fechas son iguales, gana la versión ya guardada en el servidor.
- La descarga se aplica en una transacción de Room. Una edición local con fecha
  posterior se conserva y queda pendiente para la siguiente sincronización.
- Los borrados se representan con `pendingDelete`. Se conservan las marcas en
  Room y Firestore; la lista las oculta. La ausencia de un documento remoto no
  se interpreta como un borrado.
- Las categorías todavía son locales: se conserva la categoría de una tarea
  existente y las tareas nuevas descargadas no tienen categoría asignada.
- La descarga exige respuesta del servidor. Sin conexión, los cambios siguen
  disponibles en Room; hay que volver a pulsar **Sincronizar** al reconectarse.
  Los errores se registran con la etiqueta `TaskSync` en Logcat.
- Los ciclos de sincronización se ejecutan de uno en uno. Un fallo puede dejar
  un ciclo parcialmente completado; repetirlo permite continuar.

La resolución por fecha depende del reloj de cada dispositivo. Todavía no hay
sincronización automática, purga de marcas de borrado ni sincronización de categorías.

## Desarrollo y comprobaciones

Abrir el proyecto en Android Studio con JDK 21 y el SDK indicado en
`app/build.gradle.kts`. Configurar `local.properties` con la ruta del SDK y usar
la configuración de Firebase correspondiente a la aplicación.

Firebase debe tener Email/Password habilitado y Firestore configurado para que
cada usuario solo pueda acceder a `users/{su uid}/tasks`. Las reglas desplegadas
no están versionadas en este repositorio.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Las pruebas unitarias de sincronización usan almacenes en memoria: cubren subida,
descarga, conflictos, borrados, reintentos y una edición durante la subida.
No verifican la conexión real con Firebase ni sustituyen una prueba entre dispositivos.

Para comprobar la integración, usar dos dispositivos con la misma cuenta:
crear y sincronizar en uno, descargar en el otro, editar sin conexión, reconectar
y repetir. Comprobar también los borrados y los conflictos, y verificar que otra
cuenta no puede leer los documentos de la primera.

## Pendientes del proyecto

- Categorías por usuario e identificadores UUID para sincronizarlas.
- Cambio rápido de cuenta: renovar la suscripción de tareas al cambiar el uid.
- Resolver las tareas antiguas migradas con `userId = "USER"`.
- Estado y errores de sincronización visibles, automatización y reintentos.
- Selector manual de tema y pruebas de migraciones e integración.
