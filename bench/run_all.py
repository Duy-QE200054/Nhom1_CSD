"""
Bench Runner - Điều phối các tiến trình đo thực nghiệm cho RQ1, RQ2, RQ3
Theo quy chuẩn Đề tài 2 - CSD201 FA26:
- Khởi động mỗi lượt đo bằng một tiến trình JVM riêng biệt (ProcessBuilder / subprocess).
- Giới hạn thời gian (Timeout): 60 giây. Nếu vượt quá, ép dừng (destroy/kill) và ghi nhận trạng thái 'timeout' vào CSV.
- Quản lý hạt giống (3 seed: 42, 1337, 20261019), số lượt lặp (repeat = 1..3).
- Ghi nhận đầy đủ 20 trường thông tin theo chuẩn Bảng 4 và bench/csv_schema.md.
"""

import os
import sys
import subprocess
import time
import argparse

# Đảm bảo UTF-8 trên Windows console
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

CSV_HEADER = "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,generator_version,repeat,elapsed_ms,heap_before_bytes,heap_peak_bytes,ops_count,output_check,status,xmx,machine_id,git_commit"

def get_default_jar_path():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, ".."))
    return os.path.join(root_dir, "HashTableProject", "dist", "HashTableProject.jar")

def get_default_results_path():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, ".."))
    return os.path.join(root_dir, "results", "raw", "rq3_results.csv")

def ensure_csv_header(file_path):
    os.makedirs(os.path.dirname(file_path), exist_ok=True)
    if not os.path.exists(file_path) or os.path.getsize(file_path) == 0:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(CSV_HEADER + "\n")

def run_single_benchmark(jar_path, args, timeout_sec=60):
    cmd = ["java", "-Xmx2g", "-jar", jar_path] + args
    print(f"[RUNNING] {' '.join(cmd)}")
    try:
        proc = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            timeout=timeout_sec
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

def run_rq3_benchmarks(jar_path, output_csv, quick=False, timeout_sec=60, custom_seeds=None, custom_cycles=None, repeats_count=None):
    """
    Kịch bản đo thực nghiệm RQ3 (Nhiệm vụ Cặp 3 - C3):
    - Đánh giá bóc tách cơ chế của Robin Hood Hashing:
      1) Bỏ tombstone (Backward-shift deletion): lp-backshift vs lp-tomb.
      2) Hoán đổi theo DIB (DIB swapping): rh-noearly vs lp-backshift.
      3) Cơ chế dừng sớm (Early exit): rh vs rh-noearly (đặc biệt rõ ở truy vấn miss).
    - Cấu hình:
      M = 262144 (2^18), alpha = 0.50 (N = 131072)
      C in [65536, 131072, 262144] (M/4, M/2, M)
    """
    print("\n========================================================")
    print("      RQ3 BENCHMARK SUITE - ROBIN HOOD MECHANISM       ")
    print("========================================================")

    ensure_csv_header(output_csv)

    m = 262144
    alpha = 0.50
    algos = ["rh", "rh-noearly", "lp-backshift"]

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

    print(f"Tổng số cấu hình lượt chạy cần đo: {total_configs}\n")

    for c in churn_cycles:
        for seed in seeds:
            for rep in repeats:
                for algo in algos:
                    for query in queries:
                        # 1. Lượt đo số bước dò (ops_count)
                        current += 1
                        print(f"[{current}/{total_configs}] Count probe: algo={algo}, c={c}, seed={seed}, rep={rep}, query={query}")
                        args_count = [
                            "--rq", "RQ3",
                            "--algo", algo,
                            "--m", str(m),
                            "--alpha", str(alpha),
                            "--c", str(c),
                            "--seed", str(seed),
                            "--repeat", str(rep),
                            "--query", query,
                            "--mode", "count"
                        ]
                        code, out = run_single_benchmark(jar_path, args_count, timeout_sec=timeout_sec)
                        if code == 0 and out:
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(out + "\n")
                            success_count += 1
                        elif code == -1:
                            timeout_count += 1
                            # Ghi dòng timeout đúng chuẩn CSV
                            variant = "noearly" if algo == "rh-noearly" else "base"
                            csv_algo = "rh" if "rh" in algo else algo
                            params = f"M={m};alpha={alpha:.2f};c={c};tau=Infinity;query={query}"
                            run_id = f"RUN_RQ3_{algo.upper().replace('-', '_')}_{rep}"
                            timeout_row = f"{run_id},RQ3,{csv_algo},{variant},uniform,{int(m*alpha)},{params},{seed},,1.0.0,{rep},,,,1.0000,,timeout,2g,PC_LAB_01,8b19d6d"
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
                            "--seed", str(seed),
                            "--repeat", str(rep),
                            "--query", query,
                            "--mode", "time"
                        ]
                        code, out = run_single_benchmark(jar_path, args_time, timeout_sec=timeout_sec)
                        if code == 0 and out:
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(out + "\n")
                            success_count += 1
                        elif code == -1:
                            timeout_count += 1
                            variant = "noearly" if algo == "rh-noearly" else "base"
                            csv_algo = "rh" if "rh" in algo else algo
                            params = f"M={m};alpha={alpha:.2f};c={c};tau=Infinity;query={query}"
                            run_id = f"RUN_RQ3_{algo.upper().replace('-', '_')}_{rep}"
                            timeout_row = f"{run_id},RQ3,{csv_algo},{variant},uniform,{int(m*alpha)},{params},{seed},,1.0.0,{rep},,,,,timeout,2g,PC_LAB_01,8b19d6d"
                            with open(output_csv, "a", encoding="utf-8") as f:
                                f.write(timeout_row + "\n")

    print("\n--------------------------------------------------------")
    print(f"ĐO ĐẠC HOÀN THÀNH: Thành công: {success_count}, Timeout: {timeout_count}")
    print(f"Kết quả CSV đã lưu tại: {output_csv}")
    print("--------------------------------------------------------\n")

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

    args = parser.parse_args()

    jar_path = args.jar or get_default_jar_path()
    output_path = args.output or get_default_results_path()

    custom_seeds = [int(s.strip()) for s in args.seeds.split(",")] if args.seeds else None
    custom_cycles = [int(c.strip()) for c in args.cycles.split(",")] if args.cycles else None

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
            repeats_count=args.repeats
        )
    else:
        print(f"Nhiệm vụ của Cặp 3 (C3) tập trung vào RQ3. Hãy sử dụng cờ '--rq RQ3'.")
