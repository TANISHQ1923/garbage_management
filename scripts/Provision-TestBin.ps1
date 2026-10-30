param(
    [Parameter(Mandatory = $true)][ValidateRange(-90,90)][double]$Latitude,
    [Parameter(Mandatory = $true)][ValidateRange(-180,180)][double]$Longitude,
    [Parameter(Mandatory = $true)][string]$LocationText,
    [string]$BinId = 'UNO-BIN-001',
    [string]$DeviceId = 'UNO-USB-001',
    [string]$BackendUrl = 'http://127.0.0.1:5000',
    [System.Management.Automation.PSCredential]$OfficerCredential,
    [string]$DeviceTokenPath
)
$ErrorActionPreference = 'Stop'
$target = [Uri]$BackendUrl
if ($target.Scheme -ne 'https' -and -not ($target.Scheme -eq 'http' -and $target.IsLoopback)) {
    throw 'Use HTTPS or a loopback HTTP backend.'
}
if ($OfficerCredential) {
    $email = $OfficerCredential.UserName
    $protectedPassword = $OfficerCredential.Password.Copy()
} else {
    $email = Read-Host 'Officer email (use an approved local test account)'
    $protectedPassword = Read-Host 'Officer password' -AsSecureString
}
$password = [System.Net.NetworkCredential]::new('', $protectedPassword).Password
try {
    $loginBody = @{ email = $email; password = $password; role = 'MUNICIPAL_OFFICER' } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/auth/login" -ContentType 'application/json' -Body $loginBody
    $headers = @{ Authorization = "Bearer $($login.data.token)" }
    $body = @{ binId = $BinId; deviceId = $DeviceId; label = 'Arduino Uno prototype';
        locationText = $LocationText; latitude = $Latitude; longitude = $Longitude } | ConvertTo-Json
    $result = Invoke-RestMethod -Method Post -Uri "$BackendUrl/api/bins" -Headers $headers -ContentType 'application/json' -Body $body
    # Process environment only: do not print or save the one-time device token.
    $env:BIN_DEVICE_TOKEN = $result.data.deviceToken
    $env:BIN_BACKEND_URL = $BackendUrl
    if ($DeviceTokenPath) {
        ConvertTo-SecureString $result.data.deviceToken -AsPlainText -Force | Export-Clixml -LiteralPath $DeviceTokenPath
    }
    Write-Host "Provisioned bin $BinId and device $DeviceId. Device token is set in this PowerShell process."
    Write-Host 'Run the bridge from this same terminal. Do not print or commit the token.'
} catch {
    throw 'Provisioning failed. Check local backend, officer account, transaction support, and duplicate IDs. No credential details are printed.'
} finally {
    $password = $null; $loginBody = $null; $login = $null; $headers = $null; $result = $null
    if ($protectedPassword) { $protectedPassword.Dispose() }
}
