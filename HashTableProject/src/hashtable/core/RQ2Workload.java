package hashtable.core;

import java.util.SplittableRandom;

/**
 * Bộ sinh và điều phối Workload xóa-thêm (C chu kỳ) cho RQ2 (Nhiệm vụ Cặp 2 - C2).
 *
 * RQ2: Nghiên cứu tác động của hiện tượng tích tụ Tombstone khi không Rebuild (tau = inf)
 * so với bảng đối chứng sạch (Clean Table) qua C chu kỳ xóa - thêm liên tục:
 * - Khởi tạo bảng với N = round(alpha * M) khóa ban đầu.
 * - Tại mỗi chu kỳ c trong 1..C:
 *     1. Chọn ngẫu nhiên một khóa đang sống để xóa: remove(k_old).
 *     2. Sinh một khóa mới chưa từng xuất hiện và chèn vào: put(k_new, value).
 * - Kết quả:
 *     + Bảng lp-tomb bị tích tụ tombstones (các ô EMPTY giảm dần hoặc biến mất hoàn toàn).
 *     + Tạo bảng đối chứng Clean Table từ các khóa sống tại mốc C để đối sánh.
 */
public class RQ2Workload {

    private final int capacity;
    private final double alpha;
    private final int initialN;
    private final long seed;
    private final SplittableRandom rng;

    // Danh sách các khóa đang sống trong bảng (hỗ trợ chọn ngẫu nhiên O(1) để xóa)
    private long[] aliveKeys;
    private int aliveCount;

    /**
     * Khởi tạo đối tượng quản lý Workload RQ2.
     *
     * @param capacity Dung lượng bảng M = 2^p
     * @param alpha Hệ số tải ban đầu (ví dụ 0.50)
     * @param seed Hạt giống ngẫu nhiên để đảm bảo tính tái lập (reproducibility)
     */
    public RQ2Workload(int capacity, double alpha, long seed) {
        this.capacity = capacity;
        this.alpha = alpha;
        this.initialN = (int) Math.round(alpha * capacity);
        this.seed = seed;
        this.rng = new SplittableRandom(seed);
        this.aliveKeys = new long[initialN * 2 + 1024];
        this.aliveCount = 0;
    }

    /**
     * Nạp N khóa ban đầu vào bảng.
     * Sử dụng chuỗi số ngẫu nhiên từ seed để sinh khóa duy nhất.
     */
    public void populateInitialKeys(LinearProbingTombstone table) {
        java.util.HashSet<Long> seen = new java.util.HashSet<>(initialN * 2);
        while (aliveCount < initialN) {
            long k = rng.nextLong(1, Long.MAX_VALUE);
            if (seen.add(k)) {
                ensureCapacity(aliveCount + 1);
                aliveKeys[aliveCount++] = k;
                table.put(k, aliveCount);
            }
        }
    }

    /**
     * Chạy C chu kỳ xóa - thêm liên tục (Churn).
     * Tại mỗi chu kỳ:
     * - Xóa 1 khóa sống ngẫu nhiên (chuyển thành TOMB).
     * - Thêm 1 khóa mới hoàn toàn (có thể tái sử dụng TOMB đầu tiên hoặc chiếm ô EMPTY).
     *
     * @param table Bảng đang thực nghiệm
     * @param cycles Số chu kỳ C (ví dụ: M/4, M/2, M, 2M, 4M)
     */
    public void runChurnCycles(LinearProbingTombstone table, int cycles) {
        java.util.HashSet<Long> seen = new java.util.HashSet<>();
        for (int i = 0; i < aliveCount; i++) {
            seen.add(aliveKeys[i]);
        }

        for (int c = 0; c < cycles; c++) {
            if (aliveCount == 0) {
                break;
            }

            // 1. Chọn ngẫu nhiên một khóa đang sống để xóa
            int victimIdx = rng.nextInt(aliveCount);
            long keyToRemove = aliveKeys[victimIdx];

            // Hoán đổi với phần tử cuối cùng để xóa O(1) khỏi danh sách aliveKeys
            aliveKeys[victimIdx] = aliveKeys[aliveCount - 1];
            aliveCount--;
            table.remove(keyToRemove);

            // 2. Sinh khóa mới duy nhất và chèn vào
            long newKey;
            do {
                newKey = rng.nextLong(1, Long.MAX_VALUE);
            } while (!seen.add(newKey));

            ensureCapacity(aliveCount + 1);
            aliveKeys[aliveCount++] = newKey;
            table.put(newKey, c + 1);
        }
    }

    /**
     * Dựng bảng đối chứng Clean Table từ các khóa sống hiện có của bảng nguồn.
     * Bảng sạch có cùng dung lượng M nhưng không hề chứa tombstone nào.
     */
    public LinearProbingTombstone buildCleanTable(LinearProbingTombstone source) {
        return source.createCleanTable();
    }

    public int getAliveCount() {
        return aliveCount;
    }

    public long[] getAliveKeysCopy() {
        long[] copy = new long[aliveCount];
        System.arraycopy(aliveKeys, 0, copy, 0, aliveCount);
        return copy;
    }

    private void ensureCapacity(int minCap) {
        if (minCap > aliveKeys.length) {
            int newCap = Math.max(aliveKeys.length * 2, minCap + 1024);
            long[] next = new long[newCap];
            System.arraycopy(aliveKeys, 0, next, 0, aliveCount);
            aliveKeys = next;
        }
    }
}
