package hashtable.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Cài đặt bảng băm Linear Probing với cơ chế xóa dịch lùi (lp-backshift) - Nhiệm vụ Cặp 3 (C3).
 *
 * Thuật toán xóa: Knuth Algorithm R (The Art of Computer Programming, Vol. 3, §6.4).
 *
 * Lý do chọn Knuth Algorithm R thay vì Sedgewick (chèn lại cả cụm):
 * 1. Hiệu năng tối ưu (Single-pass in-place):
 *    - Knuth Algorithm R chỉ duyệt một vòng duy nhất O(K) dọc theo cụm đụng độ,
 *      kiểm tra điều kiện vòng tròn modulo M và chỉ dịch chuyển các phần tử thực sự cần thiết
 *      vào ô trống vừa mở ra.
 *    - Ngược lại, Sedgewick xóa ô đích rồi gỡ từng phần tử phía sau ra chèn lại (re-insert)
 *      bằng put(). Mỗi lần put() phải băm lại khóa và thực hiện vòng lặp dò tìm từ đầu,
 *      dẫn đến độ phức tạp tiềm tàng O(K^2) trong cụm và lãng phí chu kỳ CPU.
 * 2. Tối ưu bộ nhớ đệm (Cache & Write Locality):
 *    - Algorithm R chỉ ghi trực tiếp (move) các phần tử cần di dời vào ô trống i,
 *      giảm thiểu số lần ghi vào RAM và giữ trọn vẹn thứ tự dồn cụm tự nhiên.
 * 3. Chuẩn mực học thuật:
 *    - Hoàn toàn khớp với tài liệu tham khảo bắt buộc [1] Knuth TAOCP đã được phân công và trích dẫn.
 */
public class LinearProbingBackshift implements HashTable {

    public static final byte EMPTY = 0;
    public static final byte OCCUPIED = 1;

    private final int capacity;       // M = 2^p
    private final int p;              // p = log2(M)
    private final int mask;           // M - 1

    private final long[] keys;        // Mảng khóa nguyên thủy
    private final int[] values;       // Mảng giá trị nguyên thủy
    private final byte[] state;       // Trạng thái: EMPTY hoặc OCCUPIED

    private int size;                 // Số lượng phần tử sống

    private boolean probeCounting;    // Cờ bật/tắt đếm bước dò
    private long probeCount;          // Tổng số bước dò tích lũy

    /**
     * Khởi tạo bảng Linear Probing Backshift với dung lượng capacity M = 2^p.
     *
     * @param capacity Dung lượng bảng (lũy thừa của 2 dương)
     */
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

        this.size = 0;
        this.probeCounting = false;
        this.probeCount = 0;
    }

    @Override
    public PutStatus put(long key, int value) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;

        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }

            byte s = state[idx];
            if (s == EMPTY) {
                keys[idx] = key;
                values[idx] = value;
                state[idx] = OCCUPIED;
                size++;
                return PutStatus.INSERTED;
            } else if (keys[idx] == key) {
                values[idx] = value;
                return PutStatus.UPDATED;
            }

            idx = (idx + 1) & mask;
        }

        return PutStatus.TABLE_FULL;
    }

    @Override
    public GetResult get(long key) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;

        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }

            byte s = state[idx];
            if (s == EMPTY) {
                return GetResult.NOT_FOUND;
            } else if (keys[idx] == key) {
                return GetResult.of(values[idx]);
            }

            idx = (idx + 1) & mask;
        }

        return GetResult.NOT_FOUND;
    }

    /**
     * Xóa khóa sử dụng Knuth Algorithm R:
     * Quét cụm đụng độ phía sau, chuyển phần tử thích hợp về lấp ô trống.
     * Hoàn toàn không tạo ô TOMB.
     */
    @Override
    public boolean remove(long key) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;
        int foundIdx = -1;

        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }

            byte s = state[idx];
            if (s == EMPTY) {
                return false;
            } else if (keys[idx] == key) {
                foundIdx = idx;
                break;
            }

            idx = (idx + 1) & mask;
        }

        if (foundIdx == -1) {
            return false;
        }

        // Áp dụng Knuth Algorithm R
        int i = foundIdx; // Ô trống cần lấp
        int j = i;

        for (int step = 0; step < capacity; step++) {
            j = (j + 1) & mask;
            if (state[j] == EMPTY || j == foundIdx) {
                break;
            }

            int r = FibonacciHash.hash(keys[j], p); // Home bucket của phần tử tại j

            // Kiểm tra r có nằm trong khoảng xoay vòng (i, j] hay không
            // Nếu r nằm trong (i, j], phần tử này KHÔNG được chuyển về i
            boolean between = (i <= j) ? (i < r && r <= j) : (i < r || r <= j);
            if (!between) {
                keys[i] = keys[j];
                values[i] = values[j];
                state[i] = OCCUPIED;

                i = j; // Ô j trở thành ô trống mới cần lấp
            }
        }

        state[i] = EMPTY;
        keys[i] = 0L;
        values[i] = 0;
        size--;

        return true;
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
        return 0; // Linear Probing Backshift không dùng ô TOMB
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
                int h = FibonacciHash.hash(keys[i], p);
                int dib = (i - h + capacity) & mask;
                totalDIB += dib;
            }
        }
        return (double) totalDIB / size;
    }

    @Override
    public int getMaxDIB() {
        int max = 0;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                int h = FibonacciHash.hash(keys[i], p);
                int dib = (i - h + capacity) & mask;
                if (dib > max) {
                    max = dib;
                }
            }
        }
        return max;
    }

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
        if (state[index] != OCCUPIED) {
            return 0;
        }
        int h = FibonacciHash.hash(keys[index], p);
        return (index - h + capacity) & mask;
    }

    /**
     * Kiểm tra bất biến cấu trúc:
     * 1. Không có ô EMPTY nào chặn giữa ô băm gốc (home) và vị trí của khóa (index).
     * 2. DIB tính được đúng và hợp lệ.
     * 3. Số ô OCCUPIED khớp với size.
     * 4. Không có khóa trùng lặp.
     */
    public void validateInvariants() {
        int occupiedCount = 0;
        Set<Long> seenKeys = new HashSet<>();
        for (int index = 0; index < capacity; index++) {
            if (state[index] == EMPTY) {
                continue;
            }
            if (state[index] != OCCUPIED) {
                throw new IllegalStateException("Trạng thái ô không xác định tại chỉ số " + index);
            }

            occupiedCount++;
            long key = keys[index];
            if (!seenKeys.add(key)) {
                throw new IllegalStateException("Phát hiện khóa trùng lặp: " + key);
            }
            int home = FibonacciHash.hash(key, p);
            int cursor = home;
            while (cursor != index) {
                if (state[cursor] == EMPTY) {
                    throw new IllegalStateException(
                            "Ô EMPTY tại chỉ số " + cursor + " chặn đường dò tới khóa " + key);
                }
                cursor = (cursor + 1) & mask;
            }
            int expectedDib = (index - home + capacity) & mask;
            if (expectedDib < 0 || expectedDib >= capacity) {
                throw new IllegalStateException("DIB không hợp lệ cho khóa " + key + ": " + expectedDib);
            }
        }
        if (occupiedCount != size) {
            throw new IllegalStateException(
                    "Số ô chiếm dụng " + occupiedCount + " không khớp với size " + size);
        }
    }
}
