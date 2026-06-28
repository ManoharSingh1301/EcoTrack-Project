# Windows PowerShell Script to Start All EcoTrack Services
# Run this script from the project root directory

Write-Host "🌱 Starting EcoTrack Microservices Platform..." -ForegroundColor Green
Write-Host ""

# Function to start a service in a new PowerShell window
function Start-Service {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [string]$Command,
        [int]$WaitSeconds
    )
    
    Write-Host "Starting $ServiceName..." -ForegroundColor Yellow
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ServicePath'; Write-Host '🚀 Starting $ServiceName' -ForegroundColor Cyan; $Command"
    
    if ($WaitSeconds -gt 0) {
        Write-Host "Waiting $WaitSeconds seconds for $ServiceName to initialize..." -ForegroundColor Gray
        Start-Sleep -Seconds $WaitSeconds
    }
}

# Get the current directory
$ProjectRoot = Get-Location

# Start Discovery Server (Eureka) - Wait 30 seconds
Start-Service -ServiceName "Discovery Server (Eureka)" `
              -ServicePath "$ProjectRoot\discovery-server" `
              -Command "mvn spring-boot:run" `
              -WaitSeconds 30

# Start API Gateway - Wait 20 seconds
Start-Service -ServiceName "API Gateway" `
              -ServicePath "$ProjectRoot\api-gateway" `
              -Command "mvn spring-boot:run" `
              -WaitSeconds 20

# Start Item Service - Wait 15 seconds
Start-Service -ServiceName "Item Service" `
              -ServicePath "$ProjectRoot\item-service" `
              -Command "mvn spring-boot:run" `
              -WaitSeconds 15

# Start User Service - Wait 15 seconds
Start-Service -ServiceName "User Service" `
              -ServicePath "$ProjectRoot\user-service" `
              -Command "mvn spring-boot:run" `
              -WaitSeconds 15

# Start Communication Service - Wait 15 seconds
Start-Service -ServiceName "Communication Service" `
              -ServicePath "$ProjectRoot\communication" `
              -Command "mvn spring-boot:run" `
              -WaitSeconds 15

# Start Frontend - No wait needed
Start-Service -ServiceName "React Frontend" `
              -ServicePath "$ProjectRoot\frontend" `
              -Command "npm run dev" `
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
