package hashtable.bench;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper CLI - Nhiệm vụ Cặp 1 (C1): chạy TỪNG mốc benchmark trong một tiến trình JVM riêng,
 * có timeout (mặc định 60 giây). Quá giờ -> kill tiến trình và ghi dòng status=timeout vào CSV.
 * Tiến trình con thoát lỗi (OOM, exception) -> ghi dòng status=error.
 *
 * Chạy TUẦN TỰ (không song song) để các phép đo không tranh CPU/RAM của nhau.
 *
 * Cách dùng:
 *   java -cp out hashtable.bench.BenchmarkWrapper --jobs jobs.txt --out results.csv [--timeout 60] [--xmx 2g]
 *
 * jobs.txt: mỗi dòng là tham số của BenchmarkRunner cho MỘT mốc (dòng trống và dòng bắt đầu bằng # bị bỏ qua):
 *   --rq RQ1 --algo sc --m 262144 --alpha 0.75 --mode time --repeat 1
 *   --rq RQ1 --algo lp-tomb --m 262144 --alpha 0.90 --mode count --repeat 1
 *
 * Tùy chọn: --cp <classpath> (mặc định: classpath của wrapper), --java <đường dẫn java>.
 * Wrapper tự thêm --out, --csv-only, --xmx vào mỗi job nếu job chưa có.
 */
public class BenchmarkWrapper {

    public static void main(String[] args) throws Exception {
        String jobsFile = BenchmarkRunner.getArg(args, "--jobs", null);
        String out = BenchmarkRunner.getArg(args, "--out", "results.csv");
        long timeoutSec = Long.parseLong(BenchmarkRunner.getArg(args, "--timeout", "60"));
        String xmx = BenchmarkRunner.getArg(args, "--xmx", "2g");
        String cp = BenchmarkRunner.getArg(args, "--cp", System.getProperty("java.class.path"));
        String javaBin = BenchmarkRunner.getArg(args, "--java",
                System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");

        if (jobsFile == null) {
            System.err.println("Thiếu --jobs. Xem hướng dẫn trong Javadoc của BenchmarkWrapper.");
            System.exit(2);
        }

        List<String> jobs = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(jobsFile), StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#")) jobs.add(t);
        }

        int ok = 0, timeouts = 0, errors = 0;
        for (int i = 0; i < jobs.size(); i++) {
            String[] jobArgs = jobs.get(i).split("\\s+");
            String status = runJob(javaBin, cp, jobArgs, out, xmx, timeoutSec, i + 1, jobs.size());
            if ("ok".equals(status)) ok++;
            else if ("timeout".equals(status)) timeouts++;
            else errors++;
        }
        System.out.println("=== Xong " + jobs.size() + " mốc: ok=" + ok + ", timeout=" + timeouts
                + ", error=" + errors + " -> " + out);
    }

    /** Chạy một mốc trong tiến trình riêng; trả về "ok" | "timeout" | "error". */
    static String runJob(String javaBin, String cp, String[] jobArgs, String out, String xmx,
                         long timeoutSec, int index, int total) throws Exception {
        List<String> runnerArgs = new ArrayList<>(Arrays.asList(jobArgs));
        if (!BenchmarkRunner.hasFlag(jobArgs, "--out")) { runnerArgs.add("--out"); runnerArgs.add(out); }
        if (!BenchmarkRunner.hasFlag(jobArgs, "--csv-only")) runnerArgs.add("--csv-only");
        if (!BenchmarkRunner.hasFlag(jobArgs, "--xmx")) { runnerArgs.add("--xmx"); runnerArgs.add(xmx); }
        String[] effective = runnerArgs.toArray(new String[0]);
        String csvFile = BenchmarkRunner.getArg(effective, "--out", out);
        String effXmx = BenchmarkRunner.getArg(effective, "--xmx", xmx);

        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.add("-Xmx" + effXmx);
        cmd.add("-cp");
        cmd.add(cp);
        cmd.add("hashtable.bench.BenchmarkRunner");
        cmd.addAll(runnerArgs);

        System.out.print("[" + index + "/" + total + "] " + String.join(" ", jobArgs) + " ... ");
        System.out.flush();

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Runner đã tự ghi CSV vào --out; bỏ stdout để không lẫn lộn, giữ stderr để thấy lỗi.
        pb.redirectOutput(ProcessBuilder.Redirect.to(new File(
                System.getProperty("os.name").toLowerCase().contains("win") ? "NUL" : "/dev/null")));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        long t0 = System.nanoTime();
        Process p = pb.start();
        boolean finished = p.waitFor(timeoutSec, TimeUnit.SECONDS);
        double secs = (System.nanoTime() - t0) / 1e9;

        if (!finished) {
            p.destroyForcibly();
            p.waitFor(10, TimeUnit.SECONDS);
            BenchmarkRunner.appendCsvLine(csvFile, BenchmarkRunner.failureRow(effective, "timeout"));
            System.out.println(String.format(java.util.Locale.ROOT, "TIMEOUT (>%ds)", timeoutSec));
            return "timeout";
        }
        if (p.exitValue() != 0) {
            BenchmarkRunner.appendCsvLine(csvFile, BenchmarkRunner.failureRow(effective, "error"));
            System.out.println("ERROR (exit=" + p.exitValue() + ")");
            return "error";
        }
        System.out.println(String.format(java.util.Locale.ROOT, "ok (%.1fs)", secs));
        return "ok";
    }
}