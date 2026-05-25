param(
    [string]$MtlsRepo = $env:MTLS_REPO
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($MtlsRepo)) {
    $MtlsRepo = Join-Path (Split-Path $PSScriptRoot -Parent | Split-Path -Parent) "mtls-secret-rotation"
}

if (-not (Test-Path $MtlsRepo)) {
    throw "mtls-secret-rotation repo not found at '$MtlsRepo'. Set MTLS_REPO or clone it as a sibling of sportsbetting-eventdriven."
}

$Pom = Join-Path $MtlsRepo "pom.xml"
if (-not (Test-Path $Pom)) {
    throw "No pom.xml found in '$MtlsRepo'."
}

Write-Host "Installing mTLS starter from $MtlsRepo"
mvn -q -f $Pom install -DskipTests
