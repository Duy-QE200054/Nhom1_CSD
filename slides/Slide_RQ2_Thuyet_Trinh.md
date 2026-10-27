# BÁO CÁO THỰC NGHIỆM RQ2 - CẶP 2 (SV 3 & SV 4)
## Đề tài 2: Chỉ mục tra cứu bằng Hash Table (Tombstone, Rebuild & Robin Hood)

---

## 1. NỘI DUNG TRÌNH CHIẾU SLIDE 6 (Dành cho Slide 6 trong bộ 8 slide)

### Tiêu đề Slide:
**KẾT QUẢ & PHÂN TÍCH RQ2: HIỆN TƯỢNG TÍCH TỤ TOMBSTONE VÀ VAI TRÒ CỦA REBUILD**

### Bố cục Slide (3 Panel trực quan):
* **Hình ảnh trung tâm**: Chiếu trực tiếp `results/figures/fig_rq2_tombstone_dynamics.png` (gồm 3 đồ thị con a, b, c).
* **Bảng số liệu đinh (Table I trích xuất)**:

| Mốc Churn $C$ | Tỷ lệ TOMB | Tỷ lệ EMPTY | Hit Probes (Base vs Clean) | Miss Probes (Base vs Clean) | Miss Latency (ms) | Tăng vọt (Surge) |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| $C = 0$ | 0.00% | 50.00% | 1.502 vs 1.502 | 2.499 vs 2.499 | 22.75 ms | **1.0$\times$** (Chuẩn Knuth) |
| $C = 0.50M$ | 30.16% | 19.84% | 2.043 vs 1.495 | 7.461 vs 2.488 | 32.50 ms | **3.0$\times$** |
| $C = 1.00M$ | 41.91% | 8.09% | 2.229 vs 1.500 | 17.235 vs 2.502 | 51.21 ms | **6.9$\times$** |
| $C = 2.00M$ | 48.64% | 1.36% | 2.330 vs 1.502 | 86.089 vs 2.501 | 297.37 ms | **34.4$\times$** |
| $C = 4.00M$ | **49.96%** | **0.04%** | 2.345 vs 1.503 | **2,843.37** vs 2.499 | **5,847.48 ms** | **1,137.9$\times$** |

### 3 Luận điểm then chốt trên Slide:
1. **Sự cạn kiệt ô rỗng (Empty Slot Depletion)**: Sau 4M chu kỳ churn, tombstone chiếm $49.96\%$ dung lượng bảng (chiếm $99.92\%$ phần khoảng trống khả dụng). Ô \texttt{EMPTY} bị triệt tiêu từ $524,288$ ô ($50\%$) xuống chỉ còn **$412$ ô ($0.04\%$)**.
2. **Sự phân kỳ bất đối xứng (Asymmetric Probe Divergence)**:
   - *Tra cứu có mặt (Hit)*: Chỉ tăng nhẹ từ $1.50$ lên $2.35$ bước dò ($1.56\times$) do dừng ngay khi thấy khóa.
   - *Tra cứu thất bại (Miss)*: Bùng nổ thảm họa từ $2.50$ lên **$2,843.37$ bước dò** (tăng vọt **$1,137.9\times$**), kéo theo độ trễ tăng **$233.3\times$** ($22.75 \to 5,847.48$ ms).
3. **Bằng chứng từ Bảng đối chứng (Clean Table Immunity)**: Bảng dựng mới từ cùng tập khóa sống duy trì bất biến $1.50$ hit probes và $2.50$ miss probes (khớp cận lý thuyết Knuth). Điều này chứng minh $100\%$ suy thoái hiệu năng là do "rác tombstone".

---

## 2. KỊCH BẢN THUYẾT TRÌNH CỦA CẶP 2 (Thời lượng: 60 - 90 giây)

> *"Kính thưa Thầy/Cô và Hội đồng, em đại diện Cặp 2 xin báo cáo kết quả thực nghiệm của Câu hỏi nghiên cứu 2: **Hiện tượng tích tụ Tombstone ảnh hưởng thế nào nếu không Rebuild?**
>
> Để trả lời RQ2 một cách khách quan nhất, chúng em thiết lập bảng Linear Probing dung lượng $M = 2^{20}$ (hơn 1 triệu ô) ở tải sống $50\%$. Sau đó, chạy mô hình Churn liên tục từ $0$ đến $4$ triệu chu kỳ thêm-xóa. Tại mỗi mốc, chúng em đo đạc song song bảng bị ô nhiễm và một **Bảng đối chứng Clean Table** dựng mới từ đúng tập khóa đó.
>
> Kết quả thực nghiệm mang lại 3 phát hiện kỹ thuật cốt lõi:
> 1. **Thứ nhất**, khi không rebuild, số ô EMPTY suy giảm theo hàm mũ, từ $50\%$ rơi tự do xuống chỉ còn **$0.04\%$ (đúng 412 ô trống)** tại mốc 4M chu kỳ. Nghĩa là $99.92\%$ khoảng trống của bảng đã bị biến thành bia mộ tombstone.
> 2. **Thứ hai**, phát hiện chấn động nhất là **sự phân kỳ bất đối xứng**: Trong khi truy vấn có mặt (Hit) chỉ tăng nhẹ từ $1.5$ lên $2.3$ bước dò rồi đi ngang, thì truy vấn thất bại (Miss) bùng nổ khủng khiếp: tăng từ $2.5$ lên tới **$2,843$ bước dò – tức tăng gấp $1,137$ lần!** Lý do là vì giải thuật Linear Probing bắt buộc phải dò xuyên qua toàn bộ các cụm tombstone dày đặc cho tới khi gặp ô EMPTY thì mới dám khẳng định khóa không tồn tại. Kéo theo độ trễ tăng vọt $233$ lần.
> 3. **Thứ ba**, Bảng đối chứng Clean Table của chúng em luôn duy trì phẳng lì ở $1.5$ hit và $2.5$ miss probes qua mọi chu kỳ, trùng khớp hoàn hảo với công thức lý thuyết của Knuth.
>
> Về biên an toàn timeout: Ở mốc nặng nhất 4M chu kỳ, bảng chạy mất $5.85$ giây, nằm an toàn dưới ngưỡng timeout $60$ giây của đề bài. Tuy nhiên, về mặt tiệm cận, khi $C \to \infty$, số ô rỗng tiến về $0$, độ phức tạp tra cứu thất bại sẽ trượt từ $O(1)$ thành quét tuyến tính $O(M)$. Do đó, **cơ chế Rebuild định kỳ theo ngưỡng $\tau \cdot M$ (với $\tau$ tối ưu từ $0.10$ đến $0.20$) là giải pháp kỹ thuật bắt buộc** để triệt tiêu tombstone và bảo vệ hệ thống. Em xin cảm ơn!"*

---

## 3. LỆNH CHẠY LẠI THỰC NGHIỆM TẠI BUỔI BẢO VỆ (LIVE DEMO < 60 GIÂY)

Khi giảng viên yêu cầu chạy lại để đối chiếu kết quả thực tế trên máy:

### Demo 1: Chạy kiểm chứng Mốc $C = 65,536$ hoặc $C = 262,144$ (Thời gian chạy: ~1 giây)
```bash
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ2 --algo lp-tomb --m 1048576 --alpha 0.50 --c 262144 --seed 42 --mode time
```
*Kết quả đối chiếu:* `elapsed_ms` $\approx 31$ ms, `ops_count` (lượt count): $1.8517$ hit probes, $4.6223$ miss probes. Trạng thái `ok`.

### Demo 2: In trực tiếp Bảng so sánh Đối chứng Contaminated Table vs Clean Table
```bash
java -Xmx2g -jar HashTableProject/dist/HashTableProject.jar --rq RQ2 --algo lp-tomb --m 262144 --alpha 0.50 --c 65536 --seed 42 --compare-clean
```
*Hệ thống sẽ in bảng định dạng chuẩn đối chiếu số ô TOMB, EMPTY, Hit probes, Miss probes giữa hai bảng.*

### Demo 3: Chạy lại toàn bộ Unit Test & Oracle Test của Cặp 2 (Thời gian: ~2 giây)
```bash
java -cp "HashTableProject/build/classes;HashTableProject/build/test/classes" hashtable.test.C2HashTableTest
```
*Kết quả:* Vượt qua 100% 6 ca test biên và Oracle 1.000.000 thao tác đối chiếu `java.util.HashMap`.

---

## 4. BỘ CÂU HỎI VẤN ĐÁP PHẢN BIỆN DÀNH CHO CẶP 2 & CÂU TRẢ LỜI ĐẠT ĐIỂM TỐI ĐA

### Câu 1: Tại sao khi tích tụ tombstone, truy vấn vắng mặt (Miss) lại tăng vọt $1,137$ lần trong khi truy vấn có mặt (Hit) chỉ tăng $1.56$ lần?
* **Trả lời chuẩn:**
  - Với **Hit query**: Phép tìm kiếm dừng lại ngay lập tức khi tìm thấy khóa trùng khớp (`state == OCCUPIED && keys[i] == key`). Do đó, nó chỉ phải đi qua các tombstone nằm chen giữa bucket băm gốc và vị trí của khóa đó trong cụm cục bộ.
  - Với **Miss query**: Trong Linear Probing, để kết luận một khóa không có trong bảng, giải thuật bắt buộc phải dò tuần tự qua tất cả các ô TOMB cho đến khi chạm được ô `EMPTY` đầu tiên. Khi $C = 4M$, số ô `EMPTY` chỉ còn $0.04\%$ ($412$ ô trên 1 triệu ô), các cụm tombstone liên kết lại thành các chuỗi khổng lồ kéo dài hàng ngàn ô, buộc thuật toán phải duyệt qua trung bình $2,843$ ô mới gặp được ô rỗng.

### Câu 2: Bảng đối chứng (Clean Table) có vai trò gì trong phương pháp luận của RQ2?
* **Trả lời chuẩn:**
  - Khi chạy 4 triệu chu kỳ churn, tập khóa sống liên tục bị thay thế. Nếu chỉ đo trên bảng tombstone, giảng viên có thể nghi ngờ rằng: "Biết đâu do tập khóa mới sinh ra bị đụng độ băm xấu hơn nên số bước dò mới tăng?".
  - Bảng Clean Table lấy chính xác $100\%$ các khóa sống hiện có tại mốc đó, chèn vào một bảng hoàn toàn mới không có tombstone. Kết quả Clean Table luôn duy trì cố định $1.50$ hit probes và $2.50$ miss probes (đúng bằng lý thuyết Knuth). Điều này cung cấp bằng chứng thực nghiệm thép cô lập biến số: chứng minh $100\%$ sự suy thoái hiệu năng là do sự cản trở của các ô Tombstone gây ra, chứ không phải do tập khóa.

### Câu 3: Tại sao trong cài đặt `put()` của `lp-tomb`, bắt buộc phải dò tiếp qua ô TOMB chứ không được chèn ngay vào ô TOMB đầu tiên nhìn thấy?
* **Trả lời chuẩn:**
  - Đây là bẫy phổ biến nhất của Linear Probing có Tombstone: Nếu ta gặp một ô TOMB và vội vã ghi đè ngay khóa mới vào đó, ta sẽ không biết được liệu khóa đó đã từng được chèn trước đó và đang nằm ở phía sau ô TOMB này hay không.
  - Nếu khóa đã tồn tại ở phía sau, hành động chèn ngay sẽ tạo ra **khóa bị nhân đôi (duplicate key)** và làm sai lệch kích thước bảng `size`.
  - Quy tắc đúng (chuẩn Algorithm của nhóm): Ghi nhớ vị trí `firstTomb = idx`, tiếp tục dò tiếp. Nếu gặp khóa đó ở ô sau $\to$ cập nhật giá trị và không tăng size. Nếu gặp ô `EMPTY` (chắc chắn khóa chưa từng có) $\to$ lúc này mới quay lại chèn vào `firstTomb` đã nhớ.

### Câu 4: Cơ chế Rebuild định kỳ của Cặp 2 hoạt động thế nào? Tại sao phải duyệt theo thứ tự chỉ số ô $0 \to M-1$? Ngưỡng $\tau$ tối ưu là bao nhiêu?
* **Trả lời chuẩn:**
  - Bộ đếm `tombstoneCount` được cập nhật sau mỗi phép `remove` và `put`. Khi `tombstoneCount >= tau * M`, bảng tự động kích hoạt hàm `rebuild()`.
  - Bảng duyệt tuần tự qua mảng cũ theo đúng chỉ số ô từ $0$ đến $M-1$, nhặt các ô `OCCUPIED` và chèn lại vào mảng mới sạch hoàn toàn tombstone.
  - Duyệt theo thứ tự chỉ số ô $0 \to M-1$ là yêu cầu bắt buộc của đề bài nhằm đảm bảo tính tái lập (determinism) và giữ nguyên thứ tự tương đối của các cụm khóa.
  - Ngưỡng tối ưu: Qua thực nghiệm, $\tau \in [0.10, 0.20]$ là vùng đánh đổi tối ưu nhất: dọn sạch tombstone giúp số bước dò miss luôn được chặn dưới $4.0$, trong khi chi phí tái cấu trúc $O(N)$ được phân bổ đều (amortized) qua hàng chục ngàn thao tác.

### Câu 5: Tại sao nhóm không dùng cách xóa dịch lùi của Sedgewick (chèn lại cả cụm) cho Linear Probing mà lại dùng Knuth Algorithm R trong C3, và so với `lp-tomb` của C2 thì phương án nào tốt hơn?
* **Trả lời chuẩn:**
  - `lp-tomb` có ưu điểm là thao tác xóa cực nhanh ($O(1)$ vì chỉ cần đánh dấu `state = TOMB`), nhưng trả giá bằng việc làm chậm tra cứu về sau và phải tốn chi phí rebuild định kỳ gây đột biến độ trễ (latency spikes).
  - Thuật toán của Sedgewick xóa một khóa rồi chèn lại từng khóa phía sau bằng phép `put` thông thường, gây ra nhiều lần tính lại hàm băm và duyệt thừa.
  - Cặp 2 và Cặp 3 đã phối hợp đối chiếu: Knuth Algorithm R chỉ duyệt cụm một lần duy nhất theo độ dài cụm $K$, dịch chuyển khóa vào ô trống khi vị trí băm gốc cho phép, vừa không tạo tombstone vừa có độ phức tạp chặt chẽ $O(K)$.
