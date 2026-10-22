package hashtable.bench;

import hashtable.core.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Khung Runner CLI - Nhiệm vụ C1 (Cặp 1).
 * Parse tham số dòng lệnh, điều phối Generator + HashTable, xuất CSV chuẩn IEEE.
 */
public class BenchmarkRunner {

    public static void main(String[] args) {
        if (hasFlag(args, "--header")) {
            System.out.println("run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit");
            return;
        }

        // 1. Parse tham số từ command line
        String rq = getArg(args, "--rq", "RQ1");
        String algo = getArg(args, "--algo", "sc");
        int m = Integer.parseInt(getArg(args, "--m", "262144"));
        double alpha = Double.parseDouble(getArg(args, "--alpha", "0.75"));
        long seed = Long.parseLong(getArg(args, "--seed", "42"));
        String mode = getArg(args, "--mode", "time");
        String dataset = getArg(args, "--dataset", "uniform");
        int c = Integer.parseInt(getArg(args, "--c", "0"));
        double tau = Double.parseDouble(getArg(args, "--tau", "Infinity"));
        int repeat = Integer.parseInt(getArg(args, "--repeat", "1"));
        String queryType = getArg(args, "--query", "hit");
        String xmx = getArg(args, "--xmx", "2g");
        String machineId = getArg(args, "--machine-id", "PC_LAB_01");
        String gitCommit = getArg(args, "--git-commit", "8b19d6d");

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

        // 3. Sinh dữ liệu và nạp vào bảng
        int n = (int) Math.round(alpha * m);
        long[] presentKeys;
        if (c > 0) {
            // Workload xóa - thêm liên tục (Churn) cho RQ2 & RQ3
            RQ2Workload workload = new RQ2Workload(m, alpha, seed);
            workload.populateInitialKeys(table);
            workload.runChurnCycles(table, c);
            presentKeys = workload.getAliveKeysCopy();
            n = presentKeys.length;
        } else {
            // Workload RQ1 tiêu chuẩn
            if ("clustered".equals(dataset)) {
                presentKeys = RQ1DataGenerator.generateClusteredKeys(n, m, seed);
            } else {
                presentKeys = RQ1DataGenerator.generatePresentKeys(n, seed);
            }
            // Nạp toàn bộ khóa vào bảng trước khi đo
            for (long k : presentKeys) {
                table.put(k, 1);
            }
        }

        // Tạo tập khóa truy vấn theo queryType
        long[] queryKeys;
        if ("miss".equalsIgnoreCase(queryType) || "absent".equalsIgnoreCase(queryType)) {
            queryKeys = RQ1DataGenerator.generateAbsentKeys(presentKeys.length, seed + 100_000L + repeat * 7L, presentKeys);
        } else if ("mixed".equalsIgnoreCase(queryType)) {
            int half = presentKeys.length / 2;
            long[] absent = RQ1DataGenerator.generateAbsentKeys(presentKeys.length - half, seed + 100_000L + repeat * 7L, presentKeys);
            queryKeys = new long[presentKeys.length];
            System.arraycopy(presentKeys, 0, queryKeys, 0, half);
            System.arraycopy(absent, 0, queryKeys, half, absent.length);
        } else {
            queryKeys = presentKeys;
        }

        // 4. Đo đạc theo chế độ time hoặc count
        long ops = queryKeys.length;
        String params = ("hit".equalsIgnoreCase(queryType))
                ? String.format(java.util.Locale.US, "M=%d;alpha=%.2f;c=%d;tau=%.2f", m, alpha, c, tau)
                : String.format(java.util.Locale.US, "M=%d;alpha=%.2f;c=%d;tau=%.2f;query=%s", m, alpha, c, tau, queryType);

        String runId = String.format("RUN_%s_%s_%d", rq, algo.toUpperCase().replace('-', '_'), repeat);
        String sha = computeSha256(presentKeys);

        if ("count".equals(mode)) {
            // Lượt đếm bước dò
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

            printCsvRow(runId, rq, csvAlgo, variant, dataset, n, params, seed,
                    sha, "1.0.0", repeat,
                    "", heapBefore(), heapPeak(), avgProbesStr,
                    checkSum, "ok", xmx, machineId, gitCommit);
        } else {
            // Warmup JIT trước khi bấm giờ: chạy thử 1000 lượt tra cứu ngoài vùng đo
            int warmupLen = Math.min(1000, queryKeys.length);
            for (int w = 0; w < warmupLen; w++) {
                table.get(queryKeys[w]);
            }

            // Lượt đo thời gian
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

            printCsvRow(runId, rq, csvAlgo, variant, dataset, n, params, seed,
                    sha, "1.0.0", repeat,
                    elapsedMsStr, heapBefore(), heapPeak(), "",
                    checkSum, "ok", xmx, machineId, gitCommit);
        }
    }

    /** Kiểm tra xem flag có tồn tại trong args không */
    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) return true;
        }
        return false;
    }

    /** Lấy giá trị tham số từ args, trả về default nếu không tìm thấy */
    private static String getArg(String[] args, String flag, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) return args[i + 1];
        }
        return def;
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

    /** In một dòng CSV đúng schema 20 cột */
    private static void printCsvRow(String runId, String rq, String algo, String variant, String dataset,
                                    int n, String params, long seed,
                                    String sha, String genVer, int repeat, Object elapsed,
                                    long heapBefore, long heapPeak, Object opsCount,
                                    long outputCheck, String status, String xmx,
                                    String machineId, String gitCommit) {
        System.out.printf("%s,%s,%s,%s,%s,%d,%s,%d,%s,%s,%d,%s,%d,%d,%s,%d,%s,%s,%s,%s%n",
                runId, rq, algo, variant, dataset, n, params, seed, sha, genVer, repeat,
                elapsed, heapBefore, heapPeak, opsCount, outputCheck, status, xmx, machineId, gitCommit);
    }
}