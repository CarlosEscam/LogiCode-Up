# Pruebas de aceptación de la calificación con IA (Etapa 6)

La especificación aprueba Ollama + Qwen2.5-VL 7B Q4_K_M y pide **registrar** en el equipo de referencia (Ryzen 7 5700U, 32 GB, Docker): lectura de símbolos, coincidencia con la nota docente, confianza, falsos automáticos (publicados con 80 % o más pero equivocados), detección de ilegibilidad, latencia y RAM máxima. Estas mediciones no condicionan la elección del modelo.

**Estado: pendiente de ejecutar en el equipo de referencia.** El entorno donde se construyó la Etapa 6 no puede descargar el modelo, así que aquí no hay resultados de Qwen. `ejecutar-pruebas.mjs` sí se ensayó de punta a punta con un modelo de texto pequeño solo para comprobar el arnés; esos números no dicen nada sobre Qwen y no se guardaron.

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
