# Phase 1 — Arduino Uno USB smart bin

Implemented path: HC-SR04 → Uno → USB serial → Python bridge → Express/MongoDB → existing Kotlin app.
The citizen workflow and Phase 0 authentication/reservation protections remain in place.

## Verification status and blockers

Updated local verification on **2026-10-30**: Docker is available (engine 29.9.2), and a new isolated MongoDB 7.0.45 replica set is running as writable primary `rs0`. **All 155 backend tests pass with all five real MongoDB cases enabled; none are skipped.** The Android debug APK builds and all 38 JVM tests pass. The Uno sketch compiled and uploaded to the physically confirmed COM4 board, and its actual JSON was read at 9600 baud. The authenticated bridge has delivered real empty-bin readings, with a recorded empty confirmation and no pickup task at that stage.

The full real-bin pilot is **still not verified**. Sustained full readings, driver assignment/display, physical pickup, photo-backed completion, and officer verification must each have their own evidence. External media storage is intentionally disabled in this local pilot to avoid inherited live Cloudinary credentials; photo completion remains blocked until approved test media storage is explicitly configured. No Atlas or pre-existing database was modified.

Live verification exposed a serial framing bug: a timeout could split a JSON line, and the bridge discarded the fragments. It now accumulates bounded partial frames until newline. All **8 bridge tests pass**, including a new split-frame regression. Android request/response body logging is disabled so passwords and tokens cannot be logged.

## Current prepared pilot — use these commands for this session

- Compose project: `smart-bin-verify-20261029-nz45f6`; container/volume were newly created and do not reuse existing Docker data.
- MongoDB is published only at `127.0.0.1:27018`. Automated tests use `phase0_test` and `phase1_test`; the fresh manual pilot uses `phase1_pilot`.
- Backend is listening on port 5000. Local runtime files and Windows-encrypted credentials/device token are in ignored `.local-phase1/`. Do not print or commit them.
- Bin `UNO-BIN-001`, device `UNO-USB-001`: **18.4624311, 73.8636227**, O Building, State Bank CHS, near Chaitraban, Upper Indira Nagar, Bibwewadi, Pune, Maharashtra 411037. Empty calibration remains **15.9 cm**.
- COM4 was confirmed by the user's unplug/replug test. Do not reopen Serial Monitor while the bridge owns it.
- Phone SM_E236B is USB-authorized. The installed pilot APK uses **127.0.0.1 via `adb reverse`**, not the phone's Wi-Fi route. The user confirmed the phone's health page succeeds.

```powershell
cd D:\EDI_SEM5\garbage_management
docker compose -p smart-bin-verify-20261029-nz45f6 -f backend/docker-compose.phase1.yml ps
adb reverse tcp:5000 tcp:5000
adb install -r .\android_app\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.garbage.management/.MainActivity
```

The USB tunnel maps phone `127.0.0.1:5000` to laptop port 5000. It must be re-established after disconnecting/reconnecting the phone. No Windows firewall rule was changed. Wi-Fi is an alternative: the laptop's observed LAN address is `192.168.1.5`, but Windows currently classifies that network as Public, so phone-to-LAN access is not verified. The MongoDB port stays loopback-only in either case.

Rebuild explicitly for USB (quote the Gradle property in PowerShell):

```powershell
cd D:\EDI_SEM5\garbage_management\android_app
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio2\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest '-PpilotBackendHost=127.0.0.1' --offline --no-daemon
```

For a Wi-Fi APK, use `'-PpilotBackendHost=192.168.1.5'` instead, verify the current address first, and confirm the health page from the phone. Emulator builds continue using `10.0.2.2`.

Local helper commands, from the repository root:

```powershell
# Only if the backend is stopped; refuses to replace an existing listener.
.\scripts\Local-Pilot.ps1 -Action Start
# Token is loaded from Windows-encrypted storage, never shown.
# Run only after stopping the existing bridge, with Serial Monitor closed.
.\scripts\Local-Pilot.ps1 -Action StopBridge
.\scripts\Local-Pilot.ps1 -Action Bridge
# Hidden prompts; changes only the two accounts in our fresh local pilot DB.
.\scripts\Local-Pilot.ps1 -Action Passwords
```

Login emails are `officer@phase1.local` and `driver@phase1.local`; use the locally chosen passwords, never send them in chat. They were set by the user during verification. `Prepare` and `Provision` have already run; do not repeat them or rotate the token unnecessarily. `Provision-TestBin.ps1` is the existing provisioning script, now also able to accept a PSCredential and save the returned token encrypted without logging it. The physical test's remaining steps are in the acceptance checklist below.

Read-only live distance feedback, without opening COM4 a second time:

```powershell
cd D:\EDI_SEM5\garbage_management
.\scripts\Local-Pilot.ps1 -Action Watch
```

This monitor reads only the isolated pilot database, prints distance/validity/filter streak/task count, and does not change data or load `.env`. Ctrl+C closes the display only; the background bridge keeps running.

### Physical acceptance evidence so far

- The phone health page succeeded through the USB tunnel (user confirmed).
- The rebuilt APK installed successfully, and Driver Home loaded (user confirmed).
- Real empty-bin telemetry reached the backend; filtered fill was approximately 2.1%, sensor-empty confirmation was recorded, and no task existed at that stage.
- Early full-target attempts produced 3.74 cm (76.5%), roughly 8 cm, and NO_ECHO, correctly creating no task. After physical adjustment, the user observed 2.44 cm/84.7%, and the backend independently recorded sustained valid readings around 2.76 cm/82.6%. The threshold was not lowered and no artificial task was inserted.
- **Sustained full and deduplication verified:** exactly one open sensor task, `BIN-3270a122-21c0-4821-8202-cb2a721aa035`, remained open through continued FULL readings. Database state was SUBMITTED / PENDING_DRIVER / NO_AVAILABLE_DRIVER, with retry attempts and a future retry time. Assigned driver ID/name/time were null. The driver was independently verified OFFLINE with no recorded location; no offline driver was assigned.
- The local driver was explicitly set OFFLINE through its authenticated API and the response confirmed OFFLINE. Do not assume a stale UI label proves database availability.
- **Real location and automatic assignment verified:** after the installed location-capture fix, the backend stored a DEVICE-origin location, measured 34 seconds old and approximately 2 metres from the registered bin. The existing worker assigned the same task to the matching driver, set that driver BUSY, and persisted ASSIGNED retry state with no next retry. Exactly one active sensor task remained. No manual officer assignment was used.
- **Driver task display verified by the user:** Driver Tasks → Refresh showed the same Assigned smart-bin task, and its details showed the confirmed bin address/location and pickup controls. No pickup was started during this checkpoint.
- Physical pickup completion, post-pickup emptying, and officer verification remain unverified. Photo completion requires approved test media storage, which is intentionally disabled in this local pilot. Do not bypass this by inserting a sample evidence URL or manually changing task status in the database.

The initial phone capture returned "Unable to get GPS location" despite Location being enabled and precise permission granted. The helper previously favored GPS even when a fresh network-provider fix could exist. It now checks both real providers/caches, rejects stale/future captures, displays capture progress, and times out after 15 seconds. New `LocationFreshnessTest` cases preserve the freshness boundary. The updated APK is installed and its successful real device-location upload was independently verified in the database. No coordinates were fabricated. Fresh location/AVAILABLE events now advance pending sensor retry deadlines so the worker can re-evaluate immediately, retaining all reservation rules (`dispatchWake.test.js`).

Verification helpers added for this local session: `scripts/Local-Pilot.ps1`, `backend/src/scripts/setLocalPilotPasswords.js`, and `backend/src/scripts/watchLocalBin.js`. The helpers have fixed loopback pilot targets; password handling uses hidden prompts and Windows-encrypted storage. The Android host can now be selected with the quoted `pilotBackendHost` Gradle property, and HTTP body logging was removed.

If you need to upload the same sketch again, first stop this pilot's bridge and close Serial Monitor/Plotter:

```powershell
$cli = 'C:\Users\SAMPADA\AppData\Local\Programs\Arduino IDE\resources\app\lib\backend\resources\arduino-cli.exe'
& $cli compile --fqbn arduino:avr:uno --build-path .\.local-phase1\uno-build .\hardware\uno_smart_bin
& $cli upload --fqbn arduino:avr:uno --port COM4 --input-dir .\.local-phase1\uno-build .\hardware\uno_smart_bin
```

Actual compile result: 3982 bytes flash and 208 bytes static RAM. Verify JSON in Serial Monitor at 9600, close Serial Monitor, then restart the bridge. Reconfirm the port if the hardware/cable changes. Earlier unavailable-Docker results were historical blockers and are superseded by the real passing tests above.

## File map

New files:

- `hardware/uno_smart_bin/uno_smart_bin.ino`
- `bridge/smart_bin_bridge.py`, `bridge/requirements.txt`, `bridge/test_bridge.py`
- `scripts/Provision-TestBin.ps1`
- `backend/docker-compose.phase1.yml`
- `backend/src/models/Bin.js`, `Device.js`, `SensorReading.js`
- `backend/src/services/binEvaluationService.js`, `sensorService.js`
- `backend/src/middleware/deviceAuthMiddleware.js`
- `backend/src/routes/binRoutes.js`, `sensorRoutes.js`
- `backend/src/workers/sensorDispatchWorker.js`
- `backend/tests/sensor.test.js`, `sensorWorker.test.js`, `sensor.mongo.test.js`
- `android_app/app/src/main/java/com/garbage/management/domain/model/SmartBinStatus.kt`
- `android_app/app/src/test/java/com/garbage/management/SmartBinDtoTest.kt`
- This runbook, `PHASE1.md`.

Extended existing files (including preserved Phase 0 work):

- `.gitignore` (ignore the dedicated Python virtual environment)
- `backend/src/app.js`, `server.js`, `models/Complaint.js`
- `backend/src/services/dispatchService.js`, `taskTransitionService.js`
- `backend/src/controllers/driverController.js`, `officerController.js`
- Under `android_app/app/src/main/java/com/garbage/management/`: `data/model/ComplaintDto.kt`, `data/remote/ApiService.kt`, `data/remote/RemoteDashboardDataSource.kt`, `domain/model/GarbageComplaint.kt`, `domain/model/DashboardData.kt`, `presentation/viewmodel/OfficerViewModel.kt`, `presentation/screens/officer/OfficerHomeScreen.kt`, `presentation/screens/driver/DriverTasksScreen.kt`, `presentation/screens/driver/DriverTaskDetailsScreen.kt`.

## Hardware and Arduino upload

1. Use your existing Uno wiring: VCC → **5V**, GND → **GND**, TRIG → **D9**, ECHO → **D10**. This is an Uno 5 V circuit; no ESP32 is involved.
2. Connect the Uno by a data-capable USB cable. Open `hardware/uno_smart_bin/uno_smart_bin.ino` in Arduino IDE.
3. Select **Tools → Board → Arduino AVR Boards → Arduino Uno**. Install Arduino AVR Boards through Boards Manager if missing.
4. Select the Uno's COM port under **Tools → Port**. Unplug/replug the board to identify the disappearing/reappearing port. The local inventory during implementation showed **COM4: USB Serial Port**; verify it belongs to the Uno. COM5/COM6 were Bluetooth ports, not evidence of an Uno.
5. Click **Verify**, then **Upload**. Close any program using that COM port first.
6. Temporarily open Serial Monitor at **9600 baud**. Expect one JSON line every 2 seconds:

```json
{"sequence":1,"uptimeMs":2000,"distanceCm":15.9,"valid":true,"reason":"OK"}
{"sequence":2,"uptimeMs":4000,"distanceCm":null,"valid":false,"reason":"NO_ECHO"}
```

7. **Close Serial Monitor before starting the bridge.** Only one process can own the serial port. Opening the port normally resets the Uno; sequence/uptime reset is expected and starts a new backend session.

The echo timeout is bounded at 25 ms. Firmware rejects no echo and its physical 2–400 cm range violations. The backend applies the much narrower registered-bin calibration. No echo never becomes `0 cm`, 100% full, or an empty bin. Nothing confidential is stored on the Uno.

## Isolated local MongoDB and real integration tests

Start Docker Desktop's Linux engine first. Then, in PowerShell:

```powershell
Set-Location D:\EDI_SEM5\garbage_management\backend
docker compose -f docker-compose.phase1.yml up -d
docker compose -f docker-compose.phase1.yml exec mongo mongosh --quiet --eval "rs.initiate({_id:'rs0',members:[{_id:0,host:'localhost:27017'}]})"
docker compose -f docker-compose.phase1.yml exec mongo mongosh --quiet --eval "db.hello().isWritablePrimary"
```

Wait until the last command returns `true`. Initialize the replica set only once; an already-initialized response on subsequent runs is expected. The compose file publishes **loopback port 27018 only** and uses a dedicated local volume. `directConnection=true` lets Windows clients use this published port without following the container's internal address.

```powershell
$env:PHASE0_TEST_MONGODB_URI = 'mongodb://127.0.0.1:27018/phase0_test?replicaSet=rs0&directConnection=true'
$env:PHASE1_TEST_MONGODB_URI = 'mongodb://127.0.0.1:27018/phase1_test?replicaSet=rs0&directConnection=true'
npm test -- --no-cache
```

Both integration suites accept only their explicit loopback test database names. They never use the application's `MONGODB_URI`. They create test-prefixed records and clean up their own records. The sensor suite tests the real unique index, concurrent duplicate ingestion/task creation, and transaction rollback after an injected task-save failure. The Phase 0 suite tests real driver reservation concurrency and rollback. All five must pass before calling the database-backed pilot verified. A standalone MongoDB server is insufficient.

Use a separate **phase1_pilot** database for the manual pilot; the automated tests do not clean it. Do not seed or migrate a live database.

## Local backend and test staff

Use a dedicated PowerShell terminal for the backend:

```powershell
Set-Location D:\EDI_SEM5\garbage_management\backend
$env:MONGODB_URI = 'mongodb://127.0.0.1:27018/phase1_pilot?replicaSet=rs0&directConnection=true'
$env:USE_SYNTHETIC_DATA = 'false'
$env:JWT_SECRET = [Guid]::NewGuid().ToString('N') + [Guid]::NewGuid().ToString('N')
$env:DRIVER_LOCATION_MAX_AGE_MS = '120000'
$env:DRIVER_SERVICE_RADIUS_KM = '20'
```

Use approved **test** Cloudinary storage for driver photo completion; set `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET` in this backend process without displaying or committing them. Missing media configuration must remain an error. Sensor ingestion itself does not upload images or call AI.

Provision local staff using the Phase 0 CLI, not public signup. In this same terminal, before starting the server:

```powershell
$env:STAFF_PROVISIONING_URI = $env:MONGODB_URI
foreach ($role in @('MUNICIPAL_OFFICER', 'GARBAGE_DRIVER')) {
    $email = Read-Host "Local test email for $role"
    $secret = Read-Host 'Password (at least 12 characters)' -AsSecureString
    $staff = @{ name = "Pilot $role"; email = $email; role = $role;
        employeeId = "PILOT-$role";
        password = [System.Net.NetworkCredential]::new('', $secret).Password }
    if ($role -eq 'GARBAGE_DRIVER') {
        $staff.phone = Read-Host 'Test driver phone'
        $staff.vehicleNumber = Read-Host 'Test vehicle identifier'
    }
    $staff | ConvertTo-Json -Compress | node src/scripts/provisionStaff.js
    $staff = $null
    $secret.Dispose()
}
npm start
```

These commands create new local staff records only. Re-running with the same email/employee ID fails rather than overwriting existing accounts. Retain the generated driver ID from the CLI output. Do not run this against live data. Keep this backend terminal running; it also runs the durable sensor dispatch worker.

## Install and launch the Python USB bridge

In a **second PowerShell terminal**:

```powershell
Set-Location D:\EDI_SEM5\garbage_management
python -m venv .venv-bin
.\.venv-bin\Scripts\python.exe -m pip install -r .\bridge\requirements.txt
.\.venv-bin\Scripts\python.exe .\bridge\smart_bin_bridge.py --list-ports
Get-CimInstance Win32_SerialPort | Select-Object DeviceID, Name
```

Choose the verified Uno port. No environment activation or execution-policy change is required. The only third-party bridge dependency is pyserial; HTTP uses Python's standard library.

Provision exactly one test bin/device using your **actual fixed bin coordinates**, obtained from a map or a phone at the bin. No GPS hardware is needed on the bin. This helper prompts for the local officer credentials, never prints the returned token, and places it in the current PowerShell process environment:

```powershell
$lat = [double](Read-Host 'Actual fixed bin latitude')
$lon = [double](Read-Host 'Actual fixed bin longitude')
$site = Read-Host 'Bin address or landmark'
.\scripts\Provision-TestBin.ps1 -Latitude $lat -Longitude $lon -LocationText $site
$env:BIN_COM_PORT = 'COM4' # replace with the verified Uno port
$env:BIN_BAUD = '9600'
# BIN_BACKEND_URL and BIN_DEVICE_TOKEN were set by provisioning.
# Default backend origin is http://127.0.0.1:5000 (no /api suffix).
.\.venv-bin\Scripts\python.exe .\bridge\smart_bin_bridge.py
```

If script execution is restricted, use PowerShell's permitted signed-script mechanism or run the script contents in your trusted terminal; do not change machine-wide policy. Do not print `$env:BIN_DEVICE_TOKEN`, paste it into chat, commit it, or pass it as a command-line argument.

Provisioning is `POST /api/bins`, protected by real officer JWT/role checks. It creates the required collections/indexes and bin/device in a transaction. Device tokens are returned once and stored only as hashes. A device token authenticates `/api/device/session` and `/api/device/readings`; it cannot register bins or act as a user. The bin is always resolved from the authenticated device. Payload `binId` is rejected.

On a later terminal session, obtain a rotated token via officer-protected `POST /api/bins/devices/UNO-USB-001/rotate` and capture `data.deviceToken` in process memory, or load the original token from an approved secret store. Rotation invalidates the old token/session. Do not try to provision duplicate IDs as a restart mechanism. HTTPS is required by the bridge for non-loopback backend origins; HTTP is allowed only for the laptop-local prototype.

### Bridge failure behavior

- Serial reads run separately from HTTP, with a bounded 128-reading queue that discards the oldest on overflow.
- Each serial connection gets a server-issued UUID session. Sequence and Uno uptime must advance. Reset/wrap/disconnect establishes a new session and resets filter confirmation, without rearming a full bin.
- The bridge flushes the serial buffer after session creation. Capture times are derived from Uno uptime and a server-time anchor, rather than assigning a fresh timestamp when old queued data is sent.
- Queue delay above 3 seconds marks a reading buffered before its first send. Session, sequence, timestamps, validity, and buffered flag remain identical through HTTP retries.
- Each request has a 4-second timeout and at most 3 attempts with bounded backoff. Unauthorized credentials stop the bridge. Malformed serial lines are discarded without logging raw payloads or credentials.
- A lost HTTP response can be retried safely: the backend deduplicates `(deviceId, sessionId, sequence)` and rejects reused IDs with different content. Old sessions, out-of-order, buffered, or expired readings are retained as ineligible history and cannot confirm fill/empty state.
- There is **no disk spool**. Process termination, queue overflow, or exhausted retries may lose readings; they never become a false empty measurement. Health becomes stale/offline, and fresh confirmation must resume after recovery.

## Calibration, filtering, health, and task semantics

Default calibration is your measured **15.9 cm empty distance**. A valid reading must be between **2.0 and 15.9 cm**, inclusive. Larger distances, no echo, negative/zero distances, and blind-zone measurements are invalid, not clamped into a valid fill.

`fillPercent = 100 × (15.9 − distanceCm) / 15.9`

This is approximate height along the sensor beam, not exact occupied volume. Uneven waste or lid movement can produce misleading echoes; mount the sensor consistently and test with a flat target first.

- Full: **80%**, corresponding to **3.18 cm or less**, while remaining above the 2 cm sensor minimum.
- Empty: **20% or less**, corresponding to **12.72 cm or more**.
- Filter: median of the last 3 consecutive valid, fresh samples. Both the current raw reading and filtered fill must satisfy the relevant threshold.
- Confirmation: at least 3 qualifying readings spanning at least **10 seconds**, after filter warmup. At the 2-second sample interval, allow roughly **16–20 seconds** of steady material position; invalid readings restart confirmation.
- A gap above 6 seconds resets the filter/streak. A reading older than 10 seconds is ineligible.
- Health: current invalid data is FAULT; measurements older than 15 seconds are STALE; contact absent for 60 seconds is OFFLINE. The last valid fill is retained and labeled accordingly, never changed to empty by a fault.

Officer-protected `PATCH /api/bins/UNO-BIN-001/config` accepts numeric calibration/threshold settings. Defaults and validation are in `binEvaluationService.js`. Configurable fields: `emptyDistanceCm`, `minDistanceCm`, `fullThreshold`, `emptyThreshold`, `filterSamples` (odd), `confirmReadings`, `confirmDurationMs`, `maxGapMs`, `maxReadingAgeMs`, `staleAfterMs`, `offlineAfterMs`, and `retryDelayMs`. Unreachable full thresholds inside the sensor blind zone are rejected. Changing configuration resets confirmation but does not rearm a full bin.

A sustained full reading creates a source=SENSOR pickup with no invented citizen identity. A partial unique database index enforces one open sensor task per bin; reading/task creation and the device/bin state change share a transaction. Ingestion fails closed if required indexes are absent.

The existing transactional dispatcher reserves one driver at a time. Pending sensor tasks persist retry state/attempt count/reason/deadline and use crash-recoverable worker leases. Retries start at 15 seconds and back off up to 5 minutes. Manual officer assignment uses the same reservation rules. Missing/stale driver GPS, busy/offline drivers, and drivers beyond the configured service radius are excluded.

Confirmed emptying rearms the bin, but **does not** set CLEANED or VERIFIED, cancel pending work, or release a driver. The linked open task stores its first sensor-empty confirmation separately. Driver photo completion remains CLEANED; officer review remains VERIFIED. An open task continues blocking a second task through CLEANED until VERIFIED/REJECTED. After closure, another task is possible only following confirmed emptying and a new sustained full condition. If an open task's bin empties before pickup, review that discrepancy rather than assuming cleanup was verified.

## Prepare Android and a real driver

1. Rebuild/install `android_app` with the matching backend changes. Configure `utils/Constants.kt` for the laptop's reachable LAN address if needed; do not put device/admin credentials into the app. Phone and laptop must reach the same backend.
2. Log in using the **local test driver account** created above. Its User and Driver records share the generated driver ID.
3. Enable Android location and grant the app's location permission. On Driver Home, press **Update Location** while physically at/near the bin. The backend requires the original recent capture timestamp and marks this as DEVICE-origin GPS; do not fake coordinates or edit database locations.
4. Set driver availability to AVAILABLE in the existing driver controls. If another task is active, finish/review it first; the backend refuses to override capacity.
5. Repeat Update Location if more than 120 seconds elapse before assignment. This phase has no continuous location tracking.
6. Officer Home has **Smart bins → Refresh**, showing last valid fill, measured/contact time, health, linked task, pending/retry reason, and assigned driver. Driver Tasks has **Refresh**, with sensor tasks labeled by bin ID. Existing map and photo pickup actions are reused.

**Foreground/manual refresh only. No background push is implemented or claimed.** Leaving the app open does not make it a server subscription; press Refresh or reopen the task list.

## Live acceptance checklist (only after real MongoDB tests pass)

1. Start the local replica set/backend and USB bridge. Confirm normal mode and real test media configuration. Refresh Officer Home: the registered bin should appear.
2. Leave the bin empty at ~15.9 cm for at least 20 seconds. Expect ONLINE health and ~0% fill. No pickup should be created.
3. Briefly interrupt an echo. Expect FAULT/invalid, retained last valid fill, and no empty/full confirmation. Restore the sensor and let valid readings recover.
4. For a no-driver test, leave the driver OFFLINE. Place a flat target ~3.0 cm below the sensor (about 81% full), above the 2 cm blind zone. Hold it steady for at least 20 seconds.
5. Refresh the officer view: exactly one linked task, SUBMITTED/PENDING_DRIVER, with NO_AVAILABLE_DRIVER and a retry deadline. Keep the target in place another 30 seconds; the task ID must not change and there must be no second open task.
6. On the real driver phone, set AVAILABLE and press Update Location. Wait for the displayed retry (initially 15 seconds, potentially longer after repeated failures). Refresh Driver Tasks: the same pickup should be ASSIGNED to that driver. Officer view should show the assignment. No citizen report or manual assignment is required.
7. Open the task, confirm the fixed location/map, and use the existing **start pickup** action. Physically remove the material and record the required after-cleaning photo using real test media storage. Complete pickup: status becomes CLEANED.
8. Hold the empty distance around 15.9 cm for at least 20 seconds. Officer view must show sensor-empty confirmation separately. Invalid/no-echo readings must not produce that confirmation. Sensor empty alone must never mark VERIFIED.
9. Officer reviews evidence and marks VERIFIED. Add material to ~3 cm again and hold steady: exactly one **new** task may be created for the next empty/full cycle. If the old task is still open, no new task is allowed.
10. Stop the bridge: refresh after 15 seconds for STALE and after 60 seconds for OFFLINE. Fill remains the last valid value. Restarting should use a new serial session; old queued readings must not count toward a fresh threshold.

Until these steps succeed with the real hardware, phone, and transaction-capable local database, treat this as implemented code with local automated checks—not a verified real-bin pilot.
