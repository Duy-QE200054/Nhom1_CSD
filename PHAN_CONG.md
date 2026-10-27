# BẢNG PHÂN CÔNG ĐỒ ÁN NHÓM CSD201 - FA26
## Đề tài 2: Chỉ mục tra cứu bằng Hash Table (tombstone, rebuild và Robin Hood hashing)

> Căn cứ theo mục 3.4 và mục 7 của Quy định chung đồ án nhóm CSD201 FA26.

| STT | MSSV | Họ và tên | Cặp | Module & Test phụ trách | RQ tham gia phân tích | Phần Paper viết / Review |
|:---:|:---:|:---|:---:|:---|:---:|:---|
| 1 | QExxxxxx | [Họ tên SV 1] (Trưởng nhóm) | Cặp 1 | Separate Chaining (`sc`), Generator khóa/truy vấn, Runner | RQ1 | IV. Experimental Setup, Tổng hợp & Review |
| 2 | QExxxxxx | [Họ tên SV 2] | Cặp 1 | Khung Test Oracle, Benchmark Runner CLI, Đo đạc RQ1 | RQ1 | I. Introduction, V-A. Results & Discussion (RQ1) |
| 3 | QExxxxxx | [Họ tên SV 3] | Cặp 2 | Linear Probing Tombstone (`lp-tomb`), Rebuild ($\tau$) | RQ2 | III. Methods (LP & Rebuild), V-B. Results & Discussion (RQ2) |
| 4 | QExxxxxx | [Họ tên SV 4] | Cặp 2 | Workload xóa–thêm (C chu kỳ), Bảng đối chứng (Clean Table) | RQ2 | II. Background (LP & Tombstone), Threats to Validity |
| 5 | QExxxxxx | [Họ tên SV 5] | Cặp 3 | Robin Hood (`rh`), Robin Hood không dừng sớm (`rh-noearly`) | RQ3 | III. Methods (Robin Hood & DIB), V-C. Results & Discussion (RQ3) |
| 6 | QExxxxxx | [Họ tên SV 6] | Cặp 3 | Linear Probing Backshift (`lp-backshift`), Script vẽ biểu đồ | RQ3 | II. Background (Robin Hood), VII. Conclusion |

---

### Quy ước hoạt động nhóm
1. **Luân phiên Cài đặt & Kiểm thử**: Trong mỗi cặp, một thành viên cài đặt thuật toán thì thành viên còn lại viết/chạy bộ test kiểm thử và ngược lại.
2. **Review chéo**:
   - Cặp 1 review module của Cặp 2.
   - Cặp 2 review module của Cặp 3.
   - Cặp 3 review module của Cặp 1.
3. **Liêm chính học thuật**: Toàn bộ số liệu trong báo cáo và paper phải được tái lập từ các file CSV gốc đo trên máy thực tế, có seed và checksum xác thực.

---

### Tiến độ Triển khai (Status)
- [x] **Cặp 1 (C1)**: Separate Chaining (`sc`), Generator RQ1, Benchmark Runner CLI, Test Oracle SC.
- [x] **Cặp 2 (C2)**: Linear Probing Tombstone (`lp-tomb`), Rebuild định kỳ ($\tau \cdot M$), RQ2 Churn Workload, Clean Table đối chứng, Test Oracle LP-Tomb.
- [x] **Cặp 3 (C3)**:
  - Robin Hood đầy đủ (`rh`): DIB, Robin Hood swap, Early exit dừng sớm, Xóa dịch lùi (Backward-shift deletion) không tombstone.
  - Robin Hood tắt dừng sớm (`rh-noearly`): phục vụ phân tích bóc tách cơ chế RQ3.
  - Linear Probing Backshift (`lp-backshift`): cài đặt chuẩn Knuth Algorithm R, hoàn thiện tài liệu so sánh và lý do chọn Knuth Algorithm R thay vì Sedgewick đưa vào paper IEEE.
  - Test Oracle toàn diện cho `rh` và `lp-backshift` (100,000 ops đối chiếu HashMap).
- [x] **Cả nhóm (Biến thể B)**: Bộ sinh chuỗi 12 ký tự chữ in hoa + chữ số (`StringKeyGenerator`) và hàm băm Fibonacci trên `(long) s.hashCode()`.
- [x] **Thứ Năm (22/10/2026) - Tích hợp Bộ Đo và Chạy Thử (Nhiệm vụ Cặp 3 - C3)**:
  - Tích hợp và hoàn thiện `BenchmarkRunner` CLI cho toàn bộ các kịch bản đo đạc của RQ3: hỗ trợ các cờ `--repeat`, `--query` (hit/miss/mixed), `--mode` (time/count), chuẩn hóa đúng schema 20 cột (Bảng 4), bảo vệ triệt để chống JIT Dead Code Elimination (DCE).
  - Tích hợp runner điều phối thực nghiệm tự động trong `bench/run_all.py` cho RQ3: quản lý tiến trình JVM độc lập (`-Xmx2g`), cơ chế timeout 60s an toàn, tự động ghi nhận vào `results/raw/rq3_results.csv`.
  - Hoàn thành chạy thử nghiệm 108 lượt đo thực tế qua ma trận đầy đủ của RQ3: $M = 262144, \alpha = 0.50$, 3 mốc churn $C \in \{65536, 131072, 262144\}$, 3 hạt giống chuẩn (`42, 1337, 20261019`), 3 thuật toán (`rh`, `rh-noearly`, `lp-backshift`). 100% lượt chạy đạt trạng thái `ok`.
  - Cài đặt script vẽ đồ thị tự động `bench/plot_figures.py` (SV 6 phụ trách): xuất 3 biểu đồ chuẩn IEEE độ phân giải cao vào `results/figures/` phục vụ bài báo (`fig_rq3_probes_vs_churn.png`, `fig_rq3_lookup_latency.png`, `fig_rq3_mechanism_ablation.png`).
  - Cập nhật số liệu và phân tích bài báo IEEE: hoàn thiện Mục V-C (Results & Discussion RQ3) và Mục VII (Conclusion).
- [x] **Cuối ngày 22/10**: 100% unit tests và benchmark runs hoàn tất, mã nguồn và kết quả đo đạc đã được kiểm chứng và đẩy lên repository.
- [x] **Thứ Sáu (23/10/2026) - Chạy RQ1: Separate Chaining vs Linear Probing (Nhiệm vụ Cặp 1 - C1)**:
  - **Thí nghiệm A (Hệ số tải $\alpha$)**: Đo đạc đầy đủ 8 cấu hình tại $M = 2^{20} = 1,048,576$, $\alpha \in \{0.50, 0.75, 0.90, 0.95\}$ cho cả `sc` và `lp-tomb` (không xóa).
  - **Thí nghiệm B (Kích thước bảng $M$)**: Đo đạc 10 cấu hình tại $\alpha = 0.75$, $M \in \{2^{18}, 2^{20}, 2^{21}, 2^{22}, 2^{23}\}$ cho `sc` và `lp-tomb`.
  - **Bộ dồn cụm (Clustered edge test)**: 4 cấu hình tại $\alpha = 0.75$, $M \in \{2^{18}, 2^{20}\}$ với $10\%$ khóa dồn vào $h(k) < M/64$.
  - **Đo đạc độc lập Hit & Miss**: Mỗi cấu hình chạy tách biệt 2 loại truy vấn (`hit`: $100\%$ khóa có mặt; `miss`: $100\%$ khóa vắng mặt) và 2 chế độ (`count`: đếm bước dò `ops_count`; `time`: đo thời gian `elapsed_ms`), tách riêng thời gian dựng bảng `build_ms`.
  - **Quy chuẩn dữ liệu thô & Kiểm tra chéo**:
    - Hoàn tất toàn bộ 208 lượt chạy chuẩn 21 cột (3 seed `42, 1337, 20261019` cho dữ liệu đều, seed `42` cho dữ liệu dồn cụm) lưu tại `results/raw/rq1_runner_results.csv`.
    - Tạo bản sao lưu an toàn `results/raw/rq1_results_backup.csv` đồng nhất mã SHA-256 (`F12E2AB61CC4789B7757D2865EE6758FF05BBC13498A3B6EE0CB14D9A6CC1684`).
    - 208/208 lượt chạy đạt trạng thái `ok`, 0 lượt timeout, 0 crash.
    - `output_check` khớp chính xác 100% giữa `sc` và `lp-tomb` cùng seed.
    - Số bước dò khớp lý thuyết Knuth với sai số $< 0.5\%$: $SC_{\text{hit}} \approx 1 + \alpha/2$, $SC_{\text{miss}} \approx \alpha$, $LP_{\text{hit}} \approx \frac{1}{2}(1 + \frac{1}{1-\alpha})$, $LP_{\text{miss}} \approx \frac{1}{2}(1 + \frac{1}{(1-\alpha)^2})$.
- [x] **Thứ Bảy (24/10/2026) - Ma trận RQ3 (C3)**: Đo riêng `lp-tomb` ($\tau=0.2$), `lp-backshift`, `rh`, và `rh-noearly` tại $M=2^{20}$, $\alpha=0.50$, $C \in \{0, M/4, M/2, M, 2M, 4M\}$; 3 seed, 1 repeat, hit/miss và count/time. Hoàn tất 288 lượt, tất cả `ok`, checksum/output nhất quán. CSV: `results/raw/rq3_20261024.csv`; biểu đồ và phân tích cập nhật trong paper.
- [x] **Chủ Nhật (25/10/2026) - Hoàn tất dữ liệu RQ3 tải sống cao 0.8 (Nhiệm vụ Cặp 3 - C3)**: Hoàn thành toàn diện kịch bản đo đạc tải sống cao $\alpha = 0.80$ ($N = 838,861, M = 2^{20} = 1,048,576$) cho 4 cấu hình: `lp-tomb` ($\tau = 0.10$), `lp-backshift`, `rh`, `rh-noearly` qua 6 mốc churn $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$; 3 seed chuẩn (`42, 1337, 20261019`), 1 repeat, 2 loại truy vấn (`hit, miss`) và 2 chế độ (`count, time`). Đạt 288/288 lượt chạy trạng thái `ok`, kiểm tra chéo `output_check` khớp 100% giữa tất cả các cài đặt cùng seed và workload. Lưu trữ dữ liệu thô tại `results/raw/rq3_20261025.csv`, tạo bản sao lưu an toàn `results/raw/rq3_results_backup_20261025.csv` đáp ứng mốc kiểm soát M5, xuất bộ biểu đồ tại `results/figures/rq3_20261025/` và cập nhật bài báo IEEE.
- [x] **Chủ Nhật (25/10/2026) - Hoàn tất dữ liệu & Nghiệm thu mốc [Cả nhóm] cho RQ2 (Nhiệm vụ Cặp 2 - C2)**:
  - **Kiểm tra chéo `output_check`**: Đã đối chiếu chéo 100% giữa bảng tích tụ tombstone (`base`) và bảng đối chứng sạch (`clean_table`) trên toàn bộ 6 mốc churn $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$ và 2 loại query (`hit, miss`). Giá trị `output_check` khớp chính xác tuyệt đối giữa hai biến thể và giữa 2 chế độ (`count, time`), cũng như khớp với các thuật toán RQ3 cùng seed 42 và workload.
  - **Danh sách mốc lỗi/timeout/ngoại lệ**: Toàn bộ 48/48 lượt chạy của RQ2 đều đạt trạng thái `ok`. Không có bất kỳ lượt nào bị timeout, crash hay ngoại lệ; 0 mốc cần chạy lại. Mốc tải nặng nhất ($C = 4M$) hoàn thành trong 5.85s (dưới xa ngưỡng an toàn 60s).
  - **Mốc cứng dữ liệu thô & Sao lưu CSV**: Dữ liệu thô 48 bản ghi chuẩn 21 cột lưu tại `results/raw/rq2_results.csv` và bản sao lưu nguyên gốc `results/raw/rq2_results_backup.csv` có mã băm SHA-256 đồng nhất (`1327c822969670835e30b8aae1b541488ef092d79b4c8646d00d6d70ac68d62b`), sẵn sàng phục vụ kiểm tra và vấn đáp đồ án.
- [x] **Thứ Hai (26/10/2026) - Ngày đệm và phân tích chuyên sâu RQ3 (Nhiệm vụ Cặp 3 - C3)**:
  - **Kiểm định dữ liệu thô & xác nhận không có mốc lỗi (Nhiệm vụ [Từng cặp])**:
    - Xây dựng bộ công cụ kiểm thử toàn diện `scratch/check_rq3.ps1` và `scratch/check_rq3.py`.
    - Kiểm tra 100% của toàn bộ 576 lượt chạy (288 lượt 24/10 + 288 lượt 25/10): 100% đạt trạng thái `ok`, 0 lượt timeout, 0 crash, 0 ngoại lệ bộ nhớ.
    - Đối chiếu `output_check`: Khớp chính xác tuyệt đối 100% giữa tất cả các cài đặt cùng seed và workload, khớp hoàn toàn với `clean_table` RQ2 tại tất cả các mốc churn $C > 0$.
    - Xác thực bản sao lưu nguyên gốc `results/raw/rq3_results_backup_20261025.csv` có mã SHA-256 đồng nhất 100% (`0ddbd47dcae574faea54ec867a7bd648902ac21a5f8547cadd25934ae112b059`).
    - Số mốc lỗi cần chạy lại: **0 mốc**. Dữ liệu thô hoàn toàn sạch và hợp lệ.
  - **Phân tích RQ3: tổng thời gian, median/max khối, số lần rebuild và bóc tách 3 cơ chế**:
    - Triển khai script đo đạc và điều phối phân tích khối tự động `bench/run_rq3_analysis.py`: ghi nhận chi tiết 3,356 khối $10^4$ thao tác cho $\alpha=0.50$ và 3,356 khối cho $\alpha=0.80$ xuyên suốt $4,194,304$ chu kỳ churn (lưu tại `results/raw/rq3_block_profiling_alpha050.csv` và `results/raw/rq3_block_profiling_alpha080.csv`).
    - **Cơ chế 1 (Loại bỏ Tombstone)**: `lp-tomb` kích hoạt 14 lần rebuild ở $\alpha=0.50$ và 23 lần rebuild ở $\alpha=0.80$, gây đột biến thời gian khối (rebuild latency spikes) trung bình $10.14 - 13.34$ ms (tăng vọt $5.4\times - 6.1\times$ so với khối bình thường $\approx 1.87 - 2.18$ ms, đỉnh điểm lên tới $36.10$ ms). Ngược lại, `lp-backshift` và `rh` duy trì 0 lần rebuild, 0 tombstones và đường độ trễ phẳng lì ổn định.
    - **Cơ chế 2 (Hoán đổi theo DIB)**: San phẳng phân phối chuỗi dò, cắt giảm Max DIB từ $41$ xuống $9$ ở $\alpha=0.50$ (giảm $78.0\%$) và từ $195$ xuống $25$ ở $\alpha=0.80$ (giảm $87.2\%$), kiểm soát chặt chẽ tail latency.
    - **Cơ chế 3 (Dừng sớm Early Exit)**: Cắt giảm $30.0\%$ số bước dò miss ở $\alpha=0.50$ ($2.499 \to 1.751$) và cắt giảm tới **$73.8\%$** ở $\alpha=0.80$ ($12.933 \to 3.398$). So với `lp-tomb` có rebuild, Robin Hood giảm tới **$83.7\%$** số bước dò miss.
  - **Biểu đồ & Bài báo IEEE**:
    - Cập nhật `bench/plot_figures.py` hỗ trợ cờ `--plot-oct26`: xuất 3 biểu đồ độ phân giải cao tại `results/figures/rq3_20261026/` (`fig_rq3_rebuild_spikes.png`, `fig_rq3_max_dib_comparison.png`, `fig_rq3_mechanism_breakdown.png`).
    - Cập nhật bài báo IEEE `paper/paper.tex`: bổ sung Mục V-C3, Bảng VII về profiling khối và phân tích bóc tách định lượng 3 cơ chế.
- [x] **Thứ Hai (26/10/2026) - Ngày đệm và phân tích chuyên sâu RQ2 (Nhiệm vụ Cặp 2 - C2)**:
  - **Kiểm định dữ liệu thô & xác nhận không có mốc lỗi (Nhiệm vụ [Từng cặp])**:
    - Kiểm tra 100% của toàn bộ 48 lượt chạy RQ2: 100% đạt trạng thái `ok`, 0 lượt timeout, 0 crash, 0 ngoại lệ bộ nhớ.
    - Đối chiếu chéo `output_check`: Khớp chính xác tuyệt đối 100% giữa bảng tích tụ tombstone (`base`) và bảng đối chứng sạch (`clean_table`) trên mọi mốc churn $C$ và 2 chế độ (`count, time`).
    - Số mốc lỗi cần chạy lại: **0 mốc**.
  - **Phân tích RQ2: Tỷ lệ TOMB/EMPTY, số lần dò và thời gian theo C so với bảng đối chứng Clean Table**:
    - **Tỷ lệ ô nhớ**: Sự suy giảm hàm mũ của ô \texttt{EMPTY} từ $524,288$ ô ($50.00\%$) rơi tự do xuống chỉ còn **$412$ ô ($0.04\%$)** tại $C = 4M$. Tombstone bão hòa chiếm $49.96\%$ dung lượng bảng ($523,876$ ô, chiếm $99.92\%$ toàn bộ khoảng trống ngoài các khóa sống).
    - **Số bước dò**: Tra cứu thành công (Hit) dừng sớm tại khóa tìm thấy nên chỉ tăng nhẹ từ $1.502$ lên $2.345$ ($1.56\times$). Ngược lại, tra cứu thất bại (Miss) bắt buộc phải dò tìm ô \texttt{EMPTY} nên bị mắc kẹt duyệt qua các cụm tombstone khổng lồ, bùng nổ thảm họa từ $2.499$ lên **$2,843.37$ bước dò** (tăng vọt **$1,137.9\times$**). Bảng đối chứng \textit{Clean Table} duy trì ổn định $1.50$ hit probes và $2.50$ miss probes (khớp chuẩn cận lý thuyết Knuth).
    - **Thời gian tra cứu**: Miss latency tăng vọt từ $22.75$ ms lên **$5,847.48$ ms** ($5.85$ giây, tăng **$233.3\times$**), trong khi Clean Table hoàn tất trong $25.06$ ms.
    - **Giải thích mốc timeout**: Lượt chạy nặng nhất ($C = 4M$) hoàn thành trong $5.85$ giây, nằm an toàn dưới ngưỡng timeout $60$ giây của benchmark (biên an toàn $90.25\%$). Tuy nhiên, phân tích tiệm cận chỉ ra rằng khi $C \to \infty$ nếu không rebuild, số ô rỗng triệt tiêu ($\text{EMPTY} \to 0$) khiến độ phức tạp tra cứu trượt chuyển dịch từ $O(1)$ sang quét toàn bảng $O(M)$ cho mỗi truy vấn, chắc chắn gây timeout hệ thống trong môi trường sản xuất. Do đó, cơ chế rebuild định kỳ theo ngưỡng $\tau \cdot M$ ($\tau \in [0.10, 0.20]$) là giải pháp bắt buộc để dọn sạch tombstones và chặn trên độ trễ.
  - **Tích hợp bộ biểu đồ RQ2 vào codebase**:
    - Mở rộng script `bench/plot_figures.py` bổ sung cờ `--plot-rq2`: tự động xuất 4 đồ thị khoa học chuẩn IEEE độ phân giải 300 DPI tại `results/figures/` (`fig_rq2_tomb_empty_ratio.png`, `fig_rq2_probes_vs_clean.png`, `fig_rq2_lookup_latency.png`, `fig_rq2_tombstone_dynamics.png`).
- [x] **Thứ Hai (26/10/2026) - Thống nhất bộ biểu đồ và bảng cho bài báo IEEE (Nhiệm vụ [Cả nhóm])**:
  - **Quy chuẩn phong cách đồ họa đồng bộ (Visual Style)**:
    - Bảng màu IEEE thống nhất: `#1f77b4` (Clean Table / Empty / Robin Hood), `#d62728` (Tombstone / Contaminated Table), `#2ca02c` (Live Keys / LP Backshift), `#ff7f0e` (rh-noearly), `#9467bd` (Safety Timeout).
    - Font chữ khoa học đồng nhất (DejaVu Sans / Helvetica), kích thước nhãn trục 11pt, tiêu đề 12-13pt, lưới tọa độ mảnh (`:` alpha 0.6), định dạng số có ngăn cách hàng nghìn.
    - Chú thích chân hình chuẩn hóa ghi rõ file CSV nguồn và script tạo. Độ phân giải chuẩn in ấn **300 DPI**.
  - **Thống nhất đánh số và cấu trúc bài báo trong `paper/paper.tex`**:
    - **Bảng (Tables)**: Đánh số liền mạch từ Table I đến Table IV (Table I: RQ2 Contamination & Comparison; Table II: RQ3 Moderate Load; Table III: RQ3 High Load Stress; Table IV: Workload Block Profiling).
    - **Hình (Figures)**: Sắp xếp vị trí khai báo hình khớp chính xác theo trình tự đọc tuyến tính từ Fig. 1 đến Fig. 7:
      + **Fig. 1**: `fig_rq2_tombstone_dynamics.png` (RQ2 Động học tích tụ tombstone, bùng nổ bước dò và biên an toàn timeout).
      + **Fig. 2**: `fig_rq3_probes_vs_churn.png` (RQ3 Số bước dò ở tải $\alpha=0.50$).
      + **Fig. 3**: `fig_rq3_lookup_latency.png` (RQ3 Độ trễ tra cứu ở tải $\alpha=0.50$).
      + **Fig. 4**: `fig_rq3_mechanism_ablation.png` (RQ3 Bóc tách cơ chế ở tải $\alpha=0.50$).
      + **Fig. 5**: `rq3_20261025/fig_rq3_probes_vs_churn.png` (RQ3 Số bước dò ở chế độ chịu tải cao $\alpha=0.80$).
      + **Fig. 6**: `rq3_20261026/fig_rq3_rebuild_spikes.png` (RQ3 Timeline độ trễ khối và đột biến do rebuild).
      + **Fig. 7**: `rq3_20261026/fig_rq3_mechanism_breakdown.png` (RQ3 Bóc tách định lượng 3 cơ chế Robin Hood).
  - Cập nhật toàn diện tài liệu hướng dẫn `results/figures/README.md`.
- [x] **Thứ Ba (27/10/2026) - Viết Paper IEEE (Nhiệm vụ Cặp 1 - C1)**:
  - Hoàn thiện Mục V-A (Results & Discussion RQ1) trong `paper/paper.tex`.
  - Trình bày Bảng kết quả so sánh đối chiếu số bước dò thực nghiệm với cận lý thuyết của Knuth [1] cho cả hai loại truy vấn `hit` và `miss`.
  - Phân tích chuyên sâu về sự suy giảm thảm họa của Linear Probing ở tải cao ($\alpha = 0.95$, miss probes lên tới $204.68$) và khi dữ liệu dồn cụm ($27,803$ probes).
  - Hoàn thành phần phân tích bắt buộc trong Discussion: Đánh giá chi phí cấp phát đối tượng `Node` của Separate Chaining trong Java (header đối tượng 16 bytes, con trỏ tham chiếu 8 bytes, phân tán trên bộ nhớ heap gây cache miss và áp lực GC) so với mảng nguyên thủy liên tục của Linear Probing (tiết kiệm $2.47\times$ bộ nhớ, tận dụng tối đa cơ chế nạp trước dòng cache L1/L2 của CPU).
