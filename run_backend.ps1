$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Host "--- Java Version Check ---" -ForegroundColor Cyan
java -version

$port = 8081
$portProcess = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portProcess) {
    Write-Host "WARNING: Port $port is already in use by process ID $($portProcess.OwningProcess)." -ForegroundColor Yellow
    Write-Host "The application might fail to start." -ForegroundColor Yellow
}

Write-Host "--- Starting Maven ---" -ForegroundColor Cyan
Push-Location $PSScriptRoot
& "./mvnw.cmd" clean spring-boot:run
Pop-Location
