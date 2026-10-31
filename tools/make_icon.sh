#!/usr/bin/env bash
# Builds the app's launcher icon from the website's favicon (site icon).
# Needs: curl and ImageMagick. Run from anywhere:  bash tools/make_icon.sh
# Optional:  BG="#FFFFFF" (icon background colour)   LOGO_FILE=/path/to/logo.png (skip download)
set -euo pipefail
cd "$(dirname "$0")/.."
BG="${BG:-#FFFFFF}"
TMP="$(mktemp -d)"
IM=convert; command -v magick >/dev/null 2>&1 && IM=magick

if [ -n "${LOGO_FILE:-}" ]; then
  cp "$LOGO_FILE" "$TMP/logo.png"
else
  ok=0
  for u in \
    "https://i0.wp.com/rosebankgospelhall.ca/wp-content/uploads/2026/04/Rosebank_logo_trans-1.png?fit=1024%2C1024&ssl=1" \
    "https://rosebankgospelhall.ca/wp-content/uploads/2026/04/Rosebank_logo_trans-1.png" \
    "https://i0.wp.com/rosebankgospelhall.ca/wp-content/uploads/2026/04/Rosebank_logo_trans-1.png?fit=270%2C270&ssl=1"; do
    if curl -fsSL --max-time 30 -A "Mozilla/5.0" "$u" -o "$TMP/logo.png" && $IM "$TMP/logo.png" -format "%w" info: >/dev/null 2>&1; then
      ok=1; break
    fi
  done
  if [ "$ok" != 1 ]; then echo "Could not download the website icon. Keeping the default icon."; exit 0; fi
fi

# 432px canvas: the logo fits the adaptive-icon safe zone (about 64%).
$IM "$TMP/logo.png[0]" -background none -alpha on -trim +repage -resize 276x276 \
    -gravity center -extent 432x432 "$TMP/fg.png"

RES=app/src/main/res
mkdir -p "$RES/drawable-nodpi"
cp "$TMP/fg.png" "$RES/drawable-nodpi/ic_launcher_foreground.png"
rm -f "$RES/drawable/ic_launcher_foreground.xml"

cat > "$RES/values/colors.xml" <<XML
<resources>
    <color name="ic_launcher_background">$BG</color>
</resources>
XML

cat > "$RES/mipmap-anydpi-v26/ic_launcher.xml" <<XML
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
XML
echo "Launcher icon updated from the website favicon."
