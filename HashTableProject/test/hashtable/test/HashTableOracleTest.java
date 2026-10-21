package hashtable.test;

import hashtable.core.GetResult;
import hashtable.core.FibonacciHash;
import hashtable.core.HashTable;
import hashtable.core.LinearProbingTombstone;
import hashtable.core.PutStatus;
import hashtable.core.SeparateChaining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Bộ test hợp đồng dùng chung cho mọi cài đặt HashTable.
 */
public final class HashTableOracleTest {

    private static final int ORACLE_CAPACITY = 1 << 12;
    private static final int ORACLE_OPERATIONS = 1_000_000;
    private static final long ORACLE_SEED = 20261022L;
    private static final int PUT = 0;
    private static final int GET = 1;
    private static final int REMOVE = 2;

    private HashTableOracleTest() {
    }

    public interface TableFactory {
        HashTable create(int capacity);
    }

    public static void runAllTests() {
        runTestsFor("sc", new TableFactory() {
            @Override
            public HashTable create(int capacity) {
                return new SeparateChaining(capacity);
            }
        });
        runTestsFor("lp-tomb (không rebuild)", new TableFactory() {
            @Override
            public HashTable create(int capacity) {
                return new LinearProbingTombstone(capacity);
            }
        });
        runTestsFor("lp-tomb (rebuild tau=0.25)", new TableFactory() {
            @Override
            public HashTable create(int capacity) {
                return new LinearProbingTombstone(capacity, 0.25);
            }
        });
    }

    /**
     * Chạy sáu ca bắt buộc và oracle một triệu thao tác cho một cài đặt.
     * Các cặp có thể đăng ký cài đặt của mình tại runAllTests() hoặc gọi trực tiếp
     * phương thức này với factory tương ứng.
     */
    public static void runTestsFor(String implementation, TableFactory factory) {
        if (implementation == null || factory == null) {
            throw new IllegalArgumentException("Tên cài đặt và factory không được null");
        }

        System.out.println("--- Oracle và 6 ca bắt buộc: " + implementation + " ---");
        testCollisionOperations(factory);
        testRemoveFromCollisionChain(factory);
        testBoundaryKeysAndValues(factory);
        testMissingAndRepeatedRemove(factory);
        testWraparoundCollisions(factory);
        testMixedOperations(factory);
        testMillionOperationOracle(implementation, factory);
        System.out.println("--- PASSED: " + implementation + " ---\n");
    }

    private static void testCollisionOperations(TableFactory factory) {
        HashTable table = factory.create(16);
        Map<Long, Integer> oracle = new HashMap<>();
        List<Long> keys = findCollidingKeys(16, -1, 3, 1L);

        for (int i = 0; i < keys.size(); i++) {
            putAndCheck(table, oracle, keys.get(i), (i + 1) * 10, "Ca 1: put va chạm");
        }
        assertEquals(oracle.size(), table.size(), "Ca 1: size sau khi chèn va chạm");
        for (long key : keys) {
            assertGetMatches(table, oracle, key, "Ca 1: get khóa va chạm");
        }
        putAndCheck(table, oracle, keys.get(1), -99, "Ca 1: cập nhật khóa va chạm");
        assertAllEntriesMatch(table, oracle, "Ca 1");
    }

    private static void testRemoveFromCollisionChain(TableFactory factory) {
        HashTable table = factory.create(16);
        Map<Long, Integer> oracle = new HashMap<>();
        List<Long> keys = findCollidingKeys(16, -1, 4, 2L);

        for (int i = 0; i < keys.size(); i++) {
            putAndCheck(table, oracle, keys.get(i), i, "Ca 2: put va chạm");
        }
        removeAndCheck(table, oracle, keys.get(1), "Ca 2: remove khóa giữa");
        for (int i = 0; i < keys.size(); i++) {
            assertGetMatches(table, oracle, keys.get(i), "Ca 2: kiểm tra sau remove");
        }
        assertAllEntriesMatch(table, oracle, "Ca 2");
    }

    private static void testBoundaryKeysAndValues(TableFactory factory) {
        HashTable table = factory.create(16);
        Map<Long, Integer> oracle = new HashMap<>();

        putAndCheck(table, oracle, 0L, Integer.MIN_VALUE, "Ca 3: khóa 0 và giá trị nhỏ nhất");
        putAndCheck(table, oracle, Long.MAX_VALUE, Integer.MAX_VALUE,
                "Ca 3: khóa lớn nhất và giá trị lớn nhất");
        putAndCheck(table, oracle, 1L, 0, "Ca 3: giá trị 0");
        assertAllEntriesMatch(table, oracle, "Ca 3");
    }

    private static void testMissingAndRepeatedRemove(TableFactory factory) {
        HashTable table = factory.create(16);
        Map<Long, Integer> oracle = new HashMap<>();

        assertGetMatches(table, oracle, 7L, "Ca 4: get khóa chưa tồn tại");
        removeAndCheck(table, oracle, 7L, "Ca 4: remove khóa chưa tồn tại");
        putAndCheck(table, oracle, 7L, 70, "Ca 4: chèn khóa");
        removeAndCheck(table, oracle, 7L, "Ca 4: remove khóa đã có");
        removeAndCheck(table, oracle, 7L, "Ca 4: remove lặp lại");
        assertAllEntriesMatch(table, oracle, "Ca 4");
    }

    private static void testWraparoundCollisions(TableFactory factory) {
        HashTable table = factory.create(16);
        Map<Long, Integer> oracle = new HashMap<>();
        List<Long> keys = findCollidingKeys(16, 15, 5, 5L);

        for (int i = 0; i < 4; i++) {
            putAndCheck(table, oracle, keys.get(i), 100 + i, "Ca 5: put va chạm qua cuối mảng");
        }
        removeAndCheck(table, oracle, keys.get(1), "Ca 5: remove giữa cụm vòng");
        putAndCheck(table, oracle, keys.get(4), 500, "Ca 5: tái sử dụng sau remove");
        assertAllEntriesMatch(table, oracle, "Ca 5");
    }

    private static void testMixedOperations(TableFactory factory) {
        HashTable table = factory.create(64);
        Map<Long, Integer> oracle = new HashMap<>();
        SplittableRandom random = new SplittableRandom(6L);

        for (int i = 0; i < 10_000; i++) {
            long key = random.nextLong(48);
            int action = random.nextInt(5);
            if (action < 2) {
                putAndCheck(table, oracle, key, random.nextInt(), "Ca 6 bước " + i + ": put");
            } else if (action < 4) {
                assertGetMatches(table, oracle, key, "Ca 6 bước " + i + ": get");
            } else {
                removeAndCheck(table, oracle, key, "Ca 6 bước " + i + ": remove");
            }
        }
        assertAllEntriesMatch(table, oracle, "Ca 6");
    }

    private static void testMillionOperationOracle(String implementation, TableFactory factory) {
        HashTable table = factory.create(ORACLE_CAPACITY);
        Map<Long, Integer> oracle = new HashMap<>();
        SplittableRandom random = new SplittableRandom(ORACLE_SEED);
        int[] actions = createBalancedActions(random);
        int putCount = 0;
        int getCount = 0;
        int removeCount = 0;

        for (int i = 0; i < actions.length; i++) {
            long key = random.nextLong(ORACLE_CAPACITY - 1L);
            switch (actions[i]) {
                case PUT:
                    putCount++;
                    putAndCheck(table, oracle, key, random.nextInt(),
                            implementation + " oracle bước " + i + ": put");
                    break;
                case GET:
                    getCount++;
                    assertGetMatches(table, oracle, key,
                            implementation + " oracle bước " + i + ": get");
                    break;
                case REMOVE:
                    removeCount++;
                    removeAndCheck(table, oracle, key,
                            implementation + " oracle bước " + i + ": remove");
                    break;
                default:
                    throw new AssertionError("Loại thao tác oracle không hợp lệ: " + actions[i]);
            }
        }

        assertEquals(400_000, putCount, implementation + ": phải có đúng 40% put");
        assertEquals(400_000, getCount, implementation + ": phải có đúng 40% get");
        assertEquals(200_000, removeCount, implementation + ": phải có đúng 20% remove");
        assertAllEntriesMatch(table, oracle, implementation + " oracle cuối");
        System.out.println("Oracle " + ORACLE_OPERATIONS + " thao tác (40/40/20), M="
                + ORACLE_CAPACITY + ", seed=" + ORACLE_SEED + ": PASSED");
    }

    private static int[] createBalancedActions(SplittableRandom random) {
        int[] actions = new int[ORACLE_OPERATIONS];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = i < 400_000 ? PUT : i < 800_000 ? GET : REMOVE;
        }
        for (int i = actions.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int action = actions[i];
            actions[i] = actions[j];
            actions[j] = action;
        }
        return actions;
    }

    private static List<Long> findCollidingKeys(int capacity, int targetBucket, int count, long seed) {
        int p = Integer.numberOfTrailingZeros(capacity);
        Map<Integer, List<Long>> buckets = new HashMap<>();
        SplittableRandom random = new SplittableRandom(seed);
        List<Long> matches = new ArrayList<>();

        for (int attempt = 0; attempt < 1_000_000; attempt++) {
            long key = random.nextLong() & Long.MAX_VALUE;
            int bucket = FibonacciHash.hash(key, p);
            if (targetBucket >= 0 && bucket != targetBucket) {
                continue;
            }
            if (targetBucket >= 0) {
                matches.add(key);
                if (matches.size() == count) {
                    return matches;
                }
            } else {
                List<Long> keys = buckets.get(bucket);
                if (keys == null) {
                    keys = new ArrayList<>();
                    buckets.put(bucket, keys);
                }
                keys.add(key);
                if (keys.size() == count) {
                    return keys;
                }
            }
        }
        throw new AssertionError("Không tìm được " + count + " khóa đụng độ trong M=" + capacity
                + (targetBucket >= 0 ? " tại bucket " + targetBucket : "") + " bằng thử ngẫu nhiên");
    }

    private static void putAndCheck(HashTable table, Map<Long, Integer> oracle,
            long key, int value, String message) {
        boolean existed = oracle.containsKey(key);
        PutStatus expected = existed ? PutStatus.UPDATED : PutStatus.INSERTED;
        PutStatus actual = table.put(key, value);
        assertEquals(expected, actual, message + ": trạng thái put");
        oracle.put(key, value);
        assertEquals(oracle.size(), table.size(), message + ": size");
    }

    private static void assertGetMatches(HashTable table, Map<Long, Integer> oracle,
            long key, String message) {
        GetResult actual = table.get(key);
        boolean expectedFound = oracle.containsKey(key);
        assertEquals(expectedFound, actual.isFound(), message + ": found");
        if (expectedFound) {
            assertEquals(oracle.get(key).intValue(), actual.getValue(), message + ": value");
        }
    }

    private static void removeAndCheck(HashTable table, Map<Long, Integer> oracle,
            long key, String message) {
        boolean expected = oracle.remove(key) != null;
        assertEquals(expected, table.remove(key), message + ": remove");
        assertEquals(oracle.size(), table.size(), message + ": size");
    }

    private static void assertAllEntriesMatch(HashTable table, Map<Long, Integer> oracle, String message) {
        assertEquals(oracle.size(), table.size(), message + ": size");
        for (Map.Entry<Long, Integer> entry : oracle.entrySet()) {
            GetResult actual = table.get(entry.getKey());
            assertEquals(true, actual.isFound(), message + ": thiếu key " + entry.getKey());
            assertEquals(entry.getValue().intValue(), actual.getValue(),
                    message + ": sai value của key " + entry.getKey());
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
