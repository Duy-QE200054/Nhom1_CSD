package hashtable.bench;

import hashtable.core.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Khung Runner CLI - Tích hợp toàn diện RQ1, RQ2, RQ3 (Cặp 1, Cặp 2, Cặp 3).
 * Parse tham số dòng lệnh, điều phối Generator + HashTable, xuất CSV chuẩn IEEE theo csv_schema.md.
 */
public class BenchmarkRunner {

    public static final String CSV_HEADER =
            "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,"
          + "elapsed_ms,build_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit";

    private static boolean csvOnly = false;
    private static String outFile = null;

    public static void main(String[] args) {
        if (hasFlag(args, "--header")) {
            System.out.println(CSV_HEADER);
            return;
        }

        // 1. Parse tham số từ command line
        csvOnly = hasFlag(args, "--csv-only");
        outFile = getArg(args, "--out", null);

        String rq = getArg(args, "--rq", "RQ1");
        String algo = getArg(args, "--algo", "sc");
        String requestedVariant = getArg(args, "--variant", "base");
        int m = Integer.parseInt(getArg(args, "--m", "262144"));
        double alpha = Double.parseDouble(getArg(args, "--alpha", "0.75"));
        long seed = Long.parseLong(getArg(args, "--seed", "42"));
        String mode = hasFlag(args, "--count") ? "count" : getArg(args, "--mode", "time");
        String dataset = getArg(args, "--dataset", "uniform");
        int c = Integer.parseInt(getArg(args, "--c", "0"));
        double tau = Double.parseDouble(getArg(args, "--tau", "Infinity"));
        int repeat = Integer.parseInt(getArg(args, "--repeat", "1"));
        String queryType = getArg(args, "--query", "hit");
        int queriesArg = Integer.parseInt(getArg(args, "--queries", "-1"));
        String xmx = getArg(args, "--xmx", "2g");
        String machineId = getArg(args, "--machine-id", "PC_LAB_01");
        String gitCommit = getArg(args, "--git-commit", "8b19d6d");
        int blockSize = Integer.parseInt(getArg(args, "--block-size", "10000"));
        String blockLogFile = getArg(args, "--block-log", null);
        boolean compareClean = hasFlag(args, "--compare-clean");

        if (!csvOnly) {
            System.out.println("[START] algo=" + algo + ", variant=" + requestedVariant + ", mode=" + mode
                    + ", dataset=" + dataset + ", M=" + m + ", alpha=" + alpha + ", c=" + c + ", seed=" + seed);
        }

        if (!"time".equals(mode) && !"count".equals(mode)) {
            throw new IllegalArgumentException("Unsupported mode: " + mode + " (use time or count)");
        }

        // 2. Chuẩn bị dữ liệu đầu vào ngoài khoảng đo dựng bảng.
        int n = (int) Math.round(alpha * m);
        RQ2Workload workload = null;
        long[] keys = null;
        boolean isRQ2 = (c > 0) || "RQ2".equalsIgnoreCase(rq) || compareClean;
        if (isRQ2) {
            workload = new RQ2Workload(m, alpha, seed);
        } else {
            if ("clustered".equals(dataset)) {
                keys = RQ1DataGenerator.generateClusteredKeys(n, m, seed);
            } else {
                keys = RQ1DataGenerator.generatePresentKeys(n, seed);
            }
        }

        // 3. Khởi tạo và dựng bảng; tách thời gian này khỏi thời gian truy vấn.
        long heapBefore = heapBefore();
        resetHeapPeak();
        long buildStartNs = System.nanoTime();
        long buildNs;
        HashTable table = createTable(algo, m, tau);
        String variant = "rh-noearly".equals(algo) ? "noearly" : requestedVariant;
        String csvAlgo = "rh-noearly".equals(algo) ? "rh" : algo;

        if (isRQ2) {
            workload.populateInitialKeys(table);
            buildNs = System.nanoTime() - buildStartNs;
            // Mở file ghi log khối nếu được yêu cầu
            PrintWriter blockWriter = null;
            if (blockLogFile != null) {
                try {
                    blockWriter = new PrintWriter(new FileWriter(blockLogFile, true));
                    blockWriter.println("# Block profiling for M=" + m + ", C=" + c + ", seed=" + seed + ", algo=" + algo);
                } catch (IOException e) {
                    System.err.println("Không thể tạo block log: " + e.getMessage());
                }
            }

            // Chạy churn với phân khối 10^4 thao tác
            List<RQ2Workload.BlockTiming> blockTimings = workload.runChurnCyclesWithBlockProfiling(table, c, blockSize, blockWriter);
            if (blockWriter != null) {
                blockWriter.close();
            }

            if (!csvOnly && blockTimings != null && !blockTimings.isEmpty()) {
                System.out.println("[INFO] Đã ghi nhận " + blockTimings.size() + " khối " + blockSize + " thao tác.");
            }

            // Nếu người dùng yêu cầu biến thể clean_table cho RQ2
            if ("clean_table".equals(requestedVariant) && table instanceof LinearProbingTombstone) {
                long cleanBuildStartNs = System.nanoTime();
                table = workload.buildCleanTable((LinearProbingTombstone) table);
                buildNs += System.nanoTime() - cleanBuildStartNs;
                variant = "clean_table";
            }
            keys = workload.getAliveKeysCopy();
            n = keys.length;
        } else {
            for (long k : keys) {
                table.put(k, 1);
            }
            buildNs = System.nanoTime() - buildStartNs;
        }
        double buildMs = buildNs / 1_000_000.0;

        // 4. Kiểm tra nếu có cờ --compare-clean (In bảng đối chứng cho RQ2)
        if (compareClean && (table instanceof LinearProbingTombstone) && workload != null) {
            int queryCount = Math.min(10_000, n);
            RQ2Workload.RQ2ComparisonResult comp = workload.compareWithCleanTable((LinearProbingTombstone) table, c, queryCount, seed + 999L);
            comp.printComparisonTable();
            return;
        }

        // Tạo tập khóa truy vấn theo queryType
        long[] queryKeys;
        int targetQueries = (queriesArg > 0) ? queriesArg : keys.length;
        if ("miss".equalsIgnoreCase(queryType) || "absent".equalsIgnoreCase(queryType)) {
            if (targetQueries <= keys.length) {
                queryKeys = RQ1DataGenerator.generateAbsentKeys(targetQueries, seed + 100_000L + repeat * 7L, keys);
            } else {
                long[] absentPool = RQ1DataGenerator.generateAbsentKeys(Math.min(keys.length, 1_000_000), seed + 100_000L + repeat * 7L, keys);
                queryKeys = new long[targetQueries];
                java.util.SplittableRandom qRnd = new java.util.SplittableRandom(seed + 100_000L + repeat * 7L);
                for (int i = 0; i < targetQueries; i++) {
                    queryKeys[i] = absentPool[qRnd.nextInt(absentPool.length)];
                }
            }
        } else if ("mixed".equalsIgnoreCase(queryType)) {
            int half = targetQueries / 2;
            long[] absentPool = RQ1DataGenerator.generateAbsentKeys(Math.min(keys.length - (keys.length / 2), 500_000), seed + 100_000L + repeat * 7L, keys);
            queryKeys = new long[targetQueries];
            java.util.SplittableRandom qRnd = new java.util.SplittableRandom(seed + 200_000L + repeat * 7L);
            for (int i = 0; i < half; i++) {
                queryKeys[i] = keys[qRnd.nextInt(keys.length)];
            }
            for (int i = half; i < targetQueries; i++) {
                queryKeys[i] = absentPool[qRnd.nextInt(absentPool.length)];
            }
        } else {
            if (targetQueries == keys.length) {
                queryKeys = keys;
            } else {
                queryKeys = new long[targetQueries];
                java.util.SplittableRandom qRnd = new java.util.SplittableRandom(seed + 55555L + repeat * 13L);
                for (int i = 0; i < targetQueries; i++) {
                    queryKeys[i] = keys[qRnd.nextInt(keys.length)];
                }
            }
        }

        // 5. Đo đạc theo chế độ count hoặc time
        long ops = queryKeys.length;
        String sha = computeSha256(keys);

        if ("count".equals(mode)) {
            // Lượt đếm bước dò: elapsed_ms để trống, ops_count có giá trị.
            table.enableProbeCounting(true);
            table.resetProbeCount();
            long checkSum = 0;
            for (long k : queryKeys) {
                GetResult r = table.get(k);
                if (r.isFound()) {
                    checkSum += r.getValue();
                } else {
                    checkSum++;
                }
            }
            long totalProbes = table.getProbeCount();
            double avgProbes = ops > 0 ? (double) totalProbes / ops : 0.0;
            String avgProbesStr = String.format(java.util.Locale.US, "%.4f", avgProbes);
            long heapPeak = heapPeak();

            printCsvRow(rq, csvAlgo, variant, dataset, n, m, alpha, c, tau, seed,
                    sha, "1.0.0", repeat,
                    "", buildMs, heapBefore, heapPeak, avgProbesStr,
                    checkSum, "ok", xmx, machineId, gitCommit, queryType);
        } else {
            // Warmup JIT trước khi bấm giờ: chạy thử 1000 lượt tra cứu ngoài vùng đo
            int warmupLen = Math.min(1000, queryKeys.length);
            for (int w = 0; w < warmupLen; w++) {
                table.get(queryKeys[w]);
            }

            // Lượt đo thời gian: elapsed_ms có giá trị, ops_count để trống
            long checkSum = 0;
            long startNs = System.nanoTime();
            for (long k : queryKeys) {
                GetResult r = table.get(k);
                if (r.isFound()) {
                    checkSum += r.getValue();
                } else {
                    checkSum++;
                }
            }
            long endNs = System.nanoTime();
            double elapsedMs = (endNs - startNs) / 1_000_000.0;
            String elapsedMsStr = String.format(java.util.Locale.US, "%.2f", elapsedMs);
            long heapPeak = heapPeak();

            printCsvRow(rq, csvAlgo, variant, dataset, n, m, alpha, c, tau, seed,
                    sha, "1.0.0", repeat,
                    elapsedMsStr, buildMs, heapBefore, heapPeak, "",
                    checkSum, "ok", xmx, machineId, gitCommit, queryType);
        }

        if (!csvOnly) {
            System.out.println("[DONE] Benchmark finished successfully (status=ok)"
                    + (outFile != null ? ", saved to " + outFile : ""));
        }
    }

    /** Kiểm tra xem flag có tồn tại trong args không */
    public static boolean hasFlag(String[] args, String flag) {
        for (String a : args) {
            if (flag.equals(a)) return true;
        }
        return false;
    }

    /** Lấy giá trị tham số từ args, trả về default nếu không tìm thấy */
    public static String getArg(String[] args, String flag, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) return args[i + 1];
        }
        return def;
    }

    private static HashTable createTable(String algo, int capacity, double tau) {
        switch (algo) {
            case "sc":
                return new SeparateChaining(capacity);
            case "lp-tomb":
                return new LinearProbingTombstone(capacity, tau);
            case "lp-backshift":
                return new LinearProbingBackshift(capacity);
            case "rh":
                return new RobinHoodTable(capacity, true);
            case "rh-noearly":
                return new RobinHoodTable(capacity, false);
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algo);
        }
    }

    public static String failureRow(String[] args, String status) {
        String rq = getArg(args, "--rq", "RQ1");
        String algo = getArg(args, "--algo", "sc");
        String variant = getArg(args, "--variant", "base");
        if ("rh-noearly".equals(algo)) {
            algo = "rh";
            variant = "noearly";
        }
        String dataset = getArg(args, "--dataset", "uniform");
        String m = getArg(args, "--m", "262144");
        String alpha = getArg(args, "--alpha", "0.75");
        String c = getArg(args, "--c", "0");
        String tau = getArg(args, "--tau", "Infinity");
        if ("Infinity".equalsIgnoreCase(tau) || "-Infinity".equalsIgnoreCase(tau)) {
            tau = "inf";
        }
        String seed = getArg(args, "--seed", "42");
        String repeat = getArg(args, "--repeat", "1");
        String xmx = getArg(args, "--xmx", "2g");
        String runId = "RUN_" + rq + "_" + algo.toUpperCase().replace('-', '_') + "_"
                + (variant.equals("base") ? "" : variant.toUpperCase() + "_") + repeat;

        return String.join(",",
                runId, rq, algo, variant, dataset, "",
                "M=" + m + ";alpha=" + alpha + ";c=" + c + ";tau=" + tau,
                seed, "", "1.0.0", repeat, "", "", "", "", "", "", status, xmx,
                getArg(args, "--machine-id", "PC_LAB_01"),
                getArg(args, "--git-commit", "8b19d6d"));
    }

    public static void appendCsvLine(String outputFile, String line) throws IOException {
        File file = new File(outputFile);
        boolean needHeader = !file.exists() || file.length() == 0;
        String content = (needHeader ? CSV_HEADER + System.lineSeparator() : "")
                + line + System.lineSeparator();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Heap đang dùng sau GC, trước khi dựng bảng. */
    public static long heapBefore() {
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /** Reset high-water marks so peak usage covers this benchmark's table build and queries. */
    private static void resetHeapPeak() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.getPeakUsage() != null) {
                pool.resetPeakUsage();
            }
        }
    }

    /** Tổng high-water mark của các heap memory pool. */
    public static long heapPeak() {
        long peak = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.getPeakUsage() != null) {
                peak += pool.getPeakUsage().getUsed();
            }
        }
        return peak;
    }

    /** Output checksum: tổng giá trị found + số lần miss (chống JIT DCE) */
    public static long outputCheck(HashTable t, long[] keys) {
        long sum = 0;
        for (long k : keys) {
            GetResult r = t.get(k);
            if (r.isFound()) sum += r.getValue();
            else sum++;
        }
        return sum;
    }

    /** SHA-256 Big-Endian của mảng khóa */
    public static String computeSha256(long[] keys) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteBuffer buffer = ByteBuffer.allocate(keys.length * 8);
            for (long k : keys) buffer.putLong(k);
            byte[] hash = md.digest(buffer.array());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    public static void printCsvRow(String rq, String algo, String variant, String dataset,
                                   int n, int m, double alpha, int c, double tau, long seed,
                                   String sha, String genVer, int repeat, Object elapsed,
                                   double buildMs, long heapBefore, long heapPeak, Object opsCount,
                                   long outputCheck, String status, String xmx,
                                   String machineId, String gitCommit) {
        printCsvRow(rq, algo, variant, dataset, n, m, alpha, c, tau, seed,
                sha, genVer, repeat, elapsed, buildMs, heapBefore, heapPeak, opsCount,
                outputCheck, status, xmx, machineId, gitCommit, "hit");
    }

    public static void printCsvRow(String rq, String algo, String variant, String dataset,
                                   int n, int m, double alpha, int c, double tau, long seed,
                                   String sha, String genVer, int repeat, Object elapsed,
                                   double buildMs, long heapBefore, long heapPeak, Object opsCount,
                                   long outputCheck, String status, String xmx,
                                   String machineId, String gitCommit, String queryType) {
        String tauStr = Double.isInfinite(tau) ? "inf" : String.format(java.util.Locale.US, "%.2f", tau);
        String params = ("hit".equalsIgnoreCase(queryType))
                ? String.format(java.util.Locale.US, "M=%d;alpha=%.2f;c=%d;tau=%s", m, alpha, c, tauStr)
                : String.format(java.util.Locale.US, "M=%d;alpha=%.2f;c=%d;tau=%s;query=%s", m, alpha, c, tauStr, queryType);

        String runId = "RUN_" + rq + "_" + algo.toUpperCase().replace('-', '_') + "_" + (variant.equals("base") ? "" : variant.toUpperCase() + "_") + repeat;
        String line = String.format(java.util.Locale.US, "%s,%s,%s,%s,%s,%d,%s,%d,%s,%s,%d,%s,%.3f,%d,%d,%s,%d,%s,%s,%s,%s",
                runId, rq, algo, variant, dataset, n, params, seed, sha, genVer, repeat,
                elapsed, buildMs, heapBefore, heapPeak, opsCount, outputCheck, status, xmx, machineId, gitCommit);

        // In ra màn hình
        if (!csvOnly) {
            System.out.println("--- RESULT (CSV) ---");
            System.out.println(CSV_HEADER);
        }
        System.out.println(line);

        // Ghi thêm vào file CSV (nếu có --out)
        if (outFile != null) {
            try {
                File f = new File(outFile);
                boolean needHeader = !f.exists() || f.length() == 0;
                try (PrintWriter pw = new PrintWriter(new FileWriter(f, true))) {
                    if (needHeader) pw.println(CSV_HEADER);
                    pw.println(line);
                }
            } catch (IOException e) {
                System.err.println("Cannot write " + outFile + ": " + e.getMessage());
            }
        }
    }
}