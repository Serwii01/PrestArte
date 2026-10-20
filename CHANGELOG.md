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

### 7. Otros

- `README.md` con la descripción del proyecto.

### Cambios de API que rompen compatibilidad

- `POST /api/loan-requests` exige `exhibitionId`.
- `POST /api/shipments/{id}/picked-up` y `/delivered` son multipart (`data` + `photos`) y exigen fotos.
- `POST /api/shipments/{id}/quote` exige `returnPrice`.
- Eliminados `/api/collectors` (salvo el panel), `/api/foundations` (salvo el panel) y `POST /api/transport-companies`.

### Base de datos

Con `ddl-auto=update` se crean solas las tablas nuevas (`loan_events`, `condition_reports`, `exhibitions`, `facility_reports`, `insurance_certificates`, `export_permits`, `supervision_checkpoints` y sus tablas de fotos) y las columnas nuevas. Las obras existentes pasan a ser anónimas. Los préstamos de demo en "pagado" o "listo para recoger" quedarán bloqueados hasta subir un seguro.

### Pendiente

- Compilar y probar el backend.
- Revocar las contraseñas de aplicación de Gmail que aparecen en el historial de git.
- Flyway, Docker y despliegue; tests; textos legales (privacidad, cookies, aviso legal).
