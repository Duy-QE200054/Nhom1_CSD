package hashtable.test;

import hashtable.core.FibonacciHash;
import hashtable.core.GetResult;
import hashtable.core.HashTable;
import hashtable.core.LinearProbingBackshift;
import hashtable.core.LinearProbingTombstone;
import hashtable.core.PutStatus;
import hashtable.core.RobinHoodTable;
import hashtable.core.SeparateChaining;
import hashtable.core.StringKeyGenerator;

/**
 * Bộ kiểm thử cho Biến thể B (Variant B):
 * - Bộ sinh chuỗi 12 ký tự chữ in hoa [A-Z] + chữ số [0-9].
 * - Hàm băm Fibonacci áp dụng trên (long) s.hashCode().
 * - Khả năng tương thích trên toàn bộ các cấu hình bảng băm: sc, lp-tomb, lp-backshift, rh.
 */
public class VariantBStringTest {

    public static void runAllTests() {
        System.out.println("--- Bắt đầu Unit Test cho Biến thể B (Variant B - String 12 ký tự) ---");
        testStringFormatAndCharset();
        testStringGenerationUniqueness();
        testStringFibonacciHashDistribution();
        testHashTableIntegrationWithStringKeys();
        System.out.println("--- TẤT CẢ CÁC BÀI TEST BIẾN THỂ B ĐÃ VƯỢT QUA THÀNH CÔNG! ---\n");
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

    /**
     * 1. Kiểm tra định dạng chuỗi: đúng 12 ký tự và chỉ chứa chữ in hoa hoặc chữ số.
     */
    public static void testStringFormatAndCharset() {
        System.out.print("[Test 1] Định dạng chuỗi (12 ký tự, A-Z và 0-9)... ");
        String[] samples = StringKeyGenerator.generatePresentStrings(100, 42L);
        assertEquals(100, samples.length, "Số lượng mẫu chuỗi");

        for (String s : samples) {
            assertEquals(12, s.length(), "Độ dài chuỗi phải chính xác bằng 12 ký tự");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                boolean isValid = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
                assertTrue(isValid, "Ký tự '" + c + "' không nằm trong tập chữ in hoa và chữ số");
            }
        }
        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra tính duy nhất của tập chuỗi sinh ra và tập vắng mặt.
     */
    public static void testStringGenerationUniqueness() {
        System.out.print("[Test 2] Tính duy nhất và tách biệt của chuỗi sinh ra... ");
        int count = 1000;
        String[] present = StringKeyGenerator.generatePresentStrings(count, 1337L);
        String[] absent = StringKeyGenerator.generateAbsentStrings(count, 1337L, present);

        java.util.HashSet<String> presentSet = new java.util.HashSet<>();
        for (String s : present) {
            assertTrue(presentSet.add(s), "Phát hiện chuỗi trùng trong tập present: " + s);
        }

        for (String s : absent) {
            assertTrue(!presentSet.contains(s), "Chuỗi vắng mặt lại xuất hiện trong tập present: " + s);
        }

        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra hàm băm Fibonacci trên chuỗi: h(s) = hash((long) s.hashCode(), p).
     */
    public static void testStringFibonacciHashDistribution() {
        System.out.print("[Test 3] Hàm băm Fibonacci cho chuỗi: (long) s.hashCode()... ");
        int p = 8; // M = 256
        int m = 1 << p;
        String[] samples = StringKeyGenerator.generatePresentStrings(500, 20261019L);

        for (String s : samples) {
            int h = FibonacciHash.hash(s, p);
            assertTrue(h >= 0 && h < m, "Chỉ số băm " + h + " phải nằm trong đoạn [0, " + (m - 1) + "]");

            long key = FibonacciHash.toKey(s);
            assertEquals((long) s.hashCode(), key, "toKey phải khớp với (long) s.hashCode()");
            assertEquals(h, FibonacciHash.hash(key, p), "h(s) phải đồng nhất với h((long) s.hashCode())");
        }

        System.out.println("PASSED");
    }

    /**
     * 4. Kiểm tra tích hợp chuỗi vào tất cả các cấu hình bảng băm: SC, LP-Tomb, LP-Backshift, RH.
     */
    public static void testHashTableIntegrationWithStringKeys() {
        System.out.print("[Test 4] Tích hợp khóa chuỗi trên 4 họ Hash Table... ");
        int m = 128;
        HashTable[] tables = new HashTable[] {
            new SeparateChaining(m),
            new LinearProbingTombstone(m),
            new LinearProbingBackshift(m),
            new RobinHoodTable(m, true),
            new RobinHoodTable(m, false)
        };

        String[] present = StringKeyGenerator.generatePresentStrings(50, 999L);
        String[] absent = StringKeyGenerator.generateAbsentStrings(50, 999L, present);

        for (HashTable table : tables) {
            // Chèn
            for (int i = 0; i < present.length; i++) {
                long key = FibonacciHash.toKey(present[i]);
                PutStatus st = table.put(key, i + 1);
                assertTrue(st == PutStatus.INSERTED || st == PutStatus.UPDATED, "Chèn thành công vào bảng");
            }

            // Tra cứu có mặt (Hit)
            for (int i = 0; i < present.length; i++) {
                long key = FibonacciHash.toKey(present[i]);
                GetResult r = table.get(key);
                assertTrue(r.isFound(), "Phải tìm thấy khóa chuỗi: " + present[i]);
                assertEquals(i + 1, r.getValue(), "Giá trị khóa chuỗi: " + present[i]);
            }

            // Tra cứu vắng mặt (Miss)
            for (String s : absent) {
                long key = FibonacciHash.toKey(s);
                GetResult r = table.get(key);
                assertTrue(!r.isFound(), "Không được tìm thấy khóa chuỗi vắng mặt: " + s);
            }

            // Xóa một nửa
            for (int i = 0; i < 25; i++) {
                long key = FibonacciHash.toKey(present[i]);
                assertTrue(table.remove(key), "Xóa khóa chuỗi: " + present[i]);
                assertTrue(!table.get(key).isFound(), "Khóa chuỗi đã bị xóa không còn tồn tại");
            }
        }

        System.out.println("PASSED");
    }
}
