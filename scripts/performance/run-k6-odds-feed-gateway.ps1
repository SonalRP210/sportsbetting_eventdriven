# Run k6 odds-feed load test (works before PATH refresh after MSI install).
# Do not pass k6's --duration/--vus CLI flags here: they replace the script's constant-arrival-rate scenario.
# Tune with project env (not K6_* — k6 v2 treats those as global overrides): e.g.
#   $env:ODDS_GATEWAY_DURATION='2m'; $env:ODDS_GATEWAY_ARRIVAL_RATE='4'; .\run-k6-odds-feed-gateway.ps1
$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$k6Script = Join-Path $scriptDir "k6-odds-feed-gateway.js"
$k6 = Get-Command k6 -ErrorAction SilentlyContinue
$exe = if ($k6) { $k6.Source } else { "C:\Program Files\k6\k6.exe" }
if (-not (Test-Path $exe)) {
    Write-Error "k6 not found. Install: winget install GrafanaLabs.k6"
    exit 1
}
Set-Location (Join-Path $scriptDir "..\..")
& $exe run $k6Script @args
