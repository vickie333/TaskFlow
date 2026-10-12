# TaskFlow

Aplicación Android de tareas con Kotlin, Jetpack Compose, Room, Hilt y Firebase.

## Funciones

- Registro, inicio y cierre de sesión con Firebase Auth.
- Crear, editar, completar y eliminar tareas; prioridades, categorías y búsqueda.
- Datos locales con Room, separados por cuenta; la interfaz siempre lee de Room.
- Sincronización de tareas y categorías con Firestore.
- Ajustes con tema **Sistema / Claro / Oscuro** persistido y recuperación explícita
  de tareas antiguas sin propietario conocido.

## Datos y cuentas

La sesión observable renueva las consultas y la navegación al cambiar de cuenta.
Los repositorios verifican el propietario de cada escritura y de la categoría
asignada. Las tareas de otra cuenta permanecen guardadas y no aparecen en la sesión
actual.

Room v6 migra las categorías a UUIDs y agrega propietario y estado de sincronización.
Si una categoría antigua era compartida por tareas de distintas cuentas, se crea
una categoría independiente por propietario. Se conservan las categorías sin dueño
con `userId = "USER"` y las relaciones de las tareas.

Desde **Ajustes → Recuperar tareas antiguas**, y tras confirmar, las tareas con
propietario `USER` o vacío se reasignan a la cuenta activa con nuevos UUIDs y las
categorías necesarias. La recuperación es transaccional; repetirla no duplica las
tareas. No se asignan datos antiguos automáticamente.

## Sincronización

Rutas remotas: `users/{uid}/categories/{categoryId}` y
`users/{uid}/tasks/{taskId}`. Los ids son UUIDs para nuevas entidades.

- Se sincronizan categorías antes de tareas para conservar las referencias.
- Cada subida usa una transacción de Firestore: gana el mayor `updatedAt`.
  En un empate gana la versión ya guardada en el servidor.
- Las aplicaciones remotas son transacciones de Room. Una edición local posterior
  realizada mientras se sincroniza se conserva y queda pendiente.
- Los borrados usan `pendingDelete`: las marcas permanecen en Room y Firestore.
  La ausencia de un documento remoto nunca se interpreta como un borrado.
- No se puede borrar localmente una categoría con tareas activas. Si su borrado
  llega de otro dispositivo, sus tareas se muestran como **Sin categoría**.
- WorkManager programa trabajo al iniciar sesión y después de escrituras, con
  requisito de conectividad, comprobación periódica y reintentos exponenciales
  para errores temporales. Al salir se cancela el trabajo de esa cuenta.
- La interfaz muestra el estado y los errores y permite reintentar. Un error de
  permisos requiere corregir la configuración; no se reintenta indefinidamente.

La resolución por fecha depende del reloj de cada dispositivo. No hay purga
automática de las marcas de borrado.

## Desarrollo y comprobaciones

Abrir el proyecto en Android Studio con JDK 21 y el SDK indicado en
`app/build.gradle.kts`. Configurar `local.properties` con la ruta del SDK y usar
la configuración de Firebase correspondiente a la aplicación.

Firebase debe tener Email/Password habilitado. Las reglas propuestas están en
[`firestore.rules`](firestore.rules): restringen lectura y escritura al uid de la
ruta, permiten categorías de la misma cuenta y mantienen los borrados lógicos.
**Versionar este archivo no despliega las reglas.** Antes de publicar, comprobarlas
con Firebase Emulator Suite y desplegarlas en el proyecto correspondiente.
La sintaxis de las condiciones sigue la
[documentación de Firebase](https://firebase.google.com/docs/firestore/security/rules-conditions).

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Las pruebas incluyen Room real con Robolectric para migración, aislamiento y
recuperación; almacenes remotos simulados para conflictos, reintentos, borrados y
escrituras concurrentes; y comprobaciones de trabajo de fondo y preferencias.
Los esquemas de Room se guardan en `app/schemas/`.

Las pruebas locales no sustituyen una integración real con Firebase. Para
comprobarla, usar dos dispositivos con la misma cuenta: crear categorías y tareas,
sincronizar, editar sin conexión, reconectar y comprobar conflictos y borrados.
Verificar también que otra cuenta no puede acceder a los documentos de la primera.
