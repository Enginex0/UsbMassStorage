#!/system/bin/sh
SKIPUNZIP=1

msd_print() {
  local msg="$1"
  local delay="${2:-0.3}"
  local mode="$3"
  local width=$(( ${#msg} + 3 ))
  [ "$width" -gt 60 ] && width=60
  if [ "$mode" = "h" ]; then
    ui_print ""
    ui_print "$(printf '%*s' "$width" | tr ' ' '=')"
    ui_print " $msg"
    ui_print "$(printf '%*s' "$width" | tr ' ' '=')"
  else
    ui_print "$msg"
  fi
  sleep "$delay"
}

unzip -o "$ZIPFILE" -x 'META-INF/*' -d "$MODPATH" >&2 || abort "  ❌ Extraction failed"

rm -f "$MODPATH/disable"

for _mgr_bin in /data/adb/ksu/bin /data/adb/ap/bin /data/adb/magisk; do
    [ -d "$_mgr_bin" ] && export PATH="$_mgr_bin:$PATH"
done

MSD_VERSION=$(grep '^version=' "$MODPATH/module.prop" | cut -d= -f2)

MSD_LOCALE=$(getprop persist.sys.locale 2>/dev/null)
[ -z "$MSD_LOCALE" ] && MSD_LOCALE=$(getprop ro.product.locale 2>/dev/null)
[ -z "$MSD_LOCALE" ] && MSD_LOCALE=$(getprop persist.sys.language 2>/dev/null)
[ -z "$MSD_LOCALE" ] && MSD_LOCALE="en"

MSD_LANG=$(echo "$MSD_LOCALE" | sed 's/[-_].*//')
MSD_REGION=$(echo "$MSD_LOCALE" | sed 's/^[a-z]*[-_]*//')

MSD_LANG_FILE=""
if [ -f "$MODPATH/lang/${MSD_LANG}_${MSD_REGION}.sh" ]; then
    MSD_LANG_FILE="$MODPATH/lang/${MSD_LANG}_${MSD_REGION}.sh"
elif [ -f "$MODPATH/lang/${MSD_LANG}.sh" ]; then
    MSD_LANG_FILE="$MODPATH/lang/${MSD_LANG}.sh"
fi

. "$MODPATH/lang/en.sh"
[ -n "$MSD_LANG_FILE" ] && . "$MSD_LANG_FILE"

ui_print ""
ui_print "==========================================="
ui_print "  💾 USB Mass Storage ${MSD_VERSION} 💾"
ui_print "==========================================="
ui_print "  🔌 ${LANG_TURN_PHONE}"
ui_print "  ✅ ${LANG_SUPPORTED}"
ui_print "==========================================="
ui_print ""
sleep 0.5

msd_print "📱 ${LANG_DETECT_ARCH}" 0.3 "h"

MODDIR="$MODPATH"
. "$MODPATH/common.sh"

if [ -z "$ABI" ]; then
    abort "  ❌ ${LANG_UNSUPPORTED_ARCH}: $(uname -m)"
fi
msd_print "  ✅ ${LANG_ARCH_OK}: $ABI"

BIN="$MODPATH/bin/${ABI}/daemon"
if [ ! -f "$BIN" ]; then
    abort "  ❌ ${LANG_BIN_NOT_FOUND}: bin/${ABI}/daemon"
fi

set_perm_recursive "$MODPATH/bin/${ABI}" 0 0 0755 0755

for d in "$MODPATH"/bin/*/; do
    [ "$d" = "$MODPATH/bin/${ABI}/" ] && continue
    rm -rf "$d"
done
msd_print "  ✅ ${LANG_BINARY_READY}"

msd_print "🔍 ${LANG_DETECT_ROOT}" 0.3 "h"

if [ -n "$KSU" ]; then
    msd_print "  ✅ ${LANG_KSU_DETECTED} (v${KSU_VER_CODE:-?})"
elif [ -n "$APATCH" ]; then
    msd_print "  ✅ ${LANG_APATCH_DETECTED} (v${APATCH_VER_CODE:-?})"
else
    msd_print "  ✅ ${LANG_MAGISK_DETECTED}"
fi

msd_print "🛡️ ${LANG_SELINUX}" 0.3 "h"

if [ -f "$MODPATH/sepolicy.rule" ]; then
    msd_print "  ✅ ${LANG_SELINUX_OK}"
else
    msd_print "  ⚠️ ${LANG_SELINUX_WARN}"
fi

DATA_DIR="/data/adb/usbmassstorage"
msd_print "📁 ${LANG_PREP_DATA}" 0.3 "h"

mkdir -p "$DATA_DIR"
echo "COUNT=0" > "$DATA_DIR/count.sh"
msd_print "  ✅ ${LANG_BOOT_RESET}"

if [ -f "$MODPATH/app.apk" ]; then
    msd_print "📲 ${LANG_INSTALL_APP}" 0.3 "h"
    pm install -r "$MODPATH/app.apk" >/dev/null 2>&1
    if [ $? -eq 0 ]; then
        msd_print "  ✅ ${LANG_APP_OK}"
    else
        msd_print "  ⚠️ ${LANG_APP_FAIL}"
    fi
    rm -f "$MODPATH/app.apk"
fi

msd_print "🚀 ${LANG_FINALIZE}" 0.3 "h"

if command -v chcon >/dev/null 2>&1; then
    find "$MODPATH" -exec chcon u:object_r:system_file:s0 {} + 2>/dev/null || true
fi

set_perm_recursive "$MODPATH/bin" 0 0 0755 0755
chmod 755 "$MODPATH"/*.sh
set_perm "$MODPATH/module.prop" 0 0 0644

msd_print "  ✅ ${LANG_PERMS_SET}"

rm -rf "$MODPATH/lang"

ui_print ""
ui_print "==========================================="
ui_print "  ✨ ${LANG_INSTALLED} ✨"
ui_print "  📖 ${LANG_OPEN_APP}"
ui_print "  🔄 ${LANG_REBOOT}"
ui_print "==========================================="
ui_print ""
