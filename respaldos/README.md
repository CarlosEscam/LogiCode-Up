# Respaldos de LogiCode

Implementa RNF-09: copias diarias de PostgreSQL y de los archivos cargados, retención, acceso y prueba de restauración.

## Cómo funciona

- El servicio `backup` de `docker-compose.yml` arranca con `docker compose up` y ejecuta [respaldo.sh](respaldo.sh).
- Cada día, a la hora `BACKUP_HOUR` de `.env` (hora de Colombia, por defecto 2), crea una carpeta `logicode-AAAAMMDD-HHMMSS` en el volumen `backups` con:
  - `base.dump`: la base completa (`pg_dump` en formato personalizado).
  - `archivos.tar.gz`: el volumen `file-storage` (materiales y entregas).
  - `prueba-restauracion.txt`: resultado de restaurar la copia en una base temporal y leer el archivo de archivos. Se hace después de **cada** copia; el panel muestra `OK` o `ERROR`.
- Si el servicio arranca después de la hora del día y aún no hay copia de ese día, la hace de una vez.
- Borra las copias de más de `BACKUP_RETENTION_DAYS` días (por defecto 30). Los archivos cargados siguen guardándose al menos seis meses en `file-storage` (RN-21); la retención de aquí es la de las copias.
- Desde **Administración → Respaldos** (`/admin`), una cuenta Administrador ve las copias y pide una inmediata; el servicio la toma en menos de un minuto.

## Acceso

Las copias contienen todo, incluidos los hashes de contraseñas. Solo las ven las cuentas Administrador desde el panel (sin descarga por la web) y quien tiene acceso a Docker en el equipo. Para guardarlas fuera del equipo, cópialas a un disco cifrado o a la nube institucional:

```bash
docker compose cp backup:/respaldos ./copias-logicode
```

## Restaurar una copia

```bash
docker compose run --rm --no-deps backup listar
docker compose stop backend
docker compose run --rm --no-deps backup restaurar logicode-20261027-020000
docker compose start backend
```

`--no-deps` evita que Compose vuelva a arrancar el backend. `restaurar` se niega si el backend sigue conectado a la base. La restauración de la base es de todo o nada; luego reemplaza el contenido de `file-storage` por el de la copia. Para solo comprobar una copia sin tocar nada: `docker compose run --rm --no-deps backup probar <copia>`.
