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
        System.out.println("--- Bat dau Unit Test cho Bien the B (Variant B - String 12 ky tu) ---");
        testStringFormatAndCharset();
        testStringGenerationUniqueness();
        testStringFibonacciHashDistribution();
        testHashTableIntegrationWithStringKeys();
        System.out.println("--- TAT CA CAC BAI TEST BIEN THE B DA VUOT QUA THANH CONG! ---\n");
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

    /**
     * 1. Kiểm tra định dạng chuỗi: đúng 12 ký tự và chỉ chứa chữ in hoa hoặc chữ số.
     */
    public static void testStringFormatAndCharset() {
        System.out.print("[Test 1] Dinh dang chuoi (12 ky tu, A-Z va 0-9)... ");
        String[] samples = StringKeyGenerator.generatePresentStrings(100, 42L);
        assertEquals(100, samples.length, "So luong mau chuoi");

        for (String s : samples) {
            assertEquals(12, s.length(), "Do dai chuoi phai chinh xac bang 12 ky tu");
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                boolean isValid = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
                assertTrue(isValid, "Ky tu '" + c + "' khong nam trong tap chu in hoa va chu so");
            }
        }
        System.out.println("PASSED");
    }

    /**
     * 2. Kiểm tra tính duy nhất của tập chuỗi sinh ra và tập vắng mặt.
     */
    public static void testStringGenerationUniqueness() {
        System.out.print("[Test 2] Tinh duy nhat va tach biet cua chuoi sinh ra... ");
        int count = 1000;
        String[] present = StringKeyGenerator.generatePresentStrings(count, 1337L);
        String[] absent = StringKeyGenerator.generateAbsentStrings(count, 1337L, present);

        java.util.HashSet<String> presentSet = new java.util.HashSet<>();
        for (String s : present) {
            assertTrue(presentSet.add(s), "Phat hien chuoi trung trong tap present: " + s);
        }

        for (String s : absent) {
            assertTrue(!presentSet.contains(s), "Chuoi vang mat lai xuat hien trong tap present: " + s);
        }

        System.out.println("PASSED");
    }

    /**
     * 3. Kiểm tra hàm băm Fibonacci trên chuỗi: h(s) = hash((long) s.hashCode(), p).
     */
    public static void testStringFibonacciHashDistribution() {
        System.out.print("[Test 3] Ham bam Fibonacci cho chuoi: (long) s.hashCode()... ");
        int p = 8; // M = 256
        int m = 1 << p;
        String[] samples = StringKeyGenerator.generatePresentStrings(500, 20261019L);

        for (String s : samples) {
            int h = FibonacciHash.hash(s, p);
            assertTrue(h >= 0 && h < m, "Chi so bam " + h + " phai nam trong doan [0, " + (m - 1) + "]");

            long key = FibonacciHash.toKey(s);
            assertEquals((long) s.hashCode(), key, "toKey phai khop voi (long) s.hashCode()");
            assertEquals(h, FibonacciHash.hash(key, p), "h(s) phai dong nhat voi h((long) s.hashCode())");
        }

        System.out.println("PASSED");
    }

    /**
     * 4. Kiểm tra tích hợp chuỗi vào tất cả các cấu hình bảng băm: SC, LP-Tomb, LP-Backshift, RH.
     */
    public static void testHashTableIntegrationWithStringKeys() {
        System.out.print("[Test 4] Tich hop khoa chuoi tren 4 ho Hash Table... ");
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
                assertTrue(st == PutStatus.INSERTED || st == PutStatus.UPDATED, "Chen thanh cong vao bang");
            }

            // Tra cứu có mặt (Hit)
            for (int i = 0; i < present.length; i++) {
                long key = FibonacciHash.toKey(present[i]);
                GetResult r = table.get(key);
                assertTrue(r.isFound(), "Phai tim thay khoa chuoi: " + present[i]);
                assertEquals(i + 1, r.getValue(), "Gia tri khoa chuoi: " + present[i]);
            }

            // Tra cứu vắng mặt (Miss)
            for (String s : absent) {
                long key = FibonacciHash.toKey(s);
                GetResult r = table.get(key);
                assertTrue(!r.isFound(), "Khong duoc tim thay khoa chuoi vang mat: " + s);
            }

            // Xóa một nửa
            for (int i = 0; i < 25; i++) {
                long key = FibonacciHash.toKey(present[i]);
                assertTrue(table.remove(key), "Xoa khoa chuoi: " + present[i]);
                assertTrue(!table.get(key).isFound(), "Khoa chuoi da bi xoa khong con ton tai");
            }
        }

        System.out.println("PASSED");
    }
}
