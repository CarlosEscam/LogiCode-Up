# LogiCode — Especificación base para desarrollo

> Documento de trabajo para Claude Code. Fuente de verdad: el informe Word de requerimientos y reglas de negocio del mismo nombre. El proyecto aún no está implementado; los requisitos describen trabajo pendiente.

## Cómo usar esta especificación

Leer el documento completo antes de modificar código. Las decisiones confirmadas se implementan como están descritas; los elementos marcados como pendientes no deben resolverse por suposición. El desarrollo se hace por etapas y cada entrega debe indicar qué quedó hecho y cómo continuar.

Requerimientos, reglas de negocio y arquitectura

Plataforma web de Pensamiento Computacional · Universidad de Pamplona · Fecha objetivo de entrega: 10 de diciembre de 2026

Este documento establece la línea base para iniciar LogiCode. El proyecto aún no se ha desarrollado: todos los requerimientos, reglas y etapas aquí descritos son trabajo por realizar. El documento de proyecto anterior se usa como borrador de ideas, no como evidencia de funcionalidades construidas. La arquitectura aplica las capas de la guía compartida y conserva las tecnologías definidas por el equipo.

| Dato | Definición inicial |
| --- | --- |
| Producto | LogiCode |
| Asignatura | Pensamiento Computacional, primer semestre |
| Institución | Universidad de Pamplona |
| Frontend | React |
| API | REST |
| Backend | Java con Spring Boot |
| Base de datos | PostgreSQL |
| Despliegue | Docker |
| Fecha objetivo de entrega | 10 de diciembre de 2026 |
| Administradores iniciales | Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi |

## 1. Propósito y objetivos
### Problema
Los materiales, tareas y notas de Pensamiento Computacional suelen quedar repartidos entre correos, chats y archivos. Quienes inician la universidad necesitan repasar algoritmos y practicar con orientación. El docente requiere una forma organizada de publicar contenidos, recibir entregas y agilizar una parte de la calificación sin perder control pedagógico.

### Propósito
LogiCode reunirá contenidos por tema, biblioteca, actividades, entregas, calificaciones, quizzes, juegos y foro. Una inteligencia artificial multimodal apoyará la evaluación de entregas en fotos y archivos mediante rúbricas y respuestas esperadas. Las calificaciones quedarán explicadas y sujetas a revisión docente según el tipo de actividad y la confianza del análisis.

### Objetivo general
Diseñar y desarrollar una plataforma web adaptable que apoye la enseñanza, práctica y evaluación del pensamiento computacional para estudiantes de primer semestre.

### Objetivos específicos
Organizar temas, recursos y actividades en un espacio común del curso.

Permitir entregar y consultar tareas, notas y retroalimentación.

Aplicar pruebas deterministas cuando la respuesta se puede ejecutar o comparar; integrar IA para analizar fotos y archivos con una rúbrica explícita.

Ofrecer biblioteca pública, foro y juegos de programación por bloques.

Documentar el diseño, la construcción, el despliegue y el uso del sistema.

## 2. Alcance y usuarios
La primera versión será una aplicación web responsive para un curso y un docente. El diseño de datos permitirá agregar cursos y periodos en el futuro. Los contenidos académicos iniciales cubrirán PSeInt, diagramas de flujo (DFD), Scratch y Arduino.

| Rol | Permisos |
| --- | --- |
| Visitante | Consultar inicio, material público de biblioteca y leer el foro. No puede participar ni consultar información privada. |
| Estudiante | Consultar contenido público y privado del curso, entregar actividades, presentar quizzes, usar juegos, participar en el foro y consultar sus notas y comentarios. |
| Docente | Administrar su curso y su contenido, habilitar estudiantes, crear actividades, revisar y calificar entregas, consultar notas y moderar el foro. |
| Administrador | Administrador (cuentas ordinarias): gestionar usuarios y tareas administrativas permitidas, consultar entregas y calificaciones, aprobar docentes, publicar biblioteca, moderar el foro y administrar respaldos. No puede responder actividades, crear temas/materiales/actividades, calificar ni ajustar notas, ni cambiar roles. Solo las dos cuentas iniciales de Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi tienen permisos completos y pueden cambiar el rol de cualquier cuenta. |

El administrador puede consultar entregas y calificaciones conforme a la regla de permisos anterior, pero no puede asignarlas ni modificarlas. Esta definición reemplaza la restricción del borrador que impedía al administrador ver notas y entregas.

## 3. Requerimientos funcionales
Los códigos RF se utilizarán para relacionar requisitos, historias de usuario, diseño y criterios de aceptación. El estado inicial de todos es Pendiente de construir.

| Código | Requerimiento |
| --- | --- |
| RF-01 | El sistema permitirá al estudiante registrarse con una cédula numérica habilitada, contraseña y correo. |
| RF-02 | El sistema permitirá solicitar una cuenta docente, que permanecerá pendiente hasta aprobación del administrador. |
| RF-03 | El sistema permitirá iniciar y cerrar sesión con cédula y contraseña. |
| RF-04 | El sistema permitirá solicitar el restablecimiento de contraseña usando únicamente un correo previamente verificado y enviará a ese correo, mediante Brevo, un enlace seguro de un solo uso con vigencia de 60 minutos. |
| RF-05 | El docente podrá registrar estudiantes de forma individual o cargar una lista desde CSV o Excel. |
| RF-06 | El docente podrá crear, ordenar y editar temas, y asociar cada uno con PSeInt, DFD, Scratch, Arduino o General. |
| RF-07 | El docente podrá asociar a cada tema guías, documentos, presentaciones, imágenes, videos cargados y enlaces externos. |
| RF-08 | El docente podrá marcar cada recurso como público o privado. |
| RF-09 | El docente podrá marcar el tema de la próxima clase. |
| RF-10 | El docente podrá crear tareas, talleres, evaluaciones, quizzes calificables y quizzes de repaso. |
| RF-11 | El docente podrá definir apertura, cierre, porcentaje, tipo de entrega y ajustes individuales de plazo o intentos. |
| RF-12 | El docente podrá configurar casos de prueba, respuestas esperadas, criterios de bloques o rúbricas para una actividad. |
| RF-13 | El sistema recibirá entregas por texto, archivo, fotografía o imagen y mostrará su estado de procesamiento y revisión. |
| RF-14 | El sistema calificará con IA las entregas compatibles en foto o archivo usando el contenido, el nombre del estudiante, el enunciado, la rúbrica y la escala. Publicará automáticamente la nota y la retroalimentación cuando el contenido sea legible y la confianza reportada por el modelo sea igual o superior al 80 %. Si la foto es ilegible o la confianza queda por debajo del umbral, dejará la entrega pendiente de confirmación docente. |
| RF-15 | Cuando una foto sea ilegible o la confianza de la evaluación sea inferior al 80 %, el sistema dejará la entrega en Por revisar y solicitará confirmación docente. Si un archivo no se puede procesar por un error técnico, conservará la entrega, mostrará el error y permitirá reintentar; no inventará ni publicará una nota. |
| RF-16 | Para PSeInt y quizzes, el sistema ejecutará o comparará respuestas de forma determinista cuando existan casos de prueba y claves configuradas. |
| RF-17 | El docente podrá consultar las entregas, el resultado de IA y el historial de cambios de nota. |
| RF-18 | El docente podrá consultar y exportar la planilla del curso a Excel. |
| RF-19 | El estudiante podrá ver actividades pendientes, entregadas, calificadas y en revisión, con fechas y estados. |
| RF-20 | El estudiante podrá consultar sus notas y la retroalimentación asociada. |
| RF-21 | El sistema permitirá realizar quizzes de repaso sin nota y consultar la revisión de respuestas. |
| RF-22 | La plataforma ofrecerá juegos didácticos de pensamiento computacional, incluidos retos de bloques que utilicen secuencias, ciclos, condiciones, rutas y depuración. |
| RF-23 | La página de inicio presentará misión, visión y explicación de la plataforma. |
| RF-24 | La biblioteca pública organizará recursos por tema y herramienta, con búsqueda o filtros. |
| RF-25 | La biblioteca permitirá previsualizar dentro de la página PDF, imágenes y videos compatibles y descargar recursos cargados cuando el formato y los permisos lo permitan. |
| RF-26 | El foro organizará hilos en PSeInt, DFD, Scratch, Arduino y General; permitirá lectura pública y escritura con sesión. |
| RF-27 | Docente y administrador podrán fijar, cerrar o eliminar hilos y mensajes. |
| RF-28 | Las cuentas iniciales con privilegios completos podrán gestionar cualquier usuario y rol, aprobar docentes, administrar biblioteca, moderar el foro, responder actividades, crear temas/materiales/actividades, consultar y modificar calificaciones y administrar respaldos. Las demás cuentas Administrador podrán gestionar usuarios y tareas administrativas ordinarias, pero no responder actividades, crear temas/materiales/actividades, calificar o ajustar notas, ni cambiar roles. |
| RF-29 | El sistema verificará la titularidad del correo mediante un enlace de confirmación de un solo uso enviado por Brevo antes de habilitar la recuperación de contraseña para esa cuenta. |
| RF-30 | Si un usuario pierde acceso al correo vinculado, podrá solicitar el cambio ingresando el último correo registrado y la contraseña actual o la inmediatamente anterior que recuerde. El sistema comparará el correo y verificará la contraseña contra los hashes almacenados; si ambos datos coinciden, permitirá registrar un correo nuevo pendiente de verificación. |

## 4. Requerimientos no funcionales
| Código | Criterio |
| --- | --- |
| RNF-01 Usabilidad | Interfaz en español, adaptable a celular y comprensible para estudiantes de primer semestre. |
| RNF-02 Seguridad | Contraseñas protegidas con BCrypt o Argon2id; conservar en PostgreSQL únicamente el hash de la contraseña actual y el hash de la inmediatamente anterior, nunca las contraseñas en texto plano. Al cambiarla, el hash actual pasa a anterior y el nuevo hash ocupa el campo actual; descartar cualquier hash anterior más antiguo. Exigir al menos 8 caracteres, incluyendo como mínimo un número y un carácter especial. Tras cambiar la contraseña, cerrar todas las sesiones activas. HTTPS en despliegue; autenticación y autorización en backend. |
| RNF-03 Privacidad | Los datos enviados al modelo local para evaluar incluirán el nombre del estudiante y el archivo, junto con el enunciado, la rúbrica y la escala que requiere la evaluación. No se enviarán cédula ni correo del estudiante al modelo. |
| RNF-04 Seguridad de archivos | Validar extensión y tipo real. Formatos iniciales: PDF; Word DOC y DOCX; presentaciones PPT y PPTX; imágenes PNG, JPG y JPEG; videos MP4 y MOV; archivos de los temas PSeInt PSC, Arduino INO y Scratch SB3. Para DFD se aceptan PDF e imágenes; el formato nativo .dfd queda fuera hasta validar compatibilidad. Límite máximo: 500 MB por archivo; los videos podrán tener hasta 1 GB. La previsualización y calificación por IA dependerán de que el formato sea compatible, pero los archivos admitidos podrán almacenarse y descargarse conforme a permisos. |
| RNF-05 Aislamiento | Ejecutar intérpretes o código entregado en sandbox con límites de CPU, memoria, tiempo, archivos y red. |
| RNF-06 Trazabilidad | Si el modelo falla, no responde o no puede procesar un archivo, conservar la entrega, reintentar de forma controlada y mostrar el estado. Publicar automáticamente solo con confianza reportada por el modelo igual o superior al 80 %; en caso contrario solicitar confirmación docente. No publicar una calificación inventada. |
| RNF-07 Disponibilidad de IA | Si el proveedor falla o supera cuota, conservar la entrega y ponerla en cola/revisión; nunca perderla ni bloquear la entrega del estudiante. |
| RNF-08 Rendimiento | Los análisis de IA y archivos largos se ejecutarán en segundo plano y mostrarán el estado al usuario. |
| RNF-09 Respaldo | Definir copias diarias de PostgreSQL y archivos, retención, acceso y prueba periódica de restauración. |
| RNF-10 Mantenibilidad | Separar responsabilidades por capas y módulos; documentar API REST con OpenAPI. |
| RNF-11 Accesibilidad | Controles de teclado, contraste, etiquetas y mensajes de error comprensibles en pantallas clave. |
| RNF-12 Conservación | Conservar archivos cargados durante al menos seis meses desde su carga; aplicar un plazo mayor si la política de la Universidad lo exige. |

## 5. Reglas de negocio
| Regla | Descripción |
| --- | --- |
| RN-01 Identidad | La cédula numérica identifica una cuenta. Un estudiante solo se registra si el docente habilitó esa cédula. |
| RN-02 Aprobación | Una cuenta docente no tiene permisos docentes hasta que el administrador la apruebe. |
| RN-03 Acceso por curso | La información privada solo se muestra a miembros autorizados del curso; la biblioteca pública y los recursos marcados como públicos no requieren cuenta. |
| RN-04 Permisos del administrador | Solo las cuentas iniciales de Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi son administradores con privilegios completos. Pueden realizar todas las operaciones y cambiar el rol de cualquier cuenta. Las demás cuentas con rol Administrador tienen únicamente los permisos administrativos ordinarios definidos para ese rol: no pueden responder actividades, crear temas/materiales/actividades, calificar o ajustar notas, ni cambiar roles. El backend debe aplicar esta diferencia mediante autorización explícita. |
| RN-05 Entregas | No se aceptan entregas después del cierre, salvo que el docente amplíe el plazo o habilite otra entrega para ese estudiante. Una actividad vencida sin entrega recibe 0,0. |
| RN-06 Escala de notas | La escala será de 0,0 a 5,0. |
| RN-07 Planilla | Las actividades con ponderación explícita pesan ese porcentaje. El porcentaje restante se distribuye por igual entre actividades calificables sin porcentaje. La suma explícita no puede superar 100 %. Si no quedan actividades sin porcentaje y la suma no es 100 %, el docente deberá ajustar la configuración antes de cerrar la definitiva. La nota definitiva y el avance parcial se mostrarán por separado. |
| RN-08 Uso de IA | La IA evalúa el archivo con el enunciado, la rúbrica y la escala definidos por el docente al publicar la actividad. Para identificar la entrega se le enviará el nombre del estudiante y el archivo; no se enviarán cédula ni correo. La respuesta incluirá nota, retroalimentación y criterios evaluados. |
| RN-09 Revisión humana | La IA publicará automáticamente la nota cuando la entrega sea legible y la confianza reportada por el modelo de la evaluación sea igual o superior al 80 %. Una foto ilegible o una confianza inferior al umbral pasa a confirmación docente. Si falla el procesamiento técnico de otro archivo, no se publica una nota y se permite reintentar. El docente podrá corregir posteriormente una nota publicada. |
| RN-10 Evaluación de Scratch y Arduino | Scratch y Arduino se evaluarán con la rúbrica del docente mediante análisis determinista cuando sea viable o mediante IA para archivos compatibles. Si la entrega es legible y la confianza reportada por el modelo es igual o superior al 80 %, la nota y la retroalimentación se publican automáticamente; si una foto es ilegible o la confianza es inferior al umbral, se solicita confirmación docente. El docente podrá corregir una nota publicada y el sistema conservará el historial. |
| RN-11 Confianza de calificación | Se publicará automáticamente la nota cuando la entrega sea legible y el modelo reporte una confianza igual o superior al 80 %. Si la foto es ilegible o la confianza reportada está por debajo del 80 %, la entrega quedará para confirmación docente. La evaluación de Ollama + Qwen2.5-VL 7B está aprobada para el proyecto. |
| RN-12 DFD | En versión inicial se acepta imagen o fotografía del diagrama. La lectura del formato nativo .dfd queda fuera hasta comprobar soporte. |
| RN-13 Quizzes | El docente define el tiempo total. Al agotarse, el intento se envía automáticamente. Un intento por defecto; el docente puede habilitar otro. |
| RN-14 Ranking | La nota del quiz depende de aciertos y está entre 0,0 y 5,0. El ranking ordena primero por nota y luego por menor tiempo. |
| RN-15 Foro | Docente y cuentas Administrador podrán fijar, cerrar o eliminar hilos y mensajes según sus permisos; solo las cuentas iniciales con privilegios completos podrán ejecutar operaciones reservadas, como cambiar roles. |
| RN-16 Protección de cuentas | El sistema impedirá desactivar o retirar los privilegios completos de una cuenta inicial si la operación deja la plataforma sin ninguna cuenta con privilegios completos. Las cuentas iniciales pueden cambiar el rol de cualquier cuenta; el backend conservará siempre al menos una cuenta inicial activa con privilegios completos. |
| RN-17 Biblioteca | Los recursos de biblioteca respetarán su configuración pública o privada. La previsualización dentro de la página y la descarga estarán disponibles según formato compatible y permisos de acceso. |
| RN-18 Uso de IA local y proveedores externos | El proveedor inicial de IA será local. El modelo recibirá nombre del estudiante, archivo y contexto académico necesario para calificar; no recibirá cédula ni correo. Cualquier cambio futuro a un proveedor externo queda fuera del alcance actual y requerirá definir su integración. |
| RN-19 Juegos por bloques | Los niveles progresarán desde secuencias y rutas hacia ciclos y condiciones. El estudiante podrá ejecutar, observar el resultado y depurar la solución. |
| RN-20 Rúbrica por actividad | El docente definirá la rúbrica y la respuesta esperada al publicar cada actividad o quiz. La rúbrica quedará versionada para que las entregas se evalúen con los criterios vigentes al momento de la entrega. |
| RN-21 Archivos y conservación | Se aceptan archivos de hasta 500 MB, excepto videos de hasta 1 GB. Los formatos iniciales son PDF, DOC, DOCX, PPT, PPTX, PNG, JPG, JPEG, MP4 y MOV, además de PSC para PSeInt, INO para Arduino y SB3 para Scratch. Para DFD se aceptan PDF e imágenes; el formato nativo .dfd queda por fuera hasta validar compatibilidad. Los archivos no se eliminarán antes de cumplir seis meses desde su carga; una política institucional puede exigir un plazo mayor. |
| RN-22 Administradores iniciales | Las cuentas iniciales de administrador con privilegios completos corresponden a Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi. Ambas tendrán todos los permisos, incluido cambiar los roles de cualquier cuenta. |
| RN-23 Recuperación de contraseña | La cuenta solo podrá iniciar recuperación mediante un correo previamente verificado. El sistema responderá con un mensaje genérico para no revelar si una cuenta existe y permitirá máximo dos solicitudes por cuenta en cada día calendario de Colombia. Brevo enviará el enlace, pero LogiCode generará, validará y consumirá el token. El token será aleatorio, de un solo uso y válido durante 60 minutos; al vencer o utilizarse quedará invalidado. Al guardar una nueva contraseña, el backend revocará todas las sesiones activas de la cuenta. La nueva contraseña tendrá al menos 8 caracteres, incluido un número y un carácter especial, y se almacenará con BCrypt o Argon2id. No se registrarán contraseñas ni tokens en logs; las credenciales de Brevo se guardarán como secretos de Docker. |
| RN-24 Verificación del correo | La recuperación de contraseña solo estará disponible para cuentas cuyo correo haya sido verificado mediante un enlace de confirmación de un solo uso enviado por Brevo. La dirección verificada será el destino del correo de recuperación. |
| RN-25 Cambio asistido del correo | Si el usuario perdió acceso al correo vinculado, podrá validar la cuenta indicando el último correo registrado y la contraseña actual o inmediatamente anterior que recuerde. El sistema comparará el correo con el asociado a la cuenta y verificará la contraseña ingresada contra el hash actual o el hash de la contraseña inmediatamente anterior usando BCrypt o Argon2id. En la base de datos se conservarán solo esos dos hashes; nunca contraseñas en texto plano. Al cambiar la contraseña, el hash actual pasa a anterior, el nuevo hash pasa a actual y se descarta el hash anterior más antiguo. Si correo y contraseña coinciden, permitirá registrar un correo nuevo pendiente; Brevo enviará un enlace de confirmación y el nuevo correo solo reemplazará al anterior para recuperación después de confirmarse. Si los datos no coinciden, no se cambiará la dirección. Aplicar el límite de dos solicitudes diarias por cuenta y registrar el evento sin guardar la contraseña ingresada. |

## 6. IA para calificar entregas
LogiCode integrará un modelo multimodal para leer fotos y archivos de actividades, incluidos ejercicios de código escrito a mano. La IA aplicará la rúbrica y respuesta esperada definidas por el docente al publicar la actividad y publicará la nota automáticamente cuando la entrega sea legible. Solo las fotos ilegibles solicitarán confirmación docente; el docente conservará la posibilidad de corregir una nota publicada.

### Flujo de procesamiento
| Paso | Responsabilidad |
| --- | --- |
| 1. Recepción | Validar rol, plazo, tipo y límite; guardar la entrega y registrar la fecha. Aceptar hasta 500 MB por archivo y hasta 1 GB para videos. |
| 2. Preparación | Guardar el original en almacenamiento de archivos. Convertir PDF a páginas/imágenes o extraer texto de formatos permitidos; conservar vínculo con la entrega. |
| 3. Evaluación determinista | Aplicar la clave/casos de prueba si son verificables. Para la IA preparar el archivo, nombre del estudiante, enunciado, rúbrica y escala. No enviar cédula ni correo. |
| 4. Evaluación asistida por IA | La IA devuelve calificación, retroalimentación, criterios y legibilidad en formato estructurado. Publicar automáticamente solo si la entrega es legible y la confianza reportada por el modelo es igual o superior al 80 %. |
| 5. Revisión | Solicitar confirmación docente si la foto es ilegible o la confianza de evaluación es menor al 80 %. Si otro archivo falla técnicamente, conservarlo, mostrar error y permitir reintento; no publicar una nota sin resultado válido. |
| 6. Registro | Guardar modelo/proveedor, versión de rúbrica, salida, nota publicada, fecha y cambios posteriores del docente para trazabilidad. |

### Opciones de IA evaluadas y selección inicial
LogiCode utilizará Ollama con Qwen2.5-VL 7B en ejecución local dentro de Docker para apoyar la lectura de imágenes y documentos, incluidos ejercicios de código manuscrito. El proveedor y el modelo están aprobados para el proyecto. La nota se publicará automáticamente desde el 80 % de confianza reportada por el modelo; las fotos ilegibles o las evaluaciones bajo ese umbral pasarán a confirmación docente.

| Opción | Qué aporta | Límites y uso recomendado |
| --- | --- | --- |
| ELEGIDA · Ollama + Qwen2.5-VL 7B (local) | Modelo multimodal local para interpretar fotos y documentos; su informe técnico incluye OCR, análisis documental y contenido manuscrito. Se ejecuta desde un servicio local/Docker y evita enviar nombres y archivos a una API externa. | Es la elección inicial para probar código escrito a mano y calificar por rúbrica. No tiene cobro por solicitud, pero requiere memoria/cómputo local. Antes de activar calificaciones reales se debe validar con entregas representativas tanto la lectura de símbolos como la calibración del umbral de confianza del 80 %. No se asumirá que un 80 % reportado por el modelo equivale a 80 % de acierto. Fotos ilegibles o evaluaciones bajo el umbral pasan a confirmación docente. |
| Ollama + Gemma 4 (local) | Modelo multimodal local de Google DeepMind; variantes pequeñas dirigidas a ejecución en dispositivo. | Alternativa local para comparar calidad y hardware. No se selecciona como proveedor inicial. |
| GroqCloud + Qwen 3.8 27B (nube, cuota gratuita) | API con entrada de imágenes y salida estructurada; documentación publica límites gratuitos por modelo. | Alternativa de nube solo para pruebas anonimizadas o con aprobación institucional; tiene límites gratuitos y procesa datos fuera del entorno local. |
| Gemini API (nube, cuota gratuita) | API multimodal que admite imágenes y archivos; útil para prototipos y comparación de calidad. | No usar la cuota gratuita con entregas reales identificables: los términos permiten uso de entradas para mejorar servicios y revisión humana. Puede evaluarse un plan con condiciones de datos distintas si se aprueba presupuesto. |

Proveedor aprobado: Ollama con Qwen2.5-VL 7B local dentro del entorno Docker. El equipo de referencia es un Ryzen 7 5700U con 32 GB de RAM. Las pruebas de funcionamiento y desempeño quedan aprobadas como parte de la planeación y se registrarán durante la etapa de implementación, sin condicionar la elección del modelo ni del proveedor.

### Prueba de selección de IA
Preparar un conjunto de referencia con entregas de prueba autorizadas y calificadas previamente por el docente: pseudocódigo y código manuscrito legible y difícil, sombras, rotación, tachones y símbolos ambiguos como 0/O, 1/l y =/≠. Usar copias de prueba sin datos personales innecesarios.

El docente define y registra la calificación de referencia por criterio antes de evaluar las muestras con Qwen2.5-VL 7B Q4_K_M en Ollama, Docker y el equipo Ryzen 7 5700U con 32 GB de RAM.

Medir lectura exacta de símbolos, coincidencia de nota con la rúbrica docente, errores de calificación automática por encima del 80 % de confianza, calibración real de ese umbral, detección de ilegibilidad, latencia por archivo y memoria máxima utilizada.

La selección de Ollama + Qwen2.5-VL 7B y el plan de pruebas quedan aprobados sin una condición adicional. Durante la implementación se registrarán versión, configuración, lectura de símbolos, coincidencia de notas con la rúbrica, confianza, legibilidad, latencia y memoria. Se conserva la regla funcional: publicar automáticamente con confianza reportada igual o superior al 80 %; si una foto es ilegible o la confianza queda bajo el umbral, solicitar confirmación docente. Los fallos técnicos permiten reintentar y no asignan nota.

Recuperación de contraseña: Spring Boot generará y validará el token de recuperación; Brevo se utilizará únicamente para entregar el correo transaccional. El token será aleatorio, de un solo uso, tendrá una vigencia de 60 minutos y se almacenará protegido. Las credenciales de Brevo se configurarán como secretos del entorno Docker, nunca en el código ni en el repositorio. La aplicación responderá de forma equivalente exista o no la cuenta solicitante para no revelar usuarios registrados.

## 7. Arquitectura de software por capas
Se recomienda un monolito modular: una aplicación backend Spring Boot organizada en módulos internos, con una API REST y un proceso de trabajo en segundo plano para tareas lentas. No se recomienda empezar con microservicios ni Kubernetes. La arquitectura separa responsabilidades sin multiplicar despliegues.

| Capa | Tecnología y componentes | Cómo se trabajará en LogiCode |
| --- | --- | --- |
| 1. Presentación | React; HTML/CSS; diseño responsive. | Muestra pantallas y recoge acciones. Consume API REST. Mantiene estado visual; no decide permisos, notas oficiales ni reglas del curso. Separar vistas por Visitante, Estudiante, Docente y Administrador. |
| 2. Borde y entrega | HTTPS/TLS; proxy reverso (Caddy, Nginx o Traefik) y CDN opcional. | Termina TLS, entrega archivos estáticos y reenvía solicitudes a la API. En desarrollo puede omitirse el CDN; se agrega cuando exista despliegue público y necesidad de caché/protección. |
| 3. API e integración | REST sobre HTTPS; JSON; OpenAPI/Swagger; prefijo /api/v1. | Controladores reciben peticiones, validan formato y permisos, llaman servicios de aplicación y devuelven códigos HTTP claros. No contienen reglas de negocio ni consultas SQL directas. |
| 4. Lógica de negocio | Java y Spring Boot; Spring Security; monolito modular. | Casos de uso y reglas: usuarios, cursos, biblioteca, actividades, entregas, calificaciones, quizzes, juegos, foro y administración. Los servicios definen transacciones y verifican autorizaciones. |
| 5. Mensajería y tareas lentas | Worker Spring Boot y tabla de trabajos en PostgreSQL al inicio; RabbitMQ como opción de crecimiento. | El análisis de IA, conversión de documentos, correo y reportes se ejecutan en segundo plano. El estudiante recibe confirmación de entrega sin esperar el resultado. Reintentos controlados; no duplicar calificaciones al reintentar. |
| 6. Acceso a datos | Spring Data JPA/Hibernate; repositorios; Flyway para migraciones. | Repositorios encapsulan consultas. Servicios gestionan transacciones. Flyway versiona cambios de esquema. PostgreSQL no se expone a React ni a Internet. |
| 7. Almacenamiento | PostgreSQL; almacenamiento de objetos compatible con S3 (MinIO local; proveedor por definir en despliegue). | PostgreSQL guarda datos estructurados y metadatos. Archivos binarios se guardan en almacenamiento de objetos con claves privadas y URL de acceso controlada; no guardar fotos grandes como columnas de la BD. |
| 8. Infraestructura | Docker y Docker Compose; imágenes separadas para frontend, backend y PostgreSQL, más almacenamiento local de desarrollo. | Desarrollar y probar en local con Docker Compose. El despliegue en línea también será con Docker. El almacenamiento de objetos y el proveedor de alojamiento quedan por decidir antes de publicar. Mantener volúmenes persistentes y copias verificables. |
| Opcional. Analítica e IA | Adaptador GradingProvider; Ollama local o API multimodal intercambiable. | La capa de aplicación llama una interfaz común. Adaptadores específicos traducen a cada proveedor. La selección del modelo no contamina las reglas del dominio ni obliga a cambiar toda la aplicación. |

### Organización interna del backend
| Paquete/capa interna | Responsabilidad | Ejemplos |
| --- | --- | --- |
| api | DTO, validación de entrada, controladores, respuestas y manejo de errores. | ActivityController, SubmissionController, AdminController |
| application | Casos de uso y orquestación. | CreateActivity, SubmitWork, RequestAiGrading, ConfirmGrade |
| domain | Entidades, invariantes y políticas puras del negocio. | Activity, Submission, Grade, Rubric, QuizAttempt |
| infrastructure | Implementaciones técnicas de persistencia, archivos, correo, IA y tareas. | JpaSubmissionRepository, S3FileStorage, OllamaGradingAdapter |

### Módulos funcionales sugeridos
Identidad y permisos; cursos y matrículas; contenidos y biblioteca; actividades y entregas; evaluación y rúbricas; quizzes y banco de preguntas; juegos; foro; administración y respaldos.

Mantener los módulos dentro del mismo backend. Separarlos en servicios independientes solo si más adelante lo justifican carga, equipo o disponibilidad.

### Recorrido de una entrega con IA
| Orden | Recorrido |
| --- | --- |
| 1 | React envía la entrega a POST /api/v1/activities/{id}/submissions con sesión activa. |
| 2 | API verifica rol, pertenencia al curso, formato, tamaño y plazo; backend guarda metadatos en PostgreSQL y archivo en object storage. |
| 3 | Backend crea un trabajo de evaluación y responde 202 Accepted con ID y estado recibido/en proceso. |
| 4 | Worker obtiene el archivo, extrae texto o convierte páginas, ejecuta reglas deterministas y, si corresponde, llama al adaptador IA con la rúbrica. |
| 5 | El worker guarda el resultado estructurado, la evidencia, la legibilidad y la confianza reportada por el modelo. Si el contenido es legible y la confianza es igual o superior al 80 %, publica automáticamente la nota y la retroalimentación; si la foto es ilegible o la confianza es menor, la deja pendiente de confirmación docente. Los errores técnicos no generan nota. |
| 6 | API registra la calificación final, auditoría y retroalimentación; React actualiza estado y notifica al estudiante según permisos. |

### Diseño REST inicial
| Recurso | Ejemplos de operaciones |
| --- | --- |
| Autenticación | POST /api/v1/auth/login · POST /api/v1/auth/password-recovery · POST /api/v1/auth/password-reset |
| Cursos y temas | GET/POST /api/v1/courses · GET/POST /api/v1/topics |
| Biblioteca | GET /api/v1/library · GET /api/v1/materials/{id}/preview · GET /api/v1/materials/{id}/download |
| Actividades y entregas | GET/POST /api/v1/activities · POST /api/v1/activities/{id}/submissions |
| Evaluación | GET /api/v1/submissions/{id}/grading · POST /api/v1/submissions/{id}/grade/confirm |
| Foro | GET/POST /api/v1/forum/threads · PATCH /api/v1/forum/threads/{id} |
| Administración | GET/POST /api/v1/admin/users · POST /api/v1/admin/teachers/{id}/approve |

## 8. Seguridad y aspectos transversales
| Aspecto | Decisión inicial |
| --- | --- |
| Autenticación | Spring Security. Cédula como identificador; hash BCrypt/Argon2id para contraseñas. Exigir 8 caracteres como mínimo, con un número y un carácter especial. Recuperación solo por correo verificado, con token aleatorio de un solo uso que vence a los 60 minutos; máximo dos solicitudes por cuenta por día calendario en Colombia. Al cambiar la contraseña, invalidar todas las sesiones activas. Usar respuesta genérica, controles contra abuso y no registrar tokens ni contraseñas. Brevo entrega los correos transaccionales; API key y remitente se configuran como secretos de Docker. |
| Autorización | RBAC para visitante, estudiante, docente y administrador, más una autorización privilegiada asignada únicamente a las dos cuentas iniciales. Comprobar pertenencia al curso y permisos en cada operación del backend; no confiar en controles de interfaz. |
| Archivos | Validar extensión, tipo y permisos; claves no adivinables y acceso controlado. Máximo 500 MB, salvo videos hasta 1 GB. Retención mínima de seis meses desde la carga. |
| IA y privacidad | Modelo de IA ejecutado localmente en Docker; procesa nombre, archivo, enunciado y rúbrica sin cédula ni correo. No almacenar imágenes completas en logs. |
| Protección de ejecución | Código de estudiantes en proceso aislado, con sin acceso a red ni secretos y límites de CPU, memoria y tiempo. |
| Observabilidad | Logs estructurados con ID de solicitud/trabajo; métricas de errores, espera y cuota; alertas de fallos sin guardar contraseñas ni entregas. |
| CI/CD | Git desde inicio; automatizar compilación, análisis y validaciones antes de publicar; separar configuración local y producción. |
| Copias | Copias diarias de PostgreSQL y archivos; conservar archivos al menos seis meses desde su carga y probar restauración antes del piloto. |

## 9. Desarrollo por etapas y puntos de control
Cada etapa termina con una revisión de aceptación antes de comenzar la siguiente. Los cambios de alcance posteriores se registran como cambio solicitado y se estiman contra dependencias ya cerradas; esta práctica reduce retrabajo, aunque el desarrollo siempre puede revelar ajustes necesarios.

| Etapa | Aprobar alcance, permisos, límites y formatos de archivos, conservación, reglas de nota, uso de IA, recuperación de contraseña con Brevo y fecha objetivo del 10 de diciembre de 2026. No se fija todavía una estimación basada en tamaño o experiencia del equipo. | Punto de control para avanzar |
| --- | --- | --- |
| Etapa 0 · Aprobación de alcance | Aprobar alcance, permisos, límites de archivos, conservación, reglas de nota, uso de IA y fecha objetivo del 10 de diciembre de 2026. No se fija todavía una estimación basada en tamaño o experiencia del equipo. | Línea base aprobada y lista priorizada de RF/RN. |
| Etapa 1 · Análisis y diseño | Casos de uso, historias con criterios de aceptación, flujo por rol, maquetas, modelo de datos, arquitectura, endpoints OpenAPI, rúbricas y prueba de lectura de código manuscrito. | Diseño revisado por docente y equipo; decisiones ambiguas resueltas antes de programar. |
| Etapa 2 · Fundaciones técnicas | Repositorio Git, ramas, estructura React/Spring Boot modular, PostgreSQL, migraciones, Docker Compose, configuración por entorno, almacenamiento local, CI básico y health checks. | La aplicación arranca completa con configuración documentada y migraciones reproducibles. |
| Etapa 3 · Acceso y permisos | Registro/inicio de sesión, confirmación de correo con Brevo, recuperación de contraseña con token de un solo uso, aprobación docente, carga de estudiantes, roles, autorización y auditoría de acceso. | Verificar permisos completos y cambio de rol únicamente en las dos cuentas iniciales; comprobar que las demás cuentas Administrador no puedan cambiar roles ni responder actividades, crear temas/materiales/actividades, calificar o ajustar notas. El sistema debe impedir quedarse sin cuenta inicial activa con privilegios completos. |
| Etapa 4 · Curso, temas y biblioteca | Curso, temas, materiales, privacidad, biblioteca pública, filtros, previsualización y descarga compatible. | Visitante y usuario de curso ven exactamente los recursos permitidos; móvil revisado. |
| Etapa 5 · Actividades y entregas | Crear actividad/rúbrica, apertura/cierre, formatos, entrega, historial y bandeja Por revisar; excepciones de plazo/segundo intento. | Un ciclo completo de creación y entrega funciona sin IA; verificar fechas y excepciones de plazo. |
| Etapa 6 · Calificación determinista e IA | Pruebas de aceptación aprobadas: ejecutar Ollama + Qwen2.5-VL 7B Q4_K_M dentro de Docker en el equipo Ryzen 7 5700U con 32 GB; documentar lectura de muestras manuscritas con rúbricas de referencia, coincidencia de notas, confianza reportada, legibilidad, latencia y RAM máxima. | Las pruebas de aceptación y la selección del proveedor/modelo están aprobadas sin condiciones adicionales. Registrar sus resultados para dejar evidencia del comportamiento en el equipo de presentación. La regla funcional permanece: confianza reportada ≥80 % y contenido legible publica la nota; foto ilegible o confianza inferior requiere confirmación docente; un fallo técnico no asigna nota y permite reintento. |
| Etapa 7 · Quizzes, PSeInt y juegos | Editor/intérprete con aislamiento, casos de prueba, quizzes y banco de preguntas, tiempo total, intentos y ranking; juegos de salida, DFD, orden y bloques con secuencia/ciclo/condición/ruta/depuración. | Niveles y calificaciones aceptados por docente; ninguna ejecución puede acceder al host o exceder límites. |
| Etapa 8 · Foro y operación docente | Foro/moderación, planilla, exportación, estadísticas básicas, biblioteca administrable, backups y panel de administración. | Permisos administrativos revisados y exportación conciliada con la escala/pesos. |
| Etapa 9 · Calidad y seguridad | Pruebas unitarias, integración, API, permisos, carga de archivos, móvil/accesibilidad, seguridad, restauración y observabilidad. | Lista de defectos críticos resuelta; restauración y flujo de calificación demostrados. |
| Etapa 10 · Piloto y entrega | Despliegue local inicial con Docker Compose y entrega objetivo el 10 de diciembre de 2026. Todo despliegue futuro será con Docker. Proveedor en línea de almacenamiento de objetos y alojamiento se decidirá antes de publicar. | Docente acepta el piloto; respaldos restaurables; manual técnico, docente y estudiante entregados. |

### Orden de prioridades
| Prioridad | Incluye |
| --- | --- |
| P0 · Esencial | Identidad y permisos, curso, temas, biblioteca, actividades, entregas, revisión y planilla. |
| P1 · Evaluación asistida | Quizzes, PSeInt, calificación IA de fotos/archivos, nota automática si es legible, revisión docente solo para foto ilegible, juegos de bloques y foro. |
| P2 · Aprendizaje y comunidad | Quizzes, PSeInt, juegos de bloques, foro y estadísticas. |
| P3 · Publicación y crecimiento | Docker de producción, observabilidad, respaldos, pruebas de restauración y soporte a más cursos. |

## 10. Acuerdos de etapa 0 y decisiones aún abiertas
Administradores iniciales con privilegios completos: Carlos Alberto Escamilla Mariño y Yarly Melitza Guerrero Berbesi. Son las únicas cuentas que pueden cambiar roles y ejecutar todas las operaciones. Otras cuentas con rol Administrador tendrán los permisos ordinarios y las restricciones descritas en la matriz de roles.

Fecha objetivo de entrega: 10 de diciembre de 2026. La planeación no se condiciona por ahora al tamaño ni a la experiencia del equipo.

Límites y formatos iniciales: 500 MB por archivo y 1 GB para video. Se aceptarán PDF, DOC/DOCX, PPT/PPTX, PNG/JPG/JPEG, MP4/MOV y formatos de tema PSC, INO y SB3; DFD se recibirá como PDF o imagen y .dfd nativo queda pendiente de validar.

El docente configura enunciado, respuesta esperada y rúbrica al publicar cada actividad o quiz; el sistema conserva la versión aplicada a cada entrega.

Las notas de IA se publican automáticamente cuando la entrega es legible y la confianza reportada por el modelo es igual o superior al 80 %. Las fotos ilegibles o resultados bajo el umbral requieren confirmación docente. Los errores técnicos no producen nota.

Correo transaccional: Brevo enviará tanto la confirmación de correo como los mensajes de recuperación; no administrará cuentas ni generará/validará los enlaces. LogiCode genera cada token temporal; Spring Boot construye el enlace y solicita su envío a Brevo; React entrega el token a la API al abrirlo; Spring Boot verifica expiración y uso, permite registrar la contraseña nueva e invalida todas las sesiones activas. Se requiere una API key y un remitente configurado en Brevo. Las credenciales se guardarán como secretos de Docker. El plan gratuito ofrece actualmente hasta 300 envíos diarios compartidos entre los mensajes enviados por la aplicación, sujeto a las condiciones del proveedor.

Proveedor aprobado para LogiCode: Ollama + Qwen2.5-VL 7B en Docker. Su selección y aprobación no quedan condicionadas a una prueba adicional. El umbral definido para publicación automática es 80 % de confianza indicada por el modelo; las fotos ilegibles o resultados bajo ese umbral solicitan confirmación docente.

Desarrollo, pruebas y primer despliegue en local con Docker. La publicación en línea queda en observación y fuera de las decisiones inmediatas; el alojamiento y almacenamiento remoto se definirán cuando se retome esa etapa.

A la IA se enviará el nombre del estudiante y el archivo, junto con la rúbrica, el enunciado y la escala necesarios para evaluar; no se enviarán cédula ni correo.

La nota va de 0,0 a 5,0. Una actividad vencida sin entrega vale 0,0 salvo que el docente habilite una nueva oportunidad. La nota definitiva se muestra separada del avance parcial.

Protocolo de pruebas aprobado: usar el portátil con AMD Ryzen 7 5700U y 32 GB de RAM para ejecutar Docker, Ollama y Qwen2.5-VL 7B Q4_K_M. La ficha de Ollama reporta aproximadamente 6 GB para esta variante y requiere Ollama 0.7.0 o posterior según la consulta realizada. Registrar como pruebas de aceptación la lectura manuscrita, coincidencia con la rúbrica, confianza, falsos automáticos, latencia y RAM máxima. Estas mediciones documentan el comportamiento del equipo y no constituyen una condición adicional para la selección aprobada.

Despliegue en línea: queda en observación y no forma parte de las decisiones de esta etapa; el proyecto se trabajará y probará localmente con Docker.

Decisión de recuperación: el correo debe verificarse antes de habilitar esta función. Al cambiar la contraseña, se cerrarán todas las sesiones activas.

Política de recuperación y contraseña: mínimo 8 caracteres, al menos un número y un carácter especial; máximo dos solicitudes por cuenta por día calendario en Colombia. La vigencia del enlace será de 60 minutos y será de un solo uso.

Recuperación si se pierde acceso al correo: el usuario ingresará el último correo registrado y la contraseña actual o la inmediatamente anterior que recuerde. El backend verificará el correo y cotejará la contraseña con el hash actual o el anterior. En PostgreSQL solo se conservarán ambos hashes, nunca el texto de las contraseñas. Si los datos coinciden, se permite registrar un correo nuevo pendiente; Brevo enviará un enlace y la dirección quedará activa para recuperación al confirmarse.

## 11. Referencias técnicas consultadas
La arquitectura toma como base la guía compartida y documentación oficial consultada el 17 de octubre de 2026. La elección de IA se debe validar con trabajos de código manuscrito del curso antes de usarla para publicar calificaciones reales.

Guía de aprendizaje Arquitectura de software moderna (material aportado por el equipo)

Gemini API: comprensión de imágenes y archivos

Gemini API: términos sobre servicios gratuitos y uso de datos

Gemini API: precios y modalidades gratuitas

Ollama Library: Qwen2.5-VL 7B Q4_K_M, tamaño publicado aproximado de 6 GB y versión mínima de Ollama indicada en su ficha (consulta del 17 de octubre de 2026).

Ollama: Qwen2.5-VL y análisis visual/documental

Ollama: Gemma 4 multimodal

Groq: modelos de visión

Groq: límites gratuitos por modelo

Groq: retención y controles de datos

Spring Boot: seguridad web

Spring Boot: persistencia SQL con JPA

Spring Data JPA: repositorios y transacciones

Docker Compose: despliegue

PostgreSQL: copias lógicas con pg_dump

Qwen2.5-VL Technical Report: OCR, visión y comprensión de documentos

Qwen2.5-VL Technical Report: OCR y documentos manuscritos

Brevo: planes y límites del plan gratuito, incluidos los correos transaccionales (consulta del 17 de octubre de 2026).

Ley 1581 de 2012: protección de datos personales, autorización e información al titular
