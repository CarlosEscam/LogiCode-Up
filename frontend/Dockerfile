# Imagen del frontend LogiCode: compila React con Vite y lo sirve con Nginx,
# que además reenvía /api al backend (mismo origen, sin CORS).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.29-alpine
# openssl: crea el certificado local del piloto la primera vez (nginx/https.sh).
RUN apk add --no-cache openssl
COPY nginx/logicode.conf /etc/nginx/logicode.conf
COPY nginx/http.conf /etc/nginx/conf.d/default.conf
COPY --chmod=755 nginx/https.sh /docker-entrypoint.d/40-logicode-https.sh
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80 443
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1/nginx-salud || exit 1
