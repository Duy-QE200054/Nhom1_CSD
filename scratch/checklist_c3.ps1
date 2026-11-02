# ==============================================================================
# Script: scratch/checklist_c3.ps1
# Nhiệm vụ: Chạy Checklist tự kiểm tra Mục 9 cho Cặp 3 (C3) - Ngày 28/10/2026
# Tác giả: nguyendinhtien824-super (C3 - Robin Hood & Backshift)
# ==============================================================================

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   CHẠY TOÀN BỘ CHECKLIST MỤC 9 TỰ KIỂM TRA CHO CẶP 3 (C3)      " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$pythonExe = "python"
if (-not (Get-Command $pythonExe -ErrorAction SilentlyContinue)) {
    Write-Host "Lỗi: Không tìm thấy Python trong PATH!" -ForegroundColor Red
    exit 1
}

& $pythonExe scratch/checklist_c3.py

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n>>> XÁC NHẬN: TẤT CẢ CÁC MỤC CHECKLIST C3 ĐỀU ĐẠT CHUẨN 100%! <<<`n" -ForegroundColor Green
} else {
    Write-Host "`n!!! CẢNH BÁO: PHÁT HIỆN LỖI TRONG CHECKLIST C3 !!!`n" -ForegroundColor Red
}
exit $LASTEXITCODE
