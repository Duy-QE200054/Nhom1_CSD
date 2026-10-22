package hashtable.core;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Bộ sinh và điều phối Workload xóa-thêm (C chu kỳ) cho RQ2 (Nhiệm vụ Cặp 2 - C2).
 *
 * RQ2: Nghiên cứu tác động của hiện tượng tích tụ Tombstone khi không Rebuild (tau = inf)
 * so với bảng đối chứng sạch (Clean Table) qua C chu kỳ xóa - thêm liên tục:
 * - Khởi tạo bảng với N = round(alpha * M) khóa ban đầu.
 * - Mảng khóa đang có aliveKeys hỗ trợ chọn ngẫu nhiên O(1) bằng kỹ thuật swap-with-last.
 * - Khối 10^4 thao tác ghi nhận thời gian từng khối (đo sự suy giảm hiệu năng theo thời gian).
 * - Đo đạc tại mốc C: 10^4 truy vấn có mặt, 10^4 truy vấn vắng mặt, tỷ lệ TOMB/EMPTY, số lần dò.
 * - Xây dựng và đánh giá Bảng đối chứng sạch (Clean Table) cho RQ2.
 */
public class RQ2Workload {

    public static final int DEFAULT_BLOCK_SIZE = 10_000;
    public static final int DEFAULT_QUERY_COUNT = 10_000;

    private final int capacity;
    private final double alpha;
    private final int initialN;
    private final long seed;
    private final SplittableRandom rng;

    // Danh sách các khóa đang sống trong bảng (hỗ trợ chọn ngẫu nhiên O(1) để xóa)
    private long[] aliveKeys;
    private int aliveCount;

    // Bộ nhớ theo dõi các khóa đã sinh để chống trùng lặp
    private final HashSet<Long> seenKeys;

    /**
     * Dữ liệu thời gian của từng khối 10^4 thao tác xóa-thêm.
     */
    public static class BlockTiming {
        public final int blockIndex;
        public final int opsCount;
        public final int startCycle;
        public final int endCycle;
        public final long elapsedNs;
        public final double elapsedMs;
        public final int tombstoneCount;
        public final int emptyCount;
        public final double tombRatio;
        public final double emptyRatio;

        public BlockTiming(int blockIndex, int opsCount, int startCycle, int endCycle,
                           long elapsedNs, int tombstoneCount, int emptyCount, int capacity) {
            this.blockIndex = blockIndex;
            this.opsCount = opsCount;
            this.startCycle = startCycle;
            this.endCycle = endCycle;
            this.elapsedNs = elapsedNs;
            this.elapsedMs = elapsedNs / 1_000_000.0;
            this.tombstoneCount = tombstoneCount;
            this.emptyCount = emptyCount;
            this.tombRatio = (double) tombstoneCount / capacity;
            this.emptyRatio = (double) emptyCount / capacity;
        }

        @Override
        public String toString() {
            return String.format("Block #%3d (cycles %6d..%6d, %5d ops): %7.2f ms | TOMB: %6d (%.2f%%) | EMPTY: %6d (%.2f%%)",
                    blockIndex, startCycle, endCycle, opsCount, elapsedMs,
                    tombstoneCount, tombRatio * 100.0, emptyCount, emptyRatio * 100.0);
        }
    }

    /**
     * Kết quả đo đạc tại mốc C cho 10^4 truy vấn có mặt và 10^4 truy vấn vắng mặt.
     */
    public static class MilestoneCMeasurement {
        public final int milestoneC;
        public final String variant;
        public final int capacity;
        public final int aliveCount;
        public final int tombstoneCount;
        public final int emptyCount;
        public final double tombRatio;
        public final double emptyRatio;

        // 10^4 truy vấn có mặt (hit)
        public final int hitCount;
        public final double hitElapsedMs;
        public final double hitAvgProbes;
        public final long hitChecksum;

        // 10^4 truy vấn vắng mặt (miss)
        public final int missCount;
        public final double missElapsedMs;
        public final double missAvgProbes;
        public final long missChecksum;

        public MilestoneCMeasurement(int milestoneC, String variant, int capacity, int aliveCount,
                                     int tombstoneCount, int emptyCount,
                                     int hitCount, double hitElapsedMs, double hitAvgProbes, long hitChecksum,
                                     int missCount, double missElapsedMs, double missAvgProbes, long missChecksum) {
            this.milestoneC = milestoneC;
            this.variant = variant;
            this.capacity = capacity;
            this.aliveCount = aliveCount;
            this.tombstoneCount = tombstoneCount;
            this.emptyCount = emptyCount;
            this.tombRatio = (double) tombstoneCount / capacity;
            this.emptyRatio = (double) emptyCount / capacity;
            this.hitCount = hitCount;
            this.hitElapsedMs = hitElapsedMs;
            this.hitAvgProbes = hitAvgProbes;
            this.hitChecksum = hitChecksum;
            this.missCount = missCount;
            this.missElapsedMs = missElapsedMs;
            this.missAvgProbes = missAvgProbes;
            this.missChecksum = missChecksum;
        }
    }

    /**
     * Bảng đối chứng kết quả RQ2 giữa bảng tích tụ Tombstone và Clean Table.
     */
    public static class RQ2ComparisonResult {
        public final MilestoneCMeasurement tombTableResult;
        public final MilestoneCMeasurement cleanTableResult;

        public RQ2ComparisonResult(MilestoneCMeasurement tombTableResult, MilestoneCMeasurement cleanTableResult) {
            this.tombTableResult = tombTableResult;
            this.cleanTableResult = cleanTableResult;
        }

        public String toMarkdownTable() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n### BẢNG ĐỐI CHỨNG RQ2: BẢNG TÍCH TỤ TOMBSTONE vs CLEAN TABLE (Mốc C = ")
              .append(tombTableResult.milestoneC).append(")\n\n");
            sb.append("| Tiêu chí đo đạc | Bảng tích tụ Tombstone (base) | Bảng đối chứng sạch (clean_table) | Hệ số chênh lệch (Tomb / Clean) |\n");
            sb.append("|:---|:---:|:---:|:---:|\n");
            sb.append(String.format("| Dung lượng bảng $M$ | %,d | %,d | 1.00x |\n",
                    tombTableResult.capacity, cleanTableResult.capacity));
            sb.append(String.format("| Số khóa sống ($N$) | %,d | %,d | 1.00x |\n",
                    tombTableResult.aliveCount, cleanTableResult.aliveCount));
            sb.append(String.format("| Số ô TOMB (Tỷ lệ %%) | %,d (%.2f%%) | %,d (%.2f%%) | %s |\n",
                    tombTableResult.tombstoneCount, tombTableResult.tombRatio * 100.0,
                    cleanTableResult.tombstoneCount, cleanTableResult.tombRatio * 100.0,
                    cleanTableResult.tombstoneCount == 0 ? "N/A (Sạch hoàn toàn)" : "-"));
            sb.append(String.format("| Số ô EMPTY (Tỷ lệ %%) | %,d (%.2f%%) | %,d (%.2f%%) | %.2fx |\n",
                    tombTableResult.emptyCount, tombTableResult.emptyRatio * 100.0,
                    cleanTableResult.emptyCount, cleanTableResult.emptyRatio * 100.0,
                    cleanTableResult.emptyCount > 0 ? (double) tombTableResult.emptyCount / cleanTableResult.emptyCount : 0.0));
            sb.append(String.format("| Số lần dò TB Có mặt (Hit probes) | **%.3f** | **%.3f** | **%.2fx** |\n",
                    tombTableResult.hitAvgProbes, cleanTableResult.hitAvgProbes,
                    cleanTableResult.hitAvgProbes > 0 ? tombTableResult.hitAvgProbes / cleanTableResult.hitAvgProbes : 1.0));
            sb.append(String.format("| Thời gian tra cứu Có mặt (Hit time) | %.2f ms | %.2f ms | %.2fx |\n",
                    tombTableResult.hitElapsedMs, cleanTableResult.hitElapsedMs,
                    cleanTableResult.hitElapsedMs > 0 ? tombTableResult.hitElapsedMs / cleanTableResult.hitElapsedMs : 1.0));
            sb.append(String.format("| Số lần dò TB Vắng mặt (Miss probes) | **%.3f** | **%.3f** | **%.2fx** |\n",
                    tombTableResult.missAvgProbes, cleanTableResult.missAvgProbes,
                    cleanTableResult.missAvgProbes > 0 ? tombTableResult.missAvgProbes / cleanTableResult.missAvgProbes : 1.0));
            sb.append(String.format("| Thời gian tra cứu Vắng mặt (Miss time) | %.2f ms | %.2f ms | %.2fx |\n",
                    tombTableResult.missElapsedMs, cleanTableResult.missElapsedMs,
                    cleanTableResult.missElapsedMs > 0 ? tombTableResult.missElapsedMs / cleanTableResult.missElapsedMs : 1.0));
            sb.append(String.format("| Output Checksum (Hit / Miss) | %d / %d | %d / %d | %s |\n",
                    tombTableResult.hitChecksum, tombTableResult.missChecksum,
                    cleanTableResult.hitChecksum, cleanTableResult.missChecksum,
                    (tombTableResult.hitChecksum == cleanTableResult.hitChecksum
                     && tombTableResult.missChecksum == cleanTableResult.missChecksum) ? "KHỚP 100%" : "LỆCH"));
            return sb.toString();
        }

        public void printComparisonTable() {
            System.out.println(toMarkdownTable());
        }
    }

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
        this.seenKeys = new HashSet<>(initialN * 2 + 1024);
    }

    /**
     * Nạp N khóa ban đầu vào bảng.
     * Sử dụng chuỗi số ngẫu nhiên từ seed để sinh khóa duy nhất.
     */
    public void populateInitialKeys(HashTable table) {
        while (aliveCount < initialN) {
            long k = rng.nextLong(1, Long.MAX_VALUE);
            if (seenKeys.add(k)) {
                ensureCapacity(aliveCount + 1);
                aliveKeys[aliveCount++] = k;
                table.put(k, aliveCount);
            }
        }
    }

    /**
     * Chạy C chu kỳ xóa - thêm liên tục (Churn).
     * Tại mỗi chu kỳ:
     * - Xóa 1 khóa sống ngẫu nhiên qua mảng aliveKeys O(1).
     * - Thêm 1 khóa mới hoàn toàn.
     *
     * @param table Bảng đang thực nghiệm
     * @param cycles Số chu kỳ C (ví dụ: M/4, M/2, M, 2M, 4M)
     */
    public void runChurnCycles(HashTable table, int cycles) {
        runChurnCyclesWithBlockProfiling(table, cycles, 0, null);
    }

    /**
     * Chạy C chu kỳ xóa - thêm với đo đạc thời gian từng khối (mặc định khối 10^4 thao tác).
     *
     * @param table Bảng đang thực nghiệm
     * @param cycles Tổng số chu kỳ C
     * @param blockSizeOps Kích thước khối (ví dụ 10,000 thao tác). Nếu <= 0 thì không chia khối.
     * @param logWriter PrintWriter để ghi log khối trực tiếp ra console hoặc file (có thể null)
     * @return Danh sách các BlockTiming đã ghi nhận
     */
    public List<BlockTiming> runChurnCyclesWithBlockProfiling(HashTable table, int cycles,
                                                             int blockSizeOps, PrintWriter logWriter) {
        List<BlockTiming> blocks = new ArrayList<>();
        int effectiveBlockSize = (blockSizeOps > 0) ? blockSizeOps : DEFAULT_BLOCK_SIZE;

        int blockIndex = 0;
        int currentBlockStartCycle = 1;
        long blockStartNs = System.nanoTime();
        int opsInCurrentBlock = 0;

        for (int c = 0; c < cycles; c++) {
            if (aliveCount == 0) {
                break;
            }

            // 1. Chọn ngẫu nhiên một khóa đang sống để xóa - ĐỘ PHỨC TẠP O(1)
            int victimIdx = rng.nextInt(aliveCount);
            long keyToRemove = aliveKeys[victimIdx];

            // Kỹ thuật Swap-with-last: Hoán đổi với phần tử cuối cùng để xóa O(1)
            aliveKeys[victimIdx] = aliveKeys[aliveCount - 1];
            aliveCount--;
            table.remove(keyToRemove);
            opsInCurrentBlock++;

            // 2. Sinh khóa mới duy nhất và chèn vào
            long newKey;
            do {
                newKey = rng.nextLong(1, Long.MAX_VALUE);
            } while (!seenKeys.add(newKey));

            ensureCapacity(aliveCount + 1);
            aliveKeys[aliveCount++] = newKey;
            table.put(newKey, c + 1);
            opsInCurrentBlock++;

            // Kiểm tra khi đủ một khối (effectiveBlockSize thao tác, tức effectiveBlockSize/2 chu kỳ)
            if (opsInCurrentBlock >= effectiveBlockSize || c == cycles - 1) {
                long elapsedNs = System.nanoTime() - blockStartNs;
                blockIndex++;
                BlockTiming timing = new BlockTiming(
                        blockIndex, opsInCurrentBlock, currentBlockStartCycle, c + 1,
                        elapsedNs, table.getTombstoneCount(), table.getEmptyCount(), table.capacity()
                );
                blocks.add(timing);

                if (logWriter != null) {
                    logWriter.println(timing);
                    logWriter.flush();
                }

                // Bắt đầu khối tiếp theo
                opsInCurrentBlock = 0;
                currentBlockStartCycle = c + 2;
                blockStartNs = System.nanoTime();
            }
        }

        return blocks;
    }

    /**
     * Thực hiện đo đạc tại mốc C:
     * - 10^4 truy vấn có mặt (thời gian + số lần dò).
     * - 10^4 truy vấn vắng mặt (thời gian + số lần dò).
     * - Thống kê tỷ lệ TOMB/EMPTY.
     *
     * @param table Bảng đang xét (base hoặc clean_table)
     * @param milestoneC Mốc C hiện tại
     * @param queryCount Số lượng truy vấn mỗi loại (mặc định 10^4 = 10,000)
     * @param querySeed Hạt giống sinh truy vấn để đảm bảo tính tái lập
     * @param variant Tên biến thể ("base" hoặc "clean_table")
     * @return Đối tượng MilestoneCMeasurement chứa đầy đủ kết quả đo
     */
    public MilestoneCMeasurement measureAtMilestoneC(HashTable table, int milestoneC,
                                                     int queryCount, long querySeed, String variant) {
        int qCount = (queryCount > 0) ? queryCount : DEFAULT_QUERY_COUNT;
        SplittableRandom qRng = new SplittableRandom(querySeed);

        // 1. Chuẩn bị tập khóa có mặt (Hit): chọn ngẫu nhiên từ aliveKeys
        long[] hitKeys = new long[qCount];
        for (int i = 0; i < qCount; i++) {
            hitKeys[i] = aliveKeys[qRng.nextInt(aliveCount)];
        }

        // 2. Chuẩn bị tập khóa vắng mặt (Miss): sinh khóa mới đảm bảo 100% không trùng với seenKeys
        long[] missKeys = new long[qCount];
        for (int i = 0; i < qCount; i++) {
            long mk;
            do {
                mk = qRng.nextLong(1, Long.MAX_VALUE);
            } while (seenKeys.contains(mk));
            missKeys[i] = mk;
        }

        // 3. Đo thời gian tra cứu Có mặt (Hit time)
        long hitStartNs = System.nanoTime();
        long hitChecksum = 0;
        for (long k : hitKeys) {
            GetResult r = table.get(k);
            if (r.isFound()) {
                hitChecksum += r.getValue();
            } else {
                hitChecksum++;
            }
        }
        double hitElapsedMs = (System.nanoTime() - hitStartNs) / 1_000_000.0;

        // 4. Đo số bước dò trung bình Có mặt (Hit probes)
        table.enableProbeCounting(true);
        table.resetProbeCount();
        for (long k : hitKeys) {
            table.get(k);
        }
        long hitTotalProbes = table.getProbeCount();
        double hitAvgProbes = (double) hitTotalProbes / qCount;
        table.enableProbeCounting(false);

        // 5. Đo thời gian tra cứu Vắng mặt (Miss time)
        long missStartNs = System.nanoTime();
        long missChecksum = 0;
        for (long k : missKeys) {
            GetResult r = table.get(k);
            if (r.isFound()) {
                missChecksum += r.getValue();
            } else {
                missChecksum++;
            }
        }
        double missElapsedMs = (System.nanoTime() - missStartNs) / 1_000_000.0;

        // 6. Đo số bước dò trung bình Vắng mặt (Miss probes)
        table.enableProbeCounting(true);
        table.resetProbeCount();
        for (long k : missKeys) {
            table.get(k);
        }
        long missTotalProbes = table.getProbeCount();
        double missAvgProbes = (double) missTotalProbes / qCount;
        table.enableProbeCounting(false);

        return new MilestoneCMeasurement(
                milestoneC, variant, table.capacity(), table.size(),
                table.getTombstoneCount(), table.getEmptyCount(),
                qCount, hitElapsedMs, hitAvgProbes, hitChecksum,
                qCount, missElapsedMs, missAvgProbes, missChecksum
        );
    }

    /**
     * Dựng bảng đối chứng Clean Table từ các khóa sống hiện có của bảng nguồn.
     * Bảng sạch có cùng dung lượng M nhưng không hề chứa tombstone nào.
     */
    public LinearProbingTombstone buildCleanTable(LinearProbingTombstone source) {
        return source.createCleanTable();
    }

    /**
     * So sánh đối chứng giữa bảng tích tụ Tombstone và Clean Table tại mốc C.
     *
     * @param tableWithTombs Bảng LinearProbingTombstone sau khi đã chạy C chu kỳ
     * @param milestoneC Mốc C
     * @param queryCount Số lượng truy vấn (mặc định 10^4)
     * @param querySeed Hạt giống truy vấn
     * @return Kết quả đối chứng đầy đủ
     */
    public RQ2ComparisonResult compareWithCleanTable(LinearProbingTombstone tableWithTombs,
                                                     int milestoneC, int queryCount, long querySeed) {
        MilestoneCMeasurement tombResult = measureAtMilestoneC(tableWithTombs, milestoneC, queryCount, querySeed, "base");
        LinearProbingTombstone cleanTable = buildCleanTable(tableWithTombs);
        MilestoneCMeasurement cleanResult = measureAtMilestoneC(cleanTable, milestoneC, queryCount, querySeed, "clean_table");
        return new RQ2ComparisonResult(tombResult, cleanResult);
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
