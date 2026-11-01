#!/bin/sh
# Respaldos de LogiCode (RNF-09): copia diaria de PostgreSQL y de los archivos cargados, retención por días y prueba
# de restauración después de cada copia. Corre en el servicio `backup` de docker-compose.yml (imagen postgres, que
# trae pg_dump y pg_restore). Ver respaldos/README.md.
#
#   servicio            ciclo: copia diaria a BACKUP_HOUR (hora de Colombia) y las que se piden desde el panel
#   crear               una copia ahora
#   probar <nombre>     restaura la copia en una base temporal y revisa el archivo de archivos
#   restaurar <nombre>  reemplaza la base y los archivos por los de la copia (con el backend y MinIO detenidos)
#
# archivos.tar.gz lleva dos carpetas: minio/ (los datos de MinIO, P-05) y archivos/ (cargas en curso y archivos que
# el backend aún no subió a MinIO). Las copias de antes de MinIO solo traen los archivos sueltos.
#   listar              copias disponibles
set -u

DIR=/respaldos
FILES=/archivos
MINIO=/minio
HOUR=${BACKUP_HOUR:-2}
KEEP=${BACKUP_RETENTION_DAYS:-30}

# Colombia no tiene horario de verano: siempre UTC-5. Así no depende de los datos de zonas de la imagen.
colombia() { date -u -d "@$(( $(date +%s) - 18000 ))" "$1"; }
log() { echo "[respaldo] $(colombia '+%Y-%m-%d %H:%M:%S') $*"; }

probar() {
  folder=$1
  db="logicode_prueba_restauracion"
  errors=$(mktemp)
  result=ERROR
  detail="No se pudo restaurar la base en una copia temporal."
  dropdb --if-exists "$db" 2>/dev/null
  if createdb "$db" 2>>"$errors" \
      && pg_restore --no-owner --exit-on-error --dbname="$db" "$folder/base.dump" 2>>"$errors" \
      && version=$(psql -tA -v ON_ERROR_STOP=1 -d "$db" -c "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1" 2>>"$errors") \
      && accounts=$(psql -tA -v ON_ERROR_STOP=1 -d "$db" -c "SELECT count(*) FROM user_account" 2>>"$errors"); then
    if tar -tzf "$folder/archivos.tar.gz" > "$errors.lista" 2>>"$errors"; then
      # Un objeto de MinIO es una carpeta con su xl.meta; .minio.sys es la configuración de MinIO.
      objects=$(grep -c '^minio/[^.].*/xl\.meta$' "$errors.lista" || true)
      loose=$(grep -v '/$' "$errors.lista" | grep -v '^minio/' | grep -vc '\.staging/' || true)
      files=$((objects + loose))
      result=OK
      detail="Base restaurada en una copia temporal: esquema versión $version, $accounts cuentas; $files archivos legibles. Probada el $(colombia '+%Y-%m-%d %H:%M')."
    else
      detail="La base se restauró, pero el archivo de archivos está dañado."
    fi
  fi
  dropdb --if-exists "$db" 2>>"$errors"
  printf '%s\n%s\n' "$result" "$detail" > "$folder/prueba-restauracion.txt"
  [ "$result" = OK ] || { log "Prueba de restauración fallida: $(tail -n 3 "$errors")"; }
  rm -f "$errors" "$errors.lista"
  log "Prueba de restauración de $(basename "$folder"): $result"
}

crear() {
  name="logicode-$(colombia +%Y%m%d-%H%M%S)"
  work="$DIR/.$name.parcial"
  rm -rf "$work" && mkdir -p "$work" || return 1
  log "Creando $name"
  # Una copia a medias nunca queda con el nombre final: el panel solo lista carpetas logicode-*.
  if ! pg_dump --format=custom --file="$work/base.dump"; then
    log "Falló pg_dump"; rm -rf "$work"; return 1
  fi
  if ! tar -czf "$work/archivos.tar.gz" -C / "${FILES#/}" "${MINIO#/}"; then
    log "Falló la copia de archivos"; rm -rf "$work"; return 1
  fi
  probar "$work"
  mv "$work" "$DIR/$name" || return 1
  find "$DIR" -mindepth 1 -maxdepth 1 -type d -name 'logicode-*' -mtime +"$KEEP" -exec rm -rf {} +
  log "Listo: $name"
}

servicio() {
  mkdir -p "$DIR/solicitudes"
  # El backend (otro usuario) deja aquí las solicitudes del panel.
  chmod 777 "$DIR/solicitudes"
  log "Servicio activo: copia diaria a las $HOUR:00 (hora de Colombia); conserva $KEEP días"
  while true; do
    printf 'hora=%s\nretencion_dias=%s\n' "$HOUR" "$KEEP" > "$DIR/estado.txt"
    today=$(colombia +%Y-%m-%d)
    hour=$(colombia +%H | sed 's/^0//')
    if [ -n "$(ls -A "$DIR/solicitudes")" ]; then
      # La solicitud se borra al terminar, para que el panel la muestre "en curso" mientras tanto.
      crear || log "Falló el respaldo solicitado desde el panel"
      rm -f "$DIR/solicitudes/"*
    elif [ "$hour" -ge "$HOUR" ] && [ "$(cat "$DIR/ultimo-diario" 2>/dev/null)" != "$today" ]; then
      echo "$today" > "$DIR/ultimo-diario"
      crear || log "Falló el respaldo diario"
    fi
    sleep 20
  done
}

restaurar() {
  folder="$DIR/${1:?Indica el nombre de la copia (ver: listar)}"
  [ -f "$folder/base.dump" ] && [ -f "$folder/archivos.tar.gz" ] || { echo "No existe la copia $1"; exit 1; }
  others=$(psql -tA -c "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND pid <> pg_backend_pid()")
  if [ "$others" != 0 ]; then
    echo "Hay $others conexiones abiertas. Detén el backend primero: docker compose stop backend minio"
    exit 1
  fi
  if wget -q -T 3 -O /dev/null http://minio:9000/minio/health/live 2>/dev/null; then
    echo "MinIO sigue en marcha. Detenlo primero: docker compose stop backend minio"
    exit 1
  fi
  log "Restaurando $1"
  # Todo o nada: si algo falla, la base queda como estaba.
  pg_restore --clean --if-exists --no-owner --single-transaction --exit-on-error --dbname="$PGDATABASE" \
      "$folder/base.dump" || { echo "No se restauró la base; quedó como estaba."; exit 1; }
  if tar -tzf "$folder/archivos.tar.gz" | grep -q '^minio/'; then
    find "$FILES" "$MINIO" -mindepth 1 -delete && tar -xzf "$folder/archivos.tar.gz" -C / \
        || { echo "La base se restauró, pero falló la copia de archivos."; exit 1; }
  else
    # Copia de antes de MinIO: los archivos vuelven a la carpeta y el backend los sube a MinIO al arrancar.
    find "$FILES" "$MINIO" -mindepth 1 -delete && tar -xzf "$folder/archivos.tar.gz" -C "$FILES" \
        || { echo "La base se restauró, pero falló la copia de archivos."; exit 1; }
  fi
  log "Restaurada $1. Arranca de nuevo: docker compose start minio backend"
}

case "${1:-servicio}" in
  servicio) servicio ;;
  crear) crear ;;
  probar) probar "$DIR/${2:?Indica el nombre de la copia}" ;;
  restaurar) restaurar "${2:-}" ;;
  listar) ls -1 "$DIR" | grep '^logicode-' || echo "Aún no hay copias." ;;
  *) echo "Uso: respaldo.sh servicio|crear|probar <nombre>|restaurar <nombre>|listar"; exit 1 ;;
esac
