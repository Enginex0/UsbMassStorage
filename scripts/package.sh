#!/usr/bin/env bash
# Build, package, deploy, and verify UsbMassStorage module ZIPs.
# Usage: ./scripts/package.sh [flags]
#
# Examples:
#   ./scripts/package.sh                               # package module ZIP
#   ./scripts/package.sh --apk=app/build/.../app.apk   # bundle APK into ZIP
#   ./scripts/package.sh --deploy --reboot              # push, install, reboot
#   ./scripts/package.sh --deploy --verify              # deploy + logcat check
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MODULE_DIR="$PROJECT_ROOT/module"
OUT_DIR="$PROJECT_ROOT/out"
PROP_FILE="$MODULE_DIR/module.prop"

APK_PATH=""
DEPLOY=false
REBOOT=false
VERIFY=false
TRACE=false
ROOT_PROVIDER="ksu"

red()    { printf '\033[0;31m%s\033[0m\n' "$*"; }
green()  { printf '\033[0;32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[0;33m%s\033[0m\n' "$*"; }
bold()   { printf '\033[1m%s\033[0m\n' "$*"; }

usage() {
    cat <<EOF
Usage: $(basename "$0") [options]

Package options:
  --apk=PATH         Bundle APK into flashable ZIP

Deploy options:
  --deploy           Push ZIP to device and install
  --reboot           Reboot device after install
  --verify           Run logcat verification after deploy
  --root PROVIDER    Root provider: ksu (default), magisk, apatch

Misc:
  -v, --verbose      Print every command as it runs
  --help             Show this help
EOF
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --apk=*)      APK_PATH="${1#--apk=}"; shift ;;
        --deploy)     DEPLOY=true; shift ;;
        --reboot)     REBOOT=true; shift ;;
        --verify)     VERIFY=true; shift ;;
        -v|--verbose) TRACE=true; shift ;;
        --root)       ROOT_PROVIDER="$2"; shift 2 ;;
        --help|-h)    usage ;;
        *)            red "Unknown flag: $1"; usage ;;
    esac
done

[[ "$TRACE" == true ]] && set -x

case "$ROOT_PROVIDER" in
    ksu)     INSTALL_CMD="ksud module install" ;;
    magisk)  INSTALL_CMD="magisk --install-module" ;;
    apatch)  INSTALL_CMD="/data/adb/apd module install" ;;
    *)       red "Unknown root provider: $ROOT_PROVIDER"; exit 1 ;;
esac

if [[ ! -f "$PROP_FILE" ]]; then
    red "module.prop not found at $PROP_FILE"
    exit 1
fi

VERSION="$(grep '^version=' "$PROP_FILE" | cut -d= -f2)"
if [[ -z "$VERSION" ]]; then
    red "Could not read version from module.prop"
    exit 1
fi

OUTPUT="$OUT_DIR/UsbMassStorage-${VERSION}.zip"

echo ""
bold "UsbMassStorage package pipeline"
echo ""

bold "==> Packaging $VERSION"
mkdir -p "$OUT_DIR"
rm -f "$OUTPUT"

if [[ -n "$APK_PATH" ]]; then
    if [[ ! -f "$APK_PATH" ]]; then
        red "APK not found: $APK_PATH"
        exit 1
    fi
    green "    Bundling APK: $(basename "$APK_PATH")"
    cp "$APK_PATH" "$MODULE_DIR/app.apk"
fi

(cd "$MODULE_DIR" && zip -r9 "$OUTPUT" . -x '*.DS_Store' -x '__MACOSX/*')

rm -f "$MODULE_DIR/app.apk"

bold "==> Build Summary"
if [[ -f "$OUTPUT" ]]; then
    local_size=$(du -h "$OUTPUT" | cut -f1)
    green "    $(basename "$OUTPUT") ($local_size)"
else
    red "    ZIP not found"
    exit 1
fi

if [[ "$DEPLOY" == true ]]; then
    if ! adb get-state &>/dev/null; then
        red "No ADB device connected"
        exit 1
    fi

    bold "==> Deploying $(basename "$OUTPUT")"
    adb push "$OUTPUT" /data/local/tmp/module.zip
    adb shell "su -c '$INSTALL_CMD /data/local/tmp/module.zip'"
    green "    Installed via $ROOT_PROVIDER"

    if [[ "$REBOOT" == true ]]; then
        bold "==> Rebooting"
        adb reboot
        echo "    Waiting for device..."
        adb wait-for-device
        sleep 10
        local_pid=$(adb shell "pidof daemon" 2>/dev/null || true)
        if [[ -n "$local_pid" ]]; then
            green "    Daemon alive (PID $local_pid)"
        else
            yellow "    Daemon not yet started, check logcat"
        fi
    fi
fi

if [[ "$VERIFY" == true ]]; then
    if ! adb get-state &>/dev/null; then
        red "No ADB device connected"
        exit 1
    fi

    bold "==> Verification"

    local_pid=$(adb shell "pidof daemon" 2>/dev/null || true)
    if [[ -n "$local_pid" ]]; then
        green "    Daemon: running (PID $local_pid)"
    else
        red "    Daemon: not running"
    fi

    local_ctx=$(adb shell "su -c 'ps -efZ | grep daemon'" 2>/dev/null | grep msd_daemon || true)
    if [[ -n "$local_ctx" ]]; then
        green "    SELinux: u:r:msd_daemon:s0"
    else
        yellow "    SELinux: context not confirmed"
    fi

    local_denials=$(adb shell "su -c 'dmesg'" 2>/dev/null | grep -c 'avc.*msd' || true)
    if [[ "$local_denials" -eq 0 ]]; then
        green "    AVC denials: 0"
    else
        yellow "    AVC denials: $local_denials (run: dmesg | grep 'avc.*msd')"
    fi

    local_errors=$(adb logcat -d -s msd-tool 2>/dev/null | grep -ciE 'error|exception' || true)
    if [[ "$local_errors" -eq 0 ]]; then
        green "    Logcat errors: 0"
    else
        yellow "    Logcat errors: $local_errors (run: adb logcat -d -s msd-tool)"
    fi
fi

echo ""
green "Done."
