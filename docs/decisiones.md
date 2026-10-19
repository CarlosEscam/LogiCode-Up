# Registro de decisiones

Tres categorías, para no confundir lo aprobado con lo propuesto:

- **Decisión aprobada**: viene de la especificación. No se cambia sin un cambio de alcance registrado.
- **Decisión técnica tomada (Etapa 2)**: elección de implementación hecha al construir la fundación, dentro de lo aprobado. Revisable.
- **Pendiente de decidir**: se necesita una respuesta antes de la etapa indicada.

## Decisiones aprobadas (especificación)

| Tema | Decisión |
| --- | --- |
| Tecnologías | React · API REST JSON · Java con Spring Boot · PostgreSQL · Docker |
| Arquitectura | Monolito modular con capas api, application, domain, infrastructure; sin microservicios ni Kubernetes |
| Despliegue | Desarrollo, pruebas y presentación en local con Docker Compose; publicación en línea en observación |
| Administradores con privilegios completos | Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi; las demás cuentas Administrador no cambian roles, no responden actividades, no crean temas/materiales/actividades, no califican ni ajustan notas |
| Contraseñas | Mínimo 8 caracteres, al menos un número y un carácter especial; BCrypt o Argon2id; solo hash actual e inmediatamente anterior |
| Recuperación | Solo con correo verificado; token generado y validado por LogiCode, aleatorio, un uso, 60 minutos; Brevo solo envía; máximo 2 solicitudes por cuenta por día calendario en Colombia; respuesta genérica; al cambiar contraseña se cierran todas las sesiones |
| Cambio asistido de correo | Último correo registrado + contraseña actual o anterior; el correo nuevo se verifica antes de reemplazar al actual |
| IA | Ollama + Qwen2.5-VL 7B (Q4_K_M) local en Docker; publicación automática con contenido legible y confianza reportada ≥ 80 %; si no, confirmación docente; un fallo técnico no genera nota |
| Privacidad IA | Se envía nombre, archivo, enunciado, rúbrica y escala; nunca cédula ni correo |
| Archivos | 500 MB por archivo, videos hasta 1 GB, retención mínima de seis meses; formatos PDF, DOC/DOCX, PPT/PPTX, PNG/JPG/JPEG, MP4/MOV, PSC, INO, SB3 |
| Notas | Escala 0,0 a 5,0; vencida sin entrega = 0,0 salvo reapertura; definitiva separada del avance parcial |
| Juegos | Bloques con secuencias, rutas, ciclos, condiciones y depuración, inspirados en Scratch |
| Migraciones | Flyway |
| Tareas lentas | Worker Spring Boot + tabla de trabajos en PostgreSQL |

## Decisiones técnicas tomadas en la Etapa 2

| # | Decisión | Motivo |
| --- | --- | --- |
| T-01 | Java 21 (LTS) y Spring Boot 4.1 | Versiones con soporte vigente a la fecha de entrega (dic. 2026). La línea 3.5 de Spring Boot ya no recibe soporte libre |
| T-02 | Maven con wrapper (`mvnw`) | No exige instalar Maven; es el estándar de Spring Boot |
| T-03 | Paquete raíz `co.edu.unipamplona.logicode`; código en inglés, interfaz y documentación en español | Coincide con los ejemplos de nombres de la especificación (ActivityController, SubmitWork) |
| T-04 | React con **JavaScript** (no TypeScript) y Vite como herramienta de compilación | TypeScript no está en las tecnologías aprobadas. Vite es la herramienta de compilación recomendada para React sin framework adicional |
| T-05 | Pruebas: JUnit 5, Testcontainers (PostgreSQL real), ArchUnit; Vitest + Testing Library; lint con oxlint | Solo en pruebas, no afectan la ejecución. ArchUnit hace cumplir las capas aprobadas |
| T-06 | Nginx sirve el frontend y reenvía `/api` | La especificación menciona Nginx como proxy reverso; evita CORS |
| T-07 | PostgreSQL 17 (imagen `postgres:17-alpine`) | Versión estable con soporte largo |
| T-08 | Errores en formato Problem Details (RFC 9457) | Estándar soportado por Spring; formato único para la interfaz |
| T-09 | Fechas en UTC en base de datos; reglas de calendario con `America/Bogota` | La regla de "día calendario en Colombia" necesita una zona explícita |
| T-10 | La contraseña de la base de datos local va en `.env` (ignorado por Git) | Es solo para el entorno local. Las credenciales de Brevo (Etapa 3) se manejarán como secretos de Docker, como pide la especificación |
| T-11 | Base de datos y backend publicados solo en `127.0.0.1`; el frontend en el puerto 3000 | PostgreSQL no se expone a la red. El frontend queda accesible para presentar desde otro equipo de la red local |
| T-12 | Flujo Git: rama `main` estable + ramas `feature/<etapa>-<tema>`; integración por pull request con CI en verde | Ver README |

## Decisiones aprobadas para la Etapa 3 (19 de octubre de 2026)

| # | Decisión | Detalle |
| --- | --- | --- |
| D-01 | Sesiones guardadas en el servidor (PostgreSQL) | Spring Session JDBC con cookie HttpOnly y protección CSRF. Cerrar todas las sesiones = borrar las sesiones de la cuenta. Reemplaza la pregunta P-01 |
| D-02 | Contraseñas con BCrypt | Incluido en Spring Security, sin dependencias adicionales. Reemplaza P-02 |
| D-03 | Cuentas iniciales cargadas desde configuración local | Cédula, nombre y correo de las dos administradoras se leen de variables de entorno (`INITIAL_ADMIN_*` en `.env`, ignorado por Git). Nunca se escriben en el código, en migraciones ni en el repositorio. No se configura contraseña: cada cuenta la define con el flujo de verificación de correo. Reemplaza P-03 |
| D-04 | Límite de 2 solicitudes diarias contado por separado | Un contador para recuperación de contraseña y otro para cambio asistido de correo, por cuenta y por día calendario en Colombia. Reemplaza P-04 |

## Pendiente de decidir

| # | Pregunta | Necesaria antes de | Propuesta |
| --- | --- | --- | --- |
| P-05 | Almacenamiento de archivos en local: MinIO (mencionado en la especificación) o volumen de Docker | Etapa 4 | MinIO en Docker Compose detrás de una interfaz `FileStorage`, para que el despliegue futuro use cualquier proveedor compatible con S3 sin cambiar el dominio |
| P-06 | Dominio y remitente de Brevo | Etapa 3 (envío real de correos) | Lo define el equipo; la API key y el remitente se configuran como secretos de Docker |

## Observaciones de la especificación (no bloquean la base)

1. Sección 6, párrafo inicial: dice "solo las fotos ilegibles solicitarán confirmación docente", mientras RN-09, RN-11 y el flujo indican que también la confianza menor a 80 %. Se aplica la regla de RN-09/RN-11.
2. Tabla de prioridades: P1 y P2 repiten quizzes, PSeInt, juegos de bloques y foro. No afecta las Etapas 1 y 2.
3. Tabla de etapas: la fila de encabezado repite el texto de la Etapa 0 (error de formato).
4. El Word `LogiCode - Requerimientos y reglas de negocio.docx` no está en la carpeta del proyecto; se trabajó con el `.md`.
