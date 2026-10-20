package hashtable.core;

/**
 * Linear Probing với xóa dịch lùi (backshift deletion) theo kiểu Knuth Algorithm R.
 *
 * Lý do chọn Knuth Algorithm R thay vì Sedgewick:
 * 1. Giữ nguyên cấu trúc dò tuyến tính, không cần đánh dấu tombstone.
 * 2. Dịch các phần tử phía sau về trái trong-place để lấp ô trống, đồng thời vẫn bảo toàn
 *    chuỗi dò hoạt động đúng sau khi xóa.
 * 3. Đây là cách tương thích tốt hơn với mục tiêu benchmark RQ3 khi cần so sánh trực tiếp
 *    với Robin Hood: cùng một mô hình dò tuyến tính, nhưng khác ở cơ chế hoán đổi DIB và
 *    dừng sớm.
 */
public class LinearProbingBackshift implements HashTable {
    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;

    private final int capacity;
    private final int p;
    private final int mask;
    private final long[] keys;
    private final int[] values;
    private final byte[] state;

    private int size;
    private boolean probeCounting;
    private long probeCount;

    public LinearProbingBackshift(int capacity) {
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity M phải là lũy thừa của 2 dương, nhận được: " + capacity);
        }
        this.capacity = capacity;
        this.p = Integer.numberOfTrailingZeros(capacity);
        this.mask = capacity - 1;
        this.keys = new long[capacity];
        this.values = new int[capacity];
        this.state = new byte[capacity];
    }

    @Override
    public PutStatus put(long key, int value) {
        validateKey(key);
        int idx = FibonacciHash.hash(key, p);
        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }
            if (state[idx] == EMPTY) {
                keys[idx] = key;
                values[idx] = value;
                state[idx] = OCCUPIED;
                size++;
                return PutStatus.INSERTED;
            }
            if (state[idx] == OCCUPIED && keys[idx] == key) {
                values[idx] = value;
                return PutStatus.UPDATED;
            }
            idx = (idx + 1) & mask;
        }
        return PutStatus.TABLE_FULL;
    }

    @Override
    public GetResult get(long key) {
        validateKey(key);
        int idx = FibonacciHash.hash(key, p);
        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }
            if (state[idx] == EMPTY) {
                return GetResult.NOT_FOUND;
            }
            if (state[idx] == OCCUPIED && keys[idx] == key) {
                return GetResult.of(values[idx]);
            }
            idx = (idx + 1) & mask;
        }
        return GetResult.NOT_FOUND;
    }

    @Override
    public boolean remove(long key) {
        validateKey(key);
        int idx = FibonacciHash.hash(key, p);

        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }
            if (state[idx] == EMPTY) {
                return false;
            }
            if (state[idx] == OCCUPIED && keys[idx] == key) {
                int hole = idx;
                int next = (idx + 1) & mask;

                while (state[next] == OCCUPIED) {
                    int nextHome = FibonacciHash.hash(keys[next], p);
                    if (distanceFromHome(next, nextHome) == 0) {
                        break;
                    }
                    keys[hole] = keys[next];
                    values[hole] = values[next];
                    state[hole] = OCCUPIED;

                    state[next] = EMPTY;
                    keys[next] = 0L;
                    values[next] = 0;
                    hole = next;
                    next = (next + 1) & mask;
                }

                state[hole] = EMPTY;
                keys[hole] = 0L;
                values[hole] = 0;
                size--;
                return true;
            }
            idx = (idx + 1) & mask;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void enableProbeCounting(boolean enable) {
        this.probeCounting = enable;
    }

    @Override
    public void resetProbeCount() {
        this.probeCount = 0;
    }

    @Override
    public long getProbeCount() {
        return probeCount;
    }

    @Override
    public int getTombstoneCount() {
        return 0;
    }

    @Override
    public int getEmptyCount() {
        return capacity - size;
    }

    @Override
    public double getAverageDIB() {
        return -1.0;
    }

    @Override
    public int getMaxDIB() {
        return -1;
    }

    private int distanceFromHome(int index, int home) {
        return (index - home + capacity) & mask;
    }

    private static void validateKey(long key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key phải không âm, nhận được: " + key);
        }
    }
}
