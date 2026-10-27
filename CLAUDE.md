# PrestArte

Red profesional y verificada para prestar obras de arte entre coleccionistas privados y fundaciones/museos, con empresas de transporte especializadas. TFG. Detalle funcional completo en `README.md`.

## Stack
- **BackEnd/**: Java 21, Spring Boot 4, Spring Security (JWT con JJWT), Spring Data JPA, MySQL, OpenPDF (contratos), Spring Mail, Lombok. Maven (`mvnw`).
- **FrontEnd/**: Angular 21 (standalone, signals, rutas lazy) + Tailwind CSS 4, servido por Nginx en Docker.
- **Despliegue**: `docker-compose.yml` (MySQL + backend + frontend/Nginx que reenvía `/api`). Ver `DESPLIEGUE.md`.

## Estructura
- `BackEnd/src/main/java/com/prestarte/tfg/` — `controller/`, `service/` (con `statemachine/` para el ciclo de vida de préstamos y envíos), `model/` (entidades y DTOs), `repository/`, `security/` (JWT, usuario actual, rate limit), `config/`.
- `FrontEnd/src/app/` — `core/` (servicios, config, `legal.ts`), `features/` por rol/pantalla.
- `CHANGELOG.md` — detalle de los cambios de la rama `mejoras-publicacion`.
- `design/`, `Memoria_Prestarte.docx` — diseño y memoria del TFG.

## Comandos
```bash
cp .env.example .env && docker compose up -d --build   # todo junto en http://localhost
cd BackEnd && ./mvnw spring-boot:run                    # backend suelto (requiere JDK 21 y MySQL)
cd FrontEnd && npm start                                # frontend suelto
cd FrontEnd && npx ng build                             # comprobar que compila
```

## Convenciones
- Código, textos de interfaz y documentación en **español**.
- Roles: coleccionista, fundación/museo, transportista, admin. Toda cuenta la verifica el admin antes de operar.
- Perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`) sin valores por defecto: si falta una variable de entorno, no arranca.
- Los archivos se sirven por `GET /api/files/{id}` con control de permisos; en el front, pipe `secureFile`.

> El estado del trabajo (qué se hizo, qué falta) está en `STATUS.md`.
