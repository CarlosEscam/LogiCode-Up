#!/usr/bin/env python3
"""Genera el documento único de LogiCode (.docx y .pdf) a partir de los .md del repositorio.

Uso: python3 docs/documento-completo/generar.py <carpeta-de-salida>
Necesita pandoc, LibreOffice (soffice, con su módulo uno de Python), Pillow y mermaid-cli
(`npx @mermaid-js/mermaid-cli`, o la ruta de mmdc en la variable MMDC).
"""
import os, re, shutil, subprocess, sys, tempfile, time, urllib.parse
from pathlib import Path
from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
DOCS = [  # orden del documento: índice, visión, técnica, uso, operación, estado y anexos
    'docs/README.md', 'docs/vision-general.md', 'docs/arquitectura.md', 'docs/modelo-de-datos.md',
    'docs/api/README.md', 'docs/permisos.md', 'docs/casos-de-uso.md', 'docs/historias-de-usuario.md',
    'docs/instalacion.md', 'docs/piloto.md', 'docs/manuales/manual-administrador.md',
    'docs/manuales/manual-docente.md', 'docs/manuales/manual-estudiante.md', 'docs/manuales/manual-tecnico.md',
    'datos-demo/README.md', 'respaldos/README.md', 'docs/calidad.md', 'docs/decisiones.md', 'docs/estado.md',
    'ia-pruebas/README.md', 'migracion/Guia-pasar-LogiCode-a-otro-PC.md',
]
ANCHO_MAX, ALTO_MAX = 6.0, 8.0  # pulgadas útiles de una hoja carta, dejando sitio al título de la sección

def slug_doc(ruta): return re.sub(r'[^a-z0-9]+', '-', ruta.lower().removesuffix('.md')).strip('-')

def slug_github(texto):
    texto = re.sub(r'\[([^\]]*)\]\([^)]*\)', r'\1', texto)
    texto = re.sub(r'[^\w\- ]', '', texto.lower().strip())
    return texto.replace(' ', '-')

def bloques(lineas):
    """Devuelve (línea, dentro_de_código, cerca) para cada línea."""
    cerca = None
    for l in lineas:
        m = re.match(r'^\s*(```+|~~~+)', l)
        if cerca is None and m:
            cerca = m.group(1); yield l, True, 'abre'; continue
        if cerca and l.strip().startswith(cerca) and l.strip().strip(cerca[0]) == '':
            cerca = None; yield l, True, 'cierra'; continue
        yield l, cerca is not None, None

def anclas(ruta):
    """Slugs de GitHub de los títulos de un documento, en orden y sin repetir (como GitHub)."""
    vistos, salida = {}, []
    for l, codigo, _ in bloques((RAIZ / ruta).read_text(encoding='utf-8').splitlines()):
        m = None if codigo else re.match(r'^(#{1,6})\s+(.*?)\s*#*\s*$', l)
        if m:
            s = slug_github(m.group(2)); n = vistos.get(s, 0); vistos[s] = n + 1
            salida.append(s if n == 0 else f'{s}-{n}')
    return salida

def mermaid(codigo, destino):
    mmdc = os.environ.get('MMDC', 'npx -y @mermaid-js/mermaid-cli').split()
    fuente = destino.with_suffix('.mmd'); fuente.write_text(codigo, encoding='utf-8')
    extra = ['-p', os.environ['PUPPETEER_CONFIG']] if os.environ.get('PUPPETEER_CONFIG') else []
    subprocess.run(mmdc + extra + ['-i', str(fuente), '-o', str(destino), '-s', '2', '-b', 'white'],
                   check=True, capture_output=True)

def imagen(alt, ruta):
    w, h = Image.open(ruta).size
    ancho = min(ANCHO_MAX, ALTO_MAX * w / h)
    return f'![{alt}]({ruta}){{width={ancho:.2f}in}}'

def preparar(ruta, ids, temp, avisos):
    doc = slug_doc(ruta); base = (RAIZ / ruta).parent
    lista_anclas = iter(anclas(ruta)); salida = []; mmd = None; n_mmd = 0

    def enlace(m):
        texto, destino = m.group(1), m.group(2)
        if re.match(r'^[a-z]+:', destino): return m.group(0)
        camino, _, ancla = destino.partition('#')
        ancla = urllib.parse.unquote(ancla)
        if camino:
            objetivo = os.path.relpath((base / urllib.parse.unquote(camino)).resolve(), RAIZ)
            if objetivo not in DOCS: return texto  # archivo que no va en el documento: queda el texto
            otro = slug_doc(objetivo)
        else:
            otro = doc
        id_ = f'{otro}--{ancla}' if ancla else ids[otro]
        if id_ not in ids['*']:
            avisos.append(f'{ruta}: ancla inexistente {destino}'); id_ = ids[otro]
        return f'[{texto}](#{id_})'

    for l, codigo, marca in bloques((RAIZ / ruta).read_text(encoding='utf-8').splitlines()):
        if mmd is not None:
            if marca == 'cierra':
                n_mmd += 1; png = temp / f'{doc}-{n_mmd}.png'; mermaid('\n'.join(mmd), png)
                salida.append(imagen('', png)); mmd = None
            else:
                mmd.append(l)
            continue
        if marca == 'abre' and l.strip().lstrip('`~').strip() == 'mermaid':
            mmd = []; continue
        if not codigo:
            t = re.match(r'^(#{1,6})\s+(.*?)\s*#*\s*$', l)
            if t:
                l = f'{t.group(1)} {t.group(2)} {{#{doc}--{next(lista_anclas)}}}'
            l = re.sub(r'!\[([^\]]*)\]\(([^)\s]+)\)',
                       lambda m: m.group(0) if re.match(r'^[a-z]+:', m.group(2))
                       else imagen(m.group(1), (base / urllib.parse.unquote(m.group(2))).resolve()), l)
            l = re.sub(r'<img\s+src="([^"]+)"\s+alt="([^"]*)"[^>]*>',
                       lambda m: imagen(m.group(2), (base / m.group(1)).resolve()), l)
            l = re.sub(r'(?<!!)\[([^\]]*)\]\(([^)\s]+)\)', enlace, l)
        salida.append(l)
    return '\n'.join(salida)

def libreoffice(docx, salida):
    """Llena la tabla de contenido y guarda el .docx y el .pdf con LibreOffice."""
    import uno
    from com.sun.star.beans import PropertyValue
    def props(**kw):
        r = []
        for k, v in kw.items():
            p = PropertyValue(); p.Name, p.Value = k, v; r.append(p)
        return tuple(r)
    perfil = tempfile.mkdtemp()
    proc = subprocess.Popen(['soffice', '--headless', '--norestore', f'-env:UserInstallation=file://{perfil}',
                             '--accept=pipe,name=logicodedoc;urp;'])
    try:
        ctx = uno.getComponentContext()
        resolver = ctx.ServiceManager.createInstanceWithContext('com.sun.star.bridge.UnoUrlResolver', ctx)
        for _ in range(60):
            try:
                remoto = resolver.resolve('uno:pipe,name=logicodedoc;urp;StarOffice.ComponentContext'); break
            except Exception:
                time.sleep(1)
        else:
            sys.exit('LibreOffice no respondió')
        escritorio = remoto.ServiceManager.createInstanceWithContext('com.sun.star.frame.Desktop', remoto)
        d = escritorio.loadComponentFromURL(uno.systemPathToFileUrl(str(docx)), '_blank', 0, props(Hidden=True))
        for _ in range(2):  # dos pasadas: la tabla de contenido ocupa páginas y corre los números
            indices = d.getDocumentIndexes()
            for i in range(indices.getCount()): indices.getByIndex(i).update()
        d.storeToURL(uno.systemPathToFileUrl(str(salida.with_suffix('.docx'))), props(FilterName='MS Word 2007 XML'))
        d.storeToURL(uno.systemPathToFileUrl(str(salida.with_suffix('.pdf'))), props(FilterName='writer_pdf_Export'))
        d.close(True)
    finally:
        proc.terminate(); proc.wait(); shutil.rmtree(perfil, ignore_errors=True)

def main():
    destino = Path(sys.argv[1]).resolve(); destino.mkdir(parents=True, exist_ok=True)
    temp = Path(tempfile.mkdtemp())
    ids = {slug_doc(r): f'{slug_doc(r)}--{anclas(r)[0]}' for r in DOCS}
    ids['*'] = {f'{slug_doc(r)}--{a}' for r in DOCS for a in anclas(r)}
    avisos = []
    partes = [preparar(r, ids, temp, avisos) for r in DOCS]
    commit = subprocess.run(['git', '-C', str(RAIZ), 'rev-parse', '--short', 'HEAD'],
                            capture_output=True, text=True).stdout.strip()
    meses = 'enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre'.split()
    hoy = time.localtime(); fecha = f'{hoy.tm_mday} de {meses[hoy.tm_mon - 1]} de {hoy.tm_year}'
    md = temp / 'documento.md'
    md.write_text('\n\n'.join(partes), encoding='utf-8')
    docx = temp / 'documento.docx'
    subprocess.run(['pandoc', str(md), '-f', 'gfm+attributes', '-o', str(docx), '--toc', '--toc-depth=2',
                    '-M', 'title=LogiCode · Documentación completa', '-M', f'subtitle=Estado al {fecha} (commit {commit})',
                    '-M', 'toc-title=Contenido', '-M', 'lang=es-CO', f'--resource-path={RAIZ}'], check=True)
    libreoffice(docx, destino / 'LogiCode-Documentacion-completa')
    for a in avisos: print('AVISO', a)
    shutil.rmtree(temp, ignore_errors=True)
    print('Listo:', destino / 'LogiCode-Documentacion-completa.docx', 'y .pdf')

if __name__ == '__main__':
    main()
