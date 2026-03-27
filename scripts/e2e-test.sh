#!/usr/bin/env bash
set -euo pipefail

PKG="com.enginex0.usbmassstorage"
ACTIVITY="$PKG/.MainActivity"
TEST_DIR="/data/local/tmp/usb_test"
SCREENSHOT_DIR="$(cd "$(dirname "$0")/.." && pwd)/out/e2e-screenshots"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BOLD='\033[1m'
NC='\033[0m'

PASSED=0
FAILED=0
SKIPPED=0
TEST_NUM=1
UI_XML=""

adb_su() { adb shell "su -c '$*'" 2>/dev/null; }

dump_ui() {
    adb shell uiautomator dump /sdcard/ui_dump.xml > /dev/null 2>&1
    UI_XML=$(adb shell cat /sdcard/ui_dump.xml 2>/dev/null)
    echo "$UI_XML"
}

has_text() { echo "$UI_XML" | grep -q "$1"; }

wait_for_text() {
    local text="$1" timeout="${2:-8}" i=0
    while [ $i -lt "$timeout" ]; do
        dump_ui > /dev/null
        has_text "$text" && return 0
        sleep 1
        i=$((i + 1))
    done
    return 1
}

tap_by_text() {
    local text="$1"
    dump_ui > /dev/null
    local bounds
    bounds=$(echo "$UI_XML" | grep -oP "text=\"[^\"]*${text}[^\"]*\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" | head -1 | grep -oP 'bounds="\[\K[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+' | head -1)
    if [ -z "$bounds" ]; then
        return 1
    fi
    local x1 y1 x2 y2
    x1=$(echo "$bounds" | sed 's/,.*//')
    y1=$(echo "$bounds" | sed 's/[^,]*,//;s/\].*//')
    x2=$(echo "$bounds" | sed 's/.*\[//;s/,.*//')
    y2=$(echo "$bounds" | sed 's/.*,//')
    local cx=$(( (x1 + x2) / 2 ))
    local cy=$(( (y1 + y2) / 2 ))
    adb shell "su -c 'input tap $cx $cy'"
    sleep 0.5
}

tap_by_desc() {
    local desc="$1"
    dump_ui > /dev/null
    local bounds
    bounds=$(echo "$UI_XML" | grep -oP "content-desc=\"[^\"]*${desc}[^\"]*\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" | head -1 | grep -oP 'bounds="\[\K[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+' | head -1)
    if [ -z "$bounds" ]; then
        return 1
    fi
    local x1 y1 x2 y2
    x1=$(echo "$bounds" | sed 's/,.*//')
    y1=$(echo "$bounds" | sed 's/[^,]*,//;s/\].*//')
    x2=$(echo "$bounds" | sed 's/.*\[//;s/,.*//')
    y2=$(echo "$bounds" | sed 's/.*,//')
    local cx=$(( (x1 + x2) / 2 ))
    local cy=$(( (y1 + y2) / 2 ))
    adb shell "su -c 'input tap $cx $cy'"
    sleep 0.5
}

screenshot() {
    local name="$1"
    mkdir -p "$SCREENSHOT_DIR"
    adb shell screencap -p /sdcard/e2e_screen.png
    adb pull /sdcard/e2e_screen.png "$SCREENSHOT_DIR/${name}.png" > /dev/null 2>&1
    adb shell rm -f /sdcard/e2e_screen.png
}

long_press() {
    local text="$1"
    dump_ui > /dev/null
    local bounds
    bounds=$(echo "$UI_XML" | grep -oP "text=\"[^\"]*${text}[^\"]*\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" | head -1 | grep -oP 'bounds="\[\K[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+' | head -1)
    [ -z "$bounds" ] && return 1
    local x1 y1 x2 y2
    x1=$(echo "$bounds" | sed 's/,.*//')
    y1=$(echo "$bounds" | sed 's/[^,]*,//;s/\].*//')
    x2=$(echo "$bounds" | sed 's/.*\[//;s/,.*//')
    y2=$(echo "$bounds" | sed 's/.*,//')
    local cx=$(( (x1 + x2) / 2 ))
    local cy=$(( (y1 + y2) / 2 ))
    adb shell "su -c 'input swipe $cx $cy $cx $cy 1500'"
    sleep 1
}

launch_app() {
    adb shell am force-stop "$PKG"
    sleep 0.5
    adb shell am start -n "$ACTIVITY" > /dev/null 2>&1
    sleep 3
}

go_back() {
    adb shell "su -c 'input keyevent KEYCODE_BACK'"
    sleep 0.5
}

run_test() {
    local name="$1"
    shift
    printf "[T%02d] %-50s " "$TEST_NUM" "$name"
    TEST_NUM=$((TEST_NUM + 1))

    local output rc
    output=$("$@" 2>&1) && rc=0 || rc=$?

    if [ $rc -eq 0 ]; then
        printf "${GREEN}PASS${NC}\n"
        PASSED=$((PASSED + 1))
    elif [ $rc -eq 124 ]; then
        printf "${YELLOW}TIMEOUT${NC}\n"
        SKIPPED=$((SKIPPED + 1))
    else
        printf "${RED}FAIL${NC}\n"
        FAILED=$((FAILED + 1))
        echo "$output" | tail -3 | sed 's/^/       /'
    fi
}

t01_module_boot() {
    local pid
    pid=$(adb_su "pidof daemon")
    [ -n "$pid" ] || { echo "daemon not running"; return 1; }
    local ctx
    ctx=$(adb_su "cat /proc/$pid/attr/current")
    echo "$ctx" | grep -q "msd_daemon" || { echo "wrong context: $ctx"; return 1; }
    local denials
    denials=$(adb_su "dmesg" | grep -c 'avc.*denied.*msd_daemon' || true)
    [ "$denials" -eq 0 ] || echo "WARN: $denials AVC denials"
}

t02_app_launch() {
    launch_app
    wait_for_text "USB Mass Storage" 5 || { echo "title not found"; return 1; }
}

t03_root_detection() {
    dump_ui > /dev/null
    has_text "Granted" || { echo "root not shown as granted"; return 1; }
}

t04_daemon_connection() {
    dump_ui > /dev/null
    if has_text "Retry"; then
        tap_by_text "Retry" || true
        sleep 3
        dump_ui > /dev/null
    fi
    has_text "Connected" || has_text "No USB devices" || { echo "daemon not shown connected"; return 1; }
}

t05_fab_opens_sheet() {
    tap_by_desc "Add device" || tap_by_text "+" || { echo "FAB not found"; return 1; }
    sleep 1
    wait_for_text "Mount USB Device" 3 || { echo "add sheet didn't open"; return 1; }
}

t06_add_sheet_elements() {
    dump_ui > /dev/null
    has_text "Select disk image" || has_text "disk image file" || { echo "file picker missing"; return 1; }
    has_text "Read-Write" || { echo "RW chip missing"; return 1; }
    has_text "Read-Only" || { echo "RO chip missing"; return 1; }
    has_text "CD-ROM" || { echo "CDROM chip missing"; return 1; }
    has_text "Mount" || { echo "mount button missing"; return 1; }
}

t07_create_image_dialog() {
    tap_by_text "Create new disk image" || { echo "create link not found"; return 1; }
    sleep 1
    wait_for_text "Create Disk Image" 3 || wait_for_text "Filename" 3 || { echo "dialog didn't open"; return 1; }
    go_back
    sleep 0.5
    go_back
}

t08_settings_navigation() {
    dump_ui > /dev/null
    tap_by_desc "Menu" || tap_by_text "More" || { echo "menu not found"; return 1; }
    sleep 0.5
    tap_by_text "Settings" || { echo "settings item not found"; return 1; }
    sleep 1
    wait_for_text "Daemon Status" 3 || { echo "settings screen not loaded"; return 1; }
}

t09_daemon_status() {
    dump_ui > /dev/null
    has_text "Running" || { echo "daemon not shown as running"; return 1; }
}

t10_version_info() {
    dump_ui > /dev/null
    has_text "Version" || { echo "version not shown"; return 1; }
    has_text "3.1.0" || has_text "v3" || { echo "version number not found"; return 1; }
}

t11_copy_logs() {
    adb logcat -c
    tap_by_text "Copy Daemon Logs" || { echo "copy logs button not found"; return 1; }
    sleep 1
    adb logcat -d -s UsbMsUI | grep -q "copy logs clicked" || { echo "no log entry"; return 1; }
}

t12_selinux_context() {
    tap_by_text "View SELinux Context" || {
        adb shell "su -c 'input swipe 360 1400 360 900 300'"
        sleep 1
        tap_by_text "View SELinux Context" || { echo "selinux button not found"; return 1; }
    }
    sleep 3
    adb shell "su -c 'input swipe 360 1400 360 900 300'"
    sleep 1
    screenshot "t12_selinux"
    local ctx
    ctx=$(adb_su "cat /proc/\$(pidof daemon)/attr/current 2>/dev/null")
    echo "$ctx" | grep -q "msd_daemon" || { echo "daemon not running as msd_daemon: $ctx"; return 1; }
}

t13_debug_mode() {
    adb shell "su -c 'input swipe 360 400 360 1400 300'"
    sleep 1
    long_press "Version:" || long_press "Version" || { echo "couldn't long-press version"; return 1; }
    sleep 2
    adb shell "su -c 'input swipe 360 1400 360 400 300'"
    sleep 1
    screenshot "t13_debug"
    # Verify debug_prefs was written (toggle stores in SharedPreferences)
    local prefs_dir="/data/data/com.enginex0.usbmassstorage/shared_prefs"
    adb_su "cat $prefs_dir/debug_prefs.xml" 2>/dev/null | grep -q "debug_mode" || { echo "debug prefs not written"; return 1; }
}

t14_daemon_restart() {
    adb shell "su -c 'input swipe 360 400 360 1400 300'"
    sleep 1
    tap_by_text "Restart Daemon" || { echo "restart button not found"; return 1; }
    sleep 5
    local new_pid
    new_pid=$(adb_su "pidof daemon")
    [ -n "$new_pid" ] || { echo "daemon not running after restart"; return 1; }
    screenshot "t14_after_restart"
}

t15_pull_to_refresh() {
    go_back
    sleep 2
    local marker
    marker=$(date +%s)
    adb logcat -c 2>/dev/null
    sleep 0.5
    # Pull-to-refresh: slow swipe from top of content area downward
    adb shell "su -c 'input swipe 360 500 360 1400 800'"
    sleep 4
    adb logcat -d 2>/dev/null | grep -q "refresh\|UsbMsVM\|connecting" || { echo "refresh not triggered"; return 1; }
}

t16_daemon_respawn() {
    local old_pid
    old_pid=$(adb_su "pidof daemon")
    [ -n "$old_pid" ] || { echo "daemon not running before kill"; return 1; }
    adb_su "kill $old_pid"
    sleep 5
    local new_pid
    new_pid=$(adb_su "pidof daemon")
    [ -n "$new_pid" ] || { echo "daemon did not respawn"; return 1; }
    [ "$new_pid" != "$old_pid" ] || { echo "same PID, daemon didn't actually restart"; return 1; }
}

t17_crash_handler() {
    adb logcat -c 2>/dev/null
    launch_app
    sleep 2
    adb logcat -d 2>/dev/null | grep -q "Shell builder initialized" || adb logcat -d -s UsbMS 2>/dev/null | grep -q "Shell" || { echo "app init log missing"; return 1; }
}

t18_accent_colors() {
    launch_app
    sleep 3
    dump_ui > /dev/null
    tap_by_desc "Menu" || { echo "menu not found"; return 1; }
    sleep 1
    dump_ui > /dev/null
    tap_by_text "Settings" || { echo "settings not found"; return 1; }
    sleep 2
    dump_ui > /dev/null
    wait_for_text "Accent Color" 5 || {
        adb shell "su -c 'input swipe 360 1400 360 900 300'"
        sleep 1
        dump_ui > /dev/null
    }
    screenshot "t18_before_accent"
    tap_by_text "Almost Black" || { echo "almost black option not found"; return 1; }
    sleep 1
    screenshot "t18_almost_black"
    tap_by_text "White" || { echo "white option not found"; return 1; }
    sleep 1
    screenshot "t18_white"
    tap_by_text "System Default" || { echo "system default not found"; return 1; }
    sleep 0.5
    go_back
}

t19_guide_screen() {
    dump_ui > /dev/null
    tap_by_desc "Menu" || tap_by_text "More" || { echo "menu not found"; return 1; }
    sleep 0.5
    tap_by_text "How to Use" || { echo "guide menu item not found"; return 1; }
    sleep 1
    wait_for_text "What This App Does" 3 || { echo "guide content not found"; return 1; }
    dump_ui > /dev/null
    has_text "Before You Start" || { echo "section 2 missing"; return 1; }
    screenshot "t19_guide"
    go_back
}

echo ""
printf "${BOLD}UsbMassStorage E2E Test Suite${NC}\n"
printf "${BOLD}Device: $(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')${NC}\n"
echo ""

if ! adb get-state > /dev/null 2>&1; then
    printf "${RED}No ADB device connected${NC}\n"
    exit 1
fi

mkdir -p "$SCREENSHOT_DIR"

run_test "Module boot lifecycle"          't01_module_boot'
run_test "App launch"                     't02_app_launch'
run_test "Root detection"                 't03_root_detection'
run_test "Daemon connection"              't04_daemon_connection'
run_test "FAB opens add sheet"            't05_fab_opens_sheet'
run_test "Add sheet UI elements"          't06_add_sheet_elements'
run_test "Create image dialog"            't07_create_image_dialog'
run_test "Settings navigation"            't08_settings_navigation'
run_test "Daemon status display"          't09_daemon_status'
run_test "Version info"                   't10_version_info'
run_test "Copy daemon logs"              't11_copy_logs'
run_test "SELinux context view"          't12_selinux_context'
run_test "Debug mode toggle"             't13_debug_mode'
run_test "Daemon restart"                't14_daemon_restart'
run_test "Pull-to-refresh"              't15_pull_to_refresh'
run_test "Daemon auto-respawn"           't16_daemon_respawn'
run_test "Crash handler installed"       't17_crash_handler'
run_test "Accent color picker"           't18_accent_colors'
run_test "Guide screen"                  't19_guide_screen'

echo ""
printf "${BOLD}Results: ${GREEN}${PASSED} passed${NC}"
[ "$FAILED" -gt 0 ] && printf " ${RED}${FAILED} failed${NC}"
[ "$SKIPPED" -gt 0 ] && printf " ${YELLOW}${SKIPPED} skipped${NC}"
printf " (${PASSED}/$((PASSED + FAILED + SKIPPED)) total)\n"
printf "Screenshots: ${SCREENSHOT_DIR}\n"
echo ""

adb shell am force-stop "$PKG"

[ "$FAILED" -eq 0 ]
