# レビュー結果: rollout-user-docs (007 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

iOS Skill の英日 14 ファイルは、対象 8 concept と現行の Swift 公開実装・テスト・Sample に対して内容上の drift がなく、構成、frontmatter、閉世界性、翻訳ロックステップ、コード例のコンパイルも成立している。一方で、予定 manifest の `ksdialogs-ios/SKILL.md` にファイルが実際に依拠する concept が揃っておらず、3e が報告する未掲載 API 候補のオーナー仕分けも完了していないため、初期生成の追従性と「簡潔でも網羅」の完了条件を満たさない。加えて、最低環境が iOS 17 / Swift 6.3 であるにもかかわらず、SwiftUI レシピの一部がレガシーな Observation 手段と不要な配列化を案内している。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` (対象が `skills/**` で、3e 未掲載名の仕分けを確認)
- `kasane/handbook/cross/test-execution.md` (iOS 公開 API の正コンパイル検査とテスト実行範囲を確認)
- `kasane/handbook/cross/local-development-setup.md` (Swift tools / iOS Deployment Target と Sample の参照方式を確認)
- `kasane/handbook/cross/runtime-behavior-verification.md` (表示・dismiss・多段表示の実行時主張の証拠を確認)
- `kasane/handbook/cross/sample-parity.md` (Sample の Dialog / Loading / Toast 経路と撮影状態を確認)
- `kasane/handbook/cross/comment-policy.md` (利用者向けコード例のコメントと公開文面を確認)
- `swift-ui-impl-skill` (Observation データフローと `ForEach` の modern API 規律を確認)

## 確認結果と証拠

- `core/api/` の 8 concept を 1 件ずつ照合した。登録・型付き結果・多段表示は `ios/Sources/KsDialogs/Presentation/Dialog.swift`、`ios/Sources/KsDialogs/Registry/DialogViewRegistry.swift`、`ios/Tests/KsDialogsTests/DialogMultiDisplayPresentationTests.swift`、`ios/Tests/KsDialogsTests/DialogResultRouteTests.swift`、モデル結合は `ios/Sources/KsDialogs/Contract/DialogViewModel.swift`、`ios/Sources/KsDialogs/Contract/DialogNotifier.swift`、`ios/Tests/KsDialogsTests/DialogTypedShowTests.swift`、レイアウトは `ios/Sources/KsDialogs/Contract/DialogOptions.swift`、`ios/Sources/KsDialogs/Contract/DialogPlacement.swift`、`ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift` とレイアウト case table、演出は `ios/Sources/KsDialogs/Contract/DialogTransition.swift` と transition tests、Loading / Toast は各 `KsLoading` / `KsToast` 契約、coordinator、registry、style、contract / coalescing / progress / non-modal / multi-display tests と一致した。
- Setup は `ios/Package.swift` の `swift-tools-version: 6.3`、product `KsDialogs`、`.iOS(.v17)` と一致する。SwiftPM 配布 URL はルート README と配布方針の現行値にも一致する。
- `xcodebuild build-for-testing -scheme KsDialogs -destination 'generic/platform=iOS Simulator' -derivedDataPath <一時領域>` を実行し、`TEST BUILD SUCCEEDED`。製品コード・テストを変更しない本 change の合意済み例外に従い、全テスト実行は省略した。
- 現行モジュールに対し、`skills/en/ksdialogs-ios/` の `SKILL.md` と references 6 本から Swift コードブロックをファイル単位で抽出し、Swift 6.3.2 / `arm64-apple-ios17.0-simulator` で全 7 ファイルを `swiftc -typecheck` した。すべて成功した。日本語版の対応コードは byte 一致するため同じ結果になる。
- 予定 manifest を入力に frontmatter、英日見出し階層、コードブロック byte 一致、concept 網羅、内部リンク解決を実行し、順に `frontmatter OK`、`en/ja heading structure OK`、`code blocks byte-identical`、`concepts coverage OK`、`All internal links resolve`。iOS Skill 14 ファイルに対する local-path / identity lint も exit 0、内部用語・interop 名・表記ゆれ grep は 0 件だった。
- frontmatter は英日とも許可フィールドだけを持ち、`license: MIT`、言語に合う `metadata.language`、`metadata.source: https://github.com/kamusoft/KsDialogs` を確認した。日本語 `description` には `KsDialogs`、`SwiftUI`、`UIKit`、`iOS`、`Dialog`、`Loading`、`Toast` などの発火用英語キーワードがある。
- 英日 7 ペアの地の文を通読し、見出し・コードだけでなく意味も対応していることを確認した。外部 URL は frontmatter の source と SwiftPM 配布座標だけで、リンクは同一 Skill 内だけ。`kasane/`、ADR 番号、change-id、`KsDialogsInterop*`、ローカル絶対パス、個体情報は含まれない。
- `ui/brief.md` の iOS 3 状態を実画像で確認した。Dialog は `basic-dialog`、Loading は 50% / `Soon...`、Toast は 3 枚完全表示で、`assets/` 3 枚は採用元 `ui/references/` とそれぞれ MD5 が一致する。iOS Toast の実メニュー操作は `deviation.md` に合意済み差分として記録されており、違反として扱わない。
- `tasks.md` の 5.1 は機械検査の実行・報告として虚偽チェックではない。6.1、6.3、6.4 は残件に対応して未チェックであり、現時点の状態を正直に表している。

## 指摘事項

### 🟠 Major: iOS の `SKILL.md` が依拠する concept を予定 manifest が追跡していない

**該当箇所**: `skills/en/ksdialogs-ios/SKILL.md:12`、`skills/en/ksdialogs-ios/SKILL.md:18`、`skills/en/ksdialogs-ios/SKILL.md:19`、`skills/en/ksdialogs-ios/SKILL.md:20`、`skills/en/ksdialogs-ios/SKILL.md:21`、`skills/en/ksdialogs-ios/SKILL.md:22`、`skills/en/ksdialogs-ios/SKILL.md:23`、`skills/en/ksdialogs-ios/SKILL.md:46` (日本語版も同じ対応行)、予定 manifest の `ksdialogs-ios/SKILL.md` entry

**問題点**: 本文は登録・show だけでなく、型付き結果、ViewModel 通知、レイアウト、演出、Loading、Toast、多段表示を概念説明・能力マップ・振り分けで断定している。しかし予定 manifest の当該 entry は `core/api/registration-show-semantics.md` だけである。`user-skills` spec が独立レビューに要求する「各ファイルの内容が依拠する concept がその `targets` にすべて載る」を満たさず、将来それらの concept が変わっても `SKILL.md` が要追従にならない。

**推奨修正**: `ksdialogs-ios/SKILL.md` の source に、現行の `registration-show` に加えて `result-notification`、`multi-display`、`model-binding`、`layout`、`transition`、`loading`、`toast` の各 semantics を追加する。planned manifest を再生成し、concept coverage だけでなくファイル単位 source 完全性を再レビューする。

### 🟠 Major: 3e の iOS 未掲載候補が「掲載または承認済み除外」へ仕分け切れていない

**該当箇所**: `kasane/handbook/cross/user-skill-api-listing.md:18`、`kasane/handbook/cross/user-skill-api-listing.md:20`、`kasane/handbook/cross/user-skill-api-listing.md:34`、`kasane/handbook/cross/user-skill-api-listing.md:36`、`tasks.md:47`

**問題点**: planned manifest に対する `api-coverage-check.py` は iOS について 8 concept 中 7 concept から未掲載候補を引き続き報告する。現行除外表で仕分け済みの値も一部含むが、`Loading.Instance` / `LoadingCoordinator`、`Dialog.Instance` / `IKsDialogs` / `ShowAsync`、`CancellationException`、`Toast.Instance` / `IKsToast`、`TimeInterval` / `UITimingCurveProvider` など、表にない候補が残る。多くは別 platform 名・内部型・標準型と見られるが、規約はレビュアーや実装者の独断除外を禁じ、報告名を掲載かオーナー承認済み除外のどちらかへ仕分けることを要求している。task 6.3 が未チェックなのは正直だが、現行状態は初期生成の完了条件ではない。

**推奨修正**: planned manifest で 3e を再実行し、iOS の報告候補を重複整理した一覧としてオーナーへ提示する。iOS 公開面として必要な名前は Skill 内の到達可能な説明へ追加し、別 platform・内部層・機械検査由来の名前は理由と基準を付けて現行除外表へ追加する。全候補の判断後に task 6.3 を完了し、Skill 修正があれば 6.4 の機械検査と独立再レビューを行う。

### 🟡 Minor: SwiftUI レシピが現行最低環境で不要なレガシー状態管理と配列化を案内する

**該当箇所**: `skills/en/ksdialogs-ios/references/view-models.md:6`、`skills/en/ksdialogs-ios/references/view-models.md:11`、`skills/en/ksdialogs-ios/references/view-models.md:27`、`skills/en/ksdialogs-ios/references/view-models.md:45`、`skills/en/ksdialogs-ios/references/view-models.md:50`、`skills/en/ksdialogs-ios/references/view-models.md:62`、`skills/en/ksdialogs-ios/references/view-models.md:110`、`skills/en/ksdialogs-ios/references/loading.md:72`、`skills/en/ksdialogs-ios/references/loading.md:77`、`skills/en/ksdialogs-ios/references/loading.md:88` (日本語版も byte 一致)

**問題点**: Skill 自身が iOS 17 / Swift 6.3 を最低環境とし、Sample も `Observation` / `@Observable` を使っているが、利用者向けレシピは理由なく `ObservableObject` / `@Published` / `@ObservedObject` を採用している。さらに `ForEach(Array(viewModel.choices.enumerated()), ...)` は `enumerated()` を直接渡せる現行 SwiftUI で不要な配列生成を行い、`swift-ui-impl-skill` の明示規律にも反する。コンパイルは成功するものの、新規利用者へレガシー形と余分な割り当てを推奨する。

**推奨修正**: 状態を共有する ViewModel レシピを `Observation` / `@Observable` と、必要な箇所の `@Bindable` に置き換える。`ForEach(viewModel.choices.enumerated(), id: \.offset)` を直接使う。英日コードブロックを同時更新し、byte 一致と7ファイルの型検査を再実行する。

## アクションプラン

1. 予定 manifest の `ksdialogs-ios/SKILL.md` source を、本文が依拠する 8 concept 全体へ補正する。
2. 3e の iOS 候補を全件オーナー判断へ送り、掲載または理由付き除外へ仕分けて task 6.3 を完了する。
3. SwiftUI レシピを Observation と直接 `enumerated()` に更新し、英日をロックステップで保つ。
4. planned manifest を使った機械検査、iOS コードブロック型検査、独立レビューを再実施する。
