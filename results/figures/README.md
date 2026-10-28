## Quy Chuẩn Thống Nhất Bộ Biểu Đồ & Bảng Cho Paper (Nhiệm vụ [Cả nhóm])

Nhằm đảm bảo tính nhất quán khoa học và quy chuẩn thẩm mỹ bài báo IEEE theo mục 7.5 Đề tài 2:
1. **Phong cách đồ họa (Visual Style)**:
   - Font chữ chuẩn khoa học (DejaVu Sans / Helvetica) đồng bộ kích cỡ: Tiêu đề hình (12-13pt bold), nhãn trục (11pt), nhãn tick (10pt), chú giải legend (9.5-10pt).
   - Bảng màu IEEE chuẩn: `#1f77b4` (Xanh dương - Clean Table / Empty slots / Robin Hood), `#d62728` (Đỏ - Contaminated Table / Tombstones), `#2ca02c` (Xanh lá - Live Keys / LP Backshift), `#ff7f0e` (Cam - rh-noearly), `#9467bd` (Tím - Ngưỡng an toàn timeout).
   - Lưới tọa độ mảnh (`:` alpha 0.6) trợ thị, định dạng số có dấu phẩy ngăn cách hàng nghìn rõ ràng.
   - Chú thích chân hình chuẩn hóa ghi rõ: `Source: <đường_dẫn_CSV_thô> | Script: bench/plot_figures.py | IEEE CSD201 FA26`.
   - Độ phân giải xuất ảnh đạt chuẩn in ấn IEEE: **300 DPI**.

2. **Thống nhất Đánh số Bảng & Hình trong Paper `paper/paper.tex`**:
   - **Bảng (Tables)**:
     - **Table I (RQ1)**: Số bước dò lý thuyết Knuth vs. thực nghiệm của Separate Chaining và Linear Probing ($M=2^{20}, \alpha \in [0.50, 0.95]$).
     - **Table II (RQ2)**: Kết quả đo thực nghiệm RQ2 và ô nhiễm tombstone ($M=2^{20}, \alpha=0.50$, so sánh đối chứng base vs clean\_table).
     - **Table III (RQ3)**: Kết quả đo thực nghiệm RQ3 tại $C=4M$ ở tải trung bình ($M=2^{20}, \alpha=0.50$).
     - **Table IV (RQ3)**: Kết quả đo thực nghiệm RQ3 tại tải cao ($\alpha=0.80, \tau=0.10$).
     - **Table V (RQ3)**: Profiling chi tiết từng khối $10^4$ thao tác và trạng thái cấu trúc bảng khi hoàn thành ($C=4M$).
   - **Hình (Figures)** theo đúng trình tự đọc tuyến tính trong Paper:
     - **Fig. 1**: `fig1_probes_vs_alpha.png` (RQ1 - Số bước dò Hit/Miss thực nghiệm vs Knuth theo hệ số tải $\alpha$).
     - **Fig. 2**: `fig2_time_vs_alpha.png` (RQ1 - Độ trễ tra cứu ns/op theo $\alpha$, minh chứng nghịch lý số lần dò vs thời gian).
     - **Fig. 3**: `fig5_memory.png` (RQ1 - Dung lượng bộ nhớ JVM heap byte/khóa giữa Node của SC và mảng nguyên thủy LP).
     - **Fig. 4**: `fig_rq2_tombstone_dynamics.png` (RQ2 - Động học tích tụ tombstone, bùng nổ bước dò và biên an toàn timeout).
     - **Fig. 5**: `rq3_20261024/fig_rq3_probes_vs_churn.png` (RQ3 - Số bước dò theo chu kỳ churn ở tải $\alpha=0.50$).
     - **Fig. 6**: `rq3_20261024/fig_rq3_lookup_latency.png` (RQ3 - Độ trễ tra cứu theo churn ở tải $\alpha=0.50$).
     - **Fig. 7**: `rq3_20261024/fig_rq3_mechanism_ablation.png` (RQ3 - Biểu đồ cột bóc tách cơ chế ở tải $\alpha=0.50$).
     - **Fig. 8**: `rq3_20261025/fig_rq3_probes_vs_churn.png` (RQ3 - Số bước dò ở chế độ chịu tải cao $\alpha=0.80$).
     - **Fig. 9**: `rq3_20261026/fig_rq3_rebuild_spikes.png` (RQ3 - Diễn tiến độ trễ khối $10^4$ thao tác và các đột biến do rebuild).
     - **Fig. 10**: `rq3_20261026/fig_rq3_mechanism_breakdown.png` (RQ3 - Bóc tách định lượng 3 cơ chế Robin Hood).

---

## Danh Sách Các Biểu Đồ RQ1 (Nhiệm vụ Cặp 1 - C1)

Bộ 5 biểu đồ cốt lõi cho Câu hỏi Nghiên cứu 1 (Separate Chaining vs. Linear Probing):

### 1. `fig1_probes_vs_alpha.png`
- **Mục đích**: So sánh số lần dò trung bình (Hit và Miss) giữa `sc` và `lp-tomb` trên thang đo logarit đối chiếu với đường cận lý thuyết Knuth Volume 3 khi $\alpha \in \{0.50, 0.75, 0.90, 0.95\}$ tại $M = 2^{20}$.
- **Phát hiện chính**:
  - Tra cứu thành công (Hit): LP tăng từ $1.50$ lên $10.85$ bước dò; SC tăng từ $1.25$ lên $1.47$ bước dò.
  - Tra cứu thất bại (Miss): LP bùng nổ do cụm sơ cấp (primary clustering) từ $2.51$ lên $206.12$ bước dò; SC giữ vững ở mức $0.95$ bước dò. Khớp chính xác với lý thuyết Knuth ($< 2.8\%$ sai lệch).

### 2. `fig2_time_vs_alpha.png`
- **Mục đích**: Khảo sát thời gian thực thi tra cứu (ns / thao tác) theo hệ số tải $\alpha$ tại $M = 2^{20}$.
- **Phát hiện chính**:
  - Tại tải trung bình ($\alpha \le 0.75$), Linear Probing đạt tốc độ cao hơn Separate Chaining ($36.9$ ns/op vs $54.1$ ns/op tại $\alpha=0.50$) nhờ mảng nguyên thủy liên tục kích hoạt cơ chế nạp trước CPU cache line (hardware prefetching).
  - Tại tải rất cao ($\alpha \ge 0.90$), Linear Probing suy giảm mạnh do chuỗi dò dài ($84.1$ ns/op tại $\alpha=0.95$), trong khi Separate Chaining duy trì ổn định ($50.7$ ns/op).

### 3. `fig5_memory.png`
- **Mục đích**: So sánh mức tiêu hao bộ nhớ phụ (byte / khóa) trên heap JVM giữa cấp phát đối tượng `Node` của SC và mảng nguyên thủy phẳng của LP.
- **Phát hiện chính**:
  - Cấu trúc `Node` trong Java 64-bit tiêu tốn $32$ bytes/node (16B header + 8B key + 4B val + 4B next pointer) cộng thêm $4$ bytes/ô bucket, dẫn đến mức tiêu thụ thực tế gấp $3.2\times$ so với Linear Probing (chỉ tốn $13$ bytes/ô: 8B key + 4B val + 1B state).

---

## Danh Sách Các Biểu Đồ RQ2 (Nhiệm vụ Cặp 2 - C2)

Lệnh tái tạo toàn bộ biểu đồ RQ2:
```powershell
python bench\plot_figures.py --plot-rq2 --rq2-csv results\raw\rq2_results_1m.csv --outdir results\figures
```

### 1. `fig_rq2_tomb_empty_ratio.png`
- **Mục đích**: Khảo sát diễn tiến phân bổ trạng thái ô nhớ (LIVE vs TOMB vs EMPTY) và sự suy kiệt ô rỗng qua 6 mốc churn $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$ tại $M = 2^{20} = 1,048,576, \alpha = 0.50$.
- **Phát hiện chính**:
  - Tỷ lệ tombstone tăng phi mã từ $0.00\%$ lên $49.96\%$ ($523,876$ ô), bão hòa $99.92\%$ khoảng trống khả dụng ngoài khóa sống.
  - Số ô EMPTY suy giảm theo hàm mũ, từ $524,288$ ô ($50.00\%$) rơi tự do xuống chỉ còn **$412$ ô ($0.04\%$)** tại $C=4M$.
- Các mốc trung gian dùng cùng seed `42`: TOMB = 197,035; 316,211; 439,432; 510,008; EMPTY = 327,253; 208,077; 84,856; 14,280.
- **Nguồn dữ liệu**: `results/raw/rq2_results_1m.csv` (trạng thái được tái lập từ workload `RQ2Workload`, vì CSV benchmark không có cột TOMB/EMPTY).

### 2. `fig_rq2_probes_vs_clean.png`
- **Mục đích**: So sánh số bước dò trung bình (Hit và Miss) giữa bảng tích tụ tombstone (`base`) và bảng đối chứng sạch (`clean_table`).
- **Phát hiện chính**:
  - Tra cứu thành công (Hit): Bảng base tăng từ $1.500$ lên $2.346$ bước dò; Clean Table ở $C=4M$ đạt $1.502$.
  - Tra cứu thất bại (Miss): Bảng base tăng từ $2.503$ lên **$2,848.989$ bước dò**; Clean Table ở $C=4M$ đạt $2.496$ (chênh lệch **$1,141.51\times$**).
- Mỗi điểm trung bình được tính từ 1,000,000 truy vấn; các giá trị Clean Table có thể thay đổi nhẹ giữa mốc do cách sắp xếp khóa.
- **Nguồn dữ liệu**: `results/raw/rq2_results_1m.csv`

### 3. `fig_rq2_lookup_latency.png`
- **Mục đích**: Phân tích độ trễ tra cứu thực tế trên 1,000,000 truy vấn mỗi loại và xác thực mốc an toàn timeout.
- **Phát hiện chính**:
  - Độ trễ tra cứu miss tăng từ $48.05$ ms lên **$18,223.54$ ms**; Clean Table tại $C=4M$ mất $89.39$ ms (base/clean = $203.87\times$).
  - Mỗi điểm đo dùng 1,000,000 truy vấn. Timeout 60 giây giới hạn toàn JVM; lần chạy chậm nhất mất 46.044 giây tính cả khởi tạo và churn, còn 13.956 giây trước timeout.
- **Nguồn dữ liệu**: `results/raw/rq2_results_1m.csv`

### 4. `fig_rq2_tombstone_dynamics.png`
- **Mục đích**: Biểu đồ 3 panel hợp nhất (khổ 2 cột IEEE) phục vụ trực tiếp Figure 1 của bài báo khoa học.
- **Nguồn dữ liệu**: probes và latency lấy từ `results/raw/rq2_results_1m.csv`; chuỗi TOMB/EMPTY được tái lập với seed `42` từ `RQ2Workload` (CSV benchmark không lưu hai bộ đếm này).

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

Để giữ nguyên các hình lịch sử bên trên, kết quả đo tại $M=2^{20}, \alpha=0.50$ được vẽ
thành bộ hình riêng trong `results/figures/rq3_20261024/`. Bộ hình mở rộng
bao gồm `lp-tomb` ($\tau=0.2$), `lp-backshift`, `rh`, và `rh-noearly`, tại các
mốc $C \in \{0, M/4, M/2, M, 2M, 4M\}$; nhãn dung lượng bảng được lấy trực tiếp
từ CSV đầu vào. Hình probes dùng trung bình ba seed; hình latency dùng median
ba seed tại từng checkpoint để giảm ảnh hưởng của một mẫu thời gian ngoại lệ.

Sau khi hoàn tất dữ liệu, tái tạo hình bằng:

```powershell
python bench\plot_figures.py --csv results\raw\rq3_20261024.csv --outdir results\figures\rq3_20261024
```

---

## Campaign tải sống cao ngày 25/10/2026 ($\alpha = 0.80$)

Chiến dịch đo đạc ngày 25/10 tập trung vào chế độ chịu tải cao (stress test) tại $\alpha = 0.80$ ($N = 838861, M = 2^{20} = 1048576$), ngưỡng rebuild $\tau = 0.10$ cho `lp-tomb` theo đúng đặc tả của Đề tài 2.

Toàn bộ biểu đồ độ phân giải cao được sinh vào thư mục `results/figures/rq3_20261025/`:
- `fig_rq3_probes_vs_churn.png`: Đồ thị diễn tiến số bước dò trung bình (Hit và Miss) qua 6 mốc churn $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$. Kết quả cho thấy `lp-tomb` có số bước dò tra cứu trượt tăng vọt lên $>20$ bước dò do cụm tombstone dày đặc dù rebuild tại $\tau=0.10$. Ngược lại, `rh` duy trì số bước dò miss cực kỳ thấp ($\approx 3.40$ bước dò).
- `fig_rq3_lookup_latency.png`: Độ trễ tra cứu trung vị (ms) qua các mốc churn.
- `fig_rq3_mechanism_ablation.png`: Biểu đồ cột bóc tách 3 cơ chế tại tải sống cao $\alpha = 0.80$. Cơ chế Early Exit giúp `rh` giảm tới **73.8% số bước dò miss** so với `lp-backshift` / `rh-noearly` (từ $12.98$ xuống $3.40$), và giảm tới **83.7%** so với `lp-tomb` ($20.84$).

Lệnh tái tạo biểu đồ ngày 25/10:

```powershell
python bench\plot_figures.py --csv results\raw\rq3_20261025.csv --outdir results\figures\rq3_20261025
```

---

## Campaign phân tích chuyên sâu ngày 26/10/2026 (Khối $10^4$ thao tác & Bóc tách cơ chế)

Phục vụ nhiệm vụ phân tích của Cặp 3 (C3) theo kế hoạch ngày 26/10, bộ hình ảnh tại thư mục `results/figures/rq3_20261026/` trực quan hóa các dữ liệu đo đạc chi tiết từng khối $10^4$ thao tác:
- `fig_rq3_rebuild_spikes.png`: Diễn tiến độ trễ từng khối $10^4$ thao tác (Block Timeline) xuyên suốt $C = 4M$ chu kỳ churn ($8.39 \times 10^6$ thao tác). Biểu đồ làm nổi bật các đột biến độ trễ (latency spikes) lên tới $29.6 - 36.1$ ms do kích hoạt `rebuild()` trong `lp-tomb` ($14$ lần ở $\alpha=0.50$ và $23$ lần ở $\alpha=0.80$), so với đường độ trễ phẳng lì ổn định $\approx 1.8 - 2.7$ ms của `rh` và `lp-backshift`.
- `fig_rq3_max_dib_comparison.png`: Diễn tiến Max DIB (khoảng cách dò lớn nhất trong bảng) giữa `lp-backshift` và `rh`. Thể hiện rõ việc hoán đổi theo DIB (Robin Hood swap) san phẳng cụm khóa, chặn Max DIB không vượt quá $9$ (ở $\alpha=0.50$) và $25$ (ở $\alpha=0.80$), trong khi `lp-backshift` để Max DIB tăng vọt lên tới $41$ và $195$.
- `fig_rq3_mechanism_breakdown.png`: Biểu đồ cột 3 bảng tổng hợp phân tích định lượng 3 cơ chế:
  1. Loại bỏ tombstone: Triệt tiêu $100\%$ các lần dừng hệ thống do rebuild ($0$ lần vs $14$ và $23$ lần trong `lp-tomb`).
  2. Hoán đổi DIB: Giảm $78.0\%$ đến $87.2\%$ Max DIB, triệt tiêu phương sai chuỗi dò và kiểm soát đuôi độ trễ (tail latency).
  3. Dừng sớm Early Exit: Cắt giảm $30.0\%$ số bước dò tra cứu trượt ở $\alpha=0.50$ và cắt giảm tới **$73.8\%$** ở $\alpha=0.80$.

Lệnh tái tạo bộ biểu đồ ngày 26/10:

```powershell
python bench\plot_figures.py --plot-oct26
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
