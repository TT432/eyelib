#!/usr/bin/env python3
"""
将 migrate26Renames 的最后一条规则（\bResourceLocation\b → Identifier）
迁移为源码内的 //? Stonecutter 条件注释。

处理逻辑：
  - 不在任何 //? 块内的 ResourceLocation 行 → 加 //? if <26.1 { ... //?} else { ... //?} 块
  - 在 //? else 分支内（26.1.2 保留）的 ResourceLocation 行 → 直接替换为 Identifier
  - 在 //? if 分支内（26.1.2 删除）的 ResourceLocation 行 → 不改

用法：
  python scripts/migrate_resource_location.py --dry-run   # 预览
  python scripts/migrate_resource_location.py              # 执行
"""

import re
import sys
import argparse
from pathlib import Path


def eval_condition_for_2612(condition: str) -> bool:
    """
    对 26.1.2 版本求值 Stonecutter 条件。
    26.1.2 >= 1.20.6, >= 1.21.1, >= 26.1。
    """
    condition = condition.strip()

    if '&&' in condition:
        parts = condition.split('&&')
        return all(eval_condition_for_2612(p.strip()) for p in parts)
    if '||' in condition:
        parts = condition.split('||')
        return any(eval_condition_for_2612(p.strip()) for p in parts)

    # >= X.Y → True (26.1.2 >= everything)
    if condition.startswith('>='):
        return True
    # < X.Y → False (26.1.2 >= everything)
    if condition.startswith('<'):
        return False

    return True


class StonecutterBlock:
    """跟踪一个 //? if/elif/else 链的状态。"""

    def __init__(self):
        self.branches: list[tuple[str, bool]] = []  # (condition, kept_in_2612)
        self.current = -1

    def add_if(self, condition: str):
        kept = eval_condition_for_2612(condition)
        self.branches = [(condition, kept)]
        self.current = 0

    def add_elif(self, condition: str):
        kept = eval_condition_for_2612(condition)
        self.branches.append((condition, kept))
        self.current = len(self.branches) - 1

    def add_else(self):
        any_kept = any(b[1] for b in self.branches)
        self.branches.append(('else', not any_kept))
        self.current = len(self.branches) - 1

    def current_kept(self) -> bool:
        return self.branches[self.current][1]


# //? 行模式
RE_IF_BLOCK = re.compile(r'^(\s*)//\?\s+if\s+(.+?)\s*\{?\s*$')
RE_ELIF_BLOCK = re.compile(r'^(\s*)//\?\}\s+else\s+if\s+(.+?)\s*\{\s*$')
RE_ELSE_BLOCK = re.compile(r'^(\s*)//\?\}\s+else\s*\{\s*$')
RE_END_BLOCK = re.compile(r'^(\s*)//\?\}\s*$')
RE_SINGLE_LINE_COND = re.compile(r'^(\s*)//\?\s+if\s+(.+?)\s*$')  # 无 { 结尾

RE_RESOURCE_LOCATION = re.compile(r'\bResourceLocation\b')


def process_file(filepath: Path, dry_run: bool) -> bool:
    """处理单个文件，返回是否有改动。"""
    raw = filepath.read_bytes()
    has_crlf = b'\r\n' in raw
    content = raw.decode('utf-8')
    if has_crlf:
        content = content.replace('\r\n', '\n')
    lines = content.split('\n')
    output: list[str] = []

    # //? 块状态栈
    block_stack: list[StonecutterBlock] = []

    # 单行条件：影响下一行
    pending_single_cond: str | None = None
    pending_single_kept: bool | None = None

    i = 0
    changed = False

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        # 处理待定的单行条件
        if pending_single_cond is not None:
            cond = pending_single_cond
            kept = pending_single_kept
            pending_single_cond = None
            pending_single_kept = None

            if RE_RESOURCE_LOCATION.search(line):
                if kept:
                    # 下一行在 26.1.2 保留，需替换
                    new_line = RE_RESOURCE_LOCATION.sub('Identifier', line)
                    if new_line != line:
                        line = new_line
                        changed = True
            output.append(line)
            i += 1
            continue

        # //? if <cond { (块开始)
        m = RE_IF_BLOCK.match(line)
        if m and stripped.endswith('{'):
            cond = m.group(2).rstrip('{').strip()
            block = StonecutterBlock()
            block.add_if(cond)
            block_stack.append(block)
            output.append(line)
            i += 1
            continue

        # //?} else if <cond {
        m = RE_ELIF_BLOCK.match(line)
        if m:
            cond = m.group(2).strip()
            if block_stack:
                block_stack[-1].add_elif(cond)
            output.append(line)
            i += 1
            continue

        # //?} else {
        m = RE_ELSE_BLOCK.match(line)
        if m:
            if block_stack:
                block_stack[-1].add_else()
            output.append(line)
            i += 1
            continue

        # //?}
        m = RE_END_BLOCK.match(line)
        if m:
            if block_stack:
                block_stack.pop()
            output.append(line)
            i += 1
            continue

        # 单行条件（无 { 结尾）
        m = RE_SINGLE_LINE_COND.match(line)
        if m and not stripped.endswith('{'):
            cond = m.group(2).strip()
            output.append(line)
            # 检查下一行是否含 ResourceLocation
            if i + 1 < len(lines):
                next_line = lines[i + 1]
                if RE_RESOURCE_LOCATION.search(next_line):
                    pending_single_cond = cond
                    pending_single_kept = eval_condition_for_2612(cond)
            i += 1
            continue

        # 排除注释行（Javadoc * 或 // 注释，但 //? 是 Stonecutter 指令）
        if stripped.startswith('*') or (stripped.startswith('//') and not stripped.startswith('//?')):
            output.append(line)
            i += 1
            continue

        # 普通 Java 代码行
        if RE_RESOURCE_LOCATION.search(line):
            # 判断当前是否在任何 //? 块内
            if block_stack:
                # 在块内，判断当前分支是否在 26.1.2 保留
                all_kept = all(b.current_kept() for b in block_stack)
                if all_kept:
                    # 26.1.2 保留此行，替换 ResourceLocation → Identifier
                    new_line = RE_RESOURCE_LOCATION.sub('Identifier', line)
                    if new_line != line:
                        line = new_line
                        changed = True
                # else: 26.1.2 删除此行，不改
                output.append(line)
            else:
                # 不在任何 //? 块内，需要加 //? if <26.1 { ... //?} else { ... //?} 块
                indent = re.match(r'^(\s*)', line).group(1)
                replaced = RE_RESOURCE_LOCATION.sub('Identifier', line)
                output.append(f'{indent}//? if <26.1 {{')
                output.append(line)
                output.append(f'{indent}//?}} else {{')
                output.append(replaced)
                output.append(f'{indent}//?}}')
                changed = True
            i += 1
            continue

        output.append(line)
        i += 1

    if changed:
        new_content = '\n'.join(output)
        if has_crlf:
            new_content = new_content.replace('\n', '\r\n')
        if not dry_run:
            filepath.write_bytes(new_content.encode('utf-8'))

    return changed


def main():
    parser = argparse.ArgumentParser(description='Migrate ResourceLocation → Identifier via //? blocks')
    parser.add_argument('--dry-run', action='store_true', help='Preview without writing')
    parser.add_argument('--path', default='src/main/java', help='Source root')
    args = parser.parse_args()

    src_root = Path(args.path)
    if not src_root.exists():
        print(f'Error: {src_root} not found', file=sys.stderr)
        sys.exit(1)

    # 找出所有含 \bResourceLocation\b 的 .java 文件
    changed_files = []
    for filepath in sorted(src_root.rglob('*.java')):
        content = filepath.read_text(encoding='utf-8')
        if not RE_RESOURCE_LOCATION.search(content):
            continue
        # 排除 PortResourceLocation / ResourceLocationBridge 等文件（文件名含但类名不含）
        # 实际上 \bResourceLocation\b 会匹配，但只在真正用到 MC ResourceLocation 时才处理
        if process_file(filepath, args.dry_run):
            changed_files.append(filepath)

    mode = 'DRY-RUN' if args.dry_run else 'APPLIED'
    print(f'[{mode}] {len(changed_files)} files changed:')
    for f in changed_files:
        print(f'  {f}')


if __name__ == '__main__':
    main()
