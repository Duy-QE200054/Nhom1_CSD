package hashtable.bench;

import hashtable.core.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Khung Runner CLI - Nhiệm vụ C1 (Cặp 1).
 * Parse tham số dòng lệnh, điều phối Generator + HashTable, xuất CSV chuẩn IEEE.
 *
 * Tham số hiển thị (mới):
 *   --csv-only        chỉ in đúng 1 dòng CSV (dùng khi chạy script gom kết quả)
 *   --out <file>      ghi thêm dòng kết quả vào file CSV (tự thêm header nếu file mới)
 */
public class BenchmarkRunner {

    private static final String CSV_HEADER =
            "run_id,rq,algo,variant,dataset,n,params,seed,sha256,gen_version,repeat,"
          + "elapsed_ms,heap_before,heap_peak,ops_count,output_check,status,xmx,machine_id,git_commit";

    private static boolean csvOnly = false;
    private static String outFile = null;

    public static void main(String[] args) {
        // 1. Parse tham số từ command line
        csvOnly = hasFlag(args, "--csv-only");
        outFile = getArg(args, "--out", null);

        String rq = getArg(args, "--rq", "RQ1");
        String algo = getArg(args, "--algo", "sc");
        int m = Integer.parseInt(getArg(args, "--m", "262144"));
        double alpha = Double.parseDouble(getArg(args, "--alpha", "0.75"));
        long seed = Long.parseLong(getArg(args, "--seed", "42"));
        String mode = getArg(args, "--mode", "time");
        String dataset = getArg(args, "--dataset", "uniform");
        int c = Integer.parseInt(getArg(args, "--c", "0"));
        double tau = Double.parseDouble(getArg(args, "--tau", "Infinity"));

        // 2. Khởi tạo bảng băm theo thuật toán (chỉ dùng interface HashTable)
        HashTable table;
        String variant = "base";
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
            System.out.println("[START] algo=" + algo + ", mode=" + mode + ", dataset=" + dataset
                    + ", M=" + m + ", alpha=" + alpha + ", seed=" + seed);
        }

        // 3. Sinh dữ liệu và nạp vào bảng
        int n = (int) Math.round(alpha * m);
        long[] keys;
        if (c > 0) {
            // Workload xóa - thêm liên tục (Churn) cho RQ2 & RQ3
            RQ2Workload workload = new RQ2Workload(m, alpha, seed);
            workload.populateInitialKeys(table);
            workload.runChurnCycles(table, c);
            keys = workload.getAliveKeysCopy();
            n = keys.length;
        } else {
            // Workload RQ1 tiêu chuẩn
            if ("clustered".equals(dataset)) {
                keys = RQ1DataGenerator.generateClusteredKeys(n, m, seed);
            } else {
                keys = RQ1DataGenerator.generatePresentKeys(n, seed);
            }
            // Warmup JVM (chèn toàn bộ khóa trước khi đo)
            for (long k : keys) {
                table.put(k, 1);
            }
        }

        // 4. Đo đạc theo chế độ time hoặc count
        long ops = keys.length;
        if ("count".equals(mode)) {
            // Lượt đếm bước dò
            table.enableProbeCounting(true);
            table.resetProbeCount();
            for (long k : keys) {
                table.get(k);
            }
            long totalProbes = table.getProbeCount();
            double avgProbes = ops > 0 ? (double) totalProbes / ops : 0.0;

            printCsvRow(rq, csvAlgo, variant, dataset, n, m, alpha, c, tau, seed,
                    computeSha256(keys), "1.0.0", 1,
                    "", heapBefore(), heapPeak(), avgProbes,
                    outputCheck(table, keys), "ok", "2g", "PC_LAB", "local");
        } else {
            // Lượt đo thời gian
            long startNs = System.nanoTime();
            for (long k : keys) {
                table.get(k);
            }
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

            printCsvRow(rq, csvAlgo, variant, dataset, n, m, alpha, c, tau, seed,
                    computeSha256(keys), "1.0.0", 1,
                    elapsedMs, heapBefore(), heapPeak(), "",
                    outputCheck(table, keys), "ok", "2g", "PC_LAB", "local");
        }

        if (!csvOnly) {
            System.out.println("[DONE] Benchmark finished successfully (status=ok)"
                    + (outFile != null ? ", saved to " + outFile : ""));
        }
    }

    /** Lấy giá trị tham số từ args, trả về default nếu không tìm thấy */
    private static String getArg(String[] args, String flag, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) return args[i + 1];
        }
        return def;
    }

    /** Kiểm tra cờ không có giá trị, ví dụ --csv-only */
    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) {
            if (flag.equals(a)) return true;
        }
        return false;
    }

    /** Ước lượng heap sử dụng sau GC */
    private static long heapBefore() {
        System.gc();
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /** Đỉnh heap hiện tại */
    private static long heapPeak() {
        return Runtime.getRuntime().totalMemory();
    }

    /** Output checksum: tổng giá trị found + số lần miss (chống JIT DCE) */
    private static long outputCheck(HashTable t, long[] keys) {
        long sum = 0;
        for (long k : keys) {
            GetResult r = t.get(k);
            if (r.isFound()) sum += r.getValue();
            else sum++;
        }
        return sum;
    }

    /** SHA-256 Big-Endian của mảng khóa */
    private static String computeSha256(long[] keys) {
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

    /** In một dòng CSV đúng schema 20 cột (kèm header/ghi file tùy tham số) */
    private static void printCsvRow(String rq, String algo, String variant, String dataset,
                                    int n, int m, double alpha, int c, double tau, long seed,
                                    String sha, String genVer, int repeat, Object elapsed,
                                    long heapBefore, long heapPeak, Object opsCount,
                                    long outputCheck, String status, String xmx,
                                    String machineId, String gitCommit) {
        String params = String.format("M=%d;alpha=%.2f;c=%d;tau=%.2f", m, alpha, c, tau);
        String runId = "RUN_" + rq + "_" + algo.toUpperCase() + "_" + repeat;
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