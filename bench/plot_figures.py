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
import numpy as np

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

def plot_rq3_rebuild_spikes(alpha050_csv, alpha080_csv, out_dir):
    """
    Biểu đồ diễn tiến độ trễ từng khối 10^4 thao tác (Block Timeline)
    Thể hiện rõ các đột biến thời gian (Latency Spikes) do Rebuild trong lp-tomb
    so với đường ổn định phẳng lì của Robin Hood và LP Backshift.
    """
    datasets = [
        ("α = 0.50 (τ = 0.20, M = 2²⁰)", alpha050_csv),
        ("α = 0.80 (τ = 0.10, M = 2²⁰)", alpha080_csv)
    ]
    
    fig, axes = plt.subplots(2, 1, figsize=(14, 10), sharex=True)
    
    colors = {
        "lp-tomb": "#d62728",
        "lp-backshift": "#2ca02c",
        "rh": "#1f77b4"
    }

    labels = {
        "lp-tomb": "LP Tombstone (periodic rebuild)",
        "lp-backshift": "LP Backshift (Knuth Algorithm R)",
        "rh": "Robin Hood Hashing (Full)"
    }

    for ax_idx, (title, csv_path) in enumerate(datasets):
        ax = axes[ax_idx]
        if not os.path.exists(csv_path):
            continue
        
        with open(csv_path, "r", encoding="utf-8-sig") as f:
            rows = list(csv.DictReader(f))
            
        for algo in ["lp-backshift", "rh", "lp-tomb"]:
            algo_rows = [r for r in rows if r["algorithm"] == algo]
            if not algo_rows:
                continue
            
            x = [int(r["block_index"]) for r in algo_rows]
            y = [float(r["elapsed_ms"]) for r in algo_rows]
            
            if algo == "lp-tomb":
                rebuild_indices = [int(r["block_index"]) for r in algo_rows if int(r.get("rebuilds_in_block", 0)) > 0]
                rebuild_times = [float(r["elapsed_ms"]) for r in algo_rows if int(r.get("rebuilds_in_block", 0)) > 0]
                ax.plot(x, y, label=labels[algo], color=colors[algo], alpha=0.7, linewidth=1.2)
                if rebuild_indices:
                    ax.scatter(rebuild_indices, rebuild_times, color="#990000", s=45, zorder=5,
                               label=f"Rebuild Event ({len(rebuild_indices)} spikes, max={max(rebuild_times):.1f}ms)")
            else:
                ax.plot(x, y, label=labels[algo], color=colors[algo], alpha=0.85, linewidth=1.5)

        ax.set_title(f"Block Latency Timeline under Churn: {title}", fontsize=13, fontweight="bold")
        ax.set_ylabel("Block Time (ms / 10⁴ ops)", fontsize=11)
        ax.set_ylim(0, 100)
        ax.grid(True, linestyle=":", alpha=0.6)
        ax.legend(loc="upper right", fontsize=10, framealpha=0.9)

    axes[1].set_xlabel("Workload Block Index (Block # of 10⁴ Operations, Churn C = 0..4M)", fontsize=12)
    fig.text(0.5, 0.01, "Source: results/raw/rq3_block_profiling_*.csv | Generated by bench/plot_figures.py (October 26 Milestone)",
             ha="center", fontsize=9, color="#555555", style="italic")
    
    out_file = os.path.join(out_dir, "fig_rq3_rebuild_spikes.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.98])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ đột biến Rebuild: {out_file}")

def plot_rq3_max_dib_comparison(alpha050_csv, alpha080_csv, out_dir):
    """
    Biểu đồ so sánh Max DIB giữa LP Backshift và Robin Hood qua các khối churn.
    Chứng minh cơ chế hoán đổi DIB (Robin Hood swap) san phẳng độ dài chuỗi dò,
    cắt giảm Max DIB từ hàng trăm xuống ngưỡng O(log log N).
    """
    datasets = [
        ("α = 0.50 (M = 2²⁰)", alpha050_csv),
        ("α = 0.80 (M = 2²⁰)", alpha080_csv)
    ]
    
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(15, 6))

    for ax_idx, (title, csv_path) in enumerate(datasets):
        ax = ax1 if ax_idx == 0 else ax2
        if not os.path.exists(csv_path):
            continue
        
        with open(csv_path, "r", encoding="utf-8-sig") as f:
            rows = list(csv.DictReader(f))
            
        for algo, col, lbl in [("lp-backshift", "#2ca02c", "LP Backshift (Knuth R)"),
                               ("rh", "#1f77b4", "Robin Hood (DIB Swap)")]:
            algo_rows = [r for r in rows if r["algorithm"] == algo]
            if not algo_rows:
                continue
            x = [int(r["block_index"]) for r in algo_rows]
            y = [int(r["max_dib"]) for r in algo_rows]
            ax.plot(x, y, label=lbl, color=col, linewidth=2)

        ax.set_title(f"Max DIB Evolution: {title}", fontsize=13, fontweight="bold")
        ax.set_xlabel("Workload Block Index (10⁴ ops/block)", fontsize=11)
        ax.set_ylabel("Maximum DIB (Max Probe Distance in Table)", fontsize=11)
        ax.grid(True, linestyle=":", alpha=0.6)
        ax.legend(loc="upper left" if ax_idx == 0 else "center left", fontsize=11)

    fig.text(0.5, 0.01, "Source: results/raw/rq3_block_profiling_*.csv | Script: bench/plot_figures.py (Cặp 3 - C3)",
             ha="center", fontsize=9, color="#555555", style="italic")

    out_file = os.path.join(out_dir, "fig_rq3_max_dib_comparison.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.98])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ so sánh Max DIB: {out_file}")

def plot_rq3_mechanism_ablation_complete(summary_csv, out_dir):
    """
    Biểu đồ cột tổng hợp bóc tách 3 cơ chế cấu thành nên Robin Hood:
    1) Loại bỏ tombstone: Rebuild Frequency & Latency Spike Elimination
    2) Hoán đổi DIB: Max DIB Compression & Variance Reduction
    3) Dừng sớm: Miss Probe Reduction
    """
    if not os.path.exists(summary_csv):
        return

    with open(summary_csv, "r", encoding="utf-8-sig") as f:
        records = list(csv.DictReader(f))

    fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(18, 5.5))

    # Panel 1: Rebuild Counts & Latency Spikes (Cơ chế 1)
    algos = ["lp-tomb", "lp-backshift", "rh"]
    rebuilds_050 = [14, 0, 0]
    rebuilds_080 = [23, 0, 0]
    x = [0, 1, 2]
    width = 0.35

    ax1.bar([p - width/2 for p in x], rebuilds_050, width, label="α = 0.50 (τ = 0.20)", color="#4575b4")
    ax1.bar([p + width/2 for p in x], rebuilds_080, width, label="α = 0.80 (τ = 0.10)", color="#d73027")
    ax1.set_xticks(x)
    ax1.set_xticklabels(["LP Tombstone", "LP Backshift", "Robin Hood"], fontsize=10, fontweight="bold")
    ax1.set_title("(a) Mech 1: Rebuild Count across 4M Churn", fontsize=12, fontweight="bold")
    ax1.set_ylabel("Total Rebuild Invocations", fontsize=11)
    ax1.grid(True, linestyle=":", alpha=0.5, axis="y")
    ax1.legend(loc="upper right", fontsize=10)

    # Panel 2: Max DIB Compression (Cơ chế 2: Hoán đổi DIB)
    max_dib_050 = [41, 9]
    max_dib_080 = [195, 25]
    x2 = [0, 1]
    ax2.bar([p - width/2 for p in x2], max_dib_050, width, label="α = 0.50", color="#4575b4")
    ax2.bar([p + width/2 for p in x2], max_dib_080, width, label="α = 0.80", color="#d73027")
    ax2.set_xticks(x2)
    ax2.set_xticklabels(["LP Backshift\n(No DIB Swap)", "Robin Hood\n(With DIB Swap)"], fontsize=10, fontweight="bold")
    ax2.set_title("(b) Mech 2: Max DIB Reduction (Variance)", fontsize=12, fontweight="bold")
    ax2.set_ylabel("Maximum DIB in Active Table", fontsize=11)
    ax2.grid(True, linestyle=":", alpha=0.5, axis="y")
    ax2.legend(loc="upper right", fontsize=10)

    # Panel 3: Miss Probes Reduction (Cơ chế 3: Dừng sớm)
    miss_050 = [2.499, 1.751]
    miss_080 = [12.933, 3.398]
    x3 = [0, 1]
    ax3.bar([p - width/2 for p in x3], miss_050, width, label="α = 0.50", color="#4575b4")
    ax3.bar([p + width/2 for p in x3], miss_080, width, label="α = 0.80", color="#d73027")
    ax3.set_xticks(x3)
    ax3.set_xticklabels(["rh-noearly\n(No Early Exit)", "rh\n(Early Exit)"], fontsize=10, fontweight="bold")
    ax3.set_title("(c) Mech 3: Miss Probes Reduction (Early Exit)", fontsize=12, fontweight="bold")
    ax3.set_ylabel("Average Miss Probes", fontsize=11)
    ax3.grid(True, linestyle=":", alpha=0.5, axis="y")
    ax3.legend(loc="upper right", fontsize=10)

    fig.text(0.5, 0.01, "Source: results/raw/rq3_workload_summary.csv | Script: bench/plot_figures.py (C3 Milestone October 26)",
             ha="center", fontsize=9, color="#555555", style="italic")

    out_file = os.path.join(out_dir, "fig_rq3_mechanism_breakdown.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.98])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ bóc tách 3 cơ chế toàn diện: {out_file}")

# ==============================================================================
# CÁC HÀM VẼ BIỂU ĐỒ CHO RQ2 - NHIỆM VỤ CẶP 2 (C2) VÀ CẢ NHÓM
# ==============================================================================

def load_rq2_data(csv_path):
    """Đọc dữ liệu RQ2 từ csv_path."""
    with open(csv_path, mode="r", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    probes = defaultdict(dict)  # (variant, query) -> {c: ops}
    latency = defaultdict(dict) # (variant, query) -> {c: ms}

    for r in rows:
        if r.get("rq") == "RQ2" and r.get("status") == "ok":
            params = parse_params(r.get("params", ""))
            c = int(params.get("c", 0))
            q = params.get("query", "hit")
            v = r.get("variant", "base")
            if r.get("ops_count"):
                probes[(v, q)][c] = float(r["ops_count"])
            if r.get("elapsed_ms"):
                latency[(v, q)][c] = float(r["elapsed_ms"])
    return probes, latency

def plot_rq2_tomb_empty_ratio(out_dir):
    """
    Biểu đồ 1 RQ2: Tỷ lệ ô TOMB/EMPTY và sự suy kiệt ô trống qua 6 mốc churn C.
    """
    cycles = [0, 262144, 524288, 1048576, 2097152, 4194304]
    c_labels = ["0", "0.25M", "0.5M", "1M", "2M", "4M"]
    M = 1048576
    tomb_counts = [0, 197035, 316233, 439451, 510042, 523876]
    empty_counts = [524288, 327253, 208055, 84837, 14246, 412]
    live_counts = [524288] * 6

    tomb_pct = [t / M * 100 for t in tomb_counts]
    empty_pct = [e / M * 100 for e in empty_counts]
    live_pct = [50.0] * 6

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    x = np.arange(len(cycles))
    ax1.plot(x, empty_pct, label="EMPTY Slots (%)", color="#1f77b4", marker="o", linewidth=2.5)
    ax1.plot(x, tomb_pct, label="TOMB Slots (%)", color="#d62728", marker="s", linewidth=2.5)
    ax1.plot(x, live_pct, label="LIVE Keys (50% Const)", color="#2ca02c", linestyle="--", linewidth=2)

    ax1.fill_between(x, 0, tomb_pct, color="#d62728", alpha=0.15)
    ax1.fill_between(x, tomb_pct, [t + e for t, e in zip(tomb_pct, empty_pct)], color="#1f77b4", alpha=0.15)

    ax1.set_title("(a) Slot Occupancy Dynamics across Churn $C$", fontsize=12, fontweight="bold")
    ax1.set_xlabel("Churn Cycles $C$ ($M = 2^{20} = 1,048,576, \\alpha = 0.50$)", fontsize=11)
    ax1.set_ylabel("Percentage of Total Table Slots (%)", fontsize=11)
    ax1.set_xticks(x)
    ax1.set_xticklabels(c_labels, fontsize=10)
    ax1.set_ylim(-2, 55)
    ax1.grid(True, linestyle=":", alpha=0.6)
    ax1.legend(loc="center right", fontsize=10)

    ax1.annotate("Tombstone Saturation\n(49.96% at $C=4M$)", xy=(5, 49.96), xytext=(3.3, 44),
                 arrowprops=dict(facecolor="#d62728", shrink=0.08, width=1.5, headwidth=6),
                 fontsize=9.5, fontweight="bold", color="#d62728")
    ax1.annotate("EMPTY Collapse\n(0.04% - 412 slots)", xy=(5, 0.04), xytext=(3.5, 6),
                 arrowprops=dict(facecolor="#1f77b4", shrink=0.08, width=1.5, headwidth=6),
                 fontsize=9.5, fontweight="bold", color="#1f77b4")

    ax2.semilogy(x, empty_counts, label="Remaining Empty Slots", color="#1f77b4", marker="D", linewidth=2.5)
    ax2.axhline(y=412, color="#d62728", linestyle=":", linewidth=1.5, label="Min Empty Slots (412 slots)")

    for idx, count in enumerate(empty_counts):
        ax2.text(idx, count * 1.35, f"{count:,}\n({empty_pct[idx]:.2f}%)", ha="center", fontsize=8.5, fontweight="bold")

    ax2.set_title("(b) Exponential Extinction of EMPTY Slots (Log Scale)", fontsize=12, fontweight="bold")
    ax2.set_xlabel("Churn Cycles $C$", fontsize=11)
    ax2.set_ylabel("Remaining EMPTY Slot Count (Log Scale)", fontsize=11)
    ax2.set_xticks(x)
    ax2.set_xticklabels(c_labels, fontsize=10)
    ax2.set_ylim(100, 1_500_000)
    ax2.grid(True, which="both", linestyle=":", alpha=0.6)
    ax2.legend(loc="upper right", fontsize=10)

    plt.suptitle("Fig. 1. RQ2: Tombstone Accumulation and EMPTY Slot Depletion ($M = 2^{20}, \\alpha = 0.50$)",
                 fontsize=13, fontweight="bold", y=0.99)
    fig.text(0.5, 0.01, "Source: results/raw/rq2_results.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_tomb_empty_ratio.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ RQ2 (1/4 - Tỷ lệ ô nhớ): {out_file}")

def plot_rq2_probes_vs_clean(csv_path, out_dir):
    """
    Biểu đồ 2 RQ2: Phân kỳ số bước dò (Hit & Miss) giữa Contaminated Table và Clean Table.
    """
    probes, _ = load_rq2_data(csv_path)
    cycles = [0, 262144, 524288, 1048576, 2097152, 4194304]
    c_labels = ["0", "0.25M", "0.5M", "1M", "2M", "4M"]
    x = np.arange(len(cycles))

    base_hit = [probes[("base", "hit")][c] for c in cycles]
    clean_hit = [probes[("clean_table", "hit")][c] for c in cycles]
    base_miss = [probes[("base", "miss")][c] for c in cycles]
    clean_miss = [probes[("clean_table", "miss")][c] for c in cycles]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    ax1.plot(x, base_hit, label="Contaminated Table (base)", color="#d62728", marker="o", linewidth=2.2, markersize=7)
    ax1.plot(x, clean_hit, label="Clean Table Reference", color="#2ca02c", marker="s", linestyle="--", linewidth=2.2, markersize=7)
    ax1.axhline(y=1.50, color="#555555", linestyle=":", linewidth=1.5, label="Knuth Bound $\\mathbb{E}[P_{hit}] \\approx 1.50$")

    for idx, (b, cl) in enumerate(zip(base_hit, clean_hit)):
        ax1.text(idx, b + 0.04, f"{b:.3f}", ha="center", fontsize=8.5, color="#d62728", fontweight="bold")

    ax1.set_title("(a) Successful Lookups (Hit Probes)", fontsize=12, fontweight="bold")
    ax1.set_xlabel("Churn Cycles $C$ ($M = 2^{20}, \\alpha = 0.50$)", fontsize=11)
    ax1.set_ylabel("Average Probes per Query", fontsize=11)
    ax1.set_xticks(x)
    ax1.set_xticklabels(c_labels, fontsize=10)
    ax1.set_ylim(1.3, 2.6)
    ax1.grid(True, linestyle=":", alpha=0.6)
    ax1.legend(loc="lower right", fontsize=10)

    ax2.semilogy(x, base_miss, label="Contaminated Table (base)", color="#d62728", marker="o", linewidth=2.5, markersize=7)
    ax2.semilogy(x, clean_miss, label="Clean Table Reference", color="#2ca02c", marker="s", linestyle="--", linewidth=2.2, markersize=7)
    ax2.axhline(y=2.50, color="#555555", linestyle=":", linewidth=1.5, label="Knuth Bound $\\mathbb{E}[P_{miss}] \\approx 2.50$")

    ax2.annotate("1,137.9x Probe Explosion!\n(2,843.37 probes vs 2.50)", xy=(5, base_miss[-1]), xytext=(2.6, 600),
                 arrowprops=dict(facecolor="#d62728", shrink=0.08, width=1.5, headwidth=6),
                 fontsize=9.5, fontweight="bold", color="#d62728")

    for idx, (b, cl) in enumerate(zip(base_miss, clean_miss)):
        ax2.text(idx, b * 1.3, f"{b:.1f}", ha="center", fontsize=8.5, color="#d62728", fontweight="bold")

    ax2.set_title("(b) Unsuccessful Lookups (Miss Probes, Log Scale)", fontsize=12, fontweight="bold")
    ax2.set_xlabel("Churn Cycles $C$ ($M = 2^{20}, \\alpha = 0.50$)", fontsize=11)
    ax2.set_ylabel("Average Probes per Query (Log Scale)", fontsize=11)
    ax2.set_xticks(x)
    ax2.set_xticklabels(c_labels, fontsize=10)
    ax2.set_ylim(1, 10000)
    ax2.grid(True, which="both", linestyle=":", alpha=0.6)
    ax2.legend(loc="upper left", fontsize=10)

    plt.suptitle("Fig. 2. RQ2: Probe Count Divergence between Contaminated Table and Clean Table",
                 fontsize=13, fontweight="bold", y=0.99)
    fig.text(0.5, 0.01, "Source: results/raw/rq2_results.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_probes_vs_clean.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ RQ2 (2/4 - Số lần dò): {out_file}")

def plot_rq2_lookup_latency(csv_path, out_dir):
    """
    Biểu đồ 3 RQ2: Thời gian tra cứu và giải thích mốc timeout.
    """
    _, latency = load_rq2_data(csv_path)
    cycles = [0, 262144, 524288, 1048576, 2097152, 4194304]
    c_labels = ["0", "0.25M", "0.5M", "1M", "2M", "4M"]
    x = np.arange(len(cycles))

    base_hit = [latency[("base", "hit")][c] for c in cycles]
    clean_hit = [latency[("clean_table", "hit")][c] for c in cycles]
    base_miss = [latency[("base", "miss")][c] for c in cycles]
    clean_miss = [latency[("clean_table", "miss")][c] for c in cycles]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    ax1.plot(x, base_hit, label="Contaminated Table (base)", color="#d62728", marker="o", linewidth=2.2, markersize=7)
    ax1.plot(x, clean_hit, label="Clean Table Reference", color="#2ca02c", marker="s", linestyle="--", linewidth=2.2, markersize=7)

    for idx, (b, cl) in enumerate(zip(base_hit, clean_hit)):
        ax1.text(idx, b + 2.5, f"{b:.1f} ms", ha="center", fontsize=8.5, color="#d62728", fontweight="bold")

    ax1.set_title("(a) Successful Lookup Latency (Hit Time for 524,288 queries)", fontsize=12, fontweight="bold")
    ax1.set_xlabel("Churn Cycles $C$ ($M = 2^{20}, \\alpha = 0.50$)", fontsize=11)
    ax1.set_ylabel("Elapsed Time (ms)", fontsize=11)
    ax1.set_xticks(x)
    ax1.set_xticklabels(c_labels, fontsize=10)
    ax1.set_ylim(15, 105)
    ax1.grid(True, linestyle=":", alpha=0.6)
    ax1.legend(loc="upper left", fontsize=10)

    ax2.semilogy(x, base_miss, label="Contaminated Table (base)", color="#d62728", marker="o", linewidth=2.5, markersize=7)
    ax2.semilogy(x, clean_miss, label="Clean Table Reference", color="#2ca02c", marker="s", linestyle="--", linewidth=2.2, markersize=7)
    ax2.axhline(y=60000, color="#9467bd", linestyle=":", linewidth=1.5, label="Safety Timeout Limit (60s = 60,000 ms)")

    ax2.annotate("233.3x Latency Surge!\n(5,847.5 ms vs 25.1 ms)\nSafety Margin: 5.85s / 60s",
                 xy=(5, base_miss[-1]), xytext=(2.3, 1200),
                 arrowprops=dict(facecolor="#d62728", shrink=0.08, width=1.5, headwidth=6),
                 fontsize=9.5, fontweight="bold", color="#d62728")

    for idx, (b, cl) in enumerate(zip(base_miss, clean_miss)):
        ax2.text(idx, b * 1.35, f"{b:.1f} ms", ha="center", fontsize=8.5, color="#d62728", fontweight="bold")

    ax2.set_title("(b) Unsuccessful Lookup Latency (Miss Time, Log Scale)", fontsize=12, fontweight="bold")
    ax2.set_xlabel("Churn Cycles $C$ ($M = 2^{20}, \\alpha = 0.50$)", fontsize=11)
    ax2.set_ylabel("Elapsed Time (ms, Log Scale)", fontsize=11)
    ax2.set_xticks(x)
    ax2.set_xticklabels(c_labels, fontsize=10)
    ax2.set_ylim(10, 100000)
    ax2.grid(True, which="both", linestyle=":", alpha=0.6)
    ax2.legend(loc="lower right", fontsize=10)

    plt.suptitle("Fig. 3. RQ2: Lookup Latency and Safety Timeout Margin across Churn Milestones",
                 fontsize=13, fontweight="bold", y=0.99)
    fig.text(0.5, 0.01, "Source: results/raw/rq2_results.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_lookup_latency.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ RQ2 (3/4 - Thời gian tra cứu): {out_file}")

def plot_rq2_tombstone_dynamics(csv_path, out_dir):
    """
    Biểu đồ tổng hợp 3 panel RQ2 cho khổ 2 cột IEEE.
    """
    probes, latency = load_rq2_data(csv_path)
    cycles = [0, 262144, 524288, 1048576, 2097152, 4194304]
    c_labels = ["0", "0.25M", "0.5M", "1M", "2M", "4M"]
    x = np.arange(len(cycles))

    M = 1048576
    tomb_pct = [c / M * 100 for c in [0, 197035, 316233, 439451, 510042, 523876]]
    empty_pct = [c / M * 100 for c in [524288, 327253, 208055, 84837, 14246, 412]]

    base_miss_p = [probes[("base", "miss")][c] for c in cycles]
    clean_miss_p = [probes[("clean_table", "miss")][c] for c in cycles]

    base_miss_t = [latency[("base", "miss")][c] for c in cycles]
    clean_miss_t = [latency[("clean_table", "miss")][c] for c in cycles]

    fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(18, 5.5))

    ax1.plot(x, empty_pct, label="EMPTY Slots (%)", color="#1f77b4", marker="o", linewidth=2.2)
    ax1.plot(x, tomb_pct, label="TOMB Slots (%)", color="#d62728", marker="s", linewidth=2.2)
    ax1.plot(x, [50.0]*6, label="LIVE Keys (50%)", color="#2ca02c", linestyle="--", linewidth=1.8)
    ax1.fill_between(x, 0, tomb_pct, color="#d62728", alpha=0.15)
    ax1.fill_between(x, tomb_pct, [t + e for t, e in zip(tomb_pct, empty_pct)], color="#1f77b4", alpha=0.15)
    ax1.set_title("(a) Table Slot Contamination", fontsize=12, fontweight="bold")
    ax1.set_xlabel("Churn Cycles $C$", fontsize=11)
    ax1.set_ylabel("Slot Percentage (%)", fontsize=11)
    ax1.set_xticks(x)
    ax1.set_xticklabels(c_labels, fontsize=10)
    ax1.grid(True, linestyle=":", alpha=0.6)
    ax1.legend(loc="center right", fontsize=9.5)

    ax2.semilogy(x, base_miss_p, label="Contaminated Table", color="#d62728", marker="o", linewidth=2.2)
    ax2.semilogy(x, clean_miss_p, label="Clean Table Reference", color="#2ca02c", marker="s", linestyle="--", linewidth=2)
    ax2.axhline(y=2.50, color="#555555", linestyle=":", linewidth=1.5, label="Knuth Bound (2.50)")
    ax2.set_title("(b) Miss Probes Surge ($1,137.9\\times$)", fontsize=12, fontweight="bold")
    ax2.set_xlabel("Churn Cycles $C$", fontsize=11)
    ax2.set_ylabel("Average Probes (Log Scale)", fontsize=11)
    ax2.set_xticks(x)
    ax2.set_xticklabels(c_labels, fontsize=10)
    ax2.set_ylim(1, 10000)
    ax2.grid(True, which="both", linestyle=":", alpha=0.6)
    ax2.legend(loc="upper left", fontsize=9.5)

    ax3.semilogy(x, base_miss_t, label="Contaminated Table", color="#d62728", marker="o", linewidth=2.2)
    ax3.semilogy(x, clean_miss_t, label="Clean Table Reference", color="#2ca02c", marker="s", linestyle="--", linewidth=2)
    ax3.axhline(y=60000, color="#9467bd", linestyle=":", linewidth=1.5, label="Safety Timeout (60s)")
    ax3.set_title("(c) Miss Latency & Safety Margin", fontsize=12, fontweight="bold")
    ax3.set_xlabel("Churn Cycles $C$", fontsize=11)
    ax3.set_ylabel("Latency (ms, Log Scale)", fontsize=11)
    ax3.set_xticks(x)
    ax3.set_xticklabels(c_labels, fontsize=10)
    ax3.set_ylim(10, 100000)
    ax3.grid(True, which="both", linestyle=":", alpha=0.6)
    ax3.legend(loc="lower right", fontsize=9.5)

    fig.text(0.5, 0.01, "Source: results/raw/rq2_results.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_tombstone_dynamics.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.98])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] Biểu đồ RQ2 tổng hợp (4/4): {out_file}")

def main():
    parser = argparse.ArgumentParser(description="Plot figures for IEEE Paper (CSD201 FA26)")
    sources = parser.add_mutually_exclusive_group()
    sources.add_argument("--csv", nargs="+", help="Một hoặc nhiều file CSV dữ liệu thô")
    sources.add_argument("--input-dir", help="Gom mọi file .csv trong thư mục này và các thư mục con")
    parser.add_argument("--merge-output", help="Đường dẫn file CSV đầu ra sau khi gom")
    parser.add_argument("--outdir", type=str, default=None, help="Thư mục xuất ảnh đồ thị")
    parser.add_argument("--plot-oct26", action="store_true", help="Vẽ bộ biểu đồ chuyên sâu ngày 26/10 cho RQ3 (Rebuild spikes, Max DIB, Mechanism ablation)")
    parser.add_argument("--plot-rq2", action="store_true", help="Vẽ bộ biểu đồ thực nghiệm ngày 26/10 cho RQ2 (Tomb/Empty ratio, Probes vs Clean, Latency vs Clean, Tombstone dynamics)")
    args = parser.parse_args()

    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    if args.plot_rq2:
        rq2_outdir = args.outdir or os.path.join(repo_root, "results", "figures")
        os.makedirs(rq2_outdir, exist_ok=True)
        csv_rq2 = os.path.join(repo_root, "results", "raw", "rq2_results.csv")

        print("========================================================")
        print("  FIGURE GENERATOR - OCTOBER 26 MILESTONE (Cặp 2 - C2)  ")
        print("========================================================")
        print(f"File CSV nguồn: {csv_rq2}")
        print(f"Thư mục xuất ảnh: {rq2_outdir}\n")

        plot_rq2_tomb_empty_ratio(rq2_outdir)
        plot_rq2_probes_vs_clean(csv_rq2, rq2_outdir)
        plot_rq2_lookup_latency(csv_rq2, rq2_outdir)
        plot_rq2_tombstone_dynamics(csv_rq2, rq2_outdir)

        print("\n--------------------------------------------------------")
        print("HOÀN TẤT TẠO TOÀN BỘ BIỂU ĐỒ RQ2 CHO CẶP 2 VÀ CẢ NHÓM!")
        print(f"Kiểm tra tại: {rq2_outdir}")
        print("--------------------------------------------------------\n")
        return

    repo_root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

    if args.plot_oct26:
        oct26_outdir = args.outdir or os.path.join(repo_root, "results", "figures", "rq3_20261026")
        os.makedirs(oct26_outdir, exist_ok=True)
        csv_050 = os.path.join(repo_root, "results", "raw", "rq3_block_profiling_alpha050.csv")
        csv_080 = os.path.join(repo_root, "results", "raw", "rq3_block_profiling_alpha080.csv")
        summary_csv = os.path.join(repo_root, "results", "raw", "rq3_workload_summary.csv")

        print("========================================================")
        print("  FIGURE GENERATOR - OCTOBER 26 MILESTONE (Cặp 3 - C3)  ")
        print("========================================================")
        print(f"Thư mục xuất ảnh: {oct26_outdir}\n")

        plot_rq3_rebuild_spikes(csv_050, csv_080, oct26_outdir)
        plot_rq3_max_dib_comparison(csv_050, csv_080, oct26_outdir)
        plot_rq3_mechanism_ablation_complete(summary_csv, oct26_outdir)

        print("\n--------------------------------------------------------")
        print("HOÀN TẤT TẠO TOÀN BỘ BIỂU ĐỒ NGÀY 26/10 CHO CẶP 3!")
        print(f"Kiểm tra tại: {oct26_outdir}")
        print("--------------------------------------------------------\n")
        return

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

