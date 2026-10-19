package hashtable.core;

/**
 * Kết quả trả về của thao tác get(key).
 * Vì giá trị int có thể là bất kỳ số nào (kể cả 0 hoặc số âm),
 * cần có cờ found riêng để phân biệt rõ ràng giữa tìm thấy và không tìm thấy.
 */
public final class GetResult {
    private final boolean found;
    private final int value;

    public static final GetResult NOT_FOUND = new GetResult(false, 0);

    public GetResult(boolean found, int value) {
        this.found = found;
        this.value = value;
    }

    public static GetResult of(int value) {
        return new GetResult(true, value);
    }

    public boolean isFound() {
        return found;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return found ? ("Found(" + value + ")") : "NotFound";
    }
}
