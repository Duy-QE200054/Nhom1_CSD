# KỊCH BẢN THUYẾT TRÌNH VÀ PHÂN VAI BÁO CÁO CHO CẶP 3 (C3)
## Đề tài 2: Hash Table - Tombstone, Rebuild và Robin Hood Hashing
**Thành viên Cặp 3 (SV 5 & SV 6)**: Nguyễn Đình Tiến (`nguyendinhtien824-super`) & Lê Hoàng Cầu  
**Phụ trách**: Cài đặt `lp-backshift`, `rh`, `rh-noearly`; Thiết kế thực nghiệm và phân tích RQ3 (Bóc tách 3 cơ chế).  
**Thời lượng phân bổ**: 1 phút 45 giây (trong tổng số 5 phút của nhóm).

---

### PHẦN 1: PHÂN VAI THUYẾT TRÌNH TỔNG THỂ (5 PHÚT)

| Slide | Nội dung chính | Người trình bày | Thời lượng |
|---|---|---|---|
| **Slide 1 - 2** | Giới thiệu, bối cảnh bài toán tra cứu giao dịch & 3 câu hỏi nghiên cứu | Cặp 1 (SV 1, SV 2) | 1 phút 00 giây |
| **Slide 3 - 4** | Thiết kế hệ thống, hàm băm Fibonacci, bộ test Oracle & 6 ca bắt buộc | Cặp 1 & Cặp 2 | 1 phút 00 giây |
| **Slide 5 - 6** | Kết quả RQ1 (SC vs LP) và RQ2 (Cái giá của Tombstone & Rebuild) | Cặp 2 (SV 3, SV 4) | 1 phút 15 giây |
| **Slide 7** | **Kết quả RQ3: Bóc tách định lượng 3 cơ chế Robin Hood Hashing** | **Cặp 3 (SV 5 - Nguyễn Đình Tiến)** | **1 phút 15 giây** |
| **Slide 8** | **Khuyến nghị kỹ thuật, Kết luận & Định hướng ứng dụng** | **Cặp 3 (SV 6 - Lê Hoàng Cầu)** | **30 giây** |

---

### PHẦN 2: KỊCH BẢN CHI TIẾT SLIDE 7 (CẶP 3 - SV 5 TRÌNH BÀY)

#### Visual trên Slide 7:
1. **Bảng số liệu tổng hợp (Table II, III, IV)**: So sánh `lp-tomb`, `lp-backshift`, `rh-noearly`, `rh` ở $C = 4M$.
2. **Đồ thị Fig. 6 (`fig_rq3_rebuild_spikes.png`)**: Trục thời gian độ trễ khối $10^4$ thao tác, làm nổi bật các đỉnh spike $10.14 - 13.34$ ms (đỉnh $36.10$ ms) của `lp-tomb` so với đường ngang phẳng lỳ của `lp-backshift` và `rh`.
3. **Đồ thị Fig. 7 (`fig_rq3_mechanism_breakdown.png`)**: Bóc tách 3 cơ chế (Bỏ tombstone, Hoán đổi DIB, Dừng sớm Early Exit).

#### Lời thoại thuyết trình (Speaking Script - 75 giây):
> *"Kính thưa Thầy/Cô và Hội đồng, em là Nguyễn Đình Tiến, đại diện Cặp 3 trình bày về Câu hỏi nghiên cứu số 3: **Bóc tách định lượng 3 cơ chế cốt lõi trong Robin Hood Hashing** qua chiến dịch đo đạc hơn 8,39 triệu thao tác churn.*
>
> *Từ kết quả RQ2, chúng ta đã thấy Tombstone tàn phá bảng băm ra sao. Nhóm em đã thiết kế biến thể ablation `rh-noearly` kết hợp cùng `lp-backshift` để cô lập chính xác giá trị của từng cơ chế kỹ thuật:*
>
> * **Thứ nhất - Loại bỏ Tombstone bằng Xóa dịch lùi (Knuth Algorithm R)**: Khi so sánh `lp-tomb` với `lp-backshift`, ta thấy ở $\alpha=0.50$ và $\alpha=0.80$, `lp-tomb` phải rebuild lần lượt 14 và 23 lần, tạo ra các xung đột biến thời gian (latency spikes) lên tới $36.10$ ms, gấp $5.4\times$ đến $6.1\times$ khối bình thường. Ngược lại, việc dịch lùi in-place triệt tiêu 100% tombstone, duy trì **0 lần rebuild**, bảo đảm thông lượng phẳng lỳ và ổn định tuyệt đối.
>
> * **Thứ hai - San phẳng phương sai chuỗi dò nhờ Hoán đổi DIB (Robin Hood Swapping)**: So sánh `lp-backshift` với `rh-noearly` cho thấy: dù cả hai đều không có tombstone, nhưng `lp-backshift` để các cụm phình to tự do, đẩy Max DIB lên 41 ở tải vừa và **195** ở tải cao. Thuật toán Robin Hood 'lấy của người giàu chia cho người nghèo' đã nén chặt Max DIB xuống chỉ còn **9** (giảm 78.0%) và **25** (giảm 87.2%), khống chế triệt để tail latency.
>
> * **Thứ ba - Tăng tốc tra cứu bằng Cơ chế Dừng sớm (Early Exit)**: So sánh `rh-noearly` với `rh` nguyên bản cho thấy quy tắc dừng sớm $DIB_{probe} > DIB_{slot}$ là 'vũ khí tối thượng' cho các truy vấn trượt (Miss Query). Ở tải vừa, nó tiết kiệm 30.0% số bước dò; và ở tải cao $\alpha=0.80$, nó cắt giảm tới **73.8% số bước dò miss** (từ 12.93 xuống chỉ còn 3.40 bước dò). So với Linear Probing dùng tombstone có rebuild, Robin Hood giảm tới **83.7% chi phí dò tìm**.
>
> *Sau đây, bạn Lê Hoàng Cầu sẽ trình bày phần khuyến nghị kỹ thuật và kết luận của nhóm."*

---

### PHẦN 3: KỊCH BẢN CHI TIẾT SLIDE 8 (CẶP 3 - SV 6 TRÌNH BÀY)

#### Visual trên Slide 8:
- Ma trận quyết định kỹ thuật (Decision Matrix): Loại bảng vs Tải trọng $\alpha$ vs Tỷ lệ thao tác Churn / Miss.
- Tóm tắt 3 khuyến nghị thực tiễn từ đồ án.

#### Lời thoại thuyết trình (Speaking Script - 30 giây):
> *"Kính thưa Thầy/Cô, từ kết quả thực nghiệm toàn diện trên 5 cấu hình bảng băm, nhóm chúng em đưa ra 3 khuyến nghị kỹ thuật thực tế:*
>
> 1. *Với hệ thống **Append-mostly** (chủ yếu thêm, ít xóa) ở tải vừa ($\alpha \le 0.50$): **Linear Probing thông thường** là tối ưu nhất vì chi phí CPU thấp, tận dụng trọn vẹn phần cứng cache prefetching.*
> 2. *Với hệ thống có **xóa định kỳ nhưng tài nguyên bộ nhớ hạn hẹp**: **Linear Probing Backshift (Knuth Algorithm R)** là lựa chọn vượt trội vì dọn dẹp cụm tại chỗ, không tốn thêm byte bộ nhớ nào cho mảng DIB và không bị đóng băng hệ thống do Rebuild.*
> 3. *Với hệ thống **Giao dịch tài chính / Microservices chịu tải cao ($\alpha \ge 0.80$)** có tỷ lệ truy vấn trượt (Miss queries) lớn: **Robin Hood Hashing toàn diện** là giải pháp bắt buộc. Dù tiêu tốn thêm mảng `int[] dib`, Robin Hood bảo vệ hệ thống không bị nghẽn mạch, cắt giảm 73.8% độ sâu dò tìm và triệt tiêu hoàn toàn rủi ro suy thoái.*
>
> *Nhóm 1 xin chân thành cảm ơn Thầy/Cô và sẵn sàng trả lời các câu hỏi phản biện!"*
