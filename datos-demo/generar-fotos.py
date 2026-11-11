# Genera las fotos sintéticas de cuaderno de muestras/curso/ a partir de curso-completo.json.
# Ya están en el repositorio: solo hace falta correrlo si cambias las actividades. Requiere Pillow.
#   python datos-demo/generar-fotos.py
# Por actividad y variante (correcta, error, incompleta) salen dos fotos: "legible" (hoja algo girada,
# letra a mano) y "borrosa" (oscura y desenfocada, para el camino de foto ilegible).
import json
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = Path(__file__).parent
OUT = HERE / 'muestras' / 'curso'
FONTS = ['C:/Windows/Fonts/Inkfree.ttf', 'C:/Windows/Fonts/comici.ttf', 'DejaVuSans-Oblique.ttf']


def font(size):
    for name in FONTS:
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            pass
    return ImageFont.load_default()


def page(lines, seed):
    rnd = random.Random(seed)
    w, h, gap = 900, 1100, 52
    img = Image.new('RGB', (w, h), (250, 247, 236))
    d = ImageDraw.Draw(img)
    for y in range(120, h, gap):
        d.line([(0, y), (w, y + rnd.randint(-3, 3))], fill=(160, 190, 230), width=2)
    d.line([(80, 0), (84, h)], fill=(230, 140, 140), width=2)
    f = font(36)
    ink = (25, 35, 95)
    for i, line in enumerate(lines):
        x = 100 + rnd.randint(-4, 4)
        y = 120 + i * gap - 44 + rnd.randint(-3, 3)
        for ch in line.replace('\t', '  '):
            d.text((x, y + rnd.randint(-2, 2)), ch, font=f, fill=ink)
            x += d.textlength(ch, font=f) + rnd.uniform(-0.5, 1.2)
    return img.rotate(rnd.uniform(-3, 3), expand=True, fillcolor=(85, 75, 65))


def blurry(img):
    img = img.filter(ImageFilter.GaussianBlur(6))
    return Image.eval(img, lambda v: int(v * 0.45))


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    data = json.loads((HERE / 'curso-completo.json').read_text(encoding='utf-8'))
    for k, activity in enumerate(data['actividades']):
        for v, (variant, lines) in enumerate(activity['variantes'].items()):
            img = page(lines, seed=k * 10 + v)
            img.save(OUT / f"{activity['clave']}-{variant}-legible.jpg", quality=72)
            blurry(img).save(OUT / f"{activity['clave']}-{variant}-borrosa.jpg", quality=60)
    print(f'Fotos en {OUT}')


if __name__ == '__main__':
    main()
