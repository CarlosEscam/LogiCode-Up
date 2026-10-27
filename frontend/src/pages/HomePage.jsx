import SystemStatus from '../features/system/SystemStatus.jsx'
import { useSession } from '../features/auth/useSession.js'
import { Link } from '../shared/router/Link.jsx'

// Página de inicio (RF-23): misión, visión y explicación de la plataforma.
// La misión y la visión se redactaron con el propósito de la especificación; el equipo puede ajustar el texto aquí.
export default function HomePage() {
  const { account } = useSession()

  return (
    <>
      <header className="page__header">
        <h1>LogiCode</h1>
        <p className="page__subtitle">
          Plataforma de Pensamiento Computacional · Universidad de Pamplona
        </p>
        <p>
          Contenidos, práctica, entregas, notas y foro del curso de primer semestre en un solo lugar, desde el
          computador o el celular.
        </p>
      </header>

      {account ? (
        <p className="card">
          Hola, {account.fullName.split(' ')[0]}. <Link to="/cursos">Ir a mis cursos</Link>
        </p>
      ) : (
        <section aria-labelledby="acceso-titulo" className="card">
          <h2 id="acceso-titulo">Entrar a LogiCode</h2>
          <ul className="links">
            <li>
              <Link to="/ingresar">Iniciar sesión</Link> con tu cédula
            </li>
            <li>
              <Link to="/registro/estudiante">Registrarme como estudiante</Link> (tu docente debe haber habilitado tu
              cédula)
            </li>
            <li>
              <Link to="/registro/docente">Solicitar cuenta de docente</Link>
            </li>
          </ul>
          <p className="page__note">Sin cuenta puedes usar la biblioteca pública, los juegos y leer el foro.</p>
        </section>
      )}

      <div className="home-grid">
        <section aria-labelledby="mision-titulo" className="card">
          <h2 id="mision-titulo">Misión</h2>
          <p>
            Acompañar a los estudiantes de primer semestre en el aprendizaje del pensamiento computacional, reuniendo
            los temas, la práctica y la evaluación del curso con retroalimentación oportuna y siempre bajo el criterio
            del docente.
          </p>
        </section>
        <section aria-labelledby="vision-titulo" className="card">
          <h2 id="vision-titulo">Visión</h2>
          <p>
            Ser el espacio donde los estudiantes de la Universidad de Pamplona aprenden y practican la lógica de
            programación desde su primer semestre, y crecer a más cursos con evaluaciones claras y explicadas.
          </p>
        </section>
      </div>

      <section aria-labelledby="como-titulo" className="card">
        <h2 id="como-titulo">Qué puedes hacer</h2>
        <dl className="home-features">
          <div>
            <dt>Aprender</dt>
            <dd>
              Temas de PSeInt, diagramas de flujo (DFD), Scratch y Arduino con guías, presentaciones y videos.{' '}
              <Link to="/biblioteca">Ver la biblioteca</Link>
            </dd>
          </div>
          <div>
            <dt>Practicar</dt>
            <dd>
              Retos de bloques con secuencias, ciclos, condiciones, rutas y depuración, quizzes de repaso y un editor
              de PSeInt que corre en el navegador. <Link to="/juegos">Jugar</Link>
            </dd>
          </div>
          <div>
            <dt>Entregar y ver tus notas</dt>
            <dd>
              Cada actividad trae su rúbrica y se califica de 0,0 a 5,0. Los programas de PSeInt y los quizzes se
              califican al instante; las fotos y archivos los puede calificar una inteligencia artificial con la rúbrica,
              y el docente revisa lo que no se lee bien. Tu nota definitiva se muestra aparte de tu avance.
            </dd>
          </div>
          <div>
            <dt>Preguntar</dt>
            <dd>
              Un foro por herramienta donde responden compañeros y docentes. <Link to="/foro">Ir al foro</Link>
            </dd>
          </div>
        </dl>
      </section>

      {!account && (
        <section aria-labelledby="empezar-titulo" className="card">
          <h2 id="empezar-titulo">Cómo empezar si eres estudiante</h2>
          <ol>
            <li>Tu docente habilita tu cédula en el curso.</li>
            <li>Te registras con tu cédula, tu correo y una contraseña.</li>
            <li>Confirmas el correo con el enlace que te llega.</li>
            <li>Inicias sesión y entras a tu curso.</li>
          </ol>
        </section>
      )}

      <section aria-labelledby="estado-titulo" className="card">
        <h2 id="estado-titulo">Estado de la plataforma</h2>
        <SystemStatus />
      </section>
    </>
  )
}
