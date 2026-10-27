// Iconos de línea del menú y de la página de inicio. Son decorativos: el texto al lado dice lo que hacen.
const PATHS = {
  inicio: 'M3 11 12 4l9 7v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z',
  biblioteca: 'M4 5a2 2 0 0 1 2-2h13v15H6a2 2 0 0 0-2 2zm0 15a2 2 0 0 0 2 2h13v-4',
  juegos: 'M7 8h10a5 5 0 0 1 0 10c-1.6 0-2.4-1-3-2h-4c-.6 1-1.4 2-3 2A5 5 0 0 1 7 8zM8 11v4M6 13h4M16 12h.01M18 14.5h.01',
  foro: 'M4 5h16v11H9l-5 4z',
  pseint: 'M8 7l-5 5 5 5M16 7l5 5-5 5M14 4l-4 16',
  cursos: 'M2 9l10-5 10 5-10 5zM6 11v5c3 2.5 9 2.5 12 0v-5M22 9v6',
  notas: 'M9 4h6M9 4a1 1 0 0 0-1 1v1h8V5a1 1 0 0 0-1-1M8 5H6a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1h-2M9 13l2 2 4-4',
  usuarios: 'M9 4.5a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7M2.5 20c.8-3.6 3.4-5.5 6.5-5.5s5.7 1.9 6.5 5.5M16 4.8a3.5 3.5 0 0 1 0 6.4M18 14.8c1.8.7 3 2.4 3.5 5.2',
  admin: 'M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z',
  auditoria: 'M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01',
}

export function Icon({ name }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <path d={PATHS[name]} fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

// Logo de LogiCode: los signos de código dentro de un círculo.
export function Logo() {
  return (
    <svg className="topbar__logo" viewBox="0 0 40 40" aria-hidden="true" focusable="false">
      <circle cx="20" cy="20" r="19" fill="#eee6f5" stroke="#56378a" strokeWidth="2" />
      <path d="M14 13 L8 20 L14 27 M26 13 L32 20 L26 27 M22.5 11 L17.5 29" fill="none" stroke="#56378a" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}
