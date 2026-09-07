#!/usr/bin/env bash
# 汎用 secret scanner (gitleaks) の一括実行。
#
# 履歴 (git モード) と作業ツリー (dir モード) を続けて検査する。
# 誤検出の除外は .gitleaks.toml、Kasane 標準 lint との役割分担も同ファイルの冒頭を参照。
#
# 実行タイミング: public 化前の点検、ksn-drift のクイックチェック、ksn-distill の archive 前。
#
# 使い方:
#   scripts/secret-scan.sh          # 履歴 + 作業ツリー
#   scripts/secret-scan.sh --git    # 履歴だけ
#   scripts/secret-scan.sh --dir    # 作業ツリーだけ
set -uo pipefail

cd "$(git rev-parse --show-toplevel)" || exit 2

if ! command -v gitleaks >/dev/null 2>&1; then
  echo "gitleaks が見つかりません。'brew install gitleaks' で導入してください。" >&2
  exit 2
fi

mode="${1:-all}"
status=0

if [ "$mode" = "all" ] || [ "$mode" = "--git" ]; then
  echo "--- 履歴 (git 全 commit)"
  gitleaks git --redact --no-banner || status=1
fi

if [ "$mode" = "all" ] || [ "$mode" = "--dir" ]; then
  echo "--- 作業ツリー"
  gitleaks dir --redact --no-banner || status=1
fi

if [ "$status" -ne 0 ]; then
  echo "" >&2
  echo "secret を検出しました。実値をコミットせず、値の失効 (rotate) と履歴からの除去を検討してください。" >&2
  echo "誤検出なら .gitleaks.toml の allowlist、または該当行末に 'gitleaks:allow' を付けてください。" >&2
fi
exit "$status"
