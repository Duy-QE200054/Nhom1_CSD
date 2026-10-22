# Chuẩn Hóa Schema CSV Cho Thực Nghiệm (Benchmark CSV Schema)
## Đề tài 2 - CSD201 FA26

> Căn cứ theo mục 6.7, 6.11 (Bảng 3, Bảng 4) của Quy định chung và mục 2 của Kế hoạch thực hiện ngày 19/10.

---

### 1. Danh sách 21 Cột Bắt Buộc (Theo Thứ Tự)

Header dòng đầu tiên của mọi file CSV:
```csv
run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,build_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit
```

| STT | Tên cột | Kiểu dữ liệu | Đơn vị / Định dạng | Ví dụ | Quy tắc đặc biệt |
|:---:|---|---|---|---|---|
| 1 | `run_id` | String | Chuỗi duy nhất | `RUN_RQ1_SC_001` | Không trùng lặp giữa các lượt |
| 2 | `rq` | String | Danh mục | `RQ1`, `RQ2`, `RQ3`, `TEST` | Đúng tên RQ |
| 3 | `algorithm` | String | Tên thuật toán | `sc`, `lp-tomb`, `lp-backshift`, `rh` | Theo quy ước Bảng 2 của đề |
| 4 | `variant` | String | Biến thể | `base`, `noearly`, `clean_table` | Bắt buộc cho việc bóc tách cơ chế |
| 5 | `dataset` | String | Phân phối dữ liệu | `uniform`, `clustered` | `clustered` chỉ dùng ở test biên RQ1 |
| 6 | `n` | Integer | Số lượng khóa | `524288`, `786432` | Số lượng phần tử sống thực tế trong bảng |
| 7 | `params` | String | `key=val;key=val` | `M=1048576;alpha=0.75;tau=0.2;C=262144` | Phân cách cặp bằng dấu `;` |
| 8 | `seed` | Long | Số nguyên 64-bit | `42`, `1337`, `20261019` | Seed cung cấp cho Generator |
| 9 | `input_sha256` | String | Hex 64 ký tự | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4...` | SHA-256 chuỗi byte Big-Endian của input |
| 10 | `generator_version` | String | Phiên bản mã sinh | `1.0.0` | Để kiểm soát tính tái lập |
| 11 | `repeat` | Integer | Số thứ tự lần lặp | `1`, `2`, `3` | Cấu hình chính lặp 3 lần; test biên lặp 1 |
| 12 | `elapsed_ms` | Double / Rỗng | Miligiây (ms) | `124.58` | Thời gian truy vấn; **để trống** khi `timeout` hoặc lượt `--count` |
| 13 | `build_ms` | Double / Rỗng | Miligiây (ms) | `85.31` | Thời gian dựng bảng (khởi tạo và nạp dữ liệu), tách khỏi thời gian truy vấn |
| 14 | `heap_before_bytes` | Long | Bytes | `14680064` | Heap đang dùng sau GC, trước khi dựng bảng |
| 15 | `heap_peak_bytes` | Long | Bytes | `48234496` | Tổng high-water mark của các heap memory pool từ lúc dựng bảng đến hết lượt đo |
| 16 | `ops_count` | Double / Rỗng | Số bước dò TB | `2.48` | **Chỉ có giá trị ở lượt `--count`**, để trống ở lượt đo thời gian |
| 17 | `output_check` | Long | Số nguyên 64-bit | `84920491823` | Tổng kiểm tra output (chống JIT DCE và kiểm chéo) |
| 18 | `status` | String | Enum trạng thái | `ok`, `timeout`, `crash`, `oom`, `wrong` | Theo Bảng 3 của Quy định chung |
| 19 | `xmx` | String | Dung lượng RAM JVM | `2g` (hoặc `1g`) | Cùng một RQ phải dùng cùng một giá trị -Xmx |
| 20 | `machine_id` | String | Tên định danh máy | `PC_MAIN_LAB` | Đảm bảo tính nhất quán của môi trường đo |
| 21 | `git_commit` | String | Mã commit Git | `a1b2c3d` | 7 ký tự commit hash |

---

### 2. Quy Tắc Ghi Nhận Cho Từng Thuật Toán & Biến Thể

#### A. Robin Hood và Biến thể tắt dừng sớm (`rh-noearly`):
- Robin Hood chuẩn đầy đủ:
  - `algorithm`: `rh`
  - `variant`: `base`
- Robin Hood tắt dừng sớm:
  - `algorithm`: `rh`
  - `variant`: `noearly`

#### B. Bảng đối chứng cho RQ2 (`clean_table`):
- Bảng tích tụ tombstone thông thường:
  - `algorithm`: `lp-tomb`, `variant`: `base`, `params`: `...;tau=inf;C=...`
- Bảng đối chứng dựng mới tinh từ đúng các khóa đang sống tại mốc $C$:
  - `algorithm`: `lp-tomb`, `variant`: `clean_table`, `params`: `...;tau=inf;C=...`

---

### 3. Quy Tắc Ghi Nhận Timeout & Lỗi

1. **Khi bị Timeout (Quá 60 giây)**:
   - Runner gọi `process.destroyForcibly()`.
   - Cột `status`: ghi chính xác là `timeout`.
   - Cột `elapsed_ms`: **để trống tuyệt đối** (ví dụ: `...,1,,,,,-1,-1,...,timeout,...`).
   - Khi vẽ biểu đồ, điểm này được ghi chú là `> 60 s`, không được gán bằng 60000ms để tránh làm sai lệch phép tính tỉ lệ tăng tốc.
2. **Tách biệt giữa Lượt đo Thời gian và Lượt `--count`**:
   - **Lượt đo thời gian** (`--mode time`): `elapsed_ms` có giá trị, `ops_count` để trống.
   - **Lượt đếm thao tác** (`--count` hoặc `--mode count`): `ops_count` có giá trị trung bình số ô đã xem (hoặc số node đối với SC), `elapsed_ms` để trống. Không bao giờ bật bộ đếm trong lượt đo thời gian.
