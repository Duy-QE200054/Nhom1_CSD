"""
Plot Figures - Tái tạo đồ thị từ các file CSV kết quả đo cho bài báo IEEE
Nhiệm vụ Cặp 3 (C3 - SV 6) theo Bảng phân công PHAN_CONG.md.

Yêu cầu theo mục 7.5 của Quy định chung Đề tài 2 CSD201:
- Tạo các hình đồ thị khoa học có mục đích rõ ràng cho bài báo IEEE.
- Đồ thị bóc tách 3 cơ chế của Robin Hood (RQ3):
  1) Bỏ tombstone (Backward-shift deletion)
  2) Hoán đổi theo DIB (DIB swapping / Variance reduction)
  3) Dừng sớm khi tìm kiếm (Early exit trên Miss queries)
- Chú thích mỗi hình ghi rõ file CSV nguồn và script tạo ra nó.
"""

import os
import sys
import csv
import glob
import statistics
from collections import defaultdict
import argparse

# Đảm bảo UTF-8 trên Windows console
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

import matplotlib
matplotlib.use("Agg")  # Chế độ headless cho server/CLI
import matplotlib.pyplot as plt

def get_default_csv_path():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, ".."))
    return os.path.join(root_dir, "results", "raw", "rq3_results.csv")

def get_default_figures_dir():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.abspath(os.path.join(script_dir, ".."))
    return os.path.join(root_dir, "results", "figures")

def parse_params(params_str):
    """Parse chuỗi params M=...;alpha=...;c=...;tau=...;query=... thành dict"""
    res = {}
    for part in params_str.split(";"):
        if "=" in part:
            k, v = part.split("=", 1)
            res[k.strip().lower()] = v.strip().lower()
    return res

def workload_label(rows):
    if not rows:
        return "workload"
    params = parse_params(rows[0].get("params", ""))
    m = int(params.get("m", 0))
    alpha = float(params.get("alpha", 0.50))
    return f"$N={round(m * alpha)}, M={m}$"

def load_data(csv_paths):
    """Đọc dữ liệu từ một hoặc nhiều file CSV chuẩn."""
    if isinstance(csv_paths, str):
        csv_paths = [csv_paths]
    rows = []
    expected_header = None
    for csv_path in csv_paths:
        if not os.path.isfile(csv_path):
            raise FileNotFoundError(f"Không tìm thấy file CSV: {csv_path}")
        with open(csv_path, "r", encoding="utf-8-sig", newline="") as f:
            reader = csv.DictReader(f)
            if not reader.fieldnames:
                raise ValueError(f"CSV không có header: {csv_path}")
            missing_columns = {"rq", "status", "params", "algorithm"} - set(reader.fieldnames)
            if missing_columns:
                raise ValueError(f"Thiếu cột bắt buộc {sorted(missing_columns)}: {csv_path}")
            if expected_header is None:
                expected_header = reader.fieldnames
            elif reader.fieldnames != expected_header:
                raise ValueError(f"Schema CSV không khớp: {csv_path}")
            for r in reader:
                if None in r:
                    raise ValueError(f"Dòng CSV có số cột thừa: {csv_path}")
                if r.get("rq") == "RQ3" and r.get("status") == "ok":
                    params = parse_params(r.get("params", ""))
                    r["parsed_c"] = int(params.get("c", 0))
                    r["parsed_query"] = params.get("query", "hit")
                    r["parsed_m"] = int(params.get("m", 262144))
                    r["parsed_alpha"] = float(params.get("alpha", 0.50))
                    rows.append(r)
    return rows

def merge_csv_files(csv_paths, output_path):
    """Gom các CSV cùng schema vào một file, chỉ ghi header một lần."""
    output_abs = os.path.abspath(output_path)
    input_abs = [os.path.abspath(path) for path in csv_paths]
    if output_abs in input_abs:
        raise ValueError("File CSV đầu ra không được trùng file CSV đầu vào.")

    expected_header = None
    total_rows = 0
    output_dir = os.path.dirname(output_abs)
    if output_dir:
        os.makedirs(output_dir, exist_ok=True)

    with open(output_abs, "w", encoding="utf-8", newline="") as out_file:
        writer = None
        for csv_path in csv_paths:
            with open(csv_path, "r", encoding="utf-8-sig", newline="") as in_file:
                reader = csv.DictReader(in_file)
                if not reader.fieldnames:
                    raise ValueError(f"CSV không có header: {csv_path}")
                if expected_header is None:
                    expected_header = reader.fieldnames
                    writer = csv.DictWriter(out_file, fieldnames=expected_header)
                    writer.writeheader()
                elif reader.fieldnames != expected_header:
                    raise ValueError(f"Schema CSV không khớp: {csv_path}")
                for row in reader:
                    if None in row:
                        raise ValueError(f"Dòng CSV có số cột thừa: {csv_path}")
                    writer.writerow(row)
                    total_rows += 1
    if expected_header is None:
        raise ValueError("Không có file CSV hợp lệ để gom.")
    return total_rows

def plot_rq3_probes_vs_churn(rows, out_dir, csv_rel_path="results/raw/rq3_results.csv"):
    """
    Hình 1: Số bước dò trung bình (Average Probes) theo chu kỳ churn C
    So sánh lp-tomb, lp-backshift, rh, và rh-noearly cho Hit và Miss.
    """
    # Gom nhóm theo (algo, query, c) -> list of ops_count
    grouped = defaultdict(list)
    for r in rows:
        ops_str = r.get("ops_count", "").strip()
        if ops_str:
            try:
                ops = float(ops_str)
                algo = r.get("algorithm", "")
                variant = r.get("variant", "base")
                key = (algo, variant, r["parsed_query"], r["parsed_c"])
                grouped[key].append(ops)
            except ValueError:
                pass

    if not grouped:
        print("[WARN] Không có dữ liệu ops_count hợp lệ để vẽ Hình 1.")
        return

    # Lấy danh sách chu kỳ C duy nhất
    cycles = sorted(list(set(r["parsed_c"] for r in rows)))
    if not cycles:
        cycles = [65536]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 6), sharey=False)

    styles = {
        ("rh", "base"): {"label": "Robin Hood (rh - Early Exit)", "color": "#1f77b4", "marker": "o", "ls": "-"},
        ("rh", "noearly"): {"label": "Robin Hood (rh-noearly)", "color": "#ff7f0e", "marker": "s", "ls": "--"},
        ("lp-backshift", "base"): {"label": "LP Backshift (Knuth R)", "color": "#2ca02c", "marker": "^", "ls": "-."},
        ("lp-tomb", "base"): {"label": "LP Tombstone (rebuild)", "color": "#d62728", "marker": "D", "ls": ":"}
    }

    # Đồ thị bên trái: Hit queries
    for (algo, variant), style in styles.items():
        y_vals = []
        x_vals = []
        for c in cycles:
            vals = grouped.get((algo, variant, "hit", c), [])
            if vals:
                x_vals.append(c)
                y_vals.append(sum(vals) / len(vals))
        if x_vals:
            ax1.plot(x_vals, y_vals, label=style["label"], color=style["color"],
                     marker=style["marker"], linestyle=style["ls"], linewidth=2, markersize=8)

    ax1.set_title("(a) Hit Queries (Successful Lookups)", fontsize=13, fontweight="bold")
    ax1.set_xlabel(f"Churn Cycles $C$ ({workload_label(rows)})", fontsize=11)
    ax1.set_ylabel("Average Probes per Query", fontsize=11)
    ax1.grid(True, linestyle=":", alpha=0.6)
    ax1.legend(loc="upper left", fontsize=10)
    ax1.set_xticks(cycles)
    ax1.set_xticklabels([f"{c//1024}K" if c >= 1024 else str(c) for c in cycles])

    # Đồ thị bên phải: Miss queries (Early Exit phát huy tác dụng vượt trội)
    for (algo, variant), style in styles.items():
        y_vals = []
        x_vals = []
        for c in cycles:
            vals = grouped.get((algo, variant, "miss", c), [])
            if vals:
                x_vals.append(c)
                y_vals.append(sum(vals) / len(vals))
        if x_vals:
            ax2.plot(x_vals, y_vals, label=style["label"], color=style["color"],
                     marker=style["marker"], linestyle=style["ls"], linewidth=2, markersize=8)

    ax2.set_title("(b) Miss Queries (Unsuccessful Lookups)", fontsize=13, fontweight="bold")
    ax2.set_xlabel(f"Churn Cycles $C$ ({workload_label(rows)})", fontsize=11)
    ax2.set_ylabel("Average Probes per Query", fontsize=11)
    ax2.grid(True, linestyle=":", alpha=0.6)
    ax2.legend(loc="upper left", fontsize=10)
    ax2.set_xticks(cycles)
    ax2.set_xticklabels([f"{c//1024}K" if c >= 1024 else str(c) for c in cycles])

    plt.suptitle("Fig. 1. RQ3 Probe Counts across Churn: Deletion and Lookup Mechanisms",
                 fontsize=14, fontweight="bold", y=0.99)
    fig.text(0.5, 0.01, f"Source: {csv_rel_path} | Generated by bench/plot_figures.py | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq3_probes_vs_churn.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.94])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ 1: {out_file}")

def plot_rq3_lookup_latency(rows, out_dir, csv_rel_path="results/raw/rq3_results.csv"):
    """
    Hình 2: Median lookup latency across seeds by churn cycle.
    """
    grouped = defaultdict(list)
    for r in rows:
        ms_str = r.get("elapsed_ms", "").strip()
        if ms_str:
            try:
                ms = float(ms_str)
                algo = r.get("algorithm", "")
                variant = r.get("variant", "base")
                key = (algo, variant, r["parsed_query"], r["parsed_c"])
                grouped[key].append(ms)
            except ValueError:
                pass

    if not grouped:
        print("[WARN] Không có dữ liệu elapsed_ms hợp lệ để vẽ Hình 2.")
        return

    cycles = sorted(list(set(r["parsed_c"] for r in rows)))
    if not cycles:
        cycles = [65536]

    fig, ax = plt.subplots(figsize=(10, 6))

    styles = {
        ("rh", "base", "hit"): {"label": "RH Base (Hit)", "color": "#1f77b4", "marker": "o", "ls": "-"},
        ("rh", "base", "miss"): {"label": "RH Base (Miss - Early Exit)", "color": "#1f77b4", "marker": "s", "ls": "--"},
        ("rh", "noearly", "hit"): {"label": "RH NoEarly (Hit)", "color": "#ff7f0e", "marker": "^", "ls": "-"},
        ("rh", "noearly", "miss"): {"label": "RH NoEarly (Miss)", "color": "#ff7f0e", "marker": "x", "ls": ":"},
        ("lp-backshift", "base", "hit"): {"label": "LP Backshift (Hit)", "color": "#2ca02c", "marker": "^", "ls": "-."},
        ("lp-backshift", "base", "miss"): {"label": "LP Backshift (Miss)", "color": "#2ca02c", "marker": "v", "ls": "--"},
        ("lp-tomb", "base", "hit"): {"label": "LP Tombstone (Hit)", "color": "#d62728", "marker": "D", "ls": "-"},
        ("lp-tomb", "base", "miss"): {"label": "LP Tombstone (Miss)", "color": "#9467bd", "marker": "P", "ls": "--"}
    }

    for (algo, variant, q), style in styles.items():
        x_vals = []
        y_vals = []
        for c in cycles:
            vals = grouped.get((algo, variant, q, c), [])
            if vals:
                x_vals.append(c)
                y_vals.append(statistics.median(vals))
        if x_vals:
            ax.plot(x_vals, y_vals, label=style["label"], color=style["color"],
                    marker=style["marker"], linestyle=style["ls"], linewidth=2, markersize=8)

    ax.set_title(f"Fig. 2. RQ3 Lookup Latency (ms) across Churn ({workload_label(rows)})",
                 fontsize=14, fontweight="bold")
    ax.set_xlabel(f"Churn Cycles $C$ ({workload_label(rows)})", fontsize=11)
    ax.set_ylabel("Execution Time (ms)", fontsize=11)
    ax.grid(True, linestyle=":", alpha=0.6)
    ax.legend(loc="upper left", fontsize=10)
    ax.set_xticks(cycles)
    ax.set_xticklabels([f"{c//1024}K" if c >= 1024 else str(c) for c in cycles])

    fig.text(0.5, 0.01, f"Source: {csv_rel_path} | Generated by bench/plot_figures.py | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq3_lookup_latency.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.98])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ 2: {out_file}")

def plot_rq3_mechanism_ablation(rows, out_dir, csv_rel_path="results/raw/rq3_results.csv"):
    """
    Hình 3: Biểu đồ cột bóc tách 3 cơ chế cốt lõi của Robin Hood Hashing (RQ3 Ablation):
    1) Backshift Deletion (loại bỏ tombstone)
    2) DIB Swapping (cân bằng chuỗi dò)
    3) Early Exit (tiết kiệm số bước dò trên Miss queries)
    """
    grouped_ops = defaultdict(list)
    for r in rows:
        ops_str = r.get("ops_count", "").strip()
        if ops_str:
            try:
                ops = float(ops_str)
                algo = r.get("algorithm", "")
                variant = r.get("variant", "base")
                key = (algo, variant, r["parsed_query"])
                grouped_ops[key].append(ops)
            except ValueError:
                pass

    if not grouped_ops:
        print("[WARN] Không đủ dữ liệu để vẽ Hình 3 (Mechanism Ablation).")
        return

    # Tính trung bình trên toàn bộ các chu kỳ C và seeds
    categories = ["Hit Queries", "Miss Queries"]
    
    def mean_probes(key):
        values = grouped_ops[key]
        return sum(values) / len(values) if values else 0.0

    algorithms = [
        ("lp-tomb", "base", "LP Tombstone", "#d62728"),
        ("lp-backshift", "base", "LP Backshift (Knuth R)", "#2ca02c"),
        ("rh", "noearly", "RH NoEarly", "#ff7f0e"),
        ("rh", "base", "Robin Hood (Early Exit)", "#1f77b4"),
    ]
    values = [
        [mean_probes((algo, variant, query)) for query in ("hit", "miss")]
        for algo, variant, _, _ in algorithms
    ]

    fig, ax = plt.subplots(figsize=(9, 6))

    import numpy as np
    x = np.arange(len(categories))
    width = 0.18
    rects = []
    for index, (_, _, label, color) in enumerate(algorithms):
        rects.append(ax.bar(
            x + (index - 1.5) * width,
            values[index],
            width,
            label=label,
            color=color,
        ))

    ax.set_ylabel("Average Probes per Lookup", fontsize=11)
    ax.set_title(f"Fig. 3. RQ3 Mechanism Decomposition ({workload_label(rows)})",
                 fontsize=14, fontweight="bold")
    ax.set_xticks(x)
    ax.set_xticklabels(categories, fontsize=12, fontweight="bold")
    ax.legend(fontsize=10)
    ax.grid(True, axis="y", linestyle=":", alpha=0.6)

    # Hiển thị nhãn giá trị trên mỗi cột
    for bars in rects:
        for rect in bars:
            height = rect.get_height()
            ax.annotate(f"{height:.2f}",
                        xy=(rect.get_x() + rect.get_width() / 2, height),
                        xytext=(0, 3),  # 3 points vertical offset
                        textcoords="offset points",
                        ha="center", va="bottom", fontsize=10, fontweight="bold")

    # Thêm mũi tên chú thích điểm nhấn Early Exit trên Miss queries
    noearly_miss = values[2][1]
    rh_miss = values[3][1]
    if noearly_miss > rh_miss:
        pct_diff = ((noearly_miss - rh_miss) / noearly_miss) * 100
        ax.annotate(f"Early Exit saves {pct_diff:.1f}% probes\non unsuccessful lookups!",
                    xy=(1 + 0.5 * width, (rh_miss + noearly_miss) / 2),
                    xytext=(0.55, max(noearly_miss, rh_miss) * 0.85),
                    arrowprops=dict(facecolor="#d62728", shrink=0.08, width=1.5, headwidth=8),
                    fontsize=10, fontweight="bold", color="#d62728",
                    bbox=dict(boxstyle="round,pad=0.3", edgecolor="#d62728", facecolor="#fff0f0"))

    fig.text(0.5, 0.01, f"Source: {csv_rel_path} | Generated by bench/plot_figures.py | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq3_mechanism_ablation.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.98])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ 3: {out_file}")

def main():
    parser = argparse.ArgumentParser(description="Plot figures for IEEE Paper (CSD201 FA26)")
    sources = parser.add_mutually_exclusive_group()
    sources.add_argument("--csv", nargs="+", help="Một hoặc nhiều file CSV dữ liệu thô")
    sources.add_argument("--input-dir", help="Gom mọi file .csv trong thư mục này và các thư mục con")
    parser.add_argument("--merge-output", help="Đường dẫn file CSV đầu ra sau khi gom")
    parser.add_argument("--outdir", type=str, default=None, help="Thư mục xuất ảnh đồ thị")
    args = parser.parse_args()

    if args.csv:
        csv_paths = args.csv
    elif args.input_dir:
        if not os.path.isdir(args.input_dir):
            parser.error(f"Thư mục CSV không tồn tại: {args.input_dir}")
        csv_paths = sorted(glob.glob(os.path.join(args.input_dir, "**", "*.csv"), recursive=True))
    else:
        csv_paths = [get_default_csv_path()]

    merge_output = args.merge_output
    should_merge = bool(args.input_dir or len(csv_paths) > 1 or merge_output)
    if should_merge:
        if merge_output is None:
            base_dir = os.path.dirname(os.path.abspath(csv_paths[0])) if csv_paths else os.path.dirname(get_default_csv_path())
            merge_output = os.path.join(base_dir, "rq3_merged.csv")
        merge_abs = os.path.abspath(merge_output)
        csv_paths = [path for path in csv_paths if os.path.abspath(path) != merge_abs]
        if not csv_paths:
            parser.error("Không có CSV đầu vào để gom.")
        try:
            merged_count = merge_csv_files(csv_paths, merge_output)
        except (OSError, ValueError, csv.Error) as error:
            print(f"[ERROR] Không thể gom CSV: {error}")
            sys.exit(1)
        print(f"Đã gom {len(csv_paths)} file / {merged_count} dòng vào: {merge_output}")
        csv_paths = [merge_output]

    out_dir = args.outdir or get_default_figures_dir()

    os.makedirs(out_dir, exist_ok=True)

    print("========================================================")
    print("      FIGURE GENERATOR FOR IEEE PAPER (Cặp 3 - C3)     ")
    print("========================================================")
    print(f"File CSV nguồn: {', '.join(csv_paths)}")
    print(f"Thư mục xuất ảnh: {out_dir}\n")

    try:
        rows = load_data(csv_paths)
    except (OSError, ValueError, csv.Error) as error:
        print(f"[ERROR] Không thể đọc CSV: {error}")
        sys.exit(1)
    if not rows:
        print(f"[ERROR] Không có dữ liệu hợp lệ trong file CSV: {', '.join(csv_paths)}")
        print("Hãy chạy script bench/run_all.py để đo đạc và tạo file CSV trước.")
        sys.exit(1)

    print(f"Đã đọc thành công {len(rows)} bản ghi đo thực nghiệm.\n")

    csv_rel = ", ".join(os.path.relpath(path, os.path.join(out_dir, "..", "..")) for path in csv_paths)

    plot_rq3_probes_vs_churn(rows, out_dir, csv_rel_path=csv_rel)
    plot_rq3_lookup_latency(rows, out_dir, csv_rel_path=csv_rel)
    plot_rq3_mechanism_ablation(rows, out_dir, csv_rel_path=csv_rel)

    print("\n--------------------------------------------------------")
    print("HOÀN TẤT TẠO TOÀN BỘ BIỂU ĐỒ RQ3 CHO BÀI BÁO IEEE!")
    print(f"Kiểm tra các file ảnh đồ thị tại: {out_dir}")
    print("--------------------------------------------------------\n")

if __name__ == "__main__":
    main()
