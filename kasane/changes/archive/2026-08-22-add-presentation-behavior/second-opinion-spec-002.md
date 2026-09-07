# セカンドオピニオン: add-presentation-behavior (spec-002)
**相方**: codex (spec-001 と同一セッションの続き) / **日付**: 2026-08-21 / **対象**: 相方指摘反映後の提案一式 (design Decision 8〜10 新設・Scenario ID 付与版) + 訂正後 ADR-0017
---
# 再レビュー結果: add-presentation-behavior

**日付**: 2026-08-21  
**判定**: NEEDS_DISCUSSION  
**指摘件数**: Critical 0 / Major 7 / Minor 1 / Suggestion 0

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

なお、前回レビューの集計欄は「Major 10」ですが、本文では Major 1〜11 が採番されています。漏れを避けるため、本文の Major 11件と Minor 2件をすべて追跡しました。

## 前回指摘の解消状況

| 前回指摘 | 状況 | 評価 |
|---|---|---|
| Major 1: `none` とオーバーレイフェード | **解消** | `none` はコンテンツのみ即完了、オーバーレイは既定フェード、引数なしと明確化された。 |
| Major 2: 採用時点・状態遷移 | **部分解消** | スナップショット、同一インスタンス、presentation 中の直列化は追加された。ただし created/attached 中の閉鎖と、オーバーレイ出現完了の扱いが未定義。 |
| Major 3: スレッド・例外・キャンセル | **部分解消** | UI thread、失敗の扱い、NonCancellable は追加された。一方、Swift の非 throwing フックでは失敗を通知できず、OS 消失中の実行中フックも未整合。 |
| Major 4: ラッチと show 返却 | **解消** | ラッチと配送が分離され、Impactにも約250msの遅延が明記された。ただし OS 発消失時の例外規則に新しい矛盾がある。 |
| Major 5: 呼び出し元キャンセルの公開結果 | **解消** | Swift / Kotlin / MAUI ごとの観察結果と Kotlin の NonCancellable 撤去が規定された。 |
| Major 6: OS 発消失と ADR・原因識別 | **解消** | ADR-0017 が訂正され、`DismissalOrigin` と配管タスクが追加された。実行中フックの寿命問題は別途新規指摘。 |
| Major 7: 公開 API 精度 | **部分解消** | API表、方向、倍率、禁止形は追加されたが、完全なシグネチャと特殊 duration の扱いが未確定。 |
| Major 8: MAUI bridge | **解消** | 完了コールバック、寿命、UI thread、fault/cancel、多重通知防止、個別タスクが追加された。 |
| Major 9: system bars | **部分解消** | APIレベル表、混在状態、behavior、iOS Scenario は追加された。「一回採用・追随なし」の検証が欠け、tasks に任意化表現が残る。 |
| Major 10: concepts 追随 | **解消** | `transition-semantics.md` 新設を含め、更新対象が具体化された。 |
| Major 11: 動的モック承認 | **解消** | brief/tasks 双方で「参考であり受け入れ基準ではない」と明記されたため、承認空欄は実装開始のゲートではなくなった。 |
| Minor 1: 回転以外のウィンドウ変化 | **解消** | bounds-only、insets-only、IME対象外が追加された。 |
| Minor 2: 既存テストと Scenario ID | **部分解消** | 既存テストの温存とID導入は明確化されたが、4テスト対3 ID、platform別IDと「同名ミラー」の不整合が残る。 |

## 指摘事項

### [🟠 Major] OS 発消失時の配送順・状態遷移・リソース解放が矛盾している

**該当箇所**: [design.md:93](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:93)、[design.md:94](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:94)、[design.md:178](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:178)、[dialog-contract/spec.md:62](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:62)、[registration-show-semantics.md:159](kasane/concepts/core/api/registration-show-semantics.md:159)

**問題点**: Decision 5-1 と Requirement は配送を「フック完了・オーバーレイ消滅・撤去後」としますが、Decision 5-2 は OS 発消失時に結果を先に配送し、実行中フックを完走させるとしています。Decision 10 は同時に `removed` へ直行するとしています。

実行中タスクは引数のホスト View を保持し得るため、`removed` 後も中身が生存します。終了しないフックなら、上位 concept の「全閉鎖経路で宣言的 UI ホストを解放する」保証にも反します。PB-TR-12 は結果値しか確認せず、配送順・タスク終了・ホスト解放を判定できません。

**推奨修正**: OS 発消失を通常の配送順から除外するか、フック終了まで待つかを明示的に選択してください。即時配送を採るなら、framework が所有するフック Task/Job をキャンセルして参照を切る規則を定め、presentation/dismissal 実行中それぞれの OS 消失 Scenario で「配送時刻・撤去・ホスト解放・フックの扱い」を検証してください。

### [🟠 Major] 状態機械が created/attached 中の閉鎖とオーバーレイ出現完了を扱っていない

**該当箇所**: [design.md:168](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:168)、[design.md:174](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:174)、[design.md:176](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:176)、[dialog-contract/spec.md:31](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:31)、[kmp-facade/spec.md:14](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/kmp-facade/spec.md:14)

**問題点**: presentation 中の閉鎖は定義されましたが、factory 内での即時結果報告や提示開始前の caller cancellationなど、`created` / `attached` 中に閉鎖信号が来る場合が未定義です。PB-KC-02 は結果だけを定めていますが、presentation/dismissal の実行有無や状態遷移が決まりません。

また `presenting` は「presentation フック完了」で `shown` になりますが、オーバーレイの出現フェード完了を待ちません。`none` ではコンテンツが即完了するため、オーバーレイが出現中のまま `shown` となり、閉鎖による消滅フェードと競合できます。提示・退出中の入力可否も未定義です。

**推奨修正**: 状態×閉鎖原因の遷移表を追加し、少なくとも以下を固定してください。

- created/attached 中の報告・caller cancellation
- presentation 完了条件が「フックのみ」か「フック＋オーバーレイ」か
- 出現中に退出へ移る場合のオーバーレイアニメーション処理
- presenting/dismissing 中の外側タップ・コンテンツ操作の可否

対応する早期報告、早期キャンセル、`none` 表示直後の閉鎖 Scenario も必要です。

### [🟠 Major] Swift のフック型では失敗を通知できず、失敗 Scenario が実装不能である

**該当箇所**: [design.md:20](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:20)、[design.md:55](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:55)、[design.md:97](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:97)、[dialog-contract/spec.md:84](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:84)、[ios-native/spec.md:30](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/ios-native/spec.md:30)

**問題点**: Swift の型は `@MainActor (UIView) async -> Void` であり、例外を伝播できません。Task cancellation も戻り値上は通常完了と区別できません。それにもかかわらず iOS は PB-TR-14/15 を含む全 PB-TR Scenario の実装を要求され、「例外を投げる Swift フック」という GIVEN を構成できません。

さらに、カスタム presentation フックが alpha/transform を変更した後に失敗した場合、ライブラリは任意の変更を復元できないため「表示状態を継続し、操作できる」ことも一般には保証できません。

**推奨修正**: Swift でも失敗を扱うなら `@MainActor (UIView) async throws -> Void` に変更してください。非 throwing を維持するなら、失敗 Requirement を Kotlin/MAUI に限定し、Swift の PB-TR-14/15 を除外してください。「操作できる」保証は、復元対象プロパティを限定して初期状態へ戻すか、「状態機械は継続する」までに弱める必要があります。

### [🟠 Major] 終了しないフックで show が永久待機し、「宙吊りなし」の説明と矛盾する

**該当箇所**: [proposal.md:29](kasane/changes/archive/2026-08-22-add-presentation-behavior/proposal.md:29)、[design.md:97](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:97)、[design.md:105](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:105)、[dialog-contract/spec.md:62](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:62)、[result-notification-semantics.md:19](kasane/concepts/core/api/result-notification-semantics.md:19)

**問題点**: 「完了＝return なので報告漏れが構造上ない」という説明は成立しません。利用者の async/suspend/Task フックは、未解決 continuation や完了しない Task を待って永久に return しないことができます。dismissal なら結果はラッチ済みでも配送されず、器も撤去されません。presentation なら `shown` に到達しません。

これは上位契約の「show は結果をちょうど1回返す」と、proposal の「宙吊り経路を作らない」に抵触します。

**推奨修正**: 次のどちらかを契約として選んでください。

- timeoutまたは cancellation escapeを設け、撤去・配送を保証する。
- 終了しない利用者フックは契約違反として、結果返却保証の明示的な前提条件にする。

後者でも caller cancellation / hostLost では永久フックから脱出できるよう規定し、終了しないフックを使った Scenario を追加してください。

### [🟠 Major] Scenario ID の「同名ミラー」が platform 分割と自己参照 Scenario により成立しない

**該当箇所**: [ADR-0016:16](kasane/decisions/core/0016-behavior-spec-scenario-tests.md:16)、[dialog-contract/spec.md:126](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:126)、[ios-native/spec.md:30](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/ios-native/spec.md:30)、[android-native/spec.md:55](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/android-native/spec.md:55)、[tasks.md:7](kasane/changes/archive/2026-08-22-add-presentation-behavior/tasks.md:7)

**問題点**:

- iOS/Android spec はともに「PB-MD 各 Scenario」を実装するとしますが、PB-MD-02 は iOS 専用、PB-MD-03 は Android 専用で、tasks も別IDを割り当てています。ADR-0016 の同名ミラーになりません。
- 既存4テストに PB-MD-01〜03を付けるため、どのテストがどの Scenario を担うか一意でありません。特に「外側タップは手前のみ」に対応する独立 Scenario がありません。
- PB-IA-04 / PB-AA-04 は「全IDのテストが存在し green」を要求する自己参照的なメタ Scenario です。通常のテスト自身からテストスイート全体の成功を判定できません。

**推奨修正**: 下先閉じは1つの共通IDに統合し、concepts のOS差分表に基づく期待値だけを各Nativeで変えてください。既存4テストには1対1のIDを用意し、外側タップ用 Scenario を追加してください。PB-IA-04 / PB-AA-04 は削除し、ID網羅性はテスト外の検査スクリプトまたはレビュー項目として管理してください。

### [🟠 Major] 公開 API と duration の境界値がまだ一意に決まらない

**該当箇所**: [design.md:52](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:52)、[design.md:57](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:57)、[design.md:58](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:58)、[design.md:67](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:67)、[dialog-contract/spec.md:100](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/dialog-contract/spec.md:100)

**問題点**: fade は完全な既定値が書かれていますが、slide/zoom は `duration:, easing:` または「同左」で、実際の既定引数付きシグネチャが固定されていません。フックプロパティの getter/visibility、Android constructor の正確な引数名、Swift 6での `any UITimingCurveProvider` を含む宣言形も不明です。

また「0以下」のみ定義され、`TimeInterval.nan` / `.infinity`、`Duration.INFINITE` が未定義です。NaN は0以下に該当せず、無限Durationはプラットフォームのミリ秒変換でオーバーフローや実行時失敗を起こし得ます。PB-TR-18も0しか検証しません。

**推奨修正**: 3形態についてコピー可能な完全シグネチャを提示し、全factoryの既定値とプロパティ可視性を固定してください。NaN・正負Infinity・`Duration.INFINITE` の拒否／即完了／上限丸めを決め、負値と特殊値を別Scenarioにしてください。

### [🟠 Major] system bars の「一回採用・追随なし」を検証できない

**該当箇所**: [design.md:159](kasane/changes/archive/2026-08-22-add-presentation-behavior/design.md:159)、[android-native/spec.md:26](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/android-native/spec.md:26)、[android-native/spec.md:28](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/android-native/spec.md:28)、[tasks.md:42](kasane/changes/archive/2026-08-22-add-presentation-behavior/tasks.md:42)

**問題点**: Requirement は「表示時の1回だけ採用し、表示中のホスト変更には追随しない」と保証しますが、PB-SB-01〜05 は初期状態しか検査しません。実装が継続的に同期してもすべて通ります。

さらに tasks の「instrumented テスト（可能な範囲）」は、SHALL Scenario を実装者判断で省略できる表現です。API 24〜29 / 30+ の証跡対象、端末/API、各Scenarioの自動・手動の区分も確定していません。

**推奨修正**: 表示後にホストの可視状態またはbehaviorを変更し、ダイアログ側が追随しない Scenario を追加してください。「可能な範囲」を削除し、各PB-SB IDについて instrumented test／実機証跡のどちらで受け入れるかを表にしてください。

### [🟡 Minor] Sample の調整値と選択状態が4形態で一意にならない

**該当箇所**: [samples/spec.md:7](kasane/changes/archive/2026-08-22-add-presentation-behavior/specs/samples/spec.md:7)、[ui/brief.md:6](kasane/changes/archive/2026-08-22-add-presentation-behavior/ui/brief.md:6)、[ui/brief.md:28](kasane/changes/archive/2026-08-22-add-presentation-behavior/ui/brief.md:28)、[ui/brief.md:31](kasane/changes/archive/2026-08-22-add-presentation-behavior/ui/brief.md:31)、[mock-preset-motion.html:117](kasane/changes/archive/2026-08-22-add-presentation-behavior/ui/mock/mock-preset-motion.html:117)

**問題点**: 非規範の動的モックには duration 100〜600ms、step 10、初期250msがありますが、brief/specにはありません。また次が未定義です。

- `None` 選択時に時間・イージングを無効化するか
- `Custom Hook` が調整値を使うか
- `Slide Up/Down` と `from: top/bottom` の対応
- easing 4文言から各ネイティブ easing への写像
- 初期選択値

このままでは「同じ操作」の基準が4ルートで割れます。

**推奨修正**: 初期値・範囲・step・選択ごとの有効/無効状態・方向/easing写像をbriefの規範表へ追加し、PB-SM Scenarioで確認してください。

## アクションプラン

1. OS 発消失、終了しないフック、早期閉鎖を含む完全な状態遷移を確定する。
2. Swift の失敗通知方式と公開API完全シグネチャ、duration特殊値を確定する。
3. PB-MD の共通ID、既存テストとの1対1対応、メタScenarioを修正する。
4. system bars の一回採用とSample調整面の受け入れ基準を追加する。

**最終判定: NEEDS_DISCUSSION**



---

## 突き合わせ結果 (2026-08-21)

| # | 指摘 | 採否 | 反映先 |
|---|---|---|---|
| R2-1 | OS 発消失時の配送順・removed 直行・ホスト解放の矛盾 | 採用 | design Decision 5-2 (実行中フックをキャンセルし参照を切って即配送・解放、配送順の明示的例外)、spec PB-TR-09/12/23 |
| R2-2 | created/attached 中の閉鎖、オーバーレイ出現完了、入力可否が未定義 | 採用 | design Decision 10 の状態×閉鎖信号の遷移表 (提示前は演出なし、presentation 完了 = フック + オーバーレイ、dismissing 中の入力無視)、spec PB-TR-19〜22 |
| R2-3 | Swift フックが非 throwing で失敗 Scenario を構成できない、「操作できる」が過大 | 採用 | Swift を `async throws` に (Decision 1・3)、失敗時保証を「状態機械は継続」に弱化 (Decision 5-5、spec「フックの失敗」) |
| R2-4 | 終了しないフックで show が永久待機 | 採用 (オーナー決定: 案C) | Decision 5-8 (前提条件 + 脱出口 (呼び出し元キャンセル / OS 発消失でフックをキャンセル) + デバッグ警告。タイムアウトは不採用)、spec「フックの完了は利用者の責務」PB-TR-24/25、proposal Impact |
| R2-5 | PB-MD の OS 別 ID・既存テストとの非1対1・メタ Scenario | 採用 | Decision 7 (PB-MD-01〜04 を既存4本と1対1、04 は共通 ID で THEN のみ OS 差、05 器消失)、ios/android の「挙動 Scenario テスト」Requirement (メタ) を削除し ID 網羅検査スクリプトを tasks 1.5 に |
| R2-6 | 完全シグネチャ・可視性・duration 特殊値 | 採用 | Decision 3 に3形態の完全シグネチャと「有限の正値でなければ即完了」規則、spec PB-TR-18/26/27 |
| R2-7 | システムバー「1回採用」の検証なし、tasks の任意化表現 | 採用 | Decision 9 に PB-SB-06 と受け入れ方式表、tasks 5.2 の「可能な範囲」削除 |
| R2-m1 | Sample 調整面が非規範 | 採用 | ui/brief.md に「調整面の規範表」(初期値・範囲・写像・None/Custom 時の扱い)、samples spec PB-SM-01 |

降格: 0件 / 未解決: 0件。オーナーによる的外れ却下なし。
