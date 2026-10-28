# Smart Garbage Management — Backend REST API & MongoDB Atlas

Production-minded Node.js + Express backend service with MongoDB Atlas integration, JWT authentication, and role-based access control for the Smart Garbage Management Application.

---

## 1. Prerequisites & Environment

- **Node.js**: v18.0.0 or later (tested on Node v22.17.1)
- **npm**: v9.0.0 or later (tested on npm 10.9.2)
- **MongoDB**: MongoDB Atlas Cloud cluster or local MongoDB instance (v6.0+)

---

## 2. Installation

Navigate to the `backend/` directory from the repository root:

```bash
cd backend
npm install
```

---

## 3. MongoDB Atlas Configuration

1. Create a free cluster on [MongoDB Atlas](https://www.mongodb.com/cloud/atlas).
2. Create a database user with read and write privileges under **Database Access**.
3. Under **Network Access**, add IP `0.0.0.0/0` (allow access from anywhere) or your current development IP.
4. Obtain your connection string under **Database > Connect > Drivers (Node.js)**:
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/garbage_management?retryWrites=true&w=majority
   ```

---

## 4. Environment Variables (`.env`)

Copy the template file to `.env`:

```bash
cp .env.example .env
```

Edit `.env` with your settings:

```env
PORT=5000
NODE_ENV=development
MONGODB_URI=mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/garbage_management?retryWrites=true&w=majority
JWT_SECRET=super_secret_jwt_key_for_smart_garbage_management_2026
JWT_EXPIRES_IN=7d
CLIENT_ORIGIN=*
```

> **Security Reminder**: Never commit `.env` or database credentials to version control. `.env` is listed in `.gitignore`.

---

## 5. Running the Backend

### Development Mode (with hot-reloading):
```bash
npm run dev
```

### Production Mode:
```bash
npm start
```

When started, the service listens on port `5000`:
- **Local Machine**: `http://localhost:5000`
- **Health Check**: `http://localhost:5000/api/health`
- **Android Emulator Base URL**: `http://10.0.2.2:5000/api/`

---

## 6. Preloading Demo Data (`npm run seed`)

Populate MongoDB with demo Citizen, Officer, and Driver accounts, pre-assigned complaints, and sample notifications:

```bash
npm run seed
```

### Demo Accounts

| Role | Email | Password | Details |
| :--- | :--- | :--- | :--- |
| **Citizen** | `citizen@example.com` | `password123` | Aarav Sharma (Green Park, Sector 4) |
| **Municipal Officer** | `officer@example.com` | `password123` | Vikram Patel (Emp ID: `MC-OFF-2026`, Ward 12) |
| **Garbage Driver** | `driver@example.com` | `password123` | Ramesh Kumar (Driver ID: `DRV-882`, Truck: `DL-01-GB-4040`) |

The seed script is idempotent and safe to run repeatedly.

---

## 7. API Endpoints Specification

### Health Check
- `GET /api/health` — Service and database connection status check

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Register a new user (`CITIZEN`, `MUNICIPAL_OFFICER`, `GARBAGE_DRIVER`)
- `POST /api/auth/login` — Authenticate and receive JWT Bearer token
- `GET /api/auth/me` — Get profile of authenticated user (*Bearer Token Required*)
- `POST /api/auth/forgot-password` — Password reset initiation

### Citizen Complaints (`/api/complaints`)
- `POST /api/complaints` — Submit a new garbage issue complaint (*Citizen Only*)
- `GET /api/complaints/my` — Get all complaints filed by the authenticated citizen (*Citizen Only*)
- `GET /api/complaints/:id` — Get single complaint details (*Authorized User*)
- `POST /api/complaints/:id/emergency` — Escalate complaint to emergency priority (*Citizen Only*)

### Municipal Officer (`/api/officer`)
- `GET /api/officer/complaints` — Filtered/sorted municipal complaints list (*Officer Only*)
- `GET /api/officer/complaints/:id` — Detailed complaint overview (*Officer Only*)
- `GET /api/officer/drivers` — List available drivers with vehicle data (*Officer Only*)
- `PUT /api/officer/complaints/:id/assign` — Assign a collection driver (*Officer Only*)
- `PUT /api/officer/complaints/:id/status` — Validate & update complaint status (*Officer Only*)
- `POST /api/officer/complaints/:id/notes` — Append internal officer note (*Officer Only*)

### Garbage Driver (`/api/driver`)
- `GET /api/driver/complaints` — Tasks assigned to the authenticated driver (*Driver Only*)
- `GET /api/driver/complaints/:id` — Assigned task details (*Driver Only*)
- `PUT /api/driver/complaints/:id/start` — Start collection (`ASSIGNED` -> `PICKUP_IN_PROGRESS`) (*Driver Only*)
- `PUT /api/driver/complaints/:id/complete` — Complete collection (*Requires after-cleaning photo*) (`PICKUP_IN_PROGRESS` -> `CLEANED`) (*Driver Only*)
- `PUT /api/driver/complaints/:id/evidence` — Update before/after photo URLs (*Driver Only*)
- `PUT /api/driver/location` — Manually submit current driver coordinates (*Driver Only*)
- `GET /api/driver/location` — Query current driver coordinates (*Driver Only*)
- `GET /api/driver/history` — Completed task history for this driver (*Driver Only*)

### Media Storage (`/api/media`)
- `POST /api/media/upload` — Upload image (`image/jpeg`, `image/png`, `image/webp`) or video (`video/mp4`, `video/quicktime`, `video/webm`) via multipart/form-data. Returns permanent HTTPS Cloudinary URL and public ID (*Authenticated*).

### Notifications (`/api/notifications`)
- `GET /api/notifications` — List notifications for authenticated user (*Scoped*)
- `PUT /api/notifications/:id/read` — Mark a notification as read (*Scoped*)
- `PUT /api/notifications/read-all` — Mark all notifications as read (*Scoped*)

---

## 8. Cloudinary Cloud Media Storage Integration (Step 8)

The backend integrates with Cloudinary for permanent, secure image and video storage:
- **Folders Hierarchy**:
  - `smart-garbage/complaints/`: Citizen garbage issue reports
  - `smart-garbage/before-cleaning/`: Driver pre-collection condition photos
  - `smart-garbage/after-cleaning/`: Driver post-collection verification photos
  - `smart-garbage/videos/`: Citizen video evidence attachments
- **Allowed MIME Types**:
  - Images: `image/jpeg`, `image/png`, `image/webp` (Max 10MB configurable via `MAX_IMAGE_SIZE_MB`)
  - Videos: `video/mp4`, `video/quicktime`, `video/webm` (Max 50MB configurable via `MAX_VIDEO_SIZE_MB`)
- **Safe Replacement**:
  - When replacing before/after photos, new uploads are persisted to MongoDB first; only upon successful database persistence is the old Cloudinary asset destroyed via `cloudinary.uploader.destroy()`.
- **Credential Security**:
  - `CLOUDINARY_API_SECRET` and API keys reside exclusively on the server in `.env`.
  - Android never receives or stores Cloudinary secrets.
- **Resilient Startup**:
  - If Cloudinary credentials are missing from `.env`, the server boots normally. Only endpoints requiring cloud uploads return a clear 503 error message directing the administrator to set credentials.

---

## 9. Android Emulator Networking

The Android emulator runs in an isolated virtual network.
- `localhost` inside the emulator refers to the emulator itself.
- To connect to the backend running on the development host machine, Android uses:
  ```
  http://10.0.2.2:5000/api/
  ```
- For a physical Android device on the same Wi-Fi, use your machine's LAN IP:
  ```
  http://192.168.1.X:5000/api/
  ```
- Configured centrally in:
  `android_app/app/src/main/java/com/garbage/management/utils/Constants.kt`

---

## 10. Running Tests

Execute the automated Jest + Supertest test suite:

```bash
npm test
```

Test coverage includes (39 automated unit tests across 3 suites):
- Health check verification
- JWT generation and Bearer token parsing
- Role-based authorization rejection (403)
- Unauthenticated access rejection (401)
- Citizen complaint submission and isolation
- Officer driver assignment and valid status progression
- Driver task isolation, collection start, and mandatory after-cleaning photo rule
- Cloudinary media upload authentication & validation
- Unsupported MIME rejection (400)
- Oversized file rejection (400)
- Cross-user complaint and evidence access rejection
- Safe asset replacement with old asset cleanup
- AI service automatic triggering on citizen complaints with image
- AI service graceful failure handling (`AI_UNAVAILABLE`) without deleting complaints
- AI service result storage (`aiAnalysis`) preserving official municipal status (`SUBMITTED`)
- Manual/re-analysis endpoint (`POST /api/complaints/:id/analyze-ai`)

---

## 11. AI Service Architecture & Workflow (Step 9)

The backend connects to an independent Python FastAPI microservice located in `ai_service/`:
- **Environment Variable**: `AI_SERVICE_URL=http://localhost:8000` (default)
- **Timeout**: `AI_SERVICE_TIMEOUT_MS=5000`
- **Workflow**:
  1. Citizen submits a garbage complaint with an attached image (hosted on Cloudinary).
  2. Backend immediately registers the complaint in MongoDB Atlas in `SUBMITTED` status.
  3. Backend calls `POST /api/predict/image` on the AI service with `{ imageUrl }`.
  4. The AI service performs SSRF-safe image download, Pillow validation, and computes assistive classification and severity estimation.
  5. The backend updates the complaint document with `aiAnalysis` and `aiAnalysisStatus = 'COMPLETED'`.
  6. If the AI service is offline, slow, or fails, the backend marks `aiAnalysisStatus = 'AI_UNAVAILABLE'` or `'FAILED'`, leaving the original complaint 100% functional.
  7. The AI result is strictly assistive (`modelStatus: 'DEVELOPMENT_BASELINE'`); it **never** alters official complaint status or auto-rejects complaints.

---

## 12. DEMO MODE & Synthetic Dataset

The backend includes a high-fidelity synthetic demo mode designed for rapid prototyping, UI demonstration, and offline evaluation across the Citizen, Municipal Officer, and Garbage Driver dashboards.

> [!NOTE]
> **Synthetic Data Disclaimer**: All records in the synthetic dataset are simulated strictly for demonstration and testing purposes. They do **not** represent real municipal records, citizen personal data, or physical garbage collection operations.

### Configuration (`USE_SYNTHETIC_DATA`)

You can toggle synthetic demo mode via the `.env` file:

```env
# Enable Synthetic Demo Dataset for Dashboards & Read APIs
USE_SYNTHETIC_DATA=true
```

- **`USE_SYNTHETIC_DATA=true`**: Dashboard read endpoints (`/api/dashboard/*`) and associated complaint/driver listings return structured, mathematically reconciled data from `backend/src/data/synthetic/synthetic-dashboard-data.json`. The backend functions seamlessly even without an active MongoDB connection.
- **`USE_SYNTHETIC_DATA=false`** (Default): Preserves existing production behavior. All dashboard queries, aggregations, and reads hit MongoDB directly.

> [!IMPORTANT]
> **MongoDB Mode Remains 100% Intact**: All write operations (filing complaints, assigning drivers, status updates, officer notes, evidence upload, collection completion) continue to use the standard MongoDB workflows regardless of demo mode.

### Dashboard Endpoints

The following REST endpoints serve role-specific dashboard metrics and activity:

| Method | Endpoint | Authorized Roles | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/dashboard/citizen` | `CITIZEN` | Returns personal complaint metrics, emergency counts, active/pending lists, and recent notifications for the authenticated citizen. |
| `GET` | `/api/dashboard/officer` | `MUNICIPAL_OFFICER` | Returns city-wide complaint status breakdowns, emergency summaries, available driver fleet stats, AI analysis highlights, and recent municipal activity. |
| `GET` | `/api/dashboard/driver` | `GARBAGE_DRIVER` | Returns assigned/in-progress/completed task counts, driver profile, current assigned tasks, and historical collections for the authenticated driver. |

All endpoints enforce existing JWT authentication and strict Role-Based Access Control (RBAC).

### Seeding Synthetic Data into MongoDB

If you wish to populate your MongoDB Atlas or local database instance with the complete synthetic dataset (21 users, 8 drivers, 120 complaints, 37 notifications):

```bash
# Seed synthetic dataset into MongoDB
npm run seed:synthetic

# Or validate dataset integrity anytime
npm run validate:synthetic
```

### Synthetic Demo Accounts

The synthetic dataset provides pre-configured credentials for testing each role:

| Role | Email | Password | Identifier / Details |
| :--- | :--- | :--- | :--- |
| **Citizen** | `aarav.sharma@example.com` | `password123` | ID: `65a000000000000000000001` (Green Park, Sector 4) |
| **Citizen** | `priya.singh@example.com` | `password123` | ID: `65a000000000000000000002` (Civil Lines) |
| **Municipal Officer** | `vikram.patel@example.com` | `password123` | ID: `65a000000000000000000011` (Emp: `MC-OFF-2026`, Ward 12) |
| **Municipal Officer** | `anjali.mehta@example.com` | `password123` | ID: `65a000000000000000000012` (Emp: `MC-OFF-2027`, Ward 14) |
| **Garbage Driver** | `ramesh.kumar@example.com` | `password123` | ID: `65a000000000000000000021` (Driver: `DRV-882`, Truck: `DL-01-GB-4040`) |
| **Garbage Driver** | `suresh.yadav@example.com` | `password123` | ID: `65a000000000000000000022` (Driver: `DRV-883`, Truck: `DL-01-GB-4041`) |