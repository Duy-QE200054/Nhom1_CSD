package hashtable.test;

import hashtable.core.FibonacciHash;
import hashtable.core.GetResult;
import hashtable.core.LinearProbingTombstone;
import hashtable.core.PutStatus;
import hashtable.core.RQ2Workload;

import java.util.HashMap;
import java.util.SplittableRandom;

/**
 * Bộ kiểm thử toàn diện cho LinearProbingTombstone (lp-tomb) - Cặp 2 (C2).
 * Kiểm tra:
 * 1. Chức năng cơ bản: put, get, remove, size, capacity.
 * 2. Logic "nhớ TOMB đầu tiên": chèn khóa mới vào ô TOMB đầu tiên tìm thấy.
 * 3. Cập nhật khóa hiện có: cập nhật tại chỗ, không ghi đè vào TOMB.
 * 4. Tái cấu trúc (Rebuild) theo ngưỡng tau * M và duyệt theo thứ tự chỉ số ô.
 * 5. Bộ đếm tombstoneCount và emptyCount.
 * 6. Đếm số bước dò (Probe Counting).
 * 7. Bảng đối chứng Clean Table.
 * 8. Kiểm thử Oracle với java.util.HashMap (100,000 thao tác ngẫu nhiên).
 */
public class LinearProbingTombstoneTest {

    public static void main(String[] args) {
        runAllTests();
    }

    public static void runAllTests() {
        System.out.println("--- Bắt đầu Unit Test cho LinearProbingTombstone (Cặp 2 - C2) ---");
        testBasicOperations();
        testRememberFirstTombstoneOnPut();
        testUpdateKeyWithPriorTombstone();
        testMultipleTombstonesFirstSelection();
        testRebuildTriggeredByTau();
        testRebuildOrderPreservation();
        testTableFullCondition();
        testProbeCounting();
        testCleanTableCreation();
        testOracleWithHashMap(100_000, 42L);
        testWorkloadBlockProfiling();
        testMilestoneCMeasurementAndComparisonTable();
        System.out.println("--- TẤT CẢ CÁC BÀI TEST CỦA C2 ĐÃ VƯỢT QUA THÀNH CÔNG! ---\n");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("THẤT BẠI: " + msg);
        }
    }

    private static void assertEquals(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new AssertionError("THẤT BẠI: " + msg + " (Mong đợi: " + expected + ", Thực tế: " + actual + ")");
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError("THẤT BẠI: " + msg + " (Mong đợi: " + expected + ", Thực tế: " + actual + ")");
        }
    }

    /**
     * 1. Kiểm tra các thao tác cơ bản: put, get, remove
     */
    public static void testBasicOperations() {
        System.out.print("[Test 1] Thao tác cơ bản (put, get, remove)... ");
        LinearProbingTombstone table = new LinearProbingTombstone(16);
        assertEquals(16, table.capacity(), "Capacity phải là 16");
        assertEquals(0, table.size(), "Size ban đầu phải là 0");
        assertEquals(16, table.getEmptyCount(), "Empty ban đầu phải là 16");
        assertEquals(0, table.getTombstoneCount(), "Tombstone ban đầu phải là 0");

        // Put khóa
        assertEquals(PutStatus.INSERTED, table.put(100L, 10), "Chèn 100L");
        assertEquals(PutStatus.INSERTED, table.put(200L, 20), "Chèn 200L");
        assertEquals(2, table.size(), "Size sau 2 lần chèn");
        table.validateInvariants();

        // Get khóa
        GetResult r1 = table.get(100L);
        assertTrue(r1.isFound(), "Phải tìm thấy 100L");
        assertEquals(10, r1.getValue(), "Giá trị 100L");

        GetResult r2 = table.get(200L);
        assertTrue(r2.isFound(), "Phải tìm thấy 200L");
        assertEquals(20, r2.getValue(), "Giá trị 200L");

        GetResult rMiss = table.get(999L);
        assertTrue(!rMiss.isFound(), "Không được tìm thấy 999L");

        // Remove khóa
        assertTrue(table.remove(100L), "Xóa 100L thành công");
        assertEquals(1, table.size(), "Size sau khi xóa 1");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count phải là 1");
        assertTrue(!table.get(100L).isFound(), "Không còn tìm thấy 100L sau khi xóa");
        table.validateInvariants();

        // Xóa lại khóa đã xóa
        assertTrue(!table.remove(100L), "Xóa lại 100L phải trả về false");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra logic cốt lõi: Ghi nhớ TOMB đầu tiên khi put khóa mới
     */
    public static void testRememberFirstTombstoneOnPut() {
        System.out.print("[Test 2] Logic ghi nhớ TOMB đầu tiên khi put khóa mới... ");
        int M = 16;
        int p = Integer.numberOfTrailingZeros(M);
        LinearProbingTombstone table = new LinearProbingTombstone(M);

        // Tìm 4 khóa khác nhau nhưng có cùng giá trị băm h(k)
        long k1 = -1, k2 = -1, k3 = -1, k4 = -1;
        int targetH = 3;
        for (long k = 1; k < 100_000; k++) {
            if (FibonacciHash.hash(k, p) == targetH) {
                if (k1 == -1) k1 = k;
                else if (k2 == -1) k2 = k;
                else if (k3 == -1) k3 = k;
                else if (k4 == -1) { k4 = k; break; }
            }
        }

        // Chèn 3 khóa: k1 ở targetH, k2 ở targetH+1, k3 ở targetH+2
        table.put(k1, 1);
        table.put(k2, 2);
        table.put(k3, 3);
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH), "k1 tại targetH");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 1), "k2 tại targetH + 1");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 2), "k3 tại targetH + 2");
        table.validateInvariants();

        // Xóa k2 -> ô targetH + 1 trở thành TOMB
        table.remove(k2);
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "Ô targetH + 1 phải là TOMB");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count = 1");
        table.validateInvariants();

        // Chèn khóa mới k4 (cũng băm về targetH):
        // Dò từ targetH (k1: occupied khác) -> targetH+1 (TOMB: nhớ firstTomb) -> targetH+2 (k3: occupied khác) -> targetH+3 (EMPTY: dừng dò).
        // Chèn k4 vào firstTomb (targetH+1)!
        PutStatus status = table.put(k4, 4);
        assertEquals(PutStatus.INSERTED, status, "k4 phải được chèn thành công");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 1), "targetH + 1 phải được tái sử dụng");
        assertEquals(k4, table.getKeyAt(targetH + 1), "Khóa tại targetH + 1 phải là k4");
        assertEquals(0, table.getTombstoneCount(), "Tombstone count phải giảm về 0");
        table.validateInvariants();

        // Kiểm tra k3 vẫn tra cứu được bình thường
        GetResult rK3 = table.get(k3);
        assertTrue(rK3.isFound(), "k3 vẫn phải tìm thấy");
        assertEquals(3, rK3.getValue(), "Giá trị của k3");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra: Khi cập nhật khóa đã tồn tại nằm sau TOMB, KHÔNG được ghi đè vào TOMB
     */
    public static void testUpdateKeyWithPriorTombstone() {
        System.out.print("[Test 3] Cập nhật khóa hiện có (không ghi đè TOMB)... ");
        int M = 16;
        int p = Integer.numberOfTrailingZeros(M);
        LinearProbingTombstone table = new LinearProbingTombstone(M);

        long k1 = -1, k2 = -1, k3 = -1;
        int targetH = 5;
        for (long k = 1; k < 100_000; k++) {
            if (FibonacciHash.hash(k, p) == targetH) {
                if (k1 == -1) k1 = k;
                else if (k2 == -1) k2 = k;
                else if (k3 == -1) { k3 = k; break; }
            }
        }

        table.put(k1, 10);
        table.put(k2, 20);
        table.put(k3, 30);

        // Xóa k2
        table.remove(k2);
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "targetH + 1 là TOMB");

        // Cập nhật k3 (nằm sau ô TOMB)
        PutStatus updateStatus = table.put(k3, 300);
        assertEquals(PutStatus.UPDATED, updateStatus, "Phải trả về UPDATED");
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "targetH + 1 vẫn phải là TOMB");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 2), "k3 vẫn nằm ở targetH + 2");
        assertEquals(300, table.getValueAt(targetH + 2), "Giá trị k3 đã cập nhật thành 300");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count vẫn là 1");
        System.out.println("PASSED");
    }

    /**
     * 4. Kiểm tra: Khi có nhiều ô TOMB, chọn đúng ô TOMB đầu tiên
     */
    public static void testMultipleTombstonesFirstSelection() {
        System.out.print("[Test 4] Chọn đúng ô TOMB đầu tiên trong nhiều ô TOMB... ");
        int M = 16;
        int p = Integer.numberOfTrailingZeros(M);
        LinearProbingTombstone table = new LinearProbingTombstone(M);

        long k1 = -1, k2 = -1, k3 = -1, k4 = -1;
        int targetH = 1;
        for (long k = 1; k < 100_000; k++) {
            if (FibonacciHash.hash(k, p) == targetH) {
                if (k1 == -1) k1 = k;
                else if (k2 == -1) k2 = k;
                else if (k3 == -1) k3 = k;
                else if (k4 == -1) { k4 = k; break; }
            }
        }

        table.put(k1, 1);
        table.put(k2, 2);
        table.put(k3, 3);

        // Xóa cả k1 và k2 -> targetH và targetH + 1 đều là TOMB
        table.remove(k1);
        table.remove(k2);
        assertEquals(2, table.getTombstoneCount(), "Có 2 tombstones");

        // Chèn k4 (cùng băm về targetH) -> phải rơi vào targetH (ô TOMB đầu tiên)
        table.put(k4, 4);
        assertEquals(k4, table.getKeyAt(targetH), "k4 phải nằm tại targetH");
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "targetH + 1 vẫn là TOMB");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count giảm còn 1");
        System.out.println("PASSED");
    }

    /**
     * 5. Kiểm tra tự động Rebuild khi tombstoneCount đạt ngưỡng tau * M
     */
    public static void testRebuildTriggeredByTau() {
        System.out.print("[Test 5] Tự động Rebuild khi tombstoneCount >= tau * M... ");
        int M = 16;
        double tau = 0.25; // 0.25 * 16 = 4 tombstones
        LinearProbingTombstone table = new LinearProbingTombstone(M, tau);
        assertEquals(4, table.getTauThreshold(), "Ngưỡng rebuild là 4");

        // Chèn 8 khóa
        for (long i = 1; i <= 8; i++) {
            table.put(i, (int) i * 10);
        }

        // Xóa 3 khóa đầu: 1, 2, 3 -> tombstoneCount = 3, chưa rebuild
        table.remove(1L);
        table.remove(2L);
        table.remove(3L);
        assertEquals(3, table.getTombstoneCount(), "Tombstone count = 3");
        assertEquals(0, table.getRebuildCount(), "Chưa rebuild");

        // Xóa khóa thứ 4: 4L -> tombstoneCount chạm 4 -> KÍCH HOẠT REBUILD
        table.remove(4L);
        assertEquals(1, table.getRebuildCount(), "Đã kích hoạt 1 lần rebuild");
        assertEquals(0, table.getTombstoneCount(), "Sau rebuild tombstoneCount phải về 0");
        assertEquals(4, table.size(), "Còn lại đúng 4 phần tử sống");

        // Xác nhận 4 phần tử sống (5, 6, 7, 8) vẫn tra cứu chính xác
        for (long i = 5; i <= 8; i++) {
            GetResult gr = table.get(i);
            assertTrue(gr.isFound(), "Phần tử " + i + " phải còn");
            assertEquals((int) i * 10, gr.getValue(), "Giá trị phần tử " + i);
        }
        System.out.println("PASSED");
    }

    /**
     * 6. Kiểm tra Rebuild duyệt theo đúng THỨ TỰ CHỈ SỐ Ô từ 0 đến M - 1
     */
    public static void testRebuildOrderPreservation() {
        System.out.print("[Test 6] Rebuild duyệt theo đúng thứ tự chỉ số ô (0..M-1)... ");
        int M = 32;
        LinearProbingTombstone table = new LinearProbingTombstone(M);
        SplittableRandom sr = new SplittableRandom(12345L);
        for (int i = 0; i < 15; i++) {
            table.put(sr.nextLong(1, 100_000), i);
        }
        // Xóa một vài khóa tạo tombstone
        int deleted = 0;
        for (int i = 0; i < M && deleted < 5; i++) {
            if (table.getStateAt(i) == LinearProbingTombstone.OCCUPIED) {
                table.remove(table.getKeyAt(i));
                deleted++;
            }
        }
        assertTrue(table.getTombstoneCount() > 0, "Có tombstones");

        int sizeBefore = table.size();
        table.rebuild();
        assertEquals(sizeBefore, table.size(), "Size giữ nguyên sau rebuild");
        assertEquals(0, table.getTombstoneCount(), "Tombstone = 0 sau rebuild");
        assertEquals(M - sizeBefore, table.getEmptyCount(), "Empty = M - size");
        System.out.println("PASSED");
    }

    /**
     * 7. Kiểm tra trạng thái bảng đầy TABLE_FULL
     */
    public static void testTableFullCondition() {
        System.out.print("[Test 7] Xử lý bảng đầy (TABLE_FULL)... ");
        int M = 8;
        LinearProbingTombstone table = new LinearProbingTombstone(M);
        for (long k = 1; k <= M; k++) {
            assertEquals(PutStatus.INSERTED, table.put(k, (int) k), "Chèn phần tử " + k);
        }
        assertEquals(M, table.size(), "Bảng đã đầy M phần tử");
        assertEquals(0, table.getEmptyCount(), "Không còn ô EMPTY");

        // Chèn thêm khóa thứ M+1 phải trả về TABLE_FULL
        PutStatus overflow = table.put(999L, 999);
        assertEquals(PutStatus.TABLE_FULL, overflow, "Phải trả về TABLE_FULL khi hết ô");

        // Cập nhật khóa hiện có vẫn phải thành công
        PutStatus updateStatus = table.put(1L, 1000);
        assertEquals(PutStatus.UPDATED, updateStatus, "Cập nhật khóa hiện có");
        System.out.println("PASSED");
    }

    /**
     * 8. Kiểm tra cơ chế đếm số bước dò (Probe Counting)
     */
    public static void testProbeCounting() {
        System.out.print("[Test 8] Đếm số bước dò (Probe Counting)... ");
        int M = 16;
        int p = Integer.numberOfTrailingZeros(M);
        LinearProbingTombstone table = new LinearProbingTombstone(M);
        table.enableProbeCounting(true);
        table.resetProbeCount();

        long k1 = 10L;
        table.put(k1, 100);
        long probesAfterPut = table.getProbeCount();
        assertTrue(probesAfterPut >= 1, "Phải có ít nhất 1 bước dò khi put");

        table.resetProbeCount();
        GetResult rHit = table.get(k1);
        assertTrue(rHit.isFound(), "Tìm thấy k1");
        long probesHit = table.getProbeCount();
        assertTrue(probesHit >= 1, "Bước dò khi hit >= 1");

        table.resetProbeCount();
        // Tìm kiếm khóa không có trong bảng (rơi vào ô EMPTY)
        table.get(999_999L);
        long probesMiss = table.getProbeCount();
        assertTrue(probesMiss >= 1, "Bước dò khi miss >= 1");
        System.out.println("PASSED");
    }

    /**
     * 9. Kiểm tra tạo Clean Table đối chứng cho RQ2
     */
    public static void testCleanTableCreation() {
        System.out.print("[Test 9] Tạo Bảng đối chứng Clean Table... ");
        int M = 64;
        RQ2Workload workload = new RQ2Workload(M, 0.5, 42L);
        LinearProbingTombstone table = new LinearProbingTombstone(M);

        workload.populateInitialKeys(table);
        assertEquals(32, table.size(), "Ban đầu có 32 phần tử");

        // Chạy 20 chu kỳ churn tạo tombstones
        workload.runChurnCycles(table, 20);
        assertTrue(table.getTombstoneCount() > 0, "Bảng sau churn phải có tombstones");
        table.validateInvariants();

        // Dựng bảng sạch đối chứng
        LinearProbingTombstone clean = workload.buildCleanTable(table);
        assertEquals(table.size(), clean.size(), "Cùng số lượng phần tử sống");
        assertEquals(0, clean.getTombstoneCount(), "Clean table không có tombstone");
        assertEquals(M - clean.size(), clean.getEmptyCount(), "Clean table có số ô EMPTY chuẩn");
        clean.validateInvariants();

        // Kiểm tra mọi phần tử sống trong bảng gốc đều tồn tại trong clean table với cùng giá trị
        for (int i = 0; i < M; i++) {
            if (table.getStateAt(i) == LinearProbingTombstone.OCCUPIED) {
                long k = table.getKeyAt(i);
                GetResult rSrc = table.get(k);
                GetResult rClean = clean.get(k);
                assertTrue(rClean.isFound(), "Khóa " + k + " phải có trong clean table");
                assertEquals(rSrc.getValue(), rClean.getValue(), "Giá trị khớp nhau");
            }
        }
        System.out.println("PASSED");
    }

    /**
     * 10. Kiểm thử Oracle toàn diện với java.util.HashMap (Stress Test 100,000 ops)
     */
    public static void testOracleWithHashMap(int operations, long seed) {
        System.out.print("[Test 10] Oracle Test đối chiếu với java.util.HashMap (" + operations + " ops)... ");
        int M = 1024; // Dung lượng đủ lớn cho stress test
        LinearProbingTombstone table = new LinearProbingTombstone(M, 0.25); // Bật cả rebuild
        HashMap<Long, Integer> oracle = new HashMap<>();
        SplittableRandom rng = new SplittableRandom(seed);

        for (int op = 0; op < operations; op++) {
            int action = rng.nextInt(100);
            long key = rng.nextLong(1, 400); // Không gian khóa nhỏ để tăng tần suất đụng độ và cập nhật/xóa

            if (action < 50) {
                // 50% thao tác PUT
                int val = rng.nextInt(10_000);
                if (oracle.size() < M - 10 || oracle.containsKey(key)) {
                    PutStatus status = table.put(key, val);
                    boolean existed = oracle.containsKey(key);
                    oracle.put(key, val);

                    if (existed) {
                        assertEquals(PutStatus.UPDATED, status, "Phải báo UPDATED khi key đã tồn tại");
                    } else {
                        assertEquals(PutStatus.INSERTED, status, "Phải báo INSERTED khi key mới");
                    }
                }
            } else if (action < 80) {
                // 30% thao tác GET
                GetResult res = table.get(key);
                if (oracle.containsKey(key)) {
                    assertTrue(res.isFound(), "Khóa " + key + " phải tìm thấy");
                    assertEquals((int) oracle.get(key), res.getValue(), "Giá trị khóa " + key);
                } else {
                    assertTrue(!res.isFound(), "Khóa " + key + " không được tìm thấy");
                }
            } else {
                // 20% thao tác REMOVE
                boolean removedTable = table.remove(key);
                boolean existedOracle = oracle.containsKey(key);
                oracle.remove(key);
                assertEquals(existedOracle, removedTable, "Kết quả remove phải khớp với oracle");
            }

            assertEquals(oracle.size(), table.size(), "Size phải luôn khớp với oracle tại bước " + op);
        }
        System.out.println("PASSED");
    }

    /**
     * 11. Kiểm tra Workload xóa-thêm O(1) và đo đạc theo từng khối (Block Profiling)
     */
    public static void testWorkloadBlockProfiling() {
        System.out.print("[Test 11] Workload O(1) và Block Profiling từng khối 10^4 thao tác... ");
        int M = 2048;
        RQ2Workload workload = new RQ2Workload(M, 0.5, 77L);
        LinearProbingTombstone table = new LinearProbingTombstone(M, 0.05);

        workload.populateInitialKeys(table);
        assertEquals(1024, workload.getAliveCount(), "Khởi tạo đúng N = 1024");
        assertEquals(1024, table.size(), "Bảng có đúng 1024 phần tử sống");

        // Chạy 5000 chu kỳ churn với khối 2000 thao tác (1000 chu kỳ/khối)
        java.util.List<RQ2Workload.BlockTiming> blocks =
                workload.runChurnCyclesWithBlockProfiling(table, 5000, 2000, null);

        assertTrue(blocks.size() >= 5, "Phải ghi nhận ít nhất 5 khối đo đạc");
        long reportedRebuilds = 0;
        for (RQ2Workload.BlockTiming b : blocks) {
            assertTrue(b.elapsedMs >= 0.0, "Thời gian khối phải không âm");
            assertTrue(b.tombstoneCount > 0, "Sau churn phải có tombstones");
            assertTrue(b.averageDIB >= 0.0, "DIB trung bình của khối phải được đo");
            assertTrue(b.maxDIB >= 0, "DIB lớn nhất của khối phải được đo");
            assertTrue(b.rebuildCount >= 0, "Số lần rebuild tích lũy của khối phải được đo");
            assertTrue(b.rebuildsInBlock >= 0, "Số rebuild trong khối phải được đo");
            reportedRebuilds += b.rebuildsInBlock;
        }
        assertTrue(table.getRebuildCount() > 0, "Workload phải ghi nhận được rebuild thực tế");
        assertEquals(table.getRebuildCount(), reportedRebuilds, "Tổng rebuild theo khối phải khớp bảng");
        RQ2Workload.BlockTiming lastBlock = blocks.get(blocks.size() - 1);
        assertTrue(Math.abs(lastBlock.averageDIB - table.computeActualAverageDIB()) < 1e-12,
                "DIB trung bình khối cuối phải khớp bảng");
        assertEquals(table.computeActualMaxDIB(), lastBlock.maxDIB, "DIB lớn nhất khối cuối phải khớp bảng");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * 12. Kiểm tra đo đạc tại mốc C và Bảng đối chứng RQ2 (Clean Table)
     */
    public static void testMilestoneCMeasurementAndComparisonTable() {
        System.out.print("[Test 12] Đo mốc C (10^4 hit, 10^4 miss) & Bảng đối chứng RQ2... ");
        int M = 1024;
        RQ2Workload workload = new RQ2Workload(M, 0.5, 999L);
        LinearProbingTombstone table = new LinearProbingTombstone(M);

        workload.populateInitialKeys(table);
        int milestoneC = 600;
        workload.runChurnCycles(table, milestoneC);

        // Đo tại mốc C với 1,000 queries hit và 1,000 queries miss (ở test này dùng 1,000 để chạy nhanh)
        RQ2Workload.MilestoneCMeasurement mHitMiss =
                workload.measureAtMilestoneC(table, milestoneC, 1_000, 42L, "base");

        assertEquals(1_000, mHitMiss.hitCount, "Số truy vấn hit đúng");
        assertEquals(1_000, mHitMiss.missCount, "Số truy vấn miss đúng");
        assertTrue(mHitMiss.hitAvgProbes >= 1.0, "Số lần dò hit >= 1.0");
        assertTrue(mHitMiss.missAvgProbes >= 1.0, "Số lần dò miss >= 1.0");
        assertTrue(mHitMiss.tombstoneCount > 0, "Số ô tombstone > 0");
        assertTrue(mHitMiss.averageDIB >= 0.0, "Phải đo DIB trung bình ở mốc C");
        assertTrue(mHitMiss.maxDIB >= 0, "Phải đo DIB lớn nhất ở mốc C");
        assertEquals(table.getRebuildCount(), mHitMiss.rebuildCount, "Số lần rebuild ở mốc C phải chính xác");

        // So sánh đối chứng với Clean Table
        RQ2Workload.RQ2ComparisonResult comp =
                workload.compareWithCleanTable(table, milestoneC, 1_000, 42L);

        assertEquals(0, comp.cleanTableResult.tombstoneCount, "Clean Table có 0 tombstone");
        assertEquals(table.size(), comp.cleanTableResult.aliveCount, "Clean Table có cùng số khóa sống");
        assertTrue(comp.cleanTableResult.averageDIB >= 0.0, "Clean Table phải có thống kê DIB");
        assertEquals(0, comp.cleanTableResult.rebuildCount, "Clean Table không có rebuild tự động");
        // Bảng tích tụ Tombstone phải có số lần dò khi miss cao hơn hoặc bằng Clean Table
        assertTrue(comp.tombTableResult.missAvgProbes >= comp.cleanTableResult.missAvgProbes,
                "Bảng tích tụ tombstone phải có số lần dò miss >= Clean Table");

        String markdown = comp.toMarkdownTable();
        assertTrue(markdown.contains("BẢNG ĐỐI CHỨNG RQ2"), "Markdown phải chứa tiêu đề bảng đối chứng");
        assertTrue(markdown.contains("DIB trung bình"), "Markdown phải có DIB trung bình");
        assertTrue(markdown.contains("DIB lớn nhất"), "Markdown phải có DIB lớn nhất");
        assertTrue(markdown.contains("Số lần rebuild tích lũy"), "Markdown phải có thống kê rebuild");
        assertTrue(markdown.contains("KHỚP 100%"), "Checksum phải khớp 100%");
        System.out.println("PASSED");
    }
}
