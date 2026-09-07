# セカンドオピニオン: add-presentation-behavior (spec-001)
**相方**: codex / **日付**: 2026-08-21 / **対象**: 提案一式 (proposal / design / specs 6 capability / tasks / ui/brief)
---
# レビュー結果: add-presentation-behavior

**日付**: 2026-08-21  
**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 0 / Major 10 / Minor 2 / Suggestion 0

## サマリー

変更の目的とプラットフォーム横断方針は明確ですが、トランジションの状態遷移、失敗時の扱い、キャンセルと結果確定の意味論が未確定です。このまま実装すると Native 2実装と MAUI/KMP で異なる挙動になり、特に退出処理の競合や宙吊りが発生し得ます。

また、`none` の意味、OS 発の器消失、Android system bars、MAUI の非同期コールバック境界に文書間の矛盾があります。いずれも実装判断へ委ねず、仕様段階で解決すべき事項です。

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 指摘事項

### [🟠 Major] `none` と常時オーバーレイフェードが両立しない

**該当箇所**: `kasane/changes/add-presentation-behavior/design.md:30`、`kasane/changes/add-presentation-behavior/design.md:47`、`kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:50`、`kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:57`、`kasane/changes/add-presentation-behavior/ui/brief.md:21`

**問題点**: design はトランジションの指定に関係なくオーバーレイを常にフェードさせる一方、`none` の Scenario は「表示・閉鎖とも演出なしで即完了」「待ちなし」としています。オーバーレイ消滅を待って器を撤去するなら、退出は即完了しません。また design は `none()` と記載しながら、直後に「全プリセットが duration / easing を持つ」としており、API 形状も矛盾しています。

**推奨修正**: 次のどちらかを明記してください。

- `none` はコンテンツのみ無演出で、オーバーレイは既定フェードし、その完了を待つ。
- `none` はオーバーレイを含む全演出を無効化する、Decision 2 の明示的例外とする。

あわせて `none` が duration / easing を受けるかを確定してください。

### [🟠 Major] トランジションの採用時点と状態遷移が定義されていない

**該当箇所**: `kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:7`、`kasane/changes/add-presentation-behavior/design.md:67`、`kasane/concepts/core/api/layout-semantics.md:86`、`ios/Sources/KsDialogs/SwiftUI/DialogSwiftUIHost.swift:48`、`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogAttributes.kt:33`

**問題点**: 「第3の添付スロット」とされていますが、いつ添付値を採用するかがありません。SwiftUI/Compose の添付は初回レイアウト中に届くため、早く読むと既定 transition になり、遅く読むと初回フレームが一瞬表示されます。表示後の状態変更で添付値が変わった場合、退出に最初と同じ `DialogTransition` を使うかも不明です。

さらに、presentation 中に結果報告や呼び出し元キャンセルが来た場合に、presentation と dismissal を直列化するのか、presentation を中断するのか決まっていません。

**推奨修正**: `created → presenting → shown → dismissing → removed` の状態遷移を定義し、少なくとも以下を固定してください。

- transition のスナップショット時点
- 初回描画と presentation フック開始の順序
- 同一インスタンスを dismissal にも使うか
- presentation 中の閉鎖要求の扱い
- dismissal フックは複数の閉鎖信号でも高々1回であること

各競合を Scenario として追加してください。

### [🟠 Major] フックの実行スレッド・例外・キャンセル方針がない

**該当箇所**: `kasane/changes/add-presentation-behavior/design.md:18`、`kasane/changes/add-presentation-behavior/design.md:73`、`kasane/changes/add-presentation-behavior/specs/maui-binding/spec.md:7`

**問題点**: Kotlin の suspend フックは例外を投げられ、MAUI の Task は faulted/cancelled になれます。Swift の async フックも Task cancellation を観察できます。しかし、失敗時に器を撤去するのか、show を失敗させるのか、結果を維持するのかがありません。タイムアウトを設けない判断は、例外処理が不要という意味にはなりません。

また UI View を受け取る公開フックなのに、MainActor/UI thread 上で呼ぶ保証がありません。

**推奨修正**: 全形態共通で以下を規定してください。

- フックは UI thread / MainActor 上で開始する
- フック例外・fault・cancellation の公開結果への写像
- 失敗時にも器と宣言的 UI ホストを必ず解放すること
- OS 発消失時に実行中フックを中断するか、結果だけ先に解放するか

正常系だけでなく、throw/fault/cancel の Scenario を追加してください。

### [🟠 Major] 「結果の確定」と「show の返却」が上位契約と衝突している

**該当箇所**: `kasane/changes/add-presentation-behavior/design.md:73`、`kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:28`、`kasane/concepts/core/api/result-notification-semantics.md:19`、`kasane/concepts/core/api/result-notification-semantics.md:23`、`kasane/changes/add-presentation-behavior/proposal.md:28`

**問題点**: 現行 concept は最初の報告時点で結果を「確定」し、以後の報告を no-op とします。一方、design は器の撤去後に結果を「確定」するとしています。結果値の選択を遅らせるのか、最初の報告で値をラッチして show への配送だけ遅らせるのかで、退出中の二重報告・器消失との競合結果が変わります。

また proposal は変更を「視覚のみ」としていますが、添付なしでも既定の約250ms退出を待つため、全 show の完了時刻が変わる観察可能な挙動変更です。

**推奨修正**: 用語を次のように分離してください。

- 最初の報告で outcome を不可逆にラッチする。
- dismissal 完了・器撤去後に、ラッチ済み outcome を show 呼び出し元へ配送する。

退出中の二重報告と器消失でも最初の outcome が維持される Scenarioを追加し、Impact/Migration Plan に待機時間の変更を明記してください。

### [🟠 Major] 呼び出し元キャンセルの公開結果が形態間で未確定

**該当箇所**: `kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:38`、`kasane/changes/add-presentation-behavior/specs/kmp-facade/spec.md:9`、`kasane/decisions/kmp/0005-swift-show-caller-cancellation-via-show-handle.md:15`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt:69`

**問題点**: concept/ADR はキャンセル時に結果を `cancelled` で確定するとしていますが、Scenario は「ダイアログが閉じる」だけで、呼び出し側が何を観察するかを検証しません。現在の Android `suspendCancellableCoroutine` は呼び出しコルーチンのキャンセルを `CancellationException` として伝播し、`DialogResult.Cancelled` は返しません。KMP Kotlin 経路も同じ選択が必要です。

**推奨修正**: 形態ごとに次を明記してください。

- Swift Task cancellation は `.cancelled` を返すのか
- Kotlin coroutine cancellation は `CancellationException` を維持し、内部 outcome だけ cancelled にするのか
- dismissal フックと器撤去を、キャンセル済み coroutine から `NonCancellable` 相当で完遂するのか

Scenario の THEN に、閉鎖だけでなく await 側の結果を追加してください。

### [🟠 Major] OS 発の器消失の例外が ADR-0017 と衝突し、現行実装では原因を識別できない

**該当箇所**: `kasane/decisions/core/0017-animation-hooks-transition-attachment.md:19`、`kasane/decisions/core/0017-animation-hooks-transition-attachment.md:38`、`kasane/changes/add-presentation-behavior/design.md:72`、`kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:26`、`ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:263`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:88`

**問題点**: ADR-0017 は cancelled を含む全閉鎖経路で dismissal フックを保証していますが、design/spec は OS 発消失を例外にしています。上位決定とデルタスペックの直接的な不一致です。

さらに現行実装は外側タップ、戻る、器消失をすべて同じ `.cancelled` に畳み、`DialogResultChannel` は outcome しか運びません。そのままでは dismissal フックを実行すべき cancelled と、実行してはいけない cancelled を提示層が判別できません。

**推奨修正**: ADR-0017 の「全閉鎖経路」を改訂するか、spec を ADR に合わせるかを先に決めてください。例外を採る場合は、公開結果とは別の内部 `DismissalOrigin` などを定義し、原因が失われない配管と Scenario を design/tasks に追加してください。

### [🟠 Major] 公開 API の形が API surface test を書ける精度に達していない

**該当箇所**: `kasane/changes/add-presentation-behavior/design.md:14`、`kasane/changes/add-presentation-behavior/design.md:40`、`kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:48`、`kasane/changes/add-presentation-behavior/tasks.md:24`

**問題点**: `DialogTransition` のプロパティ名、initializer、visibility、duration の型・単位・正確な既定値、iOS easing の具体型、direction enum、無効 duration の扱いがありません。slide の start/end が物理方向か RTL 追随か、zoom の開始倍率も不明です。この状態では task 3.6 の API surface check に一意な期待値を設定できません。

**推奨修正**: iOS / Android / MAUI ごとの公開 API 表と最小利用例を design または platform spec に追加してください。正のコンパイル例に加えて、禁止する形も明記してください。

### [🟠 Major] MAUI の非同期フックを Native が待つための bridge 設計とタスクがない

**該当箇所**: `kasane/changes/add-presentation-behavior/specs/maui-binding/spec.md:7`、`kasane/changes/add-presentation-behavior/tasks.md:21`、`maui/KsDialogs.Maui/Internals/DialogGateway.cs:37`、`maui/KsDialogs.Maui/Platforms/iOS/PlatformDialogGateway.cs:81`、`maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs:124`

**問題点**: MAUI spec は `Func<VisualElement, Task>` を Native へパススルーし、Native が完了を待つとしています。しかし現行 bridge は View、options、placement と閉鎖通知しか運ばず、Task 完了を Swift/Kotlinへ返すコールバック面がありません。C# delegate の寿命、GC、Task fault/cancel、UI thread への復帰も未設計です。

task 3.3 の「ネイティブへのパススルー」だけでは、iOS ObjC binding、Android Java binding、両 Native bridge の変更範囲を追跡できません。

**推奨修正**: `runPresentation(platformView, completion)` / `runDismissal(..., completion)` 相当の bridge protocol、delegate 保持期間、完了の多重通知防止、例外変換を design に追加してください。iOS/Android bridge・binding 定義・C# adapter を個別 task とテストへ分解してください。

### [🟠 Major] system bars の Requirement、実装範囲、受け入れ基準が一致していない

**該当箇所**: `kasane/changes/add-presentation-behavior/proposal.md:12`、`kasane/changes/add-presentation-behavior/specs/android-native/spec.md:19`、`kasane/changes/add-presentation-behavior/tasks.md:28`、`kasane/changes/add-presentation-behavior/tasks.md:30`

**問題点**: proposal/tasks は Android 11+ に限定していますが、Requirement は API level を限定せず「表示状態と挙動設定を引き継ぐ」としています。Scenario は全表示・全非表示だけで、status/navigation の混在状態や `systemBarsBehavior` を一切検証していません。初回だけのスナップショットか、ホスト変更への追随かも不明です。

また task 4.3 は iOS の成立性を実装中に確認し、不足なら「提案改訂」としています。実装開始後の proposal/spec は凍結対象なので、未確定事項を実装フェーズへ持ち込んでいます。

**推奨修正**: API 24–29 / 30+ の保証表、status/navigation 別の可視状態、behavior、採用時点を定義してください。iOS の確認は実装前に解消し、必要なら iOS capability/spec を追加してください。

### [🟠 Major] 長命 concepts への追随範囲が公開契約の変更に足りない

**該当箇所**: `kasane/config.yaml:7`、`kasane/changes/add-presentation-behavior/tasks.md:37`、`kasane/concepts/core/api/result-notification-semantics.md:19`、`kasane/concepts/core/api/multi-display-semantics.md:41`

**問題点**: プロジェクト規約は公開 API 契約を concepts の対象としていますが、task 6 は既存3文書の限定的な修正しか予定していません。`DialogTransition` の公開契約、フック保証範囲、結果ラッチと返却タイミングは長命文書に残りません。また multi-display concept は閉じるアニメーション時間を OS/実装依存としていますが、本変更はライブラリ既定値へ統一します。

**推奨修正**: 新しい transition 契約文書を `concepts/core/api/` に設けるか、registration/result/multi-display 文書へ責務別に統合してください。task 6 に、結果配送タイミングと閉鎖アニメーション規則の更新を明示してください。

### [🟠 Major] 動的モックの承認ゲートが完了していない

**該当箇所**: `kasane/changes/add-presentation-behavior/ui/brief.md:42`、`kasane/changes/add-presentation-behavior/ui/brief.md:44`

**問題点**: 静的なデモ画面は案Bとして承認済みですが、プリセットの動作イメージは「確認後に記入」のままで承認欄が空です。一方で同モックはオーバーレイとコンテンツの動作契約として参照され、task 7.4 でも突き合わせ対象です。何を正として照合するかが確定していません。

**推奨修正**: 実装開始前に修正版動的モックの承認記録を記入してください。規範にしないのであれば、「参考であり受け入れ基準ではない」と brief/tasks の双方で明記してください。

### [🟡 Minor] ウィンドウ変化 Requirement に対して回転しか検証されない

**該当箇所**: `kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:86`、`kasane/changes/add-presentation-behavior/specs/dialog-contract/spec.md:90`、`kasane/changes/add-presentation-behavior/tasks.md:7`

**問題点**: Requirement は「ウィンドウ寸法・可視領域インセットの変化」を保証しますが、Scenario/tasks は回転だけです。bounds が変わらず inset だけ変わるケースを検証できません。キーボード回避が Non-Goal なので、IME inset を含むかどうかも明示が必要です。

**推奨修正**: bounds-only と system-bar-insets-only の Scenario を分け、IME inset は対象外であることを明記してください。

### [🟡 Minor] 既存 Scenario テストの存在と tasks の新設前提が一致していない

**該当箇所**: `kasane/changes/add-presentation-behavior/proposal.md:5`、`kasane/changes/add-presentation-behavior/tasks.md:5`、`ios/Tests/KsDialogsTests/DialogMultiDisplayTests.swift:35`、`android/ksdialogs/src/test/kotlin/jp/kamusoft/ksdialogs/DialogMultiDisplayTests.kt:38`

**問題点**: iOS/Android には既に多段表示4ケース相当のテストがあります。Android を instrumented に昇格する意義はありますが、「未存在なので4本追加」と読める tasks では重複作成や旧テストとの期待値分裂が起こります。また「同名」の機械的基準となる安定IDも定義されていません。

**推奨修正**: 既存テストを「再利用・改名・instrumented へ移行・残す」に分類し、各 Scenario に `PB-MD-01` のような共通IDを付けて両 Native テストへ対応付けてください。

## アクションプラン

1. `none`、OS 発消失、呼び出し元キャンセル時の公開結果を決定する。
2. transition のスナップショット点と閉鎖状態機械、例外・競合規則を仕様化する。
3. 各形態の公開 API 表と MAUI bridge protocol を確定する。
4. system bars の API-level別契約と検証ケースを追加する。
5. concepts 追随範囲と動的モック承認を完了する。
6. 既存テストを棚卸しし、Scenario ID と tasks の対応を修正する。

**最終判定: NEEDS_DISCUSSION**


---

## 突き合わせ結果 (2026-08-21、ホスト自己レビューは指摘ゼロ → 全件「相方のみ」を根拠で判定)

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| Major 1 | none と常時オーバーレイフェードの矛盾 | 採用 | design Decision 2 (none = コンテンツのみ無演出、覆いは既定フェードし完了待ち、none は引数なし)、spec PB-TR-17 |
| Major 2 | 採用時点・状態遷移が未定義 | 採用 | design Decision 10 (状態機械、属性と同時点の採用、presentation 中の閉鎖は直列、同一インスタンス)、spec PB-TR-04/05/06 |
| Major 3 | スレッド・例外・キャンセル方針なし | 採用 | design Decision 5-5/5-6、spec「フックの失敗」PB-TR-14/15、PB-AA-03、PB-MA-03 |
| Major 4 | 「確定」と「返却」の衝突、Impact 不足 | 採用 | design Decision 5-1 (ラッチと配送)、spec「結果のラッチと配送」PB-TR-10〜13、proposal Impact |
| Major 5 | 呼び出し元キャンセルの観察結果が未確定 | 採用 | design Decision 5-6 (Swift .cancelled / Kotlin CancellationException / MAUI 経路なし、NonCancellable)、spec PB-TR-08、PB-KC-01 |
| Major 6 | OS 発消失の例外が ADR-0017 と衝突、原因識別不能 | 採用 (オーナー承認) | ADR-0017 文言訂正 (ライブラリ発の全閉鎖経路)、design Decision 5-2/5-3 (DismissalOrigin)、tasks 2.1 |
| Major 7 | 公開 API の精度不足 | 採用 | design Decision 3 の API 表 (型・factory・既定値・方向 enum の RTL 追随・zoom 倍率・duration ≤ 0・禁止形) |
| Major 8 | MAUI ブリッジ設計・タスク欠落 | 採用 | design Decision 8、tasks グループ4 (ObjC / Java 互換面・binding・C# アダプタ)、spec PB-MA-02〜04 |
| Major 9 | システムバーの保証範囲、iOS 先送り | 採用 | design Decision 9 (API レベル別保証表、採用1回、iOS は裏取り済みで Scenario 化)、spec PB-SB-01〜05、PB-IA-03 |
| Major 10 | concepts 追随範囲の不足 | 採用 | tasks 7.1 (transition-semantics.md 新設)、7.2〜7.4 の更新範囲拡大 |
| Major 11 | 動的モックの承認未完了 | 採用 (進行中) | brief.md に「参考であり受け入れ基準ではない」を明記、承認記録はオーナー確認後 |
| Minor 1 | 寸法変化の Scenario が回転のみ | 採用 | spec PB-WN-02/03 追加、IME 対象外を明記 |
| Minor 2 | 既存テストと tasks の不整合、ID 不在 | 採用 | design Decision 7 (既存4本温存 + ID 付与、instrumented は実ウィンドウ要のみ)、全 Scenario に PB-xx-NN 付与、tasks 1.1〜1.3 |

降格: 0件 / 未解決: 0件。オーナーによる的外れ却下なし (ksn-lesson 捕捉対象なし)。
