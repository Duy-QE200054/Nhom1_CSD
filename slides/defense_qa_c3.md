# BỘ CÂU HỎI PHẢN BIỆN VÀ HƯỚNG DẪN TRẢ LỜI CHO CẶP 3 (C3)
## Đề tài: Hash Table - Tombstone, Rebuild và Robin Hood Hashing
**Thành viên C3**: Nguyễn Đình Tiến (`nguyendinhtien824-super`) & Lê Hoàng Cầu  
**Mục đích**: Chuẩn bị đầy đủ cơ sở lý thuyết, công thức toán học và số liệu thực chứng để bảo vệ tự tin trước Hội đồng chấm đồ án.

---

### CÂU HỎI 1: VÌ SAO ROBIN HOOD LẠI GIẢM ĐƯỢC PHƯƠNG SAI ĐỘ DÀI DÒ (PROBE VARIANCE REDUCTION)?

#### Bản chất lý thuyết & Cơ chế:
- Trong **Linear Probing thông thường**, các phần tử chèn trước chiếm giữ các vị trí gần home bucket nhất, đẩy các phần tử chèn sau (nếu bị đụng độ liên tiếp) ngày càng ra xa. Điều này tạo nên hiện tượng dồn cụm sơ cấp (primary clustering) với phân phối độ dài dò bị lệch phải nặng nề (heavy-tailed distribution): đa số phần tử có số bước dò nhỏ (1 - 2), nhưng một số ít phần tử "không may mắn" bị trôi xa với độ sâu dò hàng chục hoặc hàng trăm ô (như Max DIB = 195 trong thực nghiệm).
- **Robin Hood Hashing** áp dụng nguyên lý: *"Lấy của người giàu (phần tử có DIB nhỏ) chia cho người nghèo (phần tử có DIB lớn)"*. 
  + Khi chèn một khóa mới $k_{new}$ với độ dời hiện tại là $DIB_{new}$, tại mỗi ô đã bị chiếm bởi $k_{old}$ có khoảng cách $DIB_{old}$:
    $$\text{Nếu } DIB_{new} > DIB_{old} \implies \text{Hoán đổi (Swap) } k_{new} \leftrightarrow k_{old}$$
  + Phần tử bị hoán đổi ($k_{old}$) tiếp tục quá trình chèn với $DIB_{old} + 1$.
- **Hệ quả Toán học**:
  1. **San phẳng phân phối (Equalization)**: Thuật toán không bao giờ để một phần tử chịu thiệt thòi quá mức trong khi phần tử khác ở ngay home bucket. Độ dài bước dò của các phần tử trong cùng cụm tiệm cận về giá trị trung bình $\mu$.
  2. **Nén chặt đuôi phân phối (Tail Truncation)**: Độ lệch chuẩn $\sigma$ và phương sai $\sigma^2$ giảm mạnh. Theo bất đẳng thức Chebyshev và Jensen, việc giảm phương sai trực tiếp khống chế độ trễ phân vị cao (tail latency - 99th/99.9th percentile).

#### Số liệu thực chứng từ đồ án:
- Tại tải $\alpha = 0.50$: Max DIB trong `lp-backshift` là **41**, trong khi Robin Hood nén xuống chỉ còn **9** (giảm **78.0%**).
- Tại tải cao $\alpha = 0.80$: Max DIB trong `lp-backshift` bùng nổ lên **195** (từng đạt đỉnh $420$ trong quá trình churn), trong khi Robin Hood giữ vững ở mức tối đa **25** (giảm **87.2%**).

---

### CÂU HỎI 2: VÌ SAO BACKWARD SHIFT LÀ GIẢI PHÁP XÓA TRIỆT ĐỂ VƯỢT TRỘI SO VỚI TOMBSTONE VÀ REBUILD?

#### Bản chất lý thuyết & Hạn chế của Tombstone:
- **Tombstone**: Khi xóa, chỉ đánh dấu ô là `TOMB`. 
  + *Ưu điểm*: Thao tác xóa cực nhanh $\mathcal{O}(1)$.
  + *Nhược điểm chí mạng (RQ2)*: Ô `TOMB` không giải phóng khoảng trống thực sự cho các truy vấn trượt (Miss Query). Miss query bắt buộc phải quét qua toàn bộ chuỗi tombstone để tìm ô `EMPTY`. Khi churn $C \to 4M$, số ô `EMPTY` suy giảm từ $50\%$ xuống còn $0.04\%$, khiến số bước dò miss bùng nổ từ $2.50$ lên **$2,843.37$ bước dò** (tăng $1,137.9\times$).
  + *Cái giá của Rebuild*: Để cứu vãn, `lp-tomb` phải rebuild định kỳ theo ngưỡng $\tau \cdot M$. Tuy nhiên, rebuild là thao tác dừng toàn bộ hệ thống (Stop-the-world), quét $M$ ô và cấp phát lại. Trong thực nghiệm, mỗi lần rebuild gây ra **đột biến độ trễ (latency spike) từ $10.14 - 13.34$ ms** (tăng vọt $5.4\times - 6.1\times$ so với bình thường, đỉnh điểm lên tới $36.10$ ms), phá vỡ cam kết SLA trong hệ thống thời gian thực.

#### Cơ chế của Backward Shift (Knuth Algorithm R):
- **Xóa dịch lùi (Backward Shift)** dọn dẹp tại chỗ (in-place cluster defragmentation) ngay tại thời điểm xóa:
  1. Khi xóa khóa tại ô $i$, ô $i$ trở thành "lỗ hổng" (hole).
  2. Thuật toán duyệt tiến dọc theo cụm đụng độ phía sau $i$ (chỉ số $j = (i + 1) \pmod M$).
  3. Với mỗi phần tử tại $j$ có home bucket là $r = h(k_j)$: Nếu $r$ không nằm trong khoảng xoay vòng $(i, j]$, phần tử này hợp lệ để lấp vào lỗ hổng $i$. Ta chuyển phần tử từ $j$ về $i$, và vị trí $j$ trở thành lỗ hổng mới ($i = j$).
  4. Quá trình dừng lại ngay khi gặp ô `EMPTY` hoặc duyệt hết cụm. Lỗ hổng cuối cùng được đánh dấu là `EMPTY`.
- **Ưu thế vượt trội**:
  - **Zero Tombstone**: Số lượng tombstone trong bảng luôn bằng **0** trong suốt 4 triệu chu kỳ churn.
  - **Zero Rebuild**: Không bao giờ cần kích hoạt rebuild (**0 lần rebuild**), loại bỏ hoàn toàn các xung đột biến thời gian (spikes), giữ đường độ trễ phẳng lỳ.
  - **Không tốn thêm bộ nhớ**: Thực hiện trực tiếp trên các mảng nguyên thủy hiện có.

---

### CÂU HỎI 3: CƠ CHẾ DỪNG SỚM (EARLY EXIT) TRONG ROBIN HOOD HOẠT ĐỘNG THẾ NÀO VÀ TIẾT KIỆM ĐƯỢC BAO NHIÊU?

#### Nguyên lý toán học & Điều kiện dừng:
- Nhờ cơ chế hoán đổi khi chèn, các phần tử trong cùng một cụm đụng độ của Robin Hood luôn được **sắp xếp theo thứ tự DIB không giảm** đối với các phần tử có cùng home bucket hoặc chuỗi dò xen kẽ.
- Khi thực hiện tra cứu một khóa $k$ (với $h = h(k)$), ta duyệt qua các ô $idx = (h + probe) \pmod M$:
  - Tại ô $idx$, khoảng cách mà truy vấn hiện tại đã đi là $probe$.
  - Ô đang xét chứa một phần tử có khoảng cách thực tế là $dib[idx]$.
- **ĐIỀU KIỆN DỪNG SỚM (EARLY EXIT)**:
  $$\text{Nếu } probe > dib[idx] \implies \text{DỪNG NGAY VÀ KẾT LUẬN: KHÓA KHÔNG TỒN TẠI (MISS)}$$
- **Chứng minh tính đúng (Correctness Proof)**:
  Nếu khóa $k$ thực sự tồn tại trong bảng ở các ô phía sau, thì tại thời điểm chèn $k$, khoảng cách của nó tại vị trí đó phải lớn hơn hoặc bằng khoảng cách dò hiện tại ($probe$). Nhưng ô $idx$ đang chứa một phần tử có $dib[idx] < probe$. Theo quy tắc hoán đổi Robin Hood, khóa $k$ đáng lẽ đã phải hoán đổi và chiếm chỗ của phần tử tại ô $idx$ rồi! Việc phần tử tại ô $idx$ vẫn giữ nguyên chứng tỏ khóa $k$ chưa từng đi qua ô này $\implies$ Khóa $k$ chắc chắn không có trong bảng.

#### Định lượng mức tiết kiệm từ thực nghiệm RQ3:
Nhóm đã cô lập cơ chế này bằng biến thể `rh-noearly` (tắt điều kiện dừng sớm, chỉ dừng khi gặp `EMPTY`):
1. **Ở tải vừa ($\alpha = 0.50, C = 4M$)**:
   - `rh-noearly`: trung bình $2.499$ bước dò miss.
   - `rh` (bật Early Exit): trung bình **$1.751$ bước dò miss**.
   - **Mức tiết kiệm**: $\frac{2.499 - 1.751}{2.499} = \mathbf{29.9\% \approx 30.0\%}$.
2. **Ở tải cao ($\alpha = 0.80, C = 4M$)**:
   - `rh-noearly` & `lp-backshift`: trung bình $12.933$ bước dò miss.
   - `rh` (bật Early Exit): trung bình **$3.398$ bước dò miss**.
   - **Mức tiết kiệm**: $\frac{12.933 - 3.398}{12.933} = \mathbf{73.7\% \approx 73.8\%}$.
3. **So với Linear Probing dùng Tombstone có Rebuild (`lp-tomb`)**:
   - `lp-tomb` ở $\alpha=0.80$ tốn trung bình $20.844$ bước dò miss (đỉnh điểm $22.263$).
   - Robin Hood tiết kiệm tới: $\frac{20.844 - 3.400}{20.844} = \mathbf{83.7\%}$ chi phí dò tìm.

---

### CÂU HỎI 4: TẠI SAO NHÓM CHỌN KNUTH ALGORITHM R THAY VÌ CÁCH XÓA CỦA SEDGEWICK?

| Tiêu chí | Knuth Algorithm R (Lựa chọn của Nhóm) | Sedgewick & Wayne (Tái chèn cả cụm) |
|---|---|---|
| **Cơ chế** | Duyệt 1 lượt duy nhất $\mathcal{O}(K)$ dọc theo cụm, kiểm tra điều kiện xoay vòng $(i, j]$ và di dời (move) trực tiếp phần tử vào lỗ hổng $i$. | Xóa ô đích, sau đó lần lượt gỡ từng phần tử phía sau ra và gọi lại hàm `put()` để tái chèn (re-insert). |
| **Độ phức tạp** | Tuyệt đối $\mathcal{O}(K)$ bước kiểm tra, tối đa $\mathcal{O}(K)$ phép gán ô nhớ. | Có thể lên tới $\mathcal{O}(K^2)$ trong trường hợp xấu vì mỗi lần `put()` lại phải băm và dò từ đầu cụm. |
| **Chi phí tính toán** | Không cần băm lại các phần tử không dịch chuyển; chỉ đọc khóa và tính $h(k)$ đúng 1 lần cho mỗi ô. | Liên tục băm lại và tính toán lại bước dò trong vòng lặp con của `put()`. |
| **Cache Locality** | Tuyệt đối tối ưu: quét tuần tự xuôi theo chiều mảng một lượt duy nhất. | Nhảy cóc qua lại giữa các ô do các lệnh `put()` độc lập. |
| **Chuẩn mực học thuật** | Khớp chính xác tài liệu tham khảo kinh điển [1] Knuth TAOCP Vol 3, §6.4. | Thuộc tài liệu [4] Sedgewick Algorithms 4th ed. |

---

### CÂU HỎI 5: KHI NÀO ROBIN HOOD CHẬM HƠN LINEAR PROBING VÀ TẠI SAO?

- **Hiện tượng thực nghiệm**: Trong Bảng II ở tải vừa $\alpha = 0.50$ với các truy vấn trúng (Hit Query), thời gian tra cứu của `rh` ($47.17$ ms) tuy nhanh hơn `lp-backshift` ($69.40$ ms), nhưng trong một số tình huống tải thấp ($\alpha \le 0.40$), `rh` có thể tốn thêm thời gian CPU.
- **Nguyên nhân kỹ thuật**:
  1. **Overhead rẽ nhánh (Branching Overhead)**: Trong mỗi bước lặp của vòng dò, Robin Hood phải nạp `dib[idx]` từ RAM và so sánh điều kiện `curDib > dib[idx]`, trong khi Linear Probing thuần túy chỉ so sánh `keys[idx] == key` và kiểm tra `state[idx] == EMPTY`.
  2. **Áp lực bộ nhớ (Memory Traffic)**: Robin Hood bắt buộc phải duy trì thêm mảng nguyên thủy `int[] dib`, làm tăng kích thước bộ nhớ đệm L1/L2 cần nạp thêm 4 bytes/ô so với Linear Probing không lưu DIB.
- **Kết luận**: Robin Hood phát huy sức mạnh vượt trội nhất khi hệ thống chịu **tải cao ($\alpha \ge 0.70$)** và có **tỷ lệ truy vấn trượt (Miss Query) lớn** (nơi mà Early Exit phát huy 73.8% cắt giảm dò tìm).

---

### CÂU HỎI 6: XỬ LÝ ĐỤNG ĐỘ VÒNG QUA CUỐI MẢNG (WRAP-AROUND) TRONG CẢ HAI THUẬT TOÁN ĐƯỢC THỰC HIỆN NHƯ THẾ NÀO?

- Cả hai bảng băm đều có kích thước lũy thừa của 2: $M = 2^p$, sử dụng toán tử bit-masking `mask = M - 1` để tăng chỉ số:
  $$idx = (idx + 1) \& mask$$
- **Trong `lp-backshift` (Knuth Algorithm R)**:
  - Khi một cụm bắt đầu ở cuối mảng (vd: ô $M-2, M-1$) và tràn qua đầu mảng (ô $0, 1$), chỉ số lỗ hổng $i$ và chỉ số quét $j$ có thể rơi vào trường hợp $i > j$.
  - Biểu thức logic của Knuth xử lý trọn vẹn cả hai trường hợp:
    ```java
    boolean between = (i <= j) ? (i < r && r <= j) : (i < r || r <= j);
    ```
  - Nếu `!between`, phần tử tại $j$ có home bucket nằm trước hoặc bằng $i$ (theo vòng tròn), do đó được phép dịch lùi về lấp vào $i$.
- **Trong `RobinHoodTable`**:
  - Khi xóa dịch lùi, chỉ số kế tiếp là `next = (curr + 1) & mask`.
  - Quá trình dịch lùi dừng lại ngay khi `state[next] == EMPTY` hoặc `dib[next] == 0`.
  - Tất cả đã được chứng minh và vượt qua 100% trong bài kiểm thử bắt buộc `testLinearProbingWrapAroundShift()` và `testRobinHoodDibSwap()` với $M = 16$.
