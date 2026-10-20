package hashtable.test;

import hashtable.core.FibonacciHash;
import hashtable.core.GetResult;
import hashtable.core.PutStatus;
import hashtable.core.RQ1DataGenerator;
import hashtable.core.SeparateChaining;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Kiểm thử C1: Separate Chaining và generator dữ liệu RQ1.
 */
public class C1SeparateChainingTest {

    public static void runAllTests() {
        testSeparateChainingOperations();
        testOracle();
        testGeneratorReproducibilityAndUniqueness();
        testAbsentKeys();
        testClusteredKeys();
        System.out.println("--- TẤT CẢ CÁC BÀI TEST CỦA C1 ĐÃ VƯỢT QUA THÀNH CÔNG! ---\n");
    }

    private static void testSeparateChainingOperations() {
        SeparateChaining table = new SeparateChaining(8);
        long first = keyForBucket(2, 3, 8);
        long second = anotherKeyForBucket(2, 3, 8, first);

        assertEquals(PutStatus.INSERTED, table.put(first, 10), "Chèn khóa đầu");
        assertEquals(PutStatus.INSERTED, table.put(second, 20), "Chèn khóa va chạm");
        assertEquals(7, table.getEmptyCount(), "Hai khóa cùng bucket chiếm một bucket");
        assertEquals(PutStatus.UPDATED, table.put(first, 11), "Cập nhật khóa tồn tại");
        assertEquals(11, table.get(first).getValue(), "Giá trị sau cập nhật");

        table.enableProbeCounting(true);
        table.resetProbeCount();
        assertEquals(true, table.remove(second), "Xóa khóa ở đầu danh sách");
        assertEquals(true, table.remove(first), "Xóa khóa cuối danh sách");
        assertEquals(2, table.getProbeCount(), "Đếm số node đã duyệt");
        assertEquals(8, table.getEmptyCount(), "Bucket rỗng sau khi xóa hết");
        assertEquals(0, table.size(), "Size sau khi xóa hết");
        assertEquals(false, table.get(first).isFound(), "Khóa đã xóa không còn");
    }

    private static void testOracle() {
        SeparateChaining table = new SeparateChaining(64);
        Map<Long, Integer> oracle = new HashMap<>();
        long state = 42L;
        for (int i = 0; i < 20_000; i++) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            long key = (state & Long.MAX_VALUE) % 2_000;
            int value = i - 10_000;
            switch (i % 3) {
                case 0:
                    PutStatus expected = oracle.containsKey(key) ? PutStatus.UPDATED : PutStatus.INSERTED;
                    assertEquals(expected, table.put(key, value), "Oracle put");
                    oracle.put(key, value);
                    break;
                case 1:
                    assertEquals(oracle.remove(key) != null, table.remove(key), "Oracle remove");
                    break;
                default:
                    GetResult result = table.get(key);
                    assertEquals(oracle.containsKey(key), result.isFound(), "Oracle get found");
                    if (result.isFound()) {
                        assertEquals(oracle.get(key).intValue(), result.getValue(), "Oracle get value");
                    }
                    break;
            }
            assertEquals(oracle.size(), table.size(), "Oracle size");
        }
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

    private static long keyForBucket(int bucket, int p, int capacity) {
        for (long key = 0; key < 1_000_000; key++) {
            if (FibonacciHash.hash(key, p) == bucket) {
                return key;
            }
        }
        throw new AssertionError("Không tìm được khóa cho bucket " + bucket + " trong M=" + capacity);
    }

    private static long anotherKeyForBucket(int bucket, int p, int capacity, long excluded) {
        for (long key = excluded + 1; key < 1_000_000; key++) {
            if (FibonacciHash.hash(key, p) == bucket) {
                return key;
            }
        }
        throw new AssertionError("Không tìm được khóa va chạm cho bucket " + bucket + " trong M=" + capacity);
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
