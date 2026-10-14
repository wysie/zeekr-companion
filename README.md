# Zeekr Shortcut — archived personal-build branch

Development and combined APK builds have moved to **main**. This branch retains
older application code; it is no longer the branch to clone for new builds.

- [Current README and features](https://github.com/wysie/zeekr-shortcut-car/blob/main/README.md)
- [Current fork workflow](https://github.com/wysie/zeekr-shortcut-car/blob/main/FORK_WORKFLOW.md)
- [Download APKs](https://github.com/wysie/zeekr-shortcut-car/releases)
- [Credits and license](https://github.com/wysie/zeekr-shortcut-car/blob/main/NOTICE.md)

For the current combined app:

```bash
git clone --branch main https://github.com/wysie/zeekr-shortcut-car.git
cd zeekr-shortcut-car
bash ./gradlew assembleRelease
```

Existing release tags and APK downloads are preserved. New combined builds come
from `main`; a release tag identifies the exact source commit of that release.
