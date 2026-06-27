#!/usr/bin/env python3
"""
迁移 @EventBusSubscriber 注解差异到源码内联。

处理 migrate26Renames 的 EventBusSubscriber 替换：
1. 删 bus 参数（26.1.2 自动推断总线）
2. 注入 modid = "eyelib"（26.1.2 需显式 modid）

只处理短名 @EventBusSubscriber（@ 后直接跟 EventBusSubscriber）。
排除 @Mod.EventBusSubscriber（Forge 形式）和全限定名（@net.xxx.EventBusSubscriber）。

用法: python migrate_event_bus_subscriber.py [--dry-run] [目录]
"""
import re
import sys
import os
from pathlib import Path


def transform_line(line):
    if '@EventBusSubscriber' not in line:
        return line, False
    stripped = line.lstrip()
    if stripped.startswith('*') or stripped.startswith('//'):
        return line, False
    if 'Mod.EventBusSubscriber' in line:
        return line, False
    if re.search(r'@\w+\.\w+', line.split('EventBusSubscriber')[0]):
        return line, False

    changed = False
    original = line

    if 'modid' not in line:
        line = re.sub(
            r',\s*bus\s*=\s*EventBusSubscriber\.Bus\.\w+', '', line)
        line = re.sub(
            r'(\()bus\s*=\s*EventBusSubscriber\.Bus\.\w+,\s*', r'\1', line)
        line = re.sub(
            r'(\()\s*bus\s*=\s*EventBusSubscriber\.Bus\.\w+\s*(\))', r'\1\2', line)

        line = re.sub(
            r'@EventBusSubscriber\(Dist\.',
            '@EventBusSubscriber(value = Dist.', line)

        line = re.sub(
            r'@EventBusSubscriber\(\)',
            '@EventBusSubscriber(modid = "eyelib")', line)
        line = re.sub(
            r'@EventBusSubscriber(?!\()',
            '@EventBusSubscriber(modid = "eyelib")', line)
        line = re.sub(
            r'@EventBusSubscriber\((?!modid)',
            '@EventBusSubscriber(modid = "eyelib", ', line)
    else:
        line = re.sub(
            r',\s*bus\s*=\s*EventBusSubscriber\.Bus\.\w+', '', line)
        line = re.sub(
            r'(\()bus\s*=\s*EventBusSubscriber\.Bus\.\w+,\s*', r'\1', line)
        line = re.sub(
            r'(\()\s*bus\s*=\s*EventBusSubscriber\.Bus\.\w+\s*(\))', r'\1\2', line)

    changed = line != original
    return line, changed


def process_file(filepath, dry_run):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    new_lines = []
    any_changed = False
    for line in lines:
        new_line, changed = transform_line(line)
        new_lines.append(new_line)
        if changed:
            any_changed = True

    if any_changed and not dry_run:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)

    return any_changed


def main():
    dry_run = '--dry-run' in sys.argv
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    src_dir = args[0] if args else 'src/main/java'

    changed_files = []
    for root, dirs, files in os.walk(src_dir):
        for fname in files:
            if not fname.endswith('.java'):
                continue
            fpath = os.path.join(root, fname)
            if process_file(fpath, dry_run):
                changed_files.append(fpath)

    mode = "DRY-RUN" if dry_run else "APPLIED"
    print(f"[{mode}] {len(changed_files)} files changed:")
    for f in sorted(changed_files):
        print(f"  {f}")


if __name__ == '__main__':
    main()
