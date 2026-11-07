# Piloto y entrega (Etapa 10)

El piloto es LogiCode instalado con Docker Compose en un PC del equipo (el **equipo servidor**, por ejemplo el portátil Ryzen 7 5700U de la presentación) y usado por el docente y su grupo desde los computadores y celulares conectados a la misma red, por HTTPS. Entrega objetivo: 10 de diciembre de 2026.

La publicación en línea sigue en observación: el proveedor de alojamiento y de almacenamiento de objetos se decide antes de publicar (especificación, Etapa 10). Todo despliegue futuro será también con Docker, con estos mismos archivos.

Manuales: [técnico](manuales/manual-tecnico.md) · [docente](manuales/manual-docente.md) · [estudiante](manuales/manual-estudiante.md).

## 1. Qué cambia en el modo piloto

`docker-compose.piloto.yml` se suma a `docker-compose.yml` y solo cambia lo necesario para servir a otros equipos:

| Qué | Desarrollo | Piloto |
| --- | --- | --- |
| Dirección | `http://localhost:3000` | `https://<IP o nombre del equipo servidor>` (puerto 443; el 80 solo redirige a HTTPS) |
| Certificado | No hay | `certificados/logicode.crt`, creado la primera vez por una autoridad local (`certificados/logicode-ca.crt`) para `localhost` y los nombres o IP de `LOGICODE_TLS_HOSTS`. Si la Universidad entrega un certificado, basta con ponerlo como `logicode.crt` y `logicode.key` |
| Cookies de sesión y CSRF | Sin `Secure` | `Secure`: solo viajan cifradas (RNF-02, R9-01) |
| Documentación de la API (`/swagger-ui.html`) | Publicada | Apagada (R9-02) |
| Base de datos y archivos | Volúmenes del proyecto `logicode` | Volúmenes propios si se usa `COMPOSE_PROJECT_NAME=logicode-piloto` (recomendado): los datos de demostración no se mezclan con los reales |

Lo demás es igual: mismas imágenes, migraciones, respaldos diarios con prueba de restauración, IA opcional (`--profile ia`) y correo con Brevo opcional (`docker-compose.brevo.yml`).

## 2. Preparar el equipo servidor (una vez)

1. Instala Docker Desktop y aplica el último bundle (README, "Iniciar").
2. Averigua la IP del equipo en la red del aula: `ipconfig` en PowerShell, línea "Dirección IPv4" del adaptador Wi-Fi o Ethernet (por ejemplo `192.168.1.50`). Conviene pedir al encargado de la red que la reserve, porque si cambia hay que rehacer el certificado (paso 6 de la sección 4).
3. Agrega estas líneas a tu `.env` (con tu IP):

   ```dotenv
   # --- Piloto (Etapa 10) ---
   COMPOSE_PROJECT_NAME=logicode-piloto
   COMPOSE_FILE=docker-compose.yml;docker-compose.piloto.yml
   LOGICODE_PUBLIC_URL=https://192.168.1.50
   LOGICODE_TLS_HOSTS=192.168.1.50
   ```

   - `COMPOSE_FILE` hace que **todos** los comandos `docker compose …` (también los de respaldos) usen el modo piloto sin escribir `-f`. En Windows los archivos se separan con `;`; en Linux o macOS, con `:`. Con Brevo: `docker-compose.yml;docker-compose.brevo.yml;docker-compose.piloto.yml`.
   - `COMPOSE_PROJECT_NAME=logicode-piloto` crea una base de datos y unos archivos nuevos, vacíos, para el piloto. La instalación de desarrollo con los datos de demostración queda intacta: para volver a ella, comenta estas cuatro líneas (y detén el piloto antes, porque usan los mismos puertos).
   - `DB_PASSWORD` debe ser una contraseña larga y propia: con una base nueva, PostgreSQL toma la que esté en `.env` al crearse.
4. Abre los puertos 80 y 443 en el firewall de Windows (PowerShell como administrador, una vez):

   ```powershell
   New-NetFirewallRule -DisplayName "LogiCode piloto" -Direction Inbound -Protocol TCP -LocalPort 80,443 -Action Allow -Profile Private
   ```

   La red del aula debe estar marcada como "Privada" en Windows. Si otro programa usa el puerto 80 o 443 (IIS, Skype antiguo, otro proyecto), detenlo durante el piloto.
5. Arranca:

   ```powershell
   docker compose up -d --build --wait
   ```

   La primera vez el frontend crea `certificados/` con la autoridad local y el certificado (`docker compose logs frontend` muestra para qué nombres e IP).
6. Crea la contraseña de las dos cuentas iniciales como en el README ("Primer ingreso de las cuentas iniciales"). Los enlaces de los correos ya llevan la dirección `https://`.
7. **No cargues los datos de demostración en el piloto**: sus cuentas tienen una contraseña conocida (`Demo#2026`). Para presentar con datos ficticios usa la instalación de desarrollo.

## 3. Que los equipos confíen en el certificado

El certificado lo firmó la autoridad local del equipo servidor, que los navegadores no conocen. Hay dos opciones:

- **Aceptar la advertencia** una vez por navegador ("Configuración avanzada" → "Continuar a …"). Funciona, pero enseña a los estudiantes a ignorar avisos de seguridad.
- **Instalar `certificados/logicode-ca.crt`** (recomendado en los equipos del aula y del docente). Se reparte por correo o USB; nunca reparta `logicode-ca.key` ni `logicode.key`.
  - Windows: doble clic → "Instalar certificado" → "Usuario actual" → "Colocar todos los certificados en el siguiente almacén" → "Entidades de certificación raíz de confianza".
  - Android: Ajustes → Seguridad → Más ajustes de seguridad → Instalar desde el almacenamiento → "Certificado de CA".
  - iPhone: abrir el archivo, instalar el perfil en Ajustes → General → VPN y gestión de dispositivos, y activarlo en Ajustes → General → Información → Ajustes de confianza de certificados.

Con un certificado de la Universidad o de una entidad pública no hace falta ninguno de los dos pasos.

## 4. Operación durante el piloto

| Tarea | Cómo |
| --- | --- |
| Ver que todo esté arriba | `docker compose ps` (todos `healthy`) y la página de inicio, sección "Estado de la plataforma" |
| Detener al final del día | `docker compose stop` (no borra nada). Para retomar: `docker compose up -d --wait` |
| Respaldo antes de un cambio | Administración → Respaldos → **Crear respaldo ahora** y esperar a que la copia diga "Restauración probada" |
| Copiar respaldos fuera del equipo | `docker compose cp backup:/respaldos ./copias-logicode` y guardar la carpeta en un disco cifrado (contiene datos de todos) |
| Restaurar | [respaldos/README.md](../respaldos/README.md); con `COMPOSE_FILE` en `.env` los comandos son los mismos |
| Si cambió la IP del equipo | Actualiza `LOGICODE_PUBLIC_URL` y `LOGICODE_TLS_HOSTS` en `.env`, borra `certificados/logicode.crt` y `certificados/logicode.key` (no la autoridad) y ejecuta `docker compose up -d --wait`. Los equipos que instalaron `logicode-ca.crt` no tienen que hacer nada |
| Instalar una versión nueva (bundle) | Respaldo → `git pull <bundle> main` → `docker compose up -d --build --wait`. Las migraciones de la base corren solas al arrancar el backend |
| Buscar un error reportado | `docker compose logs backend | Select-String <X-Request-Id>` ([calidad.md](calidad.md#observabilidad)) |

## 5. Plan del piloto

| Momento | Qué se hace | Quién |
| --- | --- | --- |
| Antes | Equipo servidor preparado (sección 2), restauración probada en la instalación del piloto (respaldo → cambio → restaurar → el cambio desaparece), curso y temas creados, cédulas del grupo habilitadas | Equipo |
| Sesión 1 (aula) | Registro de los estudiantes, recorrido por la biblioteca, los temas y los juegos, un quiz de repaso | Docente con el grupo |
| Durante el piloto | Al menos una actividad de cada tipo: entrega en foto o archivo, taller de PSeInt con casos de prueba y quiz calificable; uso del foro | Docente y estudiantes |
| Cierre | Planilla revisada y exportada, notas consultadas por los estudiantes, observaciones registradas, acta firmada | Docente y equipo |

Cada problema que aparezca se anota en el registro de observaciones (sección 6) con la hora y, si la pantalla mostró un error, el `X-Request-Id` o una captura. Lo que impida el uso se corrige antes de cerrar el piloto; lo demás queda como mejora.

Con la IA encendida, el curso del piloto empieza en calibración: el docente confirma cada propuesta de la IA ([manual del docente](manuales/manual-docente.md#calibración-de-la-ia)). Si una nota de la IA llega al estudiante sin esa confirmación, se detiene el uso de la IA (`LOGICODE_AI_ENABLED=false`) y se anota en el registro. Con la IA encendida, las fotos tardan varios minutos en el equipo de referencia; el piloto también sirve para registrar las pruebas de aceptación de Qwen (`ia-pruebas/`).

## 6. Registro de observaciones

| # | Fecha | Quién | Pantalla o función | Qué pasó | Gravedad (bloquea / molesta / sugerencia) | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | | | | | | |

## 7. Acta de aceptación del piloto

Curso: ______________________ · Periodo: ________ · Fechas del piloto: del ________ al ________

| # | Criterio | Cumple | Observaciones |
| --- | --- | --- | --- |
| 1 | Los estudiantes se registraron e ingresaron con su cédula desde computador y celular, por HTTPS | ☐ Sí ☐ No | |
| 2 | Visitantes y estudiantes vieron solo los materiales permitidos (biblioteca pública y contenido del curso) | ☐ Sí ☐ No | |
| 3 | El docente creó temas, materiales y actividades con rúbrica, y los estudiantes entregaron | ☐ Sí ☐ No | |
| 4 | Las notas automáticas (PSeInt y quiz) y las del docente quedaron bien; las que la IA no pudo leer pasaron al docente | ☐ Sí ☐ No | |
| 5 | La planilla muestra el avance parcial y la nota definitiva por separado, y la exportación coincide con la pantalla | ☐ Sí ☐ No | |
| 6 | Foro y juegos usados sin problemas que impidan su uso | ☐ Sí ☐ No | |
| 7 | Se restauró un respaldo de la instalación del piloto y los datos quedaron como en el momento del respaldo | ☐ Sí ☐ No | |
| 8 | Se entregaron los manuales técnico, docente y estudiante | ☐ Sí ☐ No | |
| 9 | Las observaciones que bloqueaban el uso quedaron resueltas | ☐ Sí ☐ No | |
| 10 | Mientras el curso estuvo en calibración, ninguna nota de la IA llegó al estudiante sin que el docente la confirmara | ☐ Sí ☐ No | |

Decisión: ☐ Piloto aceptado ☐ Aceptado con observaciones ☐ No aceptado

| Rol | Nombre | Firma | Fecha |
| --- | --- | --- | --- |
| Docente | | | |
| Equipo LogiCode | | | |
| Equipo LogiCode | | | |

## 8. Mostrar LogiCode fuera de la red (demostraciones)

Para enseñar LogiCode a alguien que no está en la misma red, mientras la publicación en línea (P-09) sigue en observación, se usa un túnel gratuito de [ngrok](https://ngrok.com). No es el piloto: es una instalación normal (sin `docker-compose.piloto.yml`) que se abre a internet solo mientras dura la demostración.

1. Crea una cuenta gratis en ngrok. En el panel copia el **Authtoken** y, en **Domains**, el dominio fijo gratuito que te asigna (algo como `nombre-asignado.ngrok-free.dev`). El token es secreto: no lo escribas en el repositorio ni en `.env`.
2. En `.env` pon `LOGICODE_PUBLIC_URL=https://<tu-dominio-de-ngrok>` y corre `docker compose up -d --wait`, para que los enlaces de los correos lleven esa dirección. Se hace una sola vez porque el dominio no cambia.
3. Abre el túnel con ngrok dentro de Docker (en Windows, Smart App Control puede bloquear `ngrok.exe`; no hace falta desactivarlo):

   ```
   docker run -it --rm -e NGROK_AUTHTOKEN=<tu-token> ngrok/ngrok:latest http --url=<tu-dominio-de-ngrok> host.docker.internal:3000
   ```

   `host.docker.internal:3000` es LogiCode visto desde Docker (puerto `FRONTEND_PORT`); en el navegador se entra por la dirección de ngrok o por `http://localhost:3000`.
4. La primera vez que alguien abre el enlace, ngrok muestra un aviso; se toca **Visit Site**. Al abrir la página sin sesión, `GET /api/v1/me` responde `401` y la página muestra al visitante: es lo esperado.
5. Cierra la ventana con Ctrl+C al terminar. Mientras está abierta, cualquiera con el enlace llega a LogiCode, y esta instalación usa la configuración de desarrollo (documentación de la API visible y cookie de sesión sin `Secure`).

Probado el 1 de noviembre de 2026 en el equipo de Carlos: LogiCode abre desde la dirección de ngrok.
