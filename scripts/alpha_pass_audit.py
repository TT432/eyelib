#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Static audit: per Bedrock entity/material/bone, which draw pass does a face get and
does its texture region contain fully transparent texels?

Purpose: a face whose texture region has alpha==0 texels but is drawn by a pass that does
NOT alpha-test (discard) writes depth over those texels. On JE that hidden depth masks the
later translucent block layer (water) in the frame composite, and between overlapping
translucent faces it makes layer order depend on submission order (flicker that looks like
z-fighting). See docs/decisions/0042-entity-pass-state-policy.md.

Sampling is bilinear along the quad's (s,t) parameterisation, i.e. exactly the texels the
renderer writes, so atlas gutters between UV islands are not counted.

Usage:
  python scripts/alpha_pass_audit.py --pack <path/to/pack.cospack> \
      --material <a.material> [--material <b.material> ...]

Requires Pillow.
"""
import argparse
import base64
import io
import json
import os
import sys

from PIL import Image


def load_materials(paths):
    raw = {}
    for p in paths:
        with open(p, encoding='utf-8') as f:
            raw.update(json.load(f).get('materials', {}))
    return raw


def split_key(key):
    return key.split(':', 1) if ':' in key else (key, '')


def resolve(raw, name, seen=None):
    """Resolve a Bedrock material short name through its base chain."""
    seen = seen or set()
    if name in seen:
        return {'kind': 'SOLID', 'found': False, 'defines': set(), 'states': set(),
                'write_depth': True, 'cull': True}
    seen.add(name)
    entry, key_name = None, None
    if name in raw:
        entry, key_name = raw[name], name
    else:
        for k, v in raw.items():
            if split_key(k)[0] == name:
                entry, key_name = v, k
                break
    if entry is None:
        return {'kind': 'SOLID', 'found': False, 'defines': set(), 'states': set(),
                'write_depth': True, 'cull': True}
    base_name = entry.get('base') or split_key(key_name)[1]
    parent = resolve(raw, base_name, seen) if base_name and base_name != name else \
        {'defines': set(), 'states': set()}
    defines = set(parent.get('defines', ()))
    states = set(parent.get('states', ()))
    for d in entry.get('defines', []) or []:
        defines.add(d)
    for d in entry.get('+defines', []) or []:
        defines.add(d)
    for d in entry.get('-defines', []) or []:
        defines.discard(d)
    for s in entry.get('states', []) or []:
        states.add(s)
    for s in entry.get('+states', []) or []:
        states.add(s)
    for s in entry.get('-states', []) or []:
        states.discard(s)
    blending = 'Blending' in states
    src = entry.get('blendSrc') or parent.get('blendSrc') or 'SourceAlpha'
    dst = entry.get('blendDst') or parent.get('blendDst') or 'OneMinusSrcAlpha'
    additive = blending and (src, dst) == ('One', 'One')
    alpha_test = 'ALPHA_TEST' in defines or 'TINTED_ALPHA_TEST' in defines
    kind = 'ADDITIVE' if additive else 'BLEND' if blending else 'ALPHA_TEST' if alpha_test else 'SOLID'
    return {'kind': kind, 'found': True, 'defines': defines, 'states': states,
            'write_depth': 'DisableDepthWrite' not in states, 'cull': 'DisableCulling' not in states,
            'src': src, 'dst': dst}


def sample_quad(img, uvs, res=24):
    """Bilinear sample the quad's (s,t) surface; returns alpha values."""
    w, h = img.size
    px = img.load()
    out = []
    for i in range(res):
        s = (i + 0.5) / res
        for j in range(res):
            t = (j + 0.5) / res
            u = ((1 - s) * (1 - t) * uvs[0][0] + s * (1 - t) * uvs[1][0]
                 + s * t * uvs[2][0] + (1 - s) * t * uvs[3][0])
            v = ((1 - s) * (1 - t) * uvs[0][1] + s * (1 - t) * uvs[1][1]
                 + s * t * uvs[2][1] + (1 - s) * t * uvs[3][1])
            x = min(w - 1, max(0, int(u * w)))
            y = min(h - 1, max(0, int(v * h)))
            p = px[x, y]
            out.append(p[3] if len(p) == 4 else 255)
    return out


def mat_short(expr, arrays):
    """Resolve a render-controller material expression to a description.materials key."""
    if isinstance(expr, list):
        expr = expr[0] if expr else None
    if not isinstance(expr, str):
        return None
    s = expr.strip()
    if s.startswith('Array.') and '[' in s:
        arr_name = s[len('Array.'):s.index('[')]
        vals = []
        for v in (arrays or {}).values():
            if isinstance(v, dict) and ('Array.' + arr_name) in v:
                vals = v['Array.' + arr_name]
                break
            if isinstance(v, list) and isinstance(arrays, dict) and v is arrays.get('Array.' + arr_name):
                vals = v
                break
        return mat_short(vals[0], arrays) if vals else None
    if 'Material.' in s:
        return s.split('Material.', 1)[1].strip()
    return None


def audit(pack_path, material_paths):
    raw = load_materials(material_paths)
    with open(pack_path, encoding='utf-8') as f:
        pack = json.load(f)

    rows = []
    for e in pack['entries']:
        ce = e['entity']['minecraft:client_entity']['description']
        mats = ce.get('materials', {})
        texs = ce.get('textures', {})
        imgs = {p: Image.open(io.BytesIO(base64.b64decode(b))).convert('RGBA')
                for p, b in e['textures'].items()}
        for geom in e['models']:
            for bone in geom['all_bones'].values():
                bone['_mat'] = None
        for rc in (e.get('renderControllers') or {}).values():
            arrays = rc.get('arrays') or {}
            for slot in rc.get('materials') or []:
                for pattern, expr in slot.items():
                    short = mat_short(expr, arrays)
                    if not short:
                        continue
                    prefix = pattern[:-1] if pattern.endswith('*') else pattern
                    for geom in e['models']:
                        for bone_name, bone in geom['all_bones'].items():
                            hit = (pattern == '*' or bone_name == pattern
                                   or (pattern.endswith('*') and (not prefix or bone_name.startswith(prefix))))
                            if hit:
                                bone['_mat'] = short

        agg = {}
        for geom in e['models']:
            for bone_name, bone in geom['all_bones'].items():
                short = bone.get('_mat') or mats.get('default')
                mat_name = mats.get(short, short) if short else None
                if not mat_name:
                    continue
                st = resolve(raw, mat_name)
                path = texs.get('default') or next(iter(texs.values()), None)
                if not path:
                    continue
                path = path if path.endswith('.png') else path + '.png'
                img = imgs.get(path)
                if img is None:
                    continue
                key = (mat_name, st['kind'], st['write_depth'], st['cull'])
                r = agg.setdefault(key, {'faces': 0, 'holey_faces': 0, 'transparent': 0, 'px': 0,
                                         'bones': {}})
                for cube in bone.get('cubes') or []:
                    for face in cube.get('faces') or []:
                        vs = face['vertexes']
                        if len(vs) != 4:
                            continue
                        alphas = sample_quad(img, [v['uv'] for v in vs])
                        r['faces'] += 1
                        r['px'] += len(alphas)
                        zeros = sum(1 for a in alphas if a < 26)
                        r['transparent'] += zeros
                        if zeros > 0.02 * len(alphas):
                            r['holey_faces'] += 1
                            r['bones'][bone_name] = r['bones'].get(bone_name, 0) + zeros
        for (mat, kind, wd, cull), r in sorted(agg.items()):
            if r['transparent'] == 0:
                continue
            top = sorted(r['bones'].items(), key=lambda kv: -kv[1])[:4]
            rows.append({
                'entity': e['identifier'], 'material': mat, 'kind': kind,
                'write_depth': wd, 'cull': cull,
                'faces': r['faces'], 'holey_faces': r['holey_faces'],
                'transparent_px': round(r['transparent'] / r['px'], 4),
                'top_bones': [b for b, _ in top],
            })
    return rows


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--pack', required=True, help='pack.cospack 路径')
    ap.add_argument('--material', action='append', default=[], help='.material 文件（可多次）')
    ap.add_argument('--json', help='把结果写到该 JSON 文件')
    args = ap.parse_args(argv)

    rows = audit(args.pack, args.material)
    print(f"{'entity':26} {'material':32} {'kind':10} {'wd':5} {'cull':5} "
          f"{'faces':6} {'holey':6} {'alpha0%':7} top bones")
    for r in rows:
        print(f"{r['entity']:26} {r['material']:32} {r['kind']:10} {str(r['write_depth']):5} "
              f"{str(r['cull']):5} {r['faces']:6} {r['holey_faces']:6} {r['transparent_px']:<7} "
              f"{','.join(r['top_bones'])}")
    if args.json:
        with open(args.json, 'w', encoding='utf-8') as f:
            json.dump(rows, f, indent=1, ensure_ascii=False)
    return 0


if __name__ == '__main__':
    sys.exit(main())
