# Tadami (タダ見)

Tadami is a learning project heavily inspired by the free and open source manga reader Tachiyomi with anime capabilities instead for Android 9.0 and above.
It is not a production ready app and mostly a protoype used to learn android development with kotlin, jetpack compose...

## Download
Get the latest version from the [releases page](https://github.com/tadamiorg/tadami/releases/latest).

## Building & releasing
Build with JDK 17, e.g. `./gradlew :app:assembleDebug`. No secrets are needed: without a keystore, debug builds use the default debug key and release builds are unsigned.
Releases are built, signed and published by the Release workflow (Actions > Release > Run workflow), which computes the version from the commits since the last release. See [docs/RELEASING.md](docs/RELEASING.md) for the required secrets and the release flow.

## Ui

### Library

![Library Screen](https://drive.google.com/uc?id=1kRLHRz3BGFvzsvW06hXmxB5lxGJM7Yrr)

### Sources

![Sources Screen](https://drive.google.com/uc?id=17Xr_98r_kwacnRBskuoN_At9qMr5i9Jy)

### Player

![Player Screen](https://drive.google.com/uc?id=1ImD_0gkX4fwCccGv_e1FJm36sMHmlXI3)

### Anime details

![Anime Details Screen](https://drive.google.com/uc?id=1T6Fss6CrqaJXve4tQbeXAY3UqPkjgN82)

### Settings

![Settings Screen](https://drive.google.com/uc?id=11wAy3DedJt78pbCNQTmrCsjWHdAR2Xyo)
