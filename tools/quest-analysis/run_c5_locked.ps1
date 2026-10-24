param(
    [ValidateSet('test', 'batch')][string]$Mode,
    [string]$Manifest,
    [string]$Output,
    [string]$Log
)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$lock = [IO.Path]::GetFullPath((Join-Path $root '..\build.lock'))
$owned = $false
while (-not $owned) {
    try {
        $handle = [IO.File]::Open($lock, 'CreateNew', 'Write', 'None')
        $writer = New-Object IO.StreamWriter($handle)
        $writer.Write("C5 offline $Mode PID=$PID $(Get-Date -Format s)")
        $writer.Close()
        $owned = $true
    } catch [IO.IOException] {
        if (-not (Test-Path -LiteralPath $lock)) { throw }
        Write-Output ('等待构建锁：' + [IO.File]::ReadAllText($lock))
        Start-Sleep -Seconds 5
    }
}
try {
    Set-Location -LiteralPath $root
    $env:JAVA_HOME = [IO.Path]::GetFullPath((Join-Path $root '..\jdk-21'))
    foreach ($argument in @($Manifest, $Output, $Log)) {
        if ($argument -match '["&|<>^%\r\n]') { throw '参数包含不允许的命令字符' }
    }
    # 让 cmd 将原始 stderr 合并入日志，避开 PS5.1 的 NativeCommandError 包装。
    if ($Mode -eq 'test') {
        $command = '.\gradlew.bat test --tests "*quest.adjust*" -PskipHandbook=1 --no-daemon'
    } else {
        $command = '.\gradlew.bat -I tools/quest-analysis/quest-adjust.gradle questAdjustBatchDryRun "-Pmanifest=' + $Manifest + '" "-Pout=' + $Output + '" -PskipHandbook=1 --no-daemon --no-configuration-cache'
    }
    $command += ' > "' + $Log + '" 2>&1'
    $process = Start-Process -FilePath $env:ComSpec -ArgumentList ('/d /s /c "' + $command + '"') -WorkingDirectory $root -WindowStyle Hidden -Wait -PassThru
    Get-Content -LiteralPath $Log -Encoding UTF8
    if ($process.ExitCode -ne 0) { throw "Gradle失败，exit=$($process.ExitCode)" }
} finally {
    if ($owned) { Remove-Item -LiteralPath $lock }
}
