# run_rq1.ps1 - Chay toan bo RQ1 (Cap 1) bang BenchmarkRunner, moi lan chay la 1 tien trinh JVM rieng, timeout 60s.
# Dat file nay trong D:\csd1\Nhom1_CSD\bench\ roi chay tu thu muc goc repo:
#   powershell -ExecutionPolicy Bypass -File .\bench\run_rq1.ps1 -Pilot     # thu M nho truoc
#   powershell -ExecutionPolicy Bypass -File .\bench\run_rq1.ps1            # chay that

param(
    [string]$Classes   = "HashTableProject\build\classes",
    [string]$Out       = "results\raw\rq1_runner_results_lab.csv",
    [long]$Seed        = 42,
    [string]$Xmx       = "2g",
    [string]$MachineId = "PC_LAB_01",
    [int]$TimeoutSec   = 60,
    [int]$ClusterLpTimeoutSec = 300,   # lp-tomb + clustered + M lon rat cham, 60s khong du
    [string]$OnlyAlgo    = "",        # vi du: lp-tomb (rong = chay het)
    [string]$OnlyDataset = "",        # vi du: clustered (rong = chay het)
    [switch]$Pilot
)

$Header = "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,build_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit"

if ($Pilot) { $Out = "results\raw\rq1_pilot.csv" }
if (Test-Path $Out) { throw "File $Out da ton tai. Doi ten/xoa truoc de tranh tron du lieu." }
New-Item -ItemType Directory -Force (Split-Path $Out) | Out-Null

function Append-Line([string]$line) {
    $needHeader = -not (Test-Path $Out) -or (Get-Item $Out).Length -eq 0
    $text = ""
    if ($needHeader) { $text += $Header + [Environment]::NewLine }
    $text += $line + [Environment]::NewLine
    [IO.File]::AppendAllText((Join-Path (Get-Location) $Out), $text, (New-Object Text.UTF8Encoding($false)))
}

# ---- Danh sach cau hinh RQ1 ----
$cfgs = @()
$m20 = 1048576
if ($Pilot) { $m20 = 16384 }
foreach ($algo in "sc","lp-tomb") {
    # Thi nghiem A: M co dinh, alpha thay doi
    foreach ($a in 0.50,0.75,0.90,0.95) { $cfgs += ,@($algo,$m20,$a,"uniform","A") }
    # Thi nghiem B: alpha = 0.75, M thay doi
    $msB = 262144,1048576,2097152,4194304,8388608
    if ($Pilot) { $msB = 4096,16384 }
    foreach ($m in $msB) { if ($m -ne $m20 -or $true) { $cfgs += ,@($algo,$m,0.75,"uniform","B") } }
    # Bo don cum: 1 luot
    $msC = 262144,1048576
    if ($Pilot) { $msC = 4096,16384 }
    foreach ($m in $msC) { $cfgs += ,@($algo,$m,0.75,"clustered","CLUSTER") }
}

$total = $cfgs.Count * 4   # moi cau hinh: {hit,miss} x {time,count}
$i = 0
foreach ($c in $cfgs) {
    $algo,$m,$a,$ds,$tag = $c
    if ($OnlyAlgo -and $algo -ne $OnlyAlgo) { $i += 4; continue }
    if ($OnlyDataset -and $ds -ne $OnlyDataset) { $i += 4; continue }
    $to = $TimeoutSec
    if ($ds -eq "clustered" -and $algo -eq "lp-tomb") { $to = $ClusterLpTimeoutSec }
    foreach ($q in "hit","miss") {
        foreach ($mode in "time","count") {
            $i++
            $argList = @(
                "-Xmx$Xmx","-cp",$Classes,"hashtable.bench.BenchmarkRunner",
                "--rq","RQ1","--algo",$algo,"--m",$m,"--alpha",$a,"--seed",$Seed,
                "--dataset",$ds,"--query",$q,"--repeat",1,"--xmx",$Xmx,
                "--machine-id",$MachineId,"--csv-only","--out",$Out
            )
            if ($mode -eq "count") { $argList += "--count" }

            Write-Host ("[{0}/{1}] {2} M={3} alpha={4} ds={5} q={6} mode={7}" -f $i,$total,$algo,$m,$a,$ds,$q,$mode)
            $p = Start-Process -FilePath "java" -ArgumentList $argList -NoNewWindow -PassThru
            $null = $p.Handle   # giu handle, neu khong ExitCode co the bi rong ($null) trong PowerShell 5.1
            if (-not $p.WaitForExit($to * 1000)) {
                $p.Kill()
                Write-Warning "  TIMEOUT (> $to s) [mode=$mode q=$q]"
                $params = "M=$m;alpha=$a;c=0;tau=inf"
                if ($q -eq "miss") { $params += ";query=miss" }   # cung quy uoc voi dong ok cua runner
                $runId  = "RUN_RQ1_" + $algo.ToUpper().Replace('-','_') + "_1"
                $line = "$runId,RQ1,$algo,base,$ds,,$params,$Seed,,1.0.0,1,,,,,,,timeout,$Xmx,$MachineId,8b19d6d"
                Append-Line $line
            } else {
                $p.WaitForExit()   # cho tien trinh ket thuc han, ExitCode moi day du
                $code = $p.ExitCode
                if ($null -eq $code) { Write-Warning "  Khong doc duoc exit code (kiem tra dong CSV vua ghi)" }
                elseif ($code -ne 0) { Write-Warning "  Loi, exit code $code" }
            }
        }
    }
}

Write-Host "`nXong. Kiem tra:"
Write-Host "  (Import-Csv $Out).Count"
Write-Host "  Import-Csv $Out | Group-Object algorithm,status | Select Name,Count"