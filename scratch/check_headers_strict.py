import csv

files = [
    ('RQ1', 'results/raw/rq1_runner_results.csv'),
    ('RQ2', 'results/raw/rq2_results.csv'),
    ('RQ3', 'results/raw/rq3_results.csv'),
]

headers = {}
for name, fpath in files:
    with open(fpath, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        h = next(reader)
        headers[name] = h

print("=" * 85)
print("              STRICT 21-COLUMN CSV SCHEMA HEADER AUDIT (RQ1, RQ2, RQ3)")
print("=" * 85)

print(f"{'STT':^4} | {'RQ1 Header':^24} | {'RQ2 Header':^24} | {'RQ3 Header':^24} | {'Status':^8}")
print("-" * 85)

all_matched = True
for idx in range(21):
    c1 = headers['RQ1'][idx] if idx < len(headers['RQ1']) else "<MISSING>"
    c2 = headers['RQ2'][idx] if idx < len(headers['RQ2']) else "<MISSING>"
    c3 = headers['RQ3'][idx] if idx < len(headers['RQ3']) else "<MISSING>"
    
    match = (c1 == c2 == c3)
    if not match:
        all_matched = False
    status = "OK" if match else "DIFF"
    print(f"{idx+1:^4} | {c1:^24} | {c2:^24} | {c3:^24} | {status:^8}")

print("-" * 85)
print(f"Total Columns: RQ1={len(headers['RQ1'])}, RQ2={len(headers['RQ2'])}, RQ3={len(headers['RQ3'])}")
print(f"Header match across all 3 files: {all_matched}")
assert all_matched and len(headers['RQ1']) == 21
print(">>> RESULT: ALL 3 PRODUCTION CSV FILES MATCH THE 21-COLUMN SCHEMA 100% PERFECTLY! <<<")
print("=" * 85)
