# Đồ Án Nhóm CSD201 (FA26) - Đề Tài 2
## Nghiên cứu và Đánh giá Hiệu năng Chỉ mục Tra cứu bằng Hash Table: Tombstone, Rebuild và Robin Hood Hashing

Nhóm sinh viên thực hiện: Nhóm 1 - Lớp SE20C - FPT University

---

## 1. Môi trường phát triển & Thực thi
- **Ngôn ngữ**: Java (Tương thích JDK 1.8+)
- **IDE**: NetBeans 17 / Dòng lệnh (CLI)
- **Cấu hình phần cứng máy đo chính**:
  - CPU: (Điền thông tin CPU máy đo chính, ví dụ: Intel Core i7 / AMD Ryzen)
  - Số nhân / Luồng: ...
  - RAM: 16 GB (hoặc 8 GB)
  - Hệ điều hành: Windows 11 / Linux
  - Tham số khởi động JVM bắt buộc: `-Xmx2g` (hoặc `-Xmx1g` nếu máy dưới 8GB RAM)

---

## 2. Cấu trúc thư mục (Chuẩn gói nộp theo Bảng 6)
```text
Nhom1_CSD/
├── paper/                  # Paper theo mẫu hội nghị IEEE (PDF và mã nguồn LaTeX/Word)
├── HashTableProject/       # Toàn bộ project NetBeans (Mã nguồn Java & Unit Test)
│   ├── src/                # Mã nguồn các thuật toán Hash Table, Generator, Benchmark
│   ├── test/               # Bộ test Oracle (HashMap) và 6 ca test biên bắt buộc
│   ├── nbproject/          # File cấu hình NetBeans Project
│   └── build.xml           # File Ant build cho NetBeans
├── bench/                  # Runner kịch bản đo và script vẽ biểu đồ
│   ├── run_all.py          # Script điều phối chạy toàn bộ RQ1, RQ2, RQ3
│   └── plot_figures.py     # Script tái tạo biểu đồ từ kết quả CSV
├── results/
│   ├── raw/                # Dữ liệu CSV kết quả đo thực tế (kể cả timeout, crash)
│   └── figures/            # Các biểu đồ đồ thị đưa vào bài báo IEEE
├── data/
│   └── README.md           # Đặc tả Generator, seed, SHA-256 dữ liệu
├── PHAN_CONG.md            # Bảng phân công chi tiết 6 thành viên theo mục 3.4
├── slides.pdf              # Slide thuyết trình bảo vệ (tối đa 8 slide)
└── README.md               # Tài liệu hướng dẫn này
```

---

## 3. Hướng dẫn Biên dịch và Chạy Test

### 3.1. Biên dịch Project (Build Jar)
Sử dụng NetBeans: Chọn project `HashTableProject` -> nhấn **Clean and Build**.  
Hoặc chạy bằng dòng lệnh Ant:
```bash
cd HashTableProject
ant jar
```
File jar sinh ra tại: `HashTableProject/dist/HashTableProject.jar`.

### 3.2. Chạy bộ kiểm thử (Unit Test & Oracle Check)
Chạy bộ test kiểm tra tính đúng so khớp oracle `HashMap` và 6 ca test biên:
```bash
cd HashTableProject
ant test
```
Oracle dùng chung chạy đúng 1.000.000 thao tác theo tỷ lệ 40% `put`, 40% `get`,
20% `remove`, với `M = 2^12`; seed cố định giúp tái lập kết quả. Sáu ca hợp đồng
được chạy cho từng factory đăng ký trong `HashTableOracleTest`. Hiện project đăng ký
`sc`, `lp-tomb` không rebuild và `lp-tomb` rebuild (`tau = 0.25`). Các cài đặt
`lp-backshift`, `rh` và `rh-noearly` có thể dùng cùng suite bằng cách đăng ký factory
của chúng trong `runAllTests()`.

Sáu ca hợp đồng lần lượt kiểm tra va chạm/cập nhật (M=16), xóa trong cụm va chạm,
khóa và giá trị biên, get/remove khóa không tồn tại và remove lặp lại, cụm va chạm
quấn vòng từ bucket cuối (M=16), và chuỗi thao tác hỗn hợp đối chiếu với `HashMap`.
Khóa va chạm của ca 1 và ca 5 được tìm bằng thử ngẫu nhiên có seed cố định.

---

## 4. Lệnh chạy lại kiểm tra (Phục vụ Vấn đáp bảo vệ - Dưới 60 giây)

Tại buổi bảo vệ, theo mục 10.1 của Quy định chung, giảng viên sẽ chọn ngẫu nhiên một cấu hình để chạy lại và đối chiếu `status`, `output_check` và thời gian (trong biên độ 0.5x - 2.0x median).

### RQ1: Separate Chaining vs Linear Probing (M = 2^18, α = 0.75)
```bash
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ1 --algo sc --m 262144 --alpha 0.75 --seed 42 --mode time
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ1 --algo lp-tomb --m 262144 --alpha 0.75 --seed 42 --mode time
```

### RQ2: Linear Probing Tombstone tích tụ (M = 2^18, C = 65536)
```bash
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ2 --algo lp-tomb --m 262144 --alpha 0.50 --c 65536 --seed 42 --mode time
```

### RQ3: Robin Hood vs LP Backshift vs LP Tombstone (M = 2^18 hoặc 2^20)
- Tải sống vừa $\alpha = 0.50$ (Ngưỡng rebuild $\tau = 0.20$ cho `lp-tomb`):
```bash
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ3 --algo rh --m 262144 --alpha 0.50 --c 65536 --seed 42 --mode time
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ3 --algo rh-noearly --m 262144 --alpha 0.50 --c 65536 --seed 42 --mode time
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ3 --algo lp-backshift --m 262144 --alpha 0.50 --c 65536 --seed 42 --mode time
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ3 --algo lp-tomb --tau 0.2 --m 262144 --alpha 0.50 --c 65536 --seed 42 --mode time
```

- Tải sống cao $\alpha = 0.80$ (Chiến dịch ngày 25/10, ngưỡng rebuild $\tau = 0.10$ cho `lp-tomb`):
```bash
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ3 --algo rh --m 1048576 --alpha 0.80 --c 262144 --seed 42 --mode time
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ3 --algo lp-backshift --m 1048576 --alpha 0.80 --c 262144 --seed 42 --mode time
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ3 --algo lp-tomb --tau 0.1 --m 1048576 --alpha 0.80 --c 262144 --seed 42 --mode time
```

*(Lưu ý: Dùng `--count` hoặc `--mode count` để chạy lượt đếm riêng; `--mode time` là lượt đo thời gian; có thể thêm `--query miss` để kiểm tra tra cứu trượt).*

### Điều Phối Toàn Bộ Kịch Bản Đo RQ3 (Bench Runner)
- Chạy toàn bộ ma trận tải sống cao ngày 25/10 ($\alpha = 0.80$):
```bash
python bench/run_all.py --rq RQ3 --m 1048576 --alpha 0.80 --cycles 0,262144,524288,1048576,2097152,4194304 --algorithms lp-tomb,lp-backshift,rh,rh-noearly --tau 0.1 --seeds 42,1337,20261019 --repeats 1 --output results/raw/rq3_20261025.csv
```
*(Thêm cờ `--quick` để chạy thử nghiệm nhanh trong vài giây).*

- Chạy bằng PowerShell với output UTF-8, kiểm tra CSV và bảng tổng kết:
```powershell
.\bench\run_rq3.ps1
```
Lệnh trên chạy quick check. Thêm `-Full` để chạy đủ 288 lượt của ma trận
$\alpha = 0.80$; mỗi lần chạy lưu vào một CSV mới trong `results\raw\`.

### Kiểm Tra Tính Toàn Vẹn & Không Có Mốc Lỗi (Nhiệm vụ 26/10)
```powershell
# Bằng PowerShell
powershell -ExecutionPolicy Bypass -File scratch/check_rq3.ps1

# Hoặc bằng Python cross-platform
python scratch/check_rq3.py
```

### Phân Tích Chuyên Sâu Khối 10⁴ Thao Tác & Bóc Tách Cơ Chế RQ3 (Nhiệm vụ 26/10)
- Chạy toàn bộ workload profiling theo khối $10^4$ thao tác và ghi nhận độ đột biến Rebuild:
```bash
python bench/run_rq3_analysis.py
```
Dữ liệu khối được xuất tự động vào: `results/raw/rq3_block_profiling_alpha050.csv`, `results/raw/rq3_block_profiling_alpha080.csv`, và `results/raw/rq3_workload_summary.csv`.

### Tái Tạo Biểu Đồ Cho Bài Báo IEEE (Figures Generator)
- Biểu đồ chiến dịch ngày 25/10 ($\alpha = 0.80$):
```bash
python bench/plot_figures.py --csv results/raw/rq3_20261025.csv --outdir results/figures/rq3_20261025
```

- Biểu đồ chuyên sâu ngày 26/10 (Rebuild latency spikes, Max DIB evolution, và bóc tách 3 cơ chế):
```bash
python bench/plot_figures.py --plot-oct26
```
Ảnh đồ thị chất lượng cao được lưu tại: `results/figures/rq3_20261026/`.

---


## 5. Giải thích các trường dữ liệu CSV (Bảng 4)
Mỗi dòng trong file CSV tại `results/raw/` tương ứng với một lượt đo độc lập:

| Tên cột | Ý nghĩa |
|---|---|
| `run_id` | Định danh duy nhất cho từng lượt chạy |
| `rq` | Câu hỏi nghiên cứu: `RQ1`, `RQ2`, `RQ3` hoặc `TEST` |
| `algorithm` | Thuật toán: `sc`, `lp-tomb`, `lp-backshift`, `rh` |
| `variant` | Biến thể hoặc cơ chế tắt: `base`, `noearly`, `clean_table` |
| `dataset` | Mã dữ liệu: `uniform`, `clustered` |
| `n` | Quy mô khóa đang chứa trong bảng |
| `params` | Tham số bổ sung (vd: `M=1048576;alpha=0.75;tau=0.2;C=262144`) |
| `seed` | Hạt giống ngẫu nhiên của Generator |
| `input_sha256` | Mã hash SHA-256 của tập dữ liệu đầu vào |
| `generator_version` | Phiên bản của bộ sinh dữ liệu (vd: `1.0.0`) |
| `repeat` | Lần lặp (từ 1 đến 3) |
| `elapsed_ms` | Thời gian chạy (ms), để trống nếu `timeout` hoặc lỗi |
| `build_ms` | Thời gian dựng và nạp dữ liệu vào bảng, tách riêng khỏi thời gian truy vấn |
| `heap_before_bytes` | Lượng heap sử dụng sau GC, trước khi dựng bảng |
| `heap_peak_bytes` | Tổng high-water mark của các heap memory pool trong lượt chạy |
| `ops_count` | Số lần dò/thao tác trung bình (chỉ có giá trị ở lượt `--count`) |
| `output_check` | Tổng kiểm tra checksum kết quả tra cứu (chống DCE) |
| `status` | Trạng thái: `ok`, `timeout`, `crash`, `oom`, `wrong` |
| `xmx` | Giới hạn bộ nhớ JVM: `2g` (hoặc `1g`) |
| `machine_id` | Mã máy đo (vd: `PC_LAB_01`) |
| `git_commit` | Hash commit Git của mã nguồn tương ứng |
