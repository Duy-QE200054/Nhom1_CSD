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
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$jarPath = Join-Path $repoRoot "HashTableProject\dist\HashTableProject.jar"
$csvFile = Join-Path $repoRoot "results\raw\rq2_results.csv"
$backupCsvFile = Join-Path $repoRoot "results\raw\rq2_results_backup.csv"

$csvHeader = "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,build_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit"

$outputAlreadyHasRows = $false
if (Test-Path $csvFile) {
    $existingHeader = Get-Content $csvFile -TotalCount 1 -Encoding UTF8
    if ($existingHeader -eq $csvHeader) {
        $outputAlreadyHasRows = @(Import-Csv $csvFile -Encoding UTF8).Count -gt 0
    } else {
        $outputAlreadyHasRows = $true
    }

    if ($outputAlreadyHasRows) {
        do {
            $runStamp = Get-Date -Format "yyyyMMdd_HHmmss_fff"
            $csvFile = Join-Path $repoRoot "results\raw\rq2_results_1m_full_$runStamp.csv"
            $backupCsvFile = Join-Path $repoRoot "results\raw\rq2_results_1m_full_${runStamp}_backup.csv"
            if ((Test-Path $csvFile) -or (Test-Path $backupCsvFile)) {
                Start-Sleep -Milliseconds 1
            }
        } while ((Test-Path $csvFile) -or (Test-Path $backupCsvFile))
        Write-Host "CSV hiện tại có dữ liệu hoặc schema khác; giữ nguyên và ghi kết quả lần này vào file mới: $csvFile" -ForegroundColor Yellow
    }
}

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
Write-Host "Mục tiêu: đo ảnh hưởng của tombstone lên tra cứu Linear Probing."
Write-Host "Cấu hình: M=$m ô; alpha=$alpha => N=$([int]($m * $alpha)) khóa sống ban đầu; tau=$tau."
Write-Host "Mỗi lượt chạy $queryCount truy vấn; C là số chu kỳ, mỗi chu kỳ gồm 1 lần xóa + 1 lần thêm."
Write-Host "Count đo probe trung bình/truy vấn; Time đo tổng thời gian truy vấn (ms)."
Write-Host "Chạy đủ $($milestones.Count) mốc; CSV sẽ được ghi tại: $csvFile"

# Đảm bảo thư mục tồn tại và khởi tạo file CSV với header chuẩn 21 cột
$csvDir = Split-Path -Parent $csvFile
if (!(Test-Path $csvDir)) {
    New-Item -ItemType Directory -Path $csvDir -Force | Out-Null
}

$needHeader = $true
if (Test-Path $csvFile) {
    $existing = Get-Content $csvFile -TotalCount 1 -Encoding UTF8
    if ($existing -eq $csvHeader) {
        $needHeader = $false
    } else {
        throw "CSV schema không khớp; giữ nguyên file hiện tại: $csvFile"
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
    $pinfo.WorkingDirectory = $repoRoot
    $pinfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $pinfo.StandardErrorEncoding = [System.Text.Encoding]::UTF8

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
    Write-Host "`n==========================================================" -ForegroundColor Cyan
    Write-Host "MỐC $lbl" -ForegroundColor Cyan
    if ($c -eq 0) {
        Write-Host "Diễn giải: chưa chạy chu kỳ xóa-thêm; đây là trạng thái ban đầu để đối chứng."
    } else {
        Write-Host "Diễn giải: chạy $c chu kỳ = $c lần xóa + $c lần thêm khóa mới (xấp xỉ $($c * 2) thao tác cập nhật)."
    }
    Write-Host "Sau churn, so sánh base (giữ tombstone) với clean_table (dựng lại sạch từ cùng các khóa sống)."

    # 1. Chạy so sánh đối chứng --compare-clean
    Write-Host "`n[KIỂM TRA NHANH] Bảng so sánh phụ dùng tối đa 10.000 truy vấn; không phải lượt lấy số liệu CSV 1 triệu."
    $compArgs = "-Xmx2g -Dfile.encoding=UTF-8 -jar `"$jarPath`" --rq RQ2 --algo lp-tomb --m $m --alpha $alpha --c $c --tau $tau --seed $seed --compare-clean"
    $compRes = Run-BenchmarkProcess -Arguments $compArgs -Timeout $timeoutSec
    if ($compRes.Status -eq "ok") {
        Write-Host $compRes.Output
    } else {
        Write-Host "  [CẢNH BÁO] Compare-clean thất bại: status=$($compRes.Status), exit=$($compRes.ExitCode)" -ForegroundColor Yellow
        if ($compRes.Error) { Write-Host "  Java stderr: $($compRes.Error)" -ForegroundColor Red }
        if ($compRes.Output) { Write-Host "  Java stdout: $($compRes.Output)" -ForegroundColor Red }
    }

    # 2. Chạy ma trận 8 lượt chuẩn cho mốc này: variant x query x mode
    foreach ($variant in $variants) {
        foreach ($query in $queries) {
            foreach ($mode in $modes) {
                $currentConfig++
                $queryDescription = if ($query -eq "hit") { "tìm khóa CÓ trong bảng" } else { "tìm khóa KHÔNG có trong workload" }
                $modeDescription = if ($mode -eq "count") { "đếm số probe trung bình" } else { "đo thời gian tra cứu" }
                Write-Host "`n[$currentConfig/$totalConfigs] BẮT ĐẦU LƯỢT ĐO"
                Write-Host "  Bảng: $variant | Query: $queryDescription | Số query: $queryCount | Phép đo: $modeDescription"
                Write-Host "  Cách diễn giải: $queryCount lần gọi get(key); churn và dựng bảng nằm ngoài vùng bấm giờ truy vấn."

                $benchArgs = "-Xmx2g -Dfile.encoding=UTF-8 -jar `"$jarPath`" --rq RQ2 --algo lp-tomb --variant $variant --m $m --alpha $alpha --c $c --tau $tau --seed $seed --repeat $repeat --query $query --queries $queryCount --mode $mode --csv-only"
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
                        $result = $csvRow | ConvertFrom-Csv -Header ($csvHeader -split ",") | Select-Object -First 1
                        Write-Host "  KẾT QUẢ ĐÃ GHI CSV: $csvFile" -ForegroundColor Green
                        Write-Host "  status=$($result.status); build_ms=$($result.build_ms); output_check/checksum=$($result.output_check)"
                        if ($mode -eq "count") {
                            Write-Host "  ops_count=$($result.ops_count) probe/truy vấn trung bình; elapsed_ms để trống."
                        } else {
                            Write-Host "  elapsed_ms=$($result.elapsed_ms) cho $queryCount truy vấn; ops_count để trống."
                        }
                        if ($query -eq "miss") {
                            Write-Host "  Kiểm tra miss: output_check=$($result.output_check); nếu bằng $queryCount thì toàn bộ truy vấn đều trả về không tìm thấy."
                        } else {
                            Write-Host "  Kiểm tra hit: output_check là checksum tổng giá trị trả về, không phải số lượng truy vấn."
                        }
                        Write-Host "  Thời gian tiến trình JVM bên ngoài (gồm khởi động và dựng dữ liệu): $($benchRes.ElapsedMs) ms."
                    } else {
                        Write-Host "  [LỖI] Java trả về thành công nhưng không tìm thấy dòng CSV 21 cột." -ForegroundColor Red
                        $errorCount++
                    }
                } elseif ($benchRes.Status -eq "timeout") {
                    $timeoutCount++
                    Write-Host "  [TIMEOUT] Vượt giới hạn ${timeoutSec}s; lượt này không được tính là kết quả thành công." -ForegroundColor Red
                    $varPrefix = ""
                    if ($variant -ne "base") { $varPrefix = $variant.ToUpper() + "_" }
                    $queryPart = $query.ToUpper()
                    $modePart = $mode.ToUpper()
                    $runId = "RUN_RQ2_LP_TOMB_${varPrefix}M${m}_A0.50_C${c}_S${seed}_R${repeat}_UNIFORM_${queryPart}_${modePart}"
                    $params = "M=$m;alpha=$alpha;c=$c;tau=inf"
                    if ($query -eq "miss") { $params += ";query=miss" }
                    $timeoutFields = @($runId, "RQ2", "lp-tomb", $variant, "uniform", "$([int]($m * $alpha))",
                        $params, "$seed", "", "1.0.0", "$repeat", "", "", "", "", "", "",
                        "timeout", "2g", "PC_LAB_01", "8b19d6d")
                    $timeoutRow = $timeoutFields -join ","
                    [System.IO.File]::AppendAllText($csvFile, $timeoutRow + "`r`n", [System.Text.Encoding]::UTF8)
                } else {
                    $errorCount++
                    Write-Host "CRASH (Exit: $($benchRes.ExitCode))" -ForegroundColor Red
                    if ($benchRes.Error) { Write-Host "  Java stderr: $($benchRes.Error)" -ForegroundColor Red }
                    if ($benchRes.Output) { Write-Host "  Java stdout: $($benchRes.Output)" -ForegroundColor Red }
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
Write-Host "`n3. Tiến hành sao lưu CSV vừa tạo..."
Copy-Item -Path $csvFile -Destination $backupCsvFile -Force
if (Test-Path $backupCsvFile) {
    $origSize = (Get-Item $csvFile).Length
    $bakSize = (Get-Item $backupCsvFile).Length
    Write-Host ('   [ĐÃ SAO LƯU THÀNH CÔNG] File sao lưu: ' + $backupCsvFile + ' - Dung lượng: ' + $bakSize + ' bytes') -ForegroundColor Green
} else {
    Write-Host '   [LỖI] Không thể tạo file sao lưu!' -ForegroundColor Red
}

Write-Host ''
Write-Host '=========================================================='
Write-Host '               HOAN TAT NHIEM VU CA NHOM (RQ2)            '
Write-Host '=========================================================='
