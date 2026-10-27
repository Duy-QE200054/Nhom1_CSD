# Checklist Mục 9 Tự Kiểm Tra Trước Khi Nộp - Cặp 1 (C1)
## Đề tài 2 - Hash Table: Separate Chaining vs Linear Probing (RQ1)
**Ngày nghiệm thu:** 28/10/2026  
**Người thực hiện:** Cặp 1 (C1) - Lớp SE20C - FPT University  

---

### Bảng Tổng Hợp Kiểm Định Mục 9 Cho Cặp 1 (RQ1)

| STT | Tiêu chí Mục 9 | Trạng thái | Đường dẫn bằng chứng vật lý trong repo |
|:---:|---|:---:|---|
| 1 | **Thí nghiệm A (Ảnh hưởng của hệ số tải $\alpha$)**:<br>Dung lượng cố định $M = 2^{20} = 1,048,576$, đo đạc 4 mức tải $\alpha \in \{0.50, 0.75, 0.90, 0.95\}$ cho cả `sc` và `lp-tomb`. | ✅ PASSED | - [rq1_runner_results.csv](../results/raw/rq1_runner_results.csv) (dòng 2-5, 13-16, 24-27, 35-38)<br>- [paper.tex](../paper/paper.tex#L214-L234) (Experiment A, Table I) |
| 2 | **Thí nghiệm B (Độ mở rộng dung lượng bảng $M$)**:<br>Hệ số tải cố định $\alpha = 0.75$, mở rộng dung lượng $M \in \{2^{18}, 2^{20}, 2^{21}, 2^{22}, 2^{23}\}$ ($2.62\times 10^5$ đến $8.38\times 10^6$ slots). | ✅ PASSED | - [rq1_runner_results.csv](../results/raw/rq1_runner_results.csv) (dòng 6-10, 17-21, 28-32, 39-43)<br>- [paper.tex](../paper/paper.tex#L245-L255) (Experiment B, phân tích L3 cache limit 12MB) |
| 3 | **Bộ test dồn cụm bất lợi (Clustered Edge Test)**:<br>10% số khóa bị ép dồn vào cụm $h(k) < M/64$, 90% phân phối đều; $\alpha = 0.75$; $M \in \{2^{18}, 2^{20}\}$; 1 repeat. | ✅ PASSED | - [rq1_runner_results.csv](../results/raw/rq1_runner_results.csv) (dòng 11-12, 22-23, 33-34, 44-45)<br>- [paper.tex](../paper/paper.tex#L256-L265) (Phân tích bùng nổ 412% bước dò ở LP do chuỗi dồn tích tụ) |
| 4 | **Tách biệt chế độ đo (`--mode count` vs `--mode time`)**:<br>Số lần dò đo bằng lượt `--count` riêng, không lẫn với lượt đo thời gian (wall-clock latency ms). | ✅ PASSED | - [BenchmarkRunner.java](../HashTableProject/src/hashtable/bench/BenchmarkRunner.java#L160-L200)<br>- [rq1_jobs.txt](../results/raw/rq1_jobs.txt) (88 jobs tách biệt count và time)<br>- [rq1_runner_results.csv](../results/raw/rq1_runner_results.csv) (ops_count có giá trị ở count, rỗng ở time) |
| 5 | **Discussion chuyên sâu về mô hình bộ nhớ đối tượng Java**:<br>Phân tích chi phí cấp phát `Node`: Object header (16B), con trỏ (4B), cache miss phi tuần tự L1/L2, áp lực Garbage Collection (GC) so với mảng nguyên thủy phẳng liên tục. | ✅ PASSED | - [paper.tex](../paper/paper.tex#L266-L277) (Discussion: The Java Memory Gap)<br>- [RQ1Analysis.java](../HashTableProject/src/hashtable/bench/RQ1Analysis.java#L40-L100) (Mô hình 32 bytes/node vs 13 bytes/slot) |
| 6 | **Mô tả môi trường phần cứng và JVM**:<br>Mô tả chi tiết CPU, xung nhịp, RAM, hệ điều hành, cờ JVM `-Xmx2g -XX:+UseG1GC`. | ✅ PASSED | - [paper.tex](../paper/paper.tex#L193-L199) (Section IV Experimental Setup)<br>- [README.md](../README.md#L8-L17) (Mục 1 Môi trường phát triển & Thực thi) |
| 7 | **Đối chiếu lý thuyết Knuth Volume 3**:<br>Sai số tương đối giữa bước dò thực nghiệm và tiệm cận Knuth $< 0.5\%$. | ✅ PASSED | - [paper.tex](../paper/paper.tex#L217-L242) (Table I và Eq. 1, Eq. 2)<br>- [verify_paper_all_numbers.py](../scratch/verify_paper_all_numbers.py) (Kiểm chéo tự động 100% PASSED) |
| 8 | **Tính toàn vẹn mã nguồn và Test Suite**:<br>Mã nguồn `SeparateChaining.java` và `RQ1Analysis.java` biên dịch sạch, vượt qua 100% test case trong suite. | ✅ PASSED | - [SeparateChaining.java](../HashTableProject/src/hashtable/core/SeparateChaining.java)<br>- [C1SeparateChainingTest.java](../HashTableProject/test/hashtable/test/C1SeparateChainingTest.java)<br>- [checklist_c3.ps1](../scratch/checklist_c3.ps1) (Chạy 0 failures) |

---

### Kết luận nghiệm thu C1
Toàn bộ **8/8 tiêu chí bắt buộc** của Cặp 1 (C1) theo Kế hoạch thực hiện Đề tài 2 và Mục 9 đã được hoàn thành, kiểm định và đối chiếu với bằng chứng vật lý đầy đủ trong repository. Sẵn sàng nộp bài!
