# Biên Bản Review Chéo Mã Nguồn (Peer Code Review): C1 → C2
## Module Được Review: `LinearProbingTombstone.java` (Cặp 2 - C2)
**Người review:** Cặp 1 (C1) - Phụ trách RQ1 & Đối chứng thuật toán  
**Người được review:** Cặp 2 (C2) - Phụ trách Thuật toán Linear Probing Tombstone & Rebuild (RQ2)  
**Thời gian review:** 28/10/2026  
**File mã nguồn đối chiếu:** [LinearProbingTombstone.java](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java)  
**Mã commit xác thực:** `f3b706b` và `719ff86`  

---

### Tóm Tắt Kết Quả Review
- **Trạng thái chung:** ✅ **APPROVED (ĐẠT CHUẨN 100% CẢ 5 ĐIỂM KỸ THUẬT BẮT BUỘC)**
- **Đánh giá:** Module được lập trình chuẩn mực, tuân thủ nghiêm ngặt đặc tả kỹ thuật của Đề tài 2, sử dụng mảng phẳng nguyên thủy song song tối ưu hiệu năng và xử lý chính xác tuyệt đối các trường hợp biên.

---

### Chi Tiết Kiểm Định 5 Điểm Kỹ Thuật Bắt Buộc

#### 1. Cơ chế `firstTomb`: Ghi nhớ ô TOMB đầu tiên và chống trùng lặp khóa
- **Tiêu chí:** `put()` phải ghi nhớ vị trí ô `TOMB` đầu tiên gặp phải trên chuỗi dò (`firstTomb`), nhưng chỉ được chèn vào `firstTomb` sau khi duyệt hết cụm mà chắc chắn rằng khóa chưa tồn tại. Nếu gặp khóa đã tồn tại trong ô `OCCUPIED`, phải cập nhật giá trị tại chỗ (`UPDATED`) và không được chèn vào `firstTomb`.
- **Dòng mã đối chiếu trong repo:** [LinearProbingTombstone.java#L123-L174](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L123-L174)
- **Đánh giá:**
  ```java
  // Dòng 138-142: Ghi nhớ ô TOMB đầu tiên
  else if (s == TOMB) {
      if (firstTomb == -1) {
          firstTomb = idx;
      }
  } else { // OCCUPIED
      if (keys[idx] == key) {
          values[idx] = value;
          return PutStatus.UPDATED; // Cập nhật tại chỗ, không dùng firstTomb!
      }
  }
  ```
  => **ĐẠT CHUẨN:** Tránh hoàn toàn lỗi sinh ra hai bản sao (duplicate keys) của cùng một khóa trong bảng băm.

---

#### 2. Cơ chế `EMPTY stop`: Dừng ở EMPTY và tiếp tục duyệt qua TOMB
- **Tiêu chí:** Tra cứu phải dừng ngay lập tức khi gặp ô `EMPTY` (trả về `NOT_FOUND`), nhưng bắt buộc phải tiếp tục chuỗi dò khi gặp ô `TOMB` vì khóa cần tìm có thể nằm phía sau ô tombstone.
- **Dòng mã đối chiếu trong repo:** [LinearProbingTombstone.java#L177-L198](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L177-L198)
- **Đánh giá:**
  ```java
  // Dòng 186-194:
  byte s = state[idx];
  if (s == EMPTY) {
      return GetResult.NOT_FOUND; // Dừng ngay khi gặp EMPTY
  } else if (s == OCCUPIED && keys[idx] == key) {
      return GetResult.of(values[idx]);
  }
  // Nếu là TOMB: tiếp tục bước dò tiếp theo mà không dừng
  idx = (idx + 1) & mask;
  ```
  => **ĐẠT CHUẨN:** Đảm bảo tính đúng đắn cho các truy vấn tra cứu sau các thao tác xóa liên tiếp.

---

#### 3. Cơ chế `tau * M rebuild`: Bộ đếm `tombstoneCount` và ngưỡng tự động tái cấu trúc
- **Tiêu chí:** Bộ đếm `tombstoneCount` phải theo dõi chính xác: tăng 1 khi xóa (`remove`), giảm 1 khi tái sử dụng ô tombstone (`put` vào `firstTomb`). Tự động kích hoạt `rebuild()` khi `tombstoneCount >= tau * capacity`.
- **Dòng mã đối chiếu trong repo:** [LinearProbingTombstone.java#L158-L160](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L158-L160) và [LinearProbingTombstone.java#L220-L226](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L220-L226)
- **Đánh giá:**
  ```java
  // Dòng 158-159: Giảm khi tái sử dụng tombstone
  insertIdx = firstTomb;
  tombstoneCount--;

  // Dòng 220-225: Tăng khi xóa và kiểm tra ngưỡng
  tombstoneCount++;
  if (tauThreshold > 0 && tombstoneCount >= tauThreshold) {
      rebuild();
  }
  ```
  => **ĐẠT CHUẨN:** Ngưỡng `tauThreshold` được tính chuẩn bằng `Math.round(tau * capacity)`, tự động kích hoạt tái cấu trúc đúng cam kết của RQ2.

---

#### 4. Cơ chế `index wrap`: Phép toán chỉ số vòng với mặt nạ bit lũy thừa 2
- **Tiêu chí:** Dung lượng bảng $M = 2^p$, sử dụng mặt nạ bit `mask = capacity - 1` với phép toán `(idx + 1) & mask` để xử lý vòng tròn khi chuỗi va chạm vượt qua cuối mảng.
- **Dòng mã đối chiếu trong repo:** [LinearProbingTombstone.java#L78-L79](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L78-L79), [L151](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L151), [L194](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L194)
- **Đánh giá:**
  ```java
  this.mask = capacity - 1;
  idx = (idx + 1) & mask;
  ```
  => **ĐẠT CHUẨN:** Sử dụng phép toán bitwise `& mask` thay vì phép chia modulo `% M`, đạt tốc độ tối đa của chu kỳ CPU và an toàn với biên mảng.

---

#### 5. Cơ chế `size update`: Cập nhật biến đếm phần tử sống và bảo toàn bất biến
- **Tiêu chí:** Cập nhật biến `size` chính xác: tăng 1 khi chèn khóa mới (`INSERTED`), không thay đổi khi cập nhật khóa cũ (`UPDATED`), giảm 1 khi xóa thành công (`remove`). Phương thức `validateInvariants()` kiểm tra số ô `OCCUPIED == size`.
- **Dòng mã đối chiếu trong repo:** [LinearProbingTombstone.java#L146](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L146), [L171](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L171), [L219](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L219), [L436-L439](../HashTableProject/src/hashtable/core/LinearProbingTombstone.java#L436-L439)
- **Đánh giá:**
  ```java
  // Dòng 146: Cập nhật khóa cũ -> size KHÔNG đổi
  return PutStatus.UPDATED;

  // Dòng 171: Chèn mới -> size tăng 1
  size++;
  return PutStatus.INSERTED;

  // Dòng 219: Xóa thành công -> size giảm 1
  size--;

  // Dòng 436-438: Bất biến nghiệm thu
  if (occupiedCount != size) {
      throw new AssertionError("Số ô OCCUPIED không khớp với size");
  }
  ```
  => **ĐẠT CHUẨN:** Đảm bảo bất biến kích thước bảng băm luôn đồng nhất trong suốt quá trình hoạt động và rebuild.

---

### Kết luận Review
Mã nguồn của Cặp 2 (`LinearProbingTombstone.java`) hoàn toàn đáp ứng trọn vẹn cả 5 điểm kỹ thuật cốt lõi theo yêu cầu. Cặp 1 đồng thuận nghiệm thu module này!
