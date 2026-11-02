# BỘ CÂU HỎI PHẢN BIỆN VÀ HƯỚNG DẪN TRẢ LỜI CHO CẶP 3 (C3)
## Đề tài: Hash Table - Tombstone, Rebuild và Robin Hood Hashing
**Thành viên C3**: Nguyễn Đình Tiến (`nguyendinhtien824-super`) & Lê Hoàng Cầu  
**Mục đích**: Chuẩn bị đầy đủ cơ sở lý thuyết, công thức toán học và số liệu thực chứng để bảo vệ tự tin trước Hội đồng chấm đồ án.

---

## TẬP DƯỢT NGÀY 29/10: BA CÂU HỎI TRỌNG TÂM CỦA HỘI ĐỒNG

Phần dưới đây là câu trả lời miệng ngắn (khoảng 20--30 giây/câu). Với câu hỏi ngoài RQ3, C3 nêu cơ chế và số liệu minh họa rồi chuyển C1 trình bày sâu phần RQ1.

### 1. Vì sao Robin Hood giảm phương sai độ dài dò?

**Trả lời miệng:** “Linear Probing có thể để một số khóa bị đẩy đi rất xa trong khi khóa khác ở gần bucket gốc. Robin Hood so sánh DIB khi chèn: khóa đã đi xa hơn sẽ được ưu tiên vị trí, còn khóa bị đẩy tiếp tục dò. Cơ chế này phân phối lại chi phí dò, giảm các trường hợp cực đoan thay vì cam kết làm mọi thao tác nhanh hơn. Trong dữ liệu RQ3, Max DIB cuối workload giảm từ 41 xuống 9 tại $\alpha=0.50$, và từ 195 xuống 25 tại $\alpha=0.80$ khi so `lp-backshift` với Robin Hood. Đây là bằng chứng về độ dịch chuyển cực đại, không phải phép đo trực tiếp phương sai hay percentile latency.”

**Nếu bị hỏi “có phải Robin Hood luôn ít probe hơn?”** Không. Robin Hood có thể tăng công việc chèn do swap và tốn thêm mảng DIB. Lợi ích phụ thuộc tải, tỷ lệ hit/miss và workload; kết quả C3 cho thấy early exit đặc biệt giảm probe ở truy vấn miss, không chứng minh mọi thời gian chạy đều tốt hơn.

### 2. Vì sao tombstone làm truy vấn vắng mặt chậm?

**Trả lời miệng:** “Trong bảng dò tuyến tính, `TOMB` không thể được xem như ô trống: khóa cần tìm có thể nằm phía sau nó, nên truy vấn miss phải tiếp tục dò cho đến khi gặp `EMPTY`. Churn làm nhiều ô không còn là `EMPTY`, khiến điểm kết thúc tìm kiếm bị đẩy xa. Ở RQ2, sau $C=4M$, số ô `EMPTY` còn 0.04% và miss tăng từ 2.499 lên 2,843.373 probe trung bình; bảng sạch cùng tập khóa chỉ khoảng 2.499. Rebuild phục hồi các ô trống nhưng thêm công việc đồng bộ vào lần cập nhật kích hoạt rebuild. Backshift tránh tích tụ tombstone, đổi lại một lần xóa có thể phải dịch chuyển cả cụm.”

**Nếu bị hỏi “backshift có loại bỏ mọi spike không?”** Không nên khẳng định như vậy. Nó loại bỏ các lần rebuild và spike do rebuild; bản thân việc dịch chuyển cụm lúc xóa vẫn có chi phí biến thiên. Trong block profiling, `lp-backshift` cũng có block chậm; kết quả phụ thuộc kích thước block và cấu hình.

### 3. Vì sao Separate Chaining ít probe hơn nhưng đôi khi không nhanh hơn Linear Probing?

**Trả lời miệng (C3 phối hợp C1):** “Probe count đếm đơn vị khác nhau: với Separate Chaining là số node đã thăm, còn với Linear Probing là số slot đã kiểm tra. Một node thường cần lần theo con trỏ tới vùng nhớ có thể phân tán; Linear Probing kiểm tra mảng liên tiếp nên tận dụng locality và hardware prefetch tốt hơn. Vì vậy ít probe hơn không tự động đồng nghĩa ít thời gian hơn. Ví dụ trong bộ đo RQ1 ở $\alpha=0.50$, hit của SC là 1.2503 node và 53.97 ns/op; LP là 1.5033 slot và 36.95 ns/op. Nhưng ở $\alpha=0.75$, phép đo hiện có lại cho SC nhanh hơn LP ở cả hit và miss. Đây là số liệu một lượt, nên kết luận đúng là tốc độ phụ thuộc workload và môi trường; không có thuật toán thắng tuyệt đối.”

**Nếu bị hỏi “SC có luôn chậm hơn không?”** Không. SC có thể phù hợp khi tải cao hoặc phân phối phù hợp; cần so cả thời gian, bộ nhớ và loại truy vấn trên cùng máy. C1 phụ trách giải thích chi tiết bảng RQ1 và chi phí cấp phát node.

### Nhắc nhanh trước khi trả lời

- Nêu cấu hình cùng số liệu nguồn: RQ2 dùng $M=2^{20}$, $\alpha=0.50$, $C=4M$; RQ3 block profiling dùng $M=2^{20}$, $\alpha=0.50/0.80$, $C=4M$.
- Phân biệt **probe count**, **Max DIB**, **block latency** và **thời gian lookup**; không dùng một đại lượng để thay thế đại lượng khác.
- Nói rõ các giới hạn: số seed hữu hạn, dữ liệu tổng hợp, một máy; không suy rộng thành bảo đảm SLA hay kết luận phổ quát.
- Phân vai: C3 trả lời Robin Hood, tombstone/backshift; chuyển C1 trả lời sâu về SC-vs-LP. Cả nhóm cần thống nhất số liệu và tránh khẳng định theo câu hỏi gợi ý nếu dữ liệu không ủng hộ.

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
  2. **Giảm các khoảng cách cực đoan**: Cơ chế hoán đổi phân phối lại độ dời giữa các khóa trong cụm, giúp tránh một số khóa chịu phần lớn chi phí dò. Max DIB thấp hơn là dấu hiệu thực nghiệm của đuôi ngắn hơn trong cấu hình đã đo; riêng phương sai và percentile latency cần được tính/đo trực tiếp, không suy ra chỉ từ Max DIB.

#### Số liệu thực chứng từ đồ án:
- Tại tải $\alpha = 0.50$: Max DIB trong `lp-backshift` là **41**, trong khi Robin Hood nén xuống chỉ còn **9** (giảm **78.0%**).
- Tại tải cao $\alpha = 0.80$: Max DIB trong `lp-backshift` bùng nổ lên **195** (từng đạt đỉnh $420$ trong quá trình churn), trong khi Robin Hood giữ vững ở mức tối đa **25** (giảm **87.2%**).

---

### CÂU HỎI 2: VÌ SAO BACKWARD SHIFT LÀ GIẢI PHÁP XÓA TRIỆT ĐỂ VƯỢT TRỘI SO VỚI TOMBSTONE VÀ REBUILD?

#### Bản chất lý thuyết & Hạn chế của Tombstone:
- **Tombstone**: Khi xóa, chỉ đánh dấu ô là `TOMB`. 
  + *Ưu điểm*: Thao tác xóa cực nhanh $\mathcal{O}(1)$.
  + *Nhược điểm chí mạng (RQ2)*: Ô `TOMB` không giải phóng khoảng trống thực sự cho các truy vấn trượt (Miss Query). Miss query bắt buộc phải quét qua toàn bộ chuỗi tombstone để tìm ô `EMPTY`. Khi churn $C \to 4M$, số ô `EMPTY` suy giảm từ $50\%$ xuống còn $0.04\%$, khiến số bước dò miss bùng nổ từ $2.50$ lên **$2,843.37$ bước dò** (tăng $1,137.9\times$).
  + *Cái giá của Rebuild*: Để cứu vãn, `lp-tomb` rebuild định kỳ theo ngưỡng $\tau \cdot M$. Rebuild thêm công việc quét/tái dựng bảng vào thao tác kích hoạt nó; nếu thực thi đồng bộ, thao tác đó có thể có latency cao hơn các thao tác thường. Trong block profiling, các block có rebuild tăng trung bình khoảng $5.4\times$--$6.1\times$ so với block thường, với đỉnh quan sát $36.10$ ms. Đây là kết quả của cấu hình đo, không đủ để kết luận về SLA hay mọi hệ thống production.

#### Cơ chế của Backward Shift (Knuth Algorithm R):
- **Xóa dịch lùi (Backward Shift)** dọn dẹp tại chỗ (in-place cluster defragmentation) ngay tại thời điểm xóa:
  1. Khi xóa khóa tại ô $i$, ô $i$ trở thành "lỗ hổng" (hole).
  2. Thuật toán duyệt tiến dọc theo cụm đụng độ phía sau $i$ (chỉ số $j = (i + 1) \pmod M$).
  3. Với mỗi phần tử tại $j$ có home bucket là $r = h(k_j)$: Nếu $r$ không nằm trong khoảng xoay vòng $(i, j]$, phần tử này hợp lệ để lấp vào lỗ hổng $i$. Ta chuyển phần tử từ $j$ về $i$, và vị trí $j$ trở thành lỗ hổng mới ($i = j$).
  4. Quá trình dừng lại ngay khi gặp ô `EMPTY` hoặc duyệt hết cụm. Lỗ hổng cuối cùng được đánh dấu là `EMPTY`.
- **Ưu thế vượt trội**:
  - **Zero Tombstone**: Số lượng tombstone trong bảng luôn bằng **0** trong suốt 4 triệu chu kỳ churn.
  - **Zero Rebuild**: Không kích hoạt rebuild (**0 lần rebuild**), do đó tránh được các spike gắn với tái dựng toàn bảng. Chi phí dịch chuyển vẫn phát sinh trong thao tác xóa và có thể biến thiên theo độ dài cụm.
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
