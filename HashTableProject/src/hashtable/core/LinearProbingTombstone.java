package hashtable.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Cài đặt bảng băm Linear Probing sử dụng Tombstone và Rebuild định kỳ (lp-tomb).
 * Phục vụ Đề tài 2 - CSD201 FA26 (Nhiệm vụ Cặp 2 - C2).
 *
 * Đặc tả kỹ thuật:
 * 1. Ba mảng phẳng nguyên thủy song song:
 *    - keys: long[] lưu khóa (không âm)
 *    - values: int[] lưu giá trị
 *    - state: byte[] lưu trạng thái của từng ô (EMPTY = 0, OCCUPIED = 1, TOMB = 2)
 * 2. Logic put():
 *    - Dò tuyến tính từ h(k) = FibonacciHash.hash(key, p).
 *    - Ghi nhớ chỉ số TOMB đầu tiên gặp trong chuỗi dò (firstTomb).
 *    - Nếu gặp OCCUPIED có keys[idx] == key -> cập nhật values[idx], trả về UPDATED (không dùng firstTomb).
 *    - Nếu gặp EMPTY -> dừng dò:
 *        + Nếu firstTomb != -1: chèn vào firstTomb, giảm tombstoneCount.
 *        + Nếu firstTomb == -1: chèn vào idx (ô EMPTY).
 *        + Tăng size, cập nhật state thành OCCUPIED, trả về INSERTED.
 *    - Nếu duyệt hết M ô mà không thấy key:
 *        + Nếu firstTomb != -1: chèn vào firstTomb, giảm tombstoneCount, tăng size, trả về INSERTED.
 *        + Nếu firstTomb == -1: bảng đầy (M ô đều OCCUPIED), trả về TABLE_FULL.
 * 3. Bộ đếm tombstone:
 *    - tombstoneCount theo dõi chính xác số ô TOMB hiện thời.
 * 4. Rebuild định kỳ theo ngưỡng tau * M:
 *    - Khi tombstoneCount >= tau * M (với tau hữu hạn > 0), tự động kích hoạt rebuild().
 *    - Duyệt qua mảng cũ theo đúng THỨ TỰ CHỈ SỐ Ô từ 0 đến M - 1.
 *    - Chèn lại tất cả các khóa sống (OCCUPIED) vào bảng mới sạch tombstones.
 */
public class LinearProbingTombstone implements HashTable {

    public static final byte EMPTY = 0;
    public static final byte OCCUPIED = 1;
    public static final byte TOMB = 2;

    private final int capacity;       // M = 2^p
    private final int p;              // p = log2(M)
    private final int mask;           // M - 1

    private long[] keys;              // Mảng khóa nguyên thủy
    private int[] values;             // Mảng giá trị nguyên thủy
    private byte[] state;             // Trạng thái ô: EMPTY, OCCUPIED, TOMB

    private int size;                 // Số lượng phần tử sống (OCCUPIED)
    private int tombstoneCount;       // Số lượng ô TOMB hiện tại

    private double tau;               // Tỉ lệ ngưỡng rebuild (tau * M)
    private int tauThreshold;         // Ngưỡng số lượng tombstone để kích hoạt rebuild
    private long rebuildCount;        // Đếm số lần đã rebuild

    private boolean probeCounting;    // Cờ bật/tắt đếm bước dò
    private long probeCount;          // Tổng số bước dò tích lũy

    /**
     * Khởi tạo bảng với dung lượng capacity (M = 2^p), không tự động rebuild (tau = vô cùng).
     *
     * @param capacity Dung lượng bảng (phải là lũy thừa của 2, ví dụ: 256, 1024, 262144, ...)
     */
    public LinearProbingTombstone(int capacity) {
        this(capacity, Double.POSITIVE_INFINITY);
    }

    /**
     * Khởi tạo bảng với dung lượng capacity và tỉ lệ ngưỡng rebuild tau.
     *
     * @param capacity Dung lượng bảng M = 2^p
     * @param tau Ngưỡng rebuild (ví dụ 0.2 nghĩa là khi tombstoneCount >= 0.2 * M thì rebuild).
     *            Nếu tau <= 0 hoặc vô cùng thì không tự động rebuild.
     */
    public LinearProbingTombstone(int capacity, double tau) {
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity M phải là lũy thừa của 2 dương, nhận được: " + capacity);
        }
        this.capacity = capacity;
        this.p = Integer.numberOfTrailingZeros(capacity);
        this.mask = capacity - 1;

        this.keys = new long[capacity];
        this.values = new int[capacity];
        this.state = new byte[capacity]; // Mặc định Java khởi tạo mảng byte với giá trị 0 (EMPTY)

        this.size = 0;
        this.tombstoneCount = 0;
        this.rebuildCount = 0;
        this.probeCounting = false;
        this.probeCount = 0;

        setTau(tau);
    }

    /**
     * Cập nhật tham số tau và tính toán lại ngưỡng kích hoạt rebuild.
     */
    public final void setTau(double tau) {
        this.tau = tau;
        if (Double.isInfinite(tau) || Double.isNaN(tau) || tau <= 0.0 || tau > 1.0) {
            this.tauThreshold = -1; // Vô hiệu hóa tự động rebuild
        } else {
            this.tauThreshold = Math.max(1, (int) Math.round(tau * capacity));
            // Nếu số tombstone hiện có đã vượt quá ngưỡng mới, kích hoạt rebuild ngay
            if (this.tombstoneCount >= this.tauThreshold) {
                rebuild();
            }
        }
    }

    public double getTau() {
        return tau;
    }

    public int getTauThreshold() {
        return tauThreshold;
    }

    public long getRebuildCount() {
        return rebuildCount;
    }

    @Override
    public PutStatus put(long key, int value) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;
        int firstTomb = -1;

        // Dò tuyến tính tối đa capacity bước
        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }

            byte s = state[idx];
            if (s == EMPTY) {
                // Gặp ô rỗng: chắc chắn khóa chưa từng tồn tại trong chuỗi dò này
                break;
            } else if (s == TOMB) {
                // Nhớ vị trí TOMB đầu tiên gặp phải để tái sử dụng nếu khóa là mới
                if (firstTomb == -1) {
                    firstTomb = idx;
                }
            } else { // OCCUPIED
                if (keys[idx] == key) {
                    // Khóa đã tồn tại: cập nhật giá trị tại chỗ, không di chuyển sang firstTomb
                    values[idx] = value;
                    return PutStatus.UPDATED;
                }
            }

            idx = (idx + 1) & mask;
        }

        // Chọn vị trí chèn
        int insertIdx;
        if (firstTomb != -1) {
            // Tái sử dụng ô TOMB đầu tiên đã ghi nhớ
            insertIdx = firstTomb;
            tombstoneCount--; // Ô TOMB này được chuyển thành OCCUPIED
        } else if (state[idx] == EMPTY) {
            // Không có TOMB nào trên đường đi, chèn vào ô EMPTY tìm thấy
            insertIdx = idx;
        } else {
            // Duyệt hết M ô mà tất cả đều là OCCUPIED (không có EMPTY và không có TOMB)
            return PutStatus.TABLE_FULL;
        }

        keys[insertIdx] = key;
        values[insertIdx] = value;
        state[insertIdx] = OCCUPIED;
        size++;

        return PutStatus.INSERTED;
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
                // Gặp ô rỗng: khóa không tồn tại
                return GetResult.NOT_FOUND;
            } else if (s == OCCUPIED && keys[idx] == key) {
                return GetResult.of(values[idx]);
            }
            // Nếu là TOMB hoặc OCCUPIED khác key: tiếp tục dò bước kế tiếp
            idx = (idx + 1) & mask;
        }

        return GetResult.NOT_FOUND;
    }

    @Override
    public boolean remove(long key) {
        int h = FibonacciHash.hash(key, p);
        int idx = h;

        for (int step = 0; step < capacity; step++) {
            if (probeCounting) {
                probeCount++;
            }

            byte s = state[idx];
            if (s == EMPTY) {
                // Dừng dò khi gặp ô EMPTY: khóa không tồn tại
                return false;
            } else if (s == OCCUPIED && keys[idx] == key) {
                // Tìm thấy khóa: đánh dấu ô thành TOMB
                state[idx] = TOMB;
                keys[idx] = 0L;
                values[idx] = 0;
                size--;
                tombstoneCount++;

                // Kiểm tra điều kiện rebuild theo ngưỡng tau * M
                if (tauThreshold > 0 && tombstoneCount >= tauThreshold) {
                    rebuild();
                }
                return true;
            }

            idx = (idx + 1) & mask;
        }

        return false;
    }

    /**
     * Tái cấu trúc bảng (Rebuild) để loại bỏ toàn bộ các ô TOMB.
     * Quy tắc bắt buộc theo đặc tả của C2:
     * - Quét mảng cũ và chèn lại các khóa sống THEO THỨ TỰ CHỈ SỐ Ô từ 0 đến capacity - 1.
     * - Bảng mới có cùng capacity M, tombstoneCount = 0.
     */
    public void rebuild() {
        long[] newKeys = new long[capacity];
        int[] newValues = new int[capacity];
        byte[] newState = new byte[capacity];

        // Chèn lại theo thứ tự chỉ số ô: 0, 1, 2, ..., capacity - 1
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                long k = keys[i];
                int v = values[i];

                int h = FibonacciHash.hash(k, p);
                int idx = h;
                // Dò tìm ô trống đầu tiên trong bảng mới
                while (newState[idx] == OCCUPIED) {
                    idx = (idx + 1) & mask;
                }
                newKeys[idx] = k;
                newValues[idx] = v;
                newState[idx] = OCCUPIED;
            }
        }

        this.keys = newKeys;
        this.values = newValues;
        this.state = newState;
        this.tombstoneCount = 0;
        this.rebuildCount++;
    }

    /**
     * Tạo bảng đối chứng sạch (Clean Table) cho RQ2:
     * Chứa đúng các khóa đang sống tại thời điểm hiện tại, chèn lại theo thứ tự chỉ số ô,
     * không chứa bất kỳ tombstone nào và có tau = vô cùng.
     *
     * @return Bảng LinearProbingTombstone đối chứng sạch hoàn toàn
     */
    public LinearProbingTombstone createCleanTable() {
        LinearProbingTombstone clean = new LinearProbingTombstone(this.capacity, Double.POSITIVE_INFINITY);
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                clean.put(keys[i], values[i]);
            }
        }
        return clean;
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
        return tombstoneCount;
    }

    @Override
    public int getEmptyCount() {
        return capacity - size - tombstoneCount;
    }

    @Override
    public double getAverageDIB() {
        return -1.0;
    }

    @Override
    public int getMaxDIB() {
        return -1;
    }

    /**
     * Tính toán khoảng cách dò trung bình (DIB) thực tế của các khóa sống hiện có.
     */
    public double computeActualAverageDIB() {
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

    /**
     * Tính toán khoảng cách dò lớn nhất (Max DIB) thực tế của các khóa sống hiện có.
     */
    public int computeActualMaxDIB() {
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

    // Các phương thức getter hỗ trợ kiểm tra trạng thái mảng nội bộ trong Unit Test
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
     * Kiểm tra bất biến cấu trúc của bảng băm Linear Probing Tombstone:
     * 1. Với mọi ô OCCUPIED lưu khóa k tại chỉ số index, truy vết từ ô băm gốc home = h(k)
     *    đến index, KHÔNG ĐƯỢC có ô nào mang trạng thái EMPTY chặn ở giữa (chỉ có thể là OCCUPIED hoặc TOMB).
     * 2. Khoảng cách DIB tính được phải đúng và nằm trong khoảng [0, capacity - 1].
     * 3. Số ô OCCUPIED phải khớp đúng với size.
     * 4. Số ô TOMB phải khớp đúng với tombstoneCount.
     * 5. Số ô EMPTY phải khớp đúng với emptyCount.
     * 6. Không có khóa trùng lặp.
     */
    public void validateInvariants() {
        int occupiedCount = 0;
        int tombCount = 0;
        Set<Long> seenKeys = new HashSet<>();
        for (int index = 0; index < capacity; index++) {
            if (state[index] == EMPTY) {
                continue;
            }
            if (state[index] == TOMB) {
                tombCount++;
                continue;
            }
            if (state[index] != OCCUPIED) {
                throw new IllegalStateException("Trạng thái ô không hợp lệ tại chỉ số " + index + ": " + state[index]);
            }

            occupiedCount++;
            long key = keys[index];
            if (!seenKeys.add(key)) {
                throw new IllegalStateException("Phát hiện khóa trùng lặp trong bảng: " + key);
            }
            int home = FibonacciHash.hash(key, p);
            int cursor = home;
            // Bất biến cấu trúc: Không được có ô EMPTY nào nằm giữa ô băm gốc (home) và vị trí khóa (index)
            while (cursor != index) {
                if (state[cursor] == EMPTY) {
                    throw new IllegalStateException(
                            "Ô EMPTY tại chỉ số " + cursor + " chặn đường dò tới khóa " + key + " tại ô " + index + " (home=" + home + ")");
                }
                cursor = (cursor + 1) & mask;
            }
            // Kiểm tra DIB đúng
            int expectedDib = (index - home + capacity) & mask;
            if (expectedDib < 0 || expectedDib >= capacity) {
                throw new IllegalStateException("DIB không hợp lệ cho khóa " + key + ": " + expectedDib);
            }
        }
        if (occupiedCount != size) {
            throw new IllegalStateException(
                    "Số ô OCCUPIED (" + occupiedCount + ") không khớp với size (" + size + ")");
        }
        if (tombCount != tombstoneCount) {
            throw new IllegalStateException(
                    "Số ô TOMB thực tế (" + tombCount + ") không khớp với tombstoneCount (" + tombstoneCount + ")");
        }
        if (capacity - occupiedCount - tombCount != getEmptyCount()) {
            throw new IllegalStateException(
                    "Số ô EMPTY thực tế (" + (capacity - occupiedCount - tombCount)
                            + ") không khớp với emptyCount (" + getEmptyCount() + ")");
        }
    }
}
