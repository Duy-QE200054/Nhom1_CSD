package hashtable.bench;

import java.util.Locale;

/**
 * Phân tích thực nghiệm và lý thuyết cho RQ1: Separate Chaining vs Linear Probing.
 * Nhiệm vụ Cặp 1 (C1 - SV 1 & SV 2) theo Bảng phân công Đề tài 2 - CSD201 FA26.
 *
 * Nội dung phân tích:
 * 1. Đối chiếu số bước dò thực nghiệm với cận lý thuyết Donald Knuth (Vol 3, §6.4).
 * 2. Phân tích chi phí bộ nhớ đối tượng Java (Node overhead: 16B header, 4B/8B reference,
 *    phân mảnh Heap, cache miss L1/L2/L3, áp lực Garbage Collection).
 * 3. So sánh với mảng nguyên thủy liên tục của Linear Probing (spatial locality, hardware prefetching).
 */
public class RQ1Analysis {

    // Kích thước ô nhớ trong HotSpot 64-bit JVM với Compressed OOPs (< 32GB RAM)
    public static final int OBJECT_HEADER_BYTES = 16;  // 12 bytes Mark/Klass word + 4 bytes padding
    public static final int LONG_KEY_BYTES = 8;        // long key
    public static final int INT_VALUE_BYTES = 4;       // int value
    public static final int NODE_REF_BYTES = 4;        // Compressed OOP pointer (Node next)
    public static final int NODE_TOTAL_BYTES = OBJECT_HEADER_BYTES + LONG_KEY_BYTES + INT_VALUE_BYTES + NODE_REF_BYTES; // 32 bytes

    public static final int BUCKET_ARRAY_ENTRY_BYTES = 4; // Node[] bucket pointer

    // Linear Probing mảng nguyên thủy: long[] keys (8B), int[] values (4B), byte[] state (1B)
    public static final int LP_SLOT_PRIMITIVE_BYTES = 8 + 4 + 1; // 13 bytes/slot

    /**
     * Knuth theoretical expected probes for Linear Probing (Hit and Miss).
     * Ref: D. E. Knuth, The Art of Computer Programming, Vol. 3, §6.4.
     */
    public static double knuthLpHit(double alpha) {
        return 0.5 * (1.0 + 1.0 / (1.0 - alpha));
    }

    public static double knuthLpMiss(double alpha) {
        return 0.5 * (1.0 + 1.0 / Math.pow(1.0 - alpha, 2.0));
    }

    /**
     * Knuth theoretical expected probes for Separate Chaining (Hit and Miss).
     */
    public static double knuthScHit(double alpha) {
        return 1.0 + alpha / 2.0;
    }

    public static double knuthScMiss(double alpha) {
        return alpha;
    }

    /**
     * Ước tính dung lượng bộ nhớ lý thuyết trên mỗi khóa (bytes per key).
     */
    public static double scMemoryPerKey(double alpha) {
        // Mỗi khóa có 1 Node (32B) + phần chia sẻ con trỏ mảng bucket (4B / alpha)
        return NODE_TOTAL_BYTES + (BUCKET_ARRAY_ENTRY_BYTES / alpha);
    }

    public static double lpMemoryPerKey(double alpha) {
        // 13 bytes mỗi ô bảng / alpha
        return LP_SLOT_PRIMITIVE_BYTES / alpha;
    }

    public static void printTheoreticalComparisonTable() {
        System.out.println("=========================================================================================");
        System.out.println("   BẢNG ĐỐI CHIẾU LÝ THUYẾT KNUTH: SEPARATE CHAINING VS LINEAR PROBING (RQ1)");
        System.out.println("=========================================================================================");
        System.out.printf(Locale.US, "%-8s | %-18s | %-18s | %-18s | %-18s%n",
                "Alpha", "Knuth LP Hit", "Knuth LP Miss", "Knuth SC Hit", "Knuth SC Miss");
        System.out.println("-----------------------------------------------------------------------------------------");

        double[] alphas = {0.50, 0.75, 0.90, 0.95};
        for (double a : alphas) {
            System.out.printf(Locale.US, "%-8.2f | %-18.3f | %-18.3f | %-18.3f | %-18.3f%n",
                    a, knuthLpHit(a), knuthLpMiss(a), knuthScHit(a), knuthScMiss(a));
        }
        System.out.println("=========================================================================================\n");
    }

    public static void printMemoryOverheadAnalysis() {
        System.out.println("=========================================================================================");
        System.out.println("   PHÂN TÍCH CHI PHÍ BỘ NHỚ ĐỐI TƯỢNG JAVA (JAVA OBJECT MODEL & CACHE LOCALITY)");
        System.out.println("=========================================================================================");
        System.out.println("1. Cấu trúc bộ nhớ Separate Chaining (SC):");
        System.out.printf("   - Object Header (HotSpot 64-bit, Compressed OOPs): %d bytes%n", OBJECT_HEADER_BYTES);
        System.out.printf("   - Primitive Fields (key: long, value: int):        %d bytes%n", LONG_KEY_BYTES + INT_VALUE_BYTES);
        System.out.printf("   - Reference Pointer (Node next):                    %d bytes%n", NODE_REF_BYTES);
        System.out.printf("   => Tổng kích thước mỗi đối tượng Node:             %d bytes%n", NODE_TOTAL_BYTES);
        System.out.printf("   - Mảng bucket Node[] con trỏ:                       %d bytes / slot%n", BUCKET_ARRAY_ENTRY_BYTES);
        System.out.println();
        System.out.println("2. Cấu trúc bộ nhớ Linear Probing (LP):");
        System.out.println("   - Mảng nguyên thủy phẳng: long[] keys (8B), int[] values (4B), byte[] state (1B)");
        System.out.printf("   => Tổng chi phí mỗi ô nhớ:                         %d bytes / slot%n", LP_SLOT_PRIMITIVE_BYTES);
        System.out.println();
        System.out.println("3. So sánh Memory Footprint lý thuyết theo Alpha:");
        System.out.printf(Locale.US, "%-8s | %-20s | %-20s | %-15s%n",
                "Alpha", "SC (bytes/key)", "LP (bytes/key)", "Tỉ lệ SC/LP");
        System.out.println("-----------------------------------------------------------------------------------------");
        double[] alphas = {0.50, 0.75, 0.90, 0.95};
        for (double a : alphas) {
            double scMem = scMemoryPerKey(a);
            double lpMem = lpMemoryPerKey(a);
            System.out.printf(Locale.US, "%-8.2f | %-20.2f | %-20.2f | %-15.2fx%n",
                    a, scMem, lpMem, scMem / lpMem);
        }
        System.out.println("=========================================================================================\n");
    }

    public static void printArchitecturalDiscussion() {
        System.out.println("=========================================================================================");
        System.out.println("   THẢO LUẬN BẮT BUỘC CHO PAPER: NGHỊCH LÝ SỐ BƯỚC DÒ VS THỜI GIAN THỰC TẾ (RQ1)");
        System.out.println("=========================================================================================");
        System.out.println("1. Phân mảnh Heap và Pointer Chasing trong Separate Chaining:");
        System.out.println("   - Mỗi phần tử được cấp phát độc lập bằng toán tử 'new Node()' trên heap của JVM.");
        System.out.println("   - Các Node liên kết đơn không nằm kề nhau trong không gian địa chỉ RAM vật lý.");
        System.out.println("   - Khi duyệt danh sách liên kết (while current != null; current = current.next), CPU");
        System.out.println("     bắt buộc phải giải tham chiếu con trỏ (pointer chasing), làm vô hiệu hóa đường");
        System.out.println("     truyền nạp trước (hardware prefetcher) và gây ra chuỗi CPU cache miss liên tiếp.");
        System.out.println();
        System.out.println("2. Spatial Locality và Hardware Prefetching trong Linear Probing:");
        System.out.println("   - Ba mảng nguyên thủy (keys, values, state) là các khối bộ nhớ liên tục.");
        System.out.println("   - Một cache line CPU chuẩn x86-64 (64 bytes) chứa tới 8 khóa long hoặc 4.9 ô đầy đủ.");
        System.out.println("   - Khi Linear Probing dò các ô liền kề (i+1), các ô tiếp theo đã được nạp sẵn vào");
        System.out.println("     L1/L2 cache nhờ cơ chế Stream Prefetcher phần cứng.");
        System.out.println("   => Hệ quả: Dù LP có số bước dò cao hơn SC, thời gian của MỖI BƯỚC DÒ trong LP rẻ hơn");
        System.out.println("      nhiều lần so với một bước duyệt con trỏ trong SC.");
        System.out.println();
        System.out.println("3. Tác động của Garbage Collection (GC):");
        System.out.println("   - SC cấp phát hàng triệu đối tượng Node nhỏ, tạo áp lực khổng lồ lên thế hệ trẻ");
        System.out.println("     (Eden space), kích hoạt các đợt Minor GC pauses liên tục trong giai đoạn build bảng.");
        System.out.println("   - LP chỉ cấp phát 3 mảng lớn một lần duy nhất, hoàn toàn không sinh rác GC.");
        System.out.println("=========================================================================================");
    }

    public static void main(String[] args) {
        printTheoreticalComparisonTable();
        printMemoryOverheadAnalysis();
        printArchitecturalDiscussion();
    }
}
