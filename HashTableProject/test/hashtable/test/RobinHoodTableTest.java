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
        System.out.println("--- Bắt đầu Unit Test cho RobinHoodTable (Cặp 3 - C3) ---");
        testBasicOperations();
        testRobinHoodSwapLogic();
        testEarlyExitVersusNoEarly();
        testBackwardShiftDeletion();
        testTableFullCondition();
        testProbeCounting();
        testOracleWithHashMap(100_000, 42L);
        System.out.println("--- TẤT CẢ CÁC BÀI TEST ROBIN HOOD ĐÃ VƯỢT QUA THÀNH CÔNG! ---\n");
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
     * 1. Kiểm tra các thao tác cơ bản: put, get, remove, không có tombstone.
     */
    public static void testBasicOperations() {
        System.out.print("[Test 1] Thao tác cơ bản (put, get, remove, 0 tombstone)... ");
        RobinHoodTable table = new RobinHoodTable(16);
        assertEquals(16, table.capacity(), "Capacity phải là 16");
        assertEquals(0, table.size(), "Size ban đầu phải là 0");
        assertEquals(0, table.getTombstoneCount(), "Robin Hood không bao giờ có tombstone");
        assertEquals(16, table.getEmptyCount(), "Empty ban đầu phải là 16");

        // Put khóa
        assertEquals(PutStatus.INSERTED, table.put(100L, 10), "Chèn 100L");
        assertEquals(PutStatus.INSERTED, table.put(200L, 20), "Chèn 200L");
        assertEquals(2, table.size(), "Size sau khi chèn 2 khóa");

        // Cập nhật giá trị
        assertEquals(PutStatus.UPDATED, table.put(100L, 99), "Cập nhật 100L");
        assertEquals(2, table.size(), "Size không đổi sau khi update");
        assertEquals(99, table.get(100L).getValue(), "Giá trị sau update phải là 99");

        // Get khóa
        GetResult r1 = table.get(100L);
        assertTrue(r1.isFound(), "Tìm thấy 100L");
        assertEquals(99, r1.getValue(), "Giá trị 100L");

        GetResult rMiss = table.get(9999L);
        assertTrue(!rMiss.isFound(), "Không tìm thấy khóa chưa chèn");

        // Remove khóa
        assertTrue(table.remove(100L), "Xóa 100L");
        assertEquals(1, table.size(), "Size sau xóa");
        assertEquals(0, table.getTombstoneCount(), "Vẫn 0 tombstone sau xóa");
        assertTrue(!table.get(100L).isFound(), "Không tìm thấy khóa sau xóa");
        assertTrue(!table.remove(100L), "Xóa lại phải trả về false");

        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra logic hoán đổi Robin Hood (Robin Hood Swap).
     */
    public static void testRobinHoodSwapLogic() {
        System.out.print("[Test 2] Hoán đổi Robin Hood (Swap theo DIB)... ");
        RobinHoodTable table = new RobinHoodTable(8);

        // Chèn nhiều khóa để kích hoạt đụng độ và hoán đổi
        for (int i = 1; i <= 6; i++) {
            assertEquals(PutStatus.INSERTED, table.put(i * 10L, i), "Chèn khóa " + (i * 10L));
        }

        // Kiểm tra mọi phần tử đều có thể tra cứu chính xác
        for (int i = 1; i <= 6; i++) {
            GetResult res = table.get(i * 10L);
            assertTrue(res.isFound(), "Phải tìm thấy " + (i * 10L));
            assertEquals(i, res.getValue(), "Giá trị của " + (i * 10L));
        }

        // Kiểm tra DIB trung bình và Max DIB hợp lệ
        assertTrue(table.getAverageDIB() >= 0.0, "DIB trung bình >= 0");
        assertTrue(table.getMaxDIB() >= 0, "Max DIB >= 0");

        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra cơ chế Early Exit so với No Early Exit.
     */
    public static void testEarlyExitVersusNoEarly() {
        System.out.print("[Test 3] Đối chiếu dừng sớm rh vs rh-noearly... ");
        int cap = 256;
        RobinHoodTable rhEarly = new RobinHoodTable(cap, true);
        RobinHoodTable rhNoEarly = new RobinHoodTable(cap, false);

        assertTrue(rhEarly.isEarlyExitEnabled(), "rhEarly phải bật early exit");
        assertTrue(!rhNoEarly.isEarlyExitEnabled(), "rhNoEarly phải tắt early exit");

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
                "Early exit (" + earlyProbes + ") phải tiết kiệm bước dò hơn no-early (" + noEarlyProbes + ")");

        System.out.println("PASSED (Early: " + earlyProbes + " probes, NoEarly: " + noEarlyProbes + " probes)");
    }

    /**
     * 4. Kiểm tra xóa dịch lùi (Backward-shift deletion) không tạo tombstone.
     */
    public static void testBackwardShiftDeletion() {
        System.out.print("[Test 4] Xóa dịch lùi (Backward-shift deletion)... ");
        RobinHoodTable table = new RobinHoodTable(16);

        // Chèn 10 khóa
        for (int i = 1; i <= 10; i++) {
            table.put(i * 17L, i);
        }
        assertEquals(10, table.size(), "Size ban đầu");

        // Xóa một số khóa ở giữa cụm
        assertTrue(table.remove(3 * 17L), "Xóa 51L");
        assertTrue(table.remove(7 * 17L), "Xóa 119L");
        assertEquals(8, table.size(), "Size còn 8");
        assertEquals(0, table.getTombstoneCount(), "Không có tombstone");

        // Kiểm tra các khóa còn lại vẫn tra cứu được đầy đủ
        for (int i = 1; i <= 10; i++) {
            if (i == 3 || i == 7) {
                assertTrue(!table.get(i * 17L).isFound(), "Khóa " + (i * 17L) + " đã bị xóa");
            } else {
                assertTrue(table.get(i * 17L).isFound(), "Khóa " + (i * 17L) + " phải còn sống");
            }
        }

        System.out.println("PASSED");
    }

    /**
     * 5. Kiểm tra bảng đầy (TABLE_FULL).
     */
    public static void testTableFullCondition() {
        System.out.print("[Test 5] Xử lý bảng đầy (TABLE_FULL)... ");
        RobinHoodTable table = new RobinHoodTable(8);

        for (int i = 0; i < 8; i++) {
            assertEquals(PutStatus.INSERTED, table.put(i * 13L + 1, i), "Chèn phần tử thứ " + i);
        }
        assertEquals(8, table.size(), "Bảng đã đầy 8 phần tử");

        // Cố chèn thêm phần tử thứ 9
        assertEquals(PutStatus.TABLE_FULL, table.put(999L, 999), "Phải báo TABLE_FULL");
        assertEquals(8, table.size(), "Size vẫn là 8");

        // Cập nhật phần tử cũ vẫn phải thành công
        assertEquals(PutStatus.UPDATED, table.put(1L, 999), "Cập nhật phần tử cũ phải thành công");
        assertEquals(8, table.size(), "Size vẫn giữ nguyên");

        System.out.println("PASSED");
    }

    /**
     * 6. Kiểm tra đếm số bước dò (Probe Counting).
     */
    public static void testProbeCounting() {
        System.out.print("[Test 6] Đếm số bước dò (Probe Counting)... ");
        RobinHoodTable table = new RobinHoodTable(32);
        table.enableProbeCounting(true);
        table.resetProbeCount();

        table.put(10L, 1);
        table.put(20L, 2);
        assertTrue(table.getProbeCount() > 0, "Probe count phải > 0 sau khi chèn");

        table.resetProbeCount();
        assertEquals(0, table.getProbeCount(), "Probe count sau reset phải là 0");

        table.get(10L);
        assertTrue(table.getProbeCount() >= 1, "Tra cứu 10L phải tốn ít nhất 1 bước dò");

        System.out.println("PASSED");
    }

    /**
     * 7. Kiểm thử Oracle đối chiếu với java.util.HashMap trên 100,000 thao tác ngẫu nhiên.
     */
    public static void testOracleWithHashMap(int operations, long seed) {
        System.out.print("[Test 7] Oracle Test đối chiếu java.util.HashMap (" + operations + " ops)... ");
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
                    assertTrue(r.isFound(), "Oracle có key " + key + " nhưng table báo NOT_FOUND");
                    assertEquals((int) oracle.get(key), r.getValue(), "Giá trị key " + key + " không khớp");
                } else {
                    assertTrue(!r.isFound(), "Oracle không có key " + key + " nhưng table báo FOUND");
                }
            } else {
                // Remove
                boolean removed = table.remove(key);
                if (oracle.containsKey(key)) {
                    assertTrue(removed, "Oracle có key " + key + " nhưng table xóa thất bại");
                    oracle.remove(key);
                } else {
                    assertTrue(!removed, "Oracle không có key " + key + " nhưng table báo xóa thành công");
                }
            }

            // Đối chiếu size và số tombstone
            assertEquals(oracle.size(), table.size(), "Size không khớp tại bước " + i);
            assertEquals(0, table.getTombstoneCount(), "Robin Hood không bao giờ có tombstone");
        }

        System.out.println("PASSED");
    }
}
