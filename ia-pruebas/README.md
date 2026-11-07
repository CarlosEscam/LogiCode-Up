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
- Desde la versión 1.1.0 (T-90) el curso de pruebas empieza en calibración, así que ninguna nota se publica sola: el arnés cuenta como "publicada sola" la que queda en Por revisar con motivo `CALIBRATION`, que es la que la IA habría publicado. Las cifras se comparan igual que antes.
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

## Pruebas sintéticas del 6 y 7 de noviembre de 2026

En el PC de Carlos, con la versión anterior a la 1.2.0 en los contenedores (sin calibración), qwen2.5vl:7b Q4_K_M. Las muestras se generan con los scripts `generar.py` y `generar2.py` de la carpeta del proyecto. Las notas de referencia las puso Claude antes de correr; no son de un docente.

| Prueba | Muestras | Sin fallo | Nota dentro de ±0,5 | Publicadas solas | Publicadas solas con nota equivocada |
| --- | --- | --- | --- | --- | --- |
| 1 (12 casos) | correctas, con errores, trampas, ilegibles, 2 PDF | 12 de 12 | 7 de 10 | 10 | 4 |
| 2 "probando límites" (30 casos) | contenido, calidad de la foto, trampas, PDF de 5 y 6 páginas | 29 de 30 | 20 de 25 | 25 | 6 |

Hallazgos:

- **Copia de la respuesta esperada.** En 3 fotos ilegibles (S09, S10 y L26), la "transcripción" que devolvió el modelo fue la respuesta esperada de la rúbrica, que se le envía en el mensaje. Dos de ellas se publicaron con nota alta. L26 es una foto borrosa de código malo y salió con 5,0. Es el riesgo más serio. Desde la 1.3.0 (T-107), la IA primero lee la foto sin ver la respuesta esperada y después califica lo leído. Falta repetir las pruebas para medir el efecto.
- **La confianza de 80 a 90 no separa aciertos de errores.** En la prueba 2, las 12 notas con confianza 95 estaban bien. En la prueba 1, una ilegible tuvo 95. La regla del 80 % se mantiene (T-40). La calibración (1.1.0) deja estas notas en manos del docente.
- **Lectura.** Lee bien fotos giradas (90° y 180°), con perspectiva, en lápiz claro, en JPEG con calidad 8, sobreexpuestas, de 640 px y en hoja cuadriculada con tinta roja en PNG. Falla con ruido fuerte y con 320 px de ancho.
- **Trampas.** Ignoró las instrucciones escritas para la IA en la hoja. Mandó al docente la tarea equivocada, la hoja en blanco, la foto sin hoja y el PDF de 6 páginas.
- **Tiempos.** Unos 2 minutos por foto, 8 minutos un PDF de 5 páginas con texto y 5 minutos un escaneo de 3 páginas.
- **Error de LogiCode encontrado (L02).** El modelo respondió 1.0000000000000002 sobre 1 punto y `AiGradingPolicy` lo rechazaba como fallo técnico. Corregido en la 1.2.1.

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
