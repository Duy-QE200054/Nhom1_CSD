# Đề cương Slide Báo cáo (Tối đa 8 Slide)
## Quy định theo Bảng 6 & Bảng 8 (Thời gian trình bày: 5 phút)

- **Slide 1: Tiêu đề & Giới thiệu Nhóm**
  - Tên đề tài: Nghiên cứu thực nghiệm các chiến lược giải quyết đụng độ trong Hash Table: Tombstone, Rebuild và Robin Hood Hashing
  - Tên thành viên 6 sinh viên, MSSV, vai trò từng cặp (Cặp 1, Cặp 2, Cặp 3).
- **Slide 2: Bối cảnh & Câu hỏi Nghiên cứu (Research Questions)**
  - Bài toán tra cứu mã giao dịch: thêm/xóa liên tục, truy vấn vắng mặt chiếm đa số.
  - Ba câu hỏi: RQ1 (SC vs LP), RQ2 (Tombstone accumulation), RQ3 (Robin Hood mechanism ablation).
- **Slide 3: Thiết kế Hệ thống & Quy ước Cài đặt**
  - Hàm băm Fibonacci, bảng cố định $M = 2^p$.
  - 5 cấu hình: `sc`, `lp-tomb`, `lp-backshift`, `rh`, `rh-noearly`.
- **Slide 4: Phương pháp Thực nghiệm & Kiểm thử Tính đúng**
  - Oracle test với `java.util.HashMap` ($10^6$ ops), 6 ca test biên.
  - Phương pháp đo: Isolation qua process riêng, timeout 60s, tách biệt lượt đo thời gian và lượt `--count`.
- **Slide 5: Kết quả & Phân tích RQ1 (Separate Chaining vs Linear Probing)**
  - Đồ thị số lần dò và thời gian tra cứu theo $\alpha$ và $M$.
  - So sánh lý thuyết Knuth. Phân tích chi phí cấp phát đối tượng `Node` của Java.
- **Slide 6: Kết quả & Phân tích RQ2 (Cái giá của Tombstone khi không Rebuild)**
  - Đồ thị suy giảm hiệu năng qua chu kỳ $C \to 4M$.
  - Hiện tượng cạn kiệt ô `EMPTY`, số lần dò vọt lên và timeout ở các mốc lớn so với Clean Table đối chứng.
- **Slide 7: Kết quả & Phân tích RQ3 (Đóng góp của từng cơ chế trong Robin Hood)**
  - Phân tích bóc tách 3 cơ chế: Bỏ tombstone (Backshift) vs Hoán đổi theo DIB vs Dừng sớm (Early exit).
  - Đột biến thời gian do Rebuild trong `lp-tomb` (median vs max khối $10^4$ ops).
- **Slide 8: Kết luận, Khuyến nghị Kỹ thuật & Bài học kinh nghiệm**
  - Khi nào nên dùng loại bảng nào?
  - Kết luận có điều kiện và giới hạn thực nghiệm.
