"""
Raw protobuf decoder for spark sampler data (spark-usercontent.lucko.me/<id>).

不需要 .proto schema,递归尝试解析 length-delimited 字段。
spark 的 SamplerData 结构(经验):
  field 1 = Metadata (跳过)
  field 2 = repeated ThreadNode (每线程一棵 StackNode 树)

StackNode 结构:
  double time(1) / string className(2) / string methodName(3) /
  int32 lineNumber(4) / int32 obfuscated(5) / repeated StackNode children(6)

输出: 每线程的 Top-N 自用时间(self time)和总时间(total time)热点。
"""

import sys
import struct
from collections import defaultdict


def read_varint(buf, pos):
    result = 0
    shift = 0
    while True:
        b = buf[pos]
        pos += 1
        result |= (b & 0x7F) << shift
        if not (b & 0x80):
            break
        shift += 7
    return result, pos


def parse_fields(buf):
    """解析一个 protobuf message 的所有字段,返回 [(field_num, wire_type, value), ...]。"""
    fields = []
    pos = 0
    n = len(buf)
    while pos < n:
        try:
            tag, pos = read_varint(buf, pos)
        except IndexError:
            break
        field_num = tag >> 3
        wire_type = tag & 0x07
        if wire_type == 0:  # varint
            val, pos = read_varint(buf, pos)
            fields.append((field_num, 0, val))
        elif wire_type == 1:  # 64-bit
            val = struct.unpack_from("<d", buf, pos)[0]
            pos += 8
            fields.append((field_num, 1, val))
        elif wire_type == 2:  # length-delimited
            length, pos = read_varint(buf, pos)
            val = buf[pos:pos + length]
            pos += length
            fields.append((field_num, 2, val))
        elif wire_type == 5:  # 32-bit
            val = struct.unpack_from("<f", buf, pos)[0]
            pos += 4
            fields.append((field_num, 5, val))
        else:
            break  # unknown wire type, stop
    return fields


def try_parse_string(b):
    try:
        s = b.decode("utf-8")
        if all(32 <= ord(c) < 127 or c in "\t\n\r" for c in s):
            return s
    except UnicodeDecodeError:
        pass
    return None


def parse_stacknode(buf):
    """解析 StackNode。返回 (time, className, methodName, lineNumber, children)。"""
    fields = parse_fields(buf)
    time = 0.0
    class_name = ""
    method_name = ""
    line_number = 0
    children = []
    for fn, wt, val in fields:
        if fn == 1 and wt == 1:
            time = val
        elif fn == 2 and wt == 2:
            s = try_parse_string(val)
            if s is not None:
                class_name = s
        elif fn == 3 and wt == 2:
            s = try_parse_string(val)
            if s is not None:
                method_name = s
        elif fn == 4:
            line_number = val
        elif fn == 6 and wt == 2:
            children.append(parse_stacknode(val))
    return (time, class_name, method_name, line_number, children)


def parse_threadnode(buf):
    """解析 ThreadNode。返回 (name, [StackNode])。"""
    fields = parse_fields(buf)
    name = ""
    stacks = []
    for fn, wt, val in fields:
        if fn == 1 and wt == 2:
            s = try_parse_string(val)
            if s is not None:
                name = s
        elif fn == 2 and wt == 2:
            stacks.append(parse_stacknode(val))
    return name, stacks


def aggregate_self(node, acc):
    """递归累计每个栈帧的 self time(= node.time - sum(children.time))。"""
    time, cls, method, line, children = node
    children_total = sum(c[0] for c in children)
    self_time = time - children_total
    key = f"{cls}.{method}"
    acc[key] += self_time
    for c in children:
        aggregate_self(c, acc)


def aggregate_total(node, acc):
    """递归累计每个栈帧的 total time(包含子调用)。"""
    time, cls, method, line, children = node
    key = f"{cls}.{method}"
    acc[key] += time
    for c in children:
        aggregate_total(c, acc)


def walk_all_frames(node, out):
    """把每个栈帧平铺成 (key, time)。"""
    time, cls, method, line, children = node
    out.append((f"{cls}.{method}", time))
    for c in children:
        walk_all_frames(c, out)


def main():
    if len(sys.argv) < 2:
        print("usage: spark_proto_decode.py <protobuf.bin>")
        sys.exit(1)
    path = sys.argv[1]
    with open(path, "rb") as f:
        buf = f.read()
    print(f"file size: {len(buf)} bytes")

    top = parse_fields(buf)
    metadata_raw = None
    threads_raw = []
    for fn, wt, val in top:
        if fn == 1 and wt == 2:
            metadata_raw = val
        elif fn == 2 and wt == 2:
            threads_raw.append(val)
        else:
            print(f"top field {fn} wire_type {wt} (skipped, len={len(val) if wt==2 else '-'})")

    print(f"metadata size: {len(metadata_raw) if metadata_raw else 0}")
    print(f"thread nodes: {len(threads_raw)}")

    if not threads_raw:
        print("NO thread data found in protobuf")
        return

    all_threads_summary = []
    for tbuf in threads_raw:
        name, stacks = parse_threadnode(tbuf)
        if not stacks:
            continue
        # roots 的 time 总和 = 该线程总采样时间(占比)
        total_root = sum(s[0] for s in stacks)
        all_threads_summary.append((name, total_root, stacks))

    all_threads_summary.sort(key=lambda x: -x[1])
    print("\n=== threads (by total time) ===")
    for name, total, stacks in all_threads_summary:
        print(f"  {name}: total_time={total:.4f} roots={len(stacks)}")

    # 对每个线程打印 self-time Top-N
    for name, total, stacks in all_threads_summary:
        if "Render" not in name and "render" not in name.lower():
            continue
        print(f"\n=== [{name}] self-time Top 25 ===")
        self_acc = defaultdict(float)
        for root in stacks:
            aggregate_self(root, self_acc)
        items = sorted(self_acc.items(), key=lambda x: -x[1])
        for key, t in items[:25]:
            pct = (t / total * 100) if total > 0 else 0
            print(f"  {t:8.4f} ({pct:5.1f}%)  {key}")

        print(f"\n=== [{name}] total-time Top 25 ===")
        tot_acc = defaultdict(float)
        for root in stacks:
            aggregate_total(root, tot_acc)
        items = sorted(tot_acc.items(), key=lambda x: -x[1])
        for key, t in items[:25]:
            pct = (t / total * 100) if total > 0 else 0
            print(f"  {t:8.4f} ({pct:5.1f}%)  {key}")


if __name__ == "__main__":
    main()
