# セカンドオピニオン: add-loading-toast-typed-show (code-001)
**相方**: codex / **label**: so-code-add-loading-toast-typed-show / **日付**: 2026-09-06 / **対象**: 作業ツリーの未コミット変更すべて (HEAD 4b3e3ad との差分。ios / android / kmp androidHostTest / maui / samples / scripts)
---
**判定: CHANGES_REQUESTED — Critical 0 / Major 1 / Minor 3 / Suggestion 0**

## 指摘事項

### [🟠 Major] 公開 contract への必須メンバー追加が既存実装を破壊する

**該当箇所**: `ios/Sources/KsDialogs/Presentation/KsLoading.swift:59`、`ios/Sources/KsDialogs/Presentation/KsToast.swift:71`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsLoading.kt:89`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/KsToast.kt:96`、`maui/KsDialogs.Maui/Presentation/IKsLoading.cs:103`、`maui/KsDialogs.Maui/Presentation/IKsToast.cs:107`

**問題点**: 型指定 API が公開 protocol/interface の新しい抽象要件になっているため、既存の利用者実装・fake・adapter は、その API を使わなくても再コンパイル時に実装追加を要求されます。実際、この diff 自身も既存テスト double に未使用の throwing stub を追加しており、破壊性を実証しています（`kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/AndroidLoadingGatewayContractTests.kt:116`、`AndroidToastGatewayContractTests.kt:72`）。

これは `proposal.md:31` の「破壊的変更なし」と両立せず、`deviation.md` もありません。更新後の正の compile 検査が成功しても、既存準拠型の互換性は証明できません。

**推奨修正**: Swift の protocol extension、Kotlin/C# の default interface implementation などを用いて、既存準拠型が新メンバーを実装しなくても成立する互換経路を設けてください。併せて、変更前相当の最小準拠型を新メンバーなしでコンパイルする互換検査を追加してください。完全な既定実装では仕様のスナップショット保証を満たせない場合は、「非破壊」という前提自体をオーナー判断へ戻す必要があります。

### [🟡 Minor] Native の Toast 失敗テストが「警告ログ」を検証していない

**該当箇所**: `ios/Tests/KsDialogsTests/ToastTypedShowTests.swift:130`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastTypedShowTests.kt:125`、`android/ksdialogs/src/androidTest/kotlin/jp/kamusoft/ksdialogs/ToastTypedShowTests.kt:221`

**問題点**: TS-TY-04/08 は「警告ログ + 1枚破棄」を失敗分類として要求します。現在の Native テストは破棄・後続継続・factory 非実行を確認していますが、警告が残ったことは検証していません。`Logger.warning` / `Log.w` が削除されても、Native 2 系統の全 Scenario テストは成功します。MAUI テストだけは警告まで確認できています。

**推奨修正**: coordinator に内部用の警告通知口を注入できるようにするか、プラットフォームログを観察し、TS-TY-04（Android は TS-TY-08 も）で警告回数と失敗理由を検証してください。

### [🟡 Minor] 動作証跡の媒体が Kasane の許可された置き場にない

**該当箇所**: `kasane/changes/add-loading-toast-typed-show/verification/sample-walkthrough/notes.md:1`

**問題点**: 16 枚の PNG が `verification/sample-walkthrough/` にあります。`ksn-core/references/ui-artifacts.md` の媒体ホワイトリストでは、実機・Simulator の動作証跡は `changes/<id>/evidence/`、視覚照合画像は `ui/verification/` に限定されています。本 change は UI 変更なしなので、現在の配置はいずれにも該当しません。

**推奨修正**: notes と PNG を `kasane/changes/add-loading-toast-typed-show/evidence/sample-walkthrough/` へ移し、参照があれば更新してください。

### [🟡 Minor] 公開 KDoc に内部 ADR ID を新たに追加している

**該当箇所**: `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingViewRegistry.kt:8`、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ToastViewRegistry.kt:8`

**問題点**: 公開型の KDoc に `core/ADR-0035` が追加されています。`comment-policy.md` は公開 doc comment に ADR ID、change、Kasane などの内部用語を含めないよう要求しています。lint 0 件でも、この類型は advisory／文脈判定なので適合の証明にはなりません。

**推奨修正**: ADR ID を削除し、既に後続段落にある「2スロット・スロット単位の後勝ち・独立レジストリ」という利用者向け契約だけで説明してください。

## サマリー

スナップショット解決、Loading の configure 完了後の合流、合流側での View factory 非実行、Toast の受理後失敗、MAUI DI 自動配線は、実装上は仕様に沿っています。提示された全ルート成功結果も受領済みとして扱い、再実行はしていません。

ただし、3言語すべての公開 contract を抽象メンバー追加で破壊している点は、変更の「非破壊」前提に反するため承認できません。あわせて、Toast の警告ログ検証、証跡配置、公開コメント規約を修正する必要があります。


## 突き合わせ結果 (ホスト review-001 との照合、2026-09-06)

| 相方の指摘 | ホスト側 | 採否 | 根拠 |
|---|---|---|---|
| Major: 公開 contract への必須メンバー追加が既存実装 (利用者の fake / adapter) を破壊する | 指摘なし | **降格 (修正サイクルは回さない。完了報告でオーナーに残論点として提示)** | Dialog の型指定 show (core/ADR-0019〜0021、accepted) が同じ形で契約に抽象メンバーを足した先例があり、proposal は「Dialog と完全に同型」を採用済み (core/ADR-0035)。proposal の「破壊的変更なし」は既存の呼び出し側 (インスタンス渡し show / インライン factory 版) の互換を指す。既定実装 (protocol extension / default interface method) で吸収する案は契約の形を変える設計判断で、実装フェーズの修正ではなく explore / propose の権利 |
| Minor: Native の Toast 失敗テスト (TS-TY-04 / 08) が警告ログを検証していない | 指摘なし | **降格** | 既存の受理後失敗の Scenario (TS-CO-07 系) も破棄と他表示への無影響だけを見る流儀で、警告の観測口は Native 2 実装に無い (MAUI は TraceListener で観測可)。観測口の追加は spec に無い内部 API の追加になる。蒸留時の参考として残す |
| Minor: 証跡 PNG が `verification/sample-walkthrough/` にある (ui-artifacts の置き場は evidence/ か ui/verification/) | 指摘なし (証跡 2 枚を開いて notes.md と一致を確認) | **降格** | specs/samples/spec.md と tasks.md 4.2 が `verification/` を明示し、archive の add-loading も `verification/sample-walkthrough/` に置いた先例あり。置き場の規約と Sample 通し証跡の関係は ksn-drift / 蒸留の論点 |
| Minor: 公開 KDoc (`LoadingViewRegistry.kt:8` / `ToastViewRegistry.kt:8`) に `core/ADR-0035` を新たに追加 | Suggestion 1 (同 change 内で MAUI は削除・Android は追加で方針が割れる) | **確定 (Minor)** | comment-policy「公開 doc コメントに内部用語を使わない」に該当。オーケストレーターが直接修正 (Android 2 ファイルの class doc から ADR ID を除去)。review-002 の指摘で iOS の 2 レジストリ (`LoadingViewRegistry.swift` / `ToastViewRegistry.swift`) も本 change が同じ行を書き換えていたと判明したため、同様に ADR ID を除去 (2026-09-06 追記) |

採用 0 / 確定 1 / 降格 3 / 未解決 0。

### オーナー確認 (2026-09-06)

降格した Major (公開 contract への抽象メンバー追加) について、オーナーが **A (このまま進める)** を選択。proposal の「破壊的変更なし」は「契約を呼ぶ側 (既存のインスタンス渡し show / インライン factory 版の呼び出し) の互換」を指し、契約を自前で実装する利用者のテストダブル (モック) が再ビルド時に新メンバーの実装を求められることは Dialog の型指定 show (core/ADR-0019〜0021) と同じ扱いで許容する。既定実装で吸収する案 (B) は採らない。蒸留時に「破壊的変更なし」の意味 (呼ぶ側の互換) を concepts に明文化する。
