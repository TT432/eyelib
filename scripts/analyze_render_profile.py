"""Analyze spark Render thread profile (CSV exported from protobuf).

Computes self-time and total-time per node, reports Top-N for:
  - self-time (where CPU actually burns)
  - total-time (hot call paths)
  - eyelib.* breakdown (the optimization target)
"""
import csv
import sys
from collections import defaultdict

CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else r'data\render-stacknodes.csv'
TOPN = int(sys.argv[2]) if len(sys.argv) > 2 else 30

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

# index by idx
by_idx = {n['idx']: n for n in nodes}

# self time = total - sum(children totals)
for n in nodes:
    s = 0.0
    for r in n['refs']:
        c = by_idx.get(r)
        if c is not None:
            s += c['total']
    n['self'] = n['total'] - s

root = nodes[-1]  # last = root (rootRefs=[20931], pool size 20932)
total_time = root['total']

def short(cls):
    return cls

def label(n):
    return '%s.%s' % (short(n['cls']), n['mtd'])

print('=== Render thread profile ===')
print('nodes: %d, root total: %.0f ms (time0=%.0f time1=%.0f)' % (len(nodes), total_time, root['total'], 0))
print()

# Top-N self-time
print('--- TOP %d SELF-TIME (ms) ---' % TOPN)
top_self = sorted(nodes, key=lambda n: n['self'], reverse=True)[:TOPN]
for i, n in enumerate(top_self):
    pct = n['self'] / total_time * 100
    print('%2d. %7.1f (%5.1f%%)  %s' % (i+1, n['self'], pct, label(n)))
print()

# Top-N total-time
print('--- TOP %d TOTAL-TIME (ms) ---' % TOPN)
top_total = sorted(nodes, key=lambda n: n['total'], reverse=True)[:TOPN]
for i, n in enumerate(top_total):
    pct = n['total'] / total_time * 100
    print('%2d. %7.1f (%5.1f%%)  %s' % (i+1, n['total'], pct, label(n)))
print()

# eyelib breakdown by class
eyelib_classes = defaultdict(lambda: {'self': 0.0, 'total': 0.0, 'count': 0})
eyelib_methods = []
for n in nodes:
    if 'tt432.eyelib' in n['cls']:
        pkg = n['cls']
        eyelib_classes[pkg]['self'] += n['self']
        eyelib_classes[pkg]['total'] = max(eyelib_classes[pkg]['total'], n['total'])
        eyelib_classes[pkg]['count'] += 1
        eyelib_methods.append(n)

print('--- EYELIB SELF-TIME BY CLASS (top 20) ---')
ec_sorted = sorted(eyelib_classes.items(), key=lambda kv: kv[1]['self'], reverse=True)[:20]
for cls, v in ec_sorted:
    print('%7.1f self  %7.1f maxtotal  %3d nodes  %s' % (v['self'], v['total'], v['count'], cls))
print()

print('--- EYELIB TOP 20 SELF-TIME METHODS ---')
em_sorted = sorted(eyelib_methods, key=lambda n: n['self'], reverse=True)[:20]
for n in em_sorted:
    pct = n['self'] / total_time * 100
    print('%7.1f (%5.1f%%)  %s.%s' % (n['self'], pct, n['cls'], n['mtd']))
print()

# eyelib aggregate
eyelib_self_total = sum(n['self'] for n in eyelib_methods)
print('EYELIB total self-time: %.1f ms (%.1f%% of %.0fms)' % (eyelib_self_total, eyelib_self_total/total_time*100, total_time))
