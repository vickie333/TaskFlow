# Pendientes de TaskFlow

Objetivo: completar los pendientes funcionales del README conservando Room como
fuente de datos y las tareas existentes. Trabajo autorizado por la usuaria;
ejecución en la rama `feat/account-sync-settings`, basada en `feat/firebase-sync`.

## Diseño

- Una sesión observable por uid controla consultas, navegación y trabajo de fondo.
  Las escrituras verifican el propietario; salir cancela el trabajo de esa cuenta.
- Room v6: categorías con UUID, propietario, fecha y marcas de sincronización y
  borrado. La migración duplica las categorías compartidas por cada propietario
  que las usa y mantiene categorías sin dueño bajo `USER`.
- Recuperación explícita de datos antiguos: copiar las categorías necesarias y
  reasignar las tareas `USER`/sin propietario a la cuenta activa con nuevos UUID.
  Nunca asignar automáticamente datos de propietario desconocido ni descartarlos.
- Sincronizar categorías antes de tareas. Mantener tombstones; referencias a
  categorías eliminadas se muestran como sin categoría. Bloquear borrados locales
  de categorías con tareas activas. Rechazar referencias a categorías de otra cuenta.
- WorkManager programa trabajo al iniciar sesión y después de escrituras, espera
  conectividad y reintenta errores temporales con backoff exponencial. Añadir
  comprobación periódica. No reintentar indefinidamente reglas denegadas.
- La UI muestra estado/error de sincronización y permite reintentar; ajustes
  contienen tema Sistema/Claro/Oscuro persistido y recuperación de tareas antiguas.
- Pruebas Robolectric con Room real para migración, aislamiento y recuperación;
  pruebas de sincronización con almacén remoto simulado. No desplegar reglas ni
  afirmar integración con Firebase real sin haberla ejecutado.

## Secuencia de implementación y verificación

- [ ] Sesión observable y migración v6: actualizar `data/`, probar migración v5
  con tareas de dos cuentas y huérfanas y validar el esquema al abrir Room.
- [ ] Repositorios y categorías remotas: probar que la sesión cambia inmediatamente,
  que una cuenta no puede editar otra, que categorías preceden a tareas y que los
  reintentos conservan escrituras locales concurrentes.
- [ ] Recuperación: comprobar que se conservan relaciones y que repetir no duplica.
- [ ] Trabajo de fondo: probar decisiones de reintento y cuenta del worker;
  conservar cancelación de coroutines, límites de tiempo y trabajo único.
- [ ] Ajustes y estado visible: persistencia del tema, navegación por sesión y
  recuperación confirmada dentro de la app. Compilar y comprobar lint.
- [ ] Actualizar README y añadir reglas de Firestore versionadas para categorías.
- [ ] Ejecutar `./gradlew testDebugUnitTest assembleDebug lintDebug`; revisar diff,
  registrar limitaciones de validación visual/remota y guardar cambios en git.
