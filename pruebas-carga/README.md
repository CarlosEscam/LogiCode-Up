# Prueba de carga del endpoint crítico (k6)

[actividades.js](actividades.js) mide `GET /api/v1/courses/{id}/activities`, la consulta que abre cada estudiante antes del cierre de una actividad (guía 8, T-101).

| Parámetro | Valor | Por qué |
| --- | --- | --- |
| Usuarios virtuales | Sube a 50 en 10 s, se queda 30 s, baja en 10 s | Un curso tiene unos 30 estudiantes; 50 usuarios que consultan cada segundo son más que todo el curso a la vez |
| Umbrales | p95 < 500 ms, errores < 1 %, verificaciones > 99 % | 500 ms es el límite de "se siente inmediato" para una lista; el 1 % es el presupuesto de error del SLO ([observabilidad.md](../docs/observabilidad.md)) |
| Destino | Solo `localhost` o `127.0.0.1` | Nunca contra el piloto ni contra internet. Para otro destino hay que escribir `LOGICODE_CARGA_PERMITIDA=si` a propósito |
| Cuenta | Estudiante de demostración `1000000205` (datos-demo) | Ficticia. Se cambia con `LOGICODE_CEDULA` y `LOGICODE_CLAVE` |

## Cómo correrla

Con LogiCode corriendo en el equipo y los datos de demostración cargados ([datos-demo/README.md](../datos-demo/README.md)). k6 no se instala: corre en Docker.

PowerShell (Windows):

```
docker run --rm --network host -v "${PWD}\pruebas-carga:/s" -e LOGICODE_URL=http://localhost:3000 grafana/k6:1.4.2 run /s/actividades.js
```

Linux:

```
docker run --rm --network host -v "$PWD/pruebas-carga:/s" -e LOGICODE_URL=http://localhost:3000 grafana/k6:1.4.2 run /s/actividades.js
```

En Docker Desktop para Windows `--network host` puede no llegar al puerto del equipo; en ese caso usa `-e LOGICODE_URL=http://host.docker.internal:3000` y `LOGICODE_CARGA_PERMITIDA=si` (sigue siendo tu propio equipo). k6 termina con código distinto de 0 si algún umbral falla.

## Resultados medidos

| Fecha | Dónde | Versión | Solicitudes | p95 | Promedio | Máximo | Errores | Umbrales |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 6 nov 2026 | Copia de ensayo en la nube (Docker, datos de demostración, trazas al 100 %) | 1.2.0 en preparación | 2003 (39,2/s) | 31,14 ms | 13,82 ms | 339 ms | 0 % | Todos pasan |
| 6 nov 2026 | La misma, repetida después de agregar el balde de 1 s | 1.2.0 en preparación | 2003 (39,1/s) | 26,37 ms | 12,83 ms | 468 ms | 0 % | Todos pasan |

**Pendiente**: correrla en el equipo servidor real del piloto (el PC de Carlos), que es más lento que la máquina de la nube, y anotar aquí el resultado.

## Límites conocidos

- Los 50 usuarios comparten una sola sesión. Mide el endpoint y la base, no el inicio de sesión ni 50 sesiones distintas en `spring_session`.
- El curso de demostración tiene pocas actividades. Con un curso real de un semestre la respuesta es más grande; repetir la prueba cuando haya datos del piloto.
