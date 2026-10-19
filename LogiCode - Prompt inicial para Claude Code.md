# Prompt inicial para Claude Code — LogiCode

Actúa como arquitecto de software y desarrollador principal del proyecto LogiCode. Vamos a iniciar el desarrollo desde cero. No supongas que existe código terminado ni presentes requerimientos como funcionalidades ya construidas.

## Lectura obligatoria

Antes de crear o cambiar archivos, lee por completo `LogiCode - Especificacion para Claude Code.md`. Ese archivo contiene los requerimientos funcionales y no funcionales, reglas de negocio, arquitectura, decisiones aprobadas y etapas. El Word `LogiCode - Requerimientos y reglas de negocio.docx` es la versión formal de referencia si está disponible.

## Objetivo de esta primera intervención

1. Inspecciona el repositorio y resume su estado actual. No sobrescribas archivos existentes sin revisarlos.
2. Contrasta el repositorio con la especificación. Señala en una lista corta las decisiones ya cerradas y cualquier contradicción que realmente impida construir la base.
3. Trabaja únicamente en la Etapa 1 (diseño técnico mínimo) y Etapa 2 (fundaciones técnicas): estructura inicial del frontend React, backend Java con Spring Boot organizado como monolito modular y en capas, API REST JSON con prefijo `/api/v1`, PostgreSQL, migraciones versionadas y Docker Compose para ejecución local.
4. Deja un `README.md` con requisitos previos, configuración local, variables de entorno de ejemplo sin secretos, comandos para iniciar y estructura del proyecto. Incluye archivos de diseño/API que hagan falta para que la siguiente etapa pueda comenzar ordenadamente.
5. Al terminar, resume los archivos creados, decisiones de estructura tomadas y el siguiente paso. No avances a autenticación, carga de archivos, calificación IA ni módulos académicos hasta cerrar la fundación y explicar qué está listo.

## Decisiones que debes respetar

- Frontend: React. API: REST. Backend: Java con Spring Boot. Base de datos: PostgreSQL. Despliegue local y futuro: Docker; por ahora el desarrollo y la presentación son locales. El despliegue en línea permanece en observación.
- Arquitectura: monolito modular, capas API, aplicación, dominio e infraestructura; separar responsabilidades y mantener las reglas del negocio fuera de controladores y componentes visuales. Procesos largos como calificación IA se ejecutarán en segundo plano cuando llegue esa etapa.
- No agregues microservicios, Kubernetes, proveedores cloud ni tecnologías no aprobadas salvo que documentes por qué una decisión es imprescindible y la presentes antes de implementarla.
- Los administradores iniciales con permisos completos son Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi. Las demás cuentas Administrador no pueden cambiar roles, responder actividades, crear temas/materiales/actividades, calificar ni ajustar notas.
- La recuperación usa Brevo para correo transaccional. El token lo crea y valida LogiCode, vence en 60 minutos y solo se usa una vez. Solo se recupera mediante correo verificado. Máximo dos solicitudes por cuenta por día calendario en Colombia. Al cambiar contraseña se cierran todas las sesiones.
- Para recuperar el acceso al correo, se comparan el último correo registrado y la contraseña actual o inmediatamente anterior. En PostgreSQL conserva únicamente los hashes seguros de ambas, nunca texto plano; al cambiar contraseña rota los hashes y elimina el más antiguo. Un correo nuevo debe verificarse antes de reemplazar al actual.
- Contraseña: mínimo 8 caracteres, por lo menos un número y un carácter especial.
- IA aprobada para el proyecto: Ollama + Qwen2.5-VL 7B ejecutado en Docker. Las pruebas de aceptación también están aprobadas y no son una condición para elegirlo. Regla funcional: nota automática con contenido legible y confianza reportada de 80 % o más; foto ilegible o confianza inferior pasa a confirmación docente. Un fallo técnico no genera nota. No inventes resultados de pruebas que no se hayan ejecutado.
- Archivos: máximo 500 MB, videos hasta 1 GB y retención mínima de seis meses. La biblioteca debe permitir previsualización en página para formatos compatibles y descarga con permisos.
- Nota de 0,0 a 5,0; actividad no entregada al vencimiento = 0,0 salvo reapertura por el docente. Mostrar nota definitiva aparte del avance parcial.
- Juegos de bloques incluirán secuencias, rutas, ciclos, condiciones y depuración, con retos inspirados en Scratch.

## Reglas de trabajo

- Sigue el orden y puntos de control del plan de etapas de la especificación. Una etapa no implica que las etapas posteriores estén implementadas.
- Usa nombres claros, configuración por entorno y secretos fuera del repositorio. No guardes contraseñas en texto plano.
- No cambies decisiones confirmadas en la especificación. Si encuentras una incompatibilidad técnica concreta, explica el problema y ofrece la alternativa más pequeña antes de cambiar el alcance.
- Mantén la documentación actualizada con cada cambio y distingue “implementado”, “pendiente” y “decisión aprobada”.
- En esta intervención, detente al terminar la base de la Etapa 2 y espera instrucciones para la siguiente etapa.
