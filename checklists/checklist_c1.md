# Checklist Mục 9 Tự Kiểm Tra Trước Khi Nộp - Cặp 1 (C1)
## Đề tài 2 - Hash Table: Separate Chaining vs Linear Probing (RQ1)
**Ngày nghiệm thu:** 28/10/2026  
**Người thực hiện:** Cặp 1 (C1) - Nguyễn Duy & Nguyễn Tuấn Hưng - Lớp SE20C - FPT University  
**Máy đo chính thức:** `PC_LAB_01` (Intel Core i5-11500H @ 2.90GHz, 32GB RAM, Windows 11 Pro, JDK 24, `-Xmx2g`)  

---

### Bảng Tổng Hợp Kiểm Định 6 Tiêu Chí Cốt Lõi Cho Cặp 1 (RQ1)

| STT | Tiêu chí RQ1 | Trạng thái | Bằng chứng vật lý trong repo (File, Dòng, Commit, Log) |
|:---:|---|:---:|---|
| 1 | **Thí nghiệm A (Ảnh hưởng của hệ số tải $\alpha$)**:<br>Dung lượng cố định $M = 2^{20} = 1,048,576$, đo đạc 4 mức tải $\alpha \in \{0.50, 0.75, 0.90, 0.95\}$ cho cả `sc` và `lp-tomb`. Khớp lý thuyết Knuth $< 0.5\%$. | ✅ PASSED | - Dữ liệu lab: [rq1_runner_results_lab.csv](../results/raw/rq1_runner_results_lab.csv) (dòng 2-5, 13-16, 24-27, 35-38)<br>- Báo cáo Paper: [paper.tex](../paper/paper.tex#L214-L236) (Experiment A, Table I)<br>- Log kiểm chéo: [verify_paper_all_numbers.log](../results/notes/verify_paper_all_numbers.log#L11-L14)<br>- Commit: `98438d5` & `735a864` |
| 2 | **Thí nghiệm B (Độ mở rộng dung lượng bảng $M$)**:<br>Hệ số tải cố định $\alpha = 0.75$, mở rộng dung lượng $M \in \{2^{18}, 2^{20}, 2^{21}, 2^{22}, 2^{23}\}$ ($2.62\times 10^5$ đến $8.38\times 10^6$ slots). Chứng minh tính bất biến quy mô (Scale-invariance) của số bước dò. | ✅ PASSED | - Dữ liệu lab: [rq1_runner_results_lab.csv](../results/raw/rq1_runner_results_lab.csv) (dòng 6-10, 17-21, 28-32, 39-43)<br>- Phân tích Paper: [paper.tex](../paper/paper.tex#L245-L255) (Experiment B, phân tích trần L3 cache 12MB)<br>- Slide báo cáo: [slide_rq1_c1.md](../slides/slide_rq1_c1.md#L40-L48)<br>- Commit: `98438d5` |
| 3 | **Bộ test dồn cụm bất lợi (Clustered Edge Test)**:<br>10% số khóa bị ép dồn vào cụm $h(k) < M/64$, 90% phân phối đều; $\alpha = 0.75$; $M \in \{2^{18}, 2^{20}\}$; 1 repeat. Chứng minh sự sụp đổ của LP do cụm sơ cấp bùng nổ 412% số bước dò và tăng 315x thời gian. | ✅ PASSED | - Dữ liệu lab: [rq1_runner_results_lab.csv](../results/raw/rq1_runner_results_lab.csv) (dòng 11-12, 22-23, 33-34, 44-45)<br>- Phân tích Paper: [paper.tex](../paper/paper.tex#L256-L265) (Adversarial Clustering Workload)<br>- Đồ thị thực nghiệm: [fig4_clustered.png](../results/figures/fig4_clustered.png)<br>- Commit: `98438d5` |
| 4 | **Tách biệt chế độ đo (`--mode count` vs `--mode time`)**:<br>Số lần dò đo bằng lượt `--count` riêng, không lẫn với lượt đo thời gian (wall-clock latency ms). | ✅ PASSED | - Mã nguồn đo: [BenchmarkRunner.java](../HashTableProject/src/hashtable/bench/BenchmarkRunner.java#L160-L200)<br>- Danh sách Job: [rq1_jobs.txt](../results/raw/rq1_jobs.txt) (88 jobs tách biệt count và time)<br>- Cấu trúc CSV: [rq1_runner_results_lab.csv](../results/raw/rq1_runner_results_lab.csv) (`ops_count` có giá trị ở count, rỗng ở time)<br>- Commit: `5af3c50` |
| 5 | **Discussion chuyên sâu về mô hình bộ nhớ đối tượng Java (The Java Memory Gap)**:<br>Phân tích chi phí cấp phát `Node`: Object header (16B), con trỏ (4B), cache miss phi tuần tự L1/L2, áp lực Garbage Collection (GC) so với mảng nguyên thủy phẳng liên tục (13B/slot). | ✅ PASSED | - Phân tích lý thuyết: [paper.tex](../paper/paper.tex#L266-L277) (Discussion: The Java Memory Gap)<br>- Mã phân tích độc lập: [RQ1Analysis.java](../HashTableProject/src/hashtable/bench/RQ1Analysis.java#L40-L100) (Mô hình 32 bytes/node vs 13 bytes/slot)<br>- Slide thuyết trình: [slide_rq1_c1.md](../slides/slide_rq1_c1.md#L51-L61)<br>- Commit: `8b678b0` & `735a864` |
| 6 | **Mô tả môi trường phần cứng và JVM**:<br>Mô tả chi tiết CPU (Intel Core i5-11500H @ 2.90GHz), 32GB RAM, Windows 11 Pro, JDK 24, cờ JVM `-Xmx2g -XX:+UseG1GC`. | ✅ PASSED | - Phần thiết lập trong bài báo: [paper.tex](../paper/paper.tex#L193-L199) (Section IV Experimental Setup)<br>- Hướng dẫn tổng thể: [README.md](../README.md#L8-L17) (Mục 1 Môi trường phát triển & Thực thi)<br>- Phân công đề tài: [PHAN_CONG.md](../PHAN_CONG.md#L4-L12)<br>- Commit: `6ffad4b` |

---

### Tiêu Chí Bổ Sung Đã Nghiệm Thu
- **Kiểm thử Unit Test & Oracle:** [C1SeparateChainingTest.java](../HashTableProject/test/hashtable/test/C1SeparateChainingTest.java) và [HashTableOracleTest.java](../HashTableProject/test/hashtable/test/HashTableOracleTest.java) (1,000,000 ops so khớp `HashMap`, 100% Passed).
- **Đối chiếu số liệu tự động:** Script [verify_paper_all_numbers.py](../scratch/verify_paper_all_numbers.py) đọc trực tiếp dữ liệu lab và ghi log tại [verify_paper_all_numbers.log](../results/notes/verify_paper_all_numbers.log) (Độ sai lệch 0.0%).

---

### Kết luận nghiệm thu C1
Toàn bộ **6/6 tiêu chí bắt buộc** của Cặp 1 (C1) theo Kế hoạch thực hiện Đề tài 2 và Mục 9 đã được hoàn thành, kiểm định và đối chiếu với bằng chứng vật lý đầy đủ trong repository. Sẵn sàng nộp bài!
