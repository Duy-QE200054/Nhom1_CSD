package hashtable.bench;

import hashtable.core.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Khung Runner CLI - Nhiệm vụ Cặp 1 (C1) & Cặp 2 (C2).
 * Parse tham số dòng lệnh, điều phối Generator + HashTable, xuất CSV chuẩn IEEE theo csv_schema.md.
 *
 * Hỗ trợ:
 * - Chuẩn hóa 20 cột CSV theo bench/csv_schema.md.
 * - RQ1: Separate Chaining vs Linear Probing (uniform & clustered).
 * - RQ2: Linear Probing Tombstone (tích tụ tombstone) vs Clean Table đối chứng,
 *        ghi thời gian từng khối 10^4 thao tác, đo tại mốc C (10^4 có mặt, 10^4 vắng mặt, tỷ lệ TOMB/EMPTY, số lần dò).
 * - RQ3: Robin Hood vs LP Backshift (early-exit ablation).
 */
public class BenchmarkRunner {

    public static final String CSV_HEADER =
            "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,"
          + "elapsed_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit";

    private static boolean csvOnly = false;
    private static String outFile = null;

    public static void main(String[] args) {
        // 1. Parse tham số từ command line
        csvOnly = hasFlag(args, "--csv-only");
        outFile = getArg(args, "--out", null);

        String rq = getArg(args, "--rq", "RQ1");
        String algo = getArg(args, "--algo", "sc");
        String requestedVariant = getArg(args, "--variant", "base");
        int m = Integer.parseInt(getArg(args, "--m", "262144"));
        double alpha = Double.parseDouble(getArg(args, "--alpha", "0.75"));
        long seed = Long.parseLong(getArg(args, "--seed", "42"));
        String mode = getArg(args, "--mode", "time");
        String dataset = getArg(args, "--dataset", "uniform");
        int c = Integer.parseInt(getArg(args, "--c", "0"));
        double tau = Double.parseDouble(getArg(args, "--tau", "Infinity"));
        int repeat = Integer.parseInt(getArg(args, "--repeat", "1"));
        String machineId = getArg(args, "--machine-id", "PC_LAB");
        String gitCommit = getArg(args, "--git-commit", "local");
        int blockSize = Integer.parseInt(getArg(args, "--block-size", "10000"));
        String blockLogFile = getArg(args, "--block-log", null);
        boolean compareClean = hasFlag(args, "--compare-clean");
        boolean rq2Measure = hasFlag(args, "--rq2-measure") || ("RQ2".equals(rq) && !compareClean && c > 0);

        // 2. Khởi tạo bảng băm theo thuật toán
        HashTable table;
        String variant = requestedVariant;
        String csvAlgo = algo;
        switch (algo) {
            case "sc":
                table = new SeparateChaining(m);
                break;
            case "lp-tomb":
                table = new LinearProbingTombstone(m, tau);
                break;
            case "lp-backshift":
                table = new LinearProbingBackshift(m);
                break;
            case "rh":
                table = new RobinHoodTable(m, true);
                break;
            case "rh-noearly":
                table = new RobinHoodTable(m, false);
                variant = "noearly";
                csvAlgo = "rh";
                break;
            default:
                throw new IllegalArgumentException("Unsupported algorithm: " + algo);
        }

        if (!csvOnly) {
            System.out.println("[START] algo=" + algo + ", variant=" + variant + ", mode=" + mode
                    + ", dataset=" + dataset + ", M=" + m + ", alpha=" + alpha + ", c=" + c + ", seed=" + seed);
        }

        // 3. Điều phối Workload theo từng RQ
        int n = (int) Math.round(alpha * m);
        long[] keys;
        RQ2Workload workload = null;

        if (c > 0) {
            // Workload xóa - thêm liên tục (Churn) cho RQ2 & RQ3
            workload = new RQ2Workload(m, alpha, seed);
            workload.populateInitialKeys(table);

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
                table = workload.buildCleanTable((LinearProbingTombstone) table);
                variant = "clean_table";
            }

            keys = workload.getAliveKeysCopy();
            n = keys.length;
        } else {
            // Workload RQ1 tiêu chuẩn
            if ("clustered".equals(dataset)) {
                keys = RQ1DataGenerator.generateClusteredKeys(n, m, seed);
            } else {
                keys = RQ1DataGenerator.generatePresentKeys(n, seed);
            }
            // Nạp toàn bộ khóa trước khi đo
            for (long k : keys) {
                table.put(k, 1);
            }
        }

        // 4. Kiểm tra nếu có cờ --compare-clean (In bảng đối chứng cho RQ2)
        if (compareClean && (table instanceof LinearProbingTombstone) && workload != null) {
            int queryCount = Math.min(10_000, n);
            RQ2Workload.RQ2ComparisonResult comp = workload.compareWithCleanTable((LinearProbingTombstone) table, c, queryCount, seed + 999L);
            comp.printComparisonTable();
            return;
        }

        // 5. Đo đạc theo chế độ count hoặc time
        long ops = keys.length;
        if ("count".equals(mode)) {
            // Lượt đếm bước dò: elapsed_ms để trống, ops_count có giá trị
            table.enableProbeCounting(true);
            table.resetProbeCount();
            for (long k : keys) {
                table.get(k);
            }
            long totalProbes = table.getProbeCount();
            double avgProbes = ops > 0 ? (double) totalProbes / ops : 0.0;

            printCsvRow(rq, csvAlgo, variant, dataset, n, m, alpha, c, tau, seed,
                    computeSha256(keys), "1.0.0", repeat,
                    "", heapBefore(), heapPeak(), avgProbes,
                    outputCheck(table, keys), "ok", "2g", machineId, gitCommit);
        } else {
            // Lượt đo thời gian: elapsed_ms có giá trị, ops_count để trống
            long startNs = System.nanoTime();
            for (long k : keys) {
                table.get(k);
            }
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            printCsvRow(rq, csvAlgo, variant, dataset, n, m, alpha, c, tau, seed,
                    computeSha256(keys), "1.0.0", repeat,
                    elapsedMs, heapBefore(), heapPeak(), "",
                    outputCheck(table, keys), "ok", "2g", machineId, gitCommit);
        }

        if (!csvOnly) {
            System.out.println("[DONE] Benchmark finished successfully (status=ok)"
                    + (outFile != null ? ", saved to " + outFile : ""));
        }
    }

    /** Lấy giá trị tham số từ args, trả về default nếu không tìm thấy */
    public static String getArg(String[] args, String flag, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) return args[i + 1];
        }
        return def;
    }

    /** Kiểm tra cờ không có giá trị, ví dụ --csv-only */
    public static boolean hasFlag(String[] args, String flag) {
        for (String a : args) {
            if (flag.equals(a)) return true;
        }
        return false;
    }

    /** Ước lượng heap sử dụng sau GC */
    public static long heapBefore() {
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /** Đỉnh heap hiện tại */
    public static long heapPeak() {
        return Runtime.getRuntime().totalMemory();
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

    /** In một dòng CSV đúng chuẩn 20 cột của csv_schema.md */
    public static void printCsvRow(String rq, String algo, String variant, String dataset,
                                   int n, int m, double alpha, int c, double tau, long seed,
                                   String sha, String genVer, int repeat, Object elapsed,
                                   long heapBefore, long heapPeak, Object opsCount,
                                   long outputCheck, String status, String xmx,
                                   String machineId, String gitCommit) {
        String tauStr = Double.isInfinite(tau) ? "inf" : String.format("%.2f", tau);
        String params = String.format("M=%d;alpha=%.2f;c=%d;tau=%s", m, alpha, c, tauStr);
        String runId = "RUN_" + rq + "_" + algo.toUpperCase() + "_" + (variant.equals("base") ? "" : variant.toUpperCase() + "_") + repeat;
        String line = String.format("%s,%s,%s,%s,%s,%d,%s,%d,%s,%s,%d,%s,%d,%d,%s,%d,%s,%s,%s,%s",
                runId, rq, algo, variant, dataset, n, params, seed, sha, genVer, repeat,
                elapsed, heapBefore, heapPeak, opsCount, outputCheck, status, xmx, machineId, gitCommit);

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