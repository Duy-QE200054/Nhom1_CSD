"""
Bench Runner - Điều phối các tiến trình đo thực nghiệm cho RQ1, RQ2, RQ3
Theo quy chuẩn:
- Khởi động mỗi lượt đo bằng một tiến trình JVM riêng biệt (ProcessBuilder / subprocess).
- Giới hạn thời gian (Timeout): 60 giây. Nếu vượt quá, ép dừng (destroy/kill) và ghi nhận trạng thái 'timeout' vào CSV.
- Quản lý hạt giống (3 seed), số lượt lặp (repeat = 1..3).
"""

import os
import sys
import subprocess
import time

def run_single_benchmark(jar_path, args, timeout_sec=60):
    cmd = ["java", "-Xmx2g", "-jar", jar_path] + args
    print(f"[RUNNING] {' '.join(cmd)}")
    try:
        proc = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=timeout_sec)
        print(f"[COMPLETED] Return code: {proc.returncode}")
        if proc.stdout:
            print(proc.stdout.strip())
        return proc.returncode, proc.stdout
    except subprocess.TimeoutExpired:
        print(f"[TIMEOUT] Process exceeded {timeout_sec}s!")
        return -1, "timeout"

if __name__ == "__main__":
    print("=== HashTable Benchmark Suite Runner ===")
    print("Sử dụng script này để kích hoạt chạy toàn bộ các mốc đo theo ma trận thực nghiệm.")
