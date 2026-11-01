# LogiCode · restaura en este PC el paquete creado con exportar.ps1 (volumenes de base de datos y archivos subidos).
# Uso, desde la carpeta del proyecto ya descomprimida, con el .env copiado y Docker Desktop abierto:
#   powershell -ExecutionPolicy Bypass -File .\importar.ps1 -Paquete C:\ruta\LogiCode-paquete
# Plan B: con -DesdeDump la base de datos se carga desde base-de-datos.dump (pg_restore) en vez de la copia del volumen.
param(
    [Parameter(Mandatory = $true)][string]$Paquete,
    [switch]$DesdeDump
)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Paso($mensaje) { Write-Host "`n== $mensaje" -ForegroundColor Cyan }
function Revisar($mensaje) { if ($LASTEXITCODE -ne 0) { throw "Fallo: $mensaje" } }

if (-not (Test-Path ".env")) { throw "Falta el archivo .env en esta carpeta. Copialo a mano desde el otro PC y vuelve a ejecutar." }
$carpetaVol = Join-Path $Paquete "volumenes"
if (-not (Test-Path $carpetaVol)) { throw "No encontre $carpetaVol. -Paquete debe ser la carpeta LogiCode-paquete." }

Paso "Leyendo la configuracion de Docker Compose"
$config = docker compose --profile ia config --format json | Out-String | ConvertFrom-Json
Revisar "docker compose config (esta abierto Docker Desktop?)"
$pg = $config.services.PSObject.Properties | Where-Object { $_.Value.image -like "postgres*" } | Select-Object -First 1
if (-not $pg) { throw "No encontre el servicio de PostgreSQL en docker-compose.yml" }
$pgServicio = $pg.Name
$imagenAyuda = $pg.Value.image
$pgVolumen = ($pg.Value.volumes | Where-Object { $_.target -like "/var/lib/postgresql*" } | Select-Object -First 1).source

Paso "Restaurando los volumenes de Docker"
$existentes = docker volume ls -q
foreach ($archivo in Get-ChildItem $carpetaVol -Filter *.tgz) {
    $clave = $archivo.Name -replace '\.tgz$', ''
    $volumen = $config.volumes.$clave
    if ($DesdeDump -and $clave -eq $pgVolumen) { Write-Host "  - ${clave}: se carga desde base-de-datos.dump"; continue }
    if (-not $volumen) { Write-Warning "docker-compose.yml no declara el volumen '$clave'; se omite."; continue }
    $nombre = $volumen.name
    # No se mezclan datos: si el volumen ya existe hay que borrarlo a proposito antes (docker compose down -v).
    if ($existentes -contains $nombre) { throw "El volumen $nombre ya existe en este PC. Revisa la guia (paso 'Si algo sale mal')." }
    Write-Host "  - $clave ($nombre)"
    docker volume create --label "com.docker.compose.project=$($config.name)" --label "com.docker.compose.volume=$clave" $nombre | Out-Null
    Revisar "crear el volumen $clave"
    docker run --rm -v "${nombre}:/v" -v "$($archivo.DirectoryName):/b:ro" --entrypoint tar $imagenAyuda -xzf "/b/$($archivo.Name)" -C /v
    Revisar "restaurar el volumen $clave"
}

if ($DesdeDump) {
    Paso "Cargando la base de datos desde base-de-datos.dump (pg_restore)"
    docker compose up -d --wait $pgServicio; Revisar "iniciar $pgServicio"
    # En un volumen nuevo, PostgreSQL se reinicia una vez al terminar de crearse; se espera a que escuche por TCP.
    docker compose exec -T $pgServicio sh -c 'until pg_isready -q -h 127.0.0.1; do sleep 1; done'
    docker compose cp (Join-Path $Paquete "base-de-datos.dump") "${pgServicio}:/tmp/logicode.dump"; Revisar "copiar el respaldo"
    docker compose exec -T $pgServicio sh -c 'pg_restore -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-postgres} --no-owner --exit-on-error /tmp/logicode.dump'
    Revisar "pg_restore"
    docker compose exec -T $pgServicio rm -f /tmp/logicode.dump
}

$perfil = @()
if (Select-String -Path ".env" -Pattern '^\s*LOGICODE_AI_ENABLED\s*=\s*true' -Quiet) { $perfil = @("--profile", "ia") }
Paso "Construyendo e iniciando LogiCode (la primera vez tarda varios minutos)"
docker compose @perfil up -d --build --wait; Revisar "docker compose up"

Paso "Listo. LogiCode quedo con los mismos datos del otro PC."
docker compose @perfil ps
