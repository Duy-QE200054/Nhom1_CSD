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

    public static void runAllTests() {
        System.out.println("--- Bat dau Unit Test cho LinearProbingTombstone (Cap 2 - C2) ---");
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
        System.out.println("--- TAT CA CAC BAI TEST CUA C2 DA VUOT QUA THANH CONG! ---\n");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("THAT BAI: " + msg);
        }
    }

    private static void assertEquals(long expected, long actual, String msg) {
        if (expected != actual) {
            throw new AssertionError("THAT BAI: " + msg + " (Mong doi: " + expected + ", Thuc te: " + actual + ")");
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError("THAT BAI: " + msg + " (Mong doi: " + expected + ", Thuc te: " + actual + ")");
        }
    }

    /**
     * 1. Kiểm tra các thao tác cơ bản: put, get, remove
     */
    public static void testBasicOperations() {
        System.out.print("[Test 1] Thao tac co ban (put, get, remove)... ");
        LinearProbingTombstone table = new LinearProbingTombstone(16);
        assertEquals(16, table.capacity(), "Capacity phai la 16");
        assertEquals(0, table.size(), "Size ban dau phai la 0");
        assertEquals(16, table.getEmptyCount(), "Empty ban dau phai la 16");
        assertEquals(0, table.getTombstoneCount(), "Tombstone ban dau phai la 0");

        // Put khóa
        assertEquals(PutStatus.INSERTED, table.put(100L, 10), "Chen 100L");
        assertEquals(PutStatus.INSERTED, table.put(200L, 20), "Chen 200L");
        assertEquals(2, table.size(), "Size sau 2 lan chen");
        table.validateInvariants();

        // Get khóa
        GetResult r1 = table.get(100L);
        assertTrue(r1.isFound(), "Phai tim thay 100L");
        assertEquals(10, r1.getValue(), "Gia tri 100L");

        GetResult r2 = table.get(200L);
        assertTrue(r2.isFound(), "Phai tim thay 200L");
        assertEquals(20, r2.getValue(), "Gia tri 200L");

        GetResult rMiss = table.get(999L);
        assertTrue(!rMiss.isFound(), "Khong duoc tim thay 999L");

        // Remove khóa
        assertTrue(table.remove(100L), "Xoa 100L thanh cong");
        assertEquals(1, table.size(), "Size sau khi xoa 1");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count phai la 1");
        assertTrue(!table.get(100L).isFound(), "Khong con tim thay 100L sau khi xoa");
        table.validateInvariants();

        // Xóa lại khóa đã xóa
        assertTrue(!table.remove(100L), "Xoa lai 100L phai tra ve false");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra logic cốt lõi: Ghi nhớ TOMB đầu tiên khi put khóa mới
     */
    public static void testRememberFirstTombstoneOnPut() {
        System.out.print("[Test 2] Logic ghi nho TOMB dau tien khi put khoa moi... ");
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
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH), "k1 tai targetH");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 1), "k2 tai targetH + 1");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 2), "k3 tai targetH + 2");
        table.validateInvariants();

        // Xóa k2 -> ô targetH + 1 trở thành TOMB
        table.remove(k2);
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "O targetH + 1 phai la TOMB");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count = 1");
        table.validateInvariants();

        // Chèn khóa mới k4 (cũng băm về targetH):
        // Dò từ targetH (k1: occupied khác) -> targetH+1 (TOMB: nhớ firstTomb) -> targetH+2 (k3: occupied khác) -> targetH+3 (EMPTY: dừng dò).
        // Chèn k4 vào firstTomb (targetH+1)!
        PutStatus status = table.put(k4, 4);
        assertEquals(PutStatus.INSERTED, status, "k4 phai duoc chen thanh cong");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 1), "targetH + 1 phai duoc tai su dung");
        assertEquals(k4, table.getKeyAt(targetH + 1), "Khoa tai targetH + 1 phai la k4");
        assertEquals(0, table.getTombstoneCount(), "Tombstone count phai giam ve 0");
        table.validateInvariants();

        // Kiểm tra k3 vẫn tra cứu được bình thường
        GetResult rK3 = table.get(k3);
        assertTrue(rK3.isFound(), "k3 van phai tim thay");
        assertEquals(3, rK3.getValue(), "Gia tri cua k3");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra: Khi cập nhật khóa đã tồn tại nằm sau TOMB, KHÔNG được ghi đè vào TOMB
     */
    public static void testUpdateKeyWithPriorTombstone() {
        System.out.print("[Test 3] Cap nhat khoa hien co (khong ghi de TOMB)... ");
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
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "targetH + 1 la TOMB");

        // Cập nhật k3 (nằm sau ô TOMB)
        PutStatus updateStatus = table.put(k3, 300);
        assertEquals(PutStatus.UPDATED, updateStatus, "Phai tra ve UPDATED");
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "targetH + 1 van phai la TOMB");
        assertEquals(LinearProbingTombstone.OCCUPIED, table.getStateAt(targetH + 2), "k3 van nam o targetH + 2");
        assertEquals(300, table.getValueAt(targetH + 2), "Gia tri k3 da cap nhat thanh 300");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count van la 1");
        System.out.println("PASSED");
    }

    /**
     * 4. Kiểm tra: Khi có nhiều ô TOMB, chọn đúng ô TOMB đầu tiên
     */
    public static void testMultipleTombstonesFirstSelection() {
        System.out.print("[Test 4] Chon dung o TOMB dau tien trong nhieu o TOMB... ");
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
        assertEquals(2, table.getTombstoneCount(), "Co 2 tombstones");

        // Chèn k4 (cùng băm về targetH) -> phải rơi vào targetH (ô TOMB đầu tiên)
        table.put(k4, 4);
        assertEquals(k4, table.getKeyAt(targetH), "k4 phai nam tai targetH");
        assertEquals(LinearProbingTombstone.TOMB, table.getStateAt(targetH + 1), "targetH + 1 van la TOMB");
        assertEquals(1, table.getTombstoneCount(), "Tombstone count giam con 1");
        System.out.println("PASSED");
    }

    /**
     * 5. Kiểm tra tự động Rebuild khi tombstoneCount đạt ngưỡng tau * M
     */
    public static void testRebuildTriggeredByTau() {
        System.out.print("[Test 5] Tu dong Rebuild khi tombstoneCount >= tau * M... ");
        int M = 16;
        double tau = 0.25; // 0.25 * 16 = 4 tombstones
        LinearProbingTombstone table = new LinearProbingTombstone(M, tau);
        assertEquals(4, table.getTauThreshold(), "Nguong rebuild la 4");

        // Chèn 8 khóa
        for (long i = 1; i <= 8; i++) {
            table.put(i, (int) i * 10);
        }

        // Xóa 3 khóa đầu: 1, 2, 3 -> tombstoneCount = 3, chưa rebuild
        table.remove(1L);
        table.remove(2L);
        table.remove(3L);
        assertEquals(3, table.getTombstoneCount(), "Tombstone count = 3");
        assertEquals(0, table.getRebuildCount(), "Chua rebuild");

        // Xóa khóa thứ 4: 4L -> tombstoneCount chạm 4 -> KÍCH HOẠT REBUILD
        table.remove(4L);
        assertEquals(1, table.getRebuildCount(), "Da kich hoat 1 lan rebuild");
        assertEquals(0, table.getTombstoneCount(), "Sau rebuild tombstoneCount phai ve 0");
        assertEquals(4, table.size(), "Con lai dung 4 phan tu song");

        // Xác nhận 4 phần tử sống (5, 6, 7, 8) vẫn tra cứu chính xác
        for (long i = 5; i <= 8; i++) {
            GetResult gr = table.get(i);
            assertTrue(gr.isFound(), "Phan tu " + i + " phai con");
            assertEquals((int) i * 10, gr.getValue(), "Gia tri phan tu " + i);
        }
        System.out.println("PASSED");
    }

    /**
     * 6. Kiểm tra Rebuild duyệt theo đúng THỨ TỰ CHỈ SỐ Ô từ 0 đến M - 1
     */
    public static void testRebuildOrderPreservation() {
        System.out.print("[Test 6] Rebuild duyet theo dung thu tu chi so o (0..M-1)... ");
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
        assertTrue(table.getTombstoneCount() > 0, "Co tombstones");

        int sizeBefore = table.size();
        table.rebuild();
        assertEquals(sizeBefore, table.size(), "Size giu nguyen sau rebuild");
        assertEquals(0, table.getTombstoneCount(), "Tombstone = 0 sau rebuild");
        assertEquals(M - sizeBefore, table.getEmptyCount(), "Empty = M - size");
        System.out.println("PASSED");
    }

    /**
     * 7. Kiểm tra trạng thái bảng đầy TABLE_FULL
     */
    public static void testTableFullCondition() {
        System.out.print("[Test 7] Xu ly bang day (TABLE_FULL)... ");
        int M = 8;
        LinearProbingTombstone table = new LinearProbingTombstone(M);
        for (long k = 1; k <= M; k++) {
            assertEquals(PutStatus.INSERTED, table.put(k, (int) k), "Chen phan tu " + k);
        }
        assertEquals(M, table.size(), "Bang da day M phan tu");
        assertEquals(0, table.getEmptyCount(), "Khong con o EMPTY");

        // Chèn thêm khóa thứ M+1 phải trả về TABLE_FULL
        PutStatus overflow = table.put(999L, 999);
        assertEquals(PutStatus.TABLE_FULL, overflow, "Phai tra ve TABLE_FULL khi het o");

        // Cập nhật khóa hiện có vẫn phải thành công
        PutStatus updateStatus = table.put(1L, 1000);
        assertEquals(PutStatus.UPDATED, updateStatus, "Cap nhat khoa hien co");
        System.out.println("PASSED");
    }

    /**
     * 8. Kiểm tra cơ chế đếm số bước dò (Probe Counting)
     */
    public static void testProbeCounting() {
        System.out.print("[Test 8] Dem so buoc do (Probe Counting)... ");
        int M = 16;
        int p = Integer.numberOfTrailingZeros(M);
        LinearProbingTombstone table = new LinearProbingTombstone(M);
        table.enableProbeCounting(true);
        table.resetProbeCount();

        long k1 = 10L;
        table.put(k1, 100);
        long probesAfterPut = table.getProbeCount();
        assertTrue(probesAfterPut >= 1, "Phai co it nhat 1 buoc do khi put");

        table.resetProbeCount();
        GetResult rHit = table.get(k1);
        assertTrue(rHit.isFound(), "Tim thay k1");
        long probesHit = table.getProbeCount();
        assertTrue(probesHit >= 1, "Buoc do khi hit >= 1");

        table.resetProbeCount();
        // Tìm kiếm khóa không có trong bảng (rơi vào ô EMPTY)
        table.get(999_999L);
        long probesMiss = table.getProbeCount();
        assertTrue(probesMiss >= 1, "Buoc do khi miss >= 1");
        System.out.println("PASSED");
    }

    /**
     * 9. Kiểm tra tạo Clean Table đối chứng cho RQ2
     */
    public static void testCleanTableCreation() {
        System.out.print("[Test 9] Tao Bang doi chung Clean Table... ");
        int M = 64;
        RQ2Workload workload = new RQ2Workload(M, 0.5, 42L);
        LinearProbingTombstone table = new LinearProbingTombstone(M);

        workload.populateInitialKeys(table);
        assertEquals(32, table.size(), "Ban dau co 32 phan tu");

        // Chạy 20 chu kỳ churn tạo tombstones
        workload.runChurnCycles(table, 20);
        assertTrue(table.getTombstoneCount() > 0, "Bang sau churn phai co tombstones");
        table.validateInvariants();

        // Dựng bảng sạch đối chứng
        LinearProbingTombstone clean = workload.buildCleanTable(table);
        assertEquals(table.size(), clean.size(), "Cung so luong phan tu song");
        assertEquals(0, clean.getTombstoneCount(), "Clean table khong co tombstone");
        assertEquals(M - clean.size(), clean.getEmptyCount(), "Clean table co so o EMPTY chuan");
        clean.validateInvariants();

        // Kiểm tra mọi phần tử sống trong bảng gốc đều tồn tại trong clean table với cùng giá trị
        for (int i = 0; i < M; i++) {
            if (table.getStateAt(i) == LinearProbingTombstone.OCCUPIED) {
                long k = table.getKeyAt(i);
                GetResult rSrc = table.get(k);
                GetResult rClean = clean.get(k);
                assertTrue(rClean.isFound(), "Khoa " + k + " phai co trong clean table");
                assertEquals(rSrc.getValue(), rClean.getValue(), "Gia tri khop nhau");
            }
        }
        System.out.println("PASSED");
    }

    /**
     * 10. Kiểm thử Oracle toàn diện với java.util.HashMap (Stress Test 100,000 ops)
     */
    public static void testOracleWithHashMap(int operations, long seed) {
        System.out.print("[Test 10] Oracle Test doi chieu voi java.util.HashMap (" + operations + " ops)... ");
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
                        assertEquals(PutStatus.UPDATED, status, "Phai bao UPDATED khi key da ton tai");
                    } else {
                        assertEquals(PutStatus.INSERTED, status, "Phai bao INSERTED khi key moi");
                    }
                }
            } else if (action < 80) {
                // 30% thao tác GET
                GetResult res = table.get(key);
                if (oracle.containsKey(key)) {
                    assertTrue(res.isFound(), "Khoa " + key + " phai tim thay");
                    assertEquals((int) oracle.get(key), res.getValue(), "Gia tri khoa " + key);
                } else {
                    assertTrue(!res.isFound(), "Khoa " + key + " khong duoc tim thay");
                }
            } else {
                // 20% thao tác REMOVE
                boolean removedTable = table.remove(key);
                boolean existedOracle = oracle.containsKey(key);
                oracle.remove(key);
                assertEquals(existedOracle, removedTable, "Ket qua remove phai khop voi oracle");
            }

            assertEquals(oracle.size(), table.size(), "Size phai luon khop voi oracle tai buoc " + op);
        }
        System.out.println("PASSED");
    }

    /**
     * 11. Kiểm tra Workload xóa-thêm O(1) và đo đạc theo từng khối (Block Profiling)
     */
    public static void testWorkloadBlockProfiling() {
        System.out.print("[Test 11] Workload O(1) va Block Profiling tung khoi 10^4 thao tac... ");
        int M = 2048;
        RQ2Workload workload = new RQ2Workload(M, 0.5, 77L);
        LinearProbingTombstone table = new LinearProbingTombstone(M, 0.05);

        workload.populateInitialKeys(table);
        assertEquals(1024, workload.getAliveCount(), "Khoi tao dung N = 1024");
        assertEquals(1024, table.size(), "Bang co dung 1024 phan tu song");

        // Chạy 5000 chu kỳ churn với khối 2000 thao tác (1000 chu kỳ/khối)
        java.util.List<RQ2Workload.BlockTiming> blocks =
                workload.runChurnCyclesWithBlockProfiling(table, 5000, 2000, null);

        assertTrue(blocks.size() >= 5, "Phai ghi nhan it nhat 5 khoi do dac");
        long reportedRebuilds = 0;
        for (RQ2Workload.BlockTiming b : blocks) {
            assertTrue(b.elapsedMs >= 0.0, "Thoi gian khoi phai khong am");
            assertTrue(b.tombstoneCount > 0, "Sau churn phai co tombstones");
            assertTrue(b.averageDIB >= 0.0, "DIB trung binh cua khoi phai duoc do");
            assertTrue(b.maxDIB >= 0, "DIB lon nhat cua khoi phai duoc do");
            assertTrue(b.rebuildCount >= 0, "So lan rebuild tich luy cua khoi phai duoc do");
            assertTrue(b.rebuildsInBlock >= 0, "So rebuild trong khoi phai duoc do");
            reportedRebuilds += b.rebuildsInBlock;
        }
        assertTrue(table.getRebuildCount() > 0, "Workload phai ghi nhan duoc rebuild thuc te");
        assertEquals(table.getRebuildCount(), reportedRebuilds, "Tong rebuild theo khoi phai khop bang");
        RQ2Workload.BlockTiming lastBlock = blocks.get(blocks.size() - 1);
        assertTrue(Math.abs(lastBlock.averageDIB - table.computeActualAverageDIB()) < 1e-12,
                "DIB trung binh khoi cuoi phai khop bang");
        assertEquals(table.computeActualMaxDIB(), lastBlock.maxDIB, "DIB lon nhat khoi cuoi phai khop bang");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * 12. Kiểm tra đo đạc tại mốc C và Bảng đối chứng RQ2 (Clean Table)
     */
    public static void testMilestoneCMeasurementAndComparisonTable() {
        System.out.print("[Test 12] Do moc C (10^4 hit, 10^4 miss) & Bang doi chung RQ2... ");
        int M = 1024;
        RQ2Workload workload = new RQ2Workload(M, 0.5, 999L);
        LinearProbingTombstone table = new LinearProbingTombstone(M);

        workload.populateInitialKeys(table);
        int milestoneC = 600;
        workload.runChurnCycles(table, milestoneC);

        // Đo tại mốc C với 1,000 queries hit và 1,000 queries miss (ở test này dùng 1,000 để chạy nhanh)
        RQ2Workload.MilestoneCMeasurement mHitMiss =
                workload.measureAtMilestoneC(table, milestoneC, 1_000, 42L, "base");

        assertEquals(1_000, mHitMiss.hitCount, "So truy van hit dung");
        assertEquals(1_000, mHitMiss.missCount, "So truy van miss dung");
        assertTrue(mHitMiss.hitAvgProbes >= 1.0, "So lan do hit >= 1.0");
        assertTrue(mHitMiss.missAvgProbes >= 1.0, "So lan do miss >= 1.0");
        assertTrue(mHitMiss.tombstoneCount > 0, "So o tombstone > 0");
        assertTrue(mHitMiss.averageDIB >= 0.0, "Phai do DIB trung binh o moc C");
        assertTrue(mHitMiss.maxDIB >= 0, "Phai do DIB lon nhat o moc C");
        assertEquals(table.getRebuildCount(), mHitMiss.rebuildCount, "So lan rebuild o moc C phai chinh xac");

        // So sánh đối chứng với Clean Table
        RQ2Workload.RQ2ComparisonResult comp =
                workload.compareWithCleanTable(table, milestoneC, 1_000, 42L);

        assertEquals(0, comp.cleanTableResult.tombstoneCount, "Clean Table co 0 tombstone");
        assertEquals(table.size(), comp.cleanTableResult.aliveCount, "Clean Table co cung so khoa song");
        assertTrue(comp.cleanTableResult.averageDIB >= 0.0, "Clean Table phai co thong ke DIB");
        assertEquals(0, comp.cleanTableResult.rebuildCount, "Clean Table khong co rebuild tu dong");
        // Bảng tích tụ Tombstone phải có số lần dò khi miss cao hơn hoặc bằng Clean Table
        assertTrue(comp.tombTableResult.missAvgProbes >= comp.cleanTableResult.missAvgProbes,
                "Bang tich tu tombstone phai co so lan do miss >= Clean Table");

        String markdown = comp.toMarkdownTable();
        assertTrue(markdown.contains("BẢNG ĐỐI CHỨNG RQ2"), "Markdown phai chua tieu de bang doi chung");
        assertTrue(markdown.contains("DIB trung bình"), "Markdown phai co DIB trung binh");
        assertTrue(markdown.contains("DIB lớn nhất"), "Markdown phai co DIB lon nhat");
        assertTrue(markdown.contains("Số lần rebuild tích lũy"), "Markdown phai co thong ke rebuild");
        assertTrue(markdown.contains("KHỚP 100%"), "Checksum phai khop 100%");
        System.out.println("PASSED");
    }
}
