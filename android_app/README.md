# Smart Garbage Management System (Android Native)

A native Android application written in **Kotlin** using **Jetpack Compose** and **Material 3**, architected with **Clean Architecture & MVVM** principles for a major B.Tech Smart City project.

---

## 🏛️ Technology Stack

- **Language:** Kotlin 2.2.x / 2.0.x
- **UI Toolkit:** Jetpack Compose with Material 3 Design
- **Architecture:** Clean Architecture + MVVM (Model-View-ViewModel)
- **Session Persistence:** Jetpack DataStore Preferences
- **Navigation:** Jetpack Navigation Compose
- **Asynchronous:** Kotlin Coroutines & StateFlow
- **Location:** Android Location API (GPS & Network Provider with manual fallback)
- **Camera & Media:** Android Activity Result APIs + FileProvider
- **Networking:** Retrofit 2 + OkHttp 3 + Gson Converter
- **Build Tool:** Gradle (Kotlin DSL `build.gradle.kts`)
- **Min SDK:** 24 (Android 7.0+)
- **Target / Compile SDK:** 35 / 37

---

## 🔑 Development Demo Accounts

Use these preloaded demo credentials to test the portals:

| Role | Email | Password | Preloaded Profile Attributes |
|---|---|---|---|
| **Citizen** | `citizen@example.com` | `password123` | Address: *Sector 4, Green Park, Smart City* |
| **Municipal Officer** | `officer@example.com` | `password123` | Employee ID: `MC-OFF-2026`, Ward: `Ward 12 - Central Zone` |
| **Garbage Driver** | `driver@example.com` | `password123` | Driver ID: `DRV-882`, Vehicle: `DL-01-GB-4040` |

*Note: In the Login Screen, you can tap any of the demo account chips under "Development Demo Accounts" to automatically populate the credentials and matching role.*

---

## 📱 Step 3 Citizen Portal Features

1. **Citizen Dashboard:**
   - Personalized greeting with logged-in citizen's name.
   - Quick action cards: **Report Garbage**, **My Complaints** (with live count badge), and **Emergency Garbage Pickup**.
   - Coming-soon cards: **Track Trucks** (vehicle GPS tracking in Step 5), **Scan Bin QR** (smart dustbin QR check-in), and **Cleanliness Feedback & Rewards**.
   - Recent complaints list showing the 3 latest reports with status badges and emergency tags.

2. **Report Garbage Form (`ReportGarbageScreen`):**
   - **Section 1: Garbage Type Selection**: Overflowing Bin, Festival Waste, Event Waste, Roadside Garbage, Flood Waste, Other Waste.
   - **Section 2: Description**: Multiline description with 500-character counter.
   - **Section 3: Image Attachment**: Camera photo capture with runtime permission & FileProvider, plus gallery photo picker with preview and remove/replace.
   - **Section 4: Video Attachment**: Choose/record video file info display with remove action.
   - **Section 5: Location Capture**: "Use Current Location" button acquiring live GPS coordinates via `LocationManager`, with manual text location fallback.
   - **Section 6: Emergency Pickup**: Switch with warning banner prioritizing urgent hazards.
   - **Section 7: Submit**: Generates unique complaint ID (`SGM-2026-XXXXXX`) and saves to the local repository.

3. **Complaint Submission Celebration (`ComplaintSubmittedScreen`):**
   - Displays prominent Complaint ID, garbage type, emergency indicator, and captured coordinates/address.
   - Direct buttons to **View Complaint Details** or **Back to Dashboard**.

4. **My Complaints (`MyComplaintsScreen`):**
   - Chronological list of all reports submitted by the citizen.
   - Filter chips: `All`, `Submitted`, `Assigned`, `In Progress`, `Cleaned`, `Verified`.
   - Empty state when no reports match.

5. **Complaint Details (`ComplaintDetailsScreen`):**
   - Shows Complaint ID, status badge, garbage type description, attached photo preview, video attachment indicator, GPS location/address, timestamp, and citizen info.
   - Status step timeline: `Submitted` (active) -> `Assigned` (pending) -> `Pickup In Progress` (pending) -> `Cleaned` (pending) -> `Verified` (pending).

---

## 📁 Project Architecture & Structure

```
android_app/
├── build.gradle.kts                        # Root build configuration (AGP 9.1.0)
├── settings.gradle.kts                     # Project repositories and module definition
├── gradle.properties                       # AndroidX, JVM memory, non-transitive R class
├── local.properties                        # Android SDK directory path
├── gradlew & gradlew.bat                   # Gradle wrapper binaries
├── gradle/wrapper/                         # Gradle wrapper jar & properties
├── README.md
└── app/
    ├── build.gradle.kts                    # App dependencies (Compose, Material 3, DataStore, Retrofit)
    ├── proguard-rules.pro                  # Proguard obfuscation rules
    └── src/
        └── main/
            ├── AndroidManifest.xml         # Manifest (Permissions, FileProvider, MainActivity)
            ├── res/
            │   ├── xml/file_paths.xml      # FileProvider paths for camera capture
            │   ├── values/                 # strings.xml, colors.xml, themes.xml
            │   ├── drawable/               # Icons & adaptive vector drawables
            │   └── mipmap-anydpi-v26/      # Launcher icons
            └── java/com/garbage/management/
                ├── MainActivity.kt         # Edge-to-edge host & ViewModel injections
                ├── SmartGarbageApp.kt      # Application class initializing AppContainer
                │
                ├── data/
                │   ├── model/              # UserDto, ApiResponseDto, LoginRequestDto
                │   ├── remote/             # ApiService, ApiClient builder
                │   ├── local/              # SessionManager (DataStore), MockAuthDataSource, LocalComplaintDataSource
                │   └── repository/         # AuthRepositoryImpl, ComplaintRepositoryImpl
                │
                ├── domain/
                │   ├── model/              # User, UserRole, AuthState, GarbageComplaint, GarbageType, ComplaintStatus
                │   ├── repository/         # AuthRepository, ComplaintRepository contracts
                │   └── usecase/            # LoginUseCase, RegisterUseCase, LogoutUseCase
                │                           # SubmitComplaintUseCase, GetCitizenComplaintsUseCase, GetComplaintByIdUseCase
                │
                ├── presentation/
                │   ├── navigation/         # Screen, AppNavGraph
                │   ├── viewmodel/          # AuthViewModel, ComplaintViewModel
                │   ├── screens/
                │   │   ├── splash/         # SplashScreen (Session check routing)
                │   │   ├── auth/           # LoginScreen, RegisterScreen, ForgotPasswordScreen
                │   │   ├── citizen/        # CitizenHomeScreen (Full Dashboard)
                │   │   │                   # ReportGarbageScreen (7-section form)
                │   │   │                   # ComplaintSubmittedScreen (Celebration view)
                │   │   │                   # MyComplaintsScreen (Filterable list)
                │   │   │                   # ComplaintDetailsScreen (Details & timeline)
                │   │   ├── officer/        # OfficerHomeScreen
                │   │   └── driver/         # DriverHomeScreen
                │   ├── components/         # AppTopBar, PrimaryAppButton, PortalNavCard, RoleBadge
                │   │                       # UriImageViewer, ComplaintStatusBadge, StatusTimelineView, LocationHelper
                │   └── theme/              # Color.kt, Theme.kt, Type.kt
                │
                ├── utils/                  # Constants.kt, Resource.kt
                │
                └── di/                     # AppContainer.kt
```

---

## 🚀 How to Run the Application

### Via Command Line:
```powershell
cd c:\FlutterDev\projects\garbage_management\android_app

# Assemble debug APK
.\gradlew.bat assembleDebug

# Install and run on connected device/emulator
.\gradlew.bat installDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`
