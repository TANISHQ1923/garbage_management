# Mid-Semester Demonstration Guide: Smart Garbage Management System

This guide outlines the step-by-step presentation script, architecture overview, demo accounts, and testing procedures for the **Smart Garbage Management System** half-semester evaluation (Steps 1 through 9).

---

## 1. System Architecture Overview

The Smart Garbage Management System is an end-to-end multi-tier platform built with clean architecture:

```
+-------------------------------------------------------------------------+
|                          Native Android Client                          |
|  - Jetpack Compose + Material 3                                         |
|  - MVVM Architecture + Coroutines / StateFlow                           |
|  - Role-based UI: Citizen, Municipal Officer, Garbage Driver            |
|  - OkHttp / Retrofit with Authorization Header Redaction                |
+-------------------+---------------------------------+-------------------+
                    | REST APIs (JSON)                | Direct Upload
                    v                                 v
+-----------------------------------+   +---------------------------------+
|          Node.js Backend          |   |       Cloud Media Storage       |
|  - Express.js + Mongoose          |   |  - Cloudinary Cloud Storage     |
|  - JWT Authentication & RBAC      |   |  - Before/After Cleaning Photos |
|  - Complaint Lifecycle Management |   |  - Complaint Video Evidence     |
+-------------------+---------------+   +---------------------------------+
                    | Internal HTTP                   ^
                    v (Timeout: 5s)                   |
+-----------------------------------+                 |
|       FastAPI Python AI Service   |                 |
|  - Computer Vision / Baseline     |                 |
|  - Waste Classification (8 types) |                 |
|  - Severity Estimation (4 levels) |                 |
+-------------------+---------------+                 |
                    |                                 |
                    v (Stores Image URLs & Metadata)  |
+-----------------------------------------------------+-------------------+
|                           MongoDB Atlas / Local                         |
|  - Users (Citizens, Officers, Drivers)                                  |
|  - Complaints (Lifecycle, AI analysis, Notes, History)                  |
|  - Driver Telemetry & Notifications                                     |
+-------------------------------------------------------------------------+
```

---

## 2. Safe Demo Accounts & Credentials

All demo accounts use the standard password: **`password123`**

### Citizen Accounts
| Name | Email | Role | Features Demonstrated |
| :--- | :--- | :--- | :--- |
| **Aarav Sharma** | `citizen@example.com` | `CITIZEN` | Standard reporting, overflowing bin complaints |
| **Priya Singh** | `citizen2@example.com` | `CITIZEN` | Festival waste, video attachment reporting |
| **Rahul Mehta** | `citizen3@example.com` | `CITIZEN` | Emergency pickup request, history review |

### Municipal Officer Accounts
| Name | Email | Role / ID | Jurisdiction |
| :--- | :--- | :--- | :--- |
| **Vikram Patel** | `officer@example.com` | `MUNICIPAL_OFFICER` (MC-OFF-2026) | Ward 12 - Central Zone |
| **Ananya Deshmukh** | `officer2@example.com` | `MUNICIPAL_OFFICER` (MC-OFF-2027) | Ward 14 - North Zone |

### Garbage Driver Accounts
| Name | Email | Driver ID | Vehicle | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Ramesh Kumar** | `driver@example.com` | `DRV-882` | `DL-01-GB-4040` (Heavy Compactor) | AVAILABLE |
| **Sunil Verma** | `driver2@example.com` | `DRV-104` | `DL-01-GB-5050` (Medium Tipper) | AVAILABLE |
| **Amitabh Ghosh** | `driver3@example.com` | `DRV-305` | `DL-01-GB-6060` (Electric Mini Van) | BUSY |
| **Rajesh Chauhan**| `driver4@example.com` | `DRV-412` | `DL-01-GB-7070` (Hydraulic Loader) | AVAILABLE |
| **Kavita Rao** | `driver5@example.com` | `DRV-520` | `DL-01-GB-8080` (Heavy Compactor) | OFFLINE |

---

## 3. Pre-Demo Setup & Service Startup

### Step 3.1: Start the AI Service (FastAPI)
```powershell
cd c:\FlutterDev\projects\garbage_management\ai_service
.\venv\Scripts\uvicorn.exe app.main:app --host 0.0.0.0 --port 8000
```
- Health check URL: `http://localhost:8000/api/health`
- Response: `{"status":"ok","service":"ai-waste-service","modelStatus":"DEVELOPMENT_BASELINE"}`

### Step 3.2: Seed the Database
```powershell
cd c:\FlutterDev\projects\garbage_management\backend
npm run seed
```
- Seeds 3 Citizens, 2 Officers, 5 Drivers, and 6 Complaints covering all 6 lifecycle stages (`SUBMITTED`, `ASSIGNED`, `PICKUP_IN_PROGRESS`, `CLEANED`, `VERIFIED`, `REJECTED`).
- Upserts are 100% idempotent and can be safely re-run before any presentation.

### Step 3.3: Start the Backend API (Node.js)
```powershell
cd c:\FlutterDev\projects\garbage_management\backend
npm run dev
```
- API URL: `http://localhost:5000/api`
- Health check: `http://localhost:5000/api/health`

### Step 3.4: Install & Launch the Android Application
The generated debug APK is located at:
```
android_app\app\build\outputs\apk\debug\app-debug.apk
```
Install on Android device or emulator:
```powershell
adb install -r android_app\app\build\outputs\apk\debug\app-debug.apk
```

---

## 4. Live Demonstration Script (End-to-End Walkthrough)

### Act 1: Citizen Waste Reporting
1. **Login as Citizen**:
   - Username: `citizen@example.com`, Password: `password123`
   - Lands on the Citizen Dashboard showing greeting, quick actions, and recent reports.
2. **File a New Garbage Report**:
   - Tap **"Report Garbage"**.
   - Select Garbage Type: `OVERFLOWING_BIN`.
   - Enter Description: `"Public dustbin overflowing at Sector 4 park, plastic containers spilled onto footpath."`
   - Capture/Select Photo: Choose an image from gallery or camera.
   - Capture Location: Tap **"Detect Current GPS Location"** (or enter manually).
   - Toggle Emergency if urgent.
   - Tap **"Submit Report"**.
3. **Observation**:
   - Real-time upload progress indicates media upload and submission.
   - Citizen receives instant confirmation dialog and in-app notification.
   - The complaint is immediately visible under **"My Reports"** with status `SUBMITTED`.

### Act 2: AI Waste Classification & Severity Analysis
1. **Explain the AI Workflow**:
   - The backend automatically forwards the uploaded image to the Python FastAPI microservice (`POST /api/predict`).
   - The computer vision baseline analyzes visual properties (color histogram, spatial distribution, edge density, entropy).
   - Results:
     - **Primary Category**: e.g., `PLASTIC` (Confidence: `91%`).
     - **Severity Level**: `HIGH` (Confidence: `86%`).
     - **Status**: `COMPLETED`.
   - Show that this happens asynchronously without blocking the citizen's report submission.

### Act 3: Municipal Officer Portal & Triage
1. **Logout & Login as Municipal Officer**:
   - Username: `officer@example.com`, Password: `password123`
   - Lands on the Officer Dashboard showing real-time statistics: Total, Pending, In Progress, Cleaned, and Emergency alerts.
2. **Review Triage & AI Results**:
   - Open **"All Complaints"**.
   - Filter by `SUBMITTED` or search for the newly filed complaint.
   - Tap to open complaint details.
   - **Showcase the AI Analysis Card**: Highlights AI Category (`PLASTIC`), Severity (`HIGH`), and confidence percentage.
3. **Assign Driver**:
   - Tap **"Assign Driver"**.
   - Select available driver **"Ramesh Kumar (DRV-882)"** with vehicle `DL-01-GB-4040`.
   - Confirm assignment.
   - Complaint status transitions to `ASSIGNED`.

### Act 4: Garbage Driver Workflow & Photo Evidence
1. **Logout & Login as Garbage Driver**:
   - Username: `driver@example.com`, Password: `password123`
   - Lands on Driver Task Screen showing assigned pickups and active tasks.
2. **Start Collection**:
   - Tap the assigned complaint (`SGM-2026-003890` or newly assigned).
   - (Optional) Take a **Before-Cleaning Photo**.
   - Tap **"Start Collection"**.
   - Status updates to `PICKUP_IN_PROGRESS`. Driver availability switches to `BUSY`.
3. **Complete Collection with Mandatory Photo Evidence**:
   - Note: Attempting to complete without an after-cleaning photo shows an explicit validation warning.
   - Capture or attach the **After-Cleaning Photo**.
   - Tap **"Complete Collection"**.
   - Photo is securely stored in Cloudinary cloud storage and linked to the complaint.
   - Status updates to `CLEANED`. Driver availability reverts to `AVAILABLE`.

### Act 5: Officer Verification & Closure
1. **Login back as Officer (`officer@example.com`)**:
   - Navigate to the complaint details.
   - Compare **Before-Cleaning** and **After-Cleaning** photo evidence side-by-side.
   - Add an Officer Note: `"Verified on-site: waste cleared and bins disinfected."`
   - Update status to **`VERIFIED`**.
2. **Citizen Confirmation**:
   - Log in as Citizen (`citizen@example.com`).
   - Open **"My Reports"** -> Complaint Details.
   - Citizen sees status `VERIFIED`, complete timeline history, officer note, and after-cleaning evidence.

---

## 5. Security & Robustness Features

1. **Header Redaction in Logs**:
   - Sensitive JWT tokens (`Authorization: Bearer ...`) are redacted from Android Logcat via OkHttp `HttpLoggingInterceptor.redactHeader("Authorization")`.
2. **Fail-Safe AI Degradation**:
   - If the AI microservice is temporarily unavailable, complaint creation **never fails**. The backend marks `aiAnalysisStatus: 'FAILED'` and allows the officer to manually triage the complaint without disruption.
3. **State Synchronization**:
   - All ViewModels (`OfficerViewModel`, `DriverViewModel`, `ComplaintViewModel`) synchronize active lists immediately upon state transitions, eliminating stale screen data.

---

## 6. Verification Pipeline

Run all test suites across the repository:

| Component | Command | Expected Result |
| :--- | :--- | :--- |
| **Python AI Microservice** | `cd ai_service && .\venv\Scripts\pytest.exe` | **10 / 10 Tests Passed** |
| **Node.js Express Backend** | `cd backend && npm test` | **39 / 39 Tests Passed** (3 suites) |
| **Android Kotlin Compilation**| `cd android_app && .\gradlew.bat compileDebugKotlin` | **BUILD SUCCESSFUL** |
| **Android Unit Tests** | `cd android_app && .\gradlew.bat testDebugUnitTest` | **BUILD SUCCESSFUL** |
| **Android Debug APK** | `cd android_app && .\gradlew.bat assembleDebug` | **BUILD SUCCESSFUL** (`app-debug.apk`) |
