package hashtable.core;

/**
 * Separate Chaining với một mảng bucket và danh sách liên kết đơn ở mỗi bucket.
 * Khóa mới được chèn vào đầu danh sách.
 */
public class SeparateChaining implements HashTable {

    private static final class Node {
        private final long key;
        private int value;
        private Node next;

        private Node(long key, int value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private final int capacity;
    private final int p;
    private final Node[] buckets;

    private int size;
    private int emptyCount;
    private boolean probeCounting;
    private long probeCount;

    public SeparateChaining(int capacity) {
        if (capacity < 2 || (capacity & (capacity - 1)) != 0
                || Integer.numberOfTrailingZeros(capacity) > 30) {
            throw new IllegalArgumentException(
                    "Capacity M phải là lũy thừa của 2 trong khoảng [2, 2^30], nhận được: " + capacity);
        }
        this.capacity = capacity;
        this.p = Integer.numberOfTrailingZeros(capacity);
        this.buckets = new Node[capacity];
        this.emptyCount = capacity;
    }

    @Override
    public PutStatus put(long key, int value) {
        validateKey(key);
        int bucket = FibonacciHash.hash(key, p);
        Node current = buckets[bucket];
        while (current != null) {
            countProbe();
            if (current.key == key) {
                current.value = value;
                return PutStatus.UPDATED;
            }
            current = current.next;
        }

        if (buckets[bucket] == null) {
            emptyCount--;
        }
        buckets[bucket] = new Node(key, value, buckets[bucket]);
        size++;
        return PutStatus.INSERTED;
    }

    @Override
    public GetResult get(long key) {
        validateKey(key);
        Node current = buckets[FibonacciHash.hash(key, p)];
        while (current != null) {
            countProbe();
            if (current.key == key) {
                return GetResult.of(current.value);
            }
            current = current.next;
        }
        return GetResult.NOT_FOUND;
    }

    @Override
    public boolean remove(long key) {
        validateKey(key);
        int bucket = FibonacciHash.hash(key, p);
        Node previous = null;
        Node current = buckets[bucket];
        while (current != null) {
            countProbe();
            if (current.key == key) {
                if (previous == null) {
                    buckets[bucket] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                if (buckets[bucket] == null) {
                    emptyCount++;
                }
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public void enableProbeCounting(boolean enable) {
        probeCounting = enable;
    }

    @Override
    public void resetProbeCount() {
        probeCount = 0;
    }

    @Override
    public long getProbeCount() {
        return probeCount;
    }

    @Override
    public int getTombstoneCount() {
        return 0;
    }

    @Override
    public int getEmptyCount() {
        return emptyCount;
    }

    @Override
    public double getAverageDIB() {
        return -1.0;
    }

    @Override
    public int getMaxDIB() {
        return -1;
    }

    private void countProbe() {
        if (probeCounting) {
            probeCount++;
        }
    }

    private static void validateKey(long key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key phải không âm, nhận được: " + key);
        }
    }
}
