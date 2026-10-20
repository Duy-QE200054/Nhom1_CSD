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
        System.out.println("--- TẤT CẢ CÁC BÀI TEST CHO RH VÀ LP-BACKSHIFT ĐÃ VƯỢT QUA! ---\n");
    }

    private static void testRobinHoodBasicOperations() {
        System.out.print("[Test RH] Chèn, truy vấn và thống kê DIB... ");
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

        assertEquals(PutStatus.INSERTED, table.put(k1, 10), "Chèn k1");
        assertEquals(PutStatus.INSERTED, table.put(k2, 20), "Chèn k2");
        assertEquals(PutStatus.INSERTED, table.put(k3, 30), "Chèn k3");
        GetResult r = table.get(k2);
        assertTrue(r.isFound(), "k2 phải tìm thấy");
        assertEquals(20, r.getValue(), "Giá trị k2");
        assertTrue(table.getAverageDIB() >= 0.0, "DIB trung bình phải hỗ trợ");
        assertTrue(table.getMaxDIB() >= 0, "Max DIB phải hỗ trợ");
        System.out.println("PASSED");
    }

    private static void testRobinHoodNoEarlyBasicOperations() {
        System.out.print("[Test RH-noearly] Chèn và tìm kiếm không có early-exit... ");
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
        assertTrue(table.get(k3).isFound(), "k3 phải được tìm thấy");
        assertTrue(!table.get(999_999L).isFound(), "Khóa không tồn tại phải trả về not found");
        assertEquals(16, table.capacity(), "Capacity đúng");
        System.out.println("PASSED");
    }

    private static void testLinearProbingBackshiftDeletion() {
        System.out.print("[Test LP-backshift] Xóa dịch lùi và khóa sau vẫn còn truy cập được... ");
        LinearProbingBackshift table = new LinearProbingBackshift(16);
        table.put(10L, 1);
        table.put(26L, 2);
        table.put(42L, 3);
        table.put(58L, 4);

        assertTrue(table.remove(26L), "Xóa 26L thành công");
        assertTrue(table.get(10L).isFound(), "10L vẫn ở đó");
        assertTrue(table.get(42L).isFound(), "42L vẫn ở đó");
        assertTrue(table.get(58L).isFound(), "58L vẫn ở đó");
        assertTrue(!table.get(26L).isFound(), "26L đã bị xóa");
        assertEquals(3, table.size(), "Size sau xóa = 3");
        assertEquals(0, table.getTombstoneCount(), "Không dùng tombstone");
        System.out.println("PASSED");
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("THẤT BẠI: " + msg);
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual)) {
            throw new AssertionError("THẤT BẠI: " + msg + " (Mong đợi: " + expected + ", Thực tế: " + actual + ")");
        }
    }
}
