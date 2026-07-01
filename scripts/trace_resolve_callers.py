"""Trace callers of BrMaterialResolver methods (resolve/collectChain/findBase) from spark CSV."""
import csv
import sys
from collections import defaultdict

CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else r'data\render-stacknodes.csv'

nodes = []
with open(CSV_PATH, newline='', encoding='utf-8') as f:
    rdr = csv.DictReader(f)
    for row in rdr:
        cr = row['childRefs'].strip().strip('"')
        if cr in ('', '[]'):
            refs = []
        else:
            refs = [int(x) for x in cr.strip('[]').split(';') if x.strip() != '']
        t0 = float(row['time0'])
        t1 = float(row['time1'])
        nodes.append({
            'idx': int(row['idx']),
            'cls': row['className'],
            'mtd': row['methodName'],
            'total': t0 + t1,
            'refs': refs,
        })

by_idx = {n['idx']: n for n in nodes}
for n in nodes:
    n['self'] = n['total'] - sum(by_idx[r]['total'] for r in n['refs'] if r in by_idx)

parents = defaultdict(list)
for n in nodes:
    for r in n['refs']:
        parents[r].append(n['idx'])

for method in ['resolve', 'collectChain', 'findBase']:
    mns = [n for n in nodes if 'BrMaterialResolver' in n['cls'] and n['mtd'] == method]
    agg = defaultdict(lambda: {'self': 0.0, 'total': 0.0, 'count': 0})
    for mn in mns:
        for pi in parents.get(mn['idx'], []):
            pn = by_idx[pi]
            key = pn['cls'].split('.')[-1] + '.' + pn['mtd']
            agg[key]['self'] += mn['self']
            agg[key]['total'] += mn['total']
            agg[key]['count'] += 1
    print('=== %s (%d nodes, %.0f ms total-self) ===' % (method, len(mns), sum(n['self'] for n in mns)))
    for k, v in sorted(agg.items(), key=lambda kv: kv[1]['self'], reverse=True)[:8]:
        print('  %7.0f self %7.0f total %2d sites  from  %s' % (v['self'], v['total'], v['count'], k))
    print()
