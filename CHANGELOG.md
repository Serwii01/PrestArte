# Cambios

## Rama `mejoras-publicacion` — preparación para publicar

> ⚠️ **El backend de esta rama no se ha compilado ni probado** (se desarrolló en un equipo sin JDK).
> Antes de integrarla en `master`: compilar, arrancar y recorrer un préstamo completo.
> El frontend sí compila (`ng build`).

### 1. Seguridad

| Cambio | Detalle |
|---|---|
| Perfil de producción | `application-prod.properties` (se activa con `SPRING_PROFILES_ACTIVE=prod`). BD, correo, `JWT_SECRET`, admin, CORS y URL del frontend **sin valores por defecto**: si falta una variable, la app no arranca. Desactiva los datos de demo y oculta las trazas de error. |
| Descarga de archivos con permisos | `GET /api/files/{id}` comprueba a qué pertenece cada archivo. Públicos: fotos de obras y documentos no confidenciales. Resto (verificación de cuentas, adjuntos del chat, documentos confidenciales, informes de estado, seguros, permisos, supervisión, informes de instalaciones): solo partes implicadas y admin. En el frontend, pipe `secureFile` que descarga con el token. |
| Validación de subidas | `UploadValidator` detecta el tipo real por los primeros bytes (JPEG, PNG, WEBP, PDF). Solo el dueño puede subir fotos a su obra. Cabecera `Content-Security-Policy: sandbox` en las descargas. |
| Límite de intentos | `RateLimitFilter`: login 10/5 min, registro 5/h, recuperar contraseña 5/15 min, restablecer 10/15 min (por IP, respuesta 429). |
| Endpoints peligrosos eliminados | `GET/POST /api/collectors` y `/api/foundations` (exponían email, teléfono y DNI y permitían crear cuentas sin verificar), `POST /api/transport-companies`. `DELETE` de transportistas solo admin. Paneles (`/dashboard`) solo para el propio usuario. Crear obras solo a nombre propio. |

### 2. Privacidad del coleccionista

- Obras **anónimas por defecto** ("Colección particular"); la identidad se revela a la fundación cuando el coleccionista **acepta** el préstamo (también en el chat y en las exposiciones).
- **Valor**: exacto solo para dueño y admin; horquilla para usuarios verificados; nada para visitantes.
- **Ubicación**: solo ciudad o provincia en público; **dirección de recogida privada**, visible para el transportista solo con un envío aprobado y en curso.
- **Visibilidad**: catálogo público o solo instituciones verificadas.

### 3. Lógica del préstamo

- **Corregido**: la comprobación de solapes solo miraba el estado `ACCEPTED` (transitorio), así que una obra podía reservarse dos veces.
- **Validación** de solicitudes (fechas obligatorias, inicio ≥ hoy, fin > inicio, sin duplicados).
- **Rechazo automático** de solicitudes solapadas al aceptar una, con email a esas fundaciones.
- **Motivo de rechazo** visible para la fundación.
- **Historial del préstamo** (`loan_events`): cada cambio de estado e hito, con autor y fecha.
- **Informes de estado** con fotos obligatorias en cada recogida y entrega, y marca de daños.

### 4. Exposiciones, instalaciones y compatibilidad

- **Exposiciones**: las solicitudes se agrupan por exposición (título, proyecto, sede, ciudad, país, fechas). Ficha con el estado de todas las obras.
- **Informe de instalaciones** de la fundación (clima, luz, seguridad, accesos, PDF oficial), caduca a los 12 meses.
- **Requisitos de préstamo** estructurados en cada obra (temperatura, humedad, lux, vitrina, seguridad, antelación mínima, tipo de correo).
- **Compatibilidad automática** entre requisitos e instalaciones, visible para la fundación antes de pedir y para el coleccionista antes de aceptar. No bloquea (decide el coleccionista), salvo la antelación mínima.

### 5. Seguro y permiso de exportación

- **Valor asegurado acordado** al aceptar.
- **Seguro clavo a clavo**: certificado en PDF que debe cubrir el valor y todo el periodo; el coleccionista lo aprueba o rechaza. También puede asegurar con su propia póliza.
- **Permiso de exportación temporal** (Ley 16/1985): se exige si la obra sale de España y tiene ≥ 100 años, es BIC o está inventariada; la fecha límite de retorno debe ser posterior al fin del préstamo.
- **Sin esta documentación la obra no puede salir** (ni "lista para recoger" ni recogida).

### 6. Costes, huella de carbono y correo virtual

- **Coste y CO₂ estimados** antes de pedir la obra: transporte ida y vuelta, caja, seguro y correo, con la tarifa del transportista (nueva en su perfil) o una de referencia. Distancias con una tabla de ~120 ciudades (`CityLocator`).
  ⚠️ Las cifras de referencia de `CostEstimateService` son supuestos a calibrar con presupuestos reales.
- **Presupuesto de ida y vuelta**: el transportista cotiza también la devolución.
- **Correo virtual**: fases documentadas con fotos (embalaje, carga, montaje…) que el coleccionista aprueba; si la obra lo exige, no se recoge sin el embalaje aprobado.

### 7. Titularidad y procedencia

- **Declaración de titularidad** obligatoria al publicar una obra (o firmable después desde su ficha). Sin ella la obra no recibe solicitudes.
- **Historial de procedencia**: propietarios anteriores con años, forma de adquisición, detalles y justificante opcional (visible para el dueño, la administración y las fundaciones que han pedido la obra).
- **Detección de huecos** en el historial, con aviso especial si caen entre **1933 y 1945** (expolio nazi; Principios de Washington).
- **Diligencia debida**: cola en el panel de administración para comprobar cada obra en bases de obras robadas y registrar el resultado (se guarda cada revisión). Las verificadas muestran el distintivo «Procedencia verificada» en el catálogo; las marcadas como sospechosas salen del catálogo y no admiten solicitudes. Se avisa al coleccionista por email.
- Nuevos endpoints: `GET/POST /api/artworks/{id}/provenance`, `DELETE /api/artworks/{id}/provenance/{entryId}`, `POST /api/artworks/{id}/title-declaration`, `GET /api/admin/due-diligence/pending`, `POST /api/admin/artworks/{id}/due-diligence`.
- **Rompe compatibilidad**: `POST /api/artworks` exige `titleDeclared: true`. Las obras ya existentes no tienen la declaración firmada y no recibirán solicitudes hasta que su dueño la firme (la obra de demo ya viene firmada).

### 8. Avisos y plazos

- **Avisos guardados** (`notifications`): cada hito de un préstamo avisa a las demás partes en la campana de la aplicación, con estado leído/no leído. Los cambios de estado y lo que exige actuar (seguro pendiente o rechazado, cambios pedidos en la supervisión, incidencias, ampliaciones) también se envían por **email** con enlace al préstamo.
- **Recordatorios diarios** (`ReminderService`, 8:00 hora de Madrid, configurable con `app.reminders.cron`): solicitud sin responder tras 7 días, fin de préstamo en 14 días, préstamo vencido sin iniciar la devolución, fecha límite de retorno del permiso de exportación (30 días antes y al vencer), seguro que termina con la obra fuera, seguro pendiente de revisión más de 3 días e informe de instalaciones a punto de caducar. Cada recordatorio se envía una sola vez (`reminder_log`).
- Endpoints: `GET /api/notifications`, `GET /api/notifications/unread-count`, `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all`.

### 9. Incentivos al coleccionista

- **Mención en cartela**, **usos de imagen autorizados** (catálogo, prensa, redes, tienda, fotos del público) y **canon de préstamo** opcional en cada obra; se muestran a la fundación antes de pedirla, entran en la estimación de costes y en el contrato PDF.
- **Certificado de exposición** en PDF descargable por coleccionista y fundación cuando la obra llega a la exposición.
- **Historial público de exposiciones** en la ficha de la obra.
- El contrato PDF usa ahora el valor asegurado acordado e incluye exposición, periodo y condiciones del prestador.
- Endpoints: `GET /api/loan-requests/{id}/exhibition-certificate`, `GET /api/artworks/{id}/exhibitions` (público).

### 10. Catálogo, ampliaciones y valoraciones

- **Búsqueda con filtros y paginación** (`GET /api/artworks/search`): texto, ciudad, años, estado, procedencia verificada, disponibles para préstamo, **libres entre dos fechas** y orden. Filtra en memoria; si el catálogo crece mucho conviene pasarlo a consultas JPA.
- **Calendario de disponibilidad** (`GET /api/artworks/{id}/availability`, público y sin datos de las partes): en la ficha de la obra y como días tachados en el calendario de la solicitud, con aviso si el rango elegido choca.
- **Ampliación del préstamo**: la fundación la pide con la obra expuesta y el coleccionista la aprueba o rechaza. Al aprobar se comprueba que la obra no esté reservada después y que el permiso de exportación lo permita; si el seguro no cubre el nuevo periodo, se avisa. Endpoints: `POST /api/loan-requests/{id}/extension`, `/extension/approve`, `/extension/reject`.
- **Valoraciones** entre las partes de un préstamo devuelto (1–5 estrellas y comentario). Resumen público por usuario (media y últimos comentarios sin autor) en el perfil de los transportistas y, para el coleccionista, la valoración de la institución que le pide una obra. Endpoints: `GET/POST /api/loan-requests/{id}/reviews`, `GET /api/users/{id}/rating` (público).

### 11. RGPD y textos legales

- Páginas **/legal/privacidad**, **/legal/aviso-legal**, **/legal/cookies** y **/legal/terminos**, enlazadas desde todos los pies de página. Los datos del titular están en `FrontEnd/src/app/core/config/legal.ts` como `[marcadores]` que hay que rellenar; los textos deben revisarse con un profesional.
- **Aceptación** obligatoria de privacidad y términos en el registro; se guarda la fecha y la versión (`termsAcceptedAt`, `termsVersion`).
- **Mi cuenta** (`/app/account`): descarga de todos los datos en JSON (`GET /api/me/export`) y **baja** con contraseña (`POST /api/me/delete`). La baja anonimiza los datos personales, elimina el documento de verificación, retira sus obras del catálogo y conserva los préstamos cerrados por obligación legal. No se permite con préstamos en curso. Se marca con `deletedAt` (no con un estado nuevo, para no tocar la columna enum existente).

### 12. Despliegue

- `Dockerfile` del backend (compila con Maven y ejecuta con JRE 21, usuario sin privilegios) y del frontend (compila Angular y lo sirve con Nginx, que reenvía `/api` al backend).
- `docker-compose.yml` con MySQL, backend y frontend; `.env.example` con todas las variables. Ver `DESPLIEGUE.md`.
- **Corregido**: el build de producción de Angular fallaba por el presupuesto de tamaño de estilos de la landing (se ha ampliado).
- **Corregido**: el logo del contrato PDF se leía de una ruta del código fuente que no existe dentro del jar; ahora se lee del classpath.

### 13. Panel de métricas

- `GET /api/admin/metrics` y panel en la administración: préstamos activos y completados, tasa de aceptación, tiempo medio de respuesta del coleccionista, valor asegurado fuera de casa, obras y su verificación, exposiciones, incidencias, usuarios por rol, préstamos por estado y obras más solicitadas.

### 14. Otros

- `README.md` con la descripción del proyecto.

### 15. Pagos: estado «Presupuesto aprobado»

- PrestArte no cobra ni mueve dinero: el transporte y el seguro se pagan fuera de la plataforma. El estado interno `PAID` se muestra ahora como **«Presupuesto aprobado»** para no dar a entender que hay un pago dentro de la app.

### 16. Tipografías e iconos propios

- Manrope y Material Symbols se sirven desde la propia aplicación (`@fontsource-variable/manrope`, `material-symbols`), no desde Google Fonts: ningún tercero recibe la IP de los visitantes. Textos de privacidad y cookies actualizados.

### 17. Informes de estado completos

- **Informe antes de desmontar** (`PRE_RETURN`): la institución documenta la obra en sala con la obra aún expuesta (`POST /api/loan-requests/{id}/condition-reports`, multipart). Es **obligatorio para iniciar la devolución**; separa los daños de la exposición de los del viaje de vuelta.
- **Marcas de daño sobre la foto**: al levantar cualquier informe se pulsa sobre la foto para señalar dónde está cada daño (posición en %, con nota). Se ven numeradas sobre las fotos del informe.
- **Firma de conformidad de la otra parte**: cada informe lo firma quien lo levanta y después la otra parte del momento (recogida de ida → coleccionista; entrega → transportista; antes de desmontar / traslado → coleccionista; recogida de vuelta → institución). Puede firmar conforme o **no conforme** con un motivo, que avisa a todos por email y queda en el historial (`POST /api/loan-requests/{id}/condition-reports/{reportId}/sign`).

### 18. Exposiciones itinerantes

- Una exposición puede tener **sedes siguientes** (institución, sala, ciudad, país y fechas). Cada sede abre después de cerrar la anterior; no se puede alargar la gira más allá del final de los préstamos en curso.
- El préstamo debe cubrir **la gira completa**; el **permiso de exportación** se exige si cualquier sede está fuera de España; el **contrato** y el **certificado de exposición** incluyen las sedes.
- **Traslado entre sedes** (`POST /api/loan-requests/{id}/transfer`): la institución levanta un informe de estado en sala (`TRANSFER`) y la obra pasa a la siguiente sede sin volver al prestador. La ficha del préstamo muestra el itinerario y la sede actual. El informe final antes de desmontar solo se permite en la última sede.

### 19. Compatibilidad de la exposición entera

- `GET /api/exhibitions/{id}/compatibility` y panel en la ficha de la exposición: cada obra frente a las instalaciones y, además, **si las obras pueden compartir sala** (rangos de temperatura y humedad comunes, luz máxima de la obra más delicada, número de correos presenciales). Avisa de que las sedes de una gira necesitan su propio informe de instalaciones.

### 20. Chat en tiempo real

- El chat deja de consultar el servidor cada 4 segundos: usa **Server-Sent Events** (`GET /api/messages/chat/{id}/stream`). El stream solo avisa del id del mensaje nuevo y el cliente lo pide por REST, así cada participante lo ve con sus reglas de anonimato. Se reconecta solo y recupera lo que llegara mientras estaba desconectado.
- Los suscriptores se guardan en memoria (una sola instancia del backend).

### 21. Archivos fuera de MySQL

- El contenido de los archivos nuevos se guarda en disco (`app.storage.dir`, volumen `files-data` en Docker); la base de datos solo guarda metadatos y una clave. Es transparente para el resto del código (listener de JPA). Si una transacción falla, el fichero se borra; al borrar un archivo, también su contenido.
- Los archivos antiguos siguen sirviéndose desde la base de datos; con `APP_STORAGE_MIGRATE_LEGACY=true` se mueven a disco al arrancar.

### 22. Geolocalización real

- Las ciudades que no están en la tabla local se buscan en **OpenStreetMap (Nominatim)**, con caché y como mucho una petición por segundo, según su política de uso. Solo se envía el nombre de la ciudad. Se puede desactivar (`APP_GEOCODING_ENABLED=false`) o apuntar a una instancia propia (`APP_GEOCODING_URL`). Mencionado en la política de privacidad.

### 23. Bases de obras robadas

- En la diligencia debida, la administración ve las bases que consultar (INTERPOL ID-Art, Art Loss Register, Lost Art, FBI National Stolen Art File, Carabinieri TPC y las unidades de patrimonio de Policía Nacional y Guardia Civil) con enlace, cómo se accede, qué cubren y los datos de la obra listos para copiar. Las marcadas componen el campo «bases consultadas» (`GET /api/admin/artworks/{id}/stolen-art-registries`).
- **Integración automática preparada**: ninguna de estas bases ofrece una API abierta, así que de serie la consulta es manual. Basta implementar `StolenArtRegistryClient` (por ejemplo, con un acceso contratado a Art Loss Register) para que se consulte sola; si encuentra una posible coincidencia, no se puede verificar la obra sin explicar en las notas por qué se descarta.

### Cambios de API que rompen compatibilidad

- `POST /api/loan-requests` exige `exhibitionId`.
- `POST /api/shipments/{id}/picked-up` y `/delivered` son multipart (`data` + `photos`) y exigen fotos.
- `POST /api/shipments/{id}/quote` exige `returnPrice`.
- Eliminados `/api/collectors` (salvo el panel), `/api/foundations` (salvo el panel) y `POST /api/transport-companies`.
- `POST /api/loan-requests/{id}/start-return` exige antes el informe de estado previo al desmontaje (y, en una gira, estar en la última sede).
- El chat ya no hace polling: el frontend necesita el endpoint de stream (y que Nginx no lo almacene en búfer; el backend ya envía `X-Accel-Buffering: no`).

### Base de datos

Con `ddl-auto=update` se crean solas las tablas nuevas (`loan_events`, `condition_reports`, `exhibitions`, `exhibition_stops`, `facility_reports`, `insurance_certificates`, `export_permits`, `supervision_checkpoints`, `damage_marks` y sus tablas de fotos) y las columnas nuevas. La columna `condition_reports.stage`, que Hibernate creó como ENUM, la convierte a VARCHAR `SchemaFixes` al arrancar (si no, no se podrían guardar las fases nuevas). Las obras existentes pasan a ser anónimas. Los préstamos de demo en "pagado" o "listo para recoger" quedarán bloqueados hasta subir un seguro.

### Pendiente

- Compilar y probar el backend.
- Revocar las contraseñas de aplicación de Gmail que aparecen en el historial de git.
- Rellenar los datos del titular en `legal.ts` y revisar los textos legales con un profesional.
- Migraciones versionadas (Flyway) y tests automáticos.
- Copias de seguridad de los volúmenes `db-data` y `files-data`.
