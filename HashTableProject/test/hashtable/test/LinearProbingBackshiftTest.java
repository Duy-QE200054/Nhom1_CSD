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
        System.out.println("--- Bat dau Unit Test cho LinearProbingBackshift (Knuth Algorithm R) (Cap 3 - C3) ---");
        testBasicOperations();
        testKnuthAlgorithmRShiftLogic();
        testWrapAroundDeletion();
        testTableFullCondition();
        testProbeCounting();
        testOracleWithHashMap(100_000, 42L);
        System.out.println("--- TAT CA CAC BAI TEST LP-BACKSHIFT DA VUOT QUA THANH CONG! ---\n");
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
     * 1. Kiểm tra thao tác cơ bản và khẳng định không có tombstone.
     */
    public static void testBasicOperations() {
        System.out.print("[Test 1] Thao tac co ban (put, get, remove, 0 tombstone)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(16);
        assertEquals(16, table.capacity(), "Capacity phai la 16");
        assertEquals(0, table.size(), "Size ban dau la 0");
        assertEquals(0, table.getTombstoneCount(), "LP Backshift khong co tombstone");
        assertEquals(16, table.getEmptyCount(), "Empty ban dau la 16");

        // Chèn khóa
        assertEquals(PutStatus.INSERTED, table.put(50L, 5), "Chen 50L");
        assertEquals(PutStatus.INSERTED, table.put(60L, 6), "Chen 60L");
        assertEquals(2, table.size(), "Size sau khi chen 2");

        // Cập nhật
        assertEquals(PutStatus.UPDATED, table.put(50L, 55), "Cap nhat 50L");
        assertEquals(2, table.size(), "Size khong doi sau cap nhat");
        assertEquals(55, table.get(50L).getValue(), "Gia tri moi cua 50L");

        // Tra cứu
        assertTrue(table.get(50L).isFound(), "Tim thay 50L");
        assertTrue(table.get(60L).isFound(), "Tim thay 60L");
        assertTrue(!table.get(999L).isFound(), "Khong tim thay 999L");

        // Xóa
        assertTrue(table.remove(50L), "Xoa 50L thanh cong");
        assertEquals(1, table.size(), "Size con 1");
        assertEquals(0, table.getTombstoneCount(), "Van 0 tombstone");
        assertTrue(!table.get(50L).isFound(), "Khong con tim thay 50L");
        assertTrue(!table.remove(50L), "Xoa lai tra ve false");

        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra tính đúng đắn của giải thuật Knuth Algorithm R trong một cụm đụng độ.
     */
    public static void testKnuthAlgorithmRShiftLogic() {
        System.out.print("[Test 2] Dich chuyen cum theo Knuth Algorithm R... ");
        LinearProbingBackshift table = new LinearProbingBackshift(16);

        // Chèn một chuỗi khóa để tạo thành cụm đụng độ liên tiếp
        for (int i = 1; i <= 8; i++) {
            table.put(i * 19L, i * 100);
        }
        assertEquals(8, table.size(), "Size = 8");

        // Xóa khóa ở đầu và giữa cụm
        assertTrue(table.remove(2 * 19L), "Xoa khoa thu 2");
        assertTrue(table.remove(5 * 19L), "Xoa khoa thu 5");
        assertEquals(6, table.size(), "Size = 6");
        assertEquals(0, table.getTombstoneCount(), "0 tombstone");

        // Kiểm tra tất cả các khóa còn lại phải tra cứu được chính xác
        for (int i = 1; i <= 8; i++) {
            GetResult res = table.get(i * 19L);
            if (i == 2 || i == 5) {
                assertTrue(!res.isFound(), "Khoa " + (i * 19L) + " phai bi xoa");
            } else {
                assertTrue(res.isFound(), "Khoa " + (i * 19L) + " phai con ton tai");
                assertEquals(i * 100, res.getValue(), "Gia tri khop");
            }
        }

        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra trường hợp cụm xoay vòng qua biên mảng (Wrap-Around).
     */
    public static void testWrapAroundDeletion() {
        System.out.print("[Test 3] Xoa khi cum xoay vong qua cuoi mang (Wrap-around)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(8);

        // Chèn 7 khóa để phủ gần hết mảng và ép xảy ra xoay vòng
        for (int i = 1; i <= 7; i++) {
            table.put(i * 7L, i);
        }
        assertEquals(7, table.size(), "Size = 7");

        // Xóa lần lượt các khóa
        for (int i = 1; i <= 7; i++) {
            assertTrue(table.remove(i * 7L), "Xoa khoa " + (i * 7L));
            assertEquals(7 - i, table.size(), "Size sau xoa");
            assertEquals(0, table.getTombstoneCount(), "0 tombstone");

            // Kiểm tra các khóa chưa bị xóa vẫn tìm thấy
            for (int j = i + 1; j <= 7; j++) {
                assertTrue(table.get(j * 7L).isFound(), "Khoa " + (j * 7L) + " van con");
            }
        }
        assertEquals(0, table.size(), "Bang tro ve rong hoan toan");

        System.out.println("PASSED");
    }

    /**
     * 4. Kiểm tra xử lý bảng đầy.
     */
    public static void testTableFullCondition() {
        System.out.print("[Test 4] Bang day (TABLE_FULL)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(8);

        for (int i = 0; i < 8; i++) {
            assertEquals(PutStatus.INSERTED, table.put(i * 11L + 3, i), "Chen phan tu thu " + i);
        }
        assertEquals(8, table.size(), "Size = 8");

        assertEquals(PutStatus.TABLE_FULL, table.put(8888L, 88), "Khong the chen khi day");
        assertEquals(8, table.size(), "Size khong doi");

        // Cập nhật phần tử có sẵn vẫn thành công
        assertEquals(PutStatus.UPDATED, table.put(3L, 999), "Cap nhat thanh cong");
        assertEquals(999, table.get(3L).getValue(), "Gia tri da cap nhat");

        System.out.println("PASSED");
    }

    /**
     * 5. Kiểm tra đếm bước dò.
     */
    public static void testProbeCounting() {
        System.out.print("[Test 5] Dem so buoc do (Probe Counting)... ");
        LinearProbingBackshift table = new LinearProbingBackshift(32);
        table.enableProbeCounting(true);
        table.resetProbeCount();

        table.put(100L, 1);
        table.put(200L, 2);
        assertTrue(table.getProbeCount() > 0, "Probe count > 0");

        table.resetProbeCount();
        table.get(100L);
        assertTrue(table.getProbeCount() >= 1, "Tra cuu ton it nhat 1 buoc");

        System.out.println("PASSED");
    }

    /**
     * 6. Kiểm thử Oracle đối chiếu với java.util.HashMap trên 100,000 thao tác ngẫu nhiên.
     */
    public static void testOracleWithHashMap(int operations, long seed) {
        System.out.print("[Test 6] Oracle Test doi chieu java.util.HashMap (" + operations + " ops)... ");
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
                    assertTrue(r.isFound(), "Oracle co key " + key + " nhung table bao NOT_FOUND");
                    assertEquals((int) oracle.get(key), r.getValue(), "Gia tri khong khop");
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

            assertEquals(oracle.size(), table.size(), "Size khong khop tai buoc " + i);
            assertEquals(0, table.getTombstoneCount(), "Backshift khong co tombstone");
        }

        System.out.println("PASSED");
    }
}
