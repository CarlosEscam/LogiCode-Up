import SystemStatus from '../features/system/SystemStatus.jsx'

// Página inicial provisional de la fundación técnica (Etapa 2).
// La página de inicio definitiva con misión, visión y explicación (RF-23) está pendiente.
export default function HomePage() {
  return (
    <main className="page">
      <header className="page__header">
        <h1>LogiCode</h1>
        <p className="page__subtitle">
          Plataforma de Pensamiento Computacional · Universidad de Pamplona
        </p>
      </header>

      <section aria-labelledby="estado-titulo" className="card">
        <h2 id="estado-titulo">Estado de la plataforma</h2>
        <SystemStatus />
      </section>

      <p className="page__note">
        Fundación técnica en construcción. Las funciones de la plataforma se habilitarán por etapas.
      </p>
    </main>
  )
}
