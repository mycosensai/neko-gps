# Neko GPS — developer tooling

Small helpers for working with the Android SDK and emulator on a Windows
development machine. Everything here is optional; the `gradlew` wrapper is
sufficient for building.

## Prerequisites

`ANDROID_SDK_ROOT` must point at your SDK. The scripts fall back to
`%LOCALAPPDATA%\Android\Sdk` when it is unset.

```bash
export ANDROID_SDK_ROOT="$LOCALAPPDATA/Android/Sdk"
```

---

## `emulator.sh`

Unified emulator helper (run from a bash shell such as Git Bash).

```bash
./tools/emulator.sh status          # list AVDs, running devices, acceleration state
./tools/emulator.sh accel-check     # is a hypervisor available?
./tools/emulator.sh create <name> <package>   # create an AVD
./tools/emulator.sh boot <name>     # boot an AVD headless and wait for it
./tools/emulator.sh install <apk>   # install an APK on the running device
./tools/emulator.sh logcat          # tail app + crash logs
./tools/emulator.sh kill            # stop all running emulators
```

### Example

```bash
./tools/emulator.sh create neko "system-images;android-34;google_apis;x86_64"
./tools/emulator.sh boot neko
./tools/emulator.sh install app/build/outputs/apk/debug/app-debug.apk
```

---

## Hardware acceleration

x86/x86_64 system images **require** a hypervisor. Check with:

```bash
./tools/emulator.sh accel-check
```

If it reports *"hypervisor driver is not installed"*, then either:

1. **Intel VT-x / AMD-V is disabled in firmware.** Enable
   *Intel Virtualization Technology* (or *SVM Mode*) in BIOS/UEFI, save, reboot.
   Confirm in Windows with:
   ```powershell
   (Get-CimInstance Win32_ComputerSystem).HypervisorPresent
   Get-ComputerInfo -Property HyperVRequirementVirtualizationFirmwareEnabled
   ```
   `VirtualizationFirmwareEnabled` must be `True`.

2. **The AEHD driver is not installed.** Run the installer *elevated*:
   ```bat
   %LOCALAPPDATA%\Android\Sdk\extras\google\Android_Emulator_Hypervisor_Driver\silent_install.bat
   ```
   Then verify the service exists:
   ```bat
   sc query aehd
   ```

> Emulator 37.x refuses to run x86 images without acceleration, and `-accel off`
> (pure TCG software emulation) is unreliable — it crashes or hangs during
> `gfxstream` initialisation. ARM images are not a workaround: v37 reports
> `FATAL | QEMU2 emulator does not support arm64 CPU architecture`.
> **Enabling VT-x in firmware is the only reliable fix.**

---

## Graphics backends

If the emulator crashes on start, try a different renderer:

```bash
emulator -avd <name> -gpu swangle    # ANGLE over SwiftShader (most compatible)
emulator -avd <name> -gpu host       # host GPU (needs working drivers)
emulator -avd <name> -gpu swiftshader
```

Valid modes in emulator 37.x: `auto`, `host`, `software`, `lavapipe`,
`swiftshader`, `swangle`. Note that `angle_indirect` is **not** valid and
silently falls back to `auto`.
