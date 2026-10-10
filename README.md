# Scanora

<p align="center">
  <img src="https://raw.githubusercontent.com/Cypher501/Scanora/main/assets/logo.png" width="140" alt="Scanora Logo">
</p>

<h3 align="center">
Offline Android-Based Academic Report Viewer & Institutional Utility Platform
</h3>

<p align="center">
  Barcode Scanning • Offline PDF Retrieval • Local Media Server • Android TV Support
</p>

---

# 📌 Overview

Scanora is an offline Android-based institutional utility platform designed for educational environments. The system enables authorized users to scan student ID barcodes and instantly retrieve locally stored academic PDF reports without requiring internet connectivity or cloud infrastructure.

The platform combines barcode scanning, PDF viewing, local media serving, FTP-based file management, MJPEG monitoring, advertisement display systems, and exam circular distribution into a unified Android TV-compatible solution.

Scanora is optimized for:
- Educational institutions
- Result kiosks
- Academic administration systems
- Offline campus infrastructure
- Android TV environments

---

# ✨ Features

## 📷 Barcode-Based Student Report Retrieval
- Real-time barcode scanning using Google ML Kit
- Automatic PDF mapping based on student register numbers
- Instant academic report retrieval

## 📄 Offline PDF Viewer
- Local PDF rendering
- Fast loading
- Android TV optimized viewing experience

## 🌐 Embedded Local Media Server
- Local HTML dashboard
- Institutional advertisement display
- Exam circular hosting
- JPG and MP4 support

## 📂 FTP File Management
- Wireless file transfer
- Remote PDF management
- Local network administration

## 🎥 MJPEG Camera Monitoring
- Live camera preview
- Android-compatible monitoring
- Local stream support

## 🪞 Mirror Mode
- Real-time camera mirror utility
- Camera2 API integration

## 📺 Android TV Optimized
- D-pad navigation support
- TV-friendly UI scaling
- Large-screen compatibility

## 📢 Advertisement System
- Automatic image slideshow
- MP4 video playback
- Institutional announcements

## 📝 Exam Circular Management
- Dynamic PDF listing
- Local circular distribution
- Browser-based access

---

# 🏗️ System Architecture

```text
USB Camera + Mouse
        ↓
Google PL Mini / Android TV
        ↓
      Scanora
        ↓
 ┌───────────────────────┐
 │ Barcode Scanner       │
 │ PDF Viewer            │
 │ Media Server          │
 │ FTP Manager           │
 │ MJPEG Streamer        │
 │ Exam Circular System  │
 │ Advertisement System  │
 └───────────────────────┘
        ↓
Local Storage / Local Network
```

---

# 🧠 Technology Stack

| Technology | Purpose |
|---|---|
| Kotlin | Core Android Development |
| Android Studio | Development Environment |
| CameraX | Camera Integration |
| Google ML Kit | Barcode Scanning |
| NanoHTTPD | Embedded HTTP Server |
| Apache FTPServer | FTP File Management |
| AndroidPdfViewer | PDF Rendering |
| ExoPlayer / Media3 | Video Playback |

---

# 📁 Folder Structure

```text
Documents/
└── Exam/
    ├── Circulars/
    │     *.pdf
    │
    └── Ads/
          *.jpg
          *.jpeg
          *.png
          *.mp4
```

Student academic reports:

```text
Documents/StudentFiles/
```

Example:

```text
D_24_EE_1797.pdf
```

---

# 🔐 Offline-First Architecture

Scanora is designed around a local-first deployment model.

The system:
- does NOT require cloud hosting
- does NOT require centralized databases
- operates entirely on local storage
- supports local network access only

This improves:
- privacy
- reliability
- deployment simplicity
- low-connectivity usability

---

# 🌐 Local Media Server

The embedded media server provides browser-accessible institutional content.

Supported:
- PDF circulars
- JPG/JPEG/PNG advertisements
- MP4 video advertisements

Example:

```text
http://192.168.x.x:8081
```

---

# 📷 Barcode Workflow

```text
Student ID Barcode
        ↓
CameraX + ML Kit Detection
        ↓
Barcode Value Extraction
        ↓
Filename Conversion
        ↓
Local PDF Search
        ↓
Academic Report Display
```

---

# 📺 Android TV Support

Scanora is optimized for:
- Android TV Boxes
- Google TV Devices
- HDMI Institutional Displays
- Large-screen Result Kiosks

---

# 🚀 Installation

## Requirements

- Android Studio
- Android SDK
- Kotlin
- Android Device / Android TV

---

## Clone Repository

```bash
git clone https://github.com/Cypher501/Scanora.git
```

---

## Open in Android Studio

```text
File → Open → Scanora
```

---

## Build Project

```text
Build → Make Project
```

---

# 🔧 Permissions

Scanora may require:

- Camera Permission
- Storage Permission
- Local Network Access

Depending on enabled modules.

---

# 📌 Use Cases

- College Reception Systems
- Academic Result Kiosks
- Examination Halls
- Student Help Desks
- Institutional Media Displays
- Offline Administrative Infrastructure

---

# 🛡️ Privacy

Scanora is designed for local institutional usage.

The application:
- does not upload student data
- does not use cloud synchronization
- does not rely on remote servers

All files remain locally controlled.

---

# ⚠️ Disclaimer

Scanora is an independent educational utility project developed for institutional and academic assistance purposes.

The application is unofficial and is not affiliated with or endorsed by any institution unless explicitly stated.

---

# 📜 License

MIT License

Copyright (c) 2026 PokyMantella501

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files to deal in the Software without restriction.

---

# 👨‍💻 Developer

### PokyMantella501
AI & Software Development Enthusiast

Developed as part of educational and institutional automation initiatives.

---

# ⭐ Future Enhancements

- Biometric Authentication
- Cloud Synchronization
- AI-Based Student Analytics
- NFC Student Cards
- Attendance Integration
- Institutional Dashboard
- Multi-Institute Support

---

# 📬 Contributions

Suggestions, improvements, and institutional collaboration ideas are welcome.

---

# 🔥 Scanora

> Offline-first institutional automation for modern educational environments.
