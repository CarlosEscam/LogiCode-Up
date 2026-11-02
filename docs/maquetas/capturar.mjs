// Guarda cada maqueta de fuente.html como docs/maquetas/<id>.png.
// Uso: node docs/maquetas/capturar.mjs  (necesita Playwright con Chromium: npm i -g playwright && npx playwright install chromium)
import { chromium } from 'playwright'
import { dirname, join } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const carpeta = dirname(fileURLToPath(import.meta.url))
const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1200, height: 900 }, deviceScaleFactor: 2 })
await page.goto(pathToFileURL(join(carpeta, 'fuente.html')).href)
const marcos = await page.$$('section.frame')
for (const marco of marcos) {
  const id = await marco.getAttribute('id')
  await marco.screenshot({ path: join(carpeta, `${id}.png`) })
  console.log(`${id}.png`)
}
await browser.close()
if (marcos.length === 0) throw new Error('fuente.html no tiene maquetas')
