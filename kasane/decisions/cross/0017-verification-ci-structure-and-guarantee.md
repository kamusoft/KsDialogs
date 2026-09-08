---
id: 0017
title: 検証 CI は platform 別 reusable workflow 5 本と入口 1 本で構成し、緑の意味を「ロジック全件通過 + native 配線のコンパイル」に限り、トリガーはブランチの役割で分ける
status: accepted
date: 2026-09-08
amended-by: [cross/0020]
---

## Context

public 化した KsDialogs には push / pull request を機械検査する CI が無く、4 形態 (iOS / Android / MAUI / KMP) のテストと lint はローカル実行に頼っていた。`develop` へ直 push する運用 (cross/ADR-0016) では壊れた commit が `develop` に載り得るため、「`develop` の先端は機械検査済みか」を事後検証で可視化し、`main` 宛ての pull request ではマージ条件として保証する仕組みが要る。後続のパッケージング・消費者検証・release workflow は、ここで定義する検証 job を土台として再利用する。

姉妹ライブラリ KsSettingsView は同型の検証 CI を運用済みで、その決定 (`../KsSettingsView/kasane/decisions/cross/0025-verification-ci-reusable-platform-workflows.md` / `0026-ci-guarantee-logic-and-wiring-not-e2e.md` / `0028-ci-triggers-by-branch-role.md`) を「コピー + 固有値の差し替え」で逆流させる。KsDialogs 固有の事情は 3 つある。JVM では走らない Scenario 群を持つ Android instrumented テスト、iOS ターゲットが native への配線 (Swift パッケージへのリンク・ObjC 公開面) であり階層化 source set の metadata compile がターゲット本体とは別の compiler 経路で走る KMP、MAUI と native の橋渡しコード (Android の JVM テスト・iOS の Swift テスト) である。

前提: 開発者は 1 人で外部からの pull request を受け付けず (cross/ADR-0013)、`develop` へ直 push する (cross/ADR-0016)。リポジトリは public で macOS ランナーを使える。gitleaks・identity-lint・scenario-id-coverage は `kasane/**` も入力にする。

## Decision

**構成**: platform 別の reusable workflow (`workflow_call`) 5 本 — ios / android / android-instrumented / kmp / maui — と、それらを呼び lint を実行する入口 workflow 1 本で構成する。release workflow は検証を再定義せず同じ reusable workflow を呼ぶ。status check の名前は「呼び出し側 job 名 / 呼ばれた側 job 名」の形 (`ios / verify` 等 5 件) と `lint` で固定し、入口の変更検出 job (`changes`) は補助 check として必須には含めない。

**緑の意味**: 「ロジックが全件通過し、native への配線がコンパイルできる」まで。実行ホストを起動して人が見る検証 (E2E) は載せない。

- 自動で成否が決まるテストは全件実行する。iOS はホスト上の `swift test` を成否判定に用いず Simulator で実行する
- テストが 1 件も実行されなければテスト自体が緑でも失敗とする。件数の下限は 0 件 = 失敗のみで固定数値は持たず、検査の単位を細かく切って「一部が丸ごと走らない」を捕まえる
- 期待する module の集合はビルド構成 (`settings.gradle.kts` の include とテストソースセットの有無) から導出し、導出が空なら検査自体を失敗とする

| job | 件数検査の単位 |
|---|---|
| android | module ごと |
| android-instrumented | module ごとの実行数 (tests − skipped) |
| kmp | ターゲット (Android host / iOS Simulator) ごと |
| ios / maui | 合計 (Swift Testing と XCTest の 2 系統を合算) |

- Android instrumented は API 36 の Emulator 1 台の別 job に隔離する。KMP は macOS 1 job で全ターゲットのテストと metadata compile を同じ Gradle 起動で回す。MAUI は facade のユニットテスト・binding と platform TFM のビルド・橋渡しテスト 2 本を 1 job で回す
- lint job は secret scan (gitleaks、版と checksum を固定し追跡内容の展開に対して実行、展開数不足で失敗)・ローカル絶対パス・個体情報・コメント規約・仕様の Scenario ID とテスト名の網羅の 5 検査を持つ

**トリガー**: ブランチの役割で分ける。

| 事象 | 走る job |
|---|---|
| `develop` への push | lint + 本体検証 5 本 |
| `main` 宛ての pull request (head は自リポジトリの `develop` のみ。lint job が検査) | 同じ + 消費者検証 (消費者検証機能の決定で追加) |

- `develop` 宛ての pull request は起動条件に持たない
- lint は起動のたびに必ず走らせる。`on.push.paths-ignore` は使わない (lint の入力である `kasane/**` を除外すると秘密情報・個体情報・未網羅 Scenario の push が未検査で通る)
- 本体検証 5 job は `develop` push に限り、軽量な変更検出 job が「ビルド・テストのどちらにも入力されないファイルだけの変更」と判定したときスキップする。判定不能 (比較元が空・到達不能・差分が空) と変更検出 job 自体の失敗は「実行する」側へ倒す
- 同じブランチ (または同じ pull request) の連続する起動では、新しい起動が走行中の古い実行を打ち切る
- `develop` には必須 status check を付けない (force-push 禁止と削除禁止のみ)。`main` には必須 status check を付け、pull request 経由を必須にする

## Alternatives Considered

### 構成と保証範囲

- **入口 workflow に各 platform の手順を直接書く** — 却下。release workflow が同じ検証を必要とし、手順が 2 箇所に重複して片方だけ更新される
- **platform ごとに独立した入口 workflow を持つ** — 却下。1 つの変更の検証結果が複数の入口に分かれ、マージ保護の登録単位と開発者が見る単位が一致しない
- **実行ホストの起動 (E2E) まで CI に載せる** — 却下。Simulator / Emulator の起動時間と環境要因の不安定さを毎回の変更が負担する。利用者に見える変更の実機確認は完了条件として手元に残る
- **iOS でホスト上の `swift test` を成否判定に使う** — 却下。UIKit を要するテストが最初から存在しないものとして扱われ、緑の意味が「一部だけ通過」に薄まる
- **実行件数を見ず成否だけで判定する / 固定数値の下限を持つ** — 却下。前者はテスト探索の破綻が緑になる。後者はテスト整理のたびに CI を直すことになる
### KsDialogs 固有のテストルート

- **Android instrumented を CI に載せない / API 29 と 36 の 2 台で回す** — 却下。前者は JVM で走らない Scenario 群が手元頼みになる。後者は所要時間が倍になり API 29 のシステムイメージ選定が要る。API 29 固有の旧経路は手元の完了条件に残す
- **KMP を Ubuntu (Android host) + macOS (iOS) の 2 job にする** — 却下。status check が非対称になり Gradle 設定が二重になる
- **KMP を Ubuntu だけで回す** — 却下。iOS 配線と ObjC 公開面の検査が落ちる
- **KMP の metadata compile を消費者検証に寄せる** — 却下。`main` 宛て pull request でしか走らず、`develop` の push で metadata compile だけが壊れる型の失敗を見落とす
### トリガーと保護設定

- **`on.push.paths-ignore` で `kasane/**` 等を除外する** — 却下 (提案段階の相方レビューで撤回)。翻案元はこの形だが、KsDialogs の lint は `kasane/**` を入力にするため、記録だけの push で秘密情報が未検査のまま public な `develop` に載る
- **変更パスによる絞り込みを一切入れない** — 却下。`develop` は必須 status check を持たないため「素通り経路を作らない」という理由が成り立たず、記録だけの push で macOS ランナーが 5 本起きるのは無駄。除外を「どの検査の入力にもならないファイル」に限れば検出力は落ちない
- **`develop` に必須 status check を登録する / enforce_admins を on にする** — 却下。管理者への強制が off なら直 push は必須 check の有無によらず通り、効力の無い設定が見かけと運用の食い違いを生む。on にすると直 push 自体が拒否され cross/ADR-0016 が成り立たない

## Consequences

- 正: release workflow が日常の検証と同一の定義を使うため両者が食い違わない
- 正: 「緑が何を保証しないか」が明示され、実行時の確認を CI で代替できるという誤解が生じにくい
- 正: テスト探索の破綻や一部 module の脱落が緑として報告される経路を塞げる
- 正: 記録だけの push では macOS ランナーが起きず、lint だけで済む
- 負: 変更が 1 形態に閉じていても常に 5 形態分の実行時間とランナーを消費する
- 負: status check 名が 2 つの job 名の組で決まるため、job 名の変更がマージ保護の設定を壊す
- 負: `develop` の検証は事後になり、壊れた commit が `develop` に載り得る。失敗通知を見て直す運用が前提
- 負: 除外対象に新しい検査の入力が入ると、その検査が `develop` で走らない経路ができる。除外リストを広げるときは各 lint の入力を確かめる
- 負: 実行時にしか現れない欠陥 (描画・入力・タイミング) は CI では捕まらない
- 負: 件数検査を platform ごとに実装するため、テスト結果の出力形式が変わると検査も追随を要する

## Revisit When

- 開発体制が変わって複数人や外部からの pull request を `develop` で受けるようになったとき
- テストルート (ビルドルート・module・ターゲット) が増減したとき
- job の所要時間が開発の待ち時間として許容できなくなったとき (高速化は別の変更で扱う)

---
出典: kasane/roadmaps/package-distribution/phases/phase-4-verification-ci/agenda.md (決定事項: 踏襲 / KMP job の形と metadata compile / 実行件数の下限と CI に載せるテストルートの範囲 / `develop` の必須 status check は登録しない) / kasane/changes/archive/2026-09-08-add-verification-ci/proposal.md / 同 specs/verification-ci/spec.md (Requirement: CI の起動条件 / platform workflow の再利用契約 / 各 platform の検証 / lint の検証) / 同 second-opinion-spec-001.md (#1 `paths-ignore` の撤回) / ../KsSettingsView/kasane/decisions/cross/0025-verification-ci-reusable-platform-workflows.md / 0026-ci-guarantee-logic-and-wiring-not-e2e.md / 0028-ci-triggers-by-branch-role.md
関連: cross/ADR-0016 (ブランチの役割。トリガーの前提) / cross/ADR-0013 (外部 pull request 不受理。head 制限の前提) / cross/ADR-0018 (検証に用いる toolchain の固定)
