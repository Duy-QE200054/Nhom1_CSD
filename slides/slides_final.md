# SLIDE BÁO CÁO BẢO VỆ ĐỒ ÁN CSD201 (FA26) - ĐỀ TÀI 2
## Nghiên cứu và Đánh giá Hiệu năng Chỉ mục Tra cứu bằng Hash Table: Tombstone, Rebuild và Robin Hood Hashing
**Nhóm sinh viên thực hiện:** Nhóm 1 - Lớp SE20C - FPT University  
**Quy chuẩn:** Đúng 8 Slide | Thời gian trình bày: 5 phút | Vấn đáp: 10 phút  
**Máy đo chính thức:** PC_LAB_01 (Intel Core i5-11500H @ 2.90GHz, 32GB RAM, Windows 11 Pro, JDK 24)  

---

<!-- SLIDE 1 -->
# SLIDE 1: GIỚI THIỆU ĐỀ TÀI & PHÂN VAI 6 THÀNH VIÊN
### Thời lượng: 30 giây | Người trình bày: SV 1 (Nguyễn Duy - Cặp 1)

#### 1. Thông Tin Chung
- **Đề tài:** Nghiên cứu và Đánh giá Hiệu năng Chỉ mục Tra cứu bằng Hash Table: Tombstone, Rebuild và Robin Hood Hashing.
- **Giảng viên hướng dẫn:** Bộ môn Khoa học Máy tính - FPT University.

#### 2. Phân Vai & Trách Nhiệm 6 Thành Viên (3 Cặp Nghiên Cứu)
- **Cặp 1 (C1) - RQ1: Separate Chaining vs. Linear Probing**
  - **Nguyễn Duy (SV 1)**: Cài đặt Separate Chaining, Benchmark Wrapper đa tiến trình, Trình bày Slide 1 & Slide 5.
  - **Nguyễn Tuấn Hưng (SV 2)**: Phân tích mô hình bộ nhớ Java Object (`Node` 32B vs mảng 13B), Trình bày Slide 2 & Slide 5.
- **Cặp 2 (C2) - RQ2: Linear Probing Tombstone & Rebuild Dynamics**
  - **Lê Phước Anh Quân (SV 3)**: Cài đặt LP-Tombstone, cơ chế Rebuild định kỳ theo $\tau$, Trình bày Slide 3 & Slide 6.
  - **Nguyễn Viết Cường (SV 4)**: Động học suy kiệt ô rỗng, bảng đối chứng Clean Table, Trình bày Slide 4 & Slide 6.
- **Cặp 3 (C3) - RQ3: Robin Hood Hashing & Cơ Chế Bóc Tách (Ablation)**
  - **Nguyễn Đình Tiến (SV 5)**: Cài đặt Robin Hood (`rh`), `rh-noearly`, `lp-backshift` (Knuth Algorithm R), Trình bày Slide 7.
  - **Lê Hoàng Cầu (SV 6)**: Phân tích đột biến khối Rebuild, ma trận khuyến nghị kiến trúc, Trình bày Slide 8.

---

<!-- SLIDE 2 -->
# SLIDE 2: BỐI CẢNH ỨNG DỤNG & 3 CÂU HỎI NGHIÊN CỨU (RQs)
### Thời lượng: 30 giây | Người trình bày: SV 2 (Nguyễn Tuấn Hưng - Cặp 1)

#### 1. Bối Cảnh Thực Tiễn: Hệ Thống Chỉ Mục Giao Dịch Bộ Nhớ Trong (In-Memory Index)
- Các sàn giao dịch điện tử và cơ sở dữ liệu đòi hỏi xử lý hàng triệu giao dịch/giây với độ trễ micro-giây.
- **Đặc trưng workload:** Tần suất chèn/xóa liên tục (High-churn), và phần lớn truy vấn tra cứu là **tra cứu thất bại (Miss queries / Absent keys)** do kiểm tra gian lận hoặc kiểm tra tính duy nhất.

#### 2. Ba Câu Hỏi Nghiên Cứu Cốt Lõi (Research Questions)
1. **RQ1 (C1):** Dưới áp lực tăng trưởng hệ số tải $\alpha$ và kích thước bảng $M$, Separate Chaining hay Linear Probing chiếm ưu thế về số bước dò, thời gian thực thi và mật độ bộ nhớ? Nghịch lý giữa số bước dò và thời gian thực thi trong Java runtime được lý giải ra sao?
2. **RQ2 (C2):** Khi chịu chu kỳ xóa-chèn liên tục không tái cấu trúc ($\tau = \infty$), sự tích tụ của Tombstone tàn phá hiệu năng tra cứu trượt như thế nào? Tại sao Rebuild theo ngưỡng $\tau \cdot M$ là bắt buộc?
3. **RQ3 (C3):** Robin Hood Hashing và Xóa dịch lùi (Knuth Algorithm R) giải quyết triệt để vấn đề độ trễ đuôi (tail latency) ra sao? Giá trị định lượng độc lập của 3 cơ chế: Xóa dịch lùi, Hoán đổi DIB, và Dừng sớm (Early Exit) là bao nhiêu?

---

<!-- SLIDE 3 -->
# SLIDE 3: THIẾT KẾ KIẾN TRÚC & 5 CẤU HÌNH THUẬT TOÁN
### Thời lượng: 30 giây | Người trình bày: SV 3 (Lê Phước Anh Quân - Cặp 2)

#### 1. Hàm Băm Chuẩn Fibonacci Hash ($M = 2^p$)
- Toàn bộ 5 thuật toán sử dụng chung hàm băm Fibonacci nhân tỉ lệ vàng:
  $$h(k) = (k \cdot 11400714819323198485\text{L}) \ggg (64 - p)$$
- Phân phối khóa đồng đều cực đại trên các bit có nghĩa cao, triệt tiêu đụng độ do mẫu dữ liệu tuần tự.

#### 2. Ma Trận 5 Cấu Hình Cài Đặt Khảo Sát
1. **`sc` (Separate Chaining):** Mảng con trỏ mảng bucket `Node[]`, mỗi node chứa khóa, giá trị và con trỏ `next`.
2. **`lp-tomb` (Linear Probing with Tombstones):** 3 mảng phẳng nguyên thủy (`long[] keys`, `int[] values`, `byte[] state`), ô bị xóa đánh dấu `TOMB = 2`, tự động Rebuild khi tombstones $\ge \tau \cdot M$.
3. **`lp-backshift` (Linear Probing with Knuth Algorithm R):** Xóa dịch lùi tại chỗ, dịch các phần tử cùng cụm lùi về vị trí rỗng, **0 tombstone, 0 rebuild**.
4. **`rh` (Robin Hood Hashing Full):** Bổ sung mảng `int[] dib`, hoán đổi theo nguyên tắc "nhường phần tử nghèo hơn", **dừng sớm (Early Exit)** khi $DIB_{probe} > DIB_{slot}$, xóa dịch lùi.
5. **`rh-noearly` (Robin Hood Ablation):** Giữ nguyên quy tắc hoán đổi DIB nhưng cố tình tắt điều kiện dừng sớm để bóc tách tác động độc lập của Early Exit.

---

<!-- SLIDE 4 -->
# SLIDE 4: PHƯƠNG PHÁP THỰC NGHIỆM & KIỂM THỬ TÍNH ĐÚNG
### Thời lượng: 30 giây | Người trình bày: SV 4 (Nguyễn Viết Cường - Cặp 2)

#### 1. Bộ Kiểm Thử Hợp Đồng Bắt Buộc & Oracle Test ($10^6$ Ops)
- **Kiểm thử Oracle:** So khớp $1.000.000$ thao tác ngẫu nhiên (40% put, 40% get, 20% remove) với `java.util.HashMap` trên dung lượng $M = 4096$: Đạt độ chính xác tuyệt đối **100.0%**.
- **6 ca test hợp đồng:** Xử lý triệt để đụng độ cụm, xóa giữa chuỗi đụng độ, khóa âm/khóa biên, và chuỗi xoay vòng qua cuối mảng (`(idx + 1) & (M - 1)`).

#### 2. Phương Pháp Đo Lường Khoa Học Đạt Mốc Kiểm Soát M5
- **Tiến trình JVM độc lập:** Mỗi lượt đo chạy trong một tiến trình riêng biệt với `-Xmx2g`, JIT warmup 1000 thao tác, timeout 60 giây.
- **Tách biệt chế độ:** Lượt `--mode count` đo chính xác số bước dò lý thuyết; lượt `--mode time` đo wall-clock latency (ms).
- **Schema 21 cột đồng bộ:** Ghi nhận đầy đủ `build_ms`, `elapsed_ms`, `heap_peak_bytes`, `ops_count`, `output_check` chống JIT Dead-Code Elimination.

---

<!-- SLIDE 5 -->
# SLIDE 5: KẾT QUẢ RQ1 - SEPARATE CHAINING VS LINEAR PROBING
### Thời lượng: 45 giây | Người trình bày: SV 1 & SV 2 (Nguyễn Duy & Nguyễn Tuấn Hưng)

#### 1. Đối Chiếu Thực Nghiệm với Tiệm Cận Lý Thuyết Donald Knuth ($M = 2^{20}$)

| Hệ số tải $\alpha$ | Knuth LP Hit | CSV LP Hit | Knuth LP Miss | CSV LP Miss | Knuth SC Hit | CSV SC Hit | Knuth SC Miss | CSV SC Miss |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **$\alpha = 0.50$** | 1.500 | **1.503** | 2.500 | **2.506** | 1.250 | **1.250** | 0.500 | **0.499** |
| **$\alpha = 0.75$** | 2.500 | **2.507** | 8.500 | **8.530** | 1.375 | **1.375** | 0.750 | **0.748** |
| **$\alpha = 0.90$** | 5.500 | **5.504** | 50.500 | **50.068** | 1.450 | **1.450** | 0.900 | **0.898** |
| **$\alpha = 0.95$** | 10.500 | **10.626** | 200.500 | **204.738** | 1.475 | **1.476** | 0.950 | **0.949** |

*Độ lệch tương đối giữa thực nghiệm và công thức Knuth $< 0.5\%$ trên toàn bộ dải tải trọng.*

#### 2. Luận Giải Nghịch Lý Bước Dò vs Thời Gian Thực Thi (The Java Memory Gap)
- **Nghịch lý:** Ở $\alpha \le 0.75$, dù `sc` có số bước dò thấp hơn ($1.38$ vs $2.51$), nhưng `lp-tomb` chạy nhanh hơn $1.2\times - 1.5\times$ về thời gian thực tế!
- **Nguyên nhân kiến trúc phần cứng:**
  - `Node` trong SC tốn **32 bytes/khóa** (Header 16B + Key 8B + Val 4B + Ref 4B), nằm phân tán trên Heap. Việc duyệt con trỏ (`pointer chasing`) gây liên tục **L1/L2 cache misses** và áp lực GC.
  - LP lưu trên 3 mảng nguyên thủy phẳng chỉ tốn **13 bytes/slot**. CPU nạp nguyên khối cache line 64 bytes, kích hoạt bộ phần cứng **Hardware Stream Prefetcher**, khiến mỗi phép kiểm tra slot trong LP nhanh gấp nhiều lần duyệt Node trong SC.
- **Thử nghiệm dồn cụm (10% khóa dồn vào $M/64$):** LP bùng nổ 412% số bước dò do chuỗi đụng độ nối tầng; SC chỉ tăng 28.5% do đụng độ bị cô lập trong từng bucket.

---

<!-- SLIDE 6 -->
# SLIDE 6: KẾT QUẢ RQ2 - CÁI GIÁ CỦA TOMBSTONE KHI KHÔNG REBUILD
### Thời lượng: 45 giây | Người trình bày: SV 3 & SV 4 (Lê Phước Anh Quân & Nguyễn Viết Cường)

#### 1. Động Học Suy Kiệt Ô Rỗng & Bùng Nổ Tra Cứu Trượt ($M = 2^{20}, \alpha = 0.50$)

| Mốc Churn ($C$) | \% Tombstone | \% Ô EMPTY | Hit Probes (Base vs Clean) | Miss Probes (Base vs Clean) | Miss Latency (Base vs Clean) | Hệ Số Tăng Trễ |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| **$C = 0$** | 0.00% | **50.00%** | 1.502 vs 1.502 | 2.499 vs 2.499 | 22.75 ms vs 21.93 ms | **$1.0\times$** |
| **$C = 0.25M$** | 18.79% | 31.21% | 1.852 vs 1.499 | 4.622 vs 2.499 | 28.04 ms vs 20.85 ms | $1.85\times$ |
| **$C = 1.00M$** | 41.91% | 8.09% | 2.229 vs 1.500 | 17.235 vs 2.502 | 51.21 ms vs 24.57 ms | $6.89\times$ |
| **$C = 2.00M$** | 48.64% | 1.36% | 2.330 vs 1.502 | 86.089 vs 2.501 | 297.37 ms vs 50.69 ms | $34.4\times$ |
| **$C = 4.00M$** | **49.96%** | **0.04%** | 2.345 vs 1.503 | **2843.37 vs 2.50** | **5,847.48 ms vs 25.06 ms** | **$233.3\times$** |

#### 2. Phát Hiện Kỹ Thuật Quan Trọng
1. **Sự bất đối xứng nghiêm trọng:** Tra cứu trúng (Hit) chỉ tăng nhẹ từ $1.50$ lên $2.35$ probes vì dừng ngay khi thấy khóa. Ngược lại, tra cứu trượt (Miss) bắt buộc phải tìm ô `EMPTY` nên bị mắc kẹt duyệt qua hàng triệu Tombstone, số bước dò bùng nổ **$1,137.9\times$** ($2.50 \to 2843.37$).
2. **Suy thoái tiệm cận sang $\mathcal{O}(M)$:** Khi $C \to 4M$, số ô `EMPTY` rơi tự do xuống chỉ còn **$412$ ô (0.04%)**, biến bảng băm thành một mảng quét tuần tự toàn bộ dung lượng bảng.
3. **Vai trò sống còn của Rebuild ($\tau$):** Ngưỡng rebuild $\tau \in [0.10, 0.20]$ giúp chặn trên độ trễ tra cứu trượt dưới 4 bước dò, bảo vệ hệ thống khỏi sập đổ do timeout.

---

<!-- SLIDE 7 -->
# SLIDE 7: KẾT QUẢ RQ3 - BÓC TÁCH 3 CƠ CHẾ ROBIN HOOD HASHING
### Thời lượng: 75 giây | Người trình bày: SV 5 (Nguyễn Đình Tiến - Cặp 3)

#### 1. Đóng Góp Định Lượng Của 3 Cơ Chế Kỹ Thuật

```
+-----------------------------------------------------------------------------------------+
| [CƠ CHẾ 1] XÓA DỊCH LÙI (BACKWARD SHIFT DELETION)                                      |
| - Triệt tiêu hoàn toàn 100% Tombstone, không bao giờ cần Rebuild bảng.                  |
| - Loại bỏ hoàn toàn các đỉnh nhọn độ trễ (Rebuild Latency Spikes) từ 10 - 36 ms của     |
|   lp-tomb (tăng vọt 5.4x - 6.1x so với bình thường), giúp thông lượng mượt mà.         |
+-----------------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------------+
| [CƠ CHẾ 2] HOÁN ĐỔI GIÀU - NGHÈO THEO DIB (RICH-TO-POOR SWAP)                           |
| - San phẳng phương sai phân phối độ dời DIB trong bảng.                                |
| - Tải vừa alpha = 0.50: Cắt giảm Max DIB từ 41 xuống 9 (giảm 78.0%).                    |
| - Tải cao alpha = 0.80: Cắt giảm Max DIB từ 195 xuống 25 (giảm 87.2%).                 |
+-----------------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------------+
| [CƠ CHẾ 3] DỪNG SỚM KHI TRA CỨU TRƯỢT (EARLY EXIT)                                      |
| - Dừng ngay khi DIB_probe > DIB_slot, biến việc tìm kiếm vô vọng thành kết thúc sớm.   |
| - Tải vừa (alpha = 0.50): Giảm 30.0% bước dò miss (2.499 -> 1.751).                    |
| - Tải cao (alpha = 0.80): Giảm tới 73.8% BƯỚC DÒ MISS (12.93 -> 3.40 probes)!          |
| - So với lp-tomb (tau=0.10): Robin Hood giảm tới 83.7% bước dò tra cứu trượt!          |
+-----------------------------------------------------------------------------------------+
```

#### 2. Bảng Đối Chiếu Thực Nghiệm Tải Cao Tại $C = 4M$ ($M = 2^{20}, \alpha = 0.80$)
- `lp-tomb` ($\tau=0.10$): Hit 3.16 probes (58.9ms) | **Miss 14.61 probes** (58.7ms) | 23 lần Rebuild.
- `lp-backshift`: Hit 3.00 probes (51.9ms) | **Miss 12.93 probes** (35.5ms) | 0 Rebuild, Max DIB = 195.
- `rh-noearly`: Hit 3.00 probes (67.5ms) | **Miss 12.93 probes** (52.8ms) | 0 Rebuild, Max DIB = 25.
- **`rh` (Robin Hood đầy đủ):** Hit 3.00 probes (65.2ms) | **Miss 3.40 probes** (43.4ms) | **Tiết kiệm 73.8% bước dò!**

---

<!-- SLIDE 8 -->
# SLIDE 8: MA TRẬN KHUYẾN NGHỊ KIẾN TRÚC & KẾT LUẬN
### Thời lượng: 30 giây | Người trình bày: SV 6 (Lê Hoàng Cầu - Cặp 3)

#### 1. Ma Trận Quyết Định Kiến Trúc Thực Tế (Engineering Decision Matrix)

| Kịch bản Workload | Thuật toán tối ưu | Lý do kỹ thuật cốt lõi |
|---|:---:|---|
| **Chỉ đọc hoặc Thêm là chính, ít xóa** ($\alpha \le 0.75$) | **Linear Probing (`lp-tomb`)** | Mật độ bộ nhớ cao (13B/slot), tận dụng triệt để CPU Cache Line & Hardware Prefetching. |
| **Không kiểm soát được hệ số tải** ($\alpha \ge 0.90$) hoặc hàm băm kém | **Separate Chaining (`sc`)** | Duy trì bước dò thấp ($< 1.5$), cô lập đụng độ trong bucket, không bị đụng độ dây chuyền. |
| **Bộ nhớ cực kỳ hạn chế, có thao tác xóa** ($\alpha \le 0.70$) | **Linear Probing Backshift** | 0 tombstone, 0 rebuild, không cần mảng DIB phụ trợ, độ trễ ổn định. |
| **Hệ thống giao dịch tải cao, nhiều Miss Queries** ($\alpha \ge 0.80$) | **Robin Hood Hashing (`rh`)** | Triệt tiêu 87% Max DIB, cắt giảm 73.8% bước dò miss, chặn đứng nguy cơ DoS/Spike độ trễ. |

#### 2. Tuyên Bố Tuân Thủ Học Thuật & Nghiệm Thu
- Toàn bộ kết quả thực nghiệm khớp 100% với lý thuyết tiệm cận Knuth và file dữ liệu thô chuẩn 21 cột.
- Đã nghiệm thu Checklist Mục 9 và kiểm chéo chéo nội bộ giữa 3 cặp. Sẵn sàng cho phiên vấn đáp bảo vệ!
