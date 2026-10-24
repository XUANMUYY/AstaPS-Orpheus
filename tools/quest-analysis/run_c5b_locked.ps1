param([ValidateSet('Test','Categories','Examples')][string]$Task='Test', [string]$OutputDir='')
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$lock=Join-Path (Split-Path $root -Parent) 'build.lock'
$owned=$false
Push-Location $root
try {
    $stream=[IO.File]::Open($lock,'CreateNew','Write')
    $writer=New-Object IO.StreamWriter($stream)
    try { $writer.Write("C5b $Task $(Get-Date -Format s)") } finally { $writer.Dispose() }
    $owned=$true
    $log=Join-Path $root "build/quest-adjust/c5b-$($Task.ToLower())-verified.log"
    if($OutputDir -and $OutputDir -notmatch '^build/quest-adjust/[A-Za-z0-9_-]+$') { throw '输出目录只能是 build/quest-adjust 下的直接子目录' }
    # Merge native stderr inside cmd, avoiding PowerShell 5.1 NativeCommandError on compiler warnings.
    if($Task -eq 'Test') {
        $gradleCommand='.\gradlew.bat test --tests "*quest.adjust*" -PskipHandbook=1 --no-daemon 2>&1'
    } elseif($Task -eq 'Categories') {
        if(!$OutputDir) { $OutputDir='build/quest-adjust/category-plans-delivery' }
        $gradleCommand=".\gradlew.bat -I tools/quest-analysis/quest-adjust.gradle questAdjustBatchDryRun -Pmanifest=build/quest-adjust/category-input/manifest.json -Pout=$OutputDir -PskipHandbook=1 --no-daemon --no-configuration-cache 2>&1"
    } else {
        if(!$OutputDir) { $OutputDir='build/quest-adjust/c5b-examples-delivery' }
        $gradleCommand=".\gradlew.bat -I tools/quest-analysis/quest-adjust.gradle questAdjustBatchDryRun -Pmanifest=build/quest-adjust/c5b-examples-input.json -Pout=$OutputDir -PskipHandbook=1 --no-daemon --no-configuration-cache 2>&1"
    }
    & cmd.exe /d /c $gradleCommand | Tee-Object -FilePath $log
    $result=$LASTEXITCODE
    if($result -ne 0) { throw "Gradle 失败：exit=$result；日志 $log" }
} finally {
    if($owned) { Remove-Item -LiteralPath $lock }
    Pop-Location
}
