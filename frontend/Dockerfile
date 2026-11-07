# Imagen del frontend LogiCode: compila React con Vite y lo sirve con Nginx,
# que además reenvía /api al backend (mismo origen, sin CORS).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Rama estable 1.30: la 1.29 ya no recibe parches (Trivy: 51 CVE altos o críticos en su Alpine 3.23; 0 en esta, T-98).
FROM nginx:1.30-alpine
# openssl: crea el certificado local del piloto la primera vez (nginx/https.sh).
RUN apk add --no-cache openssl
COPY nginx/logicode.conf /etc/nginx/logicode.conf
COPY nginx/http.conf /etc/nginx/conf.d/default.conf
COPY --chmod=755 nginx/https.sh /docker-entrypoint.d/40-logicode-https.sh
COPY --from=build /app/dist /usr/share/nginx/html
# nginx 1.30 es la rama estable; la 1.29 ya no recibe parches (Trivy: 51 CVE altos o críticos en su Alpine 3.23, T-98).
# Sin root (guía 10, Trivy DS-0002, T-98): el usuario nginx (uid 101) de la imagen escucha en 8080 y 8443, que no
# necesitan privilegios; Compose publica 3000 (o 80 y 443 en el piloto) hacia ellos. https.sh reescribe conf.d.
RUN sed -i -e '/^user /d' -e 's#^pid .*#pid /tmp/nginx.pid;#' /etc/nginx/nginx.conf \
  && mkdir -p /etc/nginx/certificados \
  && chown -R nginx:nginx /var/cache/nginx /etc/nginx/conf.d /etc/nginx/certificados
USER nginx
EXPOSE 8080 8443
HEALTHCHECK --interval=15s --timeout=5s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/nginx-salud || exit 1
