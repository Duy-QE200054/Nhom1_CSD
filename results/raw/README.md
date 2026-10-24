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

Để xem số đo mà không phải đọc các mã máy và mã chạy, dùng
[`rq3_20261024_readable.csv`](./rq3_20261024_readable.csv). Bảng này được tạo
từ 288 dòng thô, ghép count/time thành một dòng cho mỗi thuật toán, mốc churn,
seed và loại truy vấn; giữ nguyên từng seed, số probe, latency, build time và
trạng thái. Cấu hình đo của campaign: Windows 11 Pro, Intel Core i5-11500H,
RAM 32 GB, Java 24.0.2, heap tối đa 2 GB. CSV thô vẫn giữ `machine_id` để
tương thích schema và truy vết; đây là nhãn máy `PC_LAB_01`, không phải số đo.
