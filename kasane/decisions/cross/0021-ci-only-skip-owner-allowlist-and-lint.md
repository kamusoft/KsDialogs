---
id: 0021
title: CI 上だけのテスト skip はオーナーが書く許可リストと lint で統制し、検証 CI の lint job に許可リスト検査を加えて 7 検査とする (0020 を一部改訂)
status: accepted
date: 2026-09-09
amends: [cross/0020]
---

## Context

検証 CI の Android instrumented job (API 36 の Emulator 1 台、headless) で、IME の出し入れを観測するテストが間欠的に落ちた。本物の suite を CI 上で並列反復して logcat の時系列を採ったところ、原因は本体ではなくテストの観測 — 遷移途中の一瞬 (要求可視性が一瞬だけ hidden へ振れる窓) を終端状態と読む偽の合格 — だった。同じ反復で Loading 系の 4 件と MAUI iOS 橋渡しの 1 件も落ち、失敗の族は「状態遷移の観測が終端状態の合意を待っていない」「実行機の容量差」に集約できた。

これらは観測と実行条件の修正で直せたが、実機・Emulator・Simulator の上で OS 側の状態遷移に触れるテストは今後も実行機のタイミングで揺らぎ得る。オーナーは、失敗が出るたびに 1 テストずつ変更を起こす運用を「小分けにしてもキリがない」として退け、規律 (終端状態の合意を待つ観測の共通プリミティブと切り分けフロー) を先に置いた上で、それでも環境で揺らぐテストは CI 上だけ実行しない扱いを許容すると決めた。同時に、skip は直せる失敗と直せない失敗の区別がつかないまま赤を消せる最短の出口であり、「何でもスキップに逃げるのを防ぐ工夫」を求めた。

cross/ADR-0017 (cross/ADR-0020 で一部改訂) は lint job の検査を 6 つに固定しており、検査を無断で足すことはできない。

前提: CI が回すテストルートは 7 つ (Android instrumented・Android のローカル単体テスト・kmp のテストソースセット・MAUI の NUnit・MAUI Android 橋渡しの Kotlin JVM テスト・iOS の Swift テスト・MAUI iOS 橋渡しの Swift テスト) で、CI 限定 skip の印を置けるのは実行機の揺らぎを持つ 3 つ (Android instrumented・iOS・MAUI iOS 橋渡し)。開発者は 1 人で、承認の主体はオーナー (cross/ADR-0013)。

## Decision

cross/ADR-0020 の決定のうち「lint job は 6 検査を持つ」を、これに **CI 限定スキップの許可リスト検査 (`scripts/ci-skip-lint.py`、自己テスト付き) を加えた 7 検査**へ置き換える。他の決定 (構成・緑の意味・トリガー・件数検査・同期スクリプトの自己テスト) は維持する。

CI 上だけテストを実行しない扱いは、次の形でのみ許容する。

| 要素 | 決定 |
|---|---|
| 承認 | skip を付けてよいのは、オーナーが `kasane/config.yaml` の `lint.ci-skip.allow` に項目 (テスト識別子・失敗の機構・承認日・再評価期限) を書いたテストだけ。エージェントは許可リストを自分で足さず、足す diff にはオーナー指示の記録 (`deviation.md`) を要する |
| 印 | 正規の 2 つに限る — Android instrumented は `@SkipOnCi(reason)` と `SkipOnCiRule`、Swift は trait `.skipOnCi(reason)`。skip は前提の不成立として起こし、結果の skipped に数えられる形にする (件数検査「実行数 = tests − skipped」に載る) |
| CI の判定 | CI 側が明示的に渡す値で判定する — Android は実行引数 `ksdialogsCi=true`、Swift は環境変数 `TEST_RUNNER_KSDIALOGS_CI=1`。実行機が持つ汎用の CI 環境変数には頼らない |
| 正規の印以外 | 承認手順を通らない無効化手段 — Kotlin の `@Ignore`、Swift の素の `.disabled(...)` / `.enabled(if:)` / `XCTSkip` 系、NUnit の `[Ignore]` / `[Explicit]` / `Assert.Ignore` / `Assert.Inconclusive` — は、印の仕組みを持たないテストルートも含めた 7 ルートすべてで一律に違反とする。前提条件の表現 (`Assume` / `#require` / `Assume.That`) は対象にしない |
| lint | 許可リストに無い印、切り分けの 3 要素 (手元の反復で通る回数・CI での落ち方・重要な機能のテストでない理由) を欠く理由、承認の記録を欠く許可リスト項目、正規の印以外の無効化手段を違反とし、`develop` への push のたびに落とす。自己テストを本検査より先に同じ step で走らせ、検出器の退行で無音に素通りする経路を塞ぐ |

切り分けの順序 (負荷の実測 → 観測の欠陥か → 手元の反復 → 重要度) と 3 条件 (手元で通る / CI で落ちる / 重要な機能のテストでない) の運用は、規約 `kasane/handbook/cross/ci-flaky-test-policy.md` が定める。

## Alternatives Considered

- **CI 限定 skip を一切許容せず、落ちたテストごとに変更を起こして直す** — 却下。オーナー判断「小分けにしてもキリがない」。探索・提案・レビューのコストが変更ごとに付き、実行機のタイミングで揺らぐ失敗は今後も出る
- **規約文だけで運用し、許可リストと lint を持たない** — 却下。skip は作業を進める側にとって常に最短の出口に見え、「今回は例外」の判断が積み上がる。承認をオーナーが書く許可リストという形にし、対応の無い印を機械が落とす
- **実行機が持つ汎用の CI 環境変数で CI かどうかを判定する** — 却下。同じ名前の変数を持つ手元の環境で意図せず skip され、手元の実行が CI と同じ範囲を回している保証が崩れる
- **既存の無効化手段 (`@Ignore` / `.disabled` / NUnit の `[Ignore]` 等) を許可リストと突き合わせて許す** — 却下。これらは承認手順を通らずに赤を消せ、結果では skipped に数えられるため件数検査も素通りする。承認された道より安く痕跡も残らない出口になるため、一律で違反にする
- **待ち時間の延長で間欠失敗を吸収する** — 却下。実行機が遅くなれば同じ落ち方が戻る (MAUI iOS 橋渡しの Toast 期限を 6.0 秒へ広げる案を、表示時間を観察の所要から独立させる形に置き換えた)
- **修正の効き目を CI 上の反復 (N 回並列の一時 workflow) で測る** — 却下。オーナー判断「時間の無駄」。効き目は手元の反復と切り分け規約で確かめ、反復 workflow は署名の取得後に削除した

## Consequences

- 正: 承認の無い skip と、承認手順を迂回する無効化手段が、`develop` への push のたびに検出される
- 正: skip は skipped として数えられ、実行数の検査 (0 件 = 失敗) と両立する
- 正: 手元の実行は印の有無によらず全件を回すため、手元の完了判定の範囲が CI の skip で狭まらない
- 負: lint job の検査が 1 つ増え、CI が回すテストルートを増減したときは lint の走査根も追随を要する
- 負: 機械の歯止めは「印に対応する許可リスト項目があるか」までで、エージェントが印と許可リスト項目を同じ回に自分で書けば lint は通る。承認の出どころの確認はレビュー (deviation の記録) が担う
- 負: 汎用の CI 環境変数を使わないため、CI 側が判定材料を渡す配線を job ごとに持つ。配線の無い job では印が効かず、テストは CI でも実行される
- 負: 件数検査は skip が 1 件増えたことを検出しない。skip の増加に対する安全網は許可リストと再評価期限 (棚卸し) だけになる

## Revisit When

- 実行機の容量・API レベル・観測の仕組みが変わり、許可リストの項目を外せる根拠が出たとき (項目の `review-by` を過ぎたら棚卸しで再評価する)
- CI が回すテストルートが増減したとき (印を置ける面と lint の走査根の対応を見直す)
- 開発体制が変わって承認の主体がオーナー 1 人でなくなったとき

---
出典: kasane/changes/archive/2026-09-09-fix-android-instrumented-toast-ime-hide-flake/deviation.md (スコープ拡張のオーナー判断 2026-09-09・lint job への検査追加・CI 反復の却下・BV-MA-03 の期限延長の不採用) / 同 exploration.md (決定事項) / 同 evidence/ci-repeat-signature.md (署名) / 同 evidence/ci-flake-triage.md (切り分けと skip 0 件の判定) / 同 review-005.md (許可リストの provenance) / kasane/handbook/cross/ci-flaky-test-policy.md (「なぜ歯止めを機械で持つか」「skip の書き方」)
関連: cross/ADR-0017 (検証 CI の構成と lint job の検査。0020 を経て本決定が検査の集合を置き換える) / cross/ADR-0020 (同期スクリプトの自己テストを lint job で回す。自己テストを本検査と同じ step で先に走らせる理屈はこれと同じ)
