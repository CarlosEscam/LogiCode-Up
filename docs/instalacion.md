# Instalación de LogiCode

Guía única para instalar LogiCode desde cero, ponerlo en marcha, cargar datos de demostración, encender la IA, configurar el correo, servirlo a la red del aula y llevarlo a otro equipo. Para la operación diaria del equipo servidor, ver el [manual técnico](manuales/manual-tecnico.md); para las variables una por una, el [README](../README.md#variables-de-entorno).

## 1. Requisitos

| Necesidad | Windows 10/11 | Linux |
| --- | --- | --- |
| Contenedores | Docker Desktop con WSL 2, abierto y con *Engine running* | Docker Engine y Docker Compose v2.24 o superior |
| Código | Git para Windows | `git` |
| Datos demo y pruebas de IA (opcional) | Node.js 20 o superior | Node.js 20 o superior |
| Disco | Unos 10 GB libres más los archivos que se carguen; con IA, unos 6 GB más para el modelo | Igual |
| Memoria | 8 GB sin IA; con IA, el equipo de referencia tiene 32 GB (Ryzen 7 5700U) | Igual |

Para desarrollar fuera de Docker (opcional): JDK 21 y Node.js 22 LTS (README, "Desarrollo con recarga rápida").

## 2. Obtener el código

Con acceso al repositorio:

```bash
git clone <dirección del repositorio> LogiCode2.0
cd LogiCode2.0
```

Si el código llega como **bundle de Git** (un archivo `.bundle`, por ejemplo `logicode-completo.bundle`), ver la [sección 11.1](#111-recibir-el-código-como-bundle).

## 3. Crear el `.env`

El archivo `.env` va en la raíz del proyecto, junto a `docker-compose.yml`. Contiene contraseñas y datos personales: nunca se sube al repositorio (está en `.gitignore`).

```bash
cp .env.example .env            # Windows PowerShell: Copy-Item .env.example .env
```

Edítalo y cambia como mínimo:

| Variable | Qué poner |
| --- | --- |
| `DB_PASSWORD` | Una contraseña larga y propia para la base de datos. PostgreSQL la toma cuando crea la base por primera vez |
| `MINIO_ROOT_PASSWORD` | Una contraseña propia para MinIO, donde se guardan los archivos (mínimo 8 caracteres). Sin ella LogiCode no arranca. MinIO la toma la primera vez; para cambiarla después hay que crear el volumen `minio-data` de nuevo |
| `INITIAL_ADMIN_1_CEDULA`, `INITIAL_ADMIN_1_NAME`, `INITIAL_ADMIN_1_EMAIL` | Cédula, nombre y correo de la primera cuenta inicial (privilegio completo) |
| `INITIAL_ADMIN_2_*` | Los de la segunda cuenta inicial. Para una prueba se pueden dejar vacíos |
| `LOGICODE_PUBLIC_URL` | La dirección con la que se abre la interfaz (por defecto `http://localhost:3000`); aparece en los enlaces de los correos |

Los datos de las cuentas iniciales solo van en `.env`: nunca en el código, en las migraciones ni en documentos.

### Puertos ocupados

Si otro programa usa un puerto, cámbialo en `.env`:

| Variable | Por defecto | Escucha en |
| --- | --- | --- |
| `DB_PORT` | `5432` | `127.0.0.1` (solo el equipo) |
| `BACKEND_PORT` | `8080` | `127.0.0.1` (solo el equipo) |
| `FRONTEND_PORT` | `3000` | Todas las interfaces (la red local) |

Por ejemplo, `DB_PORT=5433` y `BACKEND_PORT=8081` si ya hay otro PostgreSQL u otra aplicación en esos puertos. Si cambias `FRONTEND_PORT`, cambia también `LOGICODE_PUBLIC_URL` (por ejemplo `http://localhost:3001`) y, al cargar datos demo, `LOGICODE_URL`.

## 4. Arrancar y detener

```bash
docker compose up -d --build --wait
```

La primera vez descarga imágenes y compila el backend y el frontend (10 a 20 minutos). Luego:

| Dirección | Qué es |
| --- | --- |
| `http://localhost:3000` | La aplicación. En el pie de la página de inicio, "Estado de la plataforma" debe decir que está en línea y con el esquema en la versión 9 |
| `http://localhost:3000/swagger-ui.html` | Documentación interactiva de la API (solo en desarrollo) |
| `http://localhost:8080/actuator/health` | Estado del backend (o el `BACKEND_PORT` que hayas puesto) |

| Acción | Comando |
| --- | --- |
| Ver el estado | `docker compose ps` (todos `healthy`) |
| Detener sin borrar nada | `docker compose stop` o `docker compose down` |
| Volver a arrancar | `docker compose up -d --wait` |
| Ver los logs del backend | `docker compose logs backend --since 1h` |
| **Borrar** la base de datos y todos los archivos | `docker compose down -v` (no se usa en el piloto) |

En Linux, si el backend no puede escribir los correos de prueba, crea la carpeta antes de arrancar: `mkdir -p .local/mail-outbox && chmod a+w .local/mail-outbox`. En Windows no hace falta.

## 5. Primer ingreso de una cuenta inicial

Las dos cuentas iniciales se crean solas al arrancar el backend a partir de `INITIAL_ADMIN_*`, sin contraseña:

1. Abre el correo de verificación. Sin Brevo no se envía: queda como archivo `.html` en `.local/mail-outbox/`. Ábrelo con el navegador y entra al enlace (vence en 24 horas). Si se perdió o venció, `docker compose restart backend` genera uno nuevo.
2. En la aplicación, **Iniciar sesión** → **¿Olvidaste tu contraseña?**, escribe el mismo correo y abre el nuevo archivo de `.local/mail-outbox/` (vence en 60 minutos).
3. Crea la contraseña: 8 caracteres o más, con al menos un número y un carácter especial.
4. Inicia sesión con la cédula y la contraseña.

Desde esa cuenta se aprueban docentes, se habilitan cédulas y se cambian roles ([manual del administrador](manuales/manual-administrador.md)).

## 6. Cargar datos de demostración (opcional)

Para probar o presentar con cuentas y contenidos ficticios, con la aplicación corriendo:

```bash
node datos-demo/cargar-datos-demo.mjs
```

El script pide la cédula y la contraseña de una cuenta inicial y no las guarda. Si el frontend no está en `http://localhost:3000`, define antes `LOGICODE_URL` (PowerShell: `$env:LOGICODE_URL = "http://localhost:3001"`). Las cuentas demo y lo que contiene cada etapa están en [datos-demo/README.md](../datos-demo/README.md). **Nunca se cargan en el piloto**: sus cuentas tienen una contraseña pública.

## 7. Encender la calificación con IA (opcional)

La IA viene apagada y todas las entregas van a Por revisar. Para encenderla:

1. En `.env`, `LOGICODE_AI_ENABLED=true`.
2. Arranca con el perfil `ia` y descarga el modelo una vez (unos 6 GB, quedan en el volumen `ollama-models`):

   ```bash
   docker compose --profile ia up -d --build --wait
   docker compose exec ollama ollama pull qwen2.5vl:7b
   docker compose exec ollama ollama show qwen2.5vl:7b    # debe indicar Q4_K_M
   ```

3. Desde entonces arranca siempre con `docker compose --profile ia up -d`. Para apagarla, vuelve a `false` y reinicia el backend; lo que estaba en cola pasa al docente.

En CPU cada foto puede tardar varios minutos. Las pruebas de aceptación del modelo en el equipo de referencia siguen **Pendientes**: [ia-pruebas/README.md](../ia-pruebas/README.md).

## 8. Correo real con Brevo (opcional)

Sin Brevo, los correos de verificación y recuperación quedan como archivos en `.local/mail-outbox/` del equipo servidor y no les llegan a los usuarios.

1. En Brevo, agrega y verifica el remitente `logicode.notificaciones@gmail.com` (Brevo envía un código a ese correo). Es el valor de `BREVO_SENDER_EMAIL` en `.env`.
2. Crea `secrets/brevo_api_key.txt` con la API key. La carpeta `secrets/` está fuera de Git; la API key se maneja como secreto de Docker, nunca en `.env`.
3. Arranca con el archivo adicional:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.brevo.yml up -d --build --wait
   ```

4. Pide una recuperación de contraseña con un correo real y revisa que llegue. Brevo no puede autenticar una dirección de Gmail, así que puede mostrar el remitente con un dominio de Brevo o el correo puede llegar a spam; si pasa, la solución es un dominio propio verificado en Brevo (T-86).

## 9. Modo piloto con HTTPS

Para usar LogiCode desde los computadores y celulares de la red del aula, se suma `docker-compose.piloto.yml`: HTTPS en el puerto 443 (el 80 redirige), cookies seguras, documentación de la API apagada y una base de datos aparte. Resumen (detalle en [piloto.md](piloto.md)):

1. Averigua la IP del equipo servidor y pide que la reserven.
2. Agrega a `.env`:

   ```dotenv
   COMPOSE_PROJECT_NAME=logicode-piloto
   COMPOSE_FILE=docker-compose.yml;docker-compose.piloto.yml     # en Linux, separado con ":"
   LOGICODE_PUBLIC_URL=https://<IP del servidor>
   LOGICODE_TLS_HOSTS=<IP del servidor>
   ```

3. En Windows, abre los puertos 80 y 443 en el firewall para redes privadas (comando en piloto.md).
4. `docker compose up -d --build --wait`. La primera vez se crean la autoridad local y el certificado en `certificados/`.
5. Crea la contraseña de las cuentas iniciales (sección 5) y reparte `certificados/logicode-ca.crt` a los equipos del aula. Nunca reparta los archivos `.key`.

## 10. Respaldos y restauración

El servicio `backup` arranca con `docker compose up`: copia cada día la base y los archivos a la hora `BACKUP_HOUR` (Colombia), prueba restaurar cada copia y conserva `BACKUP_RETENTION_DAYS` días. Desde **Administración → Respaldos** se ven las copias y se pide una inmediata.

```bash
docker compose cp backup:/respaldos ./copias-logicode          # sacar las copias del equipo (a un disco cifrado)
docker compose run --rm --no-deps backup listar
docker compose stop backend
docker compose run --rm --no-deps backup restaurar <nombre-de-la-copia>
docker compose start backend
```

Detalle en [respaldos/README.md](../respaldos/README.md). Las copias contienen datos personales y hashes de contraseñas.

## 11. Pasar LogiCode a otro PC

Hay dos situaciones distintas:

| Quiero… | Usa |
| --- | --- |
| Probar LogiCode en otro equipo con una base vacía (y datos demo) | El código como bundle de Git ([11.1](#111-recibir-el-código-como-bundle)) |
| Mover una instalación que ya tiene usuarios, cursos, entregas y notas | El kit de migración `migracion/` ([11.2](#112-mover-una-instalación-con-sus-datos)) |

### 11.1 Recibir el código como bundle

Un bundle trae el código con todo su historial, pero no la base de datos, los archivos subidos ni el `.env`.

```powershell
cd $HOME\Desktop
git clone -b main $HOME\Downloads\logicode-completo.bundle LogiCode2.0
cd LogiCode2.0
git log --oneline -1          # muestra el último commit que trae el bundle
```

Luego sigue las secciones 3 a 6 de esta guía (`.env`, arranque, cuenta inicial y datos demo). Para actualizar una instalación existente con un bundle nuevo: respaldo desde el panel, `git pull <archivo .bundle> main` y `docker compose up -d --build --wait`; las migraciones nuevas se aplican solas al arrancar el backend.

### 11.2 Mover una instalación con sus datos

La carpeta [`migracion/`](../migracion/) trae dos scripts de PowerShell (Windows con Docker Desktop) y su guía paso a paso: [Guia-pasar-LogiCode-a-otro-PC.md](../migracion/Guia-pasar-LogiCode-a-otro-PC.md).

| Paso | Dónde | Qué hace |
| --- | --- | --- |
| Copiar `exportar.ps1` e `importar.ps1` de `migracion/` a la raíz del proyecto | PC actual | Los scripts usan su propia carpeta como carpeta del proyecto |
| `powershell -ExecutionPolicy Bypass -File .\exportar.ps1` (opcional `-ConIA` para llevar el modelo) | PC actual | Respalda la base con `pg_dump`, detiene LogiCode, copia los volúmenes de Docker, comprime la carpeta del proyecto sin `.env` ni `secrets/` y vuelve a arrancar. Deja todo en `%USERPROFILE%\LogiCode-paquete` |
| Copiar a mano y por separado el `.env` y `secrets/` | Ambos | Tienen contraseñas y datos personales; no viajan en el paquete |
| Descomprimir el proyecto, pegar el `.env` y ejecutar `importar.ps1 -Paquete <ruta del paquete>` | PC nuevo | Restaura los volúmenes (se niega si ya existen) y arranca; con `-DesdeDump` carga la base desde el `pg_dump` (plan B) |

Precauciones:

- El paquete contiene la base completa (datos personales, notas y hashes de contraseñas). Trátalo como confidencial, igual que los respaldos, y bórralo de la memoria USB al terminar.
- El zip del proyecto incluye las carpetas que no excluye el script, entre ellas `.local/` (correos con enlaces de un solo uso) y, si existe, `certificados/` (llaves privadas del piloto). Si no las necesitas en el otro PC, bórralas del zip o de la carpeta antes de exportar.
- Revisa los puertos del `.env` en el PC nuevo (sección 3).

## 12. Problemas frecuentes

| Síntoma | Qué hacer |
| --- | --- |
| *"Define DB_PASSWORD en el archivo .env"* | Falta el `.env` o no está en la raíz del proyecto |
| *"Define MINIO_ROOT_PASSWORD en el archivo .env"* | Desde P-05 los archivos van en MinIO: agrega `MINIO_ROOT_PASSWORD` (mínimo 8 caracteres) a `.env` |
| *"port is already allocated"* | Cambia `DB_PORT`, `BACKEND_PORT` o `FRONTEND_PORT` en `.env` y vuelve a arrancar |
| El backend queda `unhealthy` | `docker compose logs backend`: base no disponible, `DB_PASSWORD` distinta de la que tenía el volumen al crearse, o una migración que falló |
| No aparece el correo de verificación | Mira `.local/mail-outbox/`; si no está, `docker compose restart backend`. En Linux revisa los permisos de la carpeta (sección 4) |
| "Cédula o contraseña incorrectas" varias veces | Tras 5 intentos fallidos en 15 minutos la cédula queda bloqueada hasta que pase la ventana |
| Las entregas se quedan en "Calificando con IA" | `docker compose --profile ia ps`; ¿se descargó el modelo? En CPU cada foto tarda minutos; el panel muestra cuántas hay en cola |
| Los celulares no abren el piloto | Firewall de Windows, red marcada como pública o equipos en otra red ([piloto.md](piloto.md)) |
| "La conexión no es privada" | Instala `logicode-ca.crt` en ese equipo ([piloto.md](piloto.md), sección 3) |
| El script de datos demo se niega a cargar | La instalación está en modo piloto; usa una instalación de desarrollo ([datos-demo/README.md](../datos-demo/README.md)) |
| *"la ejecución de scripts está deshabilitada"* al migrar | Ejecuta con `powershell -ExecutionPolicy Bypass -File ...` |
| *"El volumen ... ya existe en este PC"* al importar | Ese PC ya tiene datos de LogiCode. Solo si no los necesitas: `docker compose down -v` y repite |

Más casos en el [manual técnico](manuales/manual-tecnico.md#9-problemas-frecuentes).
