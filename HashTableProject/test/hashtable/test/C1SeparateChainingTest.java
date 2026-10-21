package hashtable.test;

import hashtable.core.FibonacciHash;
import hashtable.core.GetResult;
import hashtable.core.PutStatus;
import hashtable.core.RQ1DataGenerator;
import hashtable.core.SeparateChaining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Kiểm thử C1: Separate Chaining (sc) và generator dữ liệu RQ1.
 * Bao gồm:
 * 1. 6 ca kiểm thử bắt buộc (Ca 1 và Ca 5 dùng M = 16 với khóa đụng độ tìm bằng thử ngẫu nhiên).
 * 2. Oracle Test 1.000.000 thao tác (40/40/20) đối chiếu java.util.HashMap.
 * 3. Kiểm thử tính tái lập và phân phối của bộ sinh khóa RQ1.
 */
public class C1SeparateChainingTest {
    private static final int CAPACITY = 16;

    public static void runAllTests() {
        System.out.println("--- Bắt đầu 6 ca bắt buộc và Oracle cho Separate Chaining (Cặp 1 - C1) ---");
        testSeparateChainingRandomCollisions();
        testSeparateChainingRepeatedInsertDelete();
        testSeparateChainingBoundaryBuckets();
        testSeparateChainingUpdateInChain();
        testSeparateChainingHighLoadCollisions();
        testSeparateChainingEmptyAndMissing();

        testOracle();
        testGeneratorReproducibilityAndUniqueness();
        testAbsentKeys();
        testClusteredKeys();
        System.out.println("--- TẤT CẢ CÁC BÀI TEST CỦA C1 ĐÃ VƯỢT QUA THÀNH CÔNG! ---\n");
    }

    /**
     * Ca 1: M = 16 với khóa đụng độ tìm bằng thử ngẫu nhiên.
     * Chèn 5 khóa có cùng ô băm gốc vào cùng 1 bucket, kiểm tra tra cứu,
     * xóa node ở đầu, giữa và cuối danh sách liên kết.
     */
    private static void testSeparateChainingRandomCollisions() {
        System.out.print("[C1 Case 1] SC: M=16, khóa đụng độ ngẫu nhiên... ");
        SeparateChaining table = new SeparateChaining(CAPACITY);
        long[] keys = findCollidingKeys(CAPACITY, 5, 111L);

        for (int i = 0; i < keys.length; i++) {
            assertEquals(PutStatus.INSERTED, table.put(keys[i], i * 10), "Chèn khóa đụng độ " + i);
        }
        assertEquals(5, table.size(), "Size phải bằng 5");
        assertEquals(CAPACITY - 1, table.getEmptyCount(), "Tất cả 5 khóa chung 1 bucket nên còn M-1 bucket rỗng");

        for (int i = 0; i < keys.length; i++) {
            GetResult gr = table.get(keys[i]);
            assertTrue(gr.isFound(), "Khóa " + keys[i] + " phải tìm thấy");
            assertEquals(i * 10, gr.getValue(), "Giá trị khớp");
        }

        // Xóa phần tử giữa (keys[2])
        assertTrue(table.remove(keys[2]), "Xóa phần tử giữa chuỗi liên kết");
        assertTrue(!table.get(keys[2]).isFound(), "Phần tử đã xóa không còn tồn tại");

        // Các phần tử còn lại vẫn phải tìm thấy
        for (int i = 0; i < keys.length; i++) {
            if (i != 2) {
                assertTrue(table.get(keys[i]).isFound(), "Khóa " + keys[i] + " vẫn phải còn");
            }
        }

        // Xóa phần tử đầu chuỗi và cuối chuỗi
        assertTrue(table.remove(keys[4]), "Xóa phần tử đầu chuỗi (chèn sau cùng)");
        assertTrue(table.remove(keys[0]), "Xóa phần tử cuối chuỗi");
        assertEquals(2, table.size(), "Size còn lại 2");
        System.out.println("PASSED");
    }

    /**
     * Ca 2: Chèn/xóa liên tục qua nhiều chu kỳ trên các bucket khác nhau.
     */
    private static void testSeparateChainingRepeatedInsertDelete() {
        System.out.print("[C1 Case 2] SC: chèn/xóa liên tục... ");
        SeparateChaining table = new SeparateChaining(CAPACITY);
        for (int round = 0; round < 20; round++) {
            for (int i = 0; i < 15; i++) {
                long key = round * 1000L + i;
                assertEquals(PutStatus.INSERTED, table.put(key, i), "Chèn round " + round);
            }
            assertEquals(15, table.size(), "Size sau chèn");

            for (int i = 0; i < 15; i++) {
                long key = round * 1000L + i;
                assertTrue(table.remove(key), "Xóa round " + round);
            }
            assertEquals(0, table.size(), "Size về 0 sau khi xóa hết");
            assertEquals(CAPACITY, table.getEmptyCount(), "Mọi bucket đều rỗng");
        }
        System.out.println("PASSED");
    }

    /**
     * Ca 3: Đụng độ tại các bucket biên (bucket 0 và bucket M - 1).
     */
    private static void testSeparateChainingBoundaryBuckets() {
        System.out.print("[C1 Case 3] SC: đụng độ tại bucket biên (0 và M-1)... ");
        SeparateChaining table = new SeparateChaining(CAPACITY);
        long[] keys0 = findCollidingKeysForBucket(CAPACITY, 0, 3, 222L);
        long[] keysLast = findCollidingKeysForBucket(CAPACITY, CAPACITY - 1, 3, 333L);

        for (int i = 0; i < 3; i++) {
            table.put(keys0[i], i);
            table.put(keysLast[i], i + 10);
        }
        assertEquals(6, table.size(), "Size bằng 6");
        assertEquals(CAPACITY - 2, table.getEmptyCount(), "Chiếm đúng 2 bucket (0 và M-1)");

        for (int i = 0; i < 3; i++) {
            assertEquals(i, table.get(keys0[i]).getValue(), "Giá trị bucket 0");
            assertEquals(i + 10, table.get(keysLast[i]).getValue(), "Giá trị bucket M-1");
        }
        System.out.println("PASSED");
    }

    /**
     * Ca 4: Cập nhật giá trị khóa hiện có trong chuỗi va chạm.
     */
    private static void testSeparateChainingUpdateInChain() {
        System.out.print("[C1 Case 4] SC: cập nhật khóa hiện có trong chuỗi va chạm... ");
        SeparateChaining table = new SeparateChaining(CAPACITY);
        long[] keys = findCollidingKeys(CAPACITY, 4, 444L);

        for (int i = 0; i < keys.length; i++) {
            table.put(keys[i], i * 5);
        }
        assertEquals(4, table.size(), "Size trước cập nhật");

        // Cập nhật phần tử ở giữa chuỗi
        PutStatus status = table.put(keys[1], 999);
        assertEquals(PutStatus.UPDATED, status, "Phải báo UPDATED");
        assertEquals(4, table.size(), "Size không thay đổi khi cập nhật");
        assertEquals(999, table.get(keys[1]).getValue(), "Giá trị đã cập nhật");
        assertEquals(CAPACITY - 1, table.getEmptyCount(), "Số bucket rỗng không đổi");
        System.out.println("PASSED");
    }

    /**
     * Ca 5: M = 16 với khóa đụng độ tìm bằng thử ngẫu nhiên dưới tải cao (10 khóa chung bucket).
     * Kiểm tra đếm số bước dò (probe counting) duyệt danh sách liên kết.
     */
    private static void testSeparateChainingHighLoadCollisions() {
        System.out.print("[C1 Case 5] SC: M=16, khóa đụng độ ngẫu nhiên tải cao & đếm bước dò... ");
        SeparateChaining table = new SeparateChaining(CAPACITY);
        long[] keys = findCollidingKeys(CAPACITY, 10, 555L);

        for (int i = 0; i < keys.length; i++) {
            table.put(keys[i], i);
        }
        assertEquals(10, table.size(), "10 khóa cùng bucket");

        table.enableProbeCounting(true);
        table.resetProbeCount();

        // Khóa được chèn sau cùng nằm ở đầu danh sách liên kết -> probe count = 1
        GetResult rHead = table.get(keys[9]);
        assertTrue(rHead.isFound(), "Tìm thấy node đầu");
        assertEquals(1, table.getProbeCount(), "Node đầu chuỗi chỉ tốn 1 bước dò");

        // Khóa được chèn đầu tiên nằm ở cuối danh sách liên kết -> probe count = 10
        table.resetProbeCount();
        GetResult rTail = table.get(keys[0]);
        assertTrue(rTail.isFound(), "Tìm thấy node cuối");
        assertEquals(10, table.getProbeCount(), "Node cuối chuỗi tốn 10 bước dò");

        // Khóa vắng mặt trong cùng bucket -> duyệt hết chuỗi (10 bước) rồi trả về not found
        long absentKeyInSameBucket = findAbsentKeyInBucket(CAPACITY, keys, 777L);
        table.resetProbeCount();
        GetResult rMiss = table.get(absentKeyInSameBucket);
        assertTrue(!rMiss.isFound(), "Khóa không tồn tại");
        assertEquals(10, table.getProbeCount(), "Tìm trượt trong bucket có 10 phần tử phải dò đủ 10 bước");
        System.out.println("PASSED");
    }

    /**
     * Ca 6: Kiểm tra bảng rỗng, xóa khóa không tồn tại, đếm empty bucket.
     */
    private static void testSeparateChainingEmptyAndMissing() {
        System.out.print("[C1 Case 6] SC: bảng rỗng, xóa không tồn tại, đếm empty bucket... ");
        SeparateChaining table = new SeparateChaining(CAPACITY);
        assertEquals(0, table.size(), "Size ban đầu = 0");
        assertEquals(CAPACITY, table.getEmptyCount(), "EmptyCount ban đầu = M");

        assertTrue(!table.get(12345L).isFound(), "Get trên bảng rỗng phải trả về NOT_FOUND");
        assertTrue(!table.remove(12345L), "Remove trên bảng rỗng phải trả về false");

        table.put(100L, 1);
        assertEquals(1, table.size(), "Size = 1");
        assertEquals(CAPACITY - 1, table.getEmptyCount(), "EmptyCount = M - 1");

        assertTrue(!table.remove(999L), "Xóa khóa không có mặt phải trả về false");
        assertEquals(1, table.size(), "Size không đổi sau khi xóa trượt");

        assertTrue(table.remove(100L), "Xóa khóa có mặt trả về true");
        assertEquals(0, table.size(), "Size về 0");
        assertEquals(CAPACITY, table.getEmptyCount(), "EmptyCount quay về M");
        System.out.println("PASSED");
    }

    /**
     * Oracle Test 1.000.000 thao tác (40% put, 40% get, 20% remove) đối chiếu với java.util.HashMap.
     */
    private static void testOracle() {
        System.out.print("[C1 Oracle] sc: 1.000.000 thao tác (40/40/20)... ");
        SeparateChaining table = new SeparateChaining(16_384);
        Map<Long, Integer> oracle = new HashMap<>();
        SplittableRandom random = new SplittableRandom(42L);
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
                        "sc put tại thao tác " + op);
                oracle.put(key, value);
            } else if (slot == 1 || slot == 3) {
                // 40% get
                gets++;
                GetResult result = table.get(key);
                Integer expected = oracle.get(key);
                assertEquals(expected != null, result.isFound(), "sc get found tại thao tác " + op);
                if (expected != null) {
                    assertEquals(expected.intValue(), result.getValue(),
                            "sc get value tại thao tác " + op);
                }
            } else {
                // 20% remove
                removes++;
                assertEquals(oracle.containsKey(key), table.remove(key),
                        "sc remove tại thao tác " + op);
                oracle.remove(key);
            }

            assertEquals(oracle.size(), table.size(), "sc size tại thao tác " + op);
        }

        assertEquals(400_000, puts, "sc tổng số put");
        assertEquals(400_000, gets, "sc tổng số get");
        assertEquals(200_000, removes, "sc tổng số remove");
        System.out.println("PASSED");
    }

    private static void testGeneratorReproducibilityAndUniqueness() {
        long[] firstRun = RQ1DataGenerator.generatePresentKeys(1_000, 42L);
        long[] secondRun = RQ1DataGenerator.generatePresentKeys(1_000, 42L);
        Set<Long> unique = new HashSet<>();
        for (int i = 0; i < firstRun.length; i++) {
            assertEquals(firstRun[i], secondRun[i], "Generator phải tái lập theo seed");
            assertTrue(firstRun[i] >= 0, "Khóa phải không âm");
            unique.add(firstRun[i]);
        }
        assertEquals(firstRun.length, unique.size(), "Present keys phải duy nhất");
    }

    private static void testAbsentKeys() {
        long[] present = RQ1DataGenerator.generatePresentKeys(1_000, 1337L);
        long[] absent = RQ1DataGenerator.generateAbsentKeys(1_000, 42L, present);
        Set<Long> presentSet = new HashSet<>();
        Set<Long> absentSet = new HashSet<>();
        for (long key : present) {
            presentSet.add(key);
        }
        for (long key : absent) {
            assertTrue(!presentSet.contains(key), "Absent key không được nằm trong present set");
            absentSet.add(key);
        }
        assertEquals(absent.length, absentSet.size(), "Absent keys phải duy nhất");
    }

    private static void testClusteredKeys() {
        int capacity = 1 << 12;
        int count = 1_000;
        long[] keys = RQ1DataGenerator.generateClusteredKeys(count, capacity, 20261019L);
        int p = Integer.numberOfTrailingZeros(capacity);
        int clusterLimit = capacity / 64;
        int expectedClusterCount = (int) Math.round(count * 0.10);
        Set<Long> unique = new HashSet<>();
        for (int i = 0; i < keys.length; i++) {
            assertTrue(unique.add(keys[i]), "Clustered keys phải duy nhất");
            if (i < expectedClusterCount) {
                assertTrue(FibonacciHash.hash(keys[i], p) < clusterLimit,
                        "10% đầu tiên phải thỏa h(k) < M/64");
            }
        }
    }

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

    private static long[] findCollidingKeysForBucket(int capacity, int targetBucket, int count, long seed) {
        int p = Integer.numberOfTrailingZeros(capacity);
        SplittableRandom random = new SplittableRandom(seed);
        long[] keys = new long[count];
        List<Long> selected = new ArrayList<>();
        int found = 0;
        while (found < count) {
            long key = random.nextLong(Long.MAX_VALUE);
            if (FibonacciHash.hash(key, p) == targetBucket && !selected.contains(key)) {
                keys[found++] = key;
                selected.add(key);
            }
        }
        return keys;
    }

    private static long findAbsentKeyInBucket(int capacity, long[] presentKeys, long seed) {
        int p = Integer.numberOfTrailingZeros(capacity);
        int targetBucket = FibonacciHash.hash(presentKeys[0], p);
        SplittableRandom random = new SplittableRandom(seed);
        Set<Long> set = new HashSet<>();
        for (long k : presentKeys) {
            set.add(k);
        }
        while (true) {
            long key = random.nextLong(Long.MAX_VALUE);
            if (FibonacciHash.hash(key, p) == targetBucket && !set.contains(key)) {
                return key;
            }
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("THẤT BẠI: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("THẤT BẠI: " + message + " (mong đợi: "
                    + expected + ", thực tế: " + actual + ")");
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError("THẤT BẠI: " + message + " (mong đợi: "
                    + expected + ", thực tế: " + actual + ")");
        }
    }
}
