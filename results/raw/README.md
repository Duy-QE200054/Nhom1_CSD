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
