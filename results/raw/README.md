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

## 2. Chuẩn Schema 20 Cột (Bảng 4)
Tất cả các dòng dữ liệu đều tuân thủ nghiêm ngặt 20 cột:
```csv
run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit
```

- **Mã hash đầu vào (`input_sha256`)**: SHA-256 mảng byte Big-Endian của các khóa nạp vào bảng, đảm bảo đối chiếu công bằng giữa các thuật toán.
- **Tổng kiểm tra đầu ra (`output_check`)**: Tích lũy giá trị tra cứu trong vòng lặp đo để triệt tiêu tối ưu hóa loại bỏ mã chết (Dead Code Elimination - DCE) của HotSpot JIT.
- **Trạng thái (`status`)**: Toàn bộ các lượt đo đều đạt `ok` và hoàn thành dưới ngưỡng timeout 60 giây.

