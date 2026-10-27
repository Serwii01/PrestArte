#!/usr/bin/env bash
# Regenera los PDF del dosier comercial a partir de los HTML de esta carpeta.
# Requiere Chromium o Google Chrome y las fuentes Inter / Inter Display.
# La presentación se genera aparte con: python3 presentacion.py
set -euo pipefail
cd "$(dirname "$0")"
CHROME="${CHROME:-$(command -v chromium || command -v google-chrome || command -v chrome)}"
OUT=..
for doc in dossier:PrestArte_Dossier ficha-tecnica:PrestArte_Ficha_Tecnica one-pager:PrestArte_One_Pager; do
  src="${doc%%:*}"; dst="${doc##*:}"
  "$CHROME" --headless=new --no-sandbox --disable-gpu --no-pdf-header-footer \
    --allow-file-access-from-files --run-all-compositor-stages-before-draw --virtual-time-budget=5000 \
    --print-to-pdf="$OUT/$dst.pdf" "file://$PWD/$src.html"
done
