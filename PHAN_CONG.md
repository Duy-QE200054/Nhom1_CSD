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
- [x] **Thứ Bảy (24/10/2026) - Ma trận RQ3 (C3)**: Đo riêng `lp-tomb` ($\tau=0.2$), `lp-backshift`, `rh`, và `rh-noearly` tại $M=2^{20}$, $\alpha=0.50$, $C \in \{0, M/4, M/2, M, 2M, 4M\}$; 3 seed, 1 repeat, hit/miss và count/time. Hoàn tất 288 lượt, tất cả `ok`, checksum/output nhất quán. CSV: `results/raw/rq3_20261024.csv`; biểu đồ và phân tích cập nhật trong paper.

