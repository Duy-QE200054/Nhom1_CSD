package hashtable.core;

/**
 * Biến thể Robin Hood không áp dụng early-exit trong lookup để làm đối chứng đo lường.
 * Các cơ chế chèn và xóa vẫn dùng DIB/hoán đổi như Robin Hood chuẩn, chỉ bỏ qua dừng sớm.
 */
public class RobinHoodNoEarly extends RobinHood {
    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;

    public RobinHoodNoEarly(int capacity) {
        super(capacity);
    }

    @Override
    public GetResult get(long key) {
        validateKey(key);
        int idx = FibonacciHash.hash(key, p);
        for (int step = 0; step < capacity; step++) {
            if (isProbeCounting()) {
                incrementProbeCount();
            }
            if (getStateAt(idx) == EMPTY) {
                return GetResult.NOT_FOUND;
            }
            if (getStateAt(idx) == OCCUPIED && getKeyAt(idx) == key) {
                return GetResult.of(getValueAt(idx));
            }
            idx = (idx + 1) & mask;
        }
        return GetResult.NOT_FOUND;
    }
}

