#!/system/bin/sh
# UsbMassStorage uninstall cleanup

TAG="usbmassstorage"
DATA_DIR="/data/adb/usbmassstorage"
LOCK_FILE="/dev/usbms_svc_lock"
PKG="com.enginex0.usbmassstorage"

echo "${TAG}: uninstall started" > /dev/kmsg

# Remove persistent data
if [ -d "$DATA_DIR" ]; then
    rm -rf "$DATA_DIR"
    echo "${TAG}: removed $DATA_DIR" > /dev/kmsg
fi

# Remove lock file
if [ -f "$LOCK_FILE" ]; then
    rm -f "$LOCK_FILE"
    echo "${TAG}: removed lock file" > /dev/kmsg
fi

# Uninstall companion app
if pm list packages 2>/dev/null | grep -q "$PKG"; then
    pm uninstall "$PKG" >/dev/null 2>&1
    echo "${TAG}: uninstalled $PKG" > /dev/kmsg
fi

echo "${TAG}: uninstall complete" > /dev/kmsg
