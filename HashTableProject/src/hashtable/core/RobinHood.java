package hashtable.core;

/**
 * Robin Hood hashing chuẩn (textbook-style): mỗi khóa lưu khoảng cách từ bucket gốc
 * (DIB = Distance from Initial Bucket). Khi chèn, khóa có DIB lớn hơn sẽ "ăn cắp" chỗ
 * của khóa có DIB nhỏ hơn. Tính chất này làm giảm phương sai của chuỗi dò và hỗ trợ
 * early-exit trong lookup.
 */
public class RobinHood implements HashTable {
    private static final byte EMPTY = 0;
    private static final byte OCCUPIED = 1;

    protected final int capacity;
    protected final int p;
    protected final int mask;

    protected final long[] keys;
    protected final int[] values;
    protected final int[] dibs;
    protected final byte[] state;

    private int size;
    private boolean probeCounting;
    private long probeCount;

    public RobinHood(int capacity) {
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity M phải là lũy thừa của 2 dương, nhận được: " + capacity);
        }
        this.capacity = capacity;
        this.p = Integer.numberOfTrailingZeros(capacity);
        this.mask = capacity - 1;
        this.keys = new long[capacity];
        this.values = new int[capacity];
        this.dibs = new int[capacity];
        this.state = new byte[capacity];
    }

    @Override
    public PutStatus put(long key, int value) {
        validateKey(key);
        int home = FibonacciHash.hash(key, p);
        int idx = home;
        int incomingDib = 0;
        long insertKey = key;
        int insertValue = value;

        while (true) {
            if (probeCounting) {
                probeCount++;
            }
            if (state[idx] == EMPTY) {
                keys[idx] = insertKey;
                values[idx] = insertValue;
                dibs[idx] = incomingDib;
                state[idx] = OCCUPIED;
                size++;
                return PutStatus.INSERTED;
            }

            if (state[idx] == OCCUPIED && keys[idx] == insertKey) {
                values[idx] = insertValue;
                return PutStatus.UPDATED;
            }

            int existingDib = distanceFromHome(idx, FibonacciHash.hash(keys[idx], p));
            if (incomingDib > existingDib) {
                long displacedKey = keys[idx];
                int displacedValue = values[idx];
                int displacedDib = existingDib;

                keys[idx] = insertKey;
                values[idx] = insertValue;
                dibs[idx] = incomingDib;
                state[idx] = OCCUPIED;

                insertKey = displacedKey;
                insertValue = displacedValue;
                incomingDib = displacedDib;
                idx = (idx + 1) & mask;
                incomingDib++;
                continue;
            }

            idx = (idx + 1) & mask;
            incomingDib++;
        }
    }

    @Override
    public GetResult get(long key) {
        validateKey(key);
        int home = FibonacciHash.hash(key, p);
        int idx = home;
        int expectedDib = 0;

        while (true) {
            if (probeCounting) {
                probeCount++;
            }
            if (idx >= capacity) {
                return GetResult.NOT_FOUND;
            }
            if (state[idx] == EMPTY) {
                return GetResult.NOT_FOUND;
            }
            if (state[idx] == OCCUPIED && keys[idx] == key) {
                return GetResult.of(values[idx]);
            }

            int slotDib = dibs[idx];
            if (expectedDib > slotDib) {
                return GetResult.NOT_FOUND;
            }

            idx = (idx + 1) & mask;
            expectedDib++;
        }
    }

    @Override
    public boolean remove(long key) {
        validateKey(key);
        int idx = FibonacciHash.hash(key, p);
        int expectedDib = 0;

        while (true) {
            if (probeCounting) {
                probeCount++;
            }
            if (state[idx] == EMPTY) {
                return false;
            }
            if (state[idx] == OCCUPIED && keys[idx] == key) {
                int hole = idx;
                int next = (idx + 1) & mask;
                while (state[next] == OCCUPIED) {
                    int nextHome = FibonacciHash.hash(keys[next], p);
                    int nextDib = distanceFromHome(next, nextHome);
                    if (nextDib == 0) {
                        break;
                    }

                    keys[hole] = keys[next];
                    values[hole] = values[next];
                    dibs[hole] = nextDib - 1;
                    state[hole] = OCCUPIED;

                    state[next] = EMPTY;
                    keys[next] = 0L;
                    values[next] = 0;
                    dibs[next] = 0;

                    hole = next;
                    next = (next + 1) & mask;
                }

                state[hole] = EMPTY;
                keys[hole] = 0L;
                values[hole] = 0;
                dibs[hole] = 0;
                size--;
                return true;
            }

            int slotDib = dibs[idx];
            if (expectedDib > slotDib) {
                return false;
            }
            idx = (idx + 1) & mask;
            expectedDib++;
        }
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
        this.probeCounting = enable;
    }

    @Override
    public void resetProbeCount() {
        this.probeCount = 0;
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
        return capacity - size;
    }

    @Override
    public double getAverageDIB() {
        if (size == 0) {
            return 0.0;
        }
        long total = 0;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED) {
                total += dibs[i];
            }
        }
        return (double) total / size;
    }

    @Override
    public int getMaxDIB() {
        int max = 0;
        for (int i = 0; i < capacity; i++) {
            if (state[i] == OCCUPIED && dibs[i] > max) {
                max = dibs[i];
            }
        }
        return max;
    }

    protected int distanceFromHome(int index, int home) {
        return (index - home + capacity) & mask;
    }

    protected boolean isProbeCounting() {
        return probeCounting;
    }

    protected void incrementProbeCount() {
        if (probeCounting) {
            probeCount++;
        }
    }

    protected byte getStateAt(int index) {
        return state[index];
    }

    protected long getKeyAt(int index) {
        return keys[index];
    }

    protected int getValueAt(int index) {
        return values[index];
    }

    protected static void validateKey(long key) {
        if (key < 0) {
            throw new IllegalArgumentException("Key phải không âm, nhận được: " + key);
        }
    }
}

