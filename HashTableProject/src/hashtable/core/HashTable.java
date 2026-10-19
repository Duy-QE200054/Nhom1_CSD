package hashtable.core;

/**
 * Giao diện chung cho tất cả các cấu hình Hash Table:
 * - sc (Separate Chaining)
 * - lp-tomb (Linear Probing với Tombstone & Rebuild)
 * - lp-backshift (Linear Probing với xóa dịch lùi)
 * - rh (Robin Hood đầy đủ)
 * - rh-noearly (Robin Hood không dừng sớm)
 */
public interface HashTable {

    /**
     * Thêm cặp (key, value) vào bảng hoặc cập nhật giá trị nếu khóa đã tồn tại.
     * Bảng không tự mở rộng.
     *
     * @param key Khóa kiểu long (không âm)
     * @param value Giá trị kiểu int
     * @return Trạng thái: INSERTED, UPDATED hoặc TABLE_FULL
     */
    PutStatus put(long key, int value);

    /**
     * Tra cứu giá trị ứng với khóa key.
     *
     * @param key Khóa cần tra cứu
     * @return GetResult chứa found = true/false và giá trị value nếu tìm thấy
     */
    GetResult get(long key);

    /**
     * Xóa khóa key khỏi bảng.
     *
     * @param key Khóa cần xóa
     * @return true nếu tìm thấy và xóa thành công, false nếu khóa không tồn tại
     */
    boolean remove(long key);

    /**
     * Số lượng phần tử (khóa sống) hiện có trong bảng.
     */
    int size();

    /**
     * Dung lượng bảng M = 2^p.
     */
    int capacity();

    /**
     * Bật hoặc tắt chế độ đếm số bước dò (chỉ bật trong lượt --count theo mục 6.5).
     */
    void enableProbeCounting(boolean enable);

    /**
     * Đặt lại bộ đếm số bước dò về 0.
     */
    void resetProbeCount();

    /**
     * Lấy tổng số bước dò đã tích lũy trong các thao tác vừa thực hiện.
     */
    long getProbeCount();

    /**
     * Lấy số lượng ô ở trạng thái TOMB (chỉ áp dụng cho lp-tomb, trả về 0 cho các loại khác).
     */
    int getTombstoneCount();

    /**
     * Lấy số lượng ô trống EMPTY hiện tại.
     */
    int getEmptyCount();

    /**
     * Lấy khoảng cách dò trung bình (DIB) của các khóa sống (chỉ áp dụng cho rh, trả về -1 nếu không hỗ trợ).
     */
    double getAverageDIB();

    /**
     * Lấy khoảng cách dò lớn nhất (Max DIB) của các khóa sống (chỉ áp dụng cho rh, trả về -1 nếu không hỗ trợ).
     */
    int getMaxDIB();
}
