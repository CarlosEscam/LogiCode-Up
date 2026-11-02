# Historias de usuario

Documento de la Etapa 1 (análisis y diseño), escrito después de construir el sistema y verificado contra el código el 1 de noviembre de 2026. Cada historia indica los requisitos de la [especificación](../LogiCode%20-%20Especificacion%20para%20Claude%20Code.md) que cubre, sus criterios de aceptación en formato Dado / Cuando / Entonces y su estado:

- **Implementado**: existe en el código y los criterios tienen prueba automática o verificación registrada en [estado.md](estado.md).
- **Pendiente**: no existe o no se ha ejecutado.

Los casos de uso que detallan los flujos están en [casos-de-uso.md](casos-de-uso.md).

## Épica 1 · Acceso y cuentas

### HU-01 Registrarme con mi cédula

Como **estudiante** quiero registrarme con mi cédula, mi correo y una contraseña para entrar al curso en el que mi docente me habilitó.

- **Requisitos:** RF-01, RN-01, RNF-02. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado que mi cédula está habilitada y sin cuenta, cuando me registro con un correo libre y una contraseña válida, entonces se crea mi cuenta de estudiante y me llega el enlace para confirmar el correo.
  2. Dado que mi cédula no está habilitada, cuando intento registrarme, entonces no se crea la cuenta y el mensaje me dice que pida la habilitación a mi docente.
  3. Dado que escribo una contraseña de menos de 8 caracteres, o sin un número, o sin un carácter especial, cuando envío el formulario, entonces se rechaza y se me indica la regla.
  4. Dado que la cédula o el correo ya tienen cuenta, cuando me registro, entonces se rechaza con el motivo.

### HU-02 Solicitar una cuenta de docente

Como **docente** quiero solicitar mi cuenta para administrar mi curso cuando un administrador me apruebe.

- **Requisitos:** RF-02, RN-02. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado que envío la solicitud con datos válidos, cuando se guarda, entonces mi cuenta queda "Pendiente de aprobación".
  2. Dado que mi cuenta está pendiente, cuando intento ingresar, entonces el sistema me lo impide y me avisa que falta la aprobación.
  3. Dado que un administrador aprobó mi cuenta, cuando ingreso, entonces tengo los permisos de docente.

### HU-03 Ingresar con mi cédula

Como **usuario** quiero iniciar y cerrar sesión con mi cédula y mi contraseña para usar la plataforma de forma segura.

- **Requisitos:** RF-03, RNF-02. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una cuenta activa, cuando escribo cédula y contraseña correctas, entonces entro y veo el menú que corresponde a mis permisos.
  2. Dado datos incorrectos, cuando intento ingresar, entonces recibo un mensaje genérico que no revela cuál dato falló.
  3. Dado 5 intentos fallidos en 15 minutos para una cédula, cuando vuelvo a intentar, entonces el sistema responde que espere hasta que pase la ventana.
  4. Dado que pulso **Cerrar sesión**, cuando vuelvo a una página privada, entonces se me pide ingresar.

### HU-04 Confirmar mi correo

Como **usuario** quiero confirmar que el correo es mío para poder recuperar mi contraseña si la olvido.

- **Requisitos:** RF-29, RN-24. **Estado:** Implementado; envío real con Brevo probado el 1 de noviembre en el equipo de Carlos con `logicode.notificaciones@gmail.com` (P-06).
- **Criterios de aceptación:**
  1. Dado que recibí el enlace de confirmación, cuando lo abro dentro de 24 horas, entonces mi correo queda verificado.
  2. Dado que ya usé el enlace o pasaron 24 horas, cuando lo abro, entonces se rechaza y puedo pedir otro desde **Mi cuenta**.

### HU-05 Recuperar mi contraseña

Como **usuario** quiero recibir un enlace en mi correo verificado para crear una contraseña nueva cuando la olvide.

- **Requisitos:** RF-04, RN-23, RNF-02. **Estado:** Implementado; envío real con Brevo probado el 1 de noviembre en el equipo de Carlos con `logicode.notificaciones@gmail.com` (P-06).
- **Criterios de aceptación:**
  1. Dado que escribo un correo, cuando pido la recuperación, entonces la respuesta es la misma exista o no una cuenta con ese correo.
  2. Dado una cuenta con el correo verificado, cuando pido la recuperación, entonces recibo un enlace de un solo uso válido durante 60 minutos.
  3. Dado que ya hice 2 solicitudes hoy (día calendario de Colombia), cuando pido otra, entonces no se envía un enlace nuevo.
  4. Dado un enlace vigente, cuando guardo una contraseña válida, entonces el enlace queda usado y se cierran todas mis sesiones abiertas, también en otros equipos.
  5. Dado un enlace usado o vencido, cuando lo abro, entonces se rechaza.
  6. Dado una cuenta con el correo sin verificar, cuando pido la recuperación, entonces no se envía enlace.

### HU-06 Cambiar mi correo si ya no tengo acceso

Como **usuario** quiero registrar un correo nuevo usando el último correo y una contraseña que recuerde para no perder la recuperación de mi cuenta.

- **Requisitos:** RF-30, RN-25. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado que escribo mi cédula, el último correo registrado y mi contraseña actual o la inmediatamente anterior, cuando coinciden, entonces me llega un enlace al correo nuevo.
  2. Dado que confirmo ese enlace, cuando se procesa, entonces el correo nuevo reemplaza al anterior para la recuperación; antes de confirmarlo sigue valiendo el anterior.
  3. Dado que el correo o la contraseña no coinciden, cuando envío la solicitud, entonces no cambia nada y la respuesta no revela qué falló.
  4. Dado que ya hice 2 solicitudes de cambio de correo hoy, cuando pido otra, entonces no se procesa; este contador es distinto del de recuperación de contraseña.
  5. Dado cualquier solicitud, cuando se registra en la auditoría, entonces no se guarda la contraseña ingresada.

### HU-07 Cambiar mi contraseña

Como **usuario** quiero cambiar mi contraseña desde **Mi cuenta** para mantener segura mi cuenta.

- **Requisitos:** RNF-02. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado que escribo mi contraseña actual y una nueva válida, cuando la guardo, entonces el hash actual pasa a ser el anterior, el nuevo ocupa el actual y se descarta el más antiguo.
  2. Dado que cambié la contraseña, cuando uso otra sesión abierta, entonces esa sesión ya no sirve.

## Épica 2 · Administración de la plataforma

### HU-08 Aprobar docentes

Como **administrador** quiero aprobar las solicitudes de docentes para que solo docentes reales administren cursos.

- **Requisitos:** RF-02, RF-28, RN-02. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una solicitud pendiente, cuando pulso **Aprobar** en **Usuarios**, entonces la cuenta queda activa con permisos de docente y el hecho queda en la auditoría.
  2. Dado una cuenta de estudiante o de docente, cuando busca **Usuarios**, entonces recibe "Sin permiso".

### HU-09 Activar y desactivar cuentas

Como **administrador** quiero desactivar o activar cuentas para controlar quién usa la plataforma.

- **Requisitos:** RF-28, RN-16. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una cuenta activa, cuando la desactivo, entonces pierde el acceso en su siguiente petición y se cierran sus sesiones.
  2. Dado que soy administrador ordinario, cuando intento desactivar una cuenta inicial, entonces se rechaza.
  3. Dado que una operación dejaría la plataforma sin ninguna cuenta inicial activa con privilegio completo, cuando se intenta, entonces se rechaza.

### HU-10 Cambiar roles solo desde las cuentas iniciales

Como **administrador inicial** quiero cambiar el rol de cualquier cuenta para corregir errores de registro, sabiendo que nadie más puede hacerlo.

- **Requisitos:** RF-28, RN-04, RN-16, RN-22. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado que soy una cuenta inicial, cuando cambio el rol de otra cuenta, entonces se guarda y se cierran las sesiones de esa cuenta.
  2. Dado que soy administrador ordinario, cuando intento cambiar un rol (por la interfaz o directamente por la API), entonces recibo `403`.
  3. Dado cualquier cuenta, cuando alguien intenta darle el privilegio completo por la web, entonces no es posible: solo lo tienen las cuentas configuradas en `.env`, y el arranque lo restablece.

### HU-11 Administrador ordinario sin funciones académicas

Como **responsable del curso** quiero que las cuentas Administrador ordinarias no respondan actividades, no creen contenido y no califiquen para proteger la integridad de las notas.

- **Requisitos:** RN-04, RF-28. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado un administrador ordinario, cuando intenta crear un tema, un material o una actividad, entregar una actividad, calificar o corregir una nota, entonces recibe `403`.
  2. Dado un administrador ordinario, cuando consulta actividades, entregas, Por revisar, notas o la planilla, entonces las ve en solo lectura.

### HU-12 Ver el estado de la plataforma y la auditoría

Como **administrador** quiero ver cifras de uso y los eventos de acceso para operar el piloto y detectar abusos.

- **Requisitos:** RF-28. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado que abro **Administración**, cuando carga, entonces veo cuentas por rol y estado, cursos, actividades, entregas, notas en revisión o con la IA, materiales, espacio usado y actividad del foro.
  2. Dado que abro **Auditoría**, cuando reviso un evento, entonces veo fecha en hora de Colombia, acción, resultado y sujeto, sin contraseñas ni tokens.

### HU-13 Respaldos diarios con prueba de restauración

Como **administrador** quiero copias diarias de la base y de los archivos, probadas, para recuperar el curso si algo falla.

- **Requisitos:** RNF-09, RF-28, RNF-12. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado el servicio de respaldos corriendo, cuando llega la hora configurada (hora de Colombia), entonces se crea una copia de la base y de los archivos y se prueba restaurarla en una base aparte.
  2. Dado que pulso **Crear respaldo ahora**, cuando el servicio la toma (en menos de un minuto), entonces aparece en la lista con "Restauración probada" o el error.
  3. Dado una copia más vieja que `BACKUP_RETENTION_DAYS`, cuando corre el servicio, entonces se borra; los archivos cargados siguen guardados al menos seis meses en su volumen.
  4. Dado que necesito restaurar, cuando sigo [respaldos/README.md](../respaldos/README.md) en la consola, entonces la base y los archivos quedan como en la copia. Restaurar y descargar copias desde la web no está disponible a propósito.

## Épica 3 · Curso y contenidos

### HU-14 Inscribir a mis estudiantes desde Excel

Como **docente** quiero inscribir a mis estudiantes con una lista de Excel para no escribir cada cédula a mano.

- **Requisitos:** RF-05, RN-01, RN-03 (P-07). **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado un `.xlsx` con la cédula en la columna A y el nombre opcional en la B (con o sin encabezado), cuando pulso **Importar lista**, entonces se inscriben y habilitan las cédulas válidas y veo cuántas se inscribieron, cuántas ya estaban y qué filas tienen errores.
  2. Dado una cédula escrita como número en Excel, cuando se lee, entonces se toma sin decimales ni notación científica.
  3. Dado un `.csv` con `cedula` o `cedula,nombre`, cuando lo importo, entonces se aplican las mismas reglas.
  4. Dado un archivo de más de 1 MB, de más de 2000 filas, `.xls` antiguo o dañado, cuando lo importo, entonces se rechaza con el motivo.

### HU-15 Organizar los temas del curso

Como **docente** quiero crear y ordenar temas por herramienta y marcar la próxima clase para que el grupo sepa qué estudiar.

- **Requisitos:** RF-06, RF-09. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado mi curso, cuando creo un tema, entonces elijo la herramienta entre PSeInt, DFD, Scratch, Arduino o General.
  2. Dado varios temas, cuando uso **Subir** y **Bajar**, entonces los estudiantes los ven en ese orden.
  3. Dado que marco un tema como próxima clase, cuando marco otro, entonces el primero deja de estarlo (uno por curso).

### HU-16 Publicar materiales públicos o privados

Como **docente** quiero subir guías, presentaciones, videos y archivos de herramientas, públicos o privados, para compartirlos con el curso o con todos.

- **Requisitos:** RF-07, RF-08, RNF-04, RNF-12, RN-17, RN-21. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado un PDF, DOC/DOCX, PPT/PPTX, PNG/JPG/JPEG, MP4/MOV, `.psc`, `.ino` o `.sb3`, cuando lo subo, entonces se valida su tipo real y se acepta hasta 500 MB (videos hasta 1 GB).
  2. Dado un archivo cuyo contenido no corresponde a su extensión, cuando lo subo, entonces se rechaza.
  3. Dado un material privado, cuando lo busca alguien que no es del curso ni Administrador, entonces no lo ve (responde como si no existiera).
  4. Dado un material archivado, cuando se oculta, entonces su archivo se conserva al menos seis meses desde la carga.

### HU-17 Consultar la biblioteca sin cuenta

Como **visitante** quiero buscar y ver los materiales públicos para estudiar sin registrarme.

- **Requisitos:** RF-24, RF-25, RN-03, RN-17. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado la **Biblioteca**, cuando busco por texto y filtro por tema, herramienta o tipo, entonces veo solo materiales públicos que coinciden.
  2. Dado un PDF, una imagen o un video compatible, cuando pulso **Ver**, entonces se muestra dentro de la página; con **Descargar** lo guardo con su nombre original.

## Épica 4 · Actividades y entregas

### HU-18 Crear una actividad con rúbrica versionada

Como **docente** quiero definir enunciado, fechas, porcentaje, intentos, formas de entrega y rúbrica para evaluar con criterios claros.

- **Requisitos:** RF-10, RF-11, RN-07, RN-20. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una actividad nueva, cuando la guardo, entonces queda en borrador y solo la ve quien administra el curso.
  2. Dado una actividad sin rúbrica, cuando intento publicarla, entonces se rechaza.
  3. Dado porcentajes que pasarían de 100 % en el curso, cuando guardo, entonces se rechaza y se me dice cuánto queda disponible.
  4. Dado una actividad con entregas, cuando cambio la rúbrica o el enunciado, entonces se crea una versión nueva y cada entrega conserva la versión con la que se envió.
  5. Dado un estudiante, cuando ve la rúbrica, entonces no ve la respuesta esperada.

### HU-19 Entregar mi actividad desde el celular

Como **estudiante** quiero entregar texto, archivos o fotos de mi cuaderno desde el celular para no depender de un computador.

- **Requisitos:** RF-13, RF-19, RN-05, RNF-01, RNF-04. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una actividad abierta, cuando envío hasta 5 archivos o fotos en los formatos que pide, entonces se guarda mi intento y veo su estado.
  2. Dado un archivo de más de 500 MB (o un video de más de 1 GB) o un formato que la actividad no acepta, cuando lo envío, entonces se rechaza con el motivo.
  3. Dado que la actividad cerró y no tengo excepción, cuando intento entregar, entonces se rechaza.
  4. Dado que la actividad cerró sin que yo entregara, cuando consulto, entonces veo "Vencida sin entrega: 0,0".
  5. Dado que tengo intentos disponibles, cuando vuelvo a entregar, entonces cuenta el último intento.

### HU-20 Reabrir o dar intentos a un estudiante

Como **docente** quiero ampliar el plazo o dar intentos adicionales a un estudiante para atender una incapacidad o un segundo intento.

- **Requisitos:** RF-11, RN-05. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una actividad vencida sin entrega de un estudiante, cuando le doy un plazo nuevo, entonces puede entregar, su 0,0 desaparece y la entrega queda marcada "tarde".
  2. Dado una excepción, cuando intento poner un plazo en el pasado, entonces se rechaza.
  3. Dado un estudiante con intentos agotados, cuando le doy intentos adicionales (hasta 10), entonces puede volver a entregar.

## Épica 5 · Calificación

### HU-21 Nota de la IA solo cuando es confiable

Como **docente** quiero que la IA publique sola solo las notas de entregas legibles con confianza de 80 % o más para ahorrar tiempo sin perder el control.

- **Requisitos:** RF-14, RF-15, RN-08, RN-09, RN-11, RNF-06, RNF-07, RNF-08. **Estado:** Implementado; las pruebas de aceptación de Qwen2.5-VL en el equipo de referencia están **Pendientes**, así que la calibración real del umbral no se ha medido.
- **Criterios de aceptación:**
  1. Dado la IA encendida y una entrega legible, cuando el modelo reporta 80 % o más de confianza, entonces la nota se publica con su retroalimentación como "IA" y queda en el historial.
  2. Dado una foto ilegible o una confianza menor a 80 %, cuando termina la evaluación, entonces la entrega pasa a Por revisar con el motivo y la propuesta, sin nota publicada.
  3. Dado un fallo técnico del modelo, cuando se agotan 3 intentos automáticos, entonces la entrega pasa a Por revisar como "Falló la IA", sin nota, y puedo pulsar **Reintentar con IA**.
  4. Dado cualquier evaluación, cuando se arma la solicitud al modelo, entonces incluye el nombre del estudiante, el enunciado, la rúbrica, la escala y el contenido, y nunca la cédula, el correo ni los nombres de archivo.
  5. Dado una entrega en Word, PowerPoint, video, `.sb3`, o un PDF de más de 5 páginas, con contraseña o dañado, cuando llega, entonces va directo al docente como "Formato que la IA no lee".
  6. Dado que la IA está calificando, cuando el estudiante consulta, entonces ve "Calificando con IA" y su entrega nunca se pierde ni se bloquea.

### HU-22 La IA lee mis entregas en PDF

Como **estudiante** quiero entregar mi trabajo en PDF y que la IA lo lea para recibir la nota sin esperar al docente cuando sea posible.

- **Requisitos:** RF-14 (P-08). **Estado:** Implementado; prueba con PDF real y Qwen hecha el 1 de noviembre de 2026 en el PC de Carlos: dos notas iguales a la del docente, 8 a 9 minutos por PDF de 3 páginas ([ia-pruebas/README.md](../ia-pruebas/README.md)).
- **Criterios de aceptación:**
  1. Dado un PDF de hasta 5 páginas y 25 MB, cuando la IA lo evalúa, entonces recibe su texto (si lo tiene) y cada página como imagen.
  2. Dado un PDF escaneado sin texto, cuando la IA lo evalúa, entonces recibe las imágenes de las páginas.

### HU-23 Nota automática en talleres de PSeInt

Como **estudiante** quiero que mi programa de PSeInt se califique al instante con casos de prueba para saber enseguida si funciona.

- **Requisitos:** RF-12, RF-16, RNF-05. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una actividad con casos de prueba, cuando entrego un programa que pasa 2 de 3 casos, entonces mi nota es 3,3, publicada como "Automática" con el detalle.
  2. Dado que el programa muestra mensajes extra ("La suma es 15"), cuando la salida esperada es "15", entonces el caso pasa; "5" no coincide dentro de "15".
  3. Dado un programa con error de sintaxis, cuando lo entrego, entonces no recibo un 0 automático: la entrega va al docente como "El programa no se pudo ejecutar".
  4. Dado un programa con un ciclo infinito, cuando se ejecuta, entonces se corta por los límites (2 s, 1 000 000 de pasos) sin afectar el servidor.

### HU-24 Configurar un reto de bloques con nota

Como **docente** quiero dibujar un tablero y elegir los bloques permitidos para evaluar secuencias, ciclos y condiciones con el robot.

- **Requisitos:** RF-12, RN-19, RN-20. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado una actividad con entrega en texto, cuando marco "Calificar con un reto de bloques" y escribo el tablero (`#`, `.`, `S`, `M`, hasta 12 × 12), la orientación, el máximo de bloques (1 a 40) y los bloques permitidos, entonces puedo probarlo en la vista previa y guardarlo en la rúbrica.
  2. Dado un tablero sin salida o sin meta, filas de distinto largo, o una actividad que también tiene casos de prueba, cuando guardo, entonces se rechaza.
  3. Dado que marco "El estudiante empieza con el programa de la vista previa", cuando el estudiante abre la actividad, entonces empieza con ese programa (reto de depuración).

### HU-25 Resolver un reto de bloques con nota

Como **estudiante** quiero armar el programa del robot en la actividad y enviarlo para obtener mi nota de inmediato.

- **Requisitos:** RF-12, RF-22. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado un reto, cuando el robot llega a la meta con mi programa, entonces mi nota es 5,0 con la razón.
  2. Dado que el robot choca, termina antes de la meta o pasa de 400 pasos, cuando envío, entonces mi nota es 0,0 con la razón, y puedo volver a intentar si me quedan intentos.
  3. Dado un programa armado a mano por fuera de la interfaz con bloques no permitidos o de más, cuando llega, entonces no se califica solo: va al docente.
  4. Dado cualquier envío, cuando se califica, entonces el servidor vuelve a ejecutar el programa y no confía en el resultado del navegador.

### HU-26 Revisar y corregir notas con historial

Como **docente** quiero revisar las entregas pendientes y corregir cualquier nota dejando historial para mantener el control pedagógico.

- **Requisitos:** RF-15, RF-17, RN-06, RN-09, RN-10. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado la bandeja **Por revisar**, cuando la abro, entonces veo el último intento de cada estudiante sin nota publicada, con el motivo.
  2. Dado una entrega, cuando escribo una nota fuera de 0,0 a 5,0 o con más de un decimal, entonces se rechaza.
  3. Dado una nota publicada (por mí, por la IA o automática), cuando la corrijo, entonces el historial guarda la anterior, la nueva, quién y cuándo.
  4. Dado que califico mientras la IA trabaja, cuando la IA termina, entonces su resultado se guarda pero no cambia mi nota.

## Épica 6 · Quizzes

### HU-27 Armar quizzes desde un banco de preguntas

Como **docente** quiero un banco de preguntas y quizzes con tiempo total para evaluar y repasar.

- **Requisitos:** RF-10, RF-21, RN-13, RN-20. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado el banco, cuando creo preguntas de selección única, múltiple, falso o verdadero o respuesta corta (con hasta 5 respuestas aceptadas) de 1 a 10 puntos, entonces puedo usarlas en cualquier quiz del curso.
  2. Dado una pregunta ya respondida en un intento, cuando intento editarla, entonces se rechaza.
  3. Dado un quiz con intentos, cuando intento cambiar sus preguntas o su tiempo, entonces se rechaza.

### HU-28 Presentar un quiz con tiempo

Como **estudiante** quiero presentar el quiz sin perder mis respuestas si se cierra la página para que el tiempo sea justo.

- **Requisitos:** RF-16, RF-21, RN-13, RN-14. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado un quiz calificable, cuando lo envío, entonces mi nota es 5 × puntos obtenidos ÷ puntos totales, con un decimal, entre 0,0 y 5,0.
  2. Dado que se acaba el tiempo o cierro el navegador, cuando vence el intento, entonces el servidor lo envía con lo que guardé.
  3. Dado un quiz de repaso, cuando termino, entonces no recibo nota y veo la revisión con la explicación; puedo repetirlo.
  4. Dado el ranking, cuando lo consulto, entonces se ordena por nota y luego por menor tiempo.
  5. Dado un quiz calificable abierto, cuando lo termino, entonces no veo la clave hasta que el quiz cierre.

## Épica 7 · Práctica

### HU-29 Practicar con el editor de PSeInt

Como **estudiante** quiero ejecutar mis programas de PSeInt en el navegador para probarlos antes de entregarlos.

- **Requisitos:** RNF-05. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado un programa y unas entradas, cuando pulso **Ejecutar**, entonces veo la salida o el error con su línea.
  2. Dado un programa que usa `Para Cada` o el perfil estricto de PSeInt, cuando lo ejecuto, entonces se indica que la instrucción no está disponible (soporte **Pendiente**).

### HU-30 Encontrar juegos por herramienta y tema

Como **estudiante** quiero filtrar los juegos por herramienta y tema para practicar justo lo que me cuesta.

- **Requisitos:** RF-22, RN-19. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado el catálogo de **Juegos**, cuando elijo una herramienta (Scratch, PSeInt, DFD o Arduino) o un tema (Secuencias, Variables, Condiciones, Ciclos, Arreglos, Matrices, Textos, Funciones, Rutas, Depuración, Sensores y actuadores), entonces veo solo los juegos que coinciden y el total de niveles.
  2. Dado que ningún juego coincide, cuando filtro, entonces el catálogo lo dice.
  3. Dado que ya resolví niveles, cuando vuelvo al catálogo en el mismo navegador, entonces veo "N de M niveles resueltos" y **Seguir jugando** abre el primer nivel sin resolver.
  4. Dado un navegador en modo privado, cuando juego, entonces funciona igual aunque no se recuerde el avance.

### HU-31 Jugar los juegos de lógica

Como **estudiante** quiero niveles que vayan de lo fácil a lo difícil en cada juego para avanzar a mi ritmo sin que cuente para la nota.

- **Requisitos:** RF-22, RN-19. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado **Bloques** (24 niveles), cuando ejecuto o avanzo paso a paso, entonces veo al robot moverse; los niveles van de secuencias y rutas a ciclos, ciclos anidados y condiciones, y 7 de depuración empiezan con un programa que falla.
  2. Dado **¿Qué imprime?** (21 niveles), **Caza el error** (14) y **Ordena el algoritmo** (14), cuando respondo, entonces se comprueba en el navegador; incluyen niveles de textos y matrices.
  3. Dado **Caza el error**, cuando marco la línea correcta, entonces elijo la corrección entre tres opciones y veo la explicación.
  4. Dado **Completa el DFD** (14 niveles), cuando completo las casillas, entonces se comprueba el diagrama; del 11 al 14 tienen ciclos (rombo con flecha de regreso).
  5. Dado **¿Qué hace este código?** de Arduino (12 niveles), cuando leo el programa, entonces elijo entre cuatro descripciones de lo que pasa en la placa (LED, botón, sensor, Monitor Serie).
  6. Dado el juego de bloques, cuando elijo el personaje del robot (flecha, lápiz, cerebro o fantasma), entonces se ve hacia dónde mira y el texto de posición no cambia.

### HU-32 Editar los niveles de los juegos

Como **docente** quiero crear o cambiar niveles de `/juegos` para ajustarlos a mi grupo.

- **Requisitos:** RF-22. **Estado:** **Pendiente** (no pedido). Los niveles están fijos en el código; la alternativa disponible es el reto de bloques con nota (HU-24).

## Épica 8 · Notas y planilla

### HU-33 Ver mis notas y mi avance

Como **estudiante** quiero ver mis notas, mi avance parcial y mi nota definitiva por separado para saber cómo voy.

- **Requisitos:** RF-19, RF-20, RN-06, RN-07. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado mis actividades calificadas, cuando abro **Mis notas**, entonces veo la nota de cada una y el avance parcial (promedio con porcentajes de lo que ya tiene resultado).
  2. Dado que no todas las actividades tienen resultado o los porcentajes no suman 100 %, cuando abro **Mis notas**, entonces la nota definitiva aparece vacía con la explicación.
  3. Dado una actividad vencida sin entrega, cuando la veo, entonces aparece como 0,0 marcado con `*`.
  4. Dado una nota de la IA en revisión, cuando la consulto, entonces veo "En revisión" y no la propuesta ni la confianza.

### HU-34 Planilla del curso y exportación a Excel

Como **docente** quiero la planilla con los porcentajes y exportarla a Excel para entregar notas a la Universidad.

- **Requisitos:** RF-18, RN-07 (P-07). **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado actividades con y sin porcentaje, cuando abro la planilla, entonces las explícitas pesan su porcentaje y el resto se reparte por igual entre las que no tienen.
  2. Dado que los porcentajes explícitos ya suman 100 %, cuando hay actividades sin porcentaje, entonces valen 0 % y la planilla lo avisa.
  3. Dado que pulso **Exportar a Excel (.xlsx)**, cuando abro el archivo, entonces las notas y porcentajes son números y coinciden con la pantalla.
  4. Dado que pulso **Exportar a CSV**, cuando lo abro en Excel en español, entonces se lee directo (UTF-8 con BOM, `;`, coma decimal).
  5. Dado un estudiante inscrito, cuando abre la planilla, entonces solo ve su fila.

## Épica 9 · Foro

### HU-35 Preguntar en el foro

Como **estudiante** quiero abrir hilos y responder por herramienta para resolver dudas con compañeros y docentes.

- **Requisitos:** RF-26. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado que no tengo sesión, cuando abro el **Foro**, entonces puedo leer pero no escribir.
  2. Dado que tengo sesión, cuando abro un hilo con herramienta, título y texto, entonces aparece con mi nombre, nunca con mi cédula ni mi correo.
  3. Dado un hilo cerrado, cuando intento responder, entonces se rechaza.

### HU-36 Moderar el foro

Como **docente** o **administrador** quiero fijar, cerrar o eliminar hilos y respuestas para mantener el foro útil y respetuoso.

- **Requisitos:** RF-27, RN-15. **Estado:** Implementado.
- **Criterios de aceptación:**
  1. Dado un hilo, cuando pulso **Fijar arriba**, entonces aparece primero en la lista.
  2. Dado un hilo, cuando pulso **Cerrar a respuestas**, entonces nadie puede responder hasta que lo reabra.
  3. Dado un hilo o una respuesta, cuando lo elimino, entonces deja de verse y queda registrado quién y cuándo.
  4. Dado un estudiante, cuando abre un hilo, entonces no ve las acciones de moderación y la API se las niega.

## Épica 10 · Instalación y operación

### HU-37 Usar LogiCode desde los celulares del aula por HTTPS

Como **responsable técnico** quiero servir LogiCode por HTTPS en la red del aula para que el piloto sea seguro.

- **Requisitos:** RNF-02 (Etapa 10). **Estado:** Implementado; el piloto con el docente y el acta firmada están **Pendientes**.
- **Criterios de aceptación:**
  1. Dado el modo piloto, cuando abro la dirección del servidor por HTTP, entonces me redirige a HTTPS.
  2. Dado el modo piloto, cuando inicio sesión, entonces las cookies salen con `Secure` y la documentación de la API no responde.
  3. Dado una instalación en modo piloto, cuando intento cargar los datos demo, entonces el script se niega salvo autorización explícita.
