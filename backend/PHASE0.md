# Phase 0: reliable operation before smart-bin ingestion

No hardware endpoint, firmware, MQTT broker, new AI model, or routing engine is included.

## Operation and configuration

Normal mode never changes to synthetic mode when MongoDB disconnects. Database-dependent APIs return HTTP 503. `/api/health` remains readable. The Android app surfaces remote failures rather than claiming local complaint writes or demo dashboard results are server successes. Password reset reports that it is unconfigured instead of simulating delivery. Notification and complaint caches start empty; local fixture constructors require `demoMode = true`. The application does not inject mock authentication.

Example configuration (replace placeholders; never commit real values):

```dotenv
NODE_ENV=development
USE_SYNTHETIC_DATA=false
MONGODB_URI=mongodb://<host>:<port>/<database>?replicaSet=<replica-set>
JWT_SECRET=<long-random-server-only-secret>
DRIVER_LOCATION_MAX_AGE_MS=120000
DRIVER_SERVICE_RADIUS_KM=20
CLOUDINARY_CLOUD_NAME=<cloud-name>
CLOUDINARY_API_KEY=<server-only-key>
CLOUDINARY_API_SECRET=<server-only-secret>
```

`JWT_SECRET` is required at server startup. There is no normal-mode default signing secret. The age and radius must be positive numbers. Explicit demonstration mode is `USE_SYNTHETIC_DATA=true`; it allows fixture authentication and synthetic operations. Do not use it for real pickups. Missing Cloudinary configuration always returns an error, including in demo mode; sample URLs are never substituted for uploaded evidence.

Tests skip `.env` loading, overwrite service configuration with test-only values, mock external calls, and never start `server.js`.

## Assignment and transactions

Automatic and officer assignments use `reserveAssignment`. Eligible drivers must be AVAILABLE, have DEVICE-origin coordinates and a recent capture timestamp, and be within the configured straight-line radius. Legacy/default locations lack the origin marker and are excluded until a fresh authenticated phone upload. Uploads require `capturedAt` in epoch milliseconds; the Android app sends the original GPS capture time and rejects old last-known fixes.

A driver may have one ASSIGNED or PICKUP_IN_PROGRESS task. Repeating the same active assignment is a no-op. Assigning a different driver or assigning a terminal/in-progress task through an inappropriate path returns a conflict. A generic status change cannot bypass reservation by setting ASSIGNED.

MongoDB assignment and lifecycle changes use sessions and transactions. Driver reservation and task save commit together. Errors abort; there is no fallback to separate writes. Completion/rejection releases the driver only after checking for other active tasks in the same transaction; OFFLINE is preserved. Driver availability changes are also transactional and refuse to override active work.

Deployment requires a MongoDB replica set (including a local single-node replica set for development) or a sharded cluster supporting transactions. Standalone MongoDB returns a clear 503 for these operations. Pre-create application collections for older deployments. Do not deploy until the real transaction tests pass on the intended MongoDB version/topology.

Citizen report creation remains separate from dispatch: if assignment infrastructure fails, the saved report returns 201 with `dispatchResult.reason=DISPATCH_UNAVAILABLE` and a warning. Officers can retry using the assignment endpoint once the issue is resolved. Notification delivery occurs after assignment commit; a notification failure does not undo or misreport a committed assignment. Durable notification retry/outbox delivery remains future work.

## Identity inspection and controlled staff provisioning

The checked-in synthetic fixture has 8 driver users and 8 driver records; all 8 users have matching driver IDs. This is not a live-database audit. No live records were changed and no migration is included.

Run the read-only `node src/scripts/auditDriverIdentity.js` only after explicitly selecting `IDENTITY_AUDIT_URI=<read-only-target-uri>`. It disables automatic collection/index creation, reads identifiers only, and reports missing/multiple matches and possible employee-ID matches. It makes no repairs. Inspect its output before designing any migration; employee matching alone must not authorize a merge. Use a read-only database account. Do not seed a real environment to repair identities.

Public `/api/auth/register` accepts CITIZEN only and strips staff identity fields. Staff provisioning is an operator-only CLI, not a public HTTP route:

1. Explicitly set `STAFF_PROVISIONING_URI=<approved-target-uri>` in the operator environment.
2. Supply JSON through stdin to `node src/scripts/provisionStaff.js` with name, email, password, role, employeeId, and optional ward. Drivers also need phone and vehicleNumber.
3. It creates new records only, in a transaction. Driver IDs are generated once and shared by User and Driver. Existing records are never silently adopted or overwritten.

Protect access to this command and database credentials. Do not put passwords in shell command arguments/history; use a protected input stream. Running provisioning against any live database requires the project owner's explicit authorization.

Existing mismatched driver accounts now receive a provisioning error rather than using their user ID as a different driver identity. Fresh provisioning does not migrate them.

## Verification

`npm test -- --no-cache` runs unit/controller tests and explicit synthetic-fixture tests. Database sessions in those tests are mocked. They establish validation and call behavior, not multi-document atomicity.

`tests/phase0.mongo.test.js` contains real concurrency and rollback tests. It is skipped by default. To run, supply `PHASE0_TEST_MONGODB_URI=mongodb://127.0.0.1:<port>/phase0_test?replicaSet=<replica-set>` and run that test file. Only an explicit loopback `phase0_test` target is accepted. It creates test-prefixed records and cleans up only its own records; never point it at production or use `MONGODB_URI` as a substitute.

Android: run `gradlew.bat :app:testDebugUnitTest`. Existing workflow tests explicitly opt into fixture data; Phase0DefaultsTest verifies empty normal-mode caches.

## Next-phase readiness

The existing citizen workflow and driver pickup lifecycle remain in place. Shared reservation and error handling are available for a future sensor-task creator. Before a hardware pilot: run the real MongoDB tests, inspect actual driver identities, configure genuine media storage, and perform a phone-to-backend smoke test.

Still intentionally absent: Bin/Device registration, device credentials, sensor readings, sustained-threshold evaluation, duplicate active task prevention per bin, sensor-empty confirmation, automatic dispatch retry/acknowledgment deadlines, and background push. These belong to subsequent phases and must not be inferred from the Phase 0 tests.

## Local verification recorded

The full backend suite passed 127 tests, with the 2 real MongoDB tests skipped because no isolated local replica set was supplied. Focused assignment/authentication suites were rerun after final edits. Android compiled and its 34 JVM unit tests passed; phone/emulator UI and real service operation are not established by these tests. JavaScript syntax checks and git whitespace checks passed. Docker engine was unavailable, so no MongoDB rollback/concurrency result is claimed. No live database was read or modified.
