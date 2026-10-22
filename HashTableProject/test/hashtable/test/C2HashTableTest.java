package hashtable.test;

import hashtable.core.FibonacciHash;
import hashtable.core.GetResult;
import hashtable.core.LinearProbingTombstone;
import hashtable.core.PutStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Bộ kiểm thử toàn diện cho Cặp 2 (C2) - Linear Probing Tombstone (lp-tomb):
 * 1. 6 ca kiểm thử bắt buộc (Ca 1 và Ca 5 dùng M = 16 với khóa đụng độ tìm bằng thử ngẫu nhiên).
 * 2. Kiểm tra bất biến cấu trúc (DIB đúng, không có EMPTY chặn giữa ô băm gốc và vị trí khóa).
 * 3. Chạy Oracle (1.000.000 thao tác 40/40/20) đối chiếu java.util.HashMap cho:
 *    - lp-tomb có rebuild (tau = 0.25)
 *    - lp-tomb không rebuild
 */
public final class C2HashTableTest {
    private static final int CAPACITY = 16;

    private C2HashTableTest() {}

    public static void runAllTests() {
        System.out.println("--- Bắt đầu 6 ca bắt buộc và Oracle cho lp-tomb (Cặp 2 - C2) ---");
        testLinearProbingTombstoneRandomCollisions();
        testLinearProbingTombstoneRepeatedInsertDelete();
        testLinearProbingTombstoneWrapAroundWithTomb();
        testLinearProbingTombstoneUpdateWithPriorTomb();
        testLinearProbingTombstoneFirstTombReuseWithCollisions();
        testLinearProbingTombstoneRebuildAndInvariant();

        // Oracle cho lp-tomb CÓ REBUILD
        testOracle(new LinearProbingTombstone(16_384, 0.25), "lp-tomb (có rebuild)", 101L);
        // Oracle cho lp-tomb KHÔNG REBUILD
        testOracle(new LinearProbingTombstone(16_384), "lp-tomb (không rebuild)", 202L);

        System.out.println("--- TẤT CẢ 6 CA C2 VÀ ORACLE 1.000.000 THAO TÁC ĐÃ PASS ---\n");
    }

    /**
     * Ca 1: M = 16 với khóa đụng độ tìm bằng thử ngẫu nhiên.
     * Chèn các khóa có cùng ô băm gốc (home), xác nhận chuỗi dò tuyến tính liên tục,
     * xóa phần tử giữa chuỗi để tạo TOMB, kiểm tra bất biến cấu trúc và tính tìm kiếm được.
     */
    private static void testLinearProbingTombstoneRandomCollisions() {
        System.out.print("[C2 Case 1] LP-tomb: M=16, khóa đụng độ ngẫu nhiên... ");
        LinearProbingTombstone table = new LinearProbingTombstone(CAPACITY);
        long[] keys = findCollidingKeys(CAPACITY, 5, 201L);

        for (int i = 0; i < keys.length; i++) {
            assertEquals(PutStatus.INSERTED, table.put(keys[i], i * 10), "Chèn khóa đụng độ " + i);
            table.validateInvariants();
        }

        for (int i = 0; i < keys.length; i++) {
            assertValue(table, keys[i], i * 10);
        }

        // Xóa khóa ở giữa (keys[2]) -> biến thành TOMB
        assertTrue(table.remove(keys[2]), "Xóa khóa ở giữa chuỗi đụng độ");
        table.validateInvariants();

        // Kiểm tra các khóa còn lại vẫn tra cứu được xuyên qua ô TOMB
        for (int i = 0; i < keys.length; i++) {
            assertEquals(i != 2, table.get(keys[i]).isFound(),
                    "Khóa " + keys[i] + " (i=" + i + ") phải duy trì đúng trạng thái sau khi xóa");
        }

        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * Ca 2: Chèn và xóa liên tục qua nhiều chu kỳ (churn workload) tích lũy TOMB,
     * kiểm tra bất biến cấu trúc tại từng bước chèn/xóa.
     */
    private static void testLinearProbingTombstoneRepeatedInsertDelete() {
        System.out.print("[C2 Case 2] LP-tomb: chèn/xóa liên tục... ");
        LinearProbingTombstone table = new LinearProbingTombstone(CAPACITY);
        for (int round = 0; round < 20; round++) {
            for (int i = 0; i < 10; i++) {
                long key = round * 100L + i;
                assertEquals(PutStatus.INSERTED, table.put(key, i), "Chèn lặp lại round " + round);
                table.validateInvariants();
            }
            for (int i = 9; i >= 0; i--) {
                long key = round * 100L + i;
                assertTrue(table.remove(key), "Xóa lặp lại round " + round);
                table.validateInvariants();
            }
            assertEquals(0, table.size(), "Table rỗng sau mỗi round");
        }
        System.out.println("PASSED");
    }

    /**
     * Ca 3: Khóa đụng độ tràn qua cuối mảng (wrap-around) với ô TOMB.
     */
    private static void testLinearProbingTombstoneWrapAroundWithTomb() {
        System.out.print("[C2 Case 3] LP-tomb: wrap-around qua cuối mảng với TOMB... ");
        LinearProbingTombstone table = new LinearProbingTombstone(CAPACITY);
        long[] keys = findCollidingKeysForHome(CAPACITY, CAPACITY - 2, 4, 303L);

        for (int i = 0; i < keys.length; i++) {
            table.put(keys[i], i + 100);
            table.validateInvariants();
        }

        // Xóa phần tử tại ô wrap-around
        assertTrue(table.remove(keys[1]), "Xóa phần tử trong cụm wrap-around");
        table.validateInvariants();

        for (int i = 0; i < keys.length; i++) {
            if (i != 1) {
                assertValue(table, keys[i], i + 100);
            }
        }
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * Ca 4: Cập nhật khóa hiện có khi có ô TOMB đi trước (không ghi đè ô TOMB).
     */
    private static void testLinearProbingTombstoneUpdateWithPriorTomb() {
        System.out.print("[C2 Case 4] LP-tomb: cập nhật khóa hiện có khi có ô TOMB đi trước... ");
        LinearProbingTombstone table = new LinearProbingTombstone(CAPACITY);
        long[] keys = findCollidingKeys(CAPACITY, 3, 404L);

        table.put(keys[0], 10);
        table.put(keys[1], 20);
        table.put(keys[2], 30);
        table.validateInvariants();

        // Xóa keys[1] để biến ô giữa thành TOMB
        table.remove(keys[1]);
        table.validateInvariants();

        // Cập nhật keys[2] (nằm sau ô TOMB)
        PutStatus status = table.put(keys[2], 300);
        assertEquals(PutStatus.UPDATED, status, "Phải trả về UPDATED");
        assertEquals(300, table.get(keys[2]).getValue(), "Giá trị keys[2] phải là 300");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * Ca 5: M = 16 với khóa đụng độ tìm bằng thử ngẫu nhiên.
     * Tạo nhiều ô TOMB liên tiếp từ các khóa đụng độ, sau đó chèn khóa đụng độ mới,
     * xác nhận thuật toán tái sử dụng đúng ô TOMB ĐẦU TIÊN gặp trên chuỗi dò.
     */
    private static void testLinearProbingTombstoneFirstTombReuseWithCollisions() {
        System.out.print("[C2 Case 5] LP-tomb: M=16, khóa đụng độ ngẫu nhiên & tái sử dụng TOMB đầu... ");
        LinearProbingTombstone table = new LinearProbingTombstone(CAPACITY);
        long[] keys = findCollidingKeys(CAPACITY, 5, 505L);

        table.put(keys[0], 1);
        table.put(keys[1], 2);
        table.put(keys[2], 3);
        table.put(keys[3], 4);
        table.validateInvariants();

        // Xóa keys[1] và keys[2] -> tạo 2 ô TOMB liên tiếp
        table.remove(keys[1]);
        table.remove(keys[2]);
        table.validateInvariants();
        int tombBefore = table.getTombstoneCount();
        assertEquals(2, tombBefore, "Có 2 ô TOMB");

        // Chèn keys[4] (cùng home hash) -> phải chiếm ô TOMB đầu tiên (vị trí keys[1] trước đó)
        table.put(keys[4], 5);
        assertEquals(1, table.getTombstoneCount(), "Tombstone count giảm đi 1");
        assertValue(table, keys[4], 5);
        assertValue(table, keys[0], 1);
        assertValue(table, keys[3], 4);
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * Ca 6: Kiểm tra cơ chế Rebuild theo ngưỡng tau*M và bất biến cấu trúc trước/sau Rebuild.
     */
    private static void testLinearProbingTombstoneRebuildAndInvariant() {
        System.out.print("[C2 Case 6] LP-tomb: Rebuild theo ngưỡng tau*M và bảo toàn thứ tự ô... ");
        LinearProbingTombstone table = new LinearProbingTombstone(CAPACITY, 0.25);
        for (long i = 1; i <= 8; i++) {
            table.put(i, (int) i * 10);
            table.validateInvariants();
        }

        // Xóa 4 phần tử để chạm ngưỡng tau * M = 4
        table.remove(1L);
        table.remove(2L);
        table.remove(3L);
        table.validateInvariants();
        assertEquals(0, table.getRebuildCount(), "Chưa rebuild khi mới có 3 tombstones");

        table.remove(4L); // Chạm ngưỡng 4 -> tự động Rebuild
        assertEquals(1, table.getRebuildCount(), "Đã kích hoạt 1 lần rebuild");
        assertEquals(0, table.getTombstoneCount(), "Sau rebuild tombstoneCount phải về 0");
        assertEquals(4, table.size(), "Còn lại 4 phần tử sống");

        table.validateInvariants();

        for (long i = 5; i <= 8; i++) {
            assertValue(table, i, (int) i * 10);
        }
        System.out.println("PASSED");
    }

    /**
     * Kiểm thử Oracle 1.000.000 thao tác (40% put, 40% get, 20% remove) đối chiếu java.util.HashMap.
     */
    private static void testOracle(LinearProbingTombstone table, String name, long seed) {
        System.out.print("[C2 Oracle] " + name + ": 1.000.000 thao tác (40/40/20)... ");
        Map<Long, Integer> oracle = new HashMap<>();
        SplittableRandom random = new SplittableRandom(seed);
        int puts = 0;
        int gets = 0;
        int removes = 0;

        for (int op = 0; op < 1_000_000; op++) {
            int slot = op % 5;
            long key = random.nextLong(1, 10_001);
            int value = random.nextInt();

            if (slot == 0 || slot == 2) {
                // 40% put
                puts++;
                boolean existed = oracle.containsKey(key);
                PutStatus actual = table.put(key, value);
                assertEquals(existed ? PutStatus.UPDATED : PutStatus.INSERTED, actual,
                        name + " put tại thao tác " + op);
                oracle.put(key, value);
            } else if (slot == 1 || slot == 3) {
                // 40% get
                gets++;
                GetResult result = table.get(key);
                Integer expected = oracle.get(key);
                assertEquals(expected != null, result.isFound(), name + " get found tại thao tác " + op);
                if (expected != null) {
                    assertEquals(expected.intValue(), result.getValue(),
                            name + " get value tại thao tác " + op);
                }
            } else {
                // 20% remove
                removes++;
                assertEquals(oracle.containsKey(key), table.remove(key),
                        name + " remove tại thao tác " + op);
                oracle.remove(key);
            }

            assertEquals(oracle.size(), table.size(), name + " size tại thao tác " + op);

            // Kiểm tra bất biến cấu trúc định kỳ mỗi 100.000 thao tác để tối ưu thời gian
            if (op % 100_000 == 0) {
                table.validateInvariants();
            }
        }

        assertEquals(400_000, puts, name + " tổng số put");
        assertEquals(400_000, gets, name + " tổng số get");
        assertEquals(200_000, removes, name + " tổng số remove");

        // Kiểm tra bất biến cấu trúc cuối cùng
        table.validateInvariants();
        System.out.println("PASSED");
    }

    /**
     * Tìm count khóa có cùng ô băm gốc (home hash) trên bảng kích thước capacity bằng thử ngẫu nhiên.
     */
    private static long[] findCollidingKeys(int capacity, int count, long seed) {
        int p = Integer.numberOfTrailingZeros(capacity);
        SplittableRandom random = new SplittableRandom(seed);
        Map<Integer, List<Long>> byHome = new HashMap<>();
        while (true) {
            long key = random.nextLong(Long.MAX_VALUE);
            int home = FibonacciHash.hash(key, p);
            List<Long> keys = byHome.computeIfAbsent(home, k -> new ArrayList<>());
            if (keys.size() < count && !keys.contains(key)) {
                keys.add(key);
                if (keys.size() == count) {
                    long[] result = new long[count];
                    for (int i = 0; i < count; i++) {
                        result[i] = keys.get(i);
                    }
                    return result;
                }
            }
        }
    }

    private static long[] findCollidingKeysForHome(int capacity, int home, int count, long seed) {
        int p = Integer.numberOfTrailingZeros(capacity);
        SplittableRandom random = new SplittableRandom(seed);
        long[] keys = new long[count];
        List<Long> selected = new ArrayList<>();
        int found = 0;
        while (found < count) {
            long key = random.nextLong(Long.MAX_VALUE);
            if (FibonacciHash.hash(key, p) == home && !selected.contains(key)) {
                keys[found++] = key;
                selected.add(key);
            }
        }
        return keys;
    }

    private static void assertValue(LinearProbingTombstone table, long key, int expected) {
        GetResult result = table.get(key);
        assertTrue(result.isFound(), "Khóa " + key + " phải tìm thấy");
        assertEquals(expected, result.getValue(), "Giá trị cho khóa " + key);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("THẤT BẠI: " + message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError("THẤT BẠI: " + message + " (mong đợi: " + expected + ", thực tế: " + actual + ")");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("THẤT BẠI: " + message + " (mong đợi: " + expected + ", thực tế: " + actual + ")");
        }
    }
}
