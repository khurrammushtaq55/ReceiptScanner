# Smart Receipt Scanner (Android)

On-device receipt scanning and organization. Capture with camera, import from gallery/PDF, run on-device OCR (ML Kit), review & edit fields, and save to history. Private by design — data stays on your device.

---

## ✨ Features

- **Capture** with CameraX (tap-to-focus, quality JPEG).
- **Import** from **Gallery** (Photo Picker) and **PDF** (first page rendered via `PdfRenderer`).
- **OCR** using **ML Kit Text Recognition v2** (on device).
- **Parsing** (heuristics): merchant, date, currency, total, tax — integers near IDs/negatives are ignored.
- **Edit & Save** parsed fields in **Review**.
- **History** with thumbnails, search, and month grouping.
- **Dark theme** with Material 3 + **dynamic color** (Android 12+).
- **Adaptive launcher icon** (monochrome for themed icons).
- **Modern splash screen** (AndroidX SplashScreen).
- **Back navigation** with in-app back stack (`BackHandler`).

---

## 🧱 Tech Stack

- **Kotlin 2.0.x**, **Jetpack Compose**
- **CameraX** (preview + image capture)
- **ML Kit** Text Recognition v2 (on-device)
- **Room** (local database)
- **Koin** (DI)
- **PdfRenderer** (PDF → bitmap)
- **Coil** (thumbnails)
- **AndroidX SplashScreen** (splash)
- **Min SDK 26**, **Target SDK 36**

---

## 📁 Project Structure

