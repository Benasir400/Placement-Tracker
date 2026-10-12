# Build script to bundle React frontend into Spring Boot for single-jar deployment
Write-Host "==> Step 1: Building React Frontend..." -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot\..\frontend"
npm.cmd run build

if ($LASTEXITCODE -ne 0) {
    Write-Error "Frontend build failed!"
    exit 1
}

Write-Host "==> Step 2: Copying frontend build artifacts into Spring Boot static resources..." -ForegroundColor Cyan
$staticDir = "$PSScriptRoot\..\backend\src\main\resources\static"
if (Test-Path $staticDir) {
    Remove-Item -Recurse -Force $staticDir
}
New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
Copy-Item -Path "$PSScriptRoot\..\frontend\dist\*" -Destination $staticDir -Recurse -Force

Write-Host "==> Step 3: Packaging Spring Boot JAR with embedded frontend..." -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot\..\backend"
mvn clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nSUCCESS: Full-stack standalone JAR created at backend\target\placetrack-backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Green
    Write-Host "You can run it with: java -jar backend\target\placetrack-backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Green
} else {
    Write-Error "Backend package failed!"
}
