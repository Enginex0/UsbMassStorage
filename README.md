<p align="center">
  <h1 align="center">💾 USB Mass Storage</h1>
  <p align="center"><b>Turn Your Phone Into a USB Drive</b></p>
  <p align="center">Mount disk images as real USB storage devices. KernelSU / Magisk / APatch.</p>
  <p align="center">
    <img src="https://img.shields.io/badge/version-v3.1-blue?style=for-the-badge" alt="v3.1">
    <img src="https://img.shields.io/badge/Android-12%2B-green?style=for-the-badge&logo=android" alt="Android 12+">
    <a href="https://t.me/superpowers9"><img src="https://img.shields.io/badge/Telegram-community-blue?style=for-the-badge&logo=telegram" alt="Telegram"></a>
  </p>
</p>

<p align="center">
  🇺🇸 🇿🇦 🇸🇦 🇪🇸 🇨🇿 🇩🇰 🇩🇪 🇬🇷 🇮🇷 🇫🇮 🇫🇷 🇭🇺 🇮🇩 🇮🇹 🇮🇱 🇯🇵 🇰🇷 🇳🇱 🇳🇴 🇵🇱 🇧🇷 🇵🇹 🇷🇴 🇷🇺 🇷🇸 🇸🇪 🇹🇭 🇹🇷 🇺🇦 🇻🇳 🇨🇳 🇹🇼
</p>

---

## 🧬 What is USB Mass Storage?

When you connect your phone to a computer via USB cable, this module makes the computer see a **real USB storage device**, as if you plugged in a USB stick or CD drive. You pick a disk image file (`.img` or `.iso`) on your phone, and the computer can read and write to it directly.

No file transfer protocols, no MTP, no ADB. The computer sees a native block device.

> **This is not MTP or file transfer.** USB Mass Storage exposes raw block devices to the host computer through the kernel's USB gadget ConfigFS interface. The computer sees a real disk, not a file-sharing protocol. You can boot from it, format it, or use it exactly like a physical USB drive.

> **Who benefits?** The computer you plug into. Your phone acts as the hardware (like a USB stick), and any PC, Mac, or Linux machine sees a native USB disk. No drivers, no MTP, no special software needed on the computer side.

---

## 🔥 Why USB Mass Storage?

🔌 **Native Block Device** — The computer sees a real USB disk, not a file transfer protocol. Works with any OS, any file system, any tool that reads USB storage.

💿 **Boot From Your Phone** — Mount a bootable `.iso` as a CD-ROM drive. Install operating systems, run live CDs, or boot recovery tools directly from your phone.

📱 **Material 3 Companion App** — No terminal commands needed. Tap to mount, tap to eject. Full device management through a clean Compose UI with 32 language translations.

🛡️ **SELinux Enforcing** — The daemon runs in its own `msd_daemon` security domain with a dedicated sepolicy. No permissive hacks, no blanket root access.

🔧 **Multi-ABI Support** — arm64, armv7, and x86_64 binaries bundled. The installer detects your architecture and keeps only what's needed.

---

## ✨ Features

**USB Gadget**
- [x] **Mount `.img` files as USB drives** — read-write, read-only, or CD-ROM mode
- [x] **Mount `.iso` files as CD-ROM drives** — boot from ISO, install OS, run live CDs
- [x] **Multiple devices** — up to 8 simultaneously, each appears as a separate USB device on the computer
- [x] **Clean eject** — computer sees proper device disconnection

**Disk Management**
- [x] **Create virtual disk images** — specify size in MB/GB/GiB, created instantly
- [x] **Persistent configuration** — device mounts survive app restarts
- [x] **File descriptor passing** — zero-copy file access over Unix socket

**App & Module**
- [x] **Material 3 UI** — Jetpack Compose companion app with pull-to-refresh, bottom sheets, and snackbar alerts
- [x] **32 languages** — app and module installer auto-detect your device language
- [x] **Accent themes** — system default, almost black, and white color schemes
- [x] **In-app guide** — step-by-step usage tutorial accessible from the menu
- [x] **SELinux enforcing** — daemon runs in its own `msd_daemon` domain
- [x] **Multi-ABI** — arm64, armv7, x86_64 binaries included, installer picks the right one
- [x] **Boot guard** — automatic daemon restart with exponential backoff

---

## 📋 Requirements

> [!IMPORTANT]
> Root access is required. The daemon interacts with kernel USB gadget ConfigFS, which is a privileged operation.

**You need:**
1. Android 12 or above
2. A supported root manager: **KernelSU**, **Magisk**, or **APatch**
3. A USB cable connecting your phone to a computer

---

## 📱 Compatibility

### Root Managers

| Manager | Status | Notes |
|---|---|---|
| KernelSU | ✅ Tested | Full support including lifecycle scripts |
| Magisk | ✅ Supported | Standard module install |
| APatch | ✅ Supported | Standard module install |

### Tested Devices

| Device | Android | Status |
|---|---|---|
| Redmi 14C (2409BRN2CA) | 14 (SDK 34) | ✅ Daily driver |

> If you test on a different device, report your results in the [Telegram group](https://t.me/superpowers9).

---

## 🚀 Quick Start

1. **Download** the latest release ZIP from [Releases](https://github.com/Enginex0/UsbMassStorage/releases)
2. **Flash** via your root manager and reboot
3. **Open** the USB Mass Storage app
4. **Grant root** when prompted
5. **Tap +** to mount a disk image or create a new one

The computer detects a new USB device within seconds.

---

## ⚙️ Usage

### Mounting a Disk Image

Tap **+** on the main screen, then **Select disk image file** and pick your `.img` or `.iso`.

Choose the device type:

| Type | Use Case |
|---|---|
| **Read-Write** | Regular USB stick, computer can read and write freely |
| **Read-Only** | Write-protected USB, computer can only read |
| **CD-ROM** | Virtual CD/DVD drive, use for `.iso` files, OS installers, bootable media |

Tap **Mount**. Done.

### Creating a New Image

Tap **+**, then **Create new disk image**. Set a filename and size (e.g., `512 MB`, `2 GiB`). The image is created instantly on your phone.

After mounting, format it from the computer (FAT32, exFAT, or NTFS).

### Multiple Devices

Mount up to 8 devices at once. Each shows as a separate USB device on the computer. Tap a device card to change its type or eject it.

### Ejecting

Tap the eject icon on the device card. The computer sees the device disconnected cleanly.

---

## 🏗️ Architecture

```
┌──────────────┐    Unix Socket     ┌──────────────┐    ConfigFS     ┌──────────┐
│  Android App │ ◄──────────────► │  Rust Daemon  │ ◄────────────► │  Kernel  │
│  (Compose)   │    FD passing      │  (msd_daemon) │    USB gadget   │  USB UDC │
└──────────────┘                    └──────────────┘                 └──────────┘
```

**App** — Material 3 Compose UI. Picks files, sends file descriptors to the daemon over a Unix socket. No root needed in the app itself.

**Daemon** — Rust binary running as `msd_daemon` under SELinux. Receives file descriptors, writes USB gadget configuration to `/sys/kernel/config/usb_gadget/`. The kernel exposes the file as a USB mass storage LUN to the connected computer.

The app is a regular user app. The daemon handles all privileged operations.

---

## 🔨 Building from Source

**Prerequisites:** JDK 17, Android SDK (compile SDK 35), Rust toolchain with Android targets.

```bash
git clone https://github.com/Enginex0/UsbMassStorage.git
cd UsbMassStorage

# Build the app
./gradlew assembleRelease

# Package the module ZIP
bash scripts/package.sh --apk=app/build/outputs/apk/release/app-release.apk
```

Output ZIP lands in `out/`.

---

## 🔧 Common Issues

| Problem | Fix |
|---|---|
| Daemon not running | Reboot, or open Settings in the app and tap **Restart Daemon** |
| Root not granted | Open your root manager and grant USB Mass Storage root access |
| Computer doesn't detect USB | Reconnect the cable. Make sure USB debugging isn't overriding gadget mode |
| "AppFuse proxy" error | Your file picker is proxying the file. Use a different file manager (not Google Files) |

For other issues, ask in the [Telegram group](https://t.me/superpowers9).

---

## 💬 Community

<p align="center">
  <a href="https://t.me/superpowers9">
    <img src="https://img.shields.io/badge/⚡_JOIN_THE_GRID-SuperPowers_Telegram-black?style=for-the-badge&logo=telegram&logoColor=cyan&labelColor=0d1117&color=00d4ff" alt="Telegram">
  </a>
</p>

---

## 📄 License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

---

<p align="center">
  <b>💾 Because sometimes you just need a USB drive.</b>
</p>
