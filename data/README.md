# Đặc Tả Dữ Liệu và Bộ Sinh Khóa (Generator Specification)
## Đề tài 2: Chỉ mục tra cứu bằng Hash Table (CSD201 FA26)

Tài liệu này giải thích cơ chế sinh dữ liệu, quản lý hạt giống (seed) và kiểm tra tính toàn vẹn (SHA-256) theo quy định tại mục 6.8 của Quy định chung đồ án.

---

## 1. Nguyên tắc Sinh Dữ Liệu
Theo quy định mục 6.8:
- **Không lưu trữ file dữ liệu thô lớn hơn 10MB** trong repository. Thay vào đó, toàn bộ dữ liệu thực nghiệm được sinh bằng giải thuật giả ngẫu nhiên có thể tái lập hoàn toàn (reproducible) thông qua `SplittableRandom` cùng với `seed` xác định.
- **Phiên bản Generator**: `v1.0.0`
- **Tập hạt giống chuẩn (Seeds)**:
  - Seed 1: `42L`
  - Seed 2: `1337L`
  - Seed 3: `20261019L`

---

## 2. Các Phân Phối Dữ Liệu

### 2.1. Tập khóa có mặt (Present Keys - Hit)
- **Số lượng**: $N = \text{round}(\alpha \times M)$ khóa `long` không âm phân biệt.
- **Cách sinh**:
  - Dùng `java.util.SplittableRandom(seed).nextLong(Long.MAX_VALUE)`.
  - Sử dụng `java.util.HashSet<Long>` ở khâu sinh ban đầu để đảm bảo $100\%$ các khóa là duy nhất (không trùng lặp). Quá trình sinh này nằm ngoài vùng đo thời gian.

### 2.2. Tập truy vấn khóa vắng mặt (Absent Keys - Miss)
- **Cách sinh**:
  - Dùng `SplittableRandom` sinh các khóa ngẫu nhiên mới.
  - Kiểm tra và loại bỏ triệt để các khóa đã xuất hiện trong tập khóa có mặt thông qua `HashSet`.
  - Đảm bảo xác suất trúng khóa trong tập này chính xác bằng $0\%$.

### 2.3. Tập phân phối dồn cụm (Clustered Keys - Kiểm tra giới hạn RQ1)
- Áp dụng kiểm tra độ suy giảm hiệu năng của Linear Probing khi hàm băm bị suy biến cục bộ.
- **Quy tắc**:
  - $10\%$ số khóa được chọn lọc đặc biệt sao cho giá trị băm $h(k) < \frac{M}{64}$.
  - $90\%$ số khóa còn lại được phân phối đều ngẫu nhiên trên toàn bộ khoảng băm $[0, M)$.
  - Tải $\alpha = 0.75$, đo ở $M = 2^{18}$ và $M = 2^{20}$ (chạy 1 lượt `repeat = 1`).
  - Mã Java tương ứng là `RQ1DataGenerator.generateClusteredKeys(count, capacity, seed)`; thứ tự mảng giữ nhóm 10% khóa dồn cụm ở đầu để dễ xác minh.

### 2.4. Mã nguồn Generator RQ1
- `RQ1DataGenerator.generatePresentKeys(count, seed)` tạo khóa có mặt không trùng nhau.
- `RQ1DataGenerator.generateAbsentKeys(count, seed, presentKeys)` tạo khóa truy vấn không trùng nhau và không giao với tập có mặt.
- `RQ1DataGenerator.generateClusteredKeys(count, capacity, seed)` tạo phân phối dồn cụm theo quy tắc mục 2.3.
- Các phương thức dùng `SplittableRandom(seed)` và `HashSet` để tái lập kết quả, loại trùng; thời gian sinh dữ liệu nằm ngoài vùng benchmark.

---

## 3. Tính Toán Checksum SHA-256 của Đầu Vào
Để đảm bảo mọi lượt chạy của các thuật toán khác nhau đều nhận đúng một tập đầu vào giống hệt nhau mà không bị sai lệch:
- Mảng các khóa `long[]` được tuần tự hóa (serialize) thành chuỗi byte theo thứ tự **Big-Endian**:
  - Mỗi số `long` 64-bit chuyển thành 8 bytes liên tiếp:
    ```java
    for (int b = 7; b >= 0; b--) {
        byteBuf[offset++] = (byte) ((key >>> (b * 8)) & 0xFF);
    }
    ```
- Dùng `java.security.MessageDigest.getInstance("SHA-256")` băm toàn bộ chuỗi byte này thành mã hex 64 ký tự.
- Mã hex SHA-256 này được ghi trực tiếp vào cột `input_sha256` của file CSV kết quả đo.
