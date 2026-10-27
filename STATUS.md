# Estado del proyecto

> Lo mantiene Claude con `/cerrar`. Es la memoria de trabajo: dónde se quedó y qué viene después.

## Estado actual
Flujo de préstamo completo en `master`. En la rama `mejoras-publicacion` se preparó la publicación (seguridad, privacidad, avisos, recordatorios, procedencia, valoraciones…) y un dosier comercial, pero **el backend de esa rama no se ha compilado ni probado** (se hizo sin JDK). El frontend sí compila. Rama sincronizada con GitHub.

## Siguientes pasos
- [ ] Compilar y arrancar el backend de `mejoras-publicacion` y recorrer un préstamo completo; luego integrar en `master`
- [ ] Revocar las contraseñas de aplicación de Gmail que quedaron en el historial de git
- [ ] Rellenar los datos del titular en `FrontEnd/src/app/core/config/legal.ts` y revisar textos legales
- [ ] Migraciones versionadas con Flyway (`ddl-auto=validate`) y tests automáticos
- [ ] Copias de seguridad de los volúmenes `db-data` y `files-data`
- [ ] Calibrar tarifas de referencia de `CostEstimateService` con presupuestos reales

## Última sesión

### 2026-10-27
- Subido el commit «actualizacion» y fusionado el dosier comercial hecho en otro equipo (2026-10-23).

### 2026-10-21 (reconstruido desde git)
- Procedencia, avisos, incentivos, búsqueda, ampliaciones y valoraciones.
- Antes (2026-10-20): seguridad, privacidad y flujo de préstamo completo para publicar; CHANGELOG.

## Decisiones
- Despliegue con Docker Compose + Caddy delante para HTTPS (ver `DESPLIEGUE.md`).
- La baja de usuario anonimiza y marca `deletedAt` en vez de añadir un estado nuevo al enum.

## Problemas conocidos
- Backend de `mejoras-publicacion` sin compilar ni probar.
- Credenciales de Gmail expuestas en el historial de git.
