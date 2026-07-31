## **CanvaGrid - App Assistant for Image Transfer on Canvas**

*Developed by Elpida-Kalliopi Anthopoulou | Aristotle University of Thessaloniki (June 2026)*

[![Latest Release](https://img.shields.io/github/v/release/elkathop-776/canvagrid---android?color=green&label=Download%20APK)](https://github.com/elkathop-776/canvagrid---android/releases/latest)


## Overview
The transition from a digital reference photo to a physical canvas is a common challenge for artists—manually transferring proportions often leads to distortion or awkward cropping. **CanvaGrid** modernizes the traditional grid method by offering a native Android application that automates scaling, customizes grid overlays, and recommends optimal canvas sizes.

Whether you are a traditional painter working with oil/acrylic, an art student learning perspective, or a hobbyist, CanvaGrid streamlines your pre-drawing workflow.

## Key Features

* **Canvas Size Recommendation Engine:** Upload any image, and the app's aspect-ratio matching algorithm will calculate its proportions and suggest the best standard commercial canvas size (e.g., $40\times40$ cm, $50\times70$ cm) to avoid image distortion.
* **Real-Time Grid Customization:**
  * **Density:** Granular slider control ranging from $2\times2$ up to $12\times12$ grid lines.
  * **Opacity:** Alpha-transparency control to keep grid lines visible without hiding fine artwork details.
  * **High-Contrast Colors:** Toggle line colors (Red, Green, Blue, White, Black) to contrast against any dark or bright background.
* **Local SQLite Project Persistence:** Save active projects, grid settings, and image URIs locally. Rename, preview, or edit your saved work across multiple sessions.
* **Robust State Preservation:** Full lifecycle handling guarantees that dialogs (renaming, help modals) and active slider configurations aren't lost during screen rotations.
* **Privacy-Focused (Scoped Storage):** Built using Android's Storage Access Framework (SAF) and persistent URI permissions—no broad storage access required.

## Installation

1. Download the executable APK directly from the **[Latest Release](../../releases/latest)** page.
2. Transfer or open the `canvagrid.apk` file on your Android device.
3. Allow installation from unknown sources if prompted by your system.

## Architecture & Tech Stack

* **Platform:** Native Android (Java)
* **Architecture:** Modular Activity/Fragment layout with custom View rendering
* **Database:** SQLite (`ProjectDbHelper`, `ProjectContract`)
* **Graphics Rendering:** Custom `GridView` using low-level Android Transformation Matrices (`MSCALE_X`, `MSCALE_Y`) for accurate pixel-boundary grid drawing
* **Storage:** Storage Access Framework (SAF) with Scoped Storage architecture
* **Export Engine:** Native Android `Canvas` & `Paint` bitmap compression saved directly via `MediaStore`

## Project Documentation & Report

This repository includes the complete project documentation:
* **Full Academic Report:** Available inside the [`docs/`](./docs/) directory as `canvagrid-report.pdf`.

## Future Roadmap

* **On-Device Vision-Language Model (VLM):** Dynamic canvas recommendations based on visual content complexity rather than pure aspect ratio.
* **Augmented Reality (AR) Overlay:** Projecting customized grid lines directly onto a physical blank canvas using the phone's camera.
* **Cloud Synchronization:** Multi-device backup and restoration for saved SQLite project records.
* **Tablet Support:** Multi-pane fragment interface for larger displays.

## License & Credits

* **Author:** Elpida-Kalliopi Anthopoulou (AEM: 132)
* **Course:** Interactive Applications for Mobile Devices / Technologies of Interactive Systems
* **Institution:** Aristotle University of Thessaloniki
