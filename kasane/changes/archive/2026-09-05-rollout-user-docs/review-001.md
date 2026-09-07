# レビュー結果: rollout-user-docs — ksdialogs-ios (001 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

`ksdialogs-ios` の英日ペアは、指定された 7 ファイル構成、frontmatter、見出し構造、コードブロック、セットアップ、公開 API の主要レシピ、閉世界性について概ね要件を満たしている。対応する全 concept と iOS 公開実装をファイル単位で照合し、英語側の全 Swift コードブロックもビルド済み製品モジュールに対して型検査できた。

ただし Toast の既定 duration を存在しない static API 名で案内しており、また複数 Dialog の下段を先に終了したときの iOS 固有の結果セマンティクスが利用者だけでは理解できない形で欠落している。いずれも利用者が公開契約を誤認するため Major とし、差し戻す。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` (きっかけ: 利用者向け Skill の API 掲載と source completeness) — Skill だけで導入から目的達成まで閉じること、公開 API 名を発見可能にすること、意図的な除外は除外リストで管理することを全節で照合した
- `kasane/handbook/cross/test-execution.md` (きっかけ: change の完了判定) — `proposal.md` Impact と `tasks.md` 9.1 にある「製品コード・テスト不変更時は全ビルドルート全件実行を省略」の合意済み例外を適用した。対象差分が利用者文書だけであることを確認し、全テストは要求しなかった
- 適用外と判定: `sample-parity.md` (Sample 実装変更なし)、`runtime-behavior-verification.md` (実行時挙動変更なし)、`local-development-setup.md` (開発環境構築変更なし)、`aiforms-origin-reference.md` (未移植機能の実装・調査でない)

## 検証証拠

- `/tmp/docs-refresh-ksdialogs-manifest-planned.json` の `targets` を基準に、`SKILL.md` と `references/{dialogs,view-models,layout,transitions,loading,toast}.md` の各ファイルを、対応する core/api concept 8 本と 1 件ずつ照合した。指定 target の欠落・余分な iOS reference ファイルはなかった
- 英日とも `SKILL.md` + reference 6 本の計 7 ファイル。`SKILL.md` の frontmatter は `name` / `description` / `license: MIT` / `metadata.language` / `metadata.source` のみで、source は `https://github.com/kamusoft/KsDialogs`
- `heading-parity-check.py` は `en/ja heading structure OK`、`code-block-parity-check.py` は `code blocks byte-identical`、`frontmatter-check.py` は `frontmatter OK`、`api-coverage-check.py` は `API-name coverage OK`
- ja の description は `KsDialogs`、`SwiftUI / UIKit API`、`iOS`、`Dialog`、`Loading`、`Toast`、`layout`、`transition` を含み、英語トリガー語で検索可能
- `skills/{en,ja}/ksdialogs-ios/**` に内部パス、change / ADR、内部 interop、実装専用語の漏出はなかった。`scripts/local-path-lint.py` と `scripts/identity-lint.py` も成功した。相対リンクは同 Skill 内の既存 reference だけを指している
- `ios/Package.swift` の Swift tools 6.3 / iOS 17、および `Dialog` / `Loading` / `Toast`、各 registry、layout、transition、style の公開実装と照合した。`xcodebuild build -scheme KsDialogs -destination 'generic/platform=iOS Simulator'` は `BUILD SUCCEEDED`。英語 7 ファイルの全 Swift コードブロックをビルド済み `KsDialogs` モジュールに対して Swift 6 / iOS 17 target で `swiftc -typecheck` し、全件成功した
- `ui/brief.md` は新規 UI ではなく README 用スクリーンショットの承認記録であり、iOS Skill の文面・コード例との衝突はない。`deviation.md` の iOS Toast は同一表示経路の撮影操作だけの合意済み差分で、Skill の公開契約を変更するものではない

## 指摘事項

### [🟠 Major] Toast の既定 duration を存在しない static API として案内している

**該当箇所**: `skills/en/ksdialogs-ios/references/toast.md:3`、`skills/ja/ksdialogs-ios/references/toast.md:3`

**問題点**:
両言語とも、`duration` 省略時は `ToastStyle.defaultDuration` を使うと型名付きで記載している。しかし `defaultDuration` は `ToastStyle` の instance property (`ios/Sources/KsDialogs/Contract/ToastStyle.swift:30`) であり、static member ではない。公開 static 定数は `ToastStyle.builtinDefaultDuration` (`ToastStyle.swift:36`) だけである。実装も、まず現在の `Toast.shared.style.defaultDuration` に相当する style snapshot を使い、値が 0 以下の場合に `ToastStyle.builtinDefaultDuration` へフォールバックする (`ios/Sources/KsDialogs/Presentation/ToastCoordinator.swift:117-124`)。

コード例は正しく `ToastStyle.builtinDefaultDuration` を使うため型検査を通るが、冒頭の prose にある `ToastStyle.defaultDuration` を利用者がそのまま API として使うとコンパイルできない。公開 API の正確性と、Skill 単体で完結するという規約に反する。

**推奨修正**:
英語は例えば「omitting it uses the current `Toast.shared.style.defaultDuration` (falling back to `ToastStyle.builtinDefaultDuration` when invalid)」、日本語も意味等価に「省略時は現在の `Toast.shared.style.defaultDuration` を使い、無効値なら `ToastStyle.builtinDefaultDuration` へフォールバックする」とする。修正後も英日コードブロックの byte 一致を維持する。

### [🟠 Major] 下段 Dialog を先に終了したときの iOS 固有セマンティクスが閉世界になっていない

**該当箇所**: `skills/en/ksdialogs-ios/references/dialogs.md:78`、`skills/ja/ksdialogs-ios/references/dialogs.md:78`

**問題点**:
現在は「下のダイアログを先に閉じたときの見え方は iOS の提示挙動に従う」とだけ書かれているため、この Skill だけを読む利用者は実際に何が起き、各 `show` がどの結果で戻るかを判断できない。source の `kasane/concepts/core/api/multi-display-semantics.md:35-44` は、アプリが保持した下段の `DialogNotifier` へ先に結果を報告すると、iOS では提示の連なりごと上下とも消え、下段は報告した結果、上段は `.cancelled` で返り、上段の退出 hook は実行されない、と明記している。

この差は単なる「見え方」だけではなく上段 `show` の観測可能な結果に及ぶ。現行の直前文「each show keeps its own result / 各 show は独立した結果を持つ」と曖昧な OS 依存文だけでは、上段が表示されたまま独立して完了できるとも読める。target に指定された concept の重要な iOS 契約を欠き、利用者向け閉世界性と source completeness を満たさない。

**推奨修正**:
英日双方で、(1) 通常のユーザー操作は手前だけに届く、(2) 保持した下段 notifier をアプリが先に完了すると iOS では上下とも消える、(3) 下段は報告値、上段は `.cancelled` で戻る、(4) OS が上段の器を除去するため上段の退出 hook は走らない、を簡潔に明記する。concept や内部パスへ読者を誘導せず、Skill 内で完結させる。

## アクションプラン

1. `references/toast.md:3` の英日ペアで、既定 duration の参照先を実在する instance / fallback API に修正する
2. `references/dialogs.md:78` の英日ペアへ、下段 notifier を先に完了した場合の iOS 固有の表示・結果・退出 hook セマンティクスを補う
3. 修正後、見出し構造・コードブロック byte 一致・frontmatter・閉世界性の機械検査を再実行し、該当 2 reference と公開実装・concept の再レビューを行う
