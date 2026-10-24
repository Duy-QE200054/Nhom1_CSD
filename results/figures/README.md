# Biểu Đồ Thực Nghiệm Cho Bài Báo IEEE (Paper Figures)

Thư mục lưu trữ toàn bộ các hình ảnh đồ thị phục vụ bài báo khoa học theo mẫu IEEE, được sinh tự động từ script `bench/plot_figures.py` theo đúng quy định tại mục 7.5 của Đề tài 2 - CSD201 FA26.

Có thể gom các CSV trong thư mục dữ liệu rồi vẽ lại toàn bộ hình bằng:

```powershell
python bench\plot_figures.py --input-dir results\raw --outdir results\figures
```

Script ghi các dòng CSV vào `results\raw\rq3_merged.csv` trước khi vẽ. Nếu chỉ muốn gom một số file cụ thể, dùng `--csv file1.csv file2.csv`; có thể chỉ định nơi lưu file gom bằng `--merge-output`.

---

## Danh Sách Các Biểu Đồ RQ3 (Nhiệm vụ Cặp 3 - C3)

### 1. `fig_rq3_probes_vs_churn.png`
- **Mục đích**: Đánh giá số bước dò trung bình (Average Probes) khi quy mô chu kỳ churn $C$ tăng dần ($65536 \to 262144$) tại $M = 262144, \alpha = 0.50$.
- **Phân tích bóc tách**:
  - Đồ thị (a) - Hit queries: Cả 3 cấu hình (`rh`, `rh-noearly`, `lp-backshift`) duy trì số bước dò ổn định quanh ngưỡng lý thuyết $\approx 1.50$ bước dò, không bị suy giảm theo $C$.
  - Đồ thị (b) - Miss queries: Thể hiện ưu thế vượt trội của cơ chế dừng sớm Early Exit trong `rh` (chỉ cần $\approx 1.74$ bước dò) so với $\approx 2.49$ bước dò của `rh-noearly` và `lp-backshift` (tiết kiệm hơn $30\%$ số ô phải kiểm tra).
- **Nguồn dữ liệu**: `results/raw/rq3_results.csv`
- **Script tạo**: `bench/plot_figures.py`

## Campaign mở rộng ngày 24/10/2026

Để giữ nguyên các hình lịch sử bên trên, kết quả đo tại $M=2^{20}$ được vẽ
thành bộ hình riêng trong `results/figures/rq3_20261024/`. Bộ hình mở rộng
bao gồm `lp-tomb` ($\tau=0.2$), `lp-backshift`, `rh`, và `rh-noearly`, tại các
mốc $C \in \{0, M/4, M/2, M, 2M, 4M\}$; nhãn dung lượng bảng được lấy trực tiếp
từ CSV đầu vào.

Sau khi hoàn tất dữ liệu, tái tạo hình bằng:

```powershell
python bench\plot_figures.py --csv results\raw\rq3_20261024.csv --outdir results\figures\rq3_20261024
```

---

### 2. `fig_rq3_lookup_latency.png`
- **Mục đích**: So sánh độ trễ thời gian tra cứu thực tế (Lookup Latency tính bằng ms) trên $131072$ truy vấn sau $C$ chu kỳ churn.
- **Phát hiện chính**:
  - Không tồn tại hiện tượng tích tụ tombstone hay nghẽn phân rã hiệu năng ở cả ba cấu hình xóa không dùng tombstone.
  - Robin Hood đạt thời gian tra cứu trượt (miss) nhanh nhất nhờ thoát vòng lặp ngay khi $DIB_{cur} > DIB_{slot}$.
- **Nguồn dữ liệu**: `results/raw/rq3_results.csv`
- **Script tạo**: `bench/plot_figures.py`

---

### 3. `fig_rq3_mechanism_ablation.png`
- **Mục đích**: Biểu đồ cột bóc tách định lượng 3 cơ chế cấu thành nên Robin Hood Hashing:
  1. **Xóa dịch lùi (Backshift Deletion)**: Loại bỏ triệt để $100\%$ tombstone, bảo toàn độ rỗng bảng mà không cần chạy Rebuild tuần hoàn gây dừng hệ thống.
  2. **Hoán đổi DIB (DIB Swapping)**: Phân phối lại chuỗi dò công bằng giữa các khóa, giảm mạnh độ lệch chuẩn và chặn trên chiều dài chuỗi dò ($O(\log \log n)$).
  3. **Dừng sớm khi tra cứu (Early Exit)**: Giảm $30.2\%$ số bước dò trên các truy vấn vắng mặt (Miss), mang lại lợi thế quyết định trong các hệ thống chỉ mục giao dịch thực tế nơi truy vấn trượt chiếm ưu thế.
- **Nguồn dữ liệu**: `results/raw/rq3_results.csv`
- **Script tạo**: `bench/plot_figures.py`
