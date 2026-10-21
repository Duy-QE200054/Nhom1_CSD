package hashtable.test;

import hashtable.core.FibonacciHash;
import hashtable.core.GetResult;
import hashtable.core.HashTable;
import hashtable.core.LinearProbingBackshift;
import hashtable.core.PutStatus;
import hashtable.core.RobinHood;
import hashtable.core.RobinHoodNoEarly;
import hashtable.core.RobinHoodTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public final class C3HashTableTest {
    private static final int CAPACITY = 16;

    private C3HashTableTest() {}

    public static void runAllTests() {
        testLinearProbingRandomCollisions();
        testLinearProbingRepeatedInsertDelete();
        testLinearProbingWrapAroundShift();
        testRobinHoodDibSwap();
        testRobinHoodRandomCollisions();
        testRobinHoodNoEarlyRepeatedInsertDelete();
        testOracle(new LinearProbingBackshift(16_384), "lp-backshift", 1L);
        testOracle(new RobinHoodTable(16_384, true), "rh", 2L);
        testOracle(new RobinHoodTable(16_384, false), "rh-noearly", 3L);
        System.out.println("--- TẤT CẢ 6 CA C3 VÀ ORACLE 1.000.000 THAO TÁC ĐÃ PASS ---\n");
    }

    private static void testLinearProbingRandomCollisions() {
        System.out.print("[C3 Case 1] LP-backshift: M=16, khóa đụng độ ngẫu nhiên... ");
        LinearProbingBackshift table = new LinearProbingBackshift(CAPACITY);
        long[] keys = findCollidingKeys(CAPACITY, 5, 101L);
        for (int i = 0; i < keys.length; i++) {
            assertEquals(PutStatus.INSERTED, table.put(keys[i], i), "Insert collision key");
        }
        for (int i = 0; i < keys.length; i++) {
            assertValue(table, keys[i], i);
        }
        assertTrue(table.remove(keys[2]), "Remove middle collision key");
        for (int i = 0; i < keys.length; i++) {
            assertEquals(i != 2, table.get(keys[i]).isFound(), "Collision chain remains searchable");
        }
        table.validateInvariants();
        System.out.println("PASSED");
    }

    private static void testLinearProbingRepeatedInsertDelete() {
        System.out.print("[C3 Case 2] LP-backshift: chèn/xóa liên tục... ");
        LinearProbingBackshift table = new LinearProbingBackshift(CAPACITY);
        for (int round = 0; round < 20; round++) {
            for (int i = 0; i < 12; i++) {
                long key = round * 100L + i;
                assertEquals(PutStatus.INSERTED, table.put(key, i), "Repeated insertion");
                table.validateInvariants();
            }
            for (int i = 11; i >= 0; i--) {
                long key = round * 100L + i;
                assertTrue(table.remove(key), "Repeated deletion");
                table.validateInvariants();
            }
            assertEquals(0, table.size(), "Table empty at end of round");
        }
        System.out.println("PASSED");
    }

    private static void testLinearProbingWrapAroundShift() {
        System.out.print("[C3 Case 3] LP-backshift: dịch lùi qua cuối mảng... ");
        LinearProbingBackshift table = new LinearProbingBackshift(CAPACITY);
        long[] keys = findCollidingKeysForHome(CAPACITY, CAPACITY - 2, 4, 202L);
        for (int i = 0; i < keys.length; i++) {
            table.put(keys[i], i + 10);
        }
        assertTrue(table.remove(keys[1]), "Remove from wrapping cluster");
        for (int i = 0; i < keys.length; i++) {
            if (i != 1) {
                assertValue(table, keys[i], i + 10);
            }
        }
        table.validateInvariants();
        System.out.println("PASSED");
    }

    private static void testRobinHoodDibSwap() {
        System.out.print("[C3 Case 4] RH: hoán đổi phần tử theo DIB... ");
        RobinHoodTable table = new RobinHoodTable(CAPACITY, true);
        long homeZeroFirst = findCollidingKeysForHome(CAPACITY, 0, 2, 303L)[0];
        long homeOne = findCollidingKeysForHome(CAPACITY, 1, 1, 404L)[0];
        long homeZeroIncoming = findCollidingKeysForHome(CAPACITY, 0, 2, 505L)[1];

        table.put(homeZeroFirst, 1);
        table.put(homeOne, 2);
        table.put(homeZeroIncoming, 3);

        assertEquals(homeZeroFirst, table.getKeyAt(0), "Original home-zero key remains at home");
        assertEquals(homeZeroIncoming, table.getKeyAt(1), "Incoming key steals the home-one slot");
        assertEquals(homeOne, table.getKeyAt(2), "Displaced key moves to the next slot");
        assertEquals(0, table.getDibAt(0), "DIB at slot 0");
        assertEquals(1, table.getDibAt(1), "DIB at slot 1");
        assertEquals(1, table.getDibAt(2), "DIB at slot 2");
        table.validateInvariants();
        System.out.println("PASSED");
    }

    private static void testRobinHoodRandomCollisions() {
        System.out.print("[C3 Case 5] RH và RH-noearly: M=16, khóa đụng độ ngẫu nhiên... ");
        long[] keys = findCollidingKeys(CAPACITY, 5, 606L);
        HashTable[] tables = {
            new RobinHoodTable(CAPACITY, true),
            new RobinHoodTable(CAPACITY, false)
        };
        for (HashTable table : tables) {
            for (int i = 0; i < keys.length; i++) {
                assertEquals(PutStatus.INSERTED, table.put(keys[i], i * 7), "Insert collision key");
            }
            for (int i = 0; i < keys.length; i++) {
                assertValue(table, keys[i], i * 7);
            }
            validateRobinHood(table);
        }
        System.out.println("PASSED");
    }

    private static void testRobinHoodNoEarlyRepeatedInsertDelete() {
        System.out.print("[C3 Case 6] RH-noearly: chèn/xóa và kiểm tra DIB... ");
        RobinHoodNoEarly table = new RobinHoodNoEarly(CAPACITY);
        for (int round = 0; round < 20; round++) {
            for (int i = 0; i < 12; i++) {
                long key = round * 100L + i;
                assertEquals(PutStatus.INSERTED, table.put(key, i), "Repeated insertion");
                table.validateInvariants();
            }
            for (int i = 0; i < 12; i++) {
                long key = round * 100L + i;
                assertTrue(table.remove(key), "Repeated deletion");
                table.validateInvariants();
            }
            assertTrue(!table.get(round * 100L).isFound(), "Deleted key is absent");
        }
        System.out.println("PASSED");
    }

    private static void testOracle(HashTable table, String name, long seed) {
        System.out.print("[C3 Oracle] " + name + ": 1.000.000 thao tác (40/40/20)... ");
        Map<Long, Integer> oracle = new HashMap<>();
        SplittableRandom random = new SplittableRandom(seed);
        int puts = 0;
        int gets = 0;
        int removes = 0;
        for (int operation = 0; operation < 1_000_000; operation++) {
            int slot = operation % 5;
            long key = random.nextLong(1, 10_001);
            int value = random.nextInt();
            if (slot == 0 || slot == 2) {
                puts++;
                boolean existed = oracle.containsKey(key);
                PutStatus actual = table.put(key, value);
                assertEquals(existed ? PutStatus.UPDATED : PutStatus.INSERTED, actual,
                        name + " put at operation " + operation);
                oracle.put(key, value);
            } else if (slot == 1 || slot == 3) {
                gets++;
                GetResult result = table.get(key);
                Integer expected = oracle.get(key);
                assertEquals(expected != null, result.isFound(), name + " get found at operation " + operation);
                if (expected != null) {
                    assertEquals(expected.intValue(), result.getValue(),
                            name + " get value at operation " + operation);
                }
            } else {
                removes++;
                assertEquals(oracle.containsKey(key), table.remove(key),
                        name + " remove at operation " + operation);
                oracle.remove(key);
            }
            assertEquals(oracle.size(), table.size(), name + " size at operation " + operation);
        }
        assertEquals(400_000, puts, name + " put count");
        assertEquals(400_000, gets, name + " get count");
        assertEquals(200_000, removes, name + " remove count");
        assertEquals(0, table.getTombstoneCount(), name + " must not create tombstones");
        validateRobinHoodOrLinear(table);
        System.out.println("PASSED");
    }

    private static long[] findCollidingKeys(int capacity, int count, long seed) {
        int p = Integer.numberOfTrailingZeros(capacity);
        SplittableRandom random = new SplittableRandom(seed);
        Map<Integer, List<Long>> byHome = new HashMap<>();
        while (true) {
            long key = random.nextLong(Long.MAX_VALUE);
            int home = FibonacciHash.hash(key, p);
            List<Long> keys = byHome.get(home);
            if (keys == null) {
                keys = new ArrayList<>();
                byHome.put(home, keys);
            }
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

    private static void validateRobinHoodOrLinear(HashTable table) {
        if (table instanceof RobinHoodTable) {
            ((RobinHoodTable) table).validateInvariants();
        } else if (table instanceof RobinHood) {
            ((RobinHood) table).validateInvariants();
        } else if (table instanceof LinearProbingBackshift) {
            ((LinearProbingBackshift) table).validateInvariants();
        } else {
            throw new AssertionError("No structural validator for " + table.getClass().getName());
        }
    }

    private static void validateRobinHood(HashTable table) {
        if (table instanceof RobinHoodTable) {
            ((RobinHoodTable) table).validateInvariants();
        } else if (table instanceof RobinHood) {
            ((RobinHood) table).validateInvariants();
        } else {
            throw new AssertionError("Expected Robin Hood table, got " + table.getClass().getName());
        }
    }

    private static void assertValue(HashTable table, long key, int expected) {
        GetResult result = table.get(key);
        assertTrue(result.isFound(), "Key " + key + " must be found");
        assertEquals(expected, result.getValue(), "Value for key " + key);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("FAIL: " + message);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError("FAIL: " + message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError("FAIL: " + message + " (expected " + expected + ", got " + actual + ")");
        }
    }
}
