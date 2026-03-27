#!/system/bin/sh
# UsbMassStorage post-fs-data stage

MODDIR="${0%/*}"
TAG="usbmassstorage"
DATA_DIR="/data/adb/usbmassstorage"
COUNT_FILE="$DATA_DIR/count.sh"

# Exit if module is disabled
[ -f "$MODDIR/disable" ] && exit 0

# Add busybox paths for KSU/APatch/Magisk compatibility
for bb_path in /data/adb/ksu/bin/busybox /data/adb/ap/bin/busybox /data/adb/magisk/busybox; do
    [ -x "$bb_path" ] && export PATH="${bb_path%/*}:$PATH"
done

echo "${TAG}: post-fs-data started" > /dev/kmsg

# Bootloop guard: disable module after 3 consecutive failed boots
mkdir -p "$DATA_DIR"
COUNT=0
if [ -f "$COUNT_FILE" ]; then
    . "$COUNT_FILE"
fi

if [ "$COUNT" -ge 3 ]; then
    echo "${TAG}: boot count >= 3, disabling module" > /dev/kmsg
    touch "$MODDIR/disable"
    exit 1
fi

COUNT=$((COUNT + 1))
echo "COUNT=$COUNT" > "$COUNT_FILE"
echo "${TAG}: boot count incremented to $COUNT" > /dev/kmsg

# Source common.sh for ABI detection
. "$MODDIR/common.sh"

# Apply runtime SELinux rules
if [ -f "$MODDIR/ksu_rules.txt" ]; then
    echo "${TAG}: applying runtime sepolicy rules" > /dev/kmsg
    ksud sepolicy apply "$MODDIR/ksu_rules.txt" 2>/dev/null
    echo "${TAG}: sepolicy rules applied" > /dev/kmsg
else
    echo "${TAG}: WARNING - ksu_rules.txt not found" > /dev/kmsg
fi

echo "${TAG}: post-fs-data complete" > /dev/kmsg
