param(
    [Parameter(Mandatory=$true)][ValidateSet('Prepare','Start','Restart','ConfigureMedia','Provision','Bridge','StopBridge','Passwords','Watch')][string]$Action
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$runtime = Join-Path $root '.local-phase1'
$credentialPath = Join-Path $runtime 'credentials.clixml'
$tokenPath = Join-Path $runtime 'device-token.clixml'
$mediaPath = Join-Path $runtime 'test-media.clixml'
$localUri = 'mongodb://127.0.0.1:27018/phase1_pilot?replicaSet=rs0&directConnection=true'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null

function Set-LocalEnvironment {
    $env:NODE_ENV = 'development'
    $env:MONGODB_URI = $localUri
    $env:STAFF_PROVISIONING_URI = $localUri
    $env:USE_SYNTHETIC_DATA = 'false'
    $env:PORT = '5000'
    $env:DRIVER_LOCATION_MAX_AGE_MS = '120000'
    $env:DRIVER_SERVICE_RADIUS_KM = '20'
    # Do not inherit production media/AI configuration from backend/.env.
    $env:CLOUDINARY_CLOUD_NAME = ''
    $env:CLOUDINARY_API_KEY = ''
    $env:CLOUDINARY_API_SECRET = ''
    $env:CLOUDINARY_URL = ''
    if (Test-Path -LiteralPath $mediaPath) {
        $media = Import-Clixml -LiteralPath $mediaPath
        $env:CLOUDINARY_CLOUD_NAME = Plain $media.CloudName
        $env:CLOUDINARY_API_KEY = Plain $media.ApiKey
        $env:CLOUDINARY_API_SECRET = Plain $media.ApiSecret
    }
    $env:AI_SERVICE_URL = 'http://127.0.0.1:1'
}
function New-LocalSecret {
    $bytes = New-Object byte[] 36
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    ConvertTo-SecureString ([Convert]::ToBase64String($bytes)) -AsPlainText -Force
}
function Plain($secret) { [System.Net.NetworkCredential]::new('', $secret).Password }
function Read-LocalCredentials {
    if (-not (Test-Path -LiteralPath $credentialPath)) { throw 'Run Prepare first.' }
    Import-Clixml -LiteralPath $credentialPath
}

Set-LocalEnvironment
Push-Location (Join-Path $root 'backend')
try {
    if ($Action -eq 'Prepare') {
        if (Test-Path -LiteralPath $credentialPath) { throw 'Local credentials already exist; use Start or Passwords.' }
        $credentials = @{
            Officer = [PSCredential]::new('officer@phase1.local', (New-LocalSecret))
            Driver = [PSCredential]::new('driver@phase1.local', (New-LocalSecret))
            Jwt = New-LocalSecret
        }
        # This helper has a fixed loopback pilot target; it cannot accept an Atlas URI.
        $init = 'const m=require("mongoose"); (async()=>{await m.connect(process.env.MONGODB_URI,{autoIndex:false});for(const x of [require("./src/models/User"),require("./src/models/Driver")]){await x.createCollection();await x.createIndexes();}await m.disconnect();})().catch(()=>{console.error("Local schema initialization failed");process.exitCode=1;});'
        & node -e $init
        if ($LASTEXITCODE -ne 0) { throw 'Local pilot schema initialization failed.' }
        foreach ($key in @('Officer','Driver')) {
            $role = if ($key -eq 'Officer') { 'MUNICIPAL_OFFICER' } else { 'GARBAGE_DRIVER' }
            $staff = @{name="Local Pilot $key";email=$credentials[$key].UserName;password=(Plain $credentials[$key].Password);
                role=$role;employeeId="LOCAL-PILOT-$key";ward='Local pilot'}
            if ($key -eq 'Driver') { $staff.phone='local-test'; $staff.vehicleNumber='LOCAL-TEST-01' }
            $output = $staff | ConvertTo-Json -Compress | & node src/scripts/provisionStaff.js
            $staff = $null
            if ($LASTEXITCODE -ne 0) { throw 'Local staff provisioning failed; no credentials are printed.' }
        }
        $credentials | Export-Clixml -LiteralPath $credentialPath
        Write-Host 'Fresh local pilot staff created; credentials encrypted for this Windows account.'
    } elseif ($Action -eq 'ConfigureMedia') {
        if ((Read-Host 'Use only an approved TEST Cloudinary environment. Type TEST to confirm') -cne 'TEST') { throw 'Test storage confirmation required.' }
        $media = @{
            CloudName = Read-Host 'Test cloud name (hidden)' -AsSecureString
            ApiKey = Read-Host 'Test API key (hidden)' -AsSecureString
            ApiSecret = Read-Host 'Test API secret (hidden)' -AsSecureString
        }
        foreach ($field in @('CloudName','ApiKey','ApiSecret')) { if ([string]::IsNullOrWhiteSpace((Plain $media[$field]))) { throw 'All three test-storage settings are required.' } }
        $env:CLOUDINARY_CLOUD_NAME = Plain $media.CloudName
        $env:CLOUDINARY_API_KEY = Plain $media.ApiKey
        $env:CLOUDINARY_API_SECRET = Plain $media.ApiSecret
        # A file argument avoids Windows PowerShell's native-argument quote rewriting.
        $env:TEST_MEDIA_DIAGNOSTIC_PATH = Join-Path $runtime 'test-media-diagnostic.json'
        try {
            & node (Join-Path $root 'backend/src/scripts/checkTestMedia.cjs')
            $mediaCheckExit = $LASTEXITCODE
        } finally { Remove-Item Env:TEST_MEDIA_DIAGNOSTIC_PATH -ErrorAction SilentlyContinue }
        switch ($mediaCheckExit) {
            0 { }
            2 { throw 'Test storage settings are incomplete; nothing was saved.' }
            3 { throw 'Test storage checker execution failed; authentication was not confirmed and nothing was saved.' }
            4 { throw 'Cloudinary rejected test configuration/authentication/permissions; see the sanitized diagnostic. Nothing was saved.' }
            5 { throw 'Cloudinary connectivity/service check failed; authentication is unconfirmed and nothing was saved.' }
            default { throw 'Node checker failed to execute normally (possibly syntax/runtime error); this is not an authentication verdict. Nothing was saved.' }
        }
        $media | Export-Clixml -LiteralPath $mediaPath
        Write-Host 'Test storage saved with Windows encryption. Run Local-Pilot.ps1 -Action Restart to apply it.'
    } elseif ($Action -in @('Start','Restart')) {
        $credentials = Read-LocalCredentials
        if ($Action -eq 'Restart') {
            $meta = Get-Content -LiteralPath (Join-Path $runtime 'backend-process.json') | ConvertFrom-Json
            $owned = Get-CimInstance Win32_Process -Filter "ProcessId=$($meta.pid)"
            if ($owned) {
                if ($owned.Name -ne 'node.exe' -or $owned.CommandLine -notlike '*src/server.js*') { throw 'Refusing to stop an unrelated process.' }
                Stop-Process -Id $owned.ProcessId -Force
                Start-Sleep -Milliseconds 500
            }
        }
        if (Get-NetTCPConnection -LocalPort 5000 -State Listen -ErrorAction SilentlyContinue) { throw 'Port 5000 is occupied; refusing to replace any existing service.' }
        $env:JWT_SECRET = Plain $credentials.Jwt
        $node = (Get-Command node).Source
        $process = Start-Process -FilePath $node -ArgumentList 'src/server.js' -WorkingDirectory (Join-Path $root 'backend') -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $runtime 'backend.stdout.log') -RedirectStandardError (Join-Path $runtime 'backend.stderr.log')
        @{pid=$process.Id;database='phase1_pilot';port=5000} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runtime 'backend-process.json')
        $env:JWT_SECRET = $null
        $mediaState = if (Test-Path -LiteralPath $mediaPath) { 'approved test-media settings loaded' } else { 'photo storage unconfigured' }
        Write-Host "Local backend started (PID $($process.Id)); $mediaState."
    } elseif ($Action -eq 'Provision') {
        $credentials = Read-LocalCredentials
        if (Test-Path -LiteralPath $tokenPath) { throw 'Device token already saved; refusing to provision duplicate IDs.' }
        & (Join-Path $PSScriptRoot 'Provision-TestBin.ps1') -Latitude 18.4624311 -Longitude 73.8636227 `
            -LocationText 'O Building, State Bank CHS, near Chaitraban, Upper Indira Nagar, Bibwewadi, Pune, Maharashtra 411037.' `
            -OfficerCredential $credentials.Officer -DeviceTokenPath $tokenPath
    } elseif ($Action -eq 'Watch') {
        & node src/scripts/watchLocalBin.js
    } elseif ($Action -eq 'StopBridge') {
        $metaPath = Join-Path $runtime 'bridge-process.json'
        if (Test-Path -LiteralPath $metaPath) {
            $meta = Get-Content -LiteralPath $metaPath | ConvertFrom-Json
            $owner = Get-CimInstance Win32_Process -Filter "ProcessId=$($meta.pid)"
            if ($owner -and $owner.CommandLine -like '*Local-Pilot.ps1*' -and $owner.CommandLine -like '*Bridge*' -and $owner.ProcessId -ne $PID) {
                foreach ($child in (Get-CimInstance Win32_Process -Filter "ParentProcessId=$($meta.pid)")) {
                    if ($child.Name -match '^python.*\.exe$') { Stop-Process -Id $child.ProcessId -Force }
                }
                Stop-Process -Id $owner.ProcessId -Force
                Write-Host 'Stopped this pilot bridge; COM4 is available for Serial Monitor/upload.'
            } else { Write-Host 'No matching supervised pilot bridge is running.' }
        }
    } elseif ($Action -eq 'Bridge') {
        $metaPath = Join-Path $runtime 'bridge-process.json'
        if (Test-Path -LiteralPath $metaPath) {
            $meta = Get-Content -LiteralPath $metaPath | ConvertFrom-Json
            $owner = Get-CimInstance Win32_Process -Filter "ProcessId=$($meta.pid)"
            if ($owner -and $owner.CommandLine -like '*Local-Pilot.ps1*' -and $owner.CommandLine -like '*Bridge*' -and $owner.ProcessId -ne $PID) {
                throw 'Pilot bridge already running. Use StopBridge first; do not open the port twice.'
            }
        }
        if (-not (Test-Path -LiteralPath $tokenPath)) { throw 'Provision the local device first.' }
        $env:BIN_DEVICE_TOKEN = Plain (Import-Clixml -LiteralPath $tokenPath)
        $env:BIN_BACKEND_URL = 'http://127.0.0.1:5000'
        $env:BIN_COM_PORT = 'COM4' # Physically confirmed by unplug/replug, not inferred from its number.
        $env:BIN_BAUD = '9600'
        try { & python -B (Join-Path $root 'bridge/smart_bin_bridge.py') }
        finally { $env:BIN_DEVICE_TOKEN = $null }
    } elseif ($Action -eq 'Passwords') {
        $credentials = Read-LocalCredentials
        foreach ($key in @('Officer','Driver')) {
            $secret = Read-Host "Choose a local $key password (12+ characters; nothing is printed)" -AsSecureString
            if ((Plain $secret).Length -lt 12) { throw 'Password must have at least 12 characters.' }
            $credentials[$key] = [PSCredential]::new($credentials[$key].UserName, $secret)
        }
        $payload = @{officer=(Plain $credentials.Officer.Password);driver=(Plain $credentials.Driver.Password)}
        $payload | ConvertTo-Json -Compress | & node src/scripts/setLocalPilotPasswords.js
        $payload = $null
        if ($LASTEXITCODE -ne 0) { throw 'Local password update failed.' }
        $credentials | Export-Clixml -LiteralPath $credentialPath
        Write-Host 'Local account passwords updated without printing them.'
    }
} finally { Pop-Location }
