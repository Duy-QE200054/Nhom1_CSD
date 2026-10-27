import csv

with open('results/raw/rq2_results.csv', mode='r', encoding='utf-8') as f:
    rows = list(csv.DictReader(f))

data = {}
for r in rows:
    params = dict(p.split('=') for p in r['params'].split(';') if '=' in p)
    c = int(params.get('c', 0))
    q = params.get('query', 'hit')
    v = r['variant']
    key = (c, v, q)
    if key not in data:
        data[key] = {}
    if r['ops_count']:
        data[key]['probes'] = float(r['ops_count'])
    if r['elapsed_ms']:
        data[key]['elapsed_ms'] = float(r['elapsed_ms'])

milestones = [0, 262144, 524288, 1048576, 2097152, 4194304]

print(f"{'C':<8} | {'Base Hit P':<10} | {'Clean Hit P':<11} | {'Base Miss P':<11} | {'Clean Miss P':<12} | {'Base Hit ms':<11} | {'Clean Hit ms':<12} | {'Base Miss ms':<12} | {'Clean Miss ms':<13}")
print("-" * 120)
for c in milestones:
    bh_p = data[(c, 'base', 'hit')]['probes']
    ch_p = data[(c, 'clean_table', 'hit')]['probes']
    bm_p = data[(c, 'base', 'miss')]['probes']
    cm_p = data[(c, 'clean_table', 'miss')]['probes']

    bh_t = data[(c, 'base', 'hit')]['elapsed_ms']
    ch_t = data[(c, 'clean_table', 'hit')]['elapsed_ms']
    bm_t = data[(c, 'base', 'miss')]['elapsed_ms']
    cm_t = data[(c, 'clean_table', 'miss')]['elapsed_ms']

    print(f"{c:<8} | {bh_p:<10.4f} | {ch_p:<11.4f} | {bm_p:<11.4f} | {cm_p:<12.4f} | {bh_t:<11.2f} | {ch_t:<12.2f} | {bm_t:<12.2f} | {cm_t:<13.2f}")
