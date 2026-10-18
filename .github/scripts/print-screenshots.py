"""Prints a small contact sheet of UI-test screenshots into the job log as
base64, so they can be reviewed from environments that can't download
artifacts. The full-size images are uploaded as an artifact as well."""
import base64
import io
import pathlib
import sys

from PIL import Image

WIDTH = 300
paths = sorted(pathlib.Path(sys.argv[1]).glob("*.png"))
if not paths:
    print("No screenshots found")
    sys.exit(0)

thumbs = []
for path in paths:
    image = Image.open(path).convert("RGB")
    thumbs.append(image.resize((WIDTH, round(image.height * WIDTH / image.width)), Image.LANCZOS))

gap = 8
sheet = Image.new("RGB", (len(thumbs) * (WIDTH + gap) - gap, max(t.height for t in thumbs)), "white")
for index, thumb in enumerate(thumbs):
    sheet.paste(thumb, (index * (WIDTH + gap), 0))

buffer = io.BytesIO()
sheet.save(buffer, "JPEG", quality=60, optimize=True)
data = base64.b64encode(buffer.getvalue()).decode()
print("SHEET-BEGIN " + " ".join(p.name for p in paths))
for start in range(0, len(data), 16000):
    print("SHEET:" + data[start:start + 16000])
print("SHEET-END")
