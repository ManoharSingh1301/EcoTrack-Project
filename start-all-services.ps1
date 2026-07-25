# Windows PowerShell Script to Start All EcoTrack Services
# Run this script from the project root directory

Write-Host "🌱 Starting EcoTrack Microservices Platform..." -ForegroundColor Green
Write-Host ""

function Start-EcoTrackService {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [string]$Command,
        [int]$WaitSeconds
    )

    Write-Host "Starting $ServiceName..." -ForegroundColor Yellow
    $commandText = @"
Set-Location '$ServicePath'
`$env:DB_USERNAME = 'root'
`$env:DB_PASSWORD = 'password'
`$env:JWT_SECRET = 'dev-secret'
Write-Host '🚀 Starting $ServiceName' -ForegroundColor Cyan
$Command
"@

    Start-Process powershell -ArgumentList "-NoExit", "-Command", $commandText

    if ($WaitSeconds -gt 0) {
        Write-Host "Waiting $WaitSeconds seconds for $ServiceName to initialize..." -ForegroundColor Gray
        Start-Sleep -Seconds $WaitSeconds
    }
}

$ProjectRoot = $PSScriptRoot

Start-EcoTrackService -ServiceName "Discovery Server (Eureka)" `
    -ServicePath "$ProjectRoot\discovery-server" `
    -Command "mvn spring-boot:run" `
    -WaitSeconds 30

Start-EcoTrackService -ServiceName "API Gateway" `
    -ServicePath "$ProjectRoot\api-gateway" `
    -Command "mvn spring-boot:run" `
    -WaitSeconds 20

Start-EcoTrackService -ServiceName "Item Service" `
    -ServicePath "$ProjectRoot\item-service" `
    -Command "mvn spring-boot:run" `
    -WaitSeconds 15

Start-EcoTrackService -ServiceName "User Service" `
    -ServicePath "$ProjectRoot\user-service" `
    -Command "mvn spring-boot:run" `
    -WaitSeconds 15

Start-EcoTrackService -ServiceName "Communication Service" `
    -ServicePath "$ProjectRoot\communication" `
    -Command "mvn spring-boot:run" `
    -WaitSeconds 15

Start-EcoTrackService -ServiceName "React Frontend" `
    -ServicePath "$ProjectRoot\frontend" `
    -Command "npm install; npm run dev -- --host 0.0.0.0" `
    -WaitSeconds 0

Write-Host ""
Write-Host "✅ All services are starting!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Service URLs:" -ForegroundColor Cyan
Write-Host "  - Discovery Server (Eureka): http://localhost:8761" -ForegroundColor White
Write-Host "  - API Gateway:                http://localhost:8080" -ForegroundColor White
Write-Host "  - React Frontend:             http://localhost:5173" -ForegroundColor White
Write-Host "  - Communication Service:      http://localhost:8087" -ForegroundColor White
Write-Host ""
Write-Host "⏳ Please wait for all services to fully start (about 2-3 minutes)" -ForegroundColor Yellow
Write-Host "💡 Check the Eureka dashboard to verify all services are registered" -ForegroundColor Yellow
Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
