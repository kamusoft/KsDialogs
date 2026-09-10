# レビュー結果: add-consumer-verification (003 回目)

**日付**: 2026-09-09
**判定**: APPROVED

## サマリー

review-002 の Minor 2 件はどちらも解消している。Minor 1 (呼び出し元 `EXIT` trap 本体の引用符復号) は文字列置換をやめ `trap -p EXIT` の出力を `eval` で配列展開して控える形になり、単一引用符・二重引用符・`$`・改行・タブ・バックスラッシュを含む本体を **原文のまま** 控えて連結実行・復帰できることを bash 3.2 (`/bin/bash`) で実測した。Minor 2 (deviation.md の記録) も現行機構 (発行前の未変更検査・`EXIT` trap・`git checkout HEAD --`・呼び出し元 trap の連結) に書き直されており、`verification/lib/gradle-publish.sh` と `verification/kmp/prepare-feed.sh` の実装と一致する。

修正による退行は見つからなかった。`shellcheck -x -e SC2034 -e SC1091` は `verification/**` 全スクリプトで exit 0 (`gradle-publish.sh` は既定 severity でも指摘 0)。identity / local-path / comment-policy / ci-skip / Scenario ID の各 lint も違反 0 件で、README lint と依存検査 3 本の自己テストも通る。足場 (proposal / design / specs) は未変更のまま。

新規の指摘は Suggestion 1 件のみで、実害はない。判定は APPROVED。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| cross/comment-policy.md | 常時 (`always: true`)。新設・改訂したコメント (`verification/lib/gradle-publish.sh:94-96`・`:113`) を本文の禁止参照・禁止類型と照合 |
| cross/ci-script-deletion.md | `verification/**` のスクリプトのレビュー。今回の差分に削除は無いが、作業ツリーを上書きする `git checkout HEAD --` に事前検証 (`ksv_require_pristine_swiftpm_locks` の 3 段) が付いている点を「破壊操作の前の検証」として確認 |

`kasane/lessons/code-review.md` は不在。`kasane/lessons/process.md` の L-002 (主張の範囲と実証の範囲の一致) を証跡の読み方に適用した。前回の Suggestion (README lint の fence 判定の非対称) は見送り済みのため再指摘しない。

## 前回指摘の解消状況 (自分で実行した確認)

### Minor 1: 呼び出し元 EXIT trap 本体の復号 — **解消**

`verification/lib/gradle-publish.sh:97-104` を実読。`declared="$(trap -p EXIT)"` → `eval "declared_words=(${declared#trap -- })"` → `${declared_words[0]}` の形で、シェル自身の引用形式をシェルに復号させる。字面の置換は無くなった。

リポジトリ直下で `/bin/bash` (3.2.57) から `gradle-publish.sh` を source し、実際に trap を張って実測した (追跡ファイルは変更していない。`ksv_restore_swiftpm_locks` は clean な `kmp/.swiftpm-locks/` に対する no-op の `git checkout HEAD --`)。

| 呼び出し元 trap 本体 | 控えた本体が原文一致 | disarm 後の `trap -p EXIT` が元と一致 |
|---|---|---|
| `echo "CALLER 'q' $HOME \"d\" TRAP"` (単一・二重引用符 + `$` + エスケープ) | OK | OK |
| 改行を含む 2 文 | OK | OK |
| タブ (制御文字。`trap -p` が `$'...'` 形式で出す) | OK | OK |
| バックスラッシュ | OK | OK |
| 括弧・セミコロン・関数定義を含む本体 | OK | OK |
| バッククォート・`$HOME`・`\` を混ぜた本体 | OK | OK |

- 異常終了 (`false` で `set -e` により脱出) の経路: 復元が 1 回走り、続いて控えた本体が **展開込みで正しく** 実行された (`CALLER 'q' <HOME> "d" TRAP` — `$HOME` が実行時に展開された)。終了コード 1 は保たれる
- 呼び出し元 trap 無しの経路: `KSV_PREVIOUS_EXIT_TRAP` は空のまま、`EXIT` trap は KSV のものだけ、`ksv_disarm_swiftpm_lock_restore` 後は `trap -p EXIT` が空 (= 完全解除)。異常終了でも復元 1 回で exit 1。**前回から挙動は変わっていない**
- 復元の二重実行は起きない (成功経路は `trap - EXIT` してから直接復元、失敗経路は trap 側で 1 回)

**インジェクションの余地**: `eval` に渡る文字列は `$(trap -p EXIT)` の出力からプレフィックス `trap -- ` を落としたものだけで、外部入力 (引数・環境変数・ファイル内容) は経由しない。`trap -p` の出力はシェルが再評価可能な形に引用したものなので、本体に何が入っていても `eval` は 1 つの文字列として読む (上表の実測どおり、`$HOME` もバッククォートも控えの時点では展開されない)。呼び出し元の trap 本体自体は同一シェル内のコードで、`verification/` 内で `ksv_arm_swiftpm_lock_restore` を呼ぶ経路 (`verification/kmp/prepare-feed.sh:94` の `ksv_publish_kmp` のみ) には `EXIT` trap を張るコードが無い (`verification/android/build-consumer.sh:97` の trap は別プロセス)。危険な入力源は無い。

### Minor 2: deviation.md の記録 — **解消**

`deviation.md:6` の該当項は現行機構に書き直されている。実装と 1 対 1 で照合した。

| deviation の記述 | 実装 |
|---|---|
| 復元の主体は `verification/lib/gradle-publish.sh` の kmp/ 発行 | `verification/lib/gradle-publish.sh:38-64` の `ksv_publish_kmp` が arm / disarm を持つ |
| 発行前に「Git 管理下・追跡済み・作業ツリーと index に差分なし」を検査して変更済みなら発行せず失敗 | `:77-90` の 3 段 (`rev-parse --is-inside-work-tree` / `ls-files --error-unmatch` / `diff --quiet HEAD --`)、すべて `ksv_fail`。`verification/kmp/prepare-feed.sh:50` に前倒しの同一検査 |
| 発行の成否によらず `EXIT` trap で復元 | `:93-107` の arm、`:110-120` の disarm |
| 復元は `git checkout HEAD --` | `:128` |
| 呼び出し元の EXIT trap は控えて連結 | `:98-106` |
| 5.1 の `git status` 確認範囲に `kmp/.swiftpm-locks/` を含める | 足場 (`tasks.md:41`) は `verification/kmp/` のままで、広げた分はこの deviation が記録している (足場凍結として正しい扱い) |

## 指摘事項

### [🔵 Suggestion] deviation.md の「`EXIT` trap で復元する」は成功経路の実際とわずかにずれる

**該当箇所**: `deviation.md:6` (「発行の成否によらず `EXIT` trap で `git checkout HEAD --` により復元する」)

**問題点**: 成功経路では `ksv_disarm_swiftpm_lock_restore` (`verification/lib/gradle-publish.sh:110-120`) が先に `trap - EXIT` で仕掛けを解いてから `ksv_restore_swiftpm_locks` を直接呼ぶため、復元を実行するのは `EXIT` trap ではない。`EXIT` trap が担うのは「発行〜検証の窓で落ちた場合」で、成否によらず復元されるという結論自体は正しい。文言だけを読むと成功時も trap 経由に読めるという程度の差。

**推奨修正** (任意): 「発行の成否によらず復元する (異常終了は `EXIT` trap、成功時は trap を解いてから直接)」のように担い手を分けて書く。判断そのものは変えない。優先度は低く、このままでも蒸留に誤った機構が持ち込まれるとは考えない。

## 確認して問題がなかった観点 (指摘なし)

- **コメント規約**: `verification/lib/gradle-publish.sh:94-96` の新設コメントは `trap -p` の出力形式と復号を `eval` に任せる理由を自己完結で説明しており、作業文書のパス・ローカル通番 (`review-002` `Minor 1` 等)・履歴記述を含まない。「文字列置換ではなく eval に任せ」は過去実装の記述ではなく採らなかった手段との対比なので、禁止する記述類型 (履歴記述・過去仕様の説明) には当たらないと判断した。`:113-114` の `SC2064` 抑止の理由コメントも自己完結。`comment-policy-lint.py` は全 1004 ファイルで違反 0
- **shellcheck**: `shellcheck -x -e SC2034 -e SC1091 verification/lib/*.sh verification/*/*.sh` が exit 0。`gradle-publish.sh` は除外なし・既定 severity でも指摘 0 で、`eval` を使った 2 行に新規の指摘 (SC2086 / SC2294 等) は出ていない
- **lint 一式**: `identity-lint` / `local-path-lint` / `comment-policy-lint` / `ci-skip-lint` / `scenario-id-coverage` はいずれも違反 0。`readme-example-lint.py` は本検査・`--selftest` とも exit 0、`verification/maui/check-dependencies.py` と `verification/kmp/check-dependencies.py` の `--selftest`、`comment-policy-lint.py --selftest` も exit 0 (退行なし)
- **bash 3.2 での成立**: 消費者検証は macOS ランナーでも回り `#!/bin/bash` は 3.2 系になるため、`local -a declared_words=()` と `eval` による配列代入、`set -u` 下での `${declared_words[0]}` 参照を 3.2.57 で実測した。いずれも問題なし
- **`trap '' EXIT` (無視指定) の扱い**: 呼び出し元が `trap '' EXIT` を張っていた場合、控えは空文字になり disarm 後は「無視」ではなく「未設定」に戻る。この差は修正前 (`[ -n ... ]` の番人) からあり今回の変更で生じたものではなく、EXIT は実シグナルではないため実行時の効果 (何も走らない) も同じ。指摘としない
- **標準出力の契約**: 復元の 1 行 (`ksv_restore_swiftpm_locks` の echo) の出力位置は変わっていない。`verification/kmp/prepare-feed.sh:96-97` が最後に Swift 参照 URL と参照先を出すので `build-consumer.sh` の `tail -n 2` の契約は維持される
- **古い機構が残る箇所**: `evidence/scripts/3.1-dry-run-four-platforms.txt:73` と `evidence/premise/1.5-premise-outcome.txt:37` には `git checkout --` の記述が残るが、どちらもその時点の実行・スパイクを記録した証跡であり、後から書き換えるのは記録の改竄にあたる。長命層へ渡る記録 (deviation.md) は現行に直っているので指摘しない
- **証跡の追補**: `evidence/review-fix-001/1-kmp-lock-guard.txt` 末尾の追補は、単一引用符 1 ケースの実測と shellcheck の結果だけを主張しており、書かれている範囲は実証の範囲に収まっている (L-002 適合)。本レビューではその外側 (二重引用符・`$`・改行・制御文字・disarm 後の往復一致・trap 無しの経路) を自分で実測して埋めた
- **足場の凍結**: `proposal.md` / `design.md` / `specs/**` は HEAD から未変更。`tasks.md` の差分はチェックボックスのみで本文差分 0 (5.5 / 5.6 が未チェック)

## アクションプラン

1. (任意) `deviation.md:6` の「`EXIT` trap で」の担い手を成功 / 異常で分けて書く — Suggestion。このままでも可
2. オーナーが tasks 5.5 / 5.6 (CI 側) を通し、消費者 4 job の所要時間と `Select Xcode` の効きを証跡へ記録してから蒸留へ進む (review-001 / 002 と同じ完了条件。指摘としては再掲しない)
