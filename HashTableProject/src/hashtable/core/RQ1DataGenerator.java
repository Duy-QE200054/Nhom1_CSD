package hashtable.core;

import java.util.HashSet;
import java.util.SplittableRandom;

/**
 * Sinh dữ liệu có thể tái lập cho workload RQ1.
 */
public final class RQ1DataGenerator {

    private static final double CLUSTERED_FRACTION = 0.10;

    private RQ1DataGenerator() {
    }

    /**
     * Sinh các khóa có mặt, không trùng nhau.
     */
    public static long[] generatePresentKeys(int count, long seed) {
        validateCount(count);
        SplittableRandom random = new SplittableRandom(seed);
        HashSet<Long> seen = new HashSet<>();
        long[] keys = new long[count];
        for (int i = 0; i < count; i++) {
            keys[i] = nextUniqueKey(random, seen);
        }
        return keys;
    }

    /**
     * Sinh khóa vắng mặt, không trùng nhau và không xuất hiện trong presentKeys.
     */
    public static long[] generateAbsentKeys(int count, long seed, long[] presentKeys) {
        validateCount(count);
        if (presentKeys == null) {
            throw new IllegalArgumentException("presentKeys không được null");
        }

        HashSet<Long> seen = new HashSet<>();
        for (long key : presentKeys) {
            validateKey(key);
            seen.add(key);
        }

        SplittableRandom random = new SplittableRandom(seed);
        long[] keys = new long[count];
        for (int i = 0; i < count; i++) {
            keys[i] = nextUniqueKey(random, seen);
        }
        return keys;
    }

    /**
     * Sinh tập khóa phân phối dồn cụm: 10% có h(k) < M/64; phần còn lại
     * được sinh ngẫu nhiên trên toàn miền băm. Mọi khóa đều duy nhất.
     */
    public static long[] generateClusteredKeys(int count, int capacity, long seed) {
        validateCount(count);
        if (capacity < 64 || (capacity & (capacity - 1)) != 0
                || Integer.numberOfTrailingZeros(capacity) > 30) {
            throw new IllegalArgumentException(
                    "Capacity M phải là lũy thừa của 2 trong khoảng [64, 2^30], nhận được: " + capacity);
        }

        int p = Integer.numberOfTrailingZeros(capacity);
        int clusteredCount = (int) Math.round(count * CLUSTERED_FRACTION);
        int clusteredBucketLimit = capacity / 64;
        SplittableRandom random = new SplittableRandom(seed);
        HashSet<Long> seen = new HashSet<>();
        long[] keys = new long[count];

        for (int i = 0; i < clusteredCount; i++) {
            long key;
            do {
                key = random.nextLong(Long.MAX_VALUE);
            } while (FibonacciHash.hash(key, p) >= clusteredBucketLimit || !seen.add(key));
            keys[i] = key;
        }
        for (int i = clusteredCount; i < count; i++) {
            keys[i] = nextUniqueKey(random, seen);
        }
        return keys;
    }

    private static long nextUniqueKey(SplittableRandom random, HashSet<Long> seen) {
        long key;
        do {
            key = random.nextLong(Long.MAX_VALUE);
        } while (!seen.add(key));
        return key;
    }

    private static void validateCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Số lượng khóa không được âm: " + count);
        }
    }

    private static void validateKey(long key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key phải không âm, nhận được: " + key);
        }
    }
}
