#!/usr/bin/env bash
# UI smoke test: install, walk through main screens, capture screenshots and crash logs.
set -u
OUT=${OUT:-shots}
mkdir -p "$OUT"
PKG=com.filetor.app

dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1; }
tap() {
  dump
  if grep -q "isn&apos;t responding\|isn't responding" /tmp/ui.xml; then
    echo "Dismissing system ANR dialog"; adb shell input keyevent 4; sleep 1; dump
    if grep -q "responding" /tmp/ui.xml; then adb shell am start -n $PKG/.MainActivity >/dev/null; sleep 2; dump; fi
  fi
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys, os
t = sys.argv[1]
last = os.environ.get("LAST") == "1"
found = None
x = open('/tmp/ui.xml', encoding='utf-8').read()
for m in re.finditer(r'<node [^>]*>', x):
    n = m.group(0)
    txt = re.search(r' text="([^"]*)"', n).group(1)
    desc = re.search(r'content-desc="([^"]*)"', n).group(1)
    if txt == t or desc == t or (len(t) > 3 and txt.startswith(t)):
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()
        b = list(map(int, b))
        found = ((b[0]+b[2])//2, (b[1]+b[3])//2)
        if not last: break
if found: print(*found)
PY
)
  if [ -n "$xy" ]; then
    if [ "${LONG:-0}" = 1 ]; then set -- "$1" $xy; adb shell input swipe $2 $3 $2 $3 900; else adb shell input tap $xy; fi
    echo "tap '$1' -> $xy"
  else echo "NOT FOUND: '$1'"; fi
  sleep 2
}
longtap() { LONG=1 tap "$1"; }
check() { if adb shell "[ -e '$1' ]"; then echo "PASS: $1"; else echo "FAIL: $1"; fi; }
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
adb shell settings put global hide_error_dialogs 1 2>/dev/null || true
sleep 20
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

# --- Archive operations ---
adb shell input swipe 500 600 500 1800; sleep 1
tap "demo.zip"; tap "Extraer todo"; sleep 4; shot 16_after_extract_zip
check /sdcard/Download/demo/folder/sub/deep.txt
check /sdcard/Download/demo/notes.txt
back
tap "demo.7z"; tap "Extraer todo"; sleep 4
check "/sdcard/Download/demo (1)/folder/file1.txt"
back
tap "demo.tar.gz"; shot 17_targz_view; tap "Extraer todo"; sleep 4
check "/sdcard/Download/demo (2)/folder/file3.txt"
back
adb shell input swipe 500 600 500 1800; sleep 1
longtap "folder"; shot 18_selection
tap "Más"; shot 19_more_menu
tap "Comprimir"; shot 20_compress_dialog
LAST=1 tap "Comprimir"; sleep 5; shot 21_after_compress
adb shell ls -la /sdcard/Download/
check /sdcard/Download/folder.zip
adb logcat -d > "$OUT/logcat.txt"
grep -E "FATAL EXCEPTION" -A 30 "$OUT/logcat.txt" > "$OUT/crashes.txt" || true
echo "=== crashes ==="; cat "$OUT/crashes.txt"
adb shell ls -R /sdcard/Download > "$OUT/tree.txt" 2>&1
exit 0
