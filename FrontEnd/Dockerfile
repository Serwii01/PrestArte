# ===== Compilación =====
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# ===== Servidor web =====
# Nginx sirve la aplicación y reenvía /api al backend (mismo dominio: sin CORS).
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/prestarte/browser /usr/share/nginx/html
EXPOSE 80
