param(
    [ValidateSet("release","debug")]
    [string]$BuildType
)

$ModsPath = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Build {
    param([string]$Profile)

    Write-Host "Building project with $Profile profile..."
    Push-Location $ScriptDir

    mvn clean package -P $Profile
    if ($LASTEXITCODE -ne 0) { exit 1 }

    if (-not (Test-Path $ModsPath)) { New-Item -ItemType Directory -Force -Path $ModsPath | Out-Null }

    Write-Host "Copying JAR to $ModsPath"
    Copy-Item -Path "$ScriptDir\target\*.jar" -Destination $ModsPath -Force

    if ($Profile -eq "Debug") {
        $DebugDir = Join-Path $ModsPath "DebugAssets"
        $CommonDest = Join-Path $DebugDir "Common"
        $ServerDest = Join-Path $DebugDir "Server"

        New-Item -ItemType Directory -Force -Path $CommonDest, $ServerDest | Out-Null

        Copy-Item -Path "$ScriptDir\src\main\resources\Common\*" -Destination $CommonDest -Recurse -Force
        Copy-Item -Path "$ScriptDir\src\main\resources\Server\*" -Destination $ServerDest -Recurse -Force
        Copy-Item -Path "$ScriptDir\src\main\resources\debug_manifest.json" -Destination (Join-Path $DebugDir "manifest.json") -Force
    }

    Write-Host "$Profile build complete!"
    Pop-Location
}

Build $BuildType