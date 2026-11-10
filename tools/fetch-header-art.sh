#!/usr/bin/env bash
# Descarga la ilustración de la cabecera (URL en tools/header-art-url.txt),
# la optimiza a WebP y la guarda en el repositorio. Lo ejecuta GitHub Actions,
# que sí tiene acceso a internet. Si la imagen ya está, no hace nada.
set -euo pipefail
URL_FILE=tools/header-art-url.txt
OUT=app/src/main/res/drawable-nodpi/header_art.webp
STAMP=tools/.header-art-fetched
[ -f "$URL_FILE" ] || exit 0
URL=$(head -n1 "$URL_FILE" | tr -d '\r\n ')
[ -n "$URL" ] || exit 0
if [ -f "$OUT" ] && [ "$(cat "$STAMP" 2>/dev/null)" = "$URL" ]; then
  echo "Ilustración ya descargada."
  exit 0
fi
mkdir -p "$(dirname "$OUT")"
curl -fsSL "$URL" -o /tmp/header_src
python3 -m pip install --quiet pillow
python3 - "$OUT" <<'PY'
import sys
from PIL import Image
img = Image.open("/tmp/header_src").convert("RGB")
w = 1600
h = round(img.height * w / img.width)
img.resize((w, h), Image.LANCZOS).save(sys.argv[1], "WEBP", quality=82, method=6)
print("Guardada", sys.argv[1], w, "x", h)
PY
echo "$URL" > "$STAMP"
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add "$OUT" "$STAMP"
git commit -m "Ilustración de la cabecera descargada por GitHub Actions"
git push origin "HEAD:${GITHUB_REF_NAME}"
