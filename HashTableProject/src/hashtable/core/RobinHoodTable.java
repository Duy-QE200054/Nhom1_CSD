package hashtable.core;

/**
 * Cài đặt bảng băm Robin Hood (rh và rh-noearly) - Nhiệm vụ Cặp 3 (C3).
 *
 * Các cơ chế cốt lõi theo đề tài:
 * 1. DIB (Distance from Initial Bucket):
 *    - Khoảng cách từ vị trí ô hiện tại đến home bucket h(k) = FibonacciHash.hash(key, p).
 * 2. Robin Hood Swap ("Lấy của người giàu chia cho người nghèo"):
 *    - Khi chèn, nếu khoảng cách DIB của khóa đang xét lớn hơn DIB của khóa trong ô hiện tại,
 *      thực hiện hoán đổi hai khóa và tiếp tục đẩy phần tử bị hoán đổi đi tìm vị trí mới.
 * 3. Early Exit (Dừng sớm khi tra cứu):
 *    - Trong get() và remove(), nếu khoảng cách dò hiện tại > DIB của ô đang xét,
 *      khẳng định ngay khóa không tồn tại trong bảng mà không cần tiếp tục dò đến EMPTY.
 *    - Cờ earlyExit cho phép bật/tắt cơ chế này (true = rh chuẩn, false = rh-noearly phục vụ RQ3 ablation).
 * 4. Backward-Shift Deletion (Xóa dịch lùi):
 *    - Khi xóa một khóa, các phần tử kế tiếp trong cụm có DIB > 0 được dịch lùi 1 vị trí (DIB giảm 1)
 *      cho đến khi gặp ô EMPTY hoặc phần tử có DIB == 0.
 *    - Hoàn toàn KHÔNG tạo tombstone, giữ nguyên tính sắp xếp của DIB trong cụm.
 */
public class RobinHoodTable implements HashTable {

    public static final byte EMPTY = 0;
    public static final byte OCCUPIED = 1;

    private final int capacity;       // M = 2^p
    private final int p;              // p = log2(M)
    private final int mask;           // M - 1
    private final boolean earlyExit;  // true: rh chuẩn; false: rh-noearly

    private long[] keys;              // Mảng khóa nguyên thủy
    private int[] values;             // Mảng giá trị nguyên thủy
    private int[] dib;                // Mảng lưu DIB của từng ô
    private byte[] state;             // Trạng thái ô: EMPTY, OCCUPIED

    private int size;                 // Số lượng phần tử sống

    private boolean probeCounting;    // Cờ bật/tắt đếm bước dò
    private long probeCount;          // Tổng số bước dò tích lũy

    /**
     * Khởi tạo bảng Robin Hood chuẩn (có bật dừng sớm Early Exit).
     *
     * @param capacity Dung lượng bảng M = 2^p
     */
    public RobinHoodTable(int capacity) {
        this(capacity, true);
    }

    /**
     * Khởi tạo bảng Robin Hood với cờ cấu hình Early Exit.
     *
     * @param capacity Dung lượng bảng M = 2^p
     * @param earlyExit true nếu bật dừng sớm (rh), false nếu tắt dừng sớm (rh-noearly)
     */
    public RobinHoodTable(int capacity, boolean earlyExit) {
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity M phải là lũy thừa của 2 dương, nhận được: " + capacity);
        }
        this.capacity = capacity;
        this.p = Integer.numberOfTrailingZeros(capacity);
        this.mask = capacity - 1;
        this.earlyExit = earlyExit;

        this.keys = new long[capacity];
        this.values = new int[capacity];
        this.dib = new int[capacity];
        this.state = new byte[capacity];

        this.size = 0;
        this.probeCounting = false;
        this.probeCount = 0;
    }

    public boolean isEarlyExitEnabled() {
        return earlyExit;
    }

    @Override
    public PutStatus put(long key, int value) {
        // 1. Kiểm tra khóa đã tồn tại trong bảng để cập nhật tại chỗ
        int existingIdx = findKeyIndex(key);
        if (existingIdx != -1) {
            values[existingIdx] = value;
            return PutStatus.UPDATED;
        }

        // 2. Bảng đầy
        if (size >= capacity) {
            return PutStatus.TABLE_FULL;
        }

        // 3. Chèn Robin Hood với cơ chế hoán đổi
        long curKey = key;
        int curVal = value;
        int curDib = 0;
        int idx = FibonacciHash.hash(key, p);

        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }

            if (state[idx] == EMPTY) {
                keys[idx] = curKey;
                values[idx] = curVal;
                dib[idx] = curDib;
                state[idx] = OCCUPIED;
                size++;
                return PutStatus.INSERTED;
            }

            // Hoán đổi nếu khóa đang mang có DIB lớn hơn khóa trong ô
            if (curDib > dib[idx]) {
                long tmpKey = keys[idx];
                int tmpVal = values[idx];
                int tmpDib = dib[idx];

                keys[idx] = curKey;
                values[idx] = curVal;
                dib[idx] = curDib;

                curKey = tmpKey;
                curVal = tmpVal;
                curDib = tmpDib;
            }

            curDib++;
            idx = (idx + 1) & mask;
        }

        return PutStatus.TABLE_FULL;
    }

    @Override
    public GetResult get(long key) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;

        for (int curDib = 0; curDib < capacity; curDib++) {
            if (probeCounting) {
                probeCount++;
            }

            if (state[idx] == EMPTY) {
                return GetResult.NOT_FOUND;
            }

            if (state[idx] == OCCUPIED) {
                if (keys[idx] == key) {
                    return GetResult.of(values[idx]);
                }
                // Dừng sớm nếu bật earlyExit và khoảng cách dò hiện tại lớn hơn DIB của ô
                if (earlyExit && curDib > dib[idx]) {
                    return GetResult.NOT_FOUND;
                }
            }

            idx = (idx + 1) & mask;
        }

        return GetResult.NOT_FOUND;
    }

    @Override
    public boolean remove(long key) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;
        int targetIdx = -1;

        for (int curDib = 0; curDib < capacity; curDib++) {
            if (probeCounting) {
                probeCount++;
            }

            if (state[idx] == EMPTY) {
                return false;
            }

            if (state[idx] == OCCUPIED) {
                if (keys[idx] == key) {
                    targetIdx = idx;
                    break;
                }
                if (earlyExit && curDib > dib[idx]) {
                    return false;
                }
            }

            idx = (idx + 1) & mask;
        }

        if (targetIdx == -1) {
            return false;
        }

        // Xóa dịch lùi (Backward-shift deletion)
        int curr = targetIdx;
        while (true) {
            int next = (curr + 1) & mask;
            if (state[next] == EMPTY || dib[next] == 0) {
                // Ô tiếp theo rỗng hoặc phần tử ở ô tiếp theo ở đúng home bucket (DIB == 0)
                state[curr] = EMPTY;
                keys[curr] = 0L;
                values[curr] = 0;
                dib[curr] = 0;
                break;
            }

            // Dịch lùi phần tử từ next sang curr
            keys[curr] = keys[next];
            values[curr] = values[next];
            dib[curr] = dib[next] - 1; // Giảm DIB đi 1
            state[curr] = OCCUPIED;

            curr = next;
        }

        size--;
        return true;
    }

    private int findKeyIndex(long key) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;
        for (int curDib = 0; curDib < capacity; curDib++) {
            if (state[idx] == EMPTY) {
                return -1;
            }
            if (state[idx] == OCCUPIED) {
                if (keys[idx] == key) {
                    return idx;
                }
                if (earlyExit && curDib > dib[idx]) {
                    return -1;
                }
            }
            idx = (idx + 1) & mask;
        }
        return -1;
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
        return 0; // Robin Hood không sử dụng ô TOMB
    }

    @Override
    public int getEmptyCount() {
        return capacity - size;
    }

    @Override
    public double getAverageDIB() {
        if (size == 0) {
            return 0.0;
        }
        long totalDIB = 0;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                totalDIB += dib[i];
            }
        }
        return (double) totalDIB / size;
    }

    @Override
    public int getMaxDIB() {
        int max = 0;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED && dib[i] > max) {
                max = dib[i];
            }
        }
        return max;
    }

    // Các getter hỗ trợ kiểm tra chi tiết trong Unit Test
    public byte getStateAt(int index) {
        return state[index];
    }

    public long getKeyAt(int index) {
        return keys[index];
    }

    public int getValueAt(int index) {
        return values[index];
    }

    public int getDibAt(int index) {
        return dib[index];
    }
}
