<#
.SYNOPSIS
  AssetMan Auth Smoke Test (PowerShell)

.DESCRIPTION
  Tests:
    1) POST /api/auth/login (gets access+refresh)
    2) GET  <probe endpoint> with Authorization Bearer <access>
    3) POST /api/auth/refresh (gets new access+refresh)
    4) GET  <probe endpoint> with Authorization Bearer <new access>

  Exits non-zero on failure so it can be used in CI.
#>

param(
  [string] $BaseUrl = "http://localhost:8080",
  [string] $TenantSlug = "demo",
  [string] $Email = "admin@demo.com",
  [string] $Password = "DemoPass!",
  [string] $ProbePath = "/api/locations/tree",   # pick a real auth-protected endpoint
  [switch] $VerboseHttp
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Write-Step([string]$msg) {
  Write-Host ""
  Write-Host "==> $msg" -ForegroundColor Cyan
}

function Fail([string]$msg) {
  Write-Host ""
  Write-Host "FAIL: $msg" -ForegroundColor Red
  exit 1
}

function Assert-NotEmpty([string]$name, $value) {
  if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) {
    Fail "$name is empty/missing."
  }
}

function Invoke-Json([string]$Method, [string]$Url, $BodyObj = $null, [hashtable]$Headers = @{}) {
  $jsonBody = $null
  if ($null -ne $BodyObj) {
    $jsonBody = $BodyObj | ConvertTo-Json -Compress -Depth 20
  }

  try {
    if ($VerboseHttp) {
      Write-Host "HTTP $Method $Url"
      if ($jsonBody) { Write-Host "Body: $jsonBody" }
    }

    if ($null -ne $jsonBody) {
      return Invoke-RestMethod -Method $Method -Uri $Url -ContentType "application/json" -Headers $Headers -Body $jsonBody
    } else {
      return Invoke-RestMethod -Method $Method -Uri $Url -Headers $Headers
    }
  } catch {
    # Try to surface server response body if present (super helpful for debugging)
    $resp = $_.Exception.Response
    if ($resp -and $resp.GetResponseStream()) {
      try {
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $bodyText = $reader.ReadToEnd()
        Fail "Request failed: $Method $Url`nStatus: $($resp.StatusCode) $($resp.StatusDescription)`nBody: $bodyText"
      } catch {
        Fail "Request failed: $Method $Url`n$($_.Exception.Message)"
      }
    }
    Fail "Request failed: $Method $Url`n$($_.Exception.Message)"
  }
}

function Invoke-Probe([string]$AccessToken) {
  $probeUrl = "$BaseUrl$ProbePath"
  $headers = @{ Authorization = "Bearer $AccessToken" }

  Write-Step "Probe auth with GET $ProbePath"
  try {
    if ($VerboseHttp) { Write-Host "HTTP GET $probeUrl" }
    $r = Invoke-RestMethod -Method Get -Uri $probeUrl -Headers $headers
    Write-Host "Probe OK"
    return $r
  } catch {
    # expose response details
    $resp = $_.Exception.Response
    if ($resp -and $resp.GetResponseStream()) {
      $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
      $bodyText = $reader.ReadToEnd()
      Fail "Probe failed: GET $ProbePath`nStatus: $($resp.StatusCode) $($resp.StatusDescription)`nBody: $bodyText"
    }
    Fail "Probe failed: GET $ProbePath`n$($_.Exception.Message)"
  }
}

# ---- Run ----

Write-Step "Config"
Write-Host "BaseUrl    : $BaseUrl"
Write-Host "TenantSlug : $TenantSlug"
Write-Host "Email      : $Email"
Write-Host "ProbePath  : $ProbePath"

Write-Step "Login (POST /api/auth/login)"
$login = Invoke-Json -Method "Post" -Url "$BaseUrl/api/auth/login" -BodyObj @{
  tenantSlug = $TenantSlug
  email      = $Email
  password   = $Password
}

# Adjust property names if your DTO differs
Assert-NotEmpty "accessToken" $login.accessToken
Assert-NotEmpty "refreshToken" $login.refreshToken

Write-Host "Login OK"
Write-Host ("Access token:  {0}..." -f ([string]$login.accessToken).Substring(0, [Math]::Min(20, ([string]$login.accessToken).Length)))
Write-Host ("Refresh token: {0}..." -f ([string]$login.refreshToken).Substring(0, [Math]::Min(20, ([string]$login.refreshToken).Length)))

# Probe with access token
$null = Invoke-Probe -AccessToken $login.accessToken

Write-Step "Refresh (POST /api/auth/refresh)"
$refreshed = Invoke-Json -Method "Post" -Url "$BaseUrl/api/auth/refresh" -BodyObj @{
  refreshToken = $login.refreshToken
}

Assert-NotEmpty "accessToken (refresh)" $refreshed.accessToken
Assert-NotEmpty "refreshToken (refresh)" $refreshed.refreshToken

Write-Host "Refresh OK"
Write-Host ("New access token:  {0}..." -f ([string]$refreshed.accessToken).Substring(0, [Math]::Min(20, ([string]$refreshed.accessToken).Length)))

# Probe with refreshed access token
$null = Invoke-Probe -AccessToken $refreshed.accessToken

Write-Host ""
Write-Host "[PASS] SMOKE TEST PASSED" -ForegroundColor Green
exit 0
