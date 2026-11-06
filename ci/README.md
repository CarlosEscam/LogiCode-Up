# CI, seguridad y despliegue de LogiCode

Todo corre en el equipo del proyecto, sin subir el código a GitHub (decisión de Carlos del 6 de noviembre de 2026). Los workflows de [.github/workflows](../.github/workflows) quedan listos y solo llaman a estos scripts, así que si algún día se suben hacen exactamente lo mismo.

Requisitos: Docker Desktop encendido y Node 22 o más nuevo. No hace falta instalar Java, Maven, Semgrep ni Trivy: cada etapa corre en una imagen de Docker con versión fija.

## CI: `node ci/ci-local.mjs`

```
node ci/ci-local.mjs              # todas las etapas, en orden
node ci/ci-local.mjs backend      # una sola: lint | backend | frontend | seguridad
```

| Etapa | Qué hace | Puerta (si no se cumple, termina en FALLA) | Workflow |
| --- | --- | --- | --- |
| `lint` | oxlint en la interfaz; valida `docker-compose.yml` solo, con el archivo del piloto y con el de Brevo | 0 advertencias y 0 errores; los tres compose válidos | `ci.yml` |
| `backend` | `mvn verify`: 89 pruebas unitarias y de arquitectura (ArchUnit) y 55 de integración con PostgreSQL real (Testcontainers); informe de JaCoCo | Todas en verde; cobertura de líneas ≥ 85 % y de ramas ≥ 68 % (`backend/pom.xml`) | `ci.yml` |
| `frontend` | Vitest y construcción de Vite | Todas en verde; la construcción termina | `ci.yml` |
| `seguridad` | Semgrep (SAST) sobre el código, Dockerfiles, Nginx y compose; Trivy sobre dependencias, secretos y configuración del repositorio y sobre las imágenes `logicode-backend` y `logicode-frontend` | 0 hallazgos ERROR de Semgrep; 0 HIGH o CRITICAL de Trivy salvo los justificados en [.trivyignore.yaml](../.trivyignore.yaml) | `ci-security.yml` |

Si una etapa falla, las siguientes no corren (como en GitHub). El informe de cobertura queda en `backend/target/site/jacoco/index.html` y el de Semgrep en `ci/resultados/semgrep.json`. La primera vez tarda más porque descarga las imágenes; después Maven, npm y la base de Trivy quedan en los volúmenes `logicode-ci-m2`, `logicode-ci-node` y `logicode-ci-trivy`.

Si las imágenes `logicode-backend` y `logicode-frontend` no existen, la etapa `seguridad` las construye con `docker compose build`.

### Por qué esos umbrales

- Cobertura: medida el 6 de noviembre de 2026 en 88,6 % de líneas y 71,2 % de ramas. La puerta queda unos 3 puntos abajo: un cambio grande sin pruebas la rompe, uno normal no.
- Semgrep: hoy hay 12 hallazgos, ninguno ERROR (9 INFO, 3 WARNING). Los 3 WARNING (`no-replaceall`) son falsos positivos: `replaceAll` con un texto reemplaza todas las apariciones. Están explicados en [docs/calidad.md](../docs/calidad.md).
- Trivy: HIGH y CRITICAL bloquean. Cada excepción tiene motivo y fecha de vencimiento.

### Resultados del 6 de noviembre de 2026 (copia de ensayo en la nube)

| Corrida | Resultado |
| --- | --- |
| Primera corrida de `seguridad` | **FALLA**: Trivy encontró CVE-2026-109518 (HIGH) en `apt` de la imagen base del backend. Se revisó y se documentó como riesgo aceptado con vencimiento (no hay imagen corregida todavía) |
| Todas las etapas, después | OK: lint 5 s, backend 169 s, frontend 14 s, seguridad 43 s |
| Puerta de cobertura subida a 95 % a propósito | FALLA: "lines covered ratio is 0.88, but expected minimum is 0.95" |

**No ensayado**: correr `ci-local.mjs` en el PC con Windows. La etapa `backend` usa Testcontainers desde dentro de un contenedor (`TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`); en la nube funcionó, en Docker Desktop debería funcionar igual, pero falta comprobarlo.

`LOGICODE_CI_DOCKER_ARGS` agrega argumentos a cada `docker run` (en la nube se usó para pasar el proxy de la red). En un PC normal se deja vacío. `LOGICODE_SEMGREP_REGLAS` cambia las reglas de Semgrep (por defecto las del registro: `p/java p/javascript p/secrets p/dockerfile p/docker-compose`).

## Despliegue: `node ci/desplegar.mjs`

Entrega continua: una persona decide cuándo, el script hace el resto y vuelve atrás solo si algo falla.

```
git pull <archivo .bundle> main
node ci/desplegar.mjs
```

1. Anota la versión, el esquema de la base y las imágenes que están corriendo.
2. Hace un respaldo inmediato (`backup crear`). Si falla, no despliega nada.
3. Construye y levanta backend e interfaz, y espera su verificación de salud (hasta 240 s, `LOGICODE_ESPERA_SALUD`).
4. Pide `/api/v1/system/info` por la interfaz y comprueba que responda la versión nueva.
5. Si algo falla: vuelve a las imágenes anteriores; si la versión nueva ya había migrado la base, restaura el respaldo del paso 2 (una base migrada no sirve a la versión anterior, T-96). Termina con código 1.

Ensayos del 6 de noviembre (copia de ensayo `g2` en la nube, datos ficticios):

| Ensayo | Qué se rompió a propósito | Qué hizo el script | Tiempo total |
| --- | --- | --- | --- |
| A | Migración `V11` con un error de SQL | El backend no arrancó; la base quedó en el esquema 10 (PostgreSQL deshace la migración); volvió a las imágenes anteriores sin restaurar | 41,1 s |
| B | Migración `V11` correcta y la interfaz con una línea inválida de Nginx | La interfaz no arrancó; la base ya estaba en el esquema 11; restauró el respaldo de 1 s antes y volvió a las imágenes anteriores; la tabla nueva desapareció | 64,4 s |
| C | Nada (mismas imágenes) | Respaldo, despliegue y verificación correctos | 3,5 s |

Salidas completas en la carpeta de evidencias de la guía 11. **Pendiente**: el ensayo en el equipo servidor real, y probarlo con una versión de verdad (la 1.2.0 sobre la 1.1.2).

Lo que hace a mano el script se describe también en el [manual técnico](../docs/manuales/manual-tecnico.md#7-actualizar-a-una-versión-nueva), por si hay que hacerlo paso a paso.
