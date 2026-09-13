# Exploration: fix-wait-published-unreachable-arm

## 課題 / 動機

KsSettingsView からの知らせ (`../KsSettingsView/kasane/outbox/KsDialogs/2026-09-14-wait-published-empty-state-arm-unreachable.md`、kind: bug) による簡易起票 (2026-09-14)。

`scripts/release/central-portal.sh` の公開待ち (`cmd_wait_published`) にある空文字アーム (`"")` は、本番の呼び出し文脈では実行されていない。

- 状態照会 (`deployment_state`) は HTTP 200 以外で `fail` を呼び、`fail` は `exit 1` する
- 冒頭の `set -euo pipefail` の下では `state="$(deployment_state "${id}")"` の代入がコマンド置換の終了コードを引き継ぎ、その場でスクリプトが終了する。空文字はアームに届かない
- `--selftest` は検査を `if ( ... )` の条件部で呼ぶため bash が errexit を抑止し、アームに届いて緑になる。つまり検査が緑である理由 (アームが働いた) は本番には存在しない

相手側の実測 (こちらのスクリプトの写しで `http_request` を台本モックに差し替え、`main` の `case` から素のコマンドとして起動): 503 → PUBLISHED の台本で exit 1・照会 1 件・アーム到達 0 回。`-e` だけ外した写しではアーム到達 1 回。

**現時点で KsDialogs の挙動は壊れていない。** アームの意図が「答えの返らない照会は待たずに止める」(即失敗) であり、到達しなくても `deployment_state` の即失敗で同じ観測結果 (exit 1・照会 1 件・`::error::` 1 本) になる。壊れているのは次の 2 点:

- アームが本番では実行されない死んだコード
- コメントが言う「内側の層」が本番には存在せず、同じ結果を出しているのは errexit という設計外の経路

公開待ちの意図を「即失敗」から変えた瞬間に実害になる構造 (KsSettingsView は意図を「Portal の単発の 5xx / 429 は吸収して待ち続ける」へ変えたことで、スイープの実測とレビューで機能の不成立として露見した。本番リリースでの事故ではない)。

相手側の対応は `../KsSettingsView/kasane/decisions/cross/0031-query-failure-as-state-value-not-fail.md` (accepted、2026-09-14): 照会そのものが行えなかったことを `fail` ではなく状態値 `UNRESOLVED` で返し、致命かどうかの判断を呼び出し側 (`status` / `release` / `drop` は失敗、検証待ちは 1 回で失敗、公開待ちだけ連続回数で判断) へ移した。代入の成否に依存しなくなるので errexit の効き方が結果を変えない。

相手側が却下した案 (再検討の材料):
- `|| state=""` を足すだけ: その 1 行を消しても自己テストが緑のままになり、検出力 0 の箇所を新設する
- `( set -e; ... )` で検査文脈を張り直す: bash 3.2 では errexit が再武装されず、CI の bash 5 と挙動が割れる
- 待機関数を allowlist 型にする: Portal が状態名を足した時点でリリースが即失敗する
- 公開待ちも 1 回で失敗させる: 取り消せない `release` の後で単発の 5xx に落ちる。同じ経路の `wait-for-registries.sh` が逆の方針 (判定不能でも待つ) で非対称に理由が無い (相手側オーナー裁定 2026-09-13)

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: 照会失敗を状態値で表す契約を採るなら相手 ADR 0031 に相当する cross の ADR)

## 未決の論点

未探索 (簡易起票)。

- 公開待ちの意図を「即失敗」のまま保つか、KsSettingsView と同じ「単発の 5xx / 429 は吸収して待つ」へ変えるか。こちらの `wait-for-registries.sh` も判定不能で待機を続ける方針かを確認し、非対称の有無を見る
- 意図を変えないとしても死んだアームを残すか。残すなら「内側の層」のコメントは実態と合っていない
- 相手の契約 (`UNRESOLVED` 状態値) をそのまま写すか。写すなら 5 経路すべてが `UNRESOLVED` を扱う責任を持つ
- 確かめ方は `--selftest` ではなく `main` の `case` から素のコマンドとして呼ぶ写しで 503 の台本を回し、アームに到達するかを見る。自己テストの検査文脈そのものを本番文脈に寄せられるかは相手の却下案 (bash 3.2 の errexit) が前提
- 自己テストを CI へ載せる change add-release-script-selftests-to-lint との順序

## UI 素材 (ui/references/ の一覧と注釈)

なし。

## 変更級の推奨: 未判定

暫定 S。触るのは `scripts/release/central-portal.sh` 1 本と自己テストのみ、公開 API 変更なし、可逆。ただし公開待ちの意図を変える (吸収型にする) 判断を含めるなら、リリース手順の契約 (handbook/cross/release-procedure.md の失敗時の表) に触れるため M に上がる可能性がある。
