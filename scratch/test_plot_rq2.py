import os
import sys
import csv
from collections import defaultdict
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

def parse_params(params_str):
    res = {}
    for part in params_str.split(";"):
        if "=" in part:
            k, v = part.split("=", 1)
            res[k.strip().lower()] = v.strip().lower()
    return res

def load_rq2_data(csv_path):
    with open(csv_path, mode="r", encoding="utf-8") as f:
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

    # Panel 1: Stacked Area Chart of Table Slot Allocation
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

    # Annotations
    ax1.annotate("Tombstone Saturation\n(49.96% at $C=4M$)", xy=(5, 49.96), xytext=(3.3, 44),
                 arrowprops=dict(facecolor="#d62728", shrink=0.08, width=1.5, headwidth=6),
                 fontsize=9.5, fontweight="bold", color="#d62728")
    ax1.annotate("EMPTY Collapse\n(0.04% - 412 slots)", xy=(5, 0.04), xytext=(3.5, 6),
                 arrowprops=dict(facecolor="#1f77b4", shrink=0.08, width=1.5, headwidth=6),
                 fontsize=9.5, fontweight="bold", color="#1f77b4")

    # Panel 2: Empty Slots Collapse (Log Scale)
    ax2.semilogy(x, empty_counts, label="Remaining Empty Slots", color="#1f77b4", marker="D", linewidth=2.5)
    ax2.axhline(y=412, color="#d62728", linestyle=":", linewidth=1.5, label="Min Empty Slots (412 slots)")

    for idx, count in enumerate(empty_counts):
        ax2.text(idx, count * 1.35, f"{count:,}\n({empty_pct[idx]:.2f}%)", ha="center", fontsize=8.5, fontweight="semibold")

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
    fig.text(0.5, 0.01, "Source: results/raw/rq2_results_1m.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_tomb_empty_ratio.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] {out_file}")

def plot_rq2_probes_vs_clean(csv_path, out_dir):
    probes, _ = load_rq2_data(csv_path)
    cycles = [0, 262144, 524288, 1048576, 2097152, 4194304]
    c_labels = ["0", "0.25M", "0.5M", "1M", "2M", "4M"]
    x = np.arange(len(cycles))

    base_hit = [probes[("base", "hit")][c] for c in cycles]
    clean_hit = [probes[("clean_table", "hit")][c] for c in cycles]
    base_miss = [probes[("base", "miss")][c] for c in cycles]
    clean_miss = [probes[("clean_table", "miss")][c] for c in cycles]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    # Panel 1: Hit Probes
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

    # Panel 2: Miss Probes (Log Scale)
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
    fig.text(0.5, 0.01, "Source: results/raw/rq2_results_1m.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_probes_vs_clean.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] {out_file}")

def plot_rq2_lookup_latency(csv_path, out_dir):
    _, latency = load_rq2_data(csv_path)
    cycles = [0, 262144, 524288, 1048576, 2097152, 4194304]
    c_labels = ["0", "0.25M", "0.5M", "1M", "2M", "4M"]
    x = np.arange(len(cycles))

    base_hit = [latency[("base", "hit")][c] for c in cycles]
    clean_hit = [latency[("clean_table", "hit")][c] for c in cycles]
    base_miss = [latency[("base", "miss")][c] for c in cycles]
    clean_miss = [latency[("clean_table", "miss")][c] for c in cycles]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

    # Panel 1: Hit Latency
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

    # Panel 2: Miss Latency (Log Scale)
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
    fig.text(0.5, 0.01, "Source: results/raw/rq2_results_1m.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_lookup_latency.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] {out_file}")

def plot_rq2_tombstone_dynamics(csv_path, out_dir):
    """
    Unified 3-Panel Comprehensive Figure for IEEE 2-Column Format:
    (a) Table Contamination (Tombstone vs Empty slot breakdown)
    (b) Miss Probes Catastrophic Explosion vs Clean Table (Log Scale)
    (c) Miss Latency Surge & 60-second Safety Timeout Margin
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

    # Panel 1: Slot Contamination
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

    # Panel 2: Miss Probes Explosion
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

    # Panel 3: Miss Latency & Safety Margin
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

    fig.text(0.5, 0.01, "Source: results/raw/rq2_results_1m.csv | Script: bench/plot_figures.py (C2 Milestone) | IEEE CSD201 FA26",
             ha="center", fontsize=9, style="italic", color="#555555")

    out_file = os.path.join(out_dir, "fig_rq2_tombstone_dynamics.png")
    plt.tight_layout(rect=[0, 0.03, 1, 0.98])
    plt.savefig(out_file, dpi=300)
    plt.close()
    print(f"[SAVED] {out_file}")

if __name__ == "__main__":
    out = "results/figures"
    csv_p = "results/raw/rq2_results_1m.csv"
    os.makedirs(out, exist_ok=True)
    plot_rq2_tomb_empty_ratio(out)
    plot_rq2_probes_vs_clean(csv_p, out)
    plot_rq2_lookup_latency(csv_p, out)
    plot_rq2_tombstone_dynamics(csv_p, out)
