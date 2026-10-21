package hashtable.test;

import hashtable.core.GetResult;
import hashtable.core.LinearProbingBackshift;
import hashtable.core.PutStatus;

import java.util.HashMap;
import java.util.SplittableRandom;

/**
 * Bộ kiểm thử toàn diện cho Linear Probing Backshift (Knuth Algorithm R) - Nhiệm vụ Cặp 3 (C3).
 *
 * Kiểm tra:
 * 1. Thao tác cơ bản: put, get, remove, size, capacity, 0 tombstone.
 * 2. Giải thuật xóa Knuth Algorithm R:
 *    - Quét và dịch chuyển phần tử thích hợp lấp ô trống.
 *    - Xử lý chính xác trường hợp cụm xoay vòng qua cuối mảng (wrap-around modulo M).
 *    - Không dịch chuyển phần tử nếu home bucket của nó nằm trong (i, j].
 * 3. Xử lý bảng đầy (TABLE_FULL).
 * 4. Đếm số bước dò (Probe Counting).
 * 5. Oracle Test đối chiếu với java.util.HashMap (100,000 thao tác ngẫu nhiên).
 */
public class LinearProbingBackshiftTest {

    public static void runAllTests() {
        System.out.println("--- Bắt đầu Unit Test cho LinearProbingBackshift (Knuth Algorithm R) (Cặp 3 - C3) ---");
        testBasicOperations();
        testKnuthAlgorithmRShiftLogic();
        testWrapAroundDeletion();
        testTableFullCondition();
        testProbeCounting();
        testOracleWithHashMap(100_000, 42L);
        System.out.println("--- TẤT CẢ CÁC BÀI TEST LP-BACKSHIFT ĐÃ VƯỢT QUA THÀNH CÔNG! ---\n");
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
     * 1. Kiểm tra thao tác cơ bản và khẳng định không có tombstone.
     */
    public static void testBasicOperations() {
        System.out.print("[Test 1] Thao tác cơ bản (put, get, remove, 0 tombstone)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(16);
        assertEquals(16, table.capacity(), "Capacity phải là 16");
        assertEquals(0, table.size(), "Size ban đầu là 0");
        assertEquals(0, table.getTombstoneCount(), "LP Backshift không có tombstone");
        assertEquals(16, table.getEmptyCount(), "Empty ban đầu là 16");

        // Chèn khóa
        assertEquals(PutStatus.INSERTED, table.put(50L, 5), "Chèn 50L");
        assertEquals(PutStatus.INSERTED, table.put(60L, 6), "Chèn 60L");
        assertEquals(2, table.size(), "Size sau khi chèn 2");

        // Cập nhật
        assertEquals(PutStatus.UPDATED, table.put(50L, 55), "Cập nhật 50L");
        assertEquals(2, table.size(), "Size không đổi sau cập nhật");
        assertEquals(55, table.get(50L).getValue(), "Giá trị mới của 50L");

        // Tra cứu
        assertTrue(table.get(50L).isFound(), "Tìm thấy 50L");
        assertTrue(table.get(60L).isFound(), "Tìm thấy 60L");
        assertTrue(!table.get(999L).isFound(), "Không tìm thấy 999L");

        // Xóa
        assertTrue(table.remove(50L), "Xóa 50L thành công");
        assertEquals(1, table.size(), "Size còn 1");
        assertEquals(0, table.getTombstoneCount(), "Vẫn 0 tombstone");
        assertTrue(!table.get(50L).isFound(), "Không còn tìm thấy 50L");
        assertTrue(!table.remove(50L), "Xóa lại trả về false");

        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra tính đúng đắn của giải thuật Knuth Algorithm R trong một cụm đụng độ.
     */
    public static void testKnuthAlgorithmRShiftLogic() {
        System.out.print("[Test 2] Dịch chuyển cụm theo Knuth Algorithm R... ");
        LinearProbingBackshift table = new LinearProbingBackshift(16);

        // Chèn một chuỗi khóa để tạo thành cụm đụng độ liên tiếp
        for (int i = 1; i <= 8; i++) {
            table.put(i * 19L, i * 100);
        }
        assertEquals(8, table.size(), "Size = 8");

        // Xóa khóa ở đầu và giữa cụm
        assertTrue(table.remove(2 * 19L), "Xóa khóa thứ 2");
        assertTrue(table.remove(5 * 19L), "Xóa khóa thứ 5");
        assertEquals(6, table.size(), "Size = 6");
        assertEquals(0, table.getTombstoneCount(), "0 tombstone");

        // Kiểm tra tất cả các khóa còn lại phải tra cứu được chính xác
        for (int i = 1; i <= 8; i++) {
            GetResult res = table.get(i * 19L);
            if (i == 2 || i == 5) {
                assertTrue(!res.isFound(), "Khóa " + (i * 19L) + " phải bị xóa");
            } else {
                assertTrue(res.isFound(), "Khóa " + (i * 19L) + " phải còn tồn tại");
                assertEquals(i * 100, res.getValue(), "Giá trị khớp");
            }
        }

        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra trường hợp cụm xoay vòng qua biên mảng (Wrap-Around).
     */
    public static void testWrapAroundDeletion() {
        System.out.print("[Test 3] Xóa khi cụm xoay vòng qua cuối mảng (Wrap-around)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(8);

        // Chèn 7 khóa để phủ gần hết mảng và ép xảy ra xoay vòng
        for (int i = 1; i <= 7; i++) {
            table.put(i * 7L, i);
        }
        assertEquals(7, table.size(), "Size = 7");

        // Xóa lần lượt các khóa
        for (int i = 1; i <= 7; i++) {
            assertTrue(table.remove(i * 7L), "Xóa khóa " + (i * 7L));
            assertEquals(7 - i, table.size(), "Size sau xóa");
            assertEquals(0, table.getTombstoneCount(), "0 tombstone");

            // Kiểm tra các khóa chưa bị xóa vẫn tìm thấy
            for (int j = i + 1; j <= 7; j++) {
                assertTrue(table.get(j * 7L).isFound(), "Khóa " + (j * 7L) + " vẫn còn");
            }
        }
        assertEquals(0, table.size(), "Bảng trở về rỗng hoàn toàn");

        System.out.println("PASSED");
    }

    /**
     * 4. Kiểm tra xử lý bảng đầy.
     */
    public static void testTableFullCondition() {
        System.out.print("[Test 4] Bảng đầy (TABLE_FULL)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(8);

        for (int i = 0; i < 8; i++) {
            assertEquals(PutStatus.INSERTED, table.put(i * 11L + 3, i), "Chèn phần tử thứ " + i);
        }
        assertEquals(8, table.size(), "Size = 8");

        assertEquals(PutStatus.TABLE_FULL, table.put(8888L, 88), "Không thể chèn khi đầy");
        assertEquals(8, table.size(), "Size không đổi");

        // Cập nhật phần tử có sẵn vẫn thành công
        assertEquals(PutStatus.UPDATED, table.put(3L, 999), "Cập nhật thành công");
        assertEquals(999, table.get(3L).getValue(), "Giá trị đã cập nhật");

        System.out.println("PASSED");
    }

    /**
     * 5. Kiểm tra đếm bước dò.
     */
    public static void testProbeCounting() {
        System.out.print("[Test 5] Đếm số bước dò (Probe Counting)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(32);
        table.enableProbeCounting(true);
        table.resetProbeCount();

        table.put(100L, 1);
        table.put(200L, 2);
        assertTrue(table.getProbeCount() > 0, "Probe count > 0");

        table.resetProbeCount();
        table.get(100L);
        assertTrue(table.getProbeCount() >= 1, "Tra cứu tốn ít nhất 1 bước");

        System.out.println("PASSED");
    }

    /**
     * 6. Kiểm thử Oracle đối chiếu với java.util.HashMap trên 100,000 thao tác ngẫu nhiên.
     */
    public static void testOracleWithHashMap(int operations, long seed) {
        System.out.print("[Test 6] Oracle Test đối chiếu java.util.HashMap (" + operations + " ops)... ");
        int capacity = 1024;
        LinearProbingBackshift table = new LinearProbingBackshift(capacity);
        HashMap<Long, Integer> oracle = new HashMap<>();
        SplittableRandom rng = new SplittableRandom(seed);

        for (int i = 0; i < operations; i++) {
            int opType = rng.nextInt(3);
            long key = rng.nextLong(1, 500);
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
                    assertEquals((int) oracle.get(key), r.getValue(), "Giá trị không khớp");
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

            assertEquals(oracle.size(), table.size(), "Size không khớp tại bước " + i);
            assertEquals(0, table.getTombstoneCount(), "Backshift không có tombstone");
        }

        System.out.println("PASSED");
    }
}
