package hashtable.core;

/**
 * Hàm băm Fibonacci theo chuẩn mục 3.1 của đề tài:
 * h(k) = (int)((k * 0x9E3779B97F4A7C15L) >>> (64 - p))
 * Luôn nằm trong đoạn [0, M) với M = 2^p.
 */
public final class FibonacciHash {
    public static final long FIBONACCI_CONSTANT = 0x9E3779B97F4A7C15L;

    private FibonacciHash() {}

    /**
     * Tính chỉ số băm gốc h(k) từ khóa k và số bit lũy thừa p (với M = 2^p).
     *
     * @param key Khóa số nguyên không âm (long >= 0)
     * @param p Số bit của kích thước bảng (M = 1 << p)
     * @return Chỉ số bucket trong khoảng [0, M - 1]
     */
    public static int hash(long key, int p) {
        if (p <= 0 || p > 30) {
            throw new IllegalArgumentException("p phải nằm trong khoảng [1, 30], nhận được: " + p);
        }
        return (int) ((key * FIBONACCI_CONSTANT) >>> (64 - p));
    }

    /**
     * Hàm băm Fibonacci cho chuỗi ký tự theo chuẩn Biến thể B:
     * h(s) = hash((long) s.hashCode(), p)
     * Ép kiểu s.hashCode() sang long rồi áp dụng hàm băm Fibonacci.
     *
     * @param s Chuỗi ký tự (12 ký tự chữ in hoa + chữ số)
     * @param p Số bit của kích thước bảng (M = 1 << p)
     * @return Chỉ số bucket trong khoảng [0, M - 1]
     */
    public static int hash(String s, int p) {
        if (s == null) {
            throw new IllegalArgumentException("Chuỗi s không được null");
        }
        return hash((long) s.hashCode(), p);
    }

    /**
     * Chuyển đổi chuỗi thành khóa long theo quy chuẩn Biến thể B: (long) s.hashCode()
     *
     * @param s Chuỗi ký tự
     * @return Khóa long tương ứng
     */
    public static long toKey(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Chuỗi s không được null");
        }
        return (long) s.hashCode();
    }
}

