import csv

print("=== CHECK TABLE 1 SOURCES ===")
with open('results/raw/rq1_runner_results_lab.csv', 'r', encoding='utf-8') as f:
    lab_rows = list(csv.DictReader(f))

with open('results/raw/rq1_driver_results.csv', 'r', encoding='utf-8') as f:
    driver_rows = list(csv.DictReader(f))

alphas = [0.50, 0.75, 0.90, 0.95]
print("ALPHA | LP HIT (LAB) | LP MISS (DRIVER) | SC HIT (LAB) | SC MISS (DRIVER)")
for a in alphas:
    # lab
    lp_l = next(r for r in lab_rows if r['algorithm'] == 'lp-tomb' and r['dataset'] == 'uniform' and f'alpha={a:.2f}' in r['params'].lower() and 'm=1048576' in r['params'].lower() and r['ops_count'])
    sc_l = next(r for r in lab_rows if r['algorithm'] == 'sc' and r['dataset'] == 'uniform' and f'alpha={a:.2f}' in r['params'].lower() and 'm=1048576' in r['params'].lower() and r['ops_count'])
    
    # driver
    lp_d = next(r for r in driver_rows if r['algo'] == 'lp-tomb' and r['dataset'] == 'uniform' and float(r['alpha']) == a and int(r['m']) == 1048576)
    sc_d = next(r for r in driver_rows if r['algo'] == 'sc' and r['dataset'] == 'uniform' and float(r['alpha']) == a and int(r['m']) == 1048576)
    
    lp_hit = float(lp_l['ops_count'])
    sc_hit = float(sc_l['ops_count'])
    lp_miss = float(lp_d['absent_probes'])
    sc_miss = float(sc_d['absent_probes'])
    
    print(f"{a:4.2f}  |   {lp_hit:6.3f}   |      {lp_miss:7.3f}   |   {sc_hit:6.3f}   |    {sc_miss:6.3f}")
