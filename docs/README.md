# Documentación de LogiCode

Mapa de toda la documentación del proyecto: qué documento leer según lo que se necesite. Todos los documentos están en español y se revisaron contra el código el 2 de noviembre de 2026 (commit `85dca33`, con MinIO, Brevo, el ensayo de la IA y la revisión de dependencias; base de datos en la versión 8).

En todos los documentos se usan tres palabras con un sentido fijo:

- **Implementado**: existe en el código y tiene prueba o verificación.
- **Pendiente**: no existe todavía o no se ha ejecutado.
- **Decisión aprobada**: está definido en la especificación o lo aprobó el equipo, aunque no implique código.

## Por dónde empezar, según quién lee

### Directivo, jurado o evaluador

| Documento | Para qué |
| --- | --- |
| [Visión general](vision-general.md) | Qué es LogiCode, qué problema resuelve, quién lo usa, cómo fluye un curso, tecnología y estado en una página, glosario |
| [Estado del proyecto](estado.md) | Qué está implementado, qué falta y cómo se verificó cada etapa |
| [Casos de uso](casos-de-uso.md) | Actores y casos de uso principales con su estado |
| [Historias de usuario](historias-de-usuario.md) | Historias por épica con criterios de aceptación Dado/Cuando/Entonces |
| [Maquetas](maquetas.md) | Las 18 pantallas principales en baja fidelidad (PC y celular), con sus casos de uso e historias |
| [Decisiones](decisiones.md) | Decisiones aprobadas, decisiones técnicas tomadas y preguntas abiertas |
| [Especificación](../LogiCode%20-%20Especificacion%20para%20Claude%20Code.md) | Requisitos (RF), no funcionales (RNF), reglas de negocio (RN) y etapas: la fuente de verdad |
| [Piloto](piloto.md) | Plan del piloto, registro de observaciones y acta de aceptación |

### Docente

| Documento | Para qué |
| --- | --- |
| [Manual del docente](manuales/manual-docente.md) | Crear el curso, inscribir estudiantes (también desde Excel), temas, actividades con rúbrica, retos de bloques, quizzes, calificar, planilla y foro |
| [Visión general](vision-general.md) | Recorrido de un curso de principio a fin y glosario |
| [Datos de demostración](../datos-demo/README.md) | Cuentas ficticias para probar la plataforma antes del piloto |

### Estudiante

| Documento | Para qué |
| --- | --- |
| [Manual del estudiante](manuales/manual-estudiante.md) | Registrarse, entregar actividades, quizzes, retos de bloques, juegos, foro y notas |

### Administrador de la plataforma

| Documento | Para qué |
| --- | --- |
| [Manual del administrador](manuales/manual-administrador.md) | Panel de administración, usuarios y roles, aprobación de docentes, biblioteca, foro, respaldos; diferencias entre cuentas iniciales y demás cuentas Administrador |
| [Permisos](permisos.md) | Matriz de roles y permisos tal como la aplica el backend |
| [Respaldos](../respaldos/README.md) | Cómo funcionan las copias diarias, cómo sacarlas del equipo y cómo restaurar |

### Desarrollador o responsable técnico

| Documento | Para qué |
| --- | --- |
| [Instalación](instalacion.md) | Instalar desde cero en Windows o Linux, primer ingreso, datos demo, IA, Brevo, piloto, respaldos, problemas frecuentes y pasar LogiCode a otro PC |
| [Manual técnico](manuales/manual-tecnico.md) | Operación diaria, actualización, seguridad y problemas frecuentes del equipo servidor |
| [README del repositorio](../README.md) | Variables de entorno, comandos de inicio, desarrollo con recarga rápida, pruebas y estructura |
| [Arquitectura](arquitectura.md) | Monolito modular, módulos y sus reglas, procesos en segundo plano, despliegue, seguridad y frontend |
| [Modelo de datos](modelo-de-datos.md) | Tablas de las migraciones `V1` a `V8` con diagramas entidad-relación |
| [API REST](api/README.md) | Convenciones, formato de errores y todos los endpoints de `/api/v1` |
| [Calidad y seguridad](calidad.md) | Defectos, riesgos aceptados, revisión de seguridad, observabilidad y demostraciones |
| [Piloto](piloto.md) | Modo HTTPS para usar LogiCode desde otros equipos y celulares; acceso temporal desde fuera de la red para demostraciones (ngrok) |
| [Pruebas de aceptación de la IA](../ia-pruebas/README.md) | Arnés para medir Qwen2.5-VL en el equipo de referencia y resultados del ensayo del 1 de noviembre (con fotos reales: **Pendiente**) |
| [Migración entre equipos](../migracion/Guia-pasar-LogiCode-a-otro-PC.md) | Llevar una instalación con sus datos a otro PC (`exportar.ps1` e `importar.ps1`) |
| [CLAUDE.md](../CLAUDE.md) | Reglas para el asistente de desarrollo |
| [Documento único](documento-completo/generar.py) | Genera `LogiCode-Documentacion-completa.docx` y `.pdf` con todos estos documentos, índice y anexos: `python3 docs/documento-completo/generar.py <carpeta>` (necesita pandoc, LibreOffice y mermaid-cli) |

## Diagramas

Las fuentes Mermaid y sus imágenes están en [diagramas/](diagramas/). Vienen del análisis de la Etapa 6 ("Entregar y calificar actividades"); los que contradecían el código se corrigieron y se explica en el documento que los usa.

| Diagrama | Dónde se explica |
| --- | --- |
| [DFD de contexto](diagramas/1-dfd-contexto.mmd) y [DFD de nivel 1](diagramas/2-dfd-nivel1.mmd) | [Arquitectura](arquitectura.md#flujo-de-datos-de-entregar-y-calificar) y [Visión general](vision-general.md) |
| [Modelo de datos del flujo](diagramas/3-modelo-datos.mmd) | [Modelo de datos](modelo-de-datos.md#vista-de-conjunto) |
| [Secuencia: entregar y calificar con IA](diagramas/4-secuencia.mmd) | [Arquitectura](arquitectura.md#cola-de-calificación-con-ia) y [Casos de uso](casos-de-uso.md) |
| [Estados de una entrega](diagramas/5-estados.mmd) | [Casos de uso](casos-de-uso.md#estados-de-una-entrega) |

Además, cada documento técnico tiene sus propios diagramas Mermaid (módulos, despliegue, entidad-relación por grupo de tablas), que GitHub dibuja directamente.

## Documentos de la Etapa 1 que siguen pendientes

Casos de uso, historias de usuario y maquetas están entregados. Las rúbricas de referencia del docente y la prueba de lectura de código manuscrito (pruebas de aceptación de Qwen2.5-VL) siguen **Pendientes**. Detalle en [estado.md](estado.md).
