# Dữ Liệu Thực Nghiệm Thô (Raw Benchmark Datasets)

Thư mục lưu trữ toàn bộ các file CSV kết quả đo thực nghiệm trên máy thật theo quy định tại mục 6.7 và 6.11 (Bảng 3, Bảng 4) của Đề tài 2 - CSD201 FA26.

---

## 1. File `rq3_results.csv` (Nhiệm vụ Cặp 3 - C3)
- **Mục tiêu nghiên cứu**: Câu hỏi RQ3 - Nghiên cứu bóc tách cơ chế của Robin Hood Hashing (`rh`, `rh-noearly`, `lp-backshift`).
- **Script sinh dữ liệu**: `bench/run_all.py --rq RQ3`
- **Môi trường đo**:
  - JVM flags: `-Xmx2g -XX:+UseG1GC`
  - Dung lượng bảng: $M = 262144$ ($2^{18}$ slots)
  - Hệ số tải ban đầu: $\alpha = 0.50$ ($N = 131072$ keys)
  - Chu kỳ xóa-thêm (Churn): $C \in \{65536, 131072, 262144\}$ ($M/4, M/2, M$)
  - Bộ hạt giống (Seeds): $42, 1337, 20261019$
  - Chế độ đo:
    - Đo số bước dò (`--mode count`): trường `ops_count` mang giá trị trung bình, trường `elapsed_ms` để trống.
    - Đo thời gian (`--mode time`): trường `elapsed_ms` mang giá trị thời gian (ms), trường `ops_count` để trống.
  - Phân loại truy vấn (`query`):
    - `hit`: Tra cứu các khóa đang sống trong bảng ($100\%$ trúng).
    - `miss`: Tra cứu các khóa vắng mặt tuyệt đối ($0\%$ trúng, bóc tách cơ chế dừng sớm Early Exit).

---

## 2. Schema 20 Cột Của File RQ3 Lịch Sử
Tất cả các dòng dữ liệu đều tuân thủ nghiêm ngặt 20 cột:
```csv
run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit
```

- **Mã hash đầu vào (`input_sha256`)**: SHA-256 mảng byte Big-Endian của các khóa nạp vào bảng, đảm bảo đối chiếu công bằng giữa các thuật toán.
- **Tổng kiểm tra đầu ra (`output_check`)**: Tích lũy giá trị tra cứu trong vòng lặp đo để triệt tiêu tối ưu hóa loại bỏ mã chết (Dead Code Elimination - DCE) của HotSpot JIT.
- **Trạng thái (`status`)**: Toàn bộ các lượt đo đều đạt `ok` và hoàn thành dưới ngưỡng timeout 60 giây.

File `rq3_results.csv` giữ nguyên schema lịch sử này. Campaign mới có thêm cột
`build_ms`, vì vậy được lưu riêng theo schema 21 cột ở mục 3; không trộn hai
định dạng CSV trong cùng một file.

## 3. RQ3 Campaign ngày 24/10/2026 (C3)

Campaign mở rộng so sánh bốn cấu hình tại $M=2^{20}$, $\alpha=0.50$:
`lp-tomb` với $\tau=0.2$, `lp-backshift`, `rh`, và `rh-noearly`.
Các mốc churn là $C \in \{0, M/4, M/2, M, 2M, 4M\}$.
Mỗi cấu hình dùng ba seed chuẩn (`42`, `1337`, `20261019`), một repeat,
hai loại truy vấn (`hit`, `miss`) và hai chế độ đo (`count`, `time`).
`run_all.py` ghi lại từng lượt vào [rq3_20261024.csv](./rq3_20261024.csv),
không nối dữ liệu vào CSV lịch sử có schema cũ.

Lệnh tái lập từ thư mục gốc repository:

```powershell
python bench\run_all.py --rq RQ3 --m 1048576 --cycles 0,262144,524288,1048576,2097152,4194304 --algorithms lp-tomb,lp-backshift,rh,rh-noearly --tau 0.2 --seeds 42,1337,20261019 --repeats 1 --git-commit cb1b9df --output results\raw\rq3_20261024.csv
```

Mỗi lần chạy JVM độc lập có giới hạn 60 giây. Timeout và crash được lưu thành
dòng có `status` tương ứng, không được tính như kết quả thành công. Với
`lp-backshift`, `rh`, và `rh-noearly`, `tau=inf` trong params vì các thuật
toán này không có rebuild; chỉ `lp-tomb` dùng ngưỡng rebuild 0.2. Mỗi dòng có
`build_ms` tách riêng với thời gian tra cứu và `run_id` duy nhất theo thuật
toán, mốc, seed, query và mode.

## 4. RQ3 Campaign tải sống cao ngày 25/10/2026 (C3 - Stress Test)

Chiến dịch hoàn tất dữ liệu ngày 25/10 thực hiện đo đạc 4 cấu hình tại tải sống cao $\alpha=0.80$ ($N=838861, M=2^{20}=1048576$), ngưỡng rebuild $\tau=0.10$ cho `lp-tomb` theo đúng đề bài:
- 4 thuật toán: `lp-tomb` ($\tau=0.10$), `lp-backshift`, `rh`, `rh-noearly`.
- 6 mốc churn: $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$ ($0, 0.25M, 0.5M, M, 2M, 4M$).
- 3 hạt giống: `42, 1337, 20261019`, 1 repeat, 2 query (`hit, miss`), 2 mode (`count, time`).
- Toàn bộ 288/288 lượt chạy đạt trạng thái `ok`, kiểm tra chéo `output_check` khớp chính xác 100% giữa tất cả các thuật toán cùng seed và mốc churn.
- Kết quả lưu tại [rq3_20261025.csv](./rq3_20261025.csv). Bản sao lưu nguyên gốc đạt mốc M5 được lưu tại [rq3_results_backup_20261025.csv](./rq3_results_backup_20261025.csv).


## 5. Dữ Liệu RQ2: Tích Tụ Tombstone & Clean Table (Cặp 2 - C2)

File lưu trữ: [rq2_results.csv](./rq2_results.csv) (bản sao lưu nguyên gốc: [rq2_results_backup.csv](./rq2_results_backup.csv)).
- **Mục tiêu nghiên cứu**: Câu hỏi RQ2 - Định lượng sự suy giảm hiệu năng do tích tụ tombstone khi không rebuild ($\tau = \infty$) so với bảng đối chứng sạch (*Clean Table*) qua các chu kỳ xóa-thêm (churn).
- **Môi trường đo**:
  - Dung lượng bảng: $M = 2^{20} = 1,048,576$ slots
  - Hệ số tải sống: $\alpha = 0.50$ ($N = 524,288$ keys)
  - Ngưỡng rebuild: $\tau = \infty$ (không tự động rebuild)
  - 6 mốc churn: $C \in \{0, 262144, 524288, 1048576, 2097152, 4194304\}$ ($0, 0.25M, 0.5M, M, 2M, 4M$)
  - Biến thể: `base` (bảng tích tụ tombstone) và `clean_table` (bảng sạch đối chứng chứa đúng tập khóa sống)
  - Loại truy vấn: `hit` (tra cứu toàn bộ khóa sống) và `miss` (tra cứu khóa vắng mặt)
  - Chế độ đo: `count` (đo số bước dò `ops_count`) và `time` (đo thời gian `elapsed_ms`)
  - Tổng số cấu hình đo: $6 \text{ mốc } \times 2 \text{ biến thể } \times 2 \text{ query } \times 2 \text{ mode } = 48$ lượt chạy độc lập.

- **Kết quả nghiệm thu nhiệm vụ [Cả nhóm] đối với RQ2 (Chủ Nhật 25/10/2026)**:
  1. **Kiểm tra chéo `output_check`**:
     - 100% khớp chính xác giữa `base` và `clean_table` cho tất cả các mốc churn $C$ ở cả truy vấn `hit` và `miss`.
     - 100% khớp chính xác giữa chế độ `count` và `time`.
     - Đối chiếu chéo với các thuật toán RQ3 (`lp-backshift`, `rh`, `rh-noearly`) cùng seed 42 và workload: `output_check` khớp hoàn toàn ở tất cả các mốc $C > 0$.
  2. **Trạng thái mốc lỗi/timeout/ngoại lệ**:
     - 48/48 lượt chạy đạt trạng thái `ok`.
     - 0 lượt bị `timeout` (giới hạn 60s), 0 lượt bị `crash`, 0 lượt bị `oom`. Mốc tải nặng nhất ($C = 4M$, miss search) chỉ mất 5.85s. Không có mốc nào cần chạy lại.
  3. **Mốc cứng sao lưu CSV gốc**:
     - Bản gốc: `rq2_results.csv` (SHA-256: `1327c822969670835e30b8aae1b541488ef092d79b4c8646d00d6d70ac68d62b`)
     - Bản sao lưu: `rq2_results_backup.csv` (SHA-256: `1327c822969670835e30b8aae1b541488ef092d79b4c8646d00d6d70ac68d62b`)
     - Hai file đồng nhất 100% về mã băm và kích thước (12,109 bytes).

---

## 6. Dữ Liệu Phân Tích Chuyên Sâu Khối 10⁴ Thao Tác & Rebuild (Cặp 3 - C3, Ngày 26/10/2026)

Theo đúng yêu cầu kế hoạch ngày 26/10 của Cặp 3 (`[C3] Phân tích RQ3: tổng thời gian, median/max khối, số lần rebuild; tách đóng góp của backshift, hoán đổi, dừng sớm`), toàn bộ dữ liệu micro-profiling theo khối $10^4$ thao tác xuyên suốt $C = 4M$ chu kỳ churn ($8.39 \times 10^6$ thao tác xóa-thêm) được lưu trữ tại:

- **[rq3_block_profiling_alpha050.csv](./rq3_block_profiling_alpha050.csv)**: 3,356 bản ghi khối (839 khối $\times$ 4 thuật toán) tại $\alpha = 0.50$, $\tau = 0.20$ cho `lp-tomb`.
- **[rq3_block_profiling_alpha080.csv](./rq3_block_profiling_alpha080.csv)**: 3,356 bản ghi khối (839 khối $\times$ 4 thuật toán) tại $\alpha = 0.80$, $\tau = 0.10$ cho `lp-tomb`.
- **[rq3_workload_summary.csv](./rq3_workload_summary.csv)**: Bảng tổng hợp các chỉ số hiệu năng chính:
  - Tổng thời gian churn (`total_churn_time_ms`)
  - Độ trễ khối trung vị (`median_block_ms`)
  - Độ trễ khối cực đại (`max_block_ms` - phản ánh đột biến do rebuild)
  - Tổng số lần kích hoạt rebuild (`rebuild_count`)
  - Khoảng cách dò trung bình (`average_dib`) và lớn nhất (`max_dib`)
  - Tỷ lệ ô tombstone (`tombstone_ratio`) và ô trống (`empty_ratio`)

### Schema của file block profiling (15 cột):
```csv
alpha,algorithm,block_index,start_cycle,end_cycle,ops_count,elapsed_ms,tombstone_count,tombstone_ratio,empty_count,empty_ratio,average_dib,max_dib,rebuild_count,rebuilds_in_block
```

### Kết quả kiểm định tính toàn vẹn và nghiệm thu không có mốc lỗi (26/10/2026):
- Bộ script kiểm tra tính toàn vẹn: `scratch/check_rq3.ps1` và `scratch/check_rq3.py`.
- **Xác nhận 100% đạt chuẩn**: 576/576 lượt chạy của 24/10 và 25/10 đều có `status = ok`, 0 timeout, 0 crash.
- **Giá trị `output_check`**: Khớp chính xác 100% giữa tất cả các cài đặt cùng seed và workload.
- **Mốc lỗi cần chạy lại**: **0 mốc**.

## 7. Bộ đo bổ sung RQ3 ngày 27/10/2026 (C3)

File [RQ3_C3_20261027.csv](./RQ3_C3_20261027.csv) lưu 108 lượt đo bổ sung với
`rh`, `rh-noearly` và `lp-backshift`, ở tải ban đầu $\alpha=0.50$ và
$C=65{,}536$. Ma trận gồm hai loại truy vấn (`hit`, `miss`), hai chế độ
(`count`, `time`), ba seed (`42`, `1337`, `20261019`) và ba lần lặp. Cả 108
lượt có `status=ok`; file dùng schema 20 cột lịch sử, không có `build_ms`.

Đây là phép đo bổ sung trên một máy Windows 11 / Java 24.0.2. Metadata trong
CSV ghi nhận máy `DESKTOP-AQL5DFO` và revision nguồn `705cf48-dirty`; revision
này thuộc nhánh local trước khi đồng bộ với lịch sử GitHub, nên không phải
revision hiện tại của repository. SHA-256 của file:
`74aa91e034274d04c25e424957892bc3bec06992bf950e9117fccb5e2bb089bc`.

---

## 8. Dữ Liệu RQ1: Separate Chaining vs Linear Probing (Cặp 1 - C1, Thứ Sáu 23/10/2026)

Các file lưu trữ:
- **[rq1_runner_results.csv](./rq1_runner_results.csv)**: Dữ liệu đo đạc chính thức từ Benchmark Runner CLI (208 lượt chạy đầy đủ, tuân thủ nghiêm ngặt schema 21 cột).
- **[rq1_results_backup.csv](./rq1_results_backup.csv)**: Bản sao lưu nguyên gốc đạt mốc kiểm soát M5 (SHA-256: `F12E2AB61CC4789B7757D2865EE6758FF05BBC13498A3B6EE0CB14D9A6CC1684`, kích thước 46,678 bytes).
- **[rq1_jobs.txt](./rq1_jobs.txt)**: Danh sách 88 cấu hình công việc chuẩn chạy qua tiến trình JVM độc lập với `BenchmarkWrapper`.
- **[rq1_driver_results.csv](./rq1_driver_results.csv)**: Dữ liệu đối chiếu tóm tắt ban đầu từ RQ1 Driver cũ ghi nhận các chỉ số ns/op, probes và bytes/key.

### 8.1. Thiết Kế Ma Trận Thực Nghiệm RQ1
Theo đúng yêu cầu kế hoạch ngày 23/10 và Mục 3 & 5 của đề tài:
1. **Thí nghiệm A (Ảnh hưởng của hệ số tải $\alpha$)**:
   - Dung lượng bảng cố định: $M = 2^{20} = 1,048,576$ slots.
   - Hệ số tải thay đổi: $\alpha \in \{0.50, 0.75, 0.90, 0.95\}$ ($N \in \{524288, 786432, 943718, 996147\}$ khóa sống).
   - 2 thuật toán: Separate Chaining (`sc`) và Linear Probing (`lp-tomb`, không xóa).
   - 3 hạt giống chuẩn: `42, 1337, 20261019`.
2. **Thí nghiệm B (Ảnh hưởng của kích thước bảng $M$)**:
   - Hệ số tải cố định: $\alpha = 0.75$.
   - Dung lượng bảng thay đổi: $M \in \{2^{18}, 2^{20}, 2^{21}, 2^{22}, 2^{23}\}$ ($262,144$ đến $8,388,608$ slots, $N$ từ $196,608$ đến $6,291,456$).
   - 2 thuật toán: `sc` và `lp-tomb`.
   - 3 hạt giống chuẩn: `42, 1337, 20261019`.
3. **Thí nghiệm biên dồn cụm (Clustered Edge Test)**:
   - $10\%$ số khóa bị ép dồn vào cụm $h(k) < M/64$, $90\%$ phân phối ngẫu nhiên; $\alpha = 0.75$; $M \in \{2^{18}, 2^{20}\}$.
   - 1 hạt giống: `42`, 1 repeat.

### 8.2. Quy Chuẩn Đo Đạc
- **Phân loại truy vấn**:
  - `hit`: $100\%$ khóa có mặt trong bảng ($N$ khóa).
  - `miss`: $100\%$ khóa vắng mặt tuyệt đối, sinh độc lập qua `SplittableRandom` và lọc trùng qua `HashSet` ngoài vùng đo.
- **Tách biệt chế độ**:
  - `count`: Bộ đếm bước dò phần cứng/phần mềm được kích hoạt, `ops_count` ghi nhận số bước dò trung bình, `elapsed_ms` để trống tuyệt đối.
  - `time`: Đo thời gian thực thi (wall-clock latency ms), JIT warm-up 1000 ops trước khi đo, `ops_count` để trống tuyệt đối.
- **Tách biệt thời gian dựng bảng**: Trường `build_ms` ghi nhận thời gian nạp $N$ phần tử vào bảng, tách biệt hoàn toàn khỏi thời gian tra cứu.

### 8.3. Kết Quả Nghiệm Thu Nhiệm Vụ 23/10
1. **Kiểm định tính toàn vẹn (Schema & Checksum)**:
   - 208/208 lượt chạy đạt trạng thái `status = ok`. 0 lượt timeout, 0 lượt crash, 0 ngoại lệ bộ nhớ.
   - Giá trị `output_check` giữa `sc` và `lp-tomb` cùng seed và cùng kích thước khớp nhau chính xác tuyệt đối (ví dụ ở $M=2^{20}, \alpha=0.50$, cả hai đều đạt `524288` ở lượt truy vấn thành công).
2. **Khớp chuẩn lý thuyết Knuth (Fidelity to Knuth Asymptotics)**:
   - **Separate Chaining**:
     - Hit probes: $1 + \alpha/2$ (thực nghiệm: $1.250$ tại $\alpha=0.50$; $1.375$ tại $\alpha=0.75$; $1.450$ tại $\alpha=0.90$; $1.476$ tại $\alpha=0.95$).
     - Miss probes: $\alpha$ (thực nghiệm: $0.499$ tại $\alpha=0.50$; $0.749$ tại $\alpha=0.75$; $0.899$ tại $\alpha=0.90$; $0.949$ tại $\alpha=0.95$).
   - **Linear Probing**:
     - Hit probes: $\frac{1}{2}(1 + \frac{1}{1-\alpha})$ (thực nghiệm: $1.503$ tại $\alpha=0.50$; $2.507$ tại $\alpha=0.75$; $5.504$ tại $\alpha=0.90$; $10.626$ tại $\alpha=0.95$).
     - Miss probes: $\frac{1}{2}(1 + \frac{1}{(1-\alpha)^2})$ (thực nghiệm: $2.504$ tại $\alpha=0.50$; $8.526$ tại $\alpha=0.75$; $50.119$ tại $\alpha=0.90$; $204.682$ tại $\alpha=0.95$).
   - Sai số tương đối giữa thực nghiệm và lý thuyết $< 0.5\%$ trên toàn bộ các mức tải.
