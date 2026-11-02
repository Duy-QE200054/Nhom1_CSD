package hashtable.test;

import hashtable.core.FibonacciHash;
import hashtable.core.GetResult;
import hashtable.core.LinearProbingBackshift;
import hashtable.core.PutStatus;
import hashtable.core.RobinHood;
import hashtable.core.RobinHoodNoEarly;

public class RobinHoodAndBackshiftTest {

    public static void runAllTests() {
        testRobinHoodBasicOperations();
        testRobinHoodNoEarlyBasicOperations();
        testLinearProbingBackshiftDeletion();
        System.out.println("--- TAT CA CAC BAI TEST CHO RH VA LP-BACKSHIFT DA VUOT QUA! ---\n");
    }

    private static void testRobinHoodBasicOperations() {
        System.out.print("[Test RH] Chen, truy van va thong ke DIB... ");
        RobinHood table = new RobinHood(16);
        int p = Integer.numberOfTrailingZeros(16);
        long k1 = -1, k2 = -1, k3 = -1;
        int targetH = 5;
        for (long k = 1; k < 100_000; k++) {
            if (FibonacciHash.hash(k, p) == targetH) {
                if (k1 == -1) k1 = k;
                else if (k2 == -1) k2 = k;
                else if (k3 == -1) { k3 = k; break; }
            }
        }

        assertEquals(PutStatus.INSERTED, table.put(k1, 10), "Chen k1");
        assertEquals(PutStatus.INSERTED, table.put(k2, 20), "Chen k2");
        assertEquals(PutStatus.INSERTED, table.put(k3, 30), "Chen k3");
        GetResult r = table.get(k2);
        assertTrue(r.isFound(), "k2 phai tim thay");
        assertEquals(20, r.getValue(), "Gia tri k2");
        assertTrue(table.getAverageDIB() >= 0.0, "DIB trung binh phai ho tro");
        assertTrue(table.getMaxDIB() >= 0, "Max DIB phai ho tro");
        System.out.println("PASSED");
    }

    private static void testRobinHoodNoEarlyBasicOperations() {
        System.out.print("[Test RH-noearly] Chen va tim kiem khong co early-exit... ");
        RobinHoodNoEarly table = new RobinHoodNoEarly(16);
        int p = Integer.numberOfTrailingZeros(16);
        long k1 = -1, k2 = -1, k3 = -1;
        int targetH = 7;
        for (long k = 1; k < 100_000; k++) {
            if (FibonacciHash.hash(k, p) == targetH) {
                if (k1 == -1) k1 = k;
                else if (k2 == -1) k2 = k;
                else if (k3 == -1) { k3 = k; break; }
            }
        }

        table.put(k1, 11);
        table.put(k2, 22);
        table.put(k3, 33);
        assertTrue(table.get(k3).isFound(), "k3 phai duoc tim thay");
        assertTrue(!table.get(999_999L).isFound(), "Khoa khong ton tai phai tra ve not found");
        assertEquals(16, table.capacity(), "Capacity dung");
        System.out.println("PASSED");
    }

    private static void testLinearProbingBackshiftDeletion() {
        System.out.print("[Test LP-backshift] Xoa dich lui va khoa sau van con truy cap duoc... ");
        LinearProbingBackshift table = new LinearProbingBackshift(16);
        table.put(10L, 1);
        table.put(26L, 2);
        table.put(42L, 3);
        table.put(58L, 4);

        assertTrue(table.remove(26L), "Xoa 26L thanh cong");
        assertTrue(table.get(10L).isFound(), "10L van o do");
        assertTrue(table.get(42L).isFound(), "42L van o do");
        assertTrue(table.get(58L).isFound(), "58L van o do");
        assertTrue(!table.get(26L).isFound(), "26L da bi xoa");
        assertEquals(3, table.size(), "Size sau xoa = 3");
        assertEquals(0, table.getTombstoneCount(), "Khong dung tombstone");
        System.out.println("PASSED");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("THAT BAI: " + msg);
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError("THAT BAI: " + msg + " (Mong doi: " + expected + ", Thuc te: " + actual + ")");
        }
    }
}
