# Matriz de permisos

> Decisión aprobada en la especificación (sección 2, RF-28, RN-04, RN-15, RN-16, RN-22). **Implementada en la Etapa 3** en `identity/domain/AccessPolicy.java`; cada caso de uso exige el permiso que necesita. Las acciones de etapas futuras (actividades, notas, foro, biblioteca, respaldos) ya tienen su permiso definido y se aplicarán al construir esos endpoints.

## Modelo

- **Roles** (RBAC): `STUDENT`, `TEACHER`, `ADMIN`. El visitante es quien no tiene sesión.
- **Estado de la cuenta**: `PENDING_APPROVAL`, `ACTIVE`, `DISABLED`. Solo una cuenta `ACTIVE` tiene permisos.
- **Privilegio completo** (`full_privileges`): marca adicional, no un rol. Solo la tienen las dos cuentas iniciales: Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi. Sus datos se cargan desde `.env` (D-03); ningún endpoint puede otorgarla.
- Toda comprobación se hace en el backend, en los casos de uso (`application`). La interfaz solo oculta opciones por comodidad.

## Matriz

| Acción | Visitante | Estudiante | Docente | Admin ordinario | Admin con privilegio completo |
| --- | --- | --- | --- | --- | --- |
| Ver inicio, biblioteca pública y leer foro | Sí | Sí | Sí | Sí | Sí |
| Ver contenido privado del curso | No | Si es miembro | Su curso | Sí | Sí |
| Participar en el foro | No | Sí | Sí | Sí | Sí |
| Fijar, cerrar o eliminar hilos y mensajes | No | No | Sí | Sí | Sí |
| Responder actividades y quizzes | No | Sí | No | **No** | Sí |
| Crear temas, materiales y actividades | No | No | Sí | **No** | Sí |
| Calificar o ajustar notas | No | No | Sí | **No** | Sí |
| Consultar entregas y calificaciones | No | Las propias | Su curso | Sí (solo lectura) | Sí |
| Habilitar cédulas / cargar estudiantes | No | No | Sí | Sí | Sí |
| Aprobar docentes | No | No | No | Sí | Sí |
| Gestionar usuarios (tareas ordinarias) | No | No | No | Sí | Sí |
| Publicar/administrar biblioteca | No | No | Su contenido | Sí | Sí |
| Administrar respaldos | No | No | No | Sí | Sí |
| **Cambiar el rol de cualquier cuenta** | No | No | No | **No** | Sí |

## Permisos en el código

| Permiso | Estudiante | Docente | Admin ordinario | Admin con privilegio completo |
| --- | --- | --- | --- | --- |
| `VIEW_PRIVATE_COURSE_CONTENT` | Sí | Sí | Sí | Sí |
| `PARTICIPATE_FORUM` | Sí | Sí | Sí | Sí |
| `MODERATE_FORUM` | No | Sí | Sí | Sí |
| `ANSWER_ACTIVITIES` | Sí | No | No | Sí |
| `MANAGE_COURSE_CONTENT` | No | Sí | No | Sí |
| `GRADE` | No | Sí | No | Sí |
| `VIEW_COURSE_GRADES` | No | Sí | Sí | Sí |
| `MANAGE_STUDENT_ENABLEMENTS` | No | Sí | Sí | Sí |
| `APPROVE_TEACHERS` | No | No | Sí | Sí |
| `MANAGE_USERS` | No | No | Sí | Sí |
| `MANAGE_LIBRARY` | No | No (publica su contenido con `MANAGE_COURSE_CONTENT`) | Sí | Sí |
| `MANAGE_BACKUPS` | No | No | Sí | Sí |
| `VIEW_AUDIT` | No | No | Sí | Sí |
| `CHANGE_ROLES` | No | No | No | Sí |

Notas:

- "Habilitar cédulas" y "gestionar usuarios" para el administrador ordinario se deducen de "gestionar usuarios y tareas administrativas permitidas"; confirmar en la revisión de la Etapa 3.
- Un administrador ordinario no puede desactivar ni cambiar a una cuenta con privilegio completo.
- Etapa 4: "Su curso" significa el curso del que la cuenta es docente responsable. "Si es miembro" significa que su cédula está inscrita en el curso. "Publicar/administrar biblioteca" para el administrador ordinario es cambiar la visibilidad y archivar materiales; crear o editar temas y materiales sigue siendo solo del docente y del privilegio completo (T-28).
- Inscribir estudiantes en un curso: su docente responsable y las cuentas Administrador.
- Etapa 5: crear, editar, publicar o archivar actividades, definir la rúbrica y dar excepciones de plazo es del docente responsable y del privilegio completo. Calificar y corregir notas también (`GRADE`). Las cuentas Administrador ven actividades, entregas, la bandeja Por revisar, notas e historial en solo lectura (`VIEW_COURSE_GRADES`). Entregar exige `ANSWER_ACTIVITIES` y estar inscrito en el curso. El estudiante ve sus entregas y su nota cuando está publicada, nunca la respuesta esperada de la rúbrica (T-36).
- Etapa 6: pedir que la IA vuelva a evaluar una entrega es parte de calificar (`GRADE`): docente responsable y privilegio completo. Las cuentas Administrador ven motivo, confianza y evaluaciones de la IA en solo lectura. El estudiante ve "Calificando con IA" o "En revisión" y su nota publicada, nunca el motivo ni la propuesta de la IA (T-47).
- Etapa 7: el banco de preguntas, armar un quiz y los casos de prueba son del docente responsable y del privilegio completo (`MANAGE_COURSE_CONTENT`). Las cuentas Administrador ven el banco, la clave, los intentos y el ranking con enlace a cada intento, y pueden volver a correr los casos de prueba de una entrega, en solo lectura. Responder un quiz exige `ANSWER_ACTIVITIES` y estar inscrito; el estudiante ve el ranking del quiz con nombres y notas, pero no los intentos de otros, y la clave solo en la revisión permitida (T-58). El editor de PSeInt pide sesión; los juegos son públicos.
- Cambiar el rol o desactivar una cuenta cierra sus sesiones abiertas.
- RN-16: el backend rechaza cualquier operación que deje la plataforma sin al menos una cuenta inicial activa con privilegio completo.

## Criterios de aceptación de la Etapa 3 (de la especificación)

1. Solo las dos cuentas iniciales pueden cambiar roles y tienen todos los permisos.
2. Un administrador ordinario recibe `403` al intentar cambiar roles, responder actividades, crear temas/materiales/actividades, calificar o ajustar notas.
3. No es posible desactivar o retirar el privilegio completo de la última cuenta inicial activa.
