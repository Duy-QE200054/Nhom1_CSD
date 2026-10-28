"""
Packaging and Verification Script for Project Submission
Date: October 28, 2026
Author: nguyendinhtien824-super (Pair 3 / C3)

Validates all required submission deliverables (Source code, JAR, Tests, CSVs, Figures, Paper, README, Slides)
and builds a clean submission package archive in dist/ with a verified manifest and SHA-256 checksums.
"""

import hashlib
import os
import shutil
import sys
import zipfile

REQUIRED_ITEMS = [
    # Source Code
    "HashTableProject/src/hashtable/core/HashTable.java",
    "HashTableProject/src/hashtable/core/FibonacciHash.java",
    "HashTableProject/src/hashtable/core/SeparateChaining.java",
    "HashTableProject/src/hashtable/core/LinearProbingTombstone.java",
    "HashTableProject/src/hashtable/core/LinearProbingBackshift.java",
    "HashTableProject/src/hashtable/core/RobinHoodTable.java",
    "HashTableProject/src/hashtable/core/RobinHoodNoEarly.java",
    "HashTableProject/src/hashtable/bench/BenchmarkRunner.java",
    "HashTableProject/src/hashtable/bench/BenchmarkWrapper.java",
    
    # Tests
    "HashTableProject/test/hashtable/test/TestRunner.java",
    "HashTableProject/test/hashtable/test/HashTableOracleTest.java",
    "HashTableProject/test/hashtable/test/C3HashTableTest.java",
    "HashTableProject/test/hashtable/test/RobinHoodAndBackshiftTest.java",
    
    # Raw Data CSVs
    "results/raw/rq1_runner_results.csv",
    "results/raw/rq2_results.csv",
    "results/raw/rq3_20261024.csv",
    "results/raw/rq3_20261025.csv",
    "results/raw/rq3_block_profiling_alpha050.csv",
    "results/raw/rq3_block_profiling_alpha080.csv",
    "results/raw/rq3_workload_summary.csv",
    
    # Figures
    "results/figures/fig_rq2_tombstone_dynamics.png",
    "results/figures/rq3_20261024/fig_rq3_probes_vs_churn.png",
    "results/figures/rq3_20261024/fig_rq3_lookup_latency.png",
    "results/figures/rq3_20261024/fig_rq3_mechanism_ablation.png",
    "results/figures/rq3_20261025/fig_rq3_probes_vs_churn.png",
    "results/figures/rq3_20261026/fig_rq3_rebuild_spikes.png",
    "results/figures/rq3_20261026/fig_rq3_mechanism_breakdown.png",
    
    # Paper & Documentation
    "paper/paper.tex",
    "paper/paper.pdf",
    "README.md",
    "PHAN_CONG.md",
    "checklists/checklist_c1.md",
    "checklists/review_c1_to_c2_lptomb.md",
    "scratch/checklist_c1_rq1.txt",
    "results/notes/rq1_count_check.txt",
    "slides/slides_final.md",
    "slides/slide_rq1_c1.md",
    "slides/outline.md"
]

def sha256_file(filepath):
    h = hashlib.sha256()
    with open(filepath, 'rb') as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest().upper()

def package():
    if sys.stdout.encoding != 'utf-8':
        try:
            sys.stdout.reconfigure(encoding='utf-8')
        except Exception:
            pass

    print("=" * 75)
    print("      PACKAGE SUBMISSION VALIDATION & ARCHIVE GENERATOR (C3)")
    print("=" * 75)

    missing = []
    for item in REQUIRED_ITEMS:
        if not os.path.exists(item):
            missing.append(item)
            print(f"[MISSING] {item}")
        else:
            size_kb = os.path.getsize(item) / 1024.0
            print(f"[FOUND]   {item:65s} ({size_kb:7.1f} KB)")

    if missing:
        print(f"\n[ERROR] Missing {len(missing)} required deliverable(s)!")
        return False

    os.makedirs("dist", exist_ok=True)
    zip_path = "dist/Nhom1_CSD_DeTai2_Submission_20261028.zip"
    manifest_path = "dist/SUBMISSION_MANIFEST.txt"

    print(f"\n>>> Creating package archive: {zip_path}...")
    manifest_lines = [
        "==========================================================================",
        "          CSD201 FA26 - NHOM 1 - DE TAI 2: SUBMISSION MANIFEST            ",
        "==========================================================================",
        "Project: Hash Table - Tombstone, Rebuild & Robin Hood Hashing",
        "Date: October 28, 2026",
        "Commit Author: nguyendinhtien824-super (Pair 3 / C3)",
        "",
        "FILE MANIFEST & SHA-256 INTEGRITY CHECKSUMS:",
        "-" * 74
    ]

    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk("."):
            # Exclude git, build caches, and temp files
            if any(p in root for p in ['.git', '.vscode', 'build', '__pycache__', 'dist', 'scratch_']):
                continue
            for file in files:
                if file.endswith(('.pyc', '.class', '.tmp', '.zip')):
                    continue
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, ".")
                zipf.write(full_path, rel_path)
                checksum = sha256_file(full_path)
                manifest_lines.append(f"{checksum}  {rel_path}")

    with open(manifest_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(manifest_lines))

    archive_size_mb = os.path.getsize(zip_path) / (1024.0 * 1024.0)
    print(f"\n[SUCCESS] Package created successfully: {zip_path} ({archive_size_mb:.2f} MB)")
    print(f"[SUCCESS] Manifest recorded: {manifest_path}")
    print("=" * 75)
    return True

if __name__ == '__main__':
    success = package()
    sys.exit(0 if success else 1)
