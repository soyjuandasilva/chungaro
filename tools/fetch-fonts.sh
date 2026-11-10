#!/usr/bin/env bash
# Descarga la tipografía Barlow Semi Condensed (licencia SIL Open Font License)
# de Google Fonts a res/font. La app la usa si está; si no, usa la del sistema.
set -u
dir="app/src/main/res/font"
mkdir -p "$dir"
base="https://github.com/google/fonts/raw/main/ofl/barlowsemicondensed"
for w in Medium SemiBold Bold ExtraBold; do
  out="$dir/barlow_$(echo "$w" | tr '[:upper:]' '[:lower:]').ttf"
  curl -fsSL --retry 3 -o "$out" "$base/BarlowSemiCondensed-$w.ttf" || { echo "No se pudo descargar $w"; rm -f "$out"; }
done
ls -la "$dir" || true
