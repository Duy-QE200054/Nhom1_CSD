param(
    [switch]$Full,
    [string]$OutputPath,
    [ValidateRange(1, 3600)]
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runnerPath = Join-Path $PSScriptRoot "run_all.py"
$jarPath = Join-Path $repoRoot "HashTableProject\dist\HashTableProject.jar"

if (-not (Test-Path $runnerPath -PathType Leaf)) {
    throw "RQ3 runner not found: $runnerPath"
}

$python = Get-Command python -ErrorAction SilentlyContinue
$pythonPrefix = @()
if (-not $python) {
    $python = Get-Command py -ErrorAction SilentlyContinue
    if ($python) {
        $pythonPrefix = @("-3")
    }
}
if (-not $python) {
    throw "Python 3 was not found. Install Python 3 and add python.exe or py.exe to PATH."
}

if (-not (Test-Path $jarPath -PathType Leaf)) {
    $ant = Get-Command ant -ErrorAction SilentlyContinue
    if (-not $ant) {
        throw "Project JAR is missing and Ant was not found. Build HashTableProject in NetBeans, then run this script again."
    }

    Write-Host "Building HashTableProject.jar..." -ForegroundColor Yellow
    Push-Location (Join-Path $repoRoot "HashTableProject")
    try {
        & $ant.Source jar
        if ($LASTEXITCODE -ne 0) {
            throw "Ant build failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }

    if (-not (Test-Path $jarPath -PathType Leaf)) {
        throw "Ant completed, but the expected JAR was not created: $jarPath"
    }
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $runName = if ($Full) { "rq3_full" } else { "rq3_quick" }
    $OutputPath = Join-Path $repoRoot (
        "results\raw\{0}_{1}.csv" -f $runName, (Get-Date -Format "yyyyMMdd_HHmmss")
    )
}
elseif (-not [System.IO.Path]::IsPathRooted($OutputPath)) {
    $OutputPath = Join-Path $repoRoot $OutputPath
}

$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null

$utf8 = New-Object System.Text.UTF8Encoding($false)
$previousPythonUtf8 = $env:PYTHONUTF8
$previousPythonIoEncoding = $env:PYTHONIOENCODING
$env:PYTHONUTF8 = "1"
$env:PYTHONIOENCODING = "utf-8"
& chcp.com 65001 | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Could not set the console code page to UTF-8."
}
[Console]::OutputEncoding = $utf8
$OutputEncoding = $utf8

$commitId = "local"
$git = Get-Command git -ErrorAction SilentlyContinue
if ($git) {
    $detectedCommit = & $git.Source -C $repoRoot rev-parse --short HEAD 2>$null
    if ($LASTEXITCODE -eq 0 -and $detectedCommit) {
        $commitId = "$detectedCommit".Trim()
    }
}

$runnerArgs = @(
    $runnerPath,
    "--rq", "RQ3",
    "--jar", $jarPath,
    "--output", $OutputPath,
    "--timeout", "$TimeoutSeconds",
    "--algorithms", "lp-tomb,lp-backshift,rh,rh-noearly",
    "--git-commit", $commitId
)

if ($Full) {
    $runnerArgs += @(
        "--m", "1048576",
        "--alpha", "0.80",
        "--cycles", "0,262144,524288,1048576,2097152,4194304",
        "--seeds", "42,1337,20261019",
        "--repeats", "1",
        "--tau", "0.1"
    )
    $expectedRuns = 288
    Write-Host "RQ3 full campaign: M=1048576, alpha=0.80, 3 seeds, 6 churn checkpoints." -ForegroundColor Cyan
}
else {
    $runnerArgs += @(
        "--quick",
        "--m", "262144",
        "--alpha", "0.50",
        "--cycles", "65536",
        "--seeds", "42",
        "--repeats", "1",
        "--tau", "0.2"
    )
    $expectedRuns = 16
    Write-Host "RQ3 quick check: M=262144, alpha=0.50, seed=42, C=65536." -ForegroundColor Cyan
}

Write-Host "Algorithms: lp-tomb, lp-backshift, rh, rh-noearly" -ForegroundColor Cyan
Write-Host "Output CSV: $OutputPath" -ForegroundColor Cyan
Write-Host "Each run measures both hit/miss and count/time." -ForegroundColor Cyan
Write-Host ""

Push-Location $repoRoot
try {
    & $python.Source @pythonPrefix @runnerArgs
    $runnerExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
    if ($null -eq $previousPythonUtf8) {
        Remove-Item Env:\PYTHONUTF8 -ErrorAction SilentlyContinue
    }
    else {
        $env:PYTHONUTF8 = $previousPythonUtf8
    }
    if ($null -eq $previousPythonIoEncoding) {
        Remove-Item Env:\PYTHONIOENCODING -ErrorAction SilentlyContinue
    }
    else {
        $env:PYTHONIOENCODING = $previousPythonIoEncoding
    }
}

if ($runnerExitCode -ne 0) {
    throw "RQ3 benchmark runner failed with exit code $runnerExitCode. Review the error above."
}

if (-not (Test-Path $OutputPath -PathType Leaf)) {
    throw "RQ3 runner returned success, but the result CSV was not created: $OutputPath"
}

$rows = @(Import-Csv -Path $OutputPath -Encoding UTF8)
if ($rows.Count -ne $expectedRuns) {
    throw "Expected $expectedRuns result rows, found $($rows.Count). Check the CSV and runner output."
}

$okCount = @($rows | Where-Object { $_.status -eq "ok" }).Count
$timeoutCount = @($rows | Where-Object { $_.status -eq "timeout" }).Count
$failureCount = $rows.Count - $okCount - $timeoutCount

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "RQ3 RESULTS CHECK" -ForegroundColor Cyan
Write-Host "Rows: $($rows.Count) / $expectedRuns" -ForegroundColor White
Write-Host "Successful: $okCount | Timeout: $timeoutCount | Other failures: $failureCount" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Cyan

$summaryRows = foreach ($group in ($rows | Group-Object algorithm, variant, params)) {
    $sample = $group.Group[0]
    $cycleMatch = [regex]::Match($sample.params, "(?:^|;)c=(\d+)")
    $queryMatch = [regex]::Match($sample.run_id.ToUpperInvariant(), "_(HIT|MISS)_(COUNT|TIME)$")
    $probeValues = @(
        $group.Group |
            Where-Object { $_.status -eq "ok" -and -not [string]::IsNullOrWhiteSpace($_.ops_count) } |
            ForEach-Object { [double]::Parse($_.ops_count, [Globalization.CultureInfo]::InvariantCulture) }
    )
    $timeValues = @(
        $group.Group |
            Where-Object { $_.status -eq "ok" -and -not [string]::IsNullOrWhiteSpace($_.elapsed_ms) } |
            ForEach-Object { [double]::Parse($_.elapsed_ms, [Globalization.CultureInfo]::InvariantCulture) }
    )

    [pscustomobject]@{
        C = if ($cycleMatch.Success) { $cycleMatch.Groups[1].Value } else { "?" }
        Query = if ($queryMatch.Success) { $queryMatch.Groups[1].Value.ToLowerInvariant() } else { "?" }
        Algorithm = $sample.algorithm
        Variant = $sample.variant
        MeanProbes = if ($probeValues.Count) { [math]::Round(($probeValues | Measure-Object -Average).Average, 4) } else { "-" }
        MeanMs = if ($timeValues.Count) { [math]::Round(($timeValues | Measure-Object -Average).Average, 2) } else { "-" }
        Runs = $group.Count
        Status = if (@($group.Group | Where-Object { $_.status -ne "ok" }).Count -eq 0) { "ok" } else { "check" }
    }
}

Write-Host ""
Write-Host "SUMMARY BY CHURN, QUERY, AND ALGORITHM" -ForegroundColor Cyan
$summaryRows |
    Sort-Object { [int]$_.C }, Query, Algorithm |
    Format-Table -AutoSize |
    Out-String -Width 180 |
    Write-Host

Write-Host "CSV saved: $OutputPath" -ForegroundColor Green
if ($failureCount -gt 0) {
    Write-Warning "Some rows are not successful. Check the status column in the CSV."
}
