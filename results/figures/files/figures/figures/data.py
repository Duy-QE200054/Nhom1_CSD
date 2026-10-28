import pandas as pd, numpy as np, re
P='/mnt/project/'
def load_rq3(f):
    d=pd.read_csv(P+f)
    d['c']=d.params.str.extract(r'c=(\d+)')[0].astype(int)
    d['alpha']=d.params.str.extract(r'alpha=([\d.]+)')[0].astype(float)
    d['q']=d.run_id.str.extract(r'_(hit|miss)_(?:count|time)$',flags=re.I)[0].str.lower()
    d['mode']=d.run_id.str.extract(r'_(count|time)$',flags=re.I)[0].str.lower()
    d['name']=np.where(d.variant=='noearly','rh-noearly',d.algorithm)
    return d
def rq3_agg(d):
    cnt=d[d['mode']=='count'].groupby(['name','c','q']).ops_count.mean().unstack('q')
    tm=d[d['mode']=='time'].groupby(['name','c','q']).elapsed_ms.median().unstack('q')
    return cnt,tm
def load_rq2():
    d=pd.read_csv(P+'rq2_results_1m.csv',encoding='utf-8-sig')
    d['c']=d.params.str.extract(r'c=(\d+)')[0].astype(int)
    d['q']=np.where(d.params.str.contains('query=miss'),'miss','hit')
    d['mode']=np.where(d.elapsed_ms.notna(),'time','count')
    return d
