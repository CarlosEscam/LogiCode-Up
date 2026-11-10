import { useEffect, useState } from 'react'

// Placas por fabricante (usbVendorId) y modelo (usbProductId), como las reconoce el Arduino IDE. Los chips de los clones
// (CH340, CP210x, FTDI) no dicen el modelo de la placa.
const VENDORS = {
  0x2341: { name: 'Arduino', products: { 0x0001: 'Uno', 0x0043: 'Uno', 0x0243: 'Uno', 0x0010: 'Mega 2560', 0x0042: 'Mega 2560', 0x8036: 'Leonardo', 0x8037: 'Micro', 0x0058: 'Nano Every', 0x0069: 'Uno R4 Minima', 0x1002: 'Uno R4 WiFi' } },
  0x2a03: { name: 'Arduino', products: { 0x0043: 'Uno', 0x0042: 'Mega 2560', 0x8036: 'Leonardo' } },
  0x1a86: { name: 'Placa compatible con chip CH340 (Uno o Nano genérico)' },
  0x10c4: { name: 'Placa con chip CP210x (compatible o ESP32)' },
  0x0403: { name: 'Placa con chip FTDI (Nano antiguo o Pro Mini)' },
}
const FILTERS = Object.keys(VENDORS).map((id) => ({ usbVendorId: Number(id) }))

function describe(port) {
  const { usbVendorId, usbProductId } = port.getInfo()
  const vendor = VENDORS[usbVendorId]
  if (!vendor) return 'Placa USB'
  const model = vendor.products?.[usbProductId]
  return model ? `${vendor.name} ${model}` : vendor.products ? `${vendor.name} (modelo sin identificar)` : vendor.name
}

// Detecta las placas Arduino conectadas por USB al equipo con Web Serial (Chrome y Edge en computador). La primera vez
// el navegador pide permiso para cada placa; después las reconoce solas al conectarlas y desconectarlas.
export default function UsbBoards() {
  const supported = typeof navigator !== 'undefined' && 'serial' in navigator
  const [ports, setPorts] = useState([])
  const [message, setMessage] = useState(null)

  useEffect(() => {
    if (!supported) return undefined
    const { serial } = navigator
    const refresh = () =>
      serial
        .getPorts()
        .then(setPorts)
        .catch(() => setPorts([]))
    refresh()
    serial.addEventListener('connect', refresh)
    serial.addEventListener('disconnect', refresh)
    return () => {
      serial.removeEventListener('connect', refresh)
      serial.removeEventListener('disconnect', refresh)
    }
  }, [supported])

  async function connect() {
    setMessage(null)
    try {
      const port = await navigator.serial.requestPort({ filters: FILTERS })
      setPorts((current) => (current.includes(port) ? current : [...current, port]))
    } catch (error) {
      // NotFoundError: la persona cerró la ventana sin elegir.
      if (error?.name !== 'NotFoundError') setMessage('El navegador no dejó usar la placa. Revisa el cable y que ningún otro programa (como el Arduino IDE) la tenga abierta.')
    }
  }

  return (
    <div className="usb-boards" role="group" aria-label="Placas conectadas por USB">
      {supported ? (
        <>
          <button type="button" className="button button--secondary button--small" onClick={connect}>
            🔌 Detectar placa USB
          </button>
          <span role="status" className="usb-boards__list">
            {ports.length === 0
              ? 'Ninguna placa conectada'
              : ports.map((port, index) => (
                  <span key={index} className="usb-boards__chip">
                    <span className="arduino-ide__dot" data-on="" aria-hidden="true" /> {describe(port)} conectada
                  </span>
                ))}
          </span>
        </>
      ) : (
        <span className="page__note">Para detectar placas por USB abre LogiCode en Chrome o Edge en un computador.</span>
      )}
      {message && <span className="field__error">{message}</span>}
    </div>
  )
}
