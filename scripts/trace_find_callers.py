"""Trace direct callers of BrMaterialResolver.find from spark CSV.

Shows which call sites still invoke find, with their self-time,
so we can see which paths bypass the ModelComponent cache.
"""
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
    s = sum(by_idx[r]['total'] for r in n['refs'] if r in by_idx)
    n['self'] = n['total'] - s

# build parent map
parents = defaultdict(list)
for n in nodes:
    for r in n['refs']:
        parents[r].append(n['idx'])

# find all BrMaterialResolver.find nodes
find_nodes = [n for n in nodes if 'BrMaterialResolver' in n['cls'] and n['mtd'] == 'find']
print('=== BrMaterialResolver.find nodes: %d ===' % len(find_nodes))
find_total_self = sum(n['self'] for n in find_nodes)
print('total find self-time: %.0f ms' % find_total_self)
print()

# aggregate callers of find
caller_agg = defaultdict(lambda: {'self': 0.0, 'count': 0})
for fn in find_nodes:
    pidxs = parents.get(fn['idx'], [])
    if not pidxs:
        print('[no parent] idx=%d self=%.0f cls=%s.%s' % (fn['idx'], fn['self'], fn['cls'], fn['mtd']))
    for pi in pidxs:
        pn = by_idx[pi]
        key = '%s.%s' % (pn['cls'], pn['mtd'])
        caller_agg[key]['self'] += fn['self']
        caller_agg[key]['count'] += 1

print('--- Direct callers of find (aggregated) ---')
for key, v in sorted(caller_agg.items(), key=lambda kv: kv[1]['self'], reverse=True):
    print('%7.0f ms  %2d sites  <- %s' % (v['self'], v['count'], key))

print()
# also show resolve nodes (BrMaterialResolver.resolve)
res_nodes = [n for n in nodes if 'BrMaterialResolver' in n['cls'] and n['mtd'] == 'resolve']
res_self = sum(n['self'] for n in res_nodes)
print('BrMaterialResolver.resolve nodes: %d, total self: %.0f ms' % (len(res_nodes), res_self))

# EyelibLivingEntityRenderer.submit total
submit_nodes = [n for n in nodes if 'EyelibLivingEntityRenderer' in n['cls'] and 'submit' in n['mtd']]
print('EyelibLivingEntityRenderer.submit nodes: %d' % len(submit_nodes))
for sn in submit_nodes:
    print('  total=%.0f self=%.0f' % (sn['total'], sn['self']))
