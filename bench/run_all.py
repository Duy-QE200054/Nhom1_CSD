"""
Bench Runner - Điều phối các tiến trình đo thực nghiệm cho RQ1, RQ2, RQ3
Theo quy chuẩn Đề tài 2 - CSD201 FA26:
- Khởi động mỗi lượt đo bằng một tiến trình JVM riêng biệt (ProcessBuilder / subprocess).
- Giới hạn thời gian (Timeout): 60 giây. Nếu vượt quá, ép dừng (destroy/kill) và ghi nhận trạng thái 'timeout' vào CSV.
- Quản lý hạt giống (3 seed: 42, 1337, 20261019), số lượt lặp (repeat = 1..3).
- Ghi nhận đầy đủ 21 trường thông tin theo chuẩn Bảng 4 và bench/csv_schema.md.
"""

import os
import sys
import subprocess
import time
import argparse
import math

# Đảm bảo UTF-8 trên Windows console
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

CSV_HEADER = "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,build_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit"

def get_default_jar_path():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, ".."))
    return os.path.join(root_dir, "HashTableProject", "dist", "HashTableProject.jar")

def get_default_results_path():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, ".."))
    return os.path.join(root_dir, "results", "raw", "rq3_results.csv")

def get_repository_root():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    return os.path.abspath(os.path.join(script_dir, ".."))

def ensure_csv_header(file_path):
    os.makedirs(os.path.dirname(file_path), exist_ok=True)
    if not os.path.exists(file_path) or os.path.getsize(file_path) == 0:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(CSV_HEADER + "\n")
        return
    with open(file_path, "r", encoding="utf-8-sig") as f:
        existing_header = f.readline().strip()
    if existing_header != CSV_HEADER:
        raise ValueError(
            f"CSV schema mismatch in {file_path}; refusing to append "
            "benchmark rows with a different schema."
        )

def run_single_benchmark(jar_path, args, timeout_sec=60):
    repository_root = get_repository_root()
    jar_argument = jar_path
    try:
        if os.path.commonpath([repository_root, os.path.abspath(jar_path)]) == repository_root:
            jar_argument = os.path.relpath(jar_path, repository_root)
    except ValueError:
        pass
    cmd = ["java", "-Xmx2g", "-jar", jar_argument] + args
    print(f"[RUNNING] {' '.join(cmd)}")
    try:
        proc = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=timeout_sec,
            cwd=repository_root
        )
        if proc.returncode == 0:
            lines = [line.strip() for line in proc.stdout.strip().splitlines() if line.strip()]
            csv_lines = [l for l in lines if not l.startswith("run_id,") and l.count(",") >= 15]
            if csv_lines:
                result_row = csv_lines[-1]
                print(f"[COMPLETED] {result_row}")
                return 0, result_row
            return 0, proc.stdout.strip()
        else:
            print(f"[ERROR] Return code {proc.returncode}: {proc.stderr.strip()}")
            return proc.returncode, None
    except subprocess.TimeoutExpired:
        print(f"[TIMEOUT] Process exceeded {timeout_sec}s! Destroying JVM process.")
        return -1, "timeout"

def run_rq3_benchmarks(
    jar_path,
    output_csv,
    quick=False,
    timeout_sec=60,
    custom_seeds=None,
    custom_cycles=None,
    repeats_count=None,
    table_capacity=262144,
    algorithms=None,
    tombstone_tau=float("inf"),
    git_commit="local",
):
    """
    Kịch bản đo thực nghiệm RQ3 (Nhiệm vụ Cặp 3 - C3):
    - Đánh giá bóc tách cơ chế của Robin Hood Hashing:
      1) Bỏ tombstone (Backward-shift deletion): lp-backshift vs lp-tomb.
      2) Hoán đổi theo DIB (DIB swapping): rh-noearly vs lp-backshift.
      3) Cơ chế dừng sớm (Early exit): rh vs rh-noearly (đặc biệt rõ ở truy vấn miss).
    - Cấu hình:
      M = table_capacity, alpha = 0.50
      C in [65536, 131072, 262144] (M/4, M/2, M)
    """
    print("\n========================================================")
    print("      RQ3 BENCHMARK SUITE - ROBIN HOOD MECHANISM       ")
    print("========================================================")

    ensure_csv_header(output_csv)

    m = table_capacity
    alpha = 0.50
    algos = algorithms or ["rh", "rh-noearly", "lp-backshift"]

    if quick:
        print("[MODE] Quick Trial Mode (1 seed, 1 repeat, C=65536)")
        seeds = custom_seeds or [42]
        churn_cycles = custom_cycles or [65536]
        repeats = [1]
        queries = ["hit", "miss"]
    else:
        seeds = custom_seeds or [42, 1337, 20261019]
        churn_cycles = custom_cycles or [65536, 131072, 262144]
        rep_count = repeats_count or 3
        repeats = list(range(1, rep_count + 1))
        queries = ["hit", "miss"]
        print(f"[MODE] Matrix Mode ({len(seeds)} seeds, {len(repeats)} repeats, {len(churn_cycles)} churn cycles)")

    total_configs = len(churn_cycles) * len(seeds) * len(repeats) * len(algos) * 2 * len(queries)
    current = 0
    success_count = 0
    timeout_count = 0
    failure_count = 0

    print(f"Tổng số cấu hình lượt chạy cần đo: {total_configs}\n")

    for c in churn_cycles:
        for seed in seeds:
            for rep in repeats:
                for algo in algos:
                    for query in queries:
                        tau = tombstone_tau if algo == "lp-tomb" else float("inf")
                        tau_arg = "Infinity" if tau == float("inf") else str(tau)
                        # 1. Lượt đo số bước dò (ops_count)
                        current += 1
                        print(f"[{current}/{total_configs}] Count probe: algo={algo}, c={c}, seed={seed}, rep={rep}, query={query}")
                        args_count = [
                            "--rq", "RQ3",
                            "--algo", algo,
                            "--m", str(m),
                            "--alpha", str(alpha),
                            "--c", str(c),
                            "--tau", tau_arg,
                            "--seed", str(seed),
                            "--repeat", str(rep),
                            "--git-commit", git_commit,
                            "--query", query,
                            "--mode", "count"
                        ]
                        code, out = run_single_benchmark(jar_path, args_count, timeout_sec=timeout_sec)
                        if code == 0:
                            out = identify_result(out or "", algo, c, seed, rep, query, "count")
                            if out is None:
                                raise ValueError("Benchmark returned no row or an invalid CSV schema.")
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(out + "\n")
                            success_count += 1
                        elif code == -1:
                            timeout_count += 1
                            timeout_row = failure_row(
                                algo, m, alpha, c, tau, seed, rep, query, "count", "timeout", git_commit
                            )
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(timeout_row + "\n")
                        elif code != 0:
                            failure_count += 1
                            timeout_row = failure_row(
                                algo, m, alpha, c, tau, seed, rep, query, "count", "crash", git_commit
                            )
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(timeout_row + "\n")

                        # 2. Lượt đo thời gian (elapsed_ms)
                        current += 1
                        print(f"[{current}/{total_configs}] Time elapsed: algo={algo}, c={c}, seed={seed}, rep={rep}, query={query}")
                        args_time = [
                            "--rq", "RQ3",
                            "--algo", algo,
                            "--m", str(m),
                            "--alpha", str(alpha),
                            "--c", str(c),
                            "--tau", tau_arg,
                            "--seed", str(seed),
                            "--repeat", str(rep),
                            "--git-commit", git_commit,
                            "--query", query,
                            "--mode", "time"
                        ]
                        code, out = run_single_benchmark(jar_path, args_time, timeout_sec=timeout_sec)
                        if code == 0:
                            out = identify_result(out or "", algo, c, seed, rep, query, "time")
                            if out is None:
                                raise ValueError("Benchmark returned no row or an invalid CSV schema.")
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(out + "\n")
                            success_count += 1
                        elif code == -1:
                            timeout_count += 1
                            timeout_row = failure_row(
                                algo, m, alpha, c, tau, seed, rep, query, "time", "timeout", git_commit
                            )
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(timeout_row + "\n")
                        elif code != 0:
                            failure_count += 1
                            timeout_row = failure_row(
                                algo, m, alpha, c, tau, seed, rep, query, "time", "crash", git_commit
                            )
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(timeout_row + "\n")

    print("\n--------------------------------------------------------")
    print(f"ĐO ĐẠC HOÀN THÀNH: Thành công: {success_count}, Timeout: {timeout_count}")
    print(f"Lượt lỗi khác: {failure_count}")
    print(f"Kết quả CSV đã lưu tại: {output_csv}")
    print("--------------------------------------------------------\n")


def identify_result(output, algo, cycles, seed, repeat, query, mode):
    """Validate a Java CSV row and give each matrix run a stable unique ID."""
    rows = [line.strip() for line in output.splitlines() if line.strip()]
    if not rows:
        return None
    fields = rows[-1].split(",")
    if len(fields) != len(CSV_HEADER.split(",")):
        return None
    fields[0] = (
        f"RUN_RQ3_{algo.upper().replace('-', '_')}_C{cycles}_S{seed}_"
        f"R{repeat}_{query}_{mode}"
    )
    return ",".join(fields)


def failure_row(algo, m, alpha, cycles, tau, seed, repeat, query, mode, status, git_commit):
    """Create a schema-valid failure record without claiming a result."""
    csv_algo = "rh" if algo == "rh-noearly" else algo
    variant = "noearly" if algo == "rh-noearly" else "base"
    tau_value = "Infinity" if tau == float("inf") else f"{tau:g}"
    params = f"M={m};alpha={alpha:.2f};c={cycles};tau={tau_value};query={query}"
    run_id = (
        f"RUN_RQ3_{algo.upper().replace('-', '_')}_C{cycles}_S{seed}_"
        f"R{repeat}_{query}_{mode}"
    )
    values = [
        run_id, "RQ3", csv_algo, variant, "uniform", str(round(alpha * m)),
        params, str(seed), "", "1.0.0", str(repeat), "", "", "", "",
        "", "", status, "2g", "PC_LAB_01", git_commit,
    ]
    return ",".join(values)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="HashTable Benchmark Suite Runner (CSD201 FA26)")
    parser.add_argument("--rq", type=str, default="RQ3", choices=["RQ1", "RQ2", "RQ3", "ALL"],
                        help="Câu hỏi nghiên cứu cần chạy đo (mặc định: RQ3 cho Cặp 3)")
    parser.add_argument("--quick", action="store_true", help="Chạy thử nghiệm nhanh (1 seed, 1 repeat)")
    parser.add_argument("--jar", type=str, default=None, help="Đường dẫn file jar")
    parser.add_argument("--output", type=str, default=None, help="Đường dẫn file CSV kết quả xuất ra")
    parser.add_argument("--timeout", type=int, default=60, help="Giới hạn thời gian (giây) cho mỗi tiến trình JVM")

    parser.add_argument("--seeds", type=str, default=None, help="Danh sách seeds phân cách bởi dấu phẩy, vd: 42,1337,20261019")
    parser.add_argument("--cycles", type=str, default=None, help="Danh sách chu kỳ churn phân cách bởi dấu phẩy, vd: 65536,131072,262144")
    parser.add_argument("--repeats", type=int, default=None, help="Số lần lặp (repeat count, mặc định: 3)")
    parser.add_argument("--m", type=int, default=262144, help="Dung lượng bảng M (lũy thừa của 2)")
    parser.add_argument("--algorithms", type=str, default="rh,rh-noearly,lp-backshift",
                        help="Thuật toán phân cách bởi dấu phẩy")
    parser.add_argument("--tau", type=float, default=float("inf"),
                        help="Ngưỡng rebuild cho lp-tomb")
    parser.add_argument("--git-commit", type=str, default="local",
                        help="Mã commit của mã nguồn benchmark")

    args = parser.parse_args()

    jar_path = args.jar or get_default_jar_path()
    output_path = args.output or get_default_results_path()

    custom_seeds = [int(s.strip()) for s in args.seeds.split(",")] if args.seeds else None
    custom_cycles = [int(c.strip()) for c in args.cycles.split(",")] if args.cycles else None
    algorithms = [algo.strip() for algo in args.algorithms.split(",") if algo.strip()]
    if args.m <= 0 or args.m & (args.m - 1):
        parser.error("--m must be a positive power of two")
    if not algorithms:
        parser.error("--algorithms must contain at least one algorithm")
    supported_algorithms = {"lp-tomb", "lp-backshift", "rh", "rh-noearly"}
    unsupported_algorithms = set(algorithms) - supported_algorithms
    if unsupported_algorithms:
        parser.error(f"unsupported RQ3 algorithms: {', '.join(sorted(unsupported_algorithms))}")
    if not (0 < args.tau <= 1 or (math.isinf(args.tau) and args.tau > 0)):
        parser.error("--tau must be in (0, 1] or Infinity")

    if not os.path.exists(jar_path):
        print(f"[ERROR] Không tìm thấy file jar tại: {jar_path}")
        print("Hãy chạy lệnh 'ant jar' hoặc biên dịch project trong NetBeans trước.")
        sys.exit(1)

    if args.rq == "RQ3" or args.rq == "ALL":
        run_rq3_benchmarks(
            jar_path, output_path,
            quick=args.quick,
            timeout_sec=args.timeout,
            custom_seeds=custom_seeds,
            custom_cycles=custom_cycles,
            repeats_count=args.repeats,
            table_capacity=args.m,
            algorithms=algorithms,
            tombstone_tau=args.tau,
            git_commit=args.git_commit,
        )
    else:
        print(f"Nhiệm vụ của Cặp 3 (C3) tập trung vào RQ3. Hãy sử dụng cờ '--rq RQ3'.")
