# レビュー結果: rollout-user-docs (009 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

`ksdialogs-ios` の英日 14 ファイルを、割り当て対象の core/api 8 concept、現行の Swift 公開実装・テスト・Sample、予定 manifest、delta specs、design、deviation、UI brief と独立に照合した。ファイル単位の源泉割当、task 3e の掲載・除外分類、iOS 17 / Swift 6.3 前提の Observation コード、公開 API と挙動、Agent Skills 構造、翻訳ロックステップ、閉世界性に不整合は見つからなかった。英語版 7 ファイルの全コードブロックも現行モジュールに対して typecheck に成功し、iOS tests と Sample build も成功したため、iOS Skill 側に修正要求はない。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — `skills/**` の API 名網羅検査の仕分けに適用
- `kasane/handbook/cross/test-execution.md` — iOS テストの実行と件数報告に適用
- `kasane/handbook/cross/local-development-setup.md` — iOS Sample のビルドに適用
- `kasane/decisions/cross/0011-user-docs-as-agent-skills.md` — Agent Skills の構造、英日ロックステップ、閉世界性の accepted decision として適用
- `kasane/decisions/cross/0002-tech-stack-2026-08.md` — iOS 17 / Swift 6.3 と Observation の accepted decision として適用
- `kasane/decisions/cross/0005-public-identifier-mapping.md` — Swift module / product の公開識別子に適用

## 検査証拠

### 1. 8 concept とファイル単位の源泉完全性

- `design.md:15-30` が定める利用目的別 6 references と、`design.md:61-78` が定める初期割当を、`/tmp/docs-refresh-ksdialogs-manifest-planned.json:6-35` および英日各ファイルの実内容と突き合わせた。
- `SKILL.md` は `layout` / `loading` / `model-binding` / `multi-display` / `registration-show` / `result-notification` / `toast` / `transition` の 8 本すべてを源泉に持ち、能力マップと reference routing が全機能へ到達させている (`skills/en/ksdialogs-ios/SKILL.md:12-23`, `skills/en/ksdialogs-ios/SKILL.md:44-51`)。
- `dialogs.md` は登録・型付き結果・インライン表示・多段表示を扱い、割当 3 本と一致する (`skills/en/ksdialogs-ios/references/dialogs.md:1-99`)。`view-models.md` は notifier、VM factory、configure、同一インスタンス制約を扱い `model-binding` と一致する (`skills/en/ksdialogs-ios/references/view-models.md:1-116`)。
- `layout.md` / `transitions.md` / `loading.md` / `toast.md` は、それぞれ対応する単一 concept の公開契約を利用者レシピへ翻訳している。Loading / Toast 内の transition 添付は、それぞれの concept 自身がカスタム View の演出差し替えとして規定する内容であり、源泉不足ではない。
- 予定 manifest を入力に `concepts-coverage-check.py` を実行し、`concepts coverage OK` を確認した。iOS 7 ファイルの内容が依拠する concept は予定 manifest にすべて存在し、過剰・不足の割当はない。

### 2. task 3e の全候補分類

- 予定 manifest に対する `api-coverage-check.py` の iOS 出力を全件照合した。最初の個別候補 10 件は `kasane/handbook/cross/user-skill-api-listing.md:40-49`、残る grouped 候補は同 `:135-142` に一つ残らず掲載されている。
- iOS 出力に、現行除外リストに無い未掲載候補は 0 件だった。掲載すると分類された iOS API は、共有入口と契約 (`SKILL.md:12-23`)、layout API (`layout.md:1-83`)、transition API (`transitions.md:1-71`)、Dialog error cases (`view-models.md:39-90`)、Loading / Toast の style・registry・呼び出し面 (`loading.md:1-106`, `toast.md:1-95`) に反映済みである。
- handbook の方針「簡潔でも網羅」(`kasane/handbook/cross/user-skill-api-listing.md:16-20`) と、除外を独断で増やさない禁止 (`:149-153`) の双方に適合する。task 6.3 の成果状態は満たしており、チェックボックス更新はオーケストレーター側のライフサイクル処理として本レビューでは変更していない。

### 3. iOS 17 / Observation と公開実装・テスト・Sample

- Setup の Swift 6.3 / iOS 17 / product `KsDialogs` は `ios/Package.swift:1-27` と一致する。`@Observable` と `@Bindable` を使う ViewModel レシピ (`skills/en/ksdialogs-ios/references/view-models.md:5-36`, `:43-85`) は最低 OS の範囲で互換分岐を要せず、配列化を伴わない `ForEach(indices)` (`:92-116`) も SwiftUI 規律に適合する。
- Dialog の instance / inline SwiftUI / 型指定 show と configure は `ios/Sources/KsDialogs/Presentation/Dialog.swift:31-95`、notifier と class / Sendable 制約は `ios/Sources/KsDialogs/Contract/DialogViewModel.swift:1-35`、layout / transition 添付は `ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift:45-72` と一致する。
- Loading の registry / style / options / show / hide / setMessage / start と SwiftUI factory は `ios/Sources/KsDialogs/Presentation/Loading.swift:32-141`、Toast の message / registered / inline factory と fire-and-forget の戻り形は `ios/Sources/KsDialogs/Presentation/Toast.swift:31-82` と一致する。custom transition の Hook、preset、`none()` は `ios/Sources/KsDialogs/Contract/DialogTransition.swift:17-45`, `:50-148` と一致する。
- Sample は `@Observable` な custom Loading VM (`samples/ios/KsDialogsSample/CustomLoadingViewModel.swift:1-18`)、Dialog / Loading / Toast の独立 registry (`SampleDialogRegistration.swift:8-68`, `SampleLoadingRegistration.swift:8-14`, `SampleToastRegistration.swift:8-14`) と、文書化された instance / inline / typed show、Loading scope、Toast routes (`SampleMenuModel.swift:86-327`) を実際に使用している。
- 英語版 `SKILL.md` と references 6 本から Swift コードブロックをファイル単位で累積抽出し、ビルド済み現行 `KsDialogs` module に対して `arm64-apple-ios17.0-simulator`、Swift 6、`-strict-concurrency=complete` で `swiftc -typecheck` を実行した。7 / 7 ファイル成功。日本語版の対応コードは byte 一致するため同じ結果になる。
- `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` を `ios/` で実行し、**251 tests / 47 suites、失敗 0** (`TEST SUCCEEDED`)。型付き show、多段表示、notifier lifetime、layout、transition 後の結果配送、Loading の合流・進捗、Toast の duration・fail-fast・非対話を含む現行 suite が通過した。
- 同 Simulator を対象に `KsDialogsSample` Debug build を実行し、`BUILD SUCCEEDED`。製品コード・テスト不変更時の全ビルドルート免除 (`proposal.md:36-43`) は維持し、レビューに必要な iOS ルートだけを限定確認した。

### 4. Skill 構造、英日等価性、閉世界性

- en / ja とも `SKILL.md` + 規定 6 references のみで、frontmatter は `name` / `description` / `license` / `metadata.language` / `metadata.source` の許可形に一致する (`skills/en/ksdialogs-ios/SKILL.md:1-8`, `skills/ja/ksdialogs-ios/SKILL.md:1-8`)。`license` は MIT、source は本リポジトリ URL、name は同一である。
- ja description は日本語本文に `KsDialogs`、`SwiftUI`、`UIKit`、`iOS`、`Dialog`、`Loading`、`Toast`、`layout`、`transition` の英語 trigger keywords を含む (`skills/ja/ksdialogs-ios/SKILL.md:2-7`)。
- `heading-parity-check.py` は `en/ja heading structure OK`、`code-block-parity-check.py` は `code blocks byte-identical`、`frontmatter-check.py` は `frontmatter OK`。本文も全節を対で読み、英日で省略・追加された保証や挙動はない。
- 内部用語・ADR・interop 機械面、Skill root 外相対リンク、ローカル絶対パス、個体情報、配信識別子表記ゆれを検査して 0 件。`link-resolution-check.py` は `All internal links resolve`、追加の root 境界検査も `relative links stay inside each Skill root`。外部 URL は許可された SwiftPM 配布座標と frontmatter source だけで、内部進捗・テスト事情・Sample 内部構造の漏出はない。

### 5. deviation / UI brief / delta specs / task 6.1

- `deviation.md:3-7` の合意済み差分は移行 Skill、README 用 iOS Toast 撮影、継承 lint baseline、docs-refresh の README 例外に関するもので、iOS Skill の契約を変更しない。違反として扱う項目はない。
- `ui/brief.md:1-3`, `:17-19`, `:32-49` は README の既存 Sample スクリーンショット選定だけを UI 対象としており、iOS Skill にモック・画像・UI 実装を要求しない。Skill の UI レシピは現行 public API と Sample に一致する。
- `user-skills` delta spec の構造、frontmatter、本文構成、閉世界性、内容規約、翻訳ロックステップを全件照合した。`tasks.md:43-48` の task 6.1 が要求する「8 concept、ファイル単位の源泉完全性、構成規約、en / ja 等価性、ja trigger keywords」の iOS Skill 分を本レビューで満たす。

## 指摘事項

なし。

## アクションプラン

iOS Skill 側の修正は不要。オーケストレーターは本結果を task 6.1 の iOS Skill レビュー証跡として扱い、変更全体の残りのレビュー・検収ゲートを継続する。
