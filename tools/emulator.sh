#!/usr/bin/env bash
#
# Neko GPS — emulator helper
#
#   ./tools/emulator.sh status
#   ./tools/emulator.sh accel-check
#   ./tools/emulator.sh create <avd-name> <sdk-package>
#   ./tools/emulator.sh boot <avd-name> [gpu-mode]
#   ./tools/emulator.sh install <path-to.apk>
#   ./tools/emulator.sh logcat
#   ./tools/emulator.sh kill
#
set -uo pipefail

# ── Locate the SDK ────────────────────────────────────────────────────────────
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK_ROOT" ]; then
    if [ -n "${LOCALAPPDATA:-}" ]; then
        SDK_ROOT="$LOCALAPPDATA/Android/Sdk"
    else
        SDK_ROOT="$HOME/Android/Sdk"
    fi
fi

EMULATOR="$SDK_ROOT/emulator/emulator"
[ -x "$EMULATOR.exe" ] && EMULATOR="$EMULATOR.exe"
ADB="$SDK_ROOT/platform-tools/adb"
[ -x "$ADB.exe" ] && ADB="$ADB.exe"
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager.bat"
AVDMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/avdmanager.bat"

DEFAULT_GPU="swangle"     # most compatible renderer; see tools/README.md
BOOT_TIMEOUT_MIN=30

die() { echo "error: $*" >&2; exit 1; }

require_sdk() {
    [ -d "$SDK_ROOT" ] || die "Android SDK not found at '$SDK_ROOT'. Set ANDROID_SDK_ROOT."
    [ -e "$EMULATOR" ] || die "emulator binary not found under '$SDK_ROOT/emulator'."
}

# ── Commands ──────────────────────────────────────────────────────────────────
cmd_status() {
    require_sdk
    echo "SDK root : $SDK_ROOT"
    echo
    echo "== AVDs =="
    "$EMULATOR" -list-avds 2>/dev/null || echo "(none)"
    echo
    echo "== Running devices =="
    "$ADB" devices 2>/dev/null | sed '1d' | grep -v '^$' || echo "(none)"
    echo
    echo "== Acceleration =="
    "$EMULATOR" -accel-check 2>&1 | tail -n +2 || true
}

cmd_accel_check() {
    require_sdk
    "$EMULATOR" -accel-check 2>&1 || true
}

cmd_create() {
    local name="${1:-}" pkg="${2:-}"
    [ -n "$name" ] && [ -n "$pkg" ] || die "usage: create <avd-name> <sdk-package>"
    require_sdk
    echo "Installing '$pkg'…"
    yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" "$pkg" >/dev/null 2>&1 \
        || die "sdkmanager failed for '$pkg'"
    echo "Creating AVD '$name'…"
    echo "no" | "$AVDMANAGER" create avd -n "$name" -k "$pkg" -d pixel --force
    echo "Done: $name"
}

cmd_boot() {
    local name="${1:-}" gpu="${2:-$DEFAULT_GPU}"
    [ -n "$name" ] || die "usage: boot <avd-name> [gpu-mode]"
    require_sdk

    echo "Booting '$name' (gpu=$gpu, headless)…"
    "$EMULATOR" -avd "$name" \
        -no-snapshot -no-audio -no-boot-anim \
        -gpu "$gpu" -no-window -no-metrics \
        >"/tmp/neko-emulator-$name.log" 2>&1 &
    local emu_pid=$!

    echo "Waiting up to ${BOOT_TIMEOUT_MIN}m for boot (log: /tmp/neko-emulator-$name.log)…"
    local waited=0
    while [ "$waited" -lt $((BOOT_TIMEOUT_MIN * 60)) ]; do
        if ! kill -0 "$emu_pid" 2>/dev/null; then
            echo "Emulator exited early. Last log lines:" >&2
            tail -n 20 "/tmp/neko-emulator-$name.log" >&2
            return 1
        fi
        local booted
        booted=$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n')
        if [ "$booted" = "1" ]; then
            echo "Boot completed after $((waited / 60))m $((waited % 60))s."
            return 0
        fi
        sleep 10
        waited=$((waited + 10))
    done
    echo "Timed out after ${BOOT_TIMEOUT_MIN}m." >&2
    return 1
}

cmd_install() {
    local apk="${1:-}"
    [ -n "$apk" ] || die "usage: install <path-to.apk>"
    [ -f "$apk" ] || die "no such file: $apk"
    require_sdk
    "$ADB" wait-for-device
    "$ADB" install -r "$apk"
}

cmd_logcat() {
    require_sdk
    "$ADB" logcat -v color NekoGPS:V AndroidRuntime:E ActivityManager:I '*:S'
}

cmd_kill() {
    require_sdk
    pkill -f 'qemu-system' 2>/dev/null
    pkill -f 'emulator' 2>/dev/null
    "$ADB" devices 2>/dev/null | sed '1d' | awk '{print $1}' | while read -r s; do
        [ -n "$s" ] && "$ADB" -s "$s" emu kill 2>/dev/null
    done
    echo "Stopped running emulators."
}

case "${1:-}" in
    status)       cmd_status ;;
    accel-check)  cmd_accel_check ;;
    create)       shift; cmd_create "$@" ;;
    boot)         shift; cmd_boot "$@" ;;
    install)      shift; cmd_install "$@" ;;
    logcat)       cmd_logcat ;;
    kill)         cmd_kill ;;
    *)
        sed -n '3,12p' "$0" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
