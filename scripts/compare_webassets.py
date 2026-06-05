#!/usr/bin/env python3
"""WebView用アセットの乖離検知スクリプト(SHA-1比較・標準ライブラリのみ)。

libjeid-android-app の `app/src/main/assets/` と
libjeid-ios-app の `app/WebAssets/` は本来同一内容であるべきだが、
別リポジトリで二重管理されているため乖離しやすい。

このスクリプトは両ディレクトリ配下の全ファイルを SHA-1 で突き合わせ、
- 内容が異なるファイル(DIFF)
- 片側にしか存在しないファイル(ANDROID-ONLY / IOS-ONLY)

`show_cert/` のように片側専用が意図的なものは既定で除外する(--no-default-excludes で無効化)。

使用例:
    python3 scripts/compare_webassets.py
    python3 scripts/compare_webassets.py --ios-dir /path/to/libjeid-ios-app/app/WebAssets
    python3 scripts/compare_webassets.py --exclude 'foo/*' --exclude '*.tmp'
"""

import argparse
import fnmatch
import hashlib
import sys
from pathlib import Path

# 片側専用が意図的なファイル群(相対パスの glob)。
# show_cert はマイナンバーカードの証明書ビューアで Android のみ実装。
DEFAULT_EXCLUDES = [
    "show_cert/*",
    "show_cert/**",
]

# スクリプト位置から見た既定のディレクトリ。
# このスクリプトは libjeid-android-app/tools/ に置かれる想定。
_REPO_ROOT = Path(__file__).resolve().parent.parent
_DEFAULT_ANDROID = _REPO_ROOT / "app" / "src" / "main" / "assets"
_DEFAULT_IOS = _REPO_ROOT.parent / "libjeid-ios-app" / "app" / "WebAssets"


def is_excluded(rel_path, patterns):
    """相対パス(POSIX形式)が除外パターンのいずれかに一致するか。"""
    posix = rel_path.as_posix()
    for pat in patterns:
        if fnmatch.fnmatch(posix, pat):
            return True
    return False


def sha1_of(path):
    """ファイルの SHA-1 を16進文字列で返す(大きいファイルにも対応)。"""
    h = hashlib.sha1()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def collect(base_dir, patterns):
    """base_dir 配下の通常ファイルを {相対パス: sha1} で返す(除外適用)。"""
    result = {}
    for path in sorted(base_dir.rglob("*")):
        if not path.is_file():
            continue
        rel = path.relative_to(base_dir)
        if is_excluded(rel, patterns):
            continue
        result[rel.as_posix()] = sha1_of(path)
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(
        description="Android assets と iOS WebAssets を SHA-1 で比較する",
    )
    parser.add_argument(
        "--android-dir",
        type=Path,
        default=_DEFAULT_ANDROID,
        help="Android アセットディレクトリ (既定: %(default)s)",
    )
    parser.add_argument(
        "--ios-dir",
        type=Path,
        default=_DEFAULT_IOS,
        help="iOS WebAssets ディレクトリ (既定: %(default)s)",
    )
    parser.add_argument(
        "--exclude",
        action="append",
        default=[],
        metavar="GLOB",
        help="除外する相対パス glob (繰り返し指定可)",
    )
    parser.add_argument(
        "--no-default-excludes",
        action="store_true",
        help="既定の除外(show_cert 等)を無効化する",
    )
    parser.add_argument(
        "-q",
        "--quiet",
        action="store_true",
        help="一致したファイルの一覧を出力しない",
    )
    args = parser.parse_args(argv)

    android_dir = args.android_dir.resolve()
    ios_dir = args.ios_dir.resolve()

    for label, d in (("Android", android_dir), ("iOS", ios_dir)):
        if not d.is_dir():
            print("ERROR: {} ディレクトリが見つかりません: {}".format(label, d),
                  file=sys.stderr)
            return 2

    patterns = list(args.exclude)
    if not args.no_default_excludes:
        patterns += DEFAULT_EXCLUDES

    android = collect(android_dir, patterns)
    ios = collect(ios_dir, patterns)

    android_keys = set(android)
    ios_keys = set(ios)
    common = sorted(android_keys & ios_keys)
    android_only = sorted(android_keys - ios_keys)
    ios_only = sorted(ios_keys - android_keys)

    diff = [k for k in common if android[k] != ios[k]]
    same = [k for k in common if android[k] == ios[k]]

    print("Android : {}".format(android_dir))
    print("iOS     : {}".format(ios_dir))
    if patterns:
        print("除外    : {}".format(", ".join(patterns)))
    print("")

    if same and not args.quiet:
        print("== 一致 ({}) ==".format(len(same)))
        for k in same:
            print("  =  {}".format(k))
        print("")

    if diff:
        print("== 内容が異なる ({}) ==".format(len(diff)))
        for k in diff:
            print("  X  {}".format(k))
            print("       android sha1: {}".format(android[k]))
            print("       ios     sha1: {}".format(ios[k]))
        print("")

    if android_only:
        print("== Android にのみ存在 ({}) ==".format(len(android_only)))
        for k in android_only:
            print("  >  {}".format(k))
        print("")

    if ios_only:
        print("== iOS にのみ存在 ({}) ==".format(len(ios_only)))
        for k in ios_only:
            print("  <  {}".format(k))
        print("")

    drift = len(diff) + len(android_only) + len(ios_only)
    if drift == 0:
        print("OK: 共有アセットは完全一致しています ({} ファイル)".format(len(same)))
        return 0

    print("NG: 乖離あり (内容差={}, Androidのみ={}, iOSのみ={})".format(
        len(diff), len(android_only), len(ios_only)))
    return 1


if __name__ == "__main__":
    sys.exit(main())
