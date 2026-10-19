package hashtable.core;

/**
 * Trạng thái kết quả sau khi thực hiện thao tác put().
 * Theo quy ước mục 2 và mục 9 của đề tài.
 */
public enum PutStatus {
    /** Đã chèn khóa mới thành công (size tăng 1). */
    INSERTED,
    
    /** Khóa đã tồn tại từ trước, đã cập nhật giá trị mới (size không đổi). */
    UPDATED,
    
    /** Bảng đã đầy (đã dò M ô nhưng không tìm thấy vị trí chèn hợp lệ), không thay đổi dữ liệu. */
    TABLE_FULL
}
