#!/usr/bin/env python3
"""消費者の Package.swift をテンプレートから生成する。

iOS と KMP の消費者は、依存宣言 1 行だけが mode で変わる Package.swift を持つ。
テンプレートの差し込み口 `@KSV_DEPENDENCY@` をその 1 行へ置換して書き出す。

差し込み口はちょうど 1 か所であることを要求する。コメント等に増えると、意図しない
位置まで置換されたマニフェストで消費者ビルドが通ってしまう。

使い方:
    render-template.py --template <テンプレート> --output <出力先>
                       --dependency <依存宣言の 1 行>
"""

import argparse
import sys

PLACEHOLDER = "@KSV_DEPENDENCY@"


def render(template: str, dependency: str) -> str:
    occurrences = template.count(PLACEHOLDER)
    if occurrences != 1:
        raise SystemExit(
            f"エラー: テンプレートの差し込み口 {PLACEHOLDER} が 1 か所ではありません: {occurrences} か所"
        )
    return template.replace(PLACEHOLDER, dependency)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--template", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--dependency", required=True)
    args = parser.parse_args(argv)

    with open(args.template, encoding="utf-8") as f:
        template = f.read()

    with open(args.output, "w", encoding="utf-8") as f:
        f.write(render(template, args.dependency))

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
