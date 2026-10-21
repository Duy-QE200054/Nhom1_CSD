package hashtable.test;

import hashtable.core.*;
import java.util.*;
import java.util.function.IntFunction;

/**
 * Bộ test cho 5 cấu hình (lp-tomb chạy cả có và không rebuild):
 *   1. Oracle: 10^6 thao tác (40% put, 40% get, 20% remove) trên M = 2^12 so với HashMap<Long,Integer>.
 *   2. 6 ca bắt buộc (ca 1 và 5 dùng M = 16, khóa đụng độ tìm bằng thử ngẫu nhiên).
 *   3. Kiểm tra bất biến cấu trúc (DIB đúng, không có EMPTY chặn giữa ô băm gốc và vị trí khóa).
 *
 * Chạy: java -cp out hashtable.test.TableTests
 */
public class TableTests {

    static final class Config {
        final String name;
        final IntFunction<HashTable> make;
        final boolean canBeFull;
        Config(String name, IntFunction<HashTable> make, boolean canBeFull) {
            this.name = name;
            this.make = make;
            this.canBeFull = canBeFull;
        }
    }

    static final List<Config> CONFIGS = Arrays.asList(
        new Config("sc",                  SeparateChaining::new, false),
        new Config("lp-tomb (tau=inf)",   m -> new LinearProbingTombstone(m, Double.POSITIVE_INFINITY), true),
        new Config("lp-tomb (tau=0.20)",  m -> new LinearProbingTombstone(m, 0.20), true),
        new Config("lp-backshift",        LinearProbingBackshift::new, true),
        new Config("rh",                  m -> new RobinHoodTable(m, true), true),
        new Config("rh-noearly",          m -> new RobinHoodTable(m, false), true)
    );

    static int passed = 0, failed = 0;

    // ------------------------------------------------------------------ main
    public static void main(String[] args) {
        for (Config cfg : CONFIGS) {
            System.out.println("=== " + cfg.name + " ===");
            run(cfg, "oracle 10^6 ops, M=2^12", () -> oracle(cfg, 4096, 1_000_000, 42L));
            run(cfg, "oracle seed 7, M=2^12",   () -> oracle(cfg, 4096, 1_000_000, 7L));
            run(cfg, "case 1: collision chain (M=16)",          () -> case1(cfg));
            run(cfg, "case 2: update existing key",             () -> case2(cfg));
            run(cfg, "case 3: remove absent key",               () -> case3(cfg));
            run(cfg, "case 4: table full",                      () -> case4(cfg));
            run(cfg, "case 5: remove mid-chain (M=16)",         () -> case5(cfg));
            run(cfg, "case 6: wrap-around cluster",             () -> case6(cfg));
        }
        System.out.println("\nTONG KET: " + passed + " pass, " + failed + " fail");
        System.exit(failed == 0 ? 0 : 1);
    }

    interface Body { void run() throws Exception; }

    static void run(Config cfg, String name, Body b) {
        try {
            b.run();
            passed++;
            System.out.println("  [PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("  [FAIL] " + name + " -> " + t);
        }
    }

    static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    // ---------------------------------------------------------------- oracle
    /**
     * Khoảng khóa = M => tải cân bằng ~ 0.4/(0.4+0.2) = 2/3 M, không bao giờ đầy,
     * nhưng vẫn đủ cao để có nhiều đụng độ + update/remove trúng khóa có thật.
     */
    static void oracle(Config cfg, int m, int ops, long seed) {
        HashTable t = cfg.make.apply(m);
        Map<Long, Integer> ref = new HashMap<>();
        SplittableRandom rnd = new SplittableRandom(seed);
        int range = m;

        for (int i = 0; i < ops; i++) {
            int r = rnd.nextInt(100);
            long key = rnd.nextInt(range);
            if (r < 40) {
                int v = rnd.nextInt();
                boolean existed = ref.containsKey(key);
                PutStatus st = t.put(key, v);
                check(st == (existed ? PutStatus.UPDATED : PutStatus.INSERTED),
                        "op " + i + " put(" + key + ") got " + st + ", existed=" + existed);
                ref.put(key, v);
            } else if (r < 80) {
                GetResult g = t.get(key);
                Integer e = ref.get(key);
                check(g.isFound() == (e != null), "op " + i + " get(" + key + ") found=" + g.isFound());
                if (e != null) check(g.getValue() == e, "op " + i + " get(" + key + ") value " + g.getValue() + " != " + e);
            } else {
                boolean exp = ref.remove(key) != null;
                check(t.remove(key) == exp, "op " + i + " remove(" + key + ") expected " + exp);
            }
            check(t.size() == ref.size(), "op " + i + " size " + t.size() + " != " + ref.size());

            if (i % 20_000 == 0 || i == ops - 1) {
                fullCompare(t, ref, range);
                checkInvariants(t);
            }
        }
    }

    static void fullCompare(HashTable t, Map<Long, Integer> ref, int range) {
        for (long k = 0; k < range; k++) {
            GetResult g = t.get(k);
            Integer e = ref.get(k);
            check(g.isFound() == (e != null), "fullCompare key " + k + " found=" + g.isFound());
            if (e != null) check(g.getValue() == e, "fullCompare key " + k + " wrong value");
        }
    }

    // -------------------------------------------------------- 6 mandatory cases
    /** Tìm {@code count} khóa phân biệt có cùng ô băm gốc {@code home} bằng thử ngẫu nhiên. */
    static long[] collidingKeys(int m, int home, int count, long seed) {
        int p = Integer.numberOfTrailingZeros(m);
        SplittableRandom rnd = new SplittableRandom(seed);
        LinkedHashSet<Long> found = new LinkedHashSet<>();
        while (found.size() < count) {
            long k = rnd.nextLong(Long.MAX_VALUE);
            if (FibonacciHash.hash(k, p) == home) found.add(k);
        }
        long[] a = new long[count];
        int i = 0;
        for (long k : found) a[i++] = k;
        return a;
    }

    /** Ca 1: M=16, 4 khóa cùng home -> mọi khóa tra cứu được, size đúng. */
    static void case1(Config cfg) {
        HashTable t = cfg.make.apply(16);
        long[] k = collidingKeys(16, 5, 4, 1001);
        for (int i = 0; i < k.length; i++)
            check(t.put(k[i], 100 + i) == PutStatus.INSERTED, "put #" + i);
        check(t.size() == 4, "size");
        for (int i = 0; i < k.length; i++) {
            GetResult g = t.get(k[i]);
            check(g.isFound() && g.getValue() == 100 + i, "get #" + i);
        }
        check(!t.get(collidingKeys(16, 5, 5, 1001)[4]).isFound(), "absent colliding key must miss");
        checkInvariants(t);
    }

    /** Ca 2: put khóa đã có -> UPDATED, size không đổi (khóa nằm giữa chuỗi đụng độ). */
    static void case2(Config cfg) {
        HashTable t = cfg.make.apply(16);
        long[] k = collidingKeys(16, 3, 3, 2002);
        for (long x : k) t.put(x, 1);
        check(t.put(k[1], 99) == PutStatus.UPDATED, "UPDATED");
        check(t.size() == 3, "size unchanged");
        check(t.get(k[1]).getValue() == 99, "new value");
        check(t.get(k[0]).getValue() == 1 && t.get(k[2]).getValue() == 1, "others untouched");
        checkInvariants(t);
    }

    /** Ca 3: remove khóa không tồn tại -> false, bảng không đổi (cả khi home có cụm). */
    static void case3(Config cfg) {
        HashTable t = cfg.make.apply(16);
        check(!t.remove(12345L), "remove on empty table");
        long[] k = collidingKeys(16, 7, 4, 3003);
        for (int i = 0; i < 3; i++) t.put(k[i], i);
        check(!t.remove(k[3]), "remove absent colliding key");
        check(t.size() == 3, "size unchanged");
        for (int i = 0; i < 3; i++) check(t.get(k[i]).isFound(), "key " + i + " still there");
        checkInvariants(t);
    }

    /** Ca 4: đầy bảng M=16 -> TABLE_FULL (sc không bao giờ đầy), update vẫn được, get không lặp vô hạn. */
    static void case4(Config cfg) {
        HashTable t = cfg.make.apply(16);
        long[] keys = new long[17];
        SplittableRandom rnd = new SplittableRandom(4004);
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 17; i++) {
            long k;
            do { k = rnd.nextLong(Long.MAX_VALUE); } while (!seen.add(k));
            keys[i] = k;
        }
        for (int i = 0; i < 16; i++) check(t.put(keys[i], i) == PutStatus.INSERTED, "fill " + i);
        PutStatus st = t.put(keys[16], 16);
        if (cfg.canBeFull) {
            check(st == PutStatus.TABLE_FULL, "17th put expected TABLE_FULL, got " + st);
            check(t.size() == 16, "size stays 16");
            check(!t.get(keys[16]).isFound(), "failed key must not exist");
        } else {
            check(st == PutStatus.INSERTED, "sc never full");
        }
        check(t.put(keys[0], 777) == PutStatus.UPDATED, "update on full table");
        check(t.get(keys[0]).getValue() == 777, "updated value");
        for (int i = 1; i < 16; i++) check(t.get(keys[i]).getValue() == i, "get " + i);
        checkInvariants(t);
        // Xóa 1 khóa rồi chèn lại phải thành công
        check(t.remove(keys[5]), "remove on full table");
        check(t.put(keys[16], 16) == (cfg.canBeFull ? PutStatus.INSERTED : PutStatus.UPDATED), "put after remove");
        checkInvariants(t);
    }

    /** Ca 5: M=16, xóa khóa GIỮA chuỗi đụng độ -> các khóa sau vẫn tìm được; chèn lại được. */
    static void case5(Config cfg) {
        HashTable t = cfg.make.apply(16);
        long[] k = collidingKeys(16, 6, 4, 5005);
        for (int i = 0; i < 4; i++) t.put(k[i], 10 + i);
        check(t.remove(k[1]), "remove middle");
        check(!t.get(k[1]).isFound(), "removed key gone");
        check(t.get(k[0]).getValue() == 10, "head intact");
        check(t.get(k[2]).getValue() == 12, "after-gap key 2 reachable");
        check(t.get(k[3]).getValue() == 13, "after-gap key 3 reachable");
        check(t.size() == 3, "size");
        if (cfg.name.startsWith("lp-tomb")) {
            check(t.getTombstoneCount() == 1 || t.getTombstoneCount() == 0, "tomb count sane");
        } else {
            check(t.getTombstoneCount() == 0, "no tombstones for " + cfg.name);
        }
        checkInvariants(t);
        check(t.put(k[1], 55) == PutStatus.INSERTED, "re-insert removed key");
        check(t.get(k[1]).getValue() == 55, "re-inserted value");
        check(t.size() == 4, "size after re-insert");
        checkInvariants(t);
    }

    /** Ca 6: cụm quấn vòng cuối->đầu mảng (home=15), xóa ô đầu cụm. */
    static void case6(Config cfg) {
        if (cfg.name.equals("sc")) { // sc không có probing/quấn vòng: chỉ kiểm tra đúng chức năng
            HashTable t = cfg.make.apply(16);
            long[] k = collidingKeys(16, 15, 3, 6006);
            for (long x : k) t.put(x, 1);
            check(t.remove(k[0]) && t.get(k[1]).isFound() && t.get(k[2]).isFound(), "sc chain");
            return;
        }
        HashTable t = cfg.make.apply(16);
        long[] k = collidingKeys(16, 15, 4, 6006);
        for (int i = 0; i < 4; i++) t.put(k[i], 20 + i); // chiếm ô 15, 0, 1, 2
        check(t.remove(k[0]), "remove head at slot 15");
        for (int i = 1; i < 4; i++) check(t.get(k[i]).getValue() == 20 + i, "wrapped key " + i);
        check(!t.get(k[0]).isFound(), "removed gone");
        checkInvariants(t);
        check(t.remove(k[2]), "remove wrapped middle");
        check(t.get(k[1]).isFound() && t.get(k[3]).isFound(), "survivors");
        checkInvariants(t);
    }

    // ------------------------------------------------------- invariant checks
    static void checkInvariants(HashTable t) {
        if (t instanceof LinearProbingTombstone) checkTomb((LinearProbingTombstone) t);
        else if (t instanceof LinearProbingBackshift) checkBackshift((LinearProbingBackshift) t);
        else if (t instanceof RobinHoodTable) checkRobinHood((RobinHoodTable) t);
        // sc: không có bất biến probing
    }

    /** lp-tomb: không EMPTY chặn giữa home và vị trí khóa; đếm size/tomb/empty khớp. */
    static void checkTomb(LinearProbingTombstone t) {
        int m = t.capacity(), p = Integer.numberOfTrailingZeros(m), mask = m - 1;
        int occ = 0, tomb = 0, empty = 0;
        for (int i = 0; i < m; i++) {
            byte s = t.getStateAt(i);
            if (s == LinearProbingTombstone.EMPTY) { empty++; continue; }
            if (s == LinearProbingTombstone.TOMB) { tomb++; continue; }
            occ++;
            int c = FibonacciHash.hash(t.getKeyAt(i), p);
            while (c != i) {
                check(t.getStateAt(c) != LinearProbingTombstone.EMPTY,
                        "EMPTY at " + c + " blocks key " + t.getKeyAt(i) + " at " + i);
                c = (c + 1) & mask;
            }
        }
        check(occ == t.size(), "occupied " + occ + " != size " + t.size());
        check(tomb == t.getTombstoneCount(), "tomb " + tomb + " != counter " + t.getTombstoneCount());
        check(empty == t.getEmptyCount(), "empty " + empty + " != getEmptyCount " + t.getEmptyCount());
        if (t.getTauThreshold() > 0)
            check(t.getTombstoneCount() < t.getTauThreshold(), "tombstones exceed tau*M without rebuild");
    }

    /** rh: DIB đúng, không EMPTY chặn, không trùng khóa + tính chất sắp xếp Robin Hood. */
    static void checkRobinHood(RobinHoodTable t) {
        int m = t.capacity(), p = Integer.numberOfTrailingZeros(m), mask = m - 1;
        int occ = 0;
        Set<Long> seen = new HashSet<Long>();
        for (int i = 0; i < m; i++) {
            if (t.getStateAt(i) != RobinHoodTable.OCCUPIED) {
                check(t.getDibAt(i) == 0, "EMPTY slot " + i + " has nonzero DIB");
                continue;
            }
            occ++;
            long key = t.getKeyAt(i);
            int home = FibonacciHash.hash(key, p);
            check(t.getDibAt(i) == ((i - home + m) & mask), "wrong DIB at " + i);
            check(seen.add(key), "duplicate key " + key);
            for (int c = home; c != i; c = (c + 1) & mask)
                check(t.getStateAt(c) != RobinHoodTable.EMPTY, "EMPTY at " + c + " blocks key " + key);
            int n = (i + 1) & mask;
            if (t.getStateAt(n) == RobinHoodTable.OCCUPIED)
                check(t.getDibAt(n) <= t.getDibAt(i) + 1,
                        "Robin Hood order broken: dib[" + n + "]=" + t.getDibAt(n) + " > dib[" + i + "]+1");
        }
        check(occ == t.size(), "occupied " + occ + " != size " + t.size());
    }

    /** lp-backshift: không EMPTY chặn giữa home và vị trí khóa; không TOMB; đếm khớp. */
    static void checkBackshift(LinearProbingBackshift t) {
        int m = t.capacity(), p = Integer.numberOfTrailingZeros(m), mask = m - 1;
        int occ = 0;
        for (int i = 0; i < m; i++) {
            byte s = t.getStateAt(i);
            if (s == LinearProbingBackshift.EMPTY) continue;
            check(s == LinearProbingBackshift.OCCUPIED, "unknown state at " + i);
            occ++;
            int c = FibonacciHash.hash(t.getKeyAt(i), p);
            while (c != i) {
                check(t.getStateAt(c) != LinearProbingBackshift.EMPTY,
                        "EMPTY at " + c + " blocks key " + t.getKeyAt(i) + " at " + i);
                c = (c + 1) & mask;
            }
        }
        check(occ == t.size(), "occupied " + occ + " != size " + t.size());
    }
}