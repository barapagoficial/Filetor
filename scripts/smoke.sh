#!/usr/bin/env bash
# UI smoke test: install, walk through main screens, capture screenshots and crash logs.
set -u
OUT=${OUT:-shots}
mkdir -p "$OUT"
PKG=com.filetor.app

dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1; }
tap() {
  dump
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys
t = sys.argv[1]
x = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    txt = re.search(r' text="([^"]*)"', n).group(1)
    desc = re.search(r'content-desc="([^"]*)"', n).group(1)
    if txt == t or desc == t or (len(t) > 3 and txt.startswith(t)):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()
        b = list(map(int, b))
        print((b[0]+b[2])//2, (b[1]+b[3])//2)
        break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "tap '$1' -> $xy"; else echo "NOT FOUND: '$1'"; fi
  sleep 2
}
shot() { sleep "${2:-2}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
back() { adb shell input keyevent 4; sleep 1.5; }

adb install -r -g Filetor.apk
adb shell appops set $PKG MANAGE_EXTERNAL_STORAGE allow
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null || true

# Test data
adb shell mkdir -p /sdcard/Download/Filetor_demo /sdcard/Documents /sdcard/Pictures /sdcard/Music
adb push testdata/. /sdcard/Download/ >/dev/null
adb shell 'for i in 1 2 3; do echo "Nota $i" > /sdcard/Documents/nota_$i.txt; done'
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download >/dev/null 2>&1 || true
adb shell content call --uri content://media/external/file --method scan_volume --arg external_primary >/dev/null 2>&1 || true

adb logcat -c
adb shell am start -n $PKG/.MainActivity
shot 01_home 6
tap "Archivos";            shot 02_browser
tap "Download";            shot 03_downloads
tap "demo.zip";            shot 04_archive_view 3
back
tap "Opciones" ; shot 05_item_menu
back
tap "Herramientas";        shot 06_tools
tap "Analizar almacenamiento"; shot 07_analyzer 8
back
tap "Archivos duplicados"; shot 08_duplicates 6
back
tap "Inicio";              shot 09_home_again
tap "Imágenes";            shot 10_images 4
back
tap "Ajustes";             shot 11_settings
tap "Oscuro";              shot 12_settings_dark
back
shot 13_home_dark
tap "Archivos"
tap "Download"
tap "notes.txt";           shot 14_editor 3
back
adb shell input swipe 500 1500 500 600
shot 15_scrolled
adb logcat -d > "$OUT/logcat.txt"
grep -E "FATAL EXCEPTION|AndroidRuntime" -A 30 "$OUT/logcat.txt" > "$OUT/crashes.txt" || true
echo "=== crashes ==="; cat "$OUT/crashes.txt"
exit 0
