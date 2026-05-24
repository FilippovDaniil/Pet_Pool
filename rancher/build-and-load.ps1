param(
    [switch]$Restart
)

$ImageName = "billiard-club"
$ImageTag  = "1.0.0"
$FullImage = "${ImageName}:${ImageTag}"
$TarPath   = "$env:TEMP\${ImageName}.tar"
$Namespace = "billiard-club"
$Deploy    = "billiard-app"

Write-Host "=== Step 1: Build JAR (local Gradle) ===" -ForegroundColor Cyan
& "$PSScriptRoot\..\gradlew.bat" bootJar -x test --no-daemon
if (-not $?) { Write-Error "Gradle build failed"; exit 1 }

Write-Host "=== Step 2: Build Docker image ===" -ForegroundColor Cyan
docker build --provenance=false -t $FullImage "$PSScriptRoot\.."
if (-not $?) { Write-Error "docker build failed"; exit 1 }

Write-Host "=== Step 3: Save to tar ===" -ForegroundColor Cyan
docker save $FullImage -o $TarPath
if (-not $?) { Write-Error "docker save failed"; exit 1 }

Write-Host "=== Step 4: Load into Rancher Desktop VM ===" -ForegroundColor Cyan
$WslPath = "/mnt/c/Users/$env:USERNAME/AppData/Local/Temp/${ImageName}.tar"
rdctl shell -- sh -c "docker load < $WslPath"
if (-not $?) { Write-Error "docker load in VM failed"; exit 1 }

if ($Restart) {
    Write-Host "=== Step 5: Rollout restart ===" -ForegroundColor Cyan
    kubectl rollout restart deployment/$Deploy -n $Namespace
    kubectl rollout status deployment/$Deploy -n $Namespace --timeout=120s
}

Write-Host "=== Done! ===" -ForegroundColor Green
Write-Host "Image $FullImage loaded into Rancher Desktop VM."
Write-Host "To deploy: kubectl apply -f rancher/k8s/"
Write-Host "To update running app: .\rancher\build-and-load.ps1 -Restart"
