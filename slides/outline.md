# ĐỀ CƯƠNG SLIDE BÁO CÁO VÀ PHÂN VAI THUYẾT TRÌNH (TỐI ĐA 8 SLIDE)
## Quy định theo Bảng 6 & Bảng 8 (Thời gian thuyết trình: 5 phút | Vấn đáp: 10 phút)
**Ngày hoàn thiện**: 28/10/2026  
**Nhóm 1**: Đề tài 2 - Hash Table: tombstone, rebuild và Robin Hood Hashing  

---

### PHÂN BỔ THỜI GIAN VÀ VAI TRÒ CỦA 6 THÀNH VIÊN

| Slide | Nội dung chính | Thành viên phụ trách | Thời lượng |
|---|---|---|---|
| **Slide 1** | Tiêu đề đề tài & Giới thiệu 6 thành viên, phân công 3 cặp | Cặp 1 (SV 1: Nguyễn Duy) | 30s |
| **Slide 2** | Bối cảnh bài toán tra cứu giao dịch & Ba câu hỏi nghiên cứu (RQ1, RQ2, RQ3) | Cặp 1 (SV 2: Nguyễn Tuấn Hưng) | 30s |
| **Slide 3** | Thiết kế hệ thống: Hàm băm Fibonacci, 5 cấu hình (`sc`, `lp-tomb`, `lp-backshift`, `rh`, `rh-noearly`) | Cặp 2 (SV 3: Lê Phước Anh Quân) | 30s |
| **Slide 4** | Phương pháp thực nghiệm: Oracle test $10^6$ ops, 6 ca bắt buộc, đo process riêng | Cặp 2 (SV 4: Nguyễn Viết Cường) | 30s |
| **Slide 5** | Kết quả & Phân tích RQ1: Separate Chaining vs Linear Probing, chi phí cấp phát Node | Cặp 1 (SV 1, SV 2) | 45s |
| **Slide 6** | Kết quả & Phân tích RQ2: Cái giá của Tombstone, suy giảm ô Empty và bùng nổ bước dò | Cặp 2 (SV 3, SV 4) | 45s |
| **Slide 7** | **Kết quả & Phân tích RQ3: Bóc tách 3 cơ chế Robin Hood, đột biến độ trễ Rebuild, DIB Max** | **Cặp 3 (SV 5: Nguyễn Đình Tiến)** | **75s** |
| **Slide 8** | **Khuyến nghị kỹ thuật, Ma trận quyết định kiến trúc, Kết luận & Định hướng** | **Cặp 3 (SV 6: Lê Hoàng Cầu)** | **30s** |

---

### NỘI DUNG CHI TIẾT TỪNG SLIDE

#### Slide 1: Tiêu đề & Giới thiệu Nhóm
- **Đề tài**: Nghiên cứu thực nghiệm các chiến lược giải quyết đụng độ trong Hash Table: Tombstone, Rebuild và Robin Hood Hashing.
- **Thành viên & Cặp**:
  - Cặp 1 (C1): Nguyễn Duy & Nguyễn Tuấn Hưng (Chủ trì RQ1: `sc` vs `lp-tomb`, chi phí cấp phát `Node`).
  - Cặp 2 (C2): Lê Phước Anh Quân & Nguyễn Viết Cường (Chủ trì RQ2: `lp-tomb`, động học ô nhớ, ngưỡng $\tau$).
  - Cặp 3 (C3): Nguyễn Đình Tiến & Lê Hoàng Cầu (Chủ trì RQ3: `lp-backshift`, `rh`, `rh-noearly`, bóc tách 3 cơ chế).

#### Slide 2: Bối cảnh & Câu hỏi Nghiên cứu
- Bài toán: Hệ thống chỉ mục giao dịch tài chính tần suất cao (In-memory transaction indexing).
- Đặc thù workload: Chèn và xóa liên tục ($C \to 4M$ ops), tỷ lệ truy vấn vắng mặt (Miss Query) chiếm đa số.
- 3 câu hỏi:
  - **RQ1**: Tải trọng $\alpha$ và kích thước $M$ tác động thế nào đến `sc` và `lp-tomb`?
  - **RQ2**: Tombstone tích tụ tàn phá hiệu năng tra cứu ra sao khi không Rebuild?
  - **RQ3**: Robin Hood và Xóa dịch lùi đóng góp những gì, và từng cơ chế kỹ thuật mang lại giá trị định lượng bao nhiêu?

#### Slide 3: Thiết kế Hệ thống & Quy ước Cài đặt
- Hàm băm chung Fibonacci: $h(k) = (k \cdot \phi) \ggg (64 - p)$ với $\phi = 11400714819323198485\text{L}$, bảng $M = 2^p$.
- 5 cấu hình:
  - `sc`: Mảng `Node[]` danh sách liên kết đơn.
  - `lp-tomb`: Ba mảng nguyên thủy (`keys`, `values`, `state`), trạng thái `TOMB`, ngưỡng rebuild $\tau \cdot M$.
  - `lp-backshift`: Knuth Algorithm R xóa dịch lùi tại chỗ, 0 tombstone, 0 rebuild.
  - `rh`: Robin Hood đầy đủ (Mảng `int[] dib`, hoán đổi DIB, dừng sớm Early Exit, xóa dịch lùi).
  - `rh-noearly`: Robin Hood ablation tắt cơ chế dừng sớm.

#### Slide 4: Phương pháp Thực nghiệm & Kiểm thử Tính đúng
- Bộ test Oracle: So khớp $1.000.000$ thao tác hỗn hợp (40% put, 40% get, 20% remove) với `java.util.HashMap` trên $M=4096$.
- 6 ca test hợp đồng bắt buộc: Va chạm dồn cụm, xóa giữa cụm, xoay vòng qua cuối mảng ($M=16$).
- Đo đạc khoa học: Đo trong process JVM riêng biệt, timeout an toàn 60s, tách biệt hoàn toàn lượt `--count` (bước dò chính xác) và lượt `--mode time` (đo mili-giây).

#### Slide 5: Kết quả RQ1 (Separate Chaining vs Linear Probing)
- Biểu đồ số bước dò theo $\alpha$: `sc` có số bước dò thấp hơn $1 + \alpha/2$, nhưng thời gian tra cứu không nhanh hơn LP ở tải vừa.
- Phân tích chi phí cấp phát đối tượng `Node`: Overhead $24-32$ bytes/node, trỏ tham chiếu ngẫu nhiên làm mất tính cache locality so với mảng nguyên thủy liên tục của LP.

#### Slide 6: Kết quả RQ2 (Tombstone Pollution Dynamics)
- Hiện tượng cạn kiệt ô `EMPTY`: Tại $C = 4M$, ô `EMPTY` suy giảm từ $50\%$ xuống còn $0.04\%$.
- Bùng nổ bước dò: Số bước dò miss tăng vọt từ $2.50$ lên **$2,843.37$ bước dò** (tăng $1,137.9\times$).
- Độ trễ tra cứu vọt lên $5.85$ giây (tăng $233.3\times$). Khẳng định cơ chế Rebuild định kỳ là bắt buộc đối với `lp-tomb`.

#### Slide 7: Kết quả RQ3 (Bóc tách 3 Cơ chế Robin Hood Hashing) - Cặp 3 chủ trì
*(Chi tiết trong tài liệu: [rq3_slide_presentation.md](file:///C:/Users/Hi/.gemini/antigravity-ide/scratch/Nhom1_CSD/slides/rq3_slide_presentation.md))*
- **Cơ chế 1 (Loại bỏ Tombstone)**: `lp-backshift` triệt tiêu 100% tombstone, 0 lần rebuild, loại bỏ hoàn toàn các đỉnh đột biến thời gian $10.14 - 13.34$ ms (đỉnh $36.10$ ms) của `lp-tomb`.
- **Cơ chế 2 (Hoán đổi theo DIB)**: San phẳng phân phối, nén Max DIB từ 41 xuống 9 ở $\alpha=0.50$ (giảm 78.0%) và từ 195 xuống 25 ở $\alpha=0.80$ (giảm 87.2%), khống chế tail latency.
- **Cơ chế 3 (Dừng sớm Early Exit)**: Cắt giảm 30.0% bước dò miss ở tải vừa và **73.8% ở tải cao $\alpha=0.80$** ($12.93 \to 3.40$ bước dò). Giảm 83.7% bước dò so với `lp-tomb`.

#### Slide 8: Khuyến nghị Kỹ thuật, Kết luận & Hướng phát triển - Cặp 3 chủ trì
- **Ma trận quyết định**:
  - Workload thêm là chính, ít xóa ($\alpha \le 0.50$): Chọn **Linear Probing chuẩn**.
  - Workload xóa định kỳ, bộ nhớ eo hẹp: Chọn **Linear Probing Backshift (Knuth Algorithm R)**.
  - Workload giao dịch tải cao ($\alpha \ge 0.80$), nhiều truy vấn vắng mặt: Chọn **Robin Hood Hashing toàn diện**.
- **Tài liệu câu hỏi phản biện bảo vệ**: [defense_qa_c3.md](file:///C:/Users/Hi/.gemini/antigravity-ide/scratch/Nhom1_CSD/slides/defense_qa_c3.md).
