#!/usr/bin/env bash
# Descarga logotipos candidatos de varias fuentes para cada marca listada en
# tools/logo-sources.txt y los guarda en tools/logo-candidates/ para revisarlos.
# Solo se ejecuta cuando cambia la lista.
set -euo pipefail
SRC=tools/logo-sources.txt
OUT=tools/logo-candidates
STAMP=$OUT/.sources-sha
[ -f "$SRC" ] || exit 0
SHA=$(sha256sum "$SRC" | cut -d' ' -f1)
if [ -f "$STAMP" ] && [ "$(cat "$STAMP")" = "$SHA" ]; then echo "Candidatos al día."; exit 0; fi
mkdir -p "$OUT" /tmp/logos
python3 -m pip install --quiet pillow
python3 - "$SRC" "$OUT" <<'PY'
import sys, os, io, urllib.request, urllib.parse
from PIL import Image
src, out = sys.argv[1], sys.argv[2]
UA = {"User-Agent": "Mozilla/5.0 (Atesta logo fetcher)"}
def get(url):
    try:
        req = urllib.request.Request(url, headers=UA)
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.read()
    except Exception as e:
        print("  x", url, e); return None
def save(data, path):
    try:
        im = Image.open(io.BytesIO(data))
        # .ico: elegir el tamaño más grande
        if getattr(im, "n_frames", 1) > 1 or im.format == "ICO":
            sizes = getattr(im, "ico", None)
            if sizes: im = sizes.getimage(max(sizes.sizes()))
        im = im.convert("RGBA")
        if min(im.size) < 24: return False
        im.thumbnail((256, 256))
        im.save(path, "PNG"); print("  ok", path, im.size); return True
    except Exception as e:
        print("  no imagen", path, e); return False
for line in open(src, encoding="utf-8"):
    line = line.strip()
    if not line or line.startswith("#"): continue
    key, domains, commons = (line.split("|") + ["", ""])[:3]
    print(key)
    n = 0
    urls = []
    for d in domains.split():
        urls += [f"https://www.google.com/s2/favicons?domain={d}&sz=256",
                 f"https://icons.duckduckgo.com/ip3/{d}.ico",
                 f"https://{d}/apple-touch-icon.png",
                 f"https://www.{d}/apple-touch-icon.png"]
    for name in [c for c in commons.split(";") if c.strip()]:
        urls.append("https://commons.wikimedia.org/wiki/Special:FilePath/" + urllib.parse.quote(name.strip()) + "?width=256")
    for u in urls:
        data = get(u)
        if data and save(data, os.path.join(out, f"{key}_{n}.png")):
            with open(os.path.join(out, f"{key}_{n}.txt"), "w") as f: f.write(u + "\n")
            n += 1
PY
echo "$SHA" > "$STAMP"
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add "$OUT"
git commit -m "Logotipos candidatos descargados por GitHub Actions" || exit 0
git push origin "HEAD:${GITHUB_REF_NAME}"
