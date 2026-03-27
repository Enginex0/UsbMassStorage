## v4.0

- One-shot create, format, and mount pipeline for streamlined disk setup
- Image manager with batch select-to-delete and clear unmounted
- Auto-mount saved images at boot without opening the app
- Visual refresh: rotating backgrounds, animated cards, modernized sheets
- Auto-increment disk names with creation timestamps
- Module banner and action script for root manager integration
- Batch image operations and improved image lifecycle management
- Bypass FUSE for configfs lun writes on Android 11+ (direct sysfs path)
- Merged SELinux policy into sepolicy.rule for Magisk compatibility
- Hardened bootloop guard with retry loops and resource leak fixes
- Socket read timeout and connection reliability for daemon client
- Fixed device removal index mismatch and coroutine lifecycle
- Escaped single quotes in automount config paths
- CI pipeline with auto-build, signing, and release publishing

## v3.1

- Full app and installer localization with 32 languages
- Auto-detect device locale during module installation
- Accent color picker (system default, almost black, white)
- In-app usage guide accessible from menu
- Material 3 companion app with pull-to-refresh and bottom sheets
- SELinux enforcing with dedicated msd_daemon domain
- Boot guard with exponential backoff daemon respawn
- Multi-ABI support (arm64, armv7, x86_64)
- Create virtual disk images from the app
- Mount up to 8 USB devices simultaneously
