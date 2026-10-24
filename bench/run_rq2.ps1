# PowerShell Runner cho RQ2 - Cặp 2 (C2)
# M = 2^20 (1,048,576), alpha = 0.50, tau = Infinity, 6 mốc C

$m = 1048576
$alpha = 0.50
$seed = 42
$tau = "Infinity"
$timeoutSec = 60

$milestones = @(
    @{ C = 0; Label = "C = 0" },
    @{ C = 262144; Label = "C = 0.25M (262,144)" },
    @{ C = 524288; Label = "C = 0.50M (524,288)" },
    @{ C = 1048576; Label = "C = 1.00M (1,048,576)" },
    @{ C = 2097152; Label = "C = 2.00M (2,097,152)" },
    @{ C = 4194304; Label = "C = 4.00M (4,194,304)" }
)

Write-Host "=========================================================="
Write-Host "   CHẠY THỰC NGHIỆM RQ2: 6 MỐC C TẠI M = 2^20 (CẶP 2 - C2)"
Write-Host "=========================================================="

foreach ($item in $milestones) {
    $c = $item.C
    $lbl = $item.Label
    Write-Host "`n>>> [BẮT ĐẦU] $lbl ..."
    
    $cmd = "java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ2 --algo lp-tomb --m $m --alpha $alpha --c $c --tau $tau --seed $seed --compare-clean"
    
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $pinfo = New-Object System.Diagnostics.ProcessStartInfo
    $pinfo.FileName = "java"
    $pinfo.Arguments = "-Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ2 --algo lp-tomb --m $m --alpha $alpha --c $c --tau $tau --seed $seed --compare-clean"
    $pinfo.RedirectStandardOutput = $true
    $pinfo.RedirectStandardError = $true
    $pinfo.UseShellExecute = $false
    
    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $pinfo
    $p.Start() | Out-Null
    
    $exited = $p.WaitForExit($timeoutSec * 1000)
    $stopwatch.Stop()
    
    if (-not $exited) {
        $p.Kill()
        Write-Host "[TIMEOUT] Mốc $lbl vượt quá ${timeoutSec}s -> Đã cưỡng chế dừng JVM (Status: TIMEOUT)" -ForegroundColor Red
    } else {
        $stdout = $p.StandardOutput.ReadToEnd()
        $stderr = $p.StandardError.ReadToEnd()
        if ($p.ExitCode -eq 0) {
            Write-Host "[HOÀN TẤT] Thời gian thực thi: $($stopwatch.ElapsedMilliseconds) ms" -ForegroundColor Green
            Write-Host $stdout
        } else {
            Write-Host "[LỖI] ExitCode: $($p.ExitCode)" -ForegroundColor Red
            Write-Host $stderr
        }
    }
}
