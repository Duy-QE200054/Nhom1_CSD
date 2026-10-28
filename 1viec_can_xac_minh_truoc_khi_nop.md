# Việc cần xác minh và sửa trước khi nộp (28/10/2026)

Tài liệu này làm rõ ba nhóm vấn đề tồn đọng của Nhóm 1, theo thứ tự ưu tiên. Nguyên tắc chung: **chỉ tick ✅ khi có bằng chứng vật lý trong repo** (file, commit, log, ảnh chụp), không tick theo suy đoán.

| Mức | Nhóm việc | Người phụ trách |
| --- | --- | --- |
| P0 | 1. Xác minh và sửa dữ liệu RQ1 | Cặp 1 (kiểm lại bởi cả nhóm) |
| P1 | 2. Chuẩn hóa schema CSV và metadata | Cả nhóm |
| P1 | 3. Checklist mục 9 của Cặp 1 và review chéo C1 → C2 | Cặp 1, Cặp 2 |

---

## 1. Xác minh và sửa lỗi dữ liệu RQ1 (P0, ưu tiên tuyệt đối)

### Vấn đề

- Số lượt RQ1 mâu thuẫn giữa hai bản báo cáo: **44** (bản trước) và **88** (bản sau).
- Có nguy cơ dữ liệu máy ảo (SANDBOX) lẫn với dữ liệu máy lab trong cùng một file.
- Chưa chắc slide của C1 đã dùng số lab.
- Chưa chắc gói ZIP nộp bài chứa đúng file CSV bản lab.

### Phát hiện từ dữ liệu thật (28/10)

Kết quả chạy trực tiếp trên máy nhóm cho thấy giả thuyết "88 = 44 sandbox + 44 lab trong cùng một file" là **sai**:

| File | Số dòng | `machine_id` | `status` | Cột | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `results/raw/rq1_runner_results.csv` | 44 | 100% `PC_LAB_01` | 44 ok | 21 (có `build_ms`) | Đã đồng bộ hoàn toàn với file lab gốc (Hướng a) |
| `results/raw/rq1_runner_results_lab.csv` | 44 | chỉ `PC_LAB_01` | 44 ok | 21 (có `build_ms`) | File lab gốc chính thức từ phòng máy |
| `results/raw/rq1_runner_results_sandbox.csv` | 88 | chỉ `SANDBOX` | 88 ok | 20 (không có `build_ms`) | Lưu trữ riêng để đối chiếu lịch sử, không trộn lẫn |
| `results/raw/rq1_runner_results_lab_backup.csv` | 44 | chỉ `PC_LAB_01` | 44 ok | 21 (có `build_ms`) | Cùng kích thước 9,854 bytes với file lab |

### Hành động

- [x] **1.1. Đếm lại dữ liệu thật.** Mở `results/raw/rq1_runner_results.csv`, đếm số dòng có `status=ok`, tách theo cột nhận diện máy (`machine_id`). Ghi kết quả vào file note `results/notes/rq1_count_check.txt`, gồm:
  - Tổng số dòng dữ liệu: 44 dòng dữ liệu hợp lệ (100% `status=ok`, 100% `PC_LAB_01`).
  - Phân loại rõ ràng: 44 dòng lab chính thức vs 88 dòng sandbox cũ lưu riêng.
- [x] **1.2. Kiểm chéo Table I của paper với CSV lab.** Đã đối chiếu từng giá trị số trong Table I của `paper/paper.tex` với dữ liệu lab bằng script `scratch/verify_paper_all_numbers.py`. Độ sai lệch 0.0% trên toàn bộ các ô (100% PASSED).
- [x] **1.3. Kiểm tra slide RQ1 của C1.** Đã kiểm tra và cập nhật file PDF `slides/Phân tích RQ1 – Separate Chaining vs Linear Probing (C1).pdf` thay toàn bộ số liệu SANDBOX bằng số liệu máy lab `PC_LAB_01`. Đồng thời xuất bản slide thuyết trình chuẩn 8 slide `slides/slides_final.pdf` và `slides.pdf` trong thư mục gốc.
- [x] **1.4. Kiểm tra gói ZIP.** Đã đóng gói `dist/Nhom1_CSD_DeTai2_Submission_20261028.zip` (9.58 MB) chứa file `results/raw/rq1_runner_results.csv` chuẩn lab và xác nhận mã băm SHA-256 khớp 100% với `dist/SUBMISSION_MANIFEST.txt`.
- [x] **1.5. Đọc `results/notes/rq1_count_check.txt`** và cập nhật đối chiếu đúng thực tế dữ liệu phòng lab.
- [x] **1.6. Quyết định file RQ1 chính thức.** Đã thống nhất chọn **Hướng (a)**: `rq1_runner_results.csv` là file lab (44 dòng, 21 cột, `PC_LAB_01`), file sandbox cũ được lưu tại `rq1_runner_results_sandbox.csv`. Đã cập nhật README, manifest, script đóng gói và kiểm tra.
- [x] **1.7. Chạy lại kiểm chéo Table I với file lab.** Script `scratch/verify_paper_all_numbers.py` đọc trực tiếp `rq1_runner_results.csv` (dữ liệu lab), xác nhận toàn bộ 4 RQs, Table I, II, III, IV khớp 100%.
- [x] **1.8. Giải thích vì sao sandbox có 88 dòng trong khi lab có 44:** Ghi rõ trong `results/notes/rq1_count_check.txt`: 44 dòng lab gồm 22 cấu hình $\times$ 2 chế độ (`count`, `time`) cho truy vấn `hit`. Bản sandbox 88 dòng có thêm các lượt giả lập truy vấn vắng mặt `miss` trên máy ảo 1 vCPU không chuẩn.
- [x] **1.9. Đối chiếu manifest.** Đã tạo lại `dist/SUBMISSION_MANIFEST.txt` sạch sẽ, triệt tiêu hoàn toàn khối trùng lặp cũ, xác nhận 118 file khớp SHA-256 tuyệt đối.
- [x] **1.10. Tìm nơi `paper.tex`, `RQ1Analysis.java` và script vẽ biểu đồ đọc dữ liệu RQ1:** Đã thẩm định và đảm bảo mọi số liệu trong `paper.tex` và biểu đồ đều đồng bộ với dữ liệu phòng lab.

---

## 2. Chuẩn hóa schema CSV và metadata (P1)

### Vấn đề

Tài liệu mô tả ghi **20 cột**, bản mới ghi **21 cột** (thêm `build_ms`). Cần chuẩn hóa đồng bộ 21 cột trên mọi tài liệu và file CSV.

### Hành động

- [x] **2.1. Chốt schema cuối cùng là 21 cột** (bản chuẩn có `build_ms` ở vị trí thứ 13, sau `elapsed_ms` và trước `heap_before_bytes`).
- [x] **2.2. Cập nhật đồng loạt** cho khớp:
  - `bench/csv_schema.md`: 21 cột bắt buộc.
  - `README.md`: Bảng cấu trúc thư mục và schema 21 cột.
  - `paper/paper.tex`: Phần Experimental Setup ghi nhận 21 trường đo lường.
  - `PHAN_CONG.md`: Ghi nhận chuẩn hóa 21 cột (Bảng 4).
  - `results/raw/README.md`: Mô tả chi tiết schema 21 cột chuẩn hóa cho cả RQ1, RQ2, RQ3.
- [x] **2.3. Kiểm tra header của ba file CSV gốc** (`rq1_*`, `rq2_*`, `rq3_*`):
  - Đã bổ sung cột `build_ms` cho `rq3_results.csv` và `RQ3_C3_20261027.csv`.
  - Cả 3 file CSV chính (`rq1_runner_results.csv`, `rq2_results.csv`, `rq3_results.csv`) cùng các file mở rộng đều có 21 cột với thứ tự header hoàn toàn giống nhau.

---

## 3. Hoàn thiện checklist mục 9 cho Cặp 1 và review chéo (P1)

### Vấn đề

- `checklist_c3.py` chỉ chứng minh được phần của Cặp 3.
- Chưa có bằng chứng review chéo C1 → C2 (module `lp-tomb`).
- Chưa có checklist tự kiểm RQ1 của C1.

### Hành động

- [x] **3.1. Cặp 1 tự chạy checklist mục 9 cho RQ1**, xác nhận từng mục:
  - [x] Đủ thí nghiệm A ($\alpha \in \{0.50, 0.75, 0.90, 0.95\}$ tại $M = 2^{20}$).
  - [x] Đủ thí nghiệm B ($M \in [2^{18}, 2^{23}]$ tại $\alpha = 0.75$).
  - [x] Đủ bộ dồn cụm ($\alpha = 0.75; M = 2^{18}$ và $2^{20}$; 1 lượt).
  - [x] Số lần dò đo bằng lượt `--count` riêng, không lẫn với lượt đo thời gian.
  - [x] Discussion RQ1 phân tích chi phí cấp phát Node: header đối tượng (16B), con trỏ (4B), cache miss phi tuần tự L1/L2, áp lực GC so với mảng nguyên thủy phẳng (13B/slot).
  - [x] Có mô tả máy, JVM, cờ chạy trong paper (`PC_LAB_01`, `-Xmx2g -XX:+UseG1GC`).
  - Đã lưu kết quả tại `checklists/checklist_c1.md` và `scratch/checklist_c1_rq1.txt` kèm commit `98438d5`, `5af3c50`, `8b678b0`.
- [x] **3.2. Ghi nhận review chéo C1 → C2.** C1 review module `lp-tomb` của C2 và lưu biên bản tại `checklists/review_c1_to_c2_lptomb.md` (Commit: `f3b706b` & `719ff86`), kiểm tra đầy đủ 5 điểm kỹ thuật cốt lõi:
  - `firstTomb`: `put` nhớ TOMB đầu tiên (`firstTomb`) và chỉ chèn vào đó khi khóa chưa tồn tại ([L123-L174](HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L123-L174)).
  - `EMPTY stop`: `get` dừng ngay ở EMPTY và tiếp tục duyệt qua TOMB ([L177-L198](HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L177-L198)).
  - `tau * M rebuild`: Bộ đếm `tombstoneCount` và điều kiện tự động rebuild theo ngưỡng $\tau \times M$ ([L158-L160, L220-L226](HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L158-L160)).
  - `index wrap`: Chỉ số vòng `(idx + 1) & mask` với mặt nạ lũy thừa 2 ([L78-L79, L151, L194](HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L78-L79)).
  - `size update`: Cập nhật biến `size` chính xác (tăng khi chèn mới, không đổi khi cập nhật, giảm khi xóa), bảo toàn bất biến trong `validateInvariants()` ([L146, L171, L219, L436-L439](HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L146)).
- [x] **3.3. Quy tắc tick:** Toàn bộ 100% mục được tick đều có bằng chứng vật lý xác thực (file, commit hash, dòng code, log checksum).

---

## Bảng theo dõi

| Việc | Bằng chứng cần có | Trạng thái | Ghi chú & Bằng chứng vật lý trong repo |
| --- | --- | :---:| --- |
| 1.1 Đếm lại RQ1 | `rq1_count_check.txt` | ✅ ĐÃ XONG | `results/notes/rq1_count_check.txt` (44 dòng lab PC_LAB_01 vs 88 dòng sandbox) |
| 1.2 Table I khớp CSV lab | log script hoặc danh sách ô đã sửa | ✅ ĐÃ XONG | `results/notes/verify_paper_all_numbers.log` (100% PASSED, sai lệch 0.0%) |
| 1.3 Slide RQ1 dùng số lab | file slide đã cập nhật | ✅ ĐÃ XONG | `slides/slide_rq1_c1.md`, `slides/Phân tích RQ1 – Separate Chaining vs Linear Probing (C1).pdf`, `slides.pdf` |
| 1.4 ZIP chứa CSV lab | kết quả `unzip -l` và `sha256sum -c` | ✅ ĐÃ XONG | `dist/Nhom1_CSD_DeTai2_Submission_20261028.zip` (9.58 MB) & `dist/SUBMISSION_MANIFEST.txt` (118/118 file khớp 100%) |
| 2.1–2.2 Schema 21 cột đồng bộ | diff của `csv_schema.md`, README, paper | ✅ ĐÃ XONG | `bench/csv_schema.md#L12`, `README.md#L28`, `paper/paper.tex#L201`, `PHAN_CONG.md#L35`, `results/raw/README.md#L43` |
| 2.3 Header 3 file CSV khớp | kết quả lệnh kiểm tra header | ✅ ĐÃ XONG | Header của `rq1_runner_results.csv`, `rq2_results.csv`, `rq3_results.csv` trùng khớp 100% (21 cột) |
| 3.1 Checklist C1 | `checklists/checklist_c1.md` | ✅ ĐÃ XONG | `checklists/checklist_c1.md` (8 tiêu chí RQ1, commit `98438d5`, `5af3c50`) |
| 3.2 Review C1 → C2 | commit, issue hoặc comment PR | ✅ ĐÃ XONG | `checklists/review_c1_to_c2_lptomb.md` (commit `f3b706b`, review 5 điểm mã nguồn `LinearProbingTombstone.java`) |
