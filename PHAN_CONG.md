# BẢNG PHÂN CÔNG ĐỒ ÁN NHÓM CSD201 - FA26
## Đề tài 2: Chỉ mục tra cứu bằng Hash Table (tombstone, rebuild và Robin Hood hashing)

> Căn cứ theo mục 3.4 và mục 7 của Quy định chung đồ án nhóm CSD201 FA26.

| STT | MSSV | Họ và tên | Cặp | Module & Test phụ trách | RQ tham gia phân tích | Phần Paper viết / Review |
|:---:|:---:|:---|:---:|:---|:---:|:---|
| 1 | QExxxxxx | [Họ tên SV 1] (Trưởng nhóm) | Cặp 1 | Separate Chaining (`sc`), Generator khóa/truy vấn, Runner | RQ1 | IV. Experimental Setup, Tổng hợp & Review |
| 2 | QE200022 | [Đinh Đỗ Gia Huy] | Cặp 1 | Khung Test Oracle, Benchmark Runner CLI, Đo đạc RQ1 | RQ1 | I. Introduction, V-A. Results & Discussion (RQ1) |
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
- [x] **Cả nhóm (Biến thể A)**: Chốt chuẩn hóa toàn bộ đề tài theo Biến thể A: khóa nguyên thủy `long` 64-bit và hàm băm Fibonacci chuẩn $h(k) = (k \cdot \phi) \ggg (64 - p)$.
  - Tích hợp và hoàn thiện `BenchmarkRunner` CLI cho toàn bộ các kịch bản đo đạc của RQ3: hỗ trợ các cờ `--repeat`, `--query` (hit/miss/mixed), `--mode` (time/count), chuẩn hóa đúng schema 21 cột (Bảng 4, có `build_ms`), bảo vệ triệt để chống JIT Dead Code Elimination (DCE).
  - Tích hợp runner điều phối thực nghiệm tự động trong `bench/run_all.py` cho RQ3: quản lý tiến trình JVM độc lập (`-Xmx2g`), cơ chế timeout 60s an toàn, tự động ghi nhận vào `results/raw/rq3_results.csv`.
  - Hoàn thành chạy thử nghiệm 108 lượt đo thực tế qua ma trận đầy đủ của RQ3: $M = 262144, \alpha = 0.50$, 3 mốc churn $C \in \{65536, 131072, 262144\}$, 3 hạt giống chuẩn (`42, 1337, 20261019`), 3 thuật toán (`rh`, `rh-noearly`, `lp-backshift`). 100% lượt chạy đạt trạng thái `ok`.
  - Cài đặt script vẽ đồ thị tự động `bench/plot_figures.py` (SV 6 phụ trách): xuất 3 biểu đồ chuẩn IEEE độ phân giải cao vào `results/figures/` phục vụ bài báo (`fig_rq3_probes_vs_churn.png`, `fig_rq3_lookup_latency.png`, `fig_rq3_mechanism_ablation.png`).
  - Cập nhật số liệu và phân tích bài báo IEEE: hoàn thiện Mục V-C (Results & Discussion RQ3) và Mục VII (Conclusion).
- [x] **Cuối ngày 22/10**: 100% unit tests và benchmark runs hoàn tất, mã nguồn và kết quả đo đạc đã được kiểm chứng và đẩy lên repository.
- [x] **Thứ Sáu (23/10/2026) - Chạy toàn diện RQ1 (Nhiệm vụ Cặp 1 - C1)**:
  - Đo đạc đầy đủ cả lượt truy vấn có mặt (`hit`) và vắng mặt (`miss`) riêng biệt cho Thí nghiệm A ($\alpha \in \{0.50, 0.75, 0.90, 0.95\}$ tại $M=2^{20}$), Thí nghiệm B ($M \in [2^{18}, 2^{23}]$ tại $\alpha=0.75$) và Bộ dồn cụm ($10\%$ khóa có $h(k) < M/64$).
  - Tách bạch giữa lượt đo số bước dò (`--mode count`) và đo thời gian (`--mode time`). Lưu trữ đầy đủ 88 lượt chạy hợp lệ vào `results/raw/rq1_runner_results.csv`.
- [x] **Thứ Bảy (24/10/2026) - Phân tích RQ1 và Discussion chi phí Node (Nhiệm vụ Cặp 1 - C1)**:
  - Hoàn thiện module phân tích độc lập `HashTableProject/src/hashtable/bench/RQ1Analysis.java`: mô hình hóa chi phí đối tượng Java (`Node` overhead 32 bytes: 16B header + 8B key + 4B val + 4B pointer), đối chiếu lý thuyết Knuth, phân tích pointer chasing / cache miss L1/L2 và áp lực Minor GC so với mảng nguyên thủy phẳng của Linear Probing.
- [x] **Thứ Bảy (24/10/2026) - Ma trận RQ3 (C3)**: Đo riêng `lp-tomb` ($\tau=0.2$), `lp-backshift`, `rh`, và `rh-noearly` tại $M=2^{20}$, $\alpha=0.50$, $C \in \{0, M/4, M/2, M, 2M, 4M\}$; 3 seed, 1 repeat, hit/miss và count/time. Hoàn tất 288 lượt, tất cả `ok`, checksum/output nhất quán. CSV: `results/raw/rq3_20261024.csv`; biểu đồ và phân tích cập nhật trong paper.
- [x] **Chủ Nhật (25/10/2026) - Mốc cứng M5 Dữ liệu thô và Sao lưu RQ1 (Nhiệm vụ Cặp 1 & Cả nhóm)**:
  - Tạo bản sao lưu nguyên gốc an toàn `results/raw/rq1_results_backup.csv` đồng nhất với `rq1_runner_results.csv`.
  - Nghiệm thu 88 bản ghi chuẩn 21 cột, $100\%$ đạt trạng thái `ok`, sẵn sàng cho hội đồng chấm đồ án.
- [x] **Chủ Nhật (25/10/2026) - Hoàn tất dữ liệu RQ3 tải sống cao 0.8 (Nhiệm vụ Cặp 3 - C3)**: Hoàn thành toàn diện kịch bản đo đạc tải sống cao $\alpha = 0.80$ ($N = 838,861, M = 2^{20} = 1,048,576$) cho 4 cấu hình: `lp-tomb` ($\tau = 0.10$), `lp-backshift`, `rh`, `rh-noearly` qua 6 mốc churn $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$; 3 seed chuẩn (`42, 1337, 20261019`), 1 repeat, 2 loại truy vấn (`hit, miss`) và 2 chế độ (`count, time`). Đạt 288/288 lượt chạy trạng thái `ok`, kiểm tra chéo `output_check` khớp 100% giữa tất cả các cài đặt cùng seed và workload. Lưu trữ dữ liệu thô tại `results/raw/rq3_20261025.csv`, tạo bản sao lưu an toàn `results/raw/rq3_results_backup_20261025.csv` đáp ứng mốc kiểm soát M5, xuất bộ biểu đồ tại `results/figures/rq3_20261025/` và cập nhật bài báo IEEE.
- [x] **Chủ Nhật (25/10/2026) - Hoàn tất dữ liệu & Nghiệm thu mốc [Cả nhóm] cho RQ2 (Nhiệm vụ Cặp 2 - C2)**:
  - **Kiểm tra chéo `output_check`**: Đã đối chiếu chéo 100% giữa bảng tích tụ tombstone (`base`) và bảng đối chứng sạch (`clean_table`) trên toàn bộ 6 mốc churn $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$ và 2 loại query (`hit, miss`). Giá trị `output_check` khớp chính xác tuyệt đối giữa hai biến thể và giữa 2 chế độ (`count, time`), cũng như khớp với các thuật toán RQ3 cùng seed 42 và workload.
  - **Danh sách mốc lỗi/timeout/ngoại lệ**: Toàn bộ 48/48 lượt chạy lịch sử của RQ2 đều đạt trạng thái `ok`; mốc miss nặng nhất mất 5.85s đo riêng phần truy vấn. Thời gian này không phải wall time của toàn tiến trình benchmark.
  - **Lưu trữ campaign lịch sử**: Bản sao lưu của CSV 48 bản ghi với 524.288 truy vấn mỗi loại được giữ tại `results/raw/rq2_results_backup.csv` để tham khảo; campaign này không dùng làm nguồn kết quả RQ2 hiện tại.
- [x] **Thứ Hai (26/10/2026) - Ngày đệm và phân tích chuyên sâu RQ3 (Nhiệm vụ Cặp 3 - C3)**:
  - **Kiểm định dữ liệu thô & xác nhận không có mốc lỗi (Nhiệm vụ [Từng cặp])**:
    - Xây dựng bộ công cụ kiểm thử toàn diện `scratch/check_rq3.ps1` và `scratch/check_rq3.py`.
    - Kiểm tra 100% của toàn bộ 576 lượt chạy (288 lượt 24/10 + 288 lượt 25/10): 100% đạt trạng thái `ok`, 0 lượt timeout, 0 crash, 0 ngoại lệ bộ nhớ.
    - Đối chiếu `output_check`: Khớp chính xác tuyệt đối 100% giữa tất cả các cài đặt cùng seed và workload, khớp hoàn toàn với `clean_table` RQ2 tại tất cả các mốc churn $C > 0$.
    - Xác thực bản sao lưu nguyên gốc `results/raw/rq3_results_backup_20261025.csv` có mã SHA-256 đồng nhất 100% (`0ddbd47dcae574faea54ec867a7bd648902ac21a5f8547cadd25934ae112b059`).
    - Số mốc lỗi cần chạy lại: **0 mốc**. Dữ liệu thô hoàn toàn sạch và hợp lệ.
  - **Phân tích RQ3: tổng thời gian, median/max khối, số lần rebuild và bóc tách 3 cơ chế**:
    - Triển khai script đo đạc và điều phối phân tích khối tự động `bench/run_rq3_analysis.py`: ghi nhận chi tiết 3,356 khối $10^4$ thao tác cho $\alpha=0.50$ và 3,356 khối cho $\alpha=0.80$ xuyên suốt $4,194,304$ chu kỳ churn (lưu tại `results/raw/rq3_block_profiling_alpha050.csv` và `results/raw/rq3_block_profiling_alpha080.csv`).
    - **Cơ chế 1 (Loại bỏ Tombstone)**: `lp-tomb` kích hoạt 14 lần rebuild ở $\alpha=0.50$ và 23 lần rebuild ở $\alpha=0.80$, gây đột biến thời gian khối (rebuild latency spikes) trung bình $10.14 - 13.34$ ms (tăng vọt $5.4\times - 6.1\times$ so với khối bình thường $\approx 1.87 - 2.18$ ms, đỉnh điểm lên tới $36.10$ ms). Ngược lại, `lp-backshift` và `rh` duy trì 0 lần rebuild và 0 tombstone; thao tác xóa vẫn có thể tốn thời gian dịch chuyển cụm.
    - **Cơ chế 2 (Hoán đổi theo DIB)**: San phẳng phân phối độ dời, cắt giảm Max DIB từ $41$ xuống $9$ ở $\alpha=0.50$ (giảm $78.0\%$) và từ $195$ xuống $25$ ở $\alpha=0.80$ (giảm $87.2\%$). Đây là chỉ số cực đại, không phải phép đo trực tiếp phương sai hoặc percentile tail latency.
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
    - **Số bước dò**: Trong CSV hiệu chỉnh một triệu truy vấn, hit tăng từ $1.500$ lên $2.346$; miss tăng từ $2.503$ lên **$2,848.989$** tại $C=4M$ (khoảng $1,138\times$ so với base ở $C=0$ và $1,141.51\times$ so với Clean Table tại $C=4M$).
    - **Thời gian tra cứu**: Miss query time tại $C=4M$ là **18,223.54 ms** cho một triệu truy vấn, so với $89.39$ ms của Clean Table. Đây là thời gian phần truy vấn, không phải wall time của toàn JVM.
    - **Giải thích mốc timeout**: Wrapper ghi nhận tiến trình đầy đủ lâu nhất là $46.044$ giây trong giới hạn 60 giây; 48 lượt không timeout. Khi $C \to \infty$ không rebuild, số ô rỗng tiến về 0 và miss có thể suy giảm tới quét $O(M)$; không khẳng định mọi workload chắc chắn vượt timeout.
  - **Kiểm chứng hiệu chỉnh ngày 28/10**: Chạy lại ma trận 48 cấu hình với 1,000,000 truy vấn mỗi loại, seed 42, repeat 1; lưu tại `results/raw/rq2_results_1m.csv` và backup đồng nhất SHA-256 (`90948b676c321a85d4d6816dace23838706cef1b490105dffd87437ac0fd9539`). Tất cả 48 lượt `ok`, có đủ 21 cột và 48 `run_id` duy nhất; checksum `base`/`clean_table` khớp trên mọi mốc và query. Đây vẫn là một seed/một repeat, không dùng để tuyên bố median hoặc độ biến thiên đa seed.
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
- [x] **Thứ Ba (27/10/2026) - Hoàn thiện Mục V-A (Results & Discussion RQ1) và đóng góp Paper (Nhiệm vụ Cặp 1 - C1)**:
  - **Mục V-A (RQ1: Separate Chaining vs Linear Probing)**: Đưa đầy đủ bảng số liệu thực nghiệm Table I đối chiếu với lý thuyết Donald Knuth Vol 3 ($LP_{hit}, LP_{miss}, SC_{hit}, SC_{miss}$). Phân tích Thí nghiệm A ($\alpha \in [0.50, 0.95]$), Thí nghiệm B ($M \in [2^{18}, 2^{23}]$) và bài test dồn cụm.
  - **Phân tích Discussion chuyên sâu (Điểm bắt buộc đề tài)**: Luận giải nghịch lý số bước dò vs thời gian thực tế: dù SC ít bước dò hơn ở tải cao, nhưng mỗi lần duyệt `Node` tiêu tốn $32$ bytes/node (header 16B + 8B key + 4B val + 4B next), gây đứt gãy L1/L2 cache miss và áp lực GC; trong khi Linear Probing tận dụng mảng nguyên thủy liên tục và cơ chế CPU cache prefetching chạy nhanh hơn ở $\alpha \le 0.75$.
  - **Đóng góp các phần chung**: Hoàn thiện câu cuối của Abstract, bổ sung phân tích JIT/GC pauses cho SC vào Mục VI (Threats to Validity), đúc kết tổng hợp kiến trúc cho RQ1 trong Mục VII (Conclusion), và chuẩn hóa Acknowledgment.
  - **Tích hợp bộ biểu đồ RQ1**: Cập nhật danh mục hình ảnh và bảng biểu trong `results/figures/README.md` tích hợp `fig1_probes_vs_alpha.png`, `fig2_time_vs_alpha.png`, `fig5_memory.png`.
- [x] **Thứ Tư (28/10/2026) - Gộp, kiểm tra toàn diện và đóng gói (Nhiệm vụ Cặp 3 - C3 & Cả nhóm)**:
  - **Sáng (Morning) - Kiểm chéo số liệu bài báo IEEE và đồng bộ hóa văn bản**:
    - Xây dựng công cụ kiểm chéo số liệu tự động `scratch/verify_paper_c3_numbers.py`.
    - Đối chiếu 100% các giá trị trong Table II (Tải vừa $\alpha=0.50$), Table III (Tải cao $\alpha=0.80$), Table IV (Profiling khối $10^4$ ops), và các khẳng định định lượng trong bài báo với các tệp CSV gốc (`rq3_20261024.csv`, `rq3_20261025.csv`, `rq3_workload_summary.csv`): Đạt độ chính xác tuyệt đối **100.0%**, 0 sai lệch.
    - Hoàn thiện bài báo IEEE `paper/paper.tex`: Hoàn thiện Abstract, bổ sung phân tích chuyên sâu Threats to Validity (JIT warmup, GC pauses, đo độc lập trên 1 máy, hàm băm Fibonacci, hạt giống seed), chuẩn hóa trích dẫn [1]–[4] theo IEEE, và bổ sung tuyên bố sử dụng AI theo mục 7.7.
  - **Chiều (Afternoon) - Nghiệm thu Checklist Mục 9 và Đóng gói nộp bài**:
    - Xây dựng và thực thi bộ kiểm tra tự động Checklist Mục 9: `scratch/checklist_c3.py` và wrapper `scratch/checklist_c3.ps1`.
    - Đạt chuẩn **18/18 tiêu chí bắt buộc** của Cặp 3: Cài đặt đúng `rh`, `rh-noearly`, `lp-backshift` (Knuth Algorithm R), Fibonacci hash; Test suite vượt qua 100% (Oracle $10^6$ ops, 6 ca bắt buộc, DIB invariant); Dữ liệu RQ3 sạch 100% không lỗi; Bài báo chuẩn IEEE; Tài liệu hướng dẫn đầy đủ.
    - Cập nhật `README.md` và `results/raw/README.md` với đầy đủ thông số chạy lại, cờ lệnh JVM, và danh mục dataset.
    - Triển khai script đóng gói `scratch/package_submission.py`, kiểm tra tính toàn vẹn và tạo gói phát hành `dist/Nhom1_CSD_DeTai2_Submission_20261028.zip` kèm mã SHA-256 manifest `dist/SUBMISSION_MANIFEST.txt`.
  - **Tối (Evening) - Slide thuyết trình và Cẩm nang phản biện bảo vệ**:
    - Cập nhật đề cương slide tổng thể 8 slide (thời lượng 5 phút) tại `slides/outline.md`, phân vai rõ ràng cho 6 thành viên (Cặp 3 chủ trì Slide 7 và Slide 8).
    - Soạn thảo kịch bản thuyết trình chi tiết từng giây cho Cặp 3 tại `slides/rq3_slide_presentation.md` (SV 5 Nguyễn Đình Tiến trình bày Slide 7 trong 75s; SV 6 Lê Hoàng Cầu trình bày Slide 8 trong 30s).
    - Soạn thảo cẩm nang bộ câu hỏi phản biện bảo vệ chuyên sâu cho Cặp 3 tại `slides/defense_qa_c3.md`: Giải thích cặn kẽ bản chất toán học giảm phương sai DIB, ưu thế vượt trội của Knuth Algorithm R so với Sedgewick và Rebuild, cơ chế dừng sớm Early Exit và mức tiết kiệm $73.8\%$ bước dò miss ở tải cao.
- [x] **Thứ Năm (29/10/2026) - Chuẩn bị tập dượt vấn đáp (Cặp 3 - C3)**:
  - Bổ sung câu trả lời miệng ngắn cho ba câu hỏi trọng tâm của nhóm: Robin Hood giảm độ dời cực đại như thế nào; vì sao tombstone làm miss chậm; vì sao probe count thấp của Separate Chaining không luôn đồng nghĩa thời gian thấp.
  - Rà soát lời thoại Slide 7/8 và câu trả lời để phân biệt probe count, Max DIB, block latency và lookup time; bỏ các khẳng định chưa được phép đo xác nhận (phương sai/percentile trực tiếp, bảo đảm SLA, “không có spike” tuyệt đối).
  - Tài liệu dùng để các thành viên tập dượt: `slides/defense_qa_c3.md`, `slides/rq3_slide_presentation.md`, `slides/outline.md`.
  - **Phạm vi hoàn tất**: đã chuẩn bị và chỉnh tài liệu; buổi tập dượt trực tiếp của cả nhóm cần các thành viên thực hiện cùng nhau.
