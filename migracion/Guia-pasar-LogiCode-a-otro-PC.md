# Pasar LogiCode a otro PC

Con esta guía el otro PC queda igual que la instalación actual: el mismo código (con todo el historial de git y los cambios que aún no estén en un commit), la misma base de datos (usuarios, cursos, entregas, notas) y los mismos archivos subidos.

Funciona en Windows con Docker Desktop. Se usan los dos scripts de esta carpeta:

- `exportar.ps1` crea el paquete en el PC actual.
- `importar.ps1` lo restaura en el PC nuevo.

Los scripts usan su propia carpeta como carpeta del proyecto, por eso **hay que copiarlos de `migracion/` a la raíz del proyecto** (junto a `docker-compose.yml`) antes de ejecutarlos.

Si solo se necesita el código, sin datos, basta un bundle de git: ver [Instalación, sección 11.1](../docs/instalacion.md#111-recibir-el-código-como-bundle).

En esta guía, `<carpeta del proyecto>` es la carpeta donde está LogiCode en cada PC (por ejemplo `C:\Users\<usuario>\Desktop\LogiCode2.0`).

## Qué viaja en el paquete y qué no

| Va en el paquete | NO va (y qué hacer) |
|---|---|
| Toda la carpeta del proyecto, incluidas `.git`, `datos-demo`, `ia-pruebas`, `.local` (correos de prueba) y, si existe, `certificados` (llaves del piloto) | **`.env`** y **`secrets\`**: tienen contraseñas, cédulas y la clave de Brevo. Se copian a mano por separado |
| La base de datos: copia exacta del volumen de Docker y, además, un respaldo `pg_dump` (plan B) | `node_modules` y compilados (`backend\target`, `frontend\dist`): Docker los vuelve a generar |
| Los archivos subidos y los respaldos diarios (volúmenes `file-storage` y `backups`) | El modelo de IA de Ollama (~6 GB): se vuelve a descargar, salvo que se exporte con `-ConIA` |

> El paquete tiene la base completa: datos personales, notas y hashes de contraseñas. Trátalo como confidencial, igual que los respaldos. Si no necesitas `.local\` ni `certificados\` en el otro PC, bórralos de la carpeta del proyecto antes de exportar o del zip después.

---

## Parte 1 · En el PC actual

1. Copia `migracion\exportar.ps1` y `migracion\importar.ps1` a la raíz del proyecto, junto a `docker-compose.yml`.
2. Abre **Docker Desktop** y espera a que diga *Engine running*.
3. Abre **PowerShell** y ejecuta:

   ```powershell
   cd <carpeta del proyecto>
   powershell -ExecutionPolicy Bypass -File .\exportar.ps1
   ```

   Para llevar también el modelo de IA ya descargado (evita bajar ~6 GB en el otro PC):

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\exportar.ps1 -ConIA
   ```

   El script respalda la base de datos, detiene LogiCode un momento, copia los volúmenes, comprime la carpeta y vuelve a iniciar LogiCode.
4. Al terminar queda la carpeta **`%USERPROFILE%\LogiCode-paquete`** con:

   ```
   LogiCode-paquete\
     <nombre de la carpeta>.zip   el proyecto completo (sin .env ni secrets)
     base-de-datos.dump           respaldo pg_dump (plan B)
     importar.ps1
     volumenes\
       db-data.tgz                base de datos
       file-storage.tgz           archivos subidos
       backups.tgz                respaldos diarios
       ollama-models.tgz          solo con -ConIA
   ```

5. Copia al PC nuevo (USB, disco externo o una carpeta personal en la nube):
   - la carpeta **`LogiCode-paquete`** completa;
   - **aparte**, el archivo **`.env`** y la carpeta **`secrets\`** (si existe) de la carpeta del proyecto.

   > El `.env` tiene datos personales y contraseñas. No lo subas a GitHub ni a carpetas compartidas, y bórralo de la USB al terminar.

---

## Parte 2 · En el PC nuevo

1. Instala lo necesario:
   - **Docker Desktop** (con WSL 2). Reinicia si lo pide y ábrelo hasta que diga *Engine running*.
   - **Node.js 20 o superior**: solo para `datos-demo` y las pruebas de IA.
   - **Git** (recomendado, para seguir haciendo commits).
2. Copia la carpeta `LogiCode-paquete` a `C:\Users\<usuario>\LogiCode-paquete`.
3. Descomprime el proyecto donde vayas a trabajar. Por ejemplo, en el Escritorio:

   ```powershell
   $escritorio = [Environment]::GetFolderPath('Desktop')
   tar -xf "$HOME\LogiCode-paquete\LogiCode2.0.zip" -C $escritorio
   ```

   (Cambia `LogiCode2.0.zip` por el nombre que tenga el zip.)
4. Pega el **`.env`** (y la carpeta **`secrets\`**, si la trajiste) dentro de la carpeta del proyecto.
5. Revisa los puertos del `.env` (`DB_PORT`, `BACKEND_PORT`, `FRONTEND_PORT`). Si en el PC nuevo están ocupados, cámbialos. Si cambias `FRONTEND_PORT`, cambia también `LOGICODE_PUBLIC_URL`.
6. Restaura y arranca:

   ```powershell
   cd <carpeta del proyecto>
   powershell -ExecutionPolicy Bypass -File .\importar.ps1 -Paquete "$HOME\LogiCode-paquete"
   ```

   La primera vez tarda varios minutos porque construye las imágenes del backend y el frontend. Si el `.env` tiene `LOGICODE_AI_ENABLED=true`, también arranca Ollama.
7. Si usas IA y **no** exportaste con `-ConIA`, descarga el modelo una vez:

   ```powershell
   docker compose --profile ia up -d
   docker compose exec ollama ollama pull qwen2.5vl:7b
   ```

## Comprobar que quedó igual

- Abre `http://localhost:3000` (o el `FRONTEND_PORT` del `.env`) y entra con **la misma cuenta y contraseña** de siempre.
- Revisa que estén los cursos, actividades, entregas y notas, y que un archivo de la biblioteca se previsualice o descarga.
- Con Git instalado, `git status` y `git log --oneline -3` deben mostrar lo mismo en los dos PCs.

## Si algo sale mal

| Mensaje o síntoma | Qué hacer |
|---|---|
| *"la ejecución de scripts está deshabilitada"* | Ejecuta el script exactamente con `powershell -ExecutionPolicy Bypass -File ...` |
| *"Fallo: docker compose config"* | Docker Desktop no está abierto, o falta el `.env` en la carpeta |
| *"La carpeta ...LogiCode-paquete ya existe"* (al exportar) | Mueve o borra el paquete anterior y vuelve a exportar |
| *"El volumen ... ya existe en este PC"* (al importar) | Ese PC ya tiene datos de LogiCode. **Solo si no los necesita**, bórralos con `docker compose down -v` dentro de la carpeta del proyecto y vuelve a ejecutar `importar.ps1` |
| La base de datos no arranca después de importar | Plan B con el respaldo: `docker compose down -v` y luego `powershell -ExecutionPolicy Bypass -File .\importar.ps1 -Paquete "$HOME\LogiCode-paquete" -DesdeDump` |
| *"port is already allocated"* | Otro programa usa ese puerto: cámbialo en el `.env` y ejecuta `docker compose up -d` |

## Carpetas sincronizadas en la nube

Si la carpeta del proyecto está en una carpeta sincronizada (OneDrive u otra), la sincronización puede copiar el código, pero **no** lleva la base de datos ni los archivos subidos (viven dentro de Docker), así que igual usa el paquete. Evita trabajar en los dos PCs a la vez sobre la misma carpeta sincronizada, porque la sincronización puede dañar el `.git`.
