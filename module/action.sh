#!/system/bin/sh
PKG="com.enginex0.usbmassstorage"
ACT=".MainActivity"

if pm path "$PKG" >/dev/null 2>&1; then
    am start -n "${PKG}/${ACT}" \
        --user 0 \
        -a android.intent.action.MAIN \
        -c android.intent.category.LAUNCHER \
        > /dev/null 2>&1
else
    echo "Companion app not installed"
fi
