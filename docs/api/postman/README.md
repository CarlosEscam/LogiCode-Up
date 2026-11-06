# Colección de Postman de LogiCode

[LogiCode.postman_collection.json](LogiCode.postman_collection.json) recorre la API como lo hace un estudiante (guía 6, T-105): información pública, 401 sin sesión, token CSRF, inicio de sesión sin y con CSRF, cursos, actividades del curso (el endpoint crítico, con límite de 500 ms), un curso que no existe (404), cierre de sesión y que la sesión ya no sirva. Son 10 solicitudes y 21 verificaciones.

Usa la cuenta ficticia de estudiante de [datos-demo](../../../datos-demo/README.md) (`1000000205`). **Nunca** contra el piloto: ahí no existen los datos de demostración.

## Con Postman

Importar el archivo, cambiar la variable `base` si LogiCode no está en `http://localhost:3000` y correr la colección con el Runner.

## Con Newman (sin instalar nada)

```
npx -y newman@6.2.1 run docs/api/postman/LogiCode.postman_collection.json --env-var base=http://localhost:3000/api/v1
```

Resultado del 6 de noviembre de 2026 (copia de ensayo en la nube, versión 1.2.0 en preparación): 10 solicitudes, 21 verificaciones, 0 fallas, tiempo promedio de respuesta 78 ms.

Un detalle que la colección deja escrito: la cabecera `X-XSRF-TOKEN` debe llevar el valor de la **cookie** `XSRF-TOKEN`, como hace la interfaz. La primera versión de la colección usó el `token` del cuerpo de `/auth/csrf` y el inicio de sesión respondió 403 (en modo SPA, Spring Security compara la cabecera con la cookie).
