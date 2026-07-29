# -*- coding: utf-8 -*-
"""
文档处理工具集 - 用于 Markdown 文档的批量编辑操作

功能：
  - 文本查找替换
  - 批量多模式替换
  - 按标记插入内容
  - 替换文档章节
  - 合并文件
  - 提取文件章节

用法：
  python scripts/doc_tools.py            # 交互式选择
  或在其他脚本中 import 调用各函数

规范：
  - 所有路径使用原始字符串 r'' 或正斜杠
  - 所有 IO 使用 UTF-8 编码
  - 函数遵循"单一职责"原则
"""

import os, re, sys
from pathlib import Path
from typing import List, Tuple, Optional


# ──────────────────────────────────────────────
# 基础工具
# ──────────────────────────────────────────────

def read_file(filepath: str) -> str:
    """读取 UTF-8 文件内容"""
    with open(filepath, 'r', encoding='utf-8') as f:
        return f.read()


def write_file(filepath: str, content: str) -> None:
    """写入 UTF-8 文件（自动创建目录）"""
    Path(filepath).parent.mkdir(parents=True, exist_ok=True)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)


def resolve_path(path: str) -> str:
    """将相对路径转为绝对路径（基于项目根）"""
    if os.path.isabs(path):
        return path
    project_root = Path(__file__).resolve().parent.parent
    return str(project_root / path)


# ──────────────────────────────────────────────
# 单次文本替换
# ──────────────────────────────────────────────

def replace_text(filepath: str, old: str, new: str, *, count: int = 1) -> bool:
    """
    在文件中查找替换文本。

    参数：
      filepath  - 文件路径（相对或绝对）
      old       - 被替换的旧文本
      new       - 替换后的新文本
      count     - 替换次数，-1 表示全部替换

    返回：
      True 表示至少替换了一处，False 表示未找到
    """
    path = resolve_path(filepath)
    content = read_file(path)
    if old not in content:
        return False
    new_content = content.replace(old, new, count) if count >= 0 else content.replace(old, new)
    if new_content == content:
        return False
    write_file(path, new_content)
    print(f"  [替换成功] {os.path.basename(path)}  ({old[:40]}... -> {new[:40]}...)")
    return True


def replace_all(filepath: str, old: str, new: str) -> bool:
    """全文替换所有匹配项"""
    return replace_text(filepath, old, new, count=-1)


# ──────────────────────────────────────────────
# 批量替换
# ──────────────────────────────────────────────

def batch_replace(filepath: str, replacements: List[Tuple[str, str]]) -> int:
    """
    批量执行多个替换操作。

    参数：
      filepath      - 文件路径
      replacements  - 替换列表，每项为 (old_text, new_text)

    返回：
      成功替换的次数
    """
    path = resolve_path(filepath)
    content = read_file(path)
    count = 0

    for old, new in replacements:
        if old in content:
            content = content.replace(old, new)
            count += 1

    if count > 0:
        write_file(path, content)

    print(f"  [批量替换] {os.path.basename(path)}: {count}/{len(replacements)} 项替换成功")
    return count


# ──────────────────────────────────────────────
# 按标记插入内容
# ──────────────────────────────────────────────

def insert_after(filepath: str, marker: str, content_to_insert: str) -> bool:
    """
    在文件中指定标记文本后插入新内容。

    参数：
      filepath          - 文件路径
      marker            - 定位标记（插入在此文本之后）
      content_to_insert - 待插入的内容

    返回：
      True 表示插入成功
    """
    path = resolve_path(filepath)
    content = read_file(path)
    if marker not in content:
        return False
    idx = content.index(marker) + len(marker)
    new_content = content[:idx] + content_to_insert + content[idx:]
    write_file(path, new_content)
    return True


def insert_before(filepath: str, marker: str, content_to_insert: str) -> bool:
    """
    在文件中指定标记文本前插入新内容。
    """
    path = resolve_path(filepath)
    content = read_file(path)
    if marker not in content:
        return False
    idx = content.index(marker)
    new_content = content[:idx] + content_to_insert + content[idx:]
    write_file(path, new_content)
    return True


# ──────────────────────────────────────────────
# 章节替换
# ──────────────────────────────────────────────

def replace_section(
    filepath: str,
    start_marker: str,
    end_marker: str,
    new_content: str,
    *,
    include_end_marker: bool = False
) -> bool:
    """
    替换文件中从 start_marker 到 end_marker 之间的内容。

    参数：
      filepath            - 文件路径
      start_marker        - 起始标记
      end_marker          - 结束标记（替换到此处为止，默认不包含）
      new_content         - 替换后的新内容
      include_end_marker  - True 则连 end_marker 一并替换

    返回：
      True 表示替换成功
    """
    path = resolve_path(filepath)
    content = read_file(path)
    idx_start = content.find(start_marker)
    if idx_start < 0:
        return False
    idx_end = content.find(end_marker, idx_start + len(start_marker))
    if idx_end < 0:
        return False
    if not include_end_marker:
        new_content = new_content + end_marker
    new_full = content[:idx_start] + new_content + content[idx_end + len(end_marker):]
    write_file(path, new_full)
    return True


# ──────────────────────────────────────────────
# 文件合并
# ──────────────────────────────────────────────

def merge_files(
    input_paths: List[str],
    output_path: str,
    *,
    separator: str = "\n\n---\n\n"
) -> None:
    """
    将多个文件按顺序合并为一个文件。

    参数：
      input_paths  - 输入文件路径列表
      output_path  - 输出文件路径
      separator    - 文件之间的分隔符
    """
    output = resolve_path(output_path)
    parts = []
    for fp in input_paths:
        resolved = resolve_path(fp)
        parts.append(read_file(resolved))
    merged = separator.join(parts)
    write_file(output, merged)
    print(f"  [合并完成] {len(input_paths)} 个文件 -> {os.path.basename(output)}")


# ──────────────────────────────────────────────
# 提取章节
# ──────────────────────────────────────────────

def extract_section(
    filepath: str,
    start_marker: str,
    end_marker: str,
    *,
    include_markers: bool = False
) -> Optional[str]:
    """
    提取文件中的指定章节内容。

    参数：
      filepath         - 文件路径
      start_marker     - 起始标记
      end_marker       - 结束标记
      include_markers  - 是否包含标记行本身

    返回：
      章节文本字符串，未找到则返回 None
    """
    path = resolve_path(filepath)
    content = read_file(path)
    idx_start = content.find(start_marker)
    if idx_start < 0:
        return None
    idx_end = content.find(end_marker, idx_start + len(start_marker))
    if idx_end < 0:
        return None
    return content[idx_start if include_markers else idx_start + len(start_marker):
                   idx_end + len(end_marker) if include_markers else idx_end]


# ──────────────────────────────────────────────
# 生成 / 更新目录
# ──────────────────────────────────────────────

def generate_toc(markdown_text: str, *, max_level: int = 3) -> str:
    """
    从 Markdown 文本生成目录。

    参数：
      markdown_text  - Markdown 内容
      max_level      - 最大标题层级（默认为 3，即 ###）

    返回：
      目录的 Markdown 字符串
    """
    lines = []
    for line in markdown_text.splitlines():
        match = re.match(r'^(#{2,%d})\s+(.+)$' % max_level, line)
        if match:
            level = len(match.group(1))
            title = match.group(2).strip()
            indent = "  " * (level - 2)
            # 生成锚点
            anchor = title.lower()
            anchor = re.sub(r'[^\w\u4e00-\u9fff]+', '-', anchor).strip('-')
            lines.append(f"{indent}- [{title}](#{anchor})")
    if not lines:
        return ""
    return "## 目录\n\n" + "\n".join(lines) + "\n\n---\n"


def insert_toc(filepath: str, *, after_title: Optional[str] = None) -> bool:
    """
    在文档中自动生成并插入目录。

    参数：
      filepath     - 文件路径
      after_title  - 在此标题后插入（默认为文档第一个一级标题后）

    返回：
      True 表示成功
    """
    path = resolve_path(filepath)
    content = read_file(path)
    toc = generate_toc(content)
    if not toc:
        return False
    marker = after_title or "# "
    # 找到第一个标题行后插入
    lines = content.split('\n')
    insert_pos = None
    for i, line in enumerate(lines):
        if line.startswith(marker) and i > 0:
            insert_pos = i + 1
            break
    if insert_pos is None:
        return False
    # 避免重复插入
    if "## 目录" in content:
        return False
    lines.insert(insert_pos, '\n' + toc)
    write_file(path, '\n'.join(lines))
    return True


# ──────────────────────────────────────────────
# 入口（直接执行时可用作简单交互）
# ──────────────────────────────────────────────

if __name__ == "__main__":
    print("=" * 60)
    print("  doc_tools.py - 文档处理工具")
    print("=" * 60)
    print()
    print("  可用函数：")
    print("    replace_text()      - 文本查找替换")
    print("    replace_all()       - 全文替换")
    print("    batch_replace()     - 批量替换")
    print("    insert_after/before - 标记后/前插入")
    print("    replace_section()   - 替换章节")
    print("    merge_files()       - 合并文件")
    print("    extract_section()   - 提取章节")
    print("    generate_toc()      - 生成目录")
    print("    insert_toc()        - 插入目录到文件")
    print()
    print("  用法示例：")
    print("    from scripts.doc_tools import replace_text")
    print('    replace_text("docs/design/xxx.md", "旧文本", "新文本")')
    print()
