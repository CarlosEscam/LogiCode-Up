# Pruebas de aceptación de la calificación con IA (Etapa 6)

La especificación aprueba Ollama + Qwen2.5-VL 7B Q4_K_M y pide **registrar** en el equipo de referencia (Ryzen 7 5700U, 32 GB, Docker): lectura de símbolos, coincidencia con la nota docente, confianza, falsos automáticos (publicados con 80 % o más pero equivocados), detección de ilegibilidad, latencia y RAM máxima. Estas mediciones no condicionan la elección del modelo.

**Estado: ensayo con imágenes sintéticas hecho (ver abajo); pendiente con fotos manuscritas reales.** El entorno donde se construyó la Etapa 6 no puede descargar el modelo, así que aquí no hay resultados de Qwen. `ejecutar-pruebas.mjs` sí se ensayó de punta a punta con un modelo de texto pequeño solo para comprobar el arnés; esos números no dicen nada sobre Qwen y no se guardaron.

## 1. Preparar las muestras (antes de usar la IA)

1. Reúne fotos de entregas de prueba autorizadas, sin datos personales: pseudocódigo y código a mano, legibles y difíciles, con sombras, rotación, tachones y símbolos ambiguos (0/O, 1/l, =/≠). Incluye algunas ilegibles a propósito.
2. Copia `muestras/muestras.ejemplo.json` como `muestras/muestras.json` y pon las fotos en `muestras/`. Ambos quedan fuera de Git.
3. Por cada actividad: título, enunciado, respuesta esperada y criterios con sus puntos.
4. Por cada muestra, **el docente registra antes de correr la prueba**:
   - `puntosDocente`: puntos por criterio, en el mismo orden de la rúbrica (`null` si es ilegible).
   - `transcripcion`: lo que dice la hoja, exacto, para medir la lectura (`null` si es ilegible).
   - `legibleEsperado` y `condicion` (por ejemplo "sombra y giro", "tachones", "0 y O").

El ejemplo usa las tres imágenes sintéticas de `datos-demo/muestras/`; sirve para ensayar, no reemplaza las fotos reales.

## 2. Encender la IA

En `.env`:

```
LOGICODE_AI_ENABLED=true
```

```powershell
docker compose --profile ia up -d --build --wait
docker compose exec ollama ollama pull qwen2.5vl:7b
docker compose exec ollama ollama show qwen2.5vl:7b
```

`ollama show` debe decir `quantization Q4_K_M`. Si no, descarga la etiqueta Q4_K_M que indique la ficha del modelo y ponla en `LOGICODE_AI_MODEL`. Los datos de demostración deben estar cargados (`node datos-demo/cargar-datos-demo.mjs`): la prueba usa la docente Laura Méndez Ríos y el estudiante Felipe Ortiz Cano.

Cierra otros programas pesados durante la prueba; la RAM y la latencia dependen de ello.

## 3. Ejecutar

```powershell
node ia-pruebas/ejecutar-pruebas.mjs ia-pruebas/muestras/muestras.json
```

El script crea el curso "Pruebas de aceptación IA" y, por cada muestra, una actividad con su rúbrica y una entrega con la foto, que pasa por el flujo real (cola, Ollama, decisión de LogiCode). Antes procesa una muestra de calentamiento que no se mide, porque la primera carga del modelo tarda más. Mide la RAM del contenedor `ollama` con `docker stats` cada 5 s.

Variables opcionales: `LOGICODE_URL`, `IA_DOCENTE`, `IA_ESTUDIANTE`, `IA_CLAVE`, `IA_TOLERANCIA` (0,5 por defecto), `IA_ESPERA_MAX_MIN` (30), `IA_SIN_CALENTAR=1`.

## 4. Resultados

Quedan en `ia-pruebas/resultados/<fecha>/` (fuera de Git hasta que decidas conservarlos):

- `resultados.csv`: una fila por muestra con estado, legibilidad esperada y reportada, confianza, nota docente y de la IA, diferencia, si se publicó sola, si fue un falso automático, exactitud de lectura de caracteres y de símbolos ambiguos, duración del modelo y total, RAM máxima, error y transcripción.
- `resumen.md`: versión de Ollama y del modelo, equipo, y los totales: coincidencia de notas (±0,5), calibración del umbral (coincidencia por encima y por debajo de 80 %), falsos automáticos, detección de ilegibles, latencia (mediana y máxima) y RAM máxima.

Cómo leerlos:

- Un **falso automático** es una nota publicada sin docente que se aleja más de la tolerancia. Es el dato más importante para confiar en el umbral.
- Si la confianza alta no coincide mejor que la baja, el umbral no está calibrado; se registra como hallazgo, no cambia la regla aprobada (T-40).
- `SIN_TERMINAR` significa que no terminó dentro de `IA_ESPERA_MAX_MIN`; `FAILED`, un fallo técnico sin nota.

Para conservar los resultados en el repositorio, copia `resumen.md` a `docs/` o agrégalo a `docs/estado.md`, y quita de las transcripciones cualquier dato personal.

## Ensayo del 1 de noviembre de 2026 (imágenes sintéticas)

En el PC de Carlos (Docker: 16 CPU, 15,6 GB), Ollama 0.35.1, qwen2.5vl:7b Q4_K_M, con `muestras.ejemplo.json`. Números del script:

| Muestra | Legible esperado / IA | Confianza | Nota docente / IA | Publicada sola | Modelo |
| --- | --- | --- | --- | --- | --- |
| E01 legible, sombra y giro leve | sí / sí | 85 | 5 / 4,5 | sí, dentro de la tolerancia | 52 s |
| E02 condición invertida | sí / sí | 80 | 3 / 5 | sí, **falso automático** | 100 s |
| E03 borrosa y oscura | no / **sí** | 80 | — / 1 | sí, debía ir al docente | 85 s |

Lectura de caracteres y de símbolos ambiguos: 100 %. RAM máxima de Ollama: 11,7 GB. Hallazgo: con estas tres imágenes la confianza reportada no separa aciertos de errores. Son sintéticas y pocas, así que no es conclusión; la regla del 80 % se mantiene (T-40) hasta medir con fotos reales.

## Prueba con un PDF real (P-08)

El arnés también acepta muestras `.pdf`: crea la actividad con entrega de documentos y sube el PDF como lo haría un estudiante, así pasa por PDFBox (texto y páginas como imágenes) antes de llegar a Qwen. Los PDF y las subcarpetas de `muestras/` quedan fuera de Git.

**Estado: hecha el 1 de noviembre de 2026 (noche, hora de Colombia) en el PC de Carlos; resultados abajo.** Las muestras están en la carpeta compartida del proyecto (`ia/prueba-pdf/`): un informe KDD del Titanic que aportó Carlos, sin datos personales.

| Muestra | Qué prueba | Resultado esperado |
| --- | --- | --- |
| P02 | Páginas 1 a 3 exportadas de Word (texto + 3 imágenes) | Nota 4,0 (los puntos del docente están en `muestras-pdf.json`) |
| P03 | Las mismas 3 páginas solo como imagen, como un escaneo | Nota 4,0; compara la lectura sin capa de texto |
| P01 | Informe completo de 19 páginas | Por revisar con `UNSUPPORTED`: supera el límite de 5 páginas y no se envía al modelo |

Comprobado en la nube con la clase `PdfContent` real y PDFBox 3.0.8 (sin Qwen): P02 da 6.043 caracteres de texto y 3 imágenes, P03 da 0 caracteres y 3 imágenes, y P01 no se lee.

```powershell
$env:IA_ESPERA_MAX_MIN = "60"
node ia-pruebas/ejecutar-pruebas.mjs ia-pruebas/muestras/prueba-pdf/muestras-pdf.json
```

Con 3 páginas por entrega cada respuesta puede tardar varios minutos. Si una muestra termina en `FAILED` porque el modelo no respondió a tiempo, es un hallazgo de la prueba; se puede subir `LOGICODE_AI_TIMEOUT` (10m por defecto) en `.env` y repetir.

### Resultados (1 de noviembre de 2026)

PC de Carlos (Docker: 16 CPU, 15,6 GB), Ollama 0.35.1, qwen2.5vl:7b Q4_K_M. Números del script:

| Muestra | Resultado | Confianza | Nota docente / IA | Lectura (caracteres / símbolos) | Modelo | RAM máx. Ollama |
| --- | --- | --- | --- | --- | --- | --- |
| P02 PDF de Word, 3 páginas | Publicada sola | 90 | 4 / 4 | 99,9 % / 100 % | 522 s | 8,1 GB |
| P03 mismas páginas como escaneo | Publicada sola | 90 | 4 / 4 | 74,5 % / 63,1 % | 463 s | 8,6 GB |
| P01 informe de 19 páginas | Por revisar, `UNSUPPORTED` (no pasó por el modelo) | — | 4 / — | — | — | — |

Hallazgos:

- La IA lee y califica PDF reales: las dos notas coinciden con la del docente, incluido el criterio que el informe no cumple (referencias con autor y año, 0 puntos). No hubo falsos automáticos ni fallos técnicos.
- Sin capa de texto (escaneo) la nota fue igual, pero la transcripción es peor (74,5 % de caracteres). Con texto de Word la lectura es casi exacta porque el modelo recibe el texto extraído por PDFBox.
- Cada PDF de 3 páginas tarda 8 a 9 minutos, cerca del límite de 10 (`LOGICODE_AI_TIMEOUT`). Un PDF de 4 o 5 páginas podría pasarse; conviene medirlo antes del piloto o subir el límite.
- La regla de 5 páginas funciona: el informe completo quedó en Por revisar sin nota.
- Son dos muestras de un solo documento; no prueba la calibración de la confianza (T-40).
