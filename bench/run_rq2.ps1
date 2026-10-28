# PowerShell Runner cho RQ2 - Cặp 2 (C2)
# M = 2^20 (1,048,576), alpha = 0.50, tau = Infinity, 6 mốc C
# Ghi nhận dữ liệu chuẩn 21 cột (csv_schema.md), kiểm tra output_check chéo, sao lưu CSV gốc.

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$m = 1048576
$alpha = 0.50
$seed = 42
$repeat = 1
$queryCount = 1000000
$tau = "Infinity"
$timeoutSec = 60
$jarPath = "HashTableProject/dist/HashTableProject.jar"
$csvFile = "results/raw/rq2_results_1m.csv"
$backupCsvFile = "results/raw/rq2_results_1m_backup.csv"

$csvHeader = "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,build_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit"

$milestones = @(
    @{ C = 0; Label = "C = 0" },
    @{ C = 262144; Label = "C = 0.25M (262,144)" },
    @{ C = 524288; Label = "C = 0.50M (524,288)" },
    @{ C = 1048576; Label = "C = 1.00M (1,048,576)" },
    @{ C = 2097152; Label = "C = 2.00M (2,097,152)" },
    @{ C = 4194304; Label = "C = 4.00M (4,194,304)" }
)

Write-Host "=========================================================="
Write-Host "   BENCHMARK RUNNER RQ2: 6 MỐC C TẠI M = 2^20 (CẶP 2 - C2)"
Write-Host "=========================================================="

# Đảm bảo thư mục tồn tại và khởi tạo file CSV với header chuẩn 21 cột
$csvDir = Split-Path -Parent $csvFile
if (!(Test-Path $csvDir)) {
    New-Item -ItemType Directory -Path $csvDir -Force | Out-Null
}

$needHeader = $true
if (Test-Path $csvFile) {
    $existing = Get-Content $csvFile -TotalCount 1 -Encoding UTF8
    if ($existing -eq $csvHeader) {
        $existingRows = @(Import-Csv $csvFile -Encoding UTF8)
        if ($existingRows.Count -gt 0) {
            throw "Output already contains benchmark rows: $csvFile. Preserve it and choose a new output filename before rerunning."
        }
    } else {
        throw "Existing output has a different CSV schema: $csvFile. Preserve it and choose a new output filename."
    }
}
if ($needHeader) {
    [System.IO.File]::WriteAllText($csvFile, $csvHeader + "`r`n", [System.Text.Encoding]::UTF8)
}

function Run-BenchmarkProcess {
    param(
        [string]$Arguments,
        [int]$Timeout
    )
    $pinfo = New-Object System.Diagnostics.ProcessStartInfo
    $pinfo.FileName = "java"
    $pinfo.Arguments = $Arguments
    $pinfo.RedirectStandardOutput = $true
    $pinfo.RedirectStandardError = $true
    $pinfo.UseShellExecute = $false

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $pinfo
    $p.Start() | Out-Null

    $exited = $p.WaitForExit($Timeout * 1000)
    $stopwatch.Stop()

    if (-not $exited) {
        $p.Kill()
        return @{
            Status = "timeout"
            ElapsedMs = $stopwatch.ElapsedMilliseconds
            ExitCode = -1
            Output = ""
            Error = "Process killed after ${Timeout}s"
        }
    }
    $stdout = $p.StandardOutput.ReadToEnd()
    $stderr = $p.StandardError.ReadToEnd()
    $procStatus = if ($p.ExitCode -eq 0) { "ok" } else { "crash" }
    return @{
        Status = $procStatus
        ElapsedMs = $stopwatch.ElapsedMilliseconds
        ExitCode = $p.ExitCode
        Output = $stdout
        Error = $stderr
    }
}

$variants = @("base", "clean_table")
$queries = @("hit", "miss")
$modes = @("count", "time")

$totalConfigs = $milestones.Count * $variants.Count * $queries.Count * $modes.Count
$currentConfig = 0
$timeoutCount = 0
$errorCount = 0
$successCount = 0

foreach ($item in $milestones) {
    $c = $item.C
    $lbl = $item.Label
    Write-Host "`n>>> [MỐC $lbl] (C = $c) ..." -ForegroundColor Cyan

    # 1. Chạy so sánh đối chứng --compare-clean
    Write-Host "  -> Đang chạy so sánh đối chứng (--compare-clean)..."
    $compArgs = "-Xmx2g -jar $jarPath --rq RQ2 --algo lp-tomb --m $m --alpha $alpha --c $c --tau $tau --seed $seed --compare-clean"
    $compRes = Run-BenchmarkProcess -Arguments $compArgs -Timeout $timeoutSec
    if ($compRes.Status -eq "ok") {
        Write-Host $compRes.Output
    } else {
        Write-Host "  [CẢNH BÁO] Compare-clean thất bại hoặc timeout: $($compRes.Status)" -ForegroundColor Yellow
    }

    # 2. Chạy ma trận 8 lượt chuẩn cho mốc này: variant (base, clean_table) x query (hit, miss) x mode (count, time)
    foreach ($variant in $variants) {
        foreach ($query in $queries) {
            foreach ($mode in $modes) {
                $currentConfig++
                Write-Host "  [$currentConfig/$totalConfigs] variant=$variant, query=$query, mode=$mode ... " -NoNewline

                $benchArgs = "-Xmx2g -jar $jarPath --rq RQ2 --algo lp-tomb --variant $variant --m $m --alpha $alpha --c $c --tau $tau --seed $seed --repeat $repeat --query $query --queries $queryCount --mode $mode --csv-only"
                $benchRes = Run-BenchmarkProcess -Arguments $benchArgs -Timeout $timeoutSec

                if ($benchRes.Status -eq "ok") {
                    $lines = $benchRes.Output.Trim() -split "`r?`n"
                    $csvRow = ""
                    foreach ($l in $lines) {
                        $l = $l.Trim()
                        if ($l -and -not $l.StartsWith("run_id,") -and ($l -split ",").Count -eq 21) {
                            $csvRow = $l
                        }
                    }
                    if ($csvRow) {
                        [System.IO.File]::AppendAllText($csvFile, $csvRow + "`r`n", [System.Text.Encoding]::UTF8)
                        $successCount++
                        Write-Host "OK ($($benchRes.ElapsedMs) ms)" -ForegroundColor Green
                    } else {
                        Write-Host "LỖI PARSE CSV OUTPUT" -ForegroundColor Red
                        $errorCount++
                    }
                } elseif ($benchRes.Status -eq "timeout") {
                    $timeoutCount++
                    Write-Host "TIMEOUT (> ${timeoutSec}s)" -ForegroundColor Red
                    $varPrefix = ""
                    if ($variant -ne "base") { $varPrefix = $variant.ToUpper() + "_" }
                    $runId = "RUN_RQ2_LP_TOMB_${varPrefix}M${m}_A0.50_C${c}_S${seed}_R${repeat}_UNIFORM_${query.ToUpper()}_${mode.ToUpper()}"
                    $params = "M=$m;alpha=$alpha;c=$c;tau=inf"
                    if ($query -eq "miss") { $params += ";query=miss" }
                    $timeoutRow = "$runId,RQ2,lp-tomb,$variant,uniform,524288,$params,$seed,,1.0.0,$repeat,,,,,,timeout,2g,PC_LAB_01,8b19d6d"
                    [System.IO.File]::AppendAllText($csvFile, $timeoutRow + "`r`n", [System.Text.Encoding]::UTF8)
                } else {
                    $errorCount++
                    Write-Host "CRASH (Exit: $($benchRes.ExitCode))" -ForegroundColor Red
                }
            }
        }
    }
}

Write-Host "`n=========================================================="
Write-Host "                KIỂM TRA CHÉO DỮ LIỆU RQ2                 "
Write-Host "=========================================================="

# Đọc toàn bộ file CSV để kiểm tra chéo
$records = Import-Csv -Path $csvFile -Encoding UTF8

Write-Host "1. Tổng số dòng dữ liệu thu thập được: $($records.Count) / $totalConfigs"
Write-Host "   - Thành công: $successCount"
Write-Host "   - Timeout: $timeoutCount"
Write-Host "   - Lỗi / Crash: $errorCount"
$duplicateRunIds = @($records | Group-Object run_id | Where-Object { !$_.Name -or $_.Count -gt 1 })
Write-Host "   - run_id trùng/rỗng: $($duplicateRunIds.Count)"

# Kiểm tra output_check chéo giữa base và clean_table
Write-Host "`n2. Kiểm tra output_check chéo giữa base và clean_table:"
$mismatchCount = 0
foreach ($item in $milestones) {
    $c = $item.C
    $lbl = $item.Label
    foreach ($query in $queries) {
        if ($query -eq "hit") {
            $baseRows = @($records | Where-Object { $_.variant -eq "base" -and $_.params -like "*c=$c*" -and $_.params -notlike "*query=miss*" })
            $cleanRows = @($records | Where-Object { $_.variant -eq "clean_table" -and $_.params -like "*c=$c*" -and $_.params -notlike "*query=miss*" })
        } else {
            $baseRows = @($records | Where-Object { $_.variant -eq "base" -and $_.params -like "*c=$c*" -and $_.params -like "*query=miss*" })
            $cleanRows = @($records | Where-Object { $_.variant -eq "clean_table" -and $_.params -like "*c=$c*" -and $_.params -like "*query=miss*" })
        }

        $baseChecks = ($baseRows | Select-Object -ExpandProperty output_check -Unique) -join ", "
        $cleanChecks = ($cleanRows | Select-Object -ExpandProperty output_check -Unique) -join ", "

        $match = ($baseChecks -eq $cleanChecks) -and ($baseChecks -ne "")
        if ($match) {
            Write-Host "   [KHỚP 100%] Mốc $lbl | Query $query | output_check = $baseChecks" -ForegroundColor Green
        } else {
            Write-Host "   [LỆCH / THIẾU] Mốc $lbl | Query $query | base: $baseChecks vs clean: $cleanChecks" -ForegroundColor Red
            $mismatchCount++
        }
    }
}

# Sao lưu CSV gốc
Write-Host "`n3. Tiến hành sao lưu CSV gốc..."
Copy-Item -Path $csvFile -Destination $backupCsvFile -Force
if (Test-Path $backupCsvFile) {
    $origSize = (Get-Item $csvFile).Length
    $bakSize = (Get-Item $backupCsvFile).Length
    Write-Host ('   [ĐÃ SAO LƯU THÀNH CÔNG] File sao lưu: ' + $backupCsvFile + ' - Dung lượng: ' + $bakSize + ' bytes') -ForegroundColor Green
} else {
    Write-Host '   [LỖI] Không thể tạo file sao lưu!' -ForegroundColor Red
}

if ($successCount -ne $totalConfigs -or $timeoutCount -gt 0 -or $errorCount -gt 0 -or $mismatchCount -gt 0 -or $duplicateRunIds.Count -gt 0) {
    throw "RQ2 run incomplete: success=$successCount/$totalConfigs, timeout=$timeoutCount, errors=$errorCount, checksum mismatches=$mismatchCount, duplicate/empty run_ids=$($duplicateRunIds.Count)."
}

Write-Host ''
Write-Host '=========================================================='
Write-Host '               HOAN TAT NHIEM VU CA NHOM (RQ2)            '
Write-Host '=========================================================='
