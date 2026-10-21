package hashtable.core;

import java.util.HashSet;
import java.util.SplittableRandom;

/**
 * Bộ sinh dữ liệu cho Biến thể B (Variant B):
 * - Chuỗi 12 ký tự gồm chữ in hoa [A-Z] và chữ số [0-9].
 * - Khóa tra cứu số nguyên được ánh xạ qua (long) s.hashCode().
 * - Hàm băm Fibonacci áp dụng trên khóa (long) s.hashCode().
 */
public final class StringKeyGenerator {

    private static final char[] ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final int STRING_LENGTH = 12;

    private StringKeyGenerator() {
    }

    /**
     * Sinh một chuỗi ngẫu nhiên 12 ký tự chữ in hoa + chữ số.
     */
    public static String generateRandomString(SplittableRandom rng) {
        char[] buf = new char[STRING_LENGTH];
        for (int i = 0; i < STRING_LENGTH; i++) {
            buf[i] = ALPHANUMERIC[rng.nextInt(ALPHANUMERIC.length)];
        }
        return new String(buf);
    }

    /**
     * Sinh tập chuỗi có mặt (Present Strings) duy nhất.
     *
     * @param count Số lượng chuỗi cần sinh
     * @param seed Hạt giống ngẫu nhiên
     * @return Mảng các chuỗi 12 ký tự không trùng lặp
     */
    public static String[] generatePresentStrings(int count, long seed) {
        validateCount(count);
        SplittableRandom rng = new SplittableRandom(seed);
        HashSet<String> seen = new HashSet<>(count * 2);
        String[] result = new String[count];

        for (int i = 0; i < count; i++) {
            String s;
            do {
                s = generateRandomString(rng);
            } while (!seen.add(s));
            result[i] = s;
        }
        return result;
    }

    /**
     * Sinh tập chuỗi vắng mặt (Absent Strings) không trùng với presentStrings.
     *
     * @param count Số lượng chuỗi cần sinh
     * @param seed Hạt giống ngẫu nhiên
     * @param presentStrings Mảng chuỗi có mặt để đối chiếu loại trừ
     * @return Mảng các chuỗi vắng mặt duy nhất
     */
    public static String[] generateAbsentStrings(int count, long seed, String[] presentStrings) {
        validateCount(count);
        if (presentStrings == null) {
            throw new IllegalArgumentException("presentStrings không được null");
        }
        HashSet<String> seen = new HashSet<>(presentStrings.length * 2 + count);
        for (String s : presentStrings) {
            seen.add(s);
        }

        SplittableRandom rng = new SplittableRandom(seed);
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            String s;
            do {
                s = generateRandomString(rng);
            } while (!seen.add(s));
            result[i] = s;
        }
        return result;
    }

    /**
     * Sinh các khóa số nguyên từ chuỗi theo quy chuẩn Biến thể B: (long) s.hashCode().
     * Đảm bảo mọi khóa long sinh ra đều là duy nhất.
     */
    public static long[] generatePresentKeys(int count, long seed) {
        validateCount(count);
        SplittableRandom rng = new SplittableRandom(seed);
        HashSet<Long> seenKeys = new HashSet<>(count * 2);
        long[] keys = new long[count];

        for (int i = 0; i < count; i++) {
            long key;
            do {
                String s = generateRandomString(rng);
                key = (long) s.hashCode();
            } while (!seenKeys.add(key));
            keys[i] = key;
        }
        return keys;
    }

    /**
     * Sinh các khóa số nguyên vắng mặt từ chuỗi theo quy chuẩn Biến thể B: (long) s.hashCode().
     */
    public static long[] generateAbsentKeys(int count, long seed, long[] presentKeys) {
        validateCount(count);
        if (presentKeys == null) {
            throw new IllegalArgumentException("presentKeys không được null");
        }
        HashSet<Long> seen = new HashSet<>(presentKeys.length * 2 + count);
        for (long k : presentKeys) {
            seen.add(k);
        }

        SplittableRandom rng = new SplittableRandom(seed);
        long[] keys = new long[count];
        for (int i = 0; i < count; i++) {
            long key;
            do {
                String s = generateRandomString(rng);
                key = (long) s.hashCode();
            } while (!seen.add(key));
            keys[i] = key;
        }
        return keys;
    }

    private static void validateCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Số lượng chuỗi không được âm: " + count);
        }
    }
}
