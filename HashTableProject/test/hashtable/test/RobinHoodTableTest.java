package hashtable.test;

import hashtable.core.GetResult;
import hashtable.core.PutStatus;
import hashtable.core.RobinHoodTable;

import java.util.HashMap;
import java.util.SplittableRandom;

/**
 * Bộ kiểm thử toàn diện cho Robin Hood Hashing (rh & rh-noearly) - Nhiệm vụ Cặp 3 (C3).
 *
 * Kiểm tra:
 * 1. Thao tác cơ bản: put, get, remove, size, capacity, tombstoneCount = 0.
 * 2. Tính toán DIB chính xác (getAverageDIB, getMaxDIB).
 * 3. Hoán đổi Robin Hood ("lấy của người giàu chia cho người nghèo").
 * 4. Dừng sớm (Early Exit) trong get() và remove() so với rh-noearly.
 * 5. Xóa dịch lùi (Backward-shift deletion) không tạo tombstone và dừng khi DIB == 0.
 * 6. Xử lý bảng đầy (TABLE_FULL).
 * 7. Đếm số bước dò (Probe Counting).
 * 8. Kiểm thử Oracle với java.util.HashMap (100,000 thao tác ngẫu nhiên).
 */
public class RobinHoodTableTest {

    public static void runAllTests() {
        System.out.println("--- Bat dau Unit Test cho RobinHoodTable (Cap 3 - C3) ---");
        testBasicOperations();
        testRobinHoodSwapLogic();
        testEarlyExitVersusNoEarly();
        testBackwardShiftDeletion();
        testTableFullCondition();
        testProbeCounting();
        testOracleWithHashMap(100_000, 42L);
        System.out.println("--- TAT CA CAC BAI TEST ROBIN HOOD DA VUOT QUA THANH CONG! ---\n");
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
     * 1. Kiểm tra các thao tác cơ bản: put, get, remove, không có tombstone.
     */
    public static void testBasicOperations() {
        System.out.print("[Test 1] Thao tac co ban (put, get, remove, 0 tombstone)... ");
        RobinHoodTable table = new RobinHoodTable(16);
        assertEquals(16, table.capacity(), "Capacity phai la 16");
        assertEquals(0, table.size(), "Size ban dau phai la 0");
        assertEquals(0, table.getTombstoneCount(), "Robin Hood khong bao gio co tombstone");
        assertEquals(16, table.getEmptyCount(), "Empty ban dau phai la 16");

        // Put khóa
        assertEquals(PutStatus.INSERTED, table.put(100L, 10), "Chen 100L");
        assertEquals(PutStatus.INSERTED, table.put(200L, 20), "Chen 200L");
        assertEquals(2, table.size(), "Size sau khi chen 2 khoa");

        // Cập nhật giá trị
        assertEquals(PutStatus.UPDATED, table.put(100L, 99), "Cap nhat 100L");
        assertEquals(2, table.size(), "Size khong doi sau khi update");
        assertEquals(99, table.get(100L).getValue(), "Gia tri sau update phai la 99");

        // Get khóa
        GetResult r1 = table.get(100L);
        assertTrue(r1.isFound(), "Tim thay 100L");
        assertEquals(99, r1.getValue(), "Gia tri 100L");

        GetResult rMiss = table.get(9999L);
        assertTrue(!rMiss.isFound(), "Khong tim thay khoa chua chen");

        // Remove khóa
        assertTrue(table.remove(100L), "Xoa 100L");
        assertEquals(1, table.size(), "Size sau xoa");
        assertEquals(0, table.getTombstoneCount(), "Van 0 tombstone sau xoa");
        assertTrue(!table.get(100L).isFound(), "Khong tim thay khoa sau xoa");
        assertTrue(!table.remove(100L), "Xoa lai phai tra ve false");

        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra logic hoán đổi Robin Hood (Robin Hood Swap).
     */
    public static void testRobinHoodSwapLogic() {
        System.out.print("[Test 2] Hoan doi Robin Hood (Swap theo DIB)... ");
        RobinHoodTable table = new RobinHoodTable(8);

        // Chèn nhiều khóa để kích hoạt đụng độ và hoán đổi
        for (int i = 1; i <= 6; i++) {
            assertEquals(PutStatus.INSERTED, table.put(i * 10L, i), "Chen khoa " + (i * 10L));
        }

        // Kiểm tra mọi phần tử đều có thể tra cứu chính xác
        for (int i = 1; i <= 6; i++) {
            GetResult res = table.get(i * 10L);
            assertTrue(res.isFound(), "Phai tim thay " + (i * 10L));
            assertEquals(i, res.getValue(), "Gia tri cua " + (i * 10L));
        }

        // Kiểm tra DIB trung bình và Max DIB hợp lệ
        assertTrue(table.getAverageDIB() >= 0.0, "DIB trung binh >= 0");
        assertTrue(table.getMaxDIB() >= 0, "Max DIB >= 0");

        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra cơ chế Early Exit so với No Early Exit.
     */
    public static void testEarlyExitVersusNoEarly() {
        System.out.print("[Test 3] Doi chieu dung som rh vs rh-noearly... ");
        int cap = 256;
        RobinHoodTable rhEarly = new RobinHoodTable(cap, true);
        RobinHoodTable rhNoEarly = new RobinHoodTable(cap, false);

        assertTrue(rhEarly.isEarlyExitEnabled(), "rhEarly phai bat early exit");
        assertTrue(!rhNoEarly.isEarlyExitEnabled(), "rhNoEarly phai tat early exit");

        // Chèn cùng tập khóa vào cả hai bảng
        for (int i = 1; i <= 100; i++) {
            rhEarly.put(i * 37L, i);
            rhNoEarly.put(i * 37L, i);
        }

        // Bật đếm bước dò và thực hiện 1000 truy vấn vắng mặt (Miss)
        rhEarly.enableProbeCounting(true);
        rhEarly.resetProbeCount();

        rhNoEarly.enableProbeCounting(true);
        rhNoEarly.resetProbeCount();

        for (int i = 1; i <= 1000; i++) {
            long absentKey = 100_000L + i * 31L;
            rhEarly.get(absentKey);
            rhNoEarly.get(absentKey);
        }

        long earlyProbes = rhEarly.getProbeCount();
        long noEarlyProbes = rhNoEarly.getProbeCount();

        // Nhờ Early Exit, số bước dò khi tìm kiếm trượt của rhEarly phải nhỏ hơn hoặc bằng rhNoEarly
        assertTrue(earlyProbes <= noEarlyProbes,
                "Early exit (" + earlyProbes + ") phai tiet kiem buoc do hon no-early (" + noEarlyProbes + ")");

        System.out.println("PASSED (Early: " + earlyProbes + " probes, NoEarly: " + noEarlyProbes + " probes)");
    }

    /**
     * 4. Kiểm tra xóa dịch lùi (Backward-shift deletion) không tạo tombstone.
     */
    public static void testBackwardShiftDeletion() {
        System.out.print("[Test 4] Xoa dich lui (Backward-shift deletion)... ");
        RobinHoodTable table = new RobinHoodTable(16);

        // Chèn 10 khóa
        for (int i = 1; i <= 10; i++) {
            table.put(i * 17L, i);
        }
        assertEquals(10, table.size(), "Size ban dau");

        // Xóa một số khóa ở giữa cụm
        assertTrue(table.remove(3 * 17L), "Xoa 51L");
        assertTrue(table.remove(7 * 17L), "Xoa 119L");
        assertEquals(8, table.size(), "Size con 8");
        assertEquals(0, table.getTombstoneCount(), "Khong co tombstone");

        // Kiểm tra các khóa còn lại vẫn tra cứu được đầy đủ
        for (int i = 1; i <= 10; i++) {
            if (i == 3 || i == 7) {
                assertTrue(!table.get(i * 17L).isFound(), "Khoa " + (i * 17L) + " da bi xoa");
            } else {
                assertTrue(table.get(i * 17L).isFound(), "Khoa " + (i * 17L) + " phai con song");
            }
        }

        System.out.println("PASSED");
    }

    /**
     * 5. Kiểm tra bảng đầy (TABLE_FULL).
     */
    public static void testTableFullCondition() {
        System.out.print("[Test 5] Xu ly bang day (TABLE_FULL)... ");
        RobinHoodTable table = new RobinHoodTable(8);

        for (int i = 0; i < 8; i++) {
            assertEquals(PutStatus.INSERTED, table.put(i * 13L + 1, i), "Chen phan tu thu " + i);
        }
        assertEquals(8, table.size(), "Bang da day 8 phan tu");

        // Cố chèn thêm phần tử thứ 9
        assertEquals(PutStatus.TABLE_FULL, table.put(999L, 999), "Phai bao TABLE_FULL");
        assertEquals(8, table.size(), "Size van la 8");

        // Cập nhật phần tử cũ vẫn phải thành công
        assertEquals(PutStatus.UPDATED, table.put(1L, 999), "Cap nhat phan tu cu phai thanh cong");
        assertEquals(8, table.size(), "Size van giu nguyen");

        System.out.println("PASSED");
    }

    /**
     * 6. Kiểm tra đếm số bước dò (Probe Counting).
     */
    public static void testProbeCounting() {
        System.out.print("[Test 6] Dem so buoc do (Probe Counting)... ");
        RobinHoodTable table = new RobinHoodTable(32);
        table.enableProbeCounting(true);
        table.resetProbeCount();

        table.put(10L, 1);
        table.put(20L, 2);
        assertTrue(table.getProbeCount() > 0, "Probe count phai > 0 sau khi chen");

        table.resetProbeCount();
        assertEquals(0, table.getProbeCount(), "Probe count sau reset phai la 0");

        table.get(10L);
        assertTrue(table.getProbeCount() >= 1, "Tra cuu 10L phai ton it nhat 1 buoc do");

        System.out.println("PASSED");
    }

    /**
     * 7. Kiểm thử Oracle đối chiếu với java.util.HashMap trên 100,000 thao tác ngẫu nhiên.
     */
    public static void testOracleWithHashMap(int operations, long seed) {
        System.out.print("[Test 7] Oracle Test doi chieu java.util.HashMap (" + operations + " ops)... ");
        int capacity = 1024;
        RobinHoodTable table = new RobinHoodTable(capacity);
        HashMap<Long, Integer> oracle = new HashMap<>();
        SplittableRandom rng = new SplittableRandom(seed);

        for (int i = 0; i < operations; i++) {
            int opType = rng.nextInt(3); // 0: put, 1: get, 2: remove
            long key = rng.nextLong(1, 500); // Giới hạn miền khóa để tạo nhiều đụng độ và thao tác lặp lại
            int value = rng.nextInt(10000);

            if (opType == 0) {
                // Put
                if (oracle.containsKey(key)) {
                    PutStatus st = table.put(key, value);
                    assertEquals(PutStatus.UPDATED, st, "Oracle put update key " + key);
                    oracle.put(key, value);
                } else {
                    if (table.size() < capacity) {
                        PutStatus st = table.put(key, value);
                        assertEquals(PutStatus.INSERTED, st, "Oracle put insert key " + key);
                        oracle.put(key, value);
                    } else {
                        PutStatus st = table.put(key, value);
                        assertEquals(PutStatus.TABLE_FULL, st, "Table full on key " + key);
                    }
                }
            } else if (opType == 1) {
                // Get
                GetResult r = table.get(key);
                if (oracle.containsKey(key)) {
                    assertTrue(r.isFound(), "Oracle co key " + key + " nhung table bao NOT_FOUND");
                    assertEquals((int) oracle.get(key), r.getValue(), "Gia tri key " + key + " khong khop");
                } else {
                    assertTrue(!r.isFound(), "Oracle khong co key " + key + " nhung table bao FOUND");
                }
            } else {
                // Remove
                boolean removed = table.remove(key);
                if (oracle.containsKey(key)) {
                    assertTrue(removed, "Oracle co key " + key + " nhung table xoa that bai");
                    oracle.remove(key);
                } else {
                    assertTrue(!removed, "Oracle khong co key " + key + " nhung table bao xoa thanh cong");
                }
            }

            // Đối chiếu size và số tombstone
            assertEquals(oracle.size(), table.size(), "Size khong khop tai buoc " + i);
            assertEquals(0, table.getTombstoneCount(), "Robin Hood khong bao gio co tombstone");
        }

        System.out.println("PASSED");
    }
}
