import { navigate } from '../../shared/router/router.js'
import ArduinoEmulator from './ArduinoEmulator.jsx'
import FlowEmulator from './FlowEmulator.jsx'
import PseintEmulator from './PseintEmulator.jsx'

// Un emulador por herramienta de los temas (RF-06): PSeInt, DFD, Scratch y Arduino. Es práctica libre: no hay rúbrica,
// nota ni entrega; al ejecutar se dice si el programa compila y si corrió sin errores.
const TOOLS = [
  { key: 'pseint', label: 'PSeInt', render: () => <PseintEmulator /> },
  { key: 'dfd', label: 'DFD', render: () => <FlowEmulator mode="dfd" /> },
  { key: 'scratch', label: 'Scratch', render: () => <FlowEmulator mode="scratch" /> },
  { key: 'arduino', label: 'Arduino', render: () => <ArduinoEmulator /> },
]

export default function EmulatorPage({ tool }) {
  const current = TOOLS.find((item) => item.key === tool) ?? TOOLS[0]
  return (
    <section className="card card--wide" aria-labelledby="emulador-titulo">
      <h1 id="emulador-titulo">Emulador</h1>
      <p className="page__note">
        Práctica libre: escribe o arma tu programa y ejecútalo. No tiene nota ni se entrega; lo guardas en tu equipo si quieres.
      </p>
      <div className="tabs" role="tablist" aria-label="Lenguajes">
        {TOOLS.map((item) => (
          <button
            key={item.key}
            type="button"
            role="tab"
            id={`emulador-${item.key}`}
            aria-selected={item.key === current.key}
            aria-controls="emulador-panel"
            onClick={() => navigate(`/emulador/${item.key}`, { replace: true })}
          >
            {item.label}
          </button>
        ))}
      </div>
      <div key={current.key} id="emulador-panel" role="tabpanel" aria-labelledby={`emulador-${current.key}`} className="emulator">
        {current.render()}
      </div>
    </section>
  )
}
