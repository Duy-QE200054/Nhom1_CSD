# Ghi Chú Đọc 4 Nguồn Bắt Buộc (Literature Notes)
## Phục vụ Mục II: Background & Related Work trong Paper IEEE

> Căn cứ theo danh mục tài liệu bắt buộc tại Mục 9 của Đề tài 2 và phân công đọc tài liệu ngày 19/10.

---

### [1] D. E. Knuth, The Art of Computer Programming, Vol. 3: Sorting and Searching, 2nd ed. Reading, MA, USA: Addison-Wesley, 1998, §6.4.
- **Phân công phụ trách**: Cặp 2 và Cặp 3.
- **Nội dung & Đóng góp cốt lõi**:
  - Phân tích toán học kinh điển về hiện tượng đụng độ và dồn cụm sơ cấp (primary clustering) trong Linear Probing khi bảng đầy dần.
  - Cung cấp công thức kỳ vọng số lần dò lý thuyết khi hàm băm đều và không có xóa:
    $$LP_{hit} \approx \frac{1}{2}\left(1 + \frac{1}{1 - \alpha}\right), \quad LP_{miss} \approx \frac{1}{2}\left(1 + \frac{1}{(1 - \alpha)^2}\right)$$
  - Giới thiệu **Algorithm R** (Linear Probing Deletion): giải thuật xóa không dùng tombstone bằng cách duyệt tiếp trong cụm và dịch chuyển phần tử thích hợp lấp vào ô trống nếu vị trí băm gốc của nó nằm ngoài khoảng từ ô trống đến ô đang xét (xử lý chính xác trường hợp cụm xoay vòng qua cuối mảng).

---

### [2] W. W. Peterson, "Addressing for random-access storage," IBM Journal of Research and Development, vol. 1, no. 2, pp. 130–146, 1957.
- **Phân công phụ trách**: Cặp 2.
- **Nội dung & Đóng góp cốt lõi**:
  - Bài báo đặt nền móng đầu tiên định nghĩa và mô tả phương pháp định địa chỉ mở (Open Addressing) với dò tuyến tính (Linear Probing) trên thiết bị lưu trữ truy cập ngẫu nhiên.
  - Phân tích xác suất đụng độ và mối quan hệ giữa hệ số tải $\alpha$ và thời gian truy cập.
  - Đặt ra vấn đề xóa phần tử trong bảng dò mở: việc xóa phần tử không thể đơn giản là đặt lại ô về rỗng (EMPTY) vì sẽ làm đứt gãy chuỗi dò của các phần tử đụng độ được chèn sau đó. Đây là nguồn gốc dẫn đến sự ra đời của cơ chế đánh dấu xóa tạm thời (**Tombstone**).

---

### [3] P. Celis, P.-Å. Larson, and J. I. Munro, "Robin Hood hashing," in Proc. 26th Annu. Symp. Foundations of Computer Science (FOCS), 1985, pp. 281–288.
- **Phân công phụ trách**: Cặp 3.
- **Nội dung & Đóng góp cốt lõi**:
  - Đề xuất giải thuật băm Robin Hood dựa trên triết lý "lấy của người giàu chia cho người nghèo": cân bằng khoảng cách dò (DIB - Distance from Initial Bucket) giữa các khóa trong bảng.
  - Khi chèn một khóa mới, nếu khoảng cách dò của khóa đang chèn lớn hơn DIB của khóa nằm trong ô hiện tại, thuật toán sẽ hoán đổi (cướp chỗ) và tiếp tục đẩy khóa cũ đi tìm vị trí mới.
  - **Lợi ích vượt trội**:
    1. Làm giảm đáng kể phương sai (variance) của độ dài chuỗi dò, chặn trên DIB tối đa ($O(\log \log n)$ với xác suất cao).
    2. Cho phép cơ chế **Dừng sớm (Early Exit)** khi tìm kiếm: nếu khoảng cách dò hiện tại lớn hơn DIB của ô đang xét thì khẳng định ngay khóa không tồn tại mà không cần dò tiếp đến `EMPTY`.
    3. Kết hợp tự nhiên với xóa dịch lùi (Backward-shift deletion) mà không cần dùng đến Tombstone hay Rebuild.

---

### [4] R. Sedgewick and K. Wayne, Algorithms, 4th ed. Boston, MA, USA: Addison-Wesley, 2011, §3.4.
- **Phân công phụ trách**: Cặp 1.
- **Nội dung & Đóng góp cốt lõi**:
  - Trình bày trực quan, chuẩn mực về cài đặt Separate Chaining (danh sách liên kết đơn tại mỗi bucket) và Linear Probing trong môi trường hướng đối tượng Java.
  - Phân tích chi phí bộ nhớ của Separate Chaining: mỗi `Node` mang chi phí phụ về header đối tượng (16 bytes trên 64-bit JVM), con trỏ tham chiếu (8 bytes / 4 bytes Compressed OOPs), và phân mảnh bộ nhớ heap gây ra lỗi bộ nhớ đệm (cache miss).
  - Đưa ra giải thuật xóa Linear Probing bằng cách xóa khóa đích và chèn lại toàn bộ (rehash/re-insert) các khóa còn lại trong cùng cụm cho đến khi gặp ô trống. Cung cấp mốc đối chiếu công thức lý thuyết của Separate Chaining:
    $$SC_{hit} \approx 1 + \frac{\alpha}{2}, \quad SC_{miss} \approx \alpha$$
