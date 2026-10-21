# Despliegue

PrestArte se despliega con Docker: una base de datos MySQL, el backend (Spring Boot) y el frontend (Angular servido por Nginx, que además reenvía `/api` al backend).

## Requisitos

- Docker y Docker Compose.
- Una cuenta de correo SMTP para los avisos (por ejemplo, Gmail con contraseña de aplicación).

## Arrancar

```bash
cp .env.example .env
# Edita .env: contraseñas de la base de datos, correo, JWT_SECRET, administrador y PUBLIC_URL
docker compose up -d --build
```

La aplicación queda en `http://localhost` (o en el puerto `APP_PORT`). El primer arranque crea el administrador definido en `.env`.

Para generar el secreto JWT:

```bash
openssl rand -base64 64
```

## Publicar en un servidor

1. Un VPS con Docker (2 GB de RAM es suficiente para empezar).
2. Un dominio apuntando a la IP del servidor.
3. HTTPS delante de Nginx. Lo más sencillo es **Caddy**, que obtiene y renueva el certificado solo. Pon `APP_PORT=8080` en `.env` y un `Caddyfile` como este:

   ```
   tu-dominio.es {
       reverse_proxy localhost:8080
   }
   ```

4. En `.env`, `PUBLIC_URL=https://tu-dominio.es`.

## Antes de abrirlo al público

- [ ] Rellenar los datos del titular en `FrontEnd/src/app/core/config/legal.ts` y revisar los textos legales con un profesional.
- [ ] Revocar las contraseñas de aplicación de Gmail que quedaron en el historial de git.
- [ ] Copias de seguridad automáticas de los volúmenes `db-data` (por ejemplo, `mysqldump` diario) y `files-data` (fotos y documentos), fuera del servidor.
- [ ] Pasar el esquema de base de datos a migraciones versionadas (Flyway) y poner `ddl-auto=validate`.
- [ ] Calibrar las tarifas de referencia de `CostEstimateService` con presupuestos reales.

## Notas

- **Ficheros:** el contenido de fotos y documentos se guarda en el volumen `files-data` (`APP_STORAGE_DIR`); la base de datos solo guarda sus metadatos. Si vienes de una versión anterior, arranca una vez con `APP_STORAGE_MIGRATE_LEGACY=true` para sacar de MySQL los archivos antiguos.
- **Chat en tiempo real y límite de intentos del login:** se guardan en memoria, así que valen para una sola instancia del backend.
- **Geocodificación:** las ciudades poco habituales se buscan en OpenStreetMap (Nominatim, máx. 1 petición/s). Con mucho tráfico, usa una instancia propia (`APP_GEOCODING_URL`) o desactívalo (`APP_GEOCODING_ENABLED=false`).
- **Recordatorios:** se ejecutan cada día a las 8:00 (hora de Madrid). Se puede cambiar con la variable `APP_REMINDERS_CRON`.
