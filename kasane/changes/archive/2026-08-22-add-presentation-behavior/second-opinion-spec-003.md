# セカンドオピニオン: add-presentation-behavior (spec-003)
**相方**: codex (同一セッション 3ターン目・最終確認) / **日付**: 2026-08-21 / **対象**: R2 反映後の提案一式
---
最終確認の結果、前回指摘のうち4件が部分解消に留まっています。新規の Critical はありませんが、実装を分岐させる Major の矛盾が5件残るため、現時点では承認できません。

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 2回目指摘の解消状況

| 指摘 | 状況 | 評価 |
|---|---|---|
| Major 1: OS 発消失時の配送順・状態遷移・解放 | **部分解消** | Task/Job のキャンセル、removed 直行、提示中・退出中 Scenario は追加された。ただし未ラッチ時とラッチ済み時の結果規則が Requirement 内で矛盾する。 |
| Major 2: created/attached・オーバーレイ完了・入力可否 | **部分解消** | 状態遷移表と PB-TR-19〜22 により大部分は解消。ただし presentation 中の caller cancellation だけ、Requirement と状態遷移表が逆の規則を定める。 |
| Major 3: Swift の失敗通知 | **部分解消** | 完全シグネチャ、iOS spec、tasks は `async throws` になり、「操作できる」保証も弱化された。一方、公開 API 表と上位 ADR-0017 は非 throwing のまま。 |
| Major 4: 終了しないフック | **解消** | 有限完了を前提条件とし、caller cancellation / hostLost の脱出口、警告ログ、PB-TR-24/25 が追加された。 |
| Major 5: Scenario ID・PB-MD・メタ Scenario | **解消** | PB-MD-01〜04 が既存4本と1対1になり、PB-MD-04 は共通 ID、メタ Scenario も削除された。 |
| Major 6: 公開 API・duration 境界値 | **部分解消** | 完全シグネチャと PB-TR-26/27 は追加されたが、Swift 型の不一致と `TimeSpan.MaxValue` の扱いが矛盾する。 |
| Major 7: system bars の一回採用 | **部分解消** | PB-SB-06 と受け入れ方式表は追加された。ただし Scenario の選択肢が論理和になっており、可視状態と behavior の両方を検証する保証がない。 |
| Minor 1: Sample 調整面 | **解消** | 初期値、範囲、step、None/Custom の状態、方向・easing 写像が規範表として固定された。 |

## 指摘事項

### [Major] OS 発器消失時の結果が「常に cancelled」なのか「ラッチ済み結果を維持」するのか矛盾している

**該当箇所**: [dialog-contract/spec.md:33](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:33)、[dialog-contract/spec.md:82](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:82)、[dialog-contract/spec.md:94](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:94)、[design.md:157](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:157)

**問題点**: 「退出の開始条件」Requirement は、OS 発器消失では「即 cancelled を結果とする」と無条件に規定しています。一方、ラッチ Requirement と PB-TR-12 は、completed が既にラッチされていれば hostLost 後も completed を配送すると規定しています。

前者を文字どおり実装すると、dismissal 中の hostLost が結果を completed から cancelled へ上書きし、ラッチの不可逆性を破ります。

**推奨修正**: line 33 を「未ラッチなら cancelled をラッチする。ラッチ済みなら既存 outcome を維持する」と訂正してください。PB-TR-09 と PB-TR-12 がそれぞれ未ラッチ・ラッチ済みの具体例であることも明記してください。

### [Major] presentation 中の caller cancellation の扱いが Requirement と状態遷移表で逆になっている

**該当箇所**: [dialog-contract/spec.md:33](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:33)、[design.md:162](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:162)、[design.md:164](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:164)、[design.md:262](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:262)

**問題点**: dialog-contract は caller cancellation を含むすべての閉鎖信号について、presentation 中なら presentation を完走させると規定しています。Decision 10 の遷移表は、caller cancellation の場合だけ presentation フックをキャンセルして dismissing へ移ると規定しています。

また、Kotlin の NonCancellable 規則と終了しないフックの脱出口は、次の状態別解釈なら両立しますが、その区別が Requirement に反映されていません。

- shown でキャンセルが退出を開始する場合: dismissal を NonCancellable で完遂
- 既に dismissing の場合: 実行中の終了しない dismissal をキャンセルして脱出
- presenting の場合: presentation をキャンセルして dismissal へ移行

**推奨修正**: Decision 10 の状態別規則を dialog-contract の Requirement に反映してください。併せて「presentation 実行中の caller cancellation」と「既に dismissing 中の caller cancellation」の Scenario を追加し、PB-TR-05の通常報告とは分けて検証してください。

### [Major] Swift フックの公開シグネチャが三つの文書で一致していない

**該当箇所**: [design.md:57](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:57)、[design.md:72](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:72)、[ios-native/spec.md:7](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/ios-native/spec.md:7)、[0017-animation-hooks-transition-attachment.md:21](kasane/decisions/core/0017-animation-hooks-transition-attachment.md:21)、[tasks.md:27](kasane/changes/archive/2026-08-22-add-presentation-behavior/tasks.md:27)

**問題点**: 完全シグネチャ、iOS spec、tasks は `async throws` を要求していますが、Decision 3 の公開 API 表と上位 ADR-0017 は `async -> Void` のままです。実装者が表または ADR を正とすると、PB-TR-14/15のSwift失敗ケースを構成できません。

**推奨修正**: 公開 API 表と ADR-0017 を、完全シグネチャと同じ `@MainActor @Sendable (UIView) async throws -> Void` に統一してください。

### [Major] `TimeSpan.MaxValue` は「有限の正値」と「非有限相当」の両方に分類されている

**該当箇所**: [design.md:131](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:131)、[dialog-contract/spec.md:139](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:139)、[dialog-contract/spec.md:161](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:161)

**問題点**: 共通規則では「有限の正値」を有効とするため、C# の `TimeSpan.MaxValue` は有効値になります。一方、PB-TR-27 はそれを NaN・無限大の相当値として即完了させます。`TimeSpan` 自体には NaN やInfinityがないため、この矛盾を解かないと、巨大値をアニメーションAPIへ変換してオーバーフローする実装と、即完了する実装に分かれます。

**推奨修正**: MAUI の表現可能範囲を別途定義し、`TimeSpan.MaxValue` を無効値として即完了させるなら、その境界規則をdesignとRequirementへ明記してください。PB-TR-27ではコンテンツだけでなく、同値が設定される `overlayDuration` も即完了し、変換時にオーバーフローしないことを検証してください。

### [Major] PB-SB-06 が可視状態と behavior の片方しか検証しなくても成立する

**該当箇所**: [android-native/spec.md:53](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/android-native/spec.md:53)、[android-native/spec.md:55](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/android-native/spec.md:55)、[design.md:230](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:230)、[tasks.md:44](kasane/changes/archive/2026-08-22-add-presentation-behavior/tasks.md:44)

**問題点**: WHEN が「可視状態を変更する、または behavior を変更する」という論理和です。実装・テストが片方だけを選べるため、たとえば可視状態には継続追随するが behavior には追随しない実装でも、behavior 側だけを試せば通過します。

**推奨修正**: 可視状態変更と behavior 変更を別 Scenario ID に分けるか、1 Scenario 内で両方を順に変更して、それぞれが表示時の値を維持することを検証してください。受け入れ方式表とtasksも同じ粒度へ更新してください。

## 判定

**CHANGES_REQUESTED**

意図する設計方針自体はほぼ確定しています。上記は主に規範文書間の同期漏れと受け入れ条件の不足であり、修正後に再確認できる状態です。



---

## 突き合わせ結果 (2026-08-21)

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| R3-1 | OS 発消失時の結果が「常に cancelled」と「ラッチ維持」で矛盾 | 採用 | spec「退出の開始条件と直列化」本文を「未ラッチなら cancelled をラッチ、ラッチ済みなら維持」に訂正 (PB-TR-09 / 12 を例として明記) |
| R3-2 | presentation 中の呼び出し元キャンセルが Requirement と遷移表で逆 | 採用 | Requirement 本文に状態別規則 (presenting = presentation をキャンセルして退出 / shown = 完遂 / dismissing = 脱出) を反映、PB-TR-28 / 29 追加 |
| R3-3 | Swift フック型が API 表・ADR-0017 で非 throwing のまま | 採用 | design Decision 3 の API 表と ADR-0017 を `async throws` に統一 |
| R3-4 | `TimeSpan.MaxValue` が有効・無効の両方に分類 | 採用 | 「ミリ秒表現に変換できない大きさ (MAUI は uint.MaxValue 超) は無効」を規則化、PB-TR-27 に overlayDuration の即完了も追加 |
| R3-5 | PB-SB-06 が可視状態 / behavior の論理和 | 採用 | PB-SB-06 (可視状態) / PB-SB-07 (behavior) に分割、受け入れ方式表と tasks を更新 |

降格: 0件 / 未解決: 0件。いずれも文書間の同期漏れで設計判断の変更なし。
