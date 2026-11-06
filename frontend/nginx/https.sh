#!/bin/sh
# Modo HTTPS del piloto (Etapa 10). Lo ejecuta la imagen oficial de Nginx al arrancar (/docker-entrypoint.d).
# Con LOGICODE_HTTPS=true usa certificados/logicode.crt y .key; si no existen, crea una autoridad local
# (logicode-ca.crt, la que se instala en los equipos para no ver la advertencia) y un certificado firmado por
# ella para localhost y los nombres o IP de LOGICODE_TLS_HOSTS. Sin la variable, deja el modo HTTP.
set -eu
[ "${LOGICODE_HTTPS:-false}" = "true" ] || exit 0

dir=/etc/nginx/certificados
if [ ! -s "$dir/logicode.crt" ] || [ ! -s "$dir/logicode.key" ]; then
  if [ ! -s "$dir/logicode-ca.crt" ] || [ ! -s "$dir/logicode-ca.key" ]; then
    echo "https.sh: creando la autoridad local de certificados en certificados/"
    openssl req -x509 -newkey rsa:2048 -nodes -days 3650 -subj "/CN=LogiCode CA local" \
      -addext "basicConstraints=critical,CA:TRUE" -addext "keyUsage=critical,keyCertSign,cRLSign" \
      -keyout "$dir/logicode-ca.key" -out "$dir/logicode-ca.crt" 2>/dev/null
  fi
  # Cada nombre o IP de la lista va al certificado; las IP como IP y lo demás como nombre DNS.
  san=$(echo "localhost,127.0.0.1,${LOGICODE_TLS_HOSTS:-}" | tr ', ' '\n\n' | awk 'NF && !seen[$0]++ {
    printf "%s%s:%s", sep, ($0 ~ /^[0-9.]+$/ ? "IP" : "DNS"), $0; sep = "," }')
  echo "https.sh: creando certificados/logicode.crt para $san"
  printf 'subjectAltName=%s\nextendedKeyUsage=serverAuth\nbasicConstraints=CA:FALSE\n' "$san" > /tmp/logicode.ext
  openssl req -newkey rsa:2048 -nodes -subj "/CN=LogiCode" -keyout "$dir/logicode.key" -out /tmp/logicode.csr 2>/dev/null
  # 397 días: el máximo que aceptan los navegadores y teléfonos para un certificado de servidor.
  openssl x509 -req -in /tmp/logicode.csr -CA "$dir/logicode-ca.crt" -CAkey "$dir/logicode-ca.key" \
    -CAcreateserial -days 397 -extfile /tmp/logicode.ext -out "$dir/logicode.crt" 2>/dev/null
fi

cat > /etc/nginx/conf.d/default.conf <<'CONF'
# Modo HTTPS (piloto), escrito por nginx/https.sh. El 8080 (80 afuera) solo redirige a HTTPS.
server {
    listen 8080;
    server_name _;
    server_tokens off;
    location = /nginx-salud { access_log off; return 200 "ok\n"; }
    location / { return 301 https://$host$request_uri; }
}

server {
    listen 8443 ssl;
    http2 on;
    server_name _;
    ssl_certificate /etc/nginx/certificados/logicode.crt;
    ssl_certificate_key /etc/nginx/certificados/logicode.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_session_cache shared:SSL:10m;
    include /etc/nginx/logicode.conf;
}
CONF
