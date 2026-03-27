#!/system/bin/sh
MODDIR="${0%/*}"
TAG="usbmassstorage"
LOCK_FILE="/dev/usbms_svc_lock"

[ -f "$MODDIR/disable" ] && exit 0

set -C
if ! : > "$LOCK_FILE" 2>/dev/null; then
    echo "${TAG}: another instance already running, exiting" > /dev/kmsg
    exit 0
fi
set +C
trap 'rm -f "$LOCK_FILE"' EXIT
trap 'exit 0' INT TERM

for bb_path in /data/adb/ksu/bin/busybox /data/adb/ap/bin/busybox /data/adb/magisk/busybox; do
    [ -x "$bb_path" ] && export PATH="${bb_path%/*}:$PATH"
done

echo "${TAG}: service started" > /dev/kmsg

. "$MODDIR/common.sh"

if [ -z "$ABI" ]; then
    echo "${TAG}: ERROR - could not detect ABI" > /dev/kmsg
    exit 1
fi

if [ ! -f "$BIN" ]; then
    echo "${TAG}: ERROR - binary not found: $BIN" > /dev/kmsg
    exit 1
fi

# Trigger app-side mount after boot via explicit broadcast.
# App process has matching MCS categories to open its own files.
# Explicit broadcast bypasses MIUI auto-start and stopped-app restrictions.
(
    while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done
    sleep 3
    am broadcast \
        -a android.intent.action.BOOT_COMPLETED \
        -n com.enginex0.usbmassstorage/.BootMountReceiver \
        --include-stopped-packages --user 0 \
        > /dev/null 2>&1
    echo "${TAG}: sent boot mount broadcast to app" > /dev/kmsg
) &

BACKOFF=1
while true; do
    echo "${TAG}: launching daemon (ABI=$ABI)" > /dev/kmsg
    /system/bin/runcon u:r:msd_daemon:s0 "$BIN" daemon \
        --log-target logcat --log-level debug \
        --automount-config /data/adb/usbmassstorage/automount.conf
    rc=$?
    if [ $rc -eq 0 ]; then
        echo "COUNT=0" > /data/adb/usbmassstorage/count.sh 2>/dev/null
        break
    fi
    echo "${TAG}: daemon exited ($rc), respawning in ${BACKOFF}s" > /dev/kmsg
    sleep "$BACKOFF"
    BACKOFF=$((BACKOFF * 2))
    [ "$BACKOFF" -gt 30 ] && BACKOFF=30
done
