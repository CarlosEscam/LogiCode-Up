import SystemStatus from '../features/system/SystemStatus.jsx'
import { formatDateTime, timeLeft } from '../features/activities/labels.js'
import { useSession } from '../features/auth/useSession.js'
import { useQuery } from '../shared/api/usePagedQuery.js'
import { BrainMascot } from '../features/games/skins.jsx'
import { Link } from '../shared/router/Link.jsx'
import { Icon } from '../shared/ui/Icon.jsx'

// Página de inicio (RF-23): misión, visión y explicación de la plataforma.
// La misión y la visión se redactaron con el propósito de la especificación; el equipo puede ajustar el texto aquí.
export default function HomePage() {
  const { account, can } = useSession()

  return (
    <>
      <section className="page-head hero" aria-labelledby="inicio-titulo">
        <div className="hero__text">
          <p className="eyebrow">Plataforma de Pensamiento Computacional · Universidad de Pamplona</p>
          <h1 id="inicio-titulo">LogiCode</h1>
          <p className="hero__lead">
            Contenidos, práctica, entregas, notas y foro del curso de primer semestre en un solo lugar, desde el
            computador o el celular.
          </p>
          {account ? (
            <p className="hero__cta">
              Hola, {account.fullName.split(' ')[0]}.{' '}
              <Link to="/cursos" className="button">
                Ir a mis cursos
              </Link>
            </p>
          ) : (
            <section aria-labelledby="acceso-titulo">
              <h2 id="acceso-titulo" className="visually-hidden">
                Entrar a LogiCode
              </h2>
              <div className="hero__cta">
                <Link to="/ingresar" className="button">
                  Iniciar sesión
                </Link>
                <Link to="/registro/estudiante" className="button button--secondary">
                  Registrarme como estudiante
                </Link>
              </div>
              <p className="hero__aside">
                Inicia sesión con tu cédula. Para registrarte, tu docente debe haber habilitado tu cédula. ¿Eres docente?{' '}
                <Link to="/registro/docente">Solicitar cuenta de docente</Link>.
                <br />
                Sin cuenta puedes usar la biblioteca pública, los juegos y leer el foro.
              </p>
            </section>
          )}
        </div>
        <div className="hero__art" aria-hidden="true">
          <pre className="hero__arch">
            <b>Algoritmo</b> promedio{'\n'}
            {'  '}
            <b>Definir</b> n1, n2 <b>Como</b> Real{'\n'}
            {'  '}
            <b>Leer</b> n1, n2{'\n'}
            {'  '}
            <b>Escribir</b> <i>"Nota: "</i>, (n1+n2)/2{'\n'}
            <b>FinAlgoritmo</b>
          </pre>
          <div className="mascot">
            <BrainMascot />
          </div>
        </div>
      </section>

      {can('ANSWER_ACTIVITIES') && <MyPending />}

      <section aria-labelledby="como-titulo" className="home-section">
        <h2 id="como-titulo">Qué puedes hacer</h2>
        <dl className="home-features">
          <div>
            <Icon name="biblioteca" />
            <dt>Aprender</dt>
            <dd>
              Temas de PSeInt, diagramas de flujo (DFD), Scratch y Arduino con guías, presentaciones y videos.{' '}
              <Link to="/biblioteca">Ver la biblioteca</Link>
            </dd>
          </div>
          <div>
            <Icon name="juegos" />
            <dt>Practicar</dt>
            <dd>
              Retos de bloques con secuencias, ciclos, condiciones, rutas y depuración, quizzes de repaso y un editor
              de PSeInt que corre en el navegador. <Link to="/juegos">Jugar</Link>
            </dd>
          </div>
          <div>
            <Icon name="notas" />
            <dt>Entregar y ver tus notas</dt>
            <dd>
              Cada actividad trae su rúbrica y se califica de 0,0 a 5,0. Los programas de PSeInt y los quizzes se
              califican al instante; las fotos y archivos los puede calificar una inteligencia artificial con la rúbrica,
              y el docente revisa lo que no se lee bien. Tu nota definitiva se muestra aparte de tu avance.
            </dd>
          </div>
          <div>
            <Icon name="foro" />
            <dt>Preguntar</dt>
            <dd>
              Un foro por herramienta donde responden compañeros y docentes. <Link to="/foro">Ir al foro</Link>
            </dd>
          </div>
        </dl>
      </section>

      {!account && (
        <section aria-labelledby="empezar-titulo" className="home-section">
          <h2 id="empezar-titulo">Cómo empezar si eres estudiante</h2>
          <ol className="home-steps">
            <li>Tu docente habilita tu cédula en el curso.</li>
            <li>Te registras con tu cédula, tu correo y una contraseña.</li>
            <li>Confirmas el correo con el enlace que te llega.</li>
            <li>Inicias sesión y entras a tu curso.</li>
          </ol>
        </section>
      )}

      <div className="home-grid home-section">
        <section aria-labelledby="mision-titulo">
          <h2 id="mision-titulo">Misión</h2>
          <p>
            Acompañar a los estudiantes de primer semestre en el aprendizaje del pensamiento computacional, reuniendo
            los temas, la práctica y la evaluación del curso con retroalimentación oportuna y siempre bajo el criterio
            del docente.
          </p>
        </section>
        <section aria-labelledby="vision-titulo">
          <h2 id="vision-titulo">Visión</h2>
          <p>
            Ser el espacio donde los estudiantes de la Universidad de Pamplona aprenden y practican la lógica de
            programación desde su primer semestre, y crecer a más cursos con evaluaciones claras y explicadas.
          </p>
        </section>
      </div>

      {/* Dato técnico: lo ven los visitantes (para revisar la instalación) y los administradores. */}
      {(!account || can('MANAGE_USERS')) && (
        <section aria-labelledby="estado-titulo" className="home-status">
          <h2 id="estado-titulo">Estado de la plataforma</h2>
          <SystemStatus />
        </section>
      )}
    </>
  )
}

// Actividades que el estudiante aún puede entregar, curso por curso.
function MyPending() {
  const courses = useQuery('/courses')
  if (!courses.data?.length) return null
  return (
    <section aria-labelledby="pendientes-titulo" className="home-section">
      <h2 id="pendientes-titulo">Tus pendientes</h2>
      <div className="home-pending">
        {courses.data.map((course) => (
          <CoursePending key={course.id} course={course} />
        ))}
      </div>
    </section>
  )
}

function CoursePending({ course }) {
  const activities = useQuery(`/courses/${course.id}/activities`)
  const todo = (activities.data ?? []).filter((activity) => activity.progress?.state === 'OPEN')
  return (
    <section className="card" aria-label={course.name}>
      <h3 className="material__title">
        <Link to={`/cursos/${course.id}?seccion=actividades`}>{course.name}</Link>
      </h3>
      {activities.data && todo.length === 0 && <p className="page__note">Nada pendiente en este curso.</p>}
      {todo.length > 0 && (
        <ul className="links">
          {todo.map((activity) => (
            <li key={activity.id}>
              <Link to={`/actividades/${activity.id}`}>{activity.title}</Link>{' '}
              <span className="material__meta">
                · {timeLeft(activity.progress.deadline) ?? `cierra ${formatDateTime(activity.progress.deadline)}`}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
