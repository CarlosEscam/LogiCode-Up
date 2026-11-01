# LogiCode · crea un paquete para pasar el proyecto a otro PC: codigo (con historial git), base de datos y archivos subidos.
# Uso, desde la carpeta del proyecto y con Docker Desktop abierto:
#   powershell -ExecutionPolicy Bypass -File .\exportar.ps1
# El .env y la carpeta secrets\ NO entran al paquete: se copian a mano (tienen contrasenas y datos personales).
param(
    [string]$Destino = (Join-Path $env:USERPROFILE "LogiCode-paquete"),
    # Incluye el volumen de Ollama (el modelo pesa ~6 GB). Sin esto, el otro PC lo vuelve a descargar.
    [switch]$ConIA
)
$ErrorActionPreference = "Stop"
$proyecto = $PSScriptRoot
Set-Location $proyecto

function Paso($mensaje) { Write-Host "`n== $mensaje" -ForegroundColor Cyan }
function Revisar($mensaje) { if ($LASTEXITCODE -ne 0) { throw "Fallo: $mensaje" } }

if (Test-Path $Destino) { throw "La carpeta $Destino ya existe. Muevela o borrala y vuelve a ejecutar." }
New-Item -ItemType Directory -Path (Join-Path $Destino "volumenes") | Out-Null

Paso "Leyendo la configuracion de Docker Compose"
$config = docker compose --profile ia config --format json | Out-String | ConvertFrom-Json
Revisar "docker compose config (esta abierto Docker Desktop y existe el .env?)"
$pg = $config.services.PSObject.Properties | Where-Object { $_.Value.image -like "postgres*" } | Select-Object -First 1
if (-not $pg) { throw "No encontre el servicio de PostgreSQL en docker-compose.yml" }
$pgServicio = $pg.Name
$imagenAyuda = $pg.Value.image  # se reusa la imagen de PostgreSQL (trae tar) para no descargar otra

Paso "Respaldando la base de datos con pg_dump (plan B por si la copia del volumen no sirve)"
docker compose up -d --wait $pgServicio; Revisar "iniciar $pgServicio"
docker compose exec -T $pgServicio sh -c 'pg_dump -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-postgres} -Fc -f /tmp/logicode.dump'
Revisar "pg_dump"
docker compose cp "${pgServicio}:/tmp/logicode.dump" (Join-Path $Destino "base-de-datos.dump"); Revisar "copiar el respaldo"
docker compose exec -T $pgServicio rm -f /tmp/logicode.dump

Paso "Deteniendo LogiCode para copiar los volumenes sin cambios a medias"
docker compose --profile ia stop; Revisar "docker compose stop"

Paso "Copiando los volumenes de Docker (base de datos, archivos subidos...)"
$existentes = docker volume ls -q
foreach ($v in $config.volumes.PSObject.Properties) {
    $clave = $v.Name
    $nombre = $v.Value.name
    if (-not $ConIA -and $clave -like "*ollama*") { Write-Host "  - ${clave}: se omite (usa -ConIA para incluirlo)"; continue }
    if ($existentes -notcontains $nombre) { Write-Host "  - ${clave}: no existe en este PC, se omite"; continue }
    Write-Host "  - $clave ($nombre)"
    docker run --rm -v "${nombre}:/v:ro" -v "$(Join-Path $Destino 'volumenes'):/b" --entrypoint tar $imagenAyuda -czf "/b/$clave.tgz" -C /v .
    Revisar "copiar el volumen $clave"
}

Paso "Comprimiendo la carpeta del proyecto (sin .env, secrets, node_modules ni compilados)"
$padre = Split-Path $proyecto -Parent
$carpeta = Split-Path $proyecto -Leaf
tar.exe -a -c -f (Join-Path $Destino "$carpeta.zip") `
    --exclude=node_modules --exclude="$carpeta/.env" --exclude="$carpeta/secrets" `
    --exclude="$carpeta/backend/target" --exclude="$carpeta/frontend/dist" `
    -C $padre $carpeta
Revisar "comprimir el proyecto"
Copy-Item (Join-Path $proyecto "importar.ps1") $Destino -ErrorAction SilentlyContinue

Paso "Volviendo a iniciar LogiCode en este PC"
docker compose up -d

Paso "Listo. Paquete en $Destino"
Get-ChildItem $Destino -Recurse -File | ForEach-Object { "{0,10:N1} MB  {1}" -f ($_.Length / 1MB), $_.FullName }
Write-Host "Copia tambien A MANO, por separado: el archivo .env y la carpeta secrets\ (si existe)." -ForegroundColor Yellow
