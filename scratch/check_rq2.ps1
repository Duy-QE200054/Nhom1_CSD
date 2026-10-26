# Check RQ2 Data Integrity, output_check, and anomalies

$f1 = "results\raw\rq2_results.csv"
$f2 = "results\raw\rq2_results_backup.csv"

$h1 = (Get-FileHash -Algorithm SHA256 $f1).Hash
$h2 = (Get-FileHash -Algorithm SHA256 $f2).Hash

Write-Host "================================================================"
Write-Host "             KIỂM TRA TÍNH TOÀN VẸN VÀ BẢN SAO LƯU CSV           "
Write-Host "================================================================"
Write-Host "File gốc   : $f1"
Write-Host "SHA-256    : $h1"
Write-Host "File backup: $f2"
Write-Host "SHA-256    : $h2"
Write-Host "Khớp nhau 100%: $($h1 -eq $h2)"

$rows = Import-Csv $f1
Write-Host "`nTổng số dòng dữ liệu trong rq2_results.csv: $($rows.Count)"

Write-Host "`n================================================================"
Write-Host "             KIỂM TRA TRẠNG THÁI (STATUS / TIMEOUT / LỖI)        "
Write-Host "================================================================"
$statusGroups = $rows | Group-Object status
$statusGroups | Select-Object Name, Count | Format-Table -AutoSize

$bad = $rows | Where-Object { $_.status -ne "ok" }
if ($bad.Count -eq 0) {
    Write-Host "[XÁC NHẬN] 100% các lượt chạy có status = 'ok'. Không có timeout, crash hay oom."
} else {
    Write-Host "[CẢNH BÁO] Phát hiện lượt chạy bị lỗi:"
    $bad | Format-Table -AutoSize
}

Write-Host "`n================================================================"
Write-Host "         KIỂM TRA CHÉO OUTPUT_CHECK (BASE vs CLEAN_TABLE)        "
Write-Host "================================================================"
# Group by workload parameters: (c, seed, query)
# Each group has:
# base (count), base (time), clean_table (count), clean_table (time)

$grouped = @{}
foreach ($r in $rows) {
    # Parse c and query from params
    $params = $r.params -split ";"
    $cVal = "0"
    $queryVal = "hit"
    foreach ($p in $params) {
        if ($p -like "c=*") { $cVal = $p.Substring(2) }
        if ($p -like "query=*") { $queryVal = $p.Substring(6) }
    }
    $key = "$cVal|$($r.seed)|$queryVal"
    if (-not $grouped.ContainsKey($key)) {
        $grouped[$key] = @()
    }
    $grouped[$key] += $r
}

$allCheckSumMatch = $true
$tableReport = @()

foreach ($key in ($grouped.Keys | Sort-Object { [int]($_ -split "\|")[0] })) {
    $parts = $key -split "\|"
    $cVal = $parts[0]
    $seedVal = $parts[1]
    $queryVal = $parts[2]
    
    $groupRows = $grouped[$key]
    
    $baseRows = $groupRows | Where-Object { $_.variant -eq "base" }
    $cleanRows = $groupRows | Where-Object { $_.variant -eq "clean_table" }
    
    $baseChecks = ($baseRows | Select-Object -ExpandProperty output_check -Unique) -join ", "
    $cleanChecks = ($cleanRows | Select-Object -ExpandProperty output_check -Unique) -join ", "
    
    $isMatch = ($baseChecks -eq $cleanChecks)
    if (-not $isMatch) { $allCheckSumMatch = $false }
    
    # Get ops_count
    $baseOps = ($baseRows | Where-Object { $_.ops_count -ne "" } | Select-Object -ExpandProperty ops_count)
    $cleanOps = ($cleanRows | Where-Object { $_.ops_count -ne "" } | Select-Object -ExpandProperty ops_count)
    
    $tableReport += [PSCustomObject]@{
        Milestone_C = $cVal
        Seed = $seedVal
        Query = $queryVal
        Base_Check = $baseChecks
        Clean_Check = $cleanChecks
        Check_Match = if ($isMatch) { "MATCH" } else { "MISMATCH" }
        Base_Probes = $baseOps
        Clean_Probes = $cleanOps
    }
}

$tableReport | Format-Table -AutoSize

Write-Host "Kết luận kiểm tra chéo output_check RQ2: $(if ($allCheckSumMatch) { 'TẤT CẢ KHỚP 100%' } else { 'CÓ LỆCH' })"

Write-Host "`n================================================================"
Write-Host "             ĐỐI CHIẾU CHÉO VỚI DỮ LIỆU RQ3 (CÙNG WORKLOAD)      "
Write-Host "================================================================"
$rq3_file = "results\raw\rq3_20261024.csv"
if (Test-Path $rq3_file) {
    $rq3_rows = Import-Csv $rq3_file | Where-Object { $_.seed -eq "42" }
    Write-Host "Tìm thấy $($rq3_rows.Count) dòng tại seed 42 trong rq3_20261024.csv"
    
    # Compare output_check for each c and query
    $crossCompare = @()
    foreach ($cVal in @(0, 262144, 524288, 1048576, 2097152, 4194304)) {
        foreach ($queryVal in @("hit", "miss")) {
            $rq2_k = "$cVal|42|$queryVal"
            $rq2_check = ($grouped[$rq2_k] | Select-Object -ExpandProperty output_check -Unique) -join ","
            
            # Find in RQ3
            $rq3_matches = $rq3_rows | Where-Object { 
                $p = $_.params -split ";"
                $thisC = ""
                $thisQ = "hit"
                foreach ($item in $p) {
                    if ($item -like "c=*") { $thisC = $item.Substring(2) }
                    if ($item -like "query=*") { $thisQ = $item.Substring(6) }
                }
                ($thisC -eq [string]$cVal) -and ($thisQ -eq $queryVal)
            }
            $rq3_checks = ($rq3_matches | Select-Object -ExpandProperty output_check -Unique) -join ","
            $rq3_algos = ($rq3_matches | Select-Object -ExpandProperty algorithm -Unique) -join ", "
            
            $crossCompare += [PSCustomObject]@{
                Milestone_C = $cVal
                Query = $queryVal
                RQ2_OutputCheck = $rq2_check
                RQ3_OutputCheck = $rq3_checks
                Algos_in_RQ3 = $rq3_algos
                Cross_Match = if ($rq2_check -eq $rq3_checks) { "MATCH 100%" } else { "MISMATCH" }
            }
        }
    }
    $crossCompare | Format-Table -AutoSize
}
