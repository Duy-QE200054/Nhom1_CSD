import numpy as np, pandas as pd, matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from data import *
plt.rcParams.update({'font.family':'serif','font.size':8,'axes.titlesize':8.5,'axes.labelsize':8,'legend.fontsize':7,
 'xtick.labelsize':7.5,'ytick.labelsize':7.5,'axes.grid':True,'grid.alpha':.3,'grid.linewidth':.5,'lines.linewidth':1.3,
 'lines.markersize':4.5,'figure.dpi':150,'savefig.dpi':300,'savefig.bbox':'tight','axes.spines.top':False,'axes.spines.right':False})
CL={'lp-tomb':'#d62728','lp-backshift':'#2ca02c','rh-noearly':'#ff7f0e','rh':'#1f77b4','sc':'#1f77b4','lp':'#d62728'}
MK={'lp-tomb':'D','lp-backshift':'^','rh-noearly':'s','rh':'o'}
LB={'lp-tomb':'LP Tombstone (rebuild)','lp-backshift':'LP Backshift (Knuth R)','rh-noearly':'Robin Hood (rh-noearly)','rh':'Robin Hood (rh, early exit)'}
ORDER=['lp-tomb','lp-backshift','rh-noearly','rh']
def save(fig,name):
    fig.savefig(f'figs/{name}.png'); fig.savefig(f'figs/{name}.pdf'); plt.close(fig)

# ---------- RQ1 ----------
d=pd.read_csv(P+'rq1_driver_results.csv')
u=d[(d.dataset=='uniform')&(d.m==1048576)&(d.algo.isin(['sc','lp-tomb']))]
al=np.linspace(0.45,0.96,100)
th={'lp_hit':.5*(1+1/(1-al)),'lp_miss':.5*(1+1/(1-al)**2),'sc_hit':1+al/2,'sc_miss':al}
fig,ax=plt.subplots(1,2,figsize=(3.5,2.4))
for a,(q,col,t) in zip(ax,[('present_probes','Hit (successful)','hit'),('absent_probes','Miss (unsuccessful)','miss')]):
    for alg,key in [('sc','sc'),('lp-tomb','lp')]:
        x=u[u.algo==alg].sort_values('alpha')
        a.plot(al,th[f'{key}_{t}'],'-',color=CL[key],lw=1,alpha=.7)
        a.plot(x.alpha,x[q],'o' if alg=='sc' else 's',color=CL[key],ls='none',label=f"{'SC' if alg=='sc' else 'LP'} measured")
    a.set_yscale('log'); a.set_xlabel(r'Load factor $\alpha$',fontsize=7); a.set_title(f'{col}',fontsize=7)
a=ax[0]; a.set_ylabel('Mean probes per lookup')
h,l=a.get_legend_handles_labels(); from matplotlib.lines import Line2D
h.append(Line2D([],[],color='gray',lw=1)); l.append("Knuth formula (solid)")
a.legend(h,l,loc='upper left',fontsize=5.3)
fig.tight_layout(); save(fig,'fig1_probes_vs_alpha')

fig,ax=plt.subplots(1,2,figsize=(3.5,2.4))
for a,(q,t) in zip(ax,[('present_ns','Hit (successful)'),('absent_ns','Miss (unsuccessful)')]):
    for alg,lab,mk in [('sc','SC','o'),('lp-tomb','LP-tomb','s')]:
        x=u[u.algo==alg].sort_values('alpha'); a.plot(x.alpha,x[q],marker=mk,color=CL['sc' if alg=='sc' else 'lp'],label=lab)
    a.set_xlabel(r'Load factor $\alpha$',fontsize=7); a.set_title(f'{t}',fontsize=7); a.set_ylim(bottom=0)
ax[0].set_ylabel('Lookup latency (ns/op)'); ax[0].legend(loc='upper left',fontsize=6)
fig.tight_layout(); save(fig,'fig2_time_vs_alpha')

# memory
fig,a=plt.subplots(figsize=(3.4,2.6))
al2=np.linspace(0.45,0.96,50)
a.plot(al2,(32*al2+4)/al2*1/1,'-',color=CL['sc'],lw=.9,alpha=.6,label=r'SC model: $(32\alpha+4)/\alpha$ B')
a.plot(al2,13/al2,'-',color=CL['lp'],lw=.9,alpha=.6,label=r'LP model: $13/\alpha$ B')
for alg,mk in [('sc','o'),('lp-tomb','s')]:
    x=u[u.algo==alg].sort_values('alpha'); a.plot(x.alpha,x.bytes_per_key,mk,color=CL['sc' if alg=='sc' else 'lp'],ls='none',label=f"{'SC' if alg=='sc' else 'LP-tomb'} measured")
a.set_xlabel(r'Load factor $\alpha$'); a.set_ylabel('Heap bytes per key'); a.set_ylim(0,None); a.legend(fontsize=6.3)
fig.tight_layout(); save(fig,'fig5_memory')

# ---------- RQ2 ----------
r2=load_rq2()
cs=[0,262144,524288,1048576,2097152,4194304]; xl=['0','0.25M','0.5M','1M','2M','4M']
cnt=r2[r2['mode']=='count'].set_index(['variant','q','c']).ops_count
tm=r2[r2['mode']=='time'].set_index(['variant','q','c']).elapsed_ms
# TOMB/EMPTY not recorded in the CSVs -> values from paper Table II
TOMB=[0.00,18.79,30.16,41.91,48.64,49.96]; EMP=[50.00,31.21,19.84,8.09,1.36,0.04]
fig,ax=plt.subplots(1,3,figsize=(7.2,2.4)); xs=np.arange(6)
a=ax[0]; a.fill_between(xs,TOMB,color='#f4b6b6',alpha=.6); a.fill_between(xs,EMP,color='#b9d3ea',alpha=.6)
a.plot(xs,TOMB,'o-',color=CL['lp-tomb'],ms=3,label='TOMB'); a.plot(xs,EMP,'s-',color=CL['rh'],ms=3,label='EMPTY'); a.axhline(50,color='#2ca02c',ls='--',lw=1,label='LIVE keys (50%)')
a.set_title('(a) Slot contamination'); a.set_ylabel('Slots (% of $M$)'); a.set_ylim(0,62); a.legend(loc='upper center',fontsize=6,ncol=1,bbox_to_anchor=(0.5,0.82))
a=ax[1]
a.plot(xs,[cnt['base','miss',c] for c in cs],'o-',color=CL['lp-tomb'],label='Contaminated'); a.plot(xs,[cnt['clean_table','miss',c] for c in cs],'s--',color='#2ca02c',label='Clean table')
a.axhline(2.5,color='k',ls=':',lw=.8,label='Knuth bound (2.50)'); a.set_yscale('log')
ratio=cnt['base','miss',4194304]/cnt['clean_table','miss',4194304]
a.set_title(f'(b) Miss probes ({ratio:,.1f}$\\times$ at $C$=4M)'); a.set_ylabel('Mean probes (log)'); a.legend(fontsize=6)
a=ax[2]
a.plot(xs,[tm['base','miss',c] for c in cs],'o-',color=CL['lp-tomb'],label='Contaminated'); a.plot(xs,[tm['clean_table','miss',c] for c in cs],'s--',color='#2ca02c',label='Clean table')
a.axhline(60000,color='k',ls=':',lw=.8,label='Process timeout (60 s)'); a.set_yscale('log'); a.set_title('(c) Miss-lookup latency'); a.set_ylabel('1M-query batch (ms, log)'); a.legend(fontsize=6,loc='center left')
for a in ax: a.set_xticks(xs); a.set_xticklabels(xl,fontsize=6.3); a.set_xlabel('Churn cycles $C$')
fig.tight_layout(); save(fig,'fig_rq2_tombstone_dynamics')

# ---------- RQ3 ----------
CC=[0,262144,524288,1048576,2097152,4194304]; cl=['0','256K','512K','1024K','2048K','4096K']
def churn_plots(f,tag,N,title_a):
    d=load_rq3(f); c,t=rq3_agg(d)
    fig,ax=plt.subplots(1,2,figsize=(7.2,2.6))
    for a,q,tt in zip(ax,['hit','miss'],['(a) Hit queries','(b) Miss queries']):
        for n in ORDER:
            a.plot(xs,[c.loc[(n,cc),q] for cc in CC],marker=MK[n],color=CL[n],label=LB[n],ls=':' if n=='lp-tomb' else ('--' if n=='rh-noearly' else '-'),alpha=.9)
        a.set_xticks(xs); a.set_xticklabels(cl,fontsize=6.5); a.set_xlabel(f'Churn cycles $C$ ($N$={N:,})'); a.set_title(tt)
    ax[0].set_ylabel('Mean probes per query'); ax[0].legend(fontsize=6.3)
    fig.tight_layout(); save(fig,f'fig_rq3_probes_vs_churn_{tag}')
    return d,c,t
d50,c50,t50=churn_plots('rq3_20261024.csv','a050',524288,'')
d80,c80,t80=churn_plots('rq3_20261025.csv','a080',838861,'')

# latency (alpha=0.5) with per-seed spread
fig,ax=plt.subplots(1,2,figsize=(7.2,2.6))
raw=d50[d50['mode']=='time']
for a,q,tt in zip(ax,['hit','miss'],['(a) Hit lookups','(b) Miss lookups']):
    for n in ORDER:
        a.plot(xs,[t50.loc[(n,cc),q] for cc in CC],marker=MK[n],color=CL[n],label=LB[n],ls=':' if n=='lp-tomb' else ('--' if n=='rh-noearly' else '-'))
    a.set_xticks(xs); a.set_xticklabels(cl,fontsize=6.5); a.set_xlabel(r'Churn cycles $C$ ($N$=524,288, $M=2^{20}$)'); a.set_title(tt)
ax[0].set_ylabel('Lookup time (ms, median of 3 seeds)'); ax[0].legend(fontsize=6.3,loc='upper left')
fig.tight_layout(); save(fig,'fig_rq3_lookup_latency')

# mechanism decomposition alpha .5 : mean across checkpoints & seeds
m=d50[d50['mode']=='count'].groupby(['name','q']).ops_count.mean().unstack()
fig,a=plt.subplots(figsize=(3.6,2.7)); w=.2
for i,n in enumerate(ORDER):
    xx=np.arange(2)+(i-1.5)*w; v=[m.loc[n,'hit'],m.loc[n,'miss']]
    b=a.bar(xx,v,w,color=CL[n],label=LB[n],edgecolor='k',lw=.4)
    for x_,v_ in zip(xx,v): a.text(x_,v_+.05,f'{v_:.2f}',ha='center',fontsize=6)
a.set_xticks([0,1]); a.set_xticklabels(['Hit queries','Miss queries']); a.set_ylabel('Mean probes per lookup'); a.set_ylim(0,4.9); a.legend(fontsize=6,loc='upper left')
red=1-m.loc['rh','miss']/m.loc['rh-noearly','miss']
a.annotate(f'Early exit: {red*100:.1f}% fewer\nmiss probes',xy=(1.3,m.loc['rh','miss']),xytext=(0.55,3.3),fontsize=6.5,arrowprops=dict(arrowstyle='->',lw=.7))
fig.tight_layout(); save(fig,'fig_rq3_mechanism_ablation')

# rebuild spikes
fig,ax=plt.subplots(2,1,figsize=(7.2,4.6),sharex=True)
for a,(s,tau) in zip(ax,[('050',0.20),('080',0.10)]):
    b=pd.read_csv(P+f'rq3_block_profiling_alpha{s}.csv')
    for n,col,lw in [('lp-backshift','#2ca02c',.7),('rh','#1f77b4',.7),('lp-tomb','#d62728',.8)]:
        x=b[b.algorithm==n]; a.plot(x.block_index,x.elapsed_ms,color=col,lw=lw,alpha=.85,label={'lp-backshift':'LP Backshift','rh':'Robin Hood (full)','lp-tomb':'LP Tombstone (periodic rebuild)'}[n])
    x=b[(b.algorithm=='lp-tomb')&(b.rebuilds_in_block>0)]
    a.scatter(x.block_index,x.elapsed_ms,s=16,color='darkred',zorder=5,label=f'Rebuild event ({len(x)} blocks, max={x.elapsed_ms.max():.1f} ms)')
    a.set_ylim(0,130); a.set_ylabel('Block time (ms / $10^4$ ops)'); a.set_title(rf'$\alpha$={int(s)/100:.2f}, $\tau$={tau:.2f}, $M=2^{{20}}$'); a.legend(fontsize=6,loc='upper right',ncol=2)
ax[1].set_xlabel('Block index (1 block = $10^4$ operations, $C$ = 0..4M)')
fig.tight_layout(); save(fig,'fig_rq3_rebuild_spikes')

# mechanism breakdown (C=4M)
ws=pd.read_csv(P+'rq3_workload_summary.csv')
fig,ax=plt.subplots(1,3,figsize=(7.2,2.4))
a=ax[0]; 
for i,(lf,col) in enumerate([(0.5,'#4472c4'),(0.8,'#d62728')]):
    s=ws[ws.load_factor==lf].set_index('algorithm')
    a.bar(np.arange(3)+(i-.5)*.38,[s.loc[n,'rebuild_count'] for n in ['lp-tomb','lp-backshift','rh']],.38,color=col,label=rf'$\alpha$={lf:.2f}')
a.set_xticks(range(3)); a.set_xticklabels(['LP-tomb','LP-backshift','RH'],fontsize=6.5); a.set_ylabel('Rebuilds in 4M cycles'); a.set_title('(a) Mech. 1: rebuilds'); a.legend(fontsize=6)
a=ax[1]
for i,(lf,col) in enumerate([(0.5,'#4472c4'),(0.8,'#d62728')]):
    s=ws[(ws.load_factor==lf)].set_index('algorithm')
    a.bar(np.arange(2)+(i-.5)*.38,[s.loc['lp-backshift','max_dib'],s.loc['rh','max_dib']],.38,color=col,label=rf'$\alpha$={lf:.2f}')
    for j,n in enumerate(['lp-backshift','rh']): a.text(j+(i-.5)*.38,s.loc[n,'max_dib']+3,int(s.loc[n,'max_dib']),ha='center',fontsize=6)
a.set_xticks(range(2)); a.set_xticklabels(['LP-backshift\n(no DIB swap)','RH\n(DIB swap)'],fontsize=6.5); a.set_ylabel('Max DIB at $C$=4M'); a.set_title('(b) Mech. 2: tail displacement')
a=ax[2]
for i,(cc_,lf,col) in enumerate([(c50,0.5,'#4472c4'),(c80,0.8,'#d62728')]):
    v=[cc_.loc[('rh-noearly',4194304),'miss'],cc_.loc[('rh',4194304),'miss']]
    a.bar(np.arange(2)+(i-.5)*.38,v,.38,color=col,label=rf'$\alpha$={lf:.2f}')
    for j,v_ in enumerate(v): a.text(j+(i-.5)*.38,v_+.2,f'{v_:.2f}',ha='center',fontsize=6)
a.set_xticks(range(2)); a.set_xticklabels(['rh-noearly\n(no early exit)','rh\n(early exit)'],fontsize=6.5); a.set_ylabel('Mean miss probes at $C$=4M'); a.set_title('(c) Mech. 3: early exit')
fig.tight_layout(); save(fig,'fig_rq3_mechanism_breakdown')
print('early exit reduction a0.5 C=4M',1-c50.loc[('rh',4194304),'miss']/c50.loc[('rh-noearly',4194304),'miss'],'a0.8',1-c80.loc[('rh',4194304),'miss']/c80.loc[('rh-noearly',4194304),'miss'])
print('mean-over-ckpt reduction a0.5',red)
