"""Trace callers of Throwable.fillInStackTrace and regex Pattern.compile
in the Render thread profile. These are the top CPU wasters."""
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
        nodes.append({
            'idx': int(row['idx']),
            'cls': row['className'],
            'mtd': row['methodName'],
            'total': float(row['time0']) + float(row['time1']),
            'refs': refs,
        })

by_idx = {n['idx']: n for n in nodes}

# Build reverse index: child idx -> list of parent idx
parents = defaultdict(list)
for n in nodes:
    for r in n['refs']:
        parents[r].append(n['idx'])

def label(n):
    return '%s.%s' % (n['cls'], n['mtd'])

def walk_up(start_idx, max_depth=12):
    """Walk up the parent chain, collecting the call path until we hit
    a non-Throwable/non-exception business frame."""
    chain = []
    cur = start_idx
    seen = set()
    for _ in range(max_depth):
        if cur in seen:
            break
        seen.add(cur)
        ps = parents.get(cur, [])
        if not ps:
            break
        cur = ps[0]  # first parent
        n = by_idx.get(cur)
        if n is None:
            break
        chain.append(n)
    return chain

def is_throwable_frame(n):
    c = n['cls']
    return ('Throwable' in c or 'Exception' in c or 'Error' in c
            or c.endswith('RuntimeException') or 'NoSuchField' in c
            or 'NoSuchMethod' in c or 'ClassNotFound' in c
            or 'InvocationTarget' in c or 'NumberFormatException' in c
            or c == 'java.lang.Object')

def first_business_frame(start_idx):
    """Walk up until first non-throwable, non-reflection frame."""
    cur = start_idx
    seen = set()
    for _ in range(20):
        if cur in seen:
            break
        seen.add(cur)
        ps = parents.get(cur, [])
        if not ps:
            break
        cur = ps[0]
        n = by_idx.get(cur)
        if n is None:
            break
        if not is_throwable_frame(n) and 'reflect' not in n['cls'] and n['cls'] != 'java.lang.Class':
            return n
    return None

# Aggregate fillInStackTrace callers
print('=== THROWABLE.fillInStackTrace caller analysis ===')
fillin_nodes = [n for n in nodes if n['mtd'] == 'fillInStackTrace']
fillin_total = sum(n['total'] for n in fillin_nodes)
print('fillInStackTrace nodes: %d, total time: %.0f ms' % (len(fillin_nodes), fillin_total))
print()

# Group by first business frame
biz_callers = defaultdict(lambda: {'self_time': 0.0, 'samples': 0})
for n in fillin_nodes:
    biz = first_business_frame(n['idx'])
    key = label(biz) if biz else '<unknown>'
    biz_callers[key]['self_time'] += n['total']
    biz_callers[key]['samples'] += 1

print('--- fillInStackTrace grouped by first business caller (top 25) ---')
bc_sorted = sorted(biz_callers.items(), key=lambda kv: kv[1]['self_time'], reverse=True)[:25]
for key, v in bc_sorted:
    print('%7.0f ms  %3d nodes  %s' % (v['self_time'], v['samples'], key))
print()

# Regex compile callers
print('=== java.util.regex.Pattern compile/optimize callers ===')
regex_nodes = [n for n in nodes if n['cls'] == 'java.util.regex.Pattern'
               and n['mtd'] in ('compile', 'atom', 'BnM.optimize', 'optimize', 'match')]
regex_total = sum(n['total'] for n in regex_nodes)
print('regex Pattern nodes: %d, total time: %.0f ms' % (len(regex_nodes), regex_total))
print()
regex_callers = defaultdict(lambda: {'self_time': 0.0, 'samples': 0})
for n in regex_nodes:
    biz = first_business_frame(n['idx'])
    key = label(biz) if biz else '<unknown>'
    regex_callers[key]['self_time'] += n['total']
    regex_callers[key]['samples'] += 1
rc_sorted = sorted(regex_callers.items(), key=lambda kv: kv[1]['self_time'], reverse=True)[:20]
print('--- regex grouped by first business caller (top 20) ---')
for key, v in rc_sorted:
    print('%7.0f ms  %3d nodes  %s' % (v['self_time'], v['samples'], key))
