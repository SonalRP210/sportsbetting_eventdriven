# Install mtls-spring-boot-starter into local ~/.m2 from sibling repo.
$ErrorActionPreference = "Stop"
$SportsRoot = Split-Path $PSScriptRoot -Parent
$MtlsRepo = Join-Path (Split-Path $SportsRoot -Parent) "mtls-secret-rotation"
if (-not (Test-Path (Join-Path $MtlsRepo "pom.xml"))) {
    throw "mtls-secret-rotation not found at $MtlsRepo"
}
Write-Host "Installing from $MtlsRepo"
Push-Location $MtlsRepo
mvn -q clean install -DskipTests
Pop-Location
Write-Host "Installed com.example.mtls:mtls-spring-boot-starter:1.0.0-SNAPSHOT"
