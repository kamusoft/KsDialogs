# レビュー結果: rename-swiftui-transition-modifier (001 回目)

**日付**: 2026-09-06
**判定**: APPROVED

## サマリー

SwiftUI の演出 modifier を `.dialogTransition(_:)` から `.ksDialogTransition(_:)` へ改名する S 級変更。diff は 5 ファイル 5 行の識別子置換のみで、合意済みスコープ (旧名を残さない・テストと doc コメントを追随・concepts / Skill / Sample は触らない) をちょうど満たしている。旧名 `dialogTransition` はソースツリー (`ios/` `samples/` `android/` `maui/`) から完全に消えており、新名は UIKit 側の `UIView.ksDialogTransition` および同ファイル内の `ksDialogOptions` / `ksDialogPlacement` と同じ流儀で揃った。ビルド・テストとも green (251 tests / 47 suites passed、failures 0) で、Scenario 網羅も未網羅なし。Critical / Major の指摘なし。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 触れた doc コメント 1 箇所 (`ios/Sources/KsDialogs/Kmp/KsToastKmp.swift:48`) はリポジトリ内コード識別子への参照のみで、許容参照の範囲内。禁止参照・禁止記述類型の混入なし
- `kasane/handbook/cross/test-execution.md` (テストの実行・結果の報告・完了判定) — ios/ の全件実行コマンドと件数の確認方法、Scenario ID 網羅検査の実行に適用
- `kasane/decisions/core/0002-public-api-shape.md` (accepted) / `kasane/decisions/core/0015-attribute-supply-content-attachment.md` (accepted) — 命名ポリシーと SwiftUI 添付面の定義に照合
- 適用外と判断: `sample-parity.md` (`samples/` 不変更)、`user-skill-api-listing.md` (`skills/` 不変更)、`runtime-behavior-verification.md` (実行時挙動の変更なし)、`local-development-setup.md`、`aiforms-origin-reference.md`
- `kasane/lessons/code-review.md` は存在しないため、重点観点 / 指摘しないことの適用なし

## 検証した内容

**ビルド・テスト** (レビュアー自身で実行、`ios/`):

```
xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=<iPhone 17 / iOS 26.5>'
→ ** TEST SUCCEEDED **  Test run with 251 tests in 47 suites passed (24.3s)
   Executed 0 tests, with 0 failures  ← XCTest 側は 0 件表示 (全テストが Swift Testing 製。regulation どおり)
```

handbook `test-execution.md` の実測値 (251 tests / 47 suites) と一致。ビルド警告なし。

**補助検査**:

- `python3 scripts/scenario-id-coverage.py` → 179/204 (除外 25 件)、**未網羅なし**。`[PB-IA-02] SwiftUI modifier での添付が器で採用される` は `ios/Tests/KsDialogsTests/DialogTransitionAttachmentTests.swift:34` に ID・テスト名とも改名前と同一で残っており、固定している Scenario は変わっていない
- `python3 scripts/comment-policy-lint.py` → 禁止 0 件 (検査対象 919 ファイル)

**旧名の残存**: `dialogTransition` (先頭小文字) の出現を `ios/` `samples/` `android/` `maui/` で走査し **0 件**。定義側 (`ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift:68`)、呼び出し側 (テスト 3 ファイル)、doc コメント (`ios/Sources/KsDialogs/Kmp/KsToastKmp.swift:48`) のすべてが新名に揃っている。deprecated 併存が残っていないことも確認済み (案 A の合意どおり)。

**命名の流儀**: `UIView.ksDialogTransition` (`ios/Sources/KsDialogs/Contract/UIViewDialogAttributes.swift:28`) と同名・同義になり、SwiftUI 側 3 modifier (`ksDialogOptions` / `ksDialogPlacement` / `ksDialogTransition`) の接頭辞が揃った。SwiftUI の `View` 拡張と `UIView` の格納プロパティは別型への追加のため名前衝突は生じない。ADR との整合も確認した — core/ADR-0002 の命名ポリシーは「原典命名が非対称な箇所は対称性を優先して改める」と定めており、本改名はこれに沿う。core/ADR-0015 が SwiftUI 添付面を `.ksDialogOptions(...)` / `.ksDialogPlacement(...)` として記述していることとも整合する。cross/ADR-0005 はモジュール名・パッケージ名・配布 ID の写像表でメンバー名を扱わないため、追記不要という探索時の判断は妥当。

**スコープ**: 変更ファイルは合意済みの 5 ファイルのみ (`git status`)。付随修正・無関係な整形・スコープ外の変更の混入なし。exploration.md にも差分があるが、内容は「探索で確認した現状」「選択肢の表」「決定事項」「変更級の確定」で、実装中の書き換え (実装結果に合わせた事後改変) ではなく、簡易起票から探索完了への昇格そのものと判断した。

## 指摘事項

### [🔵 Suggestion] 追随先の文書が旧名のまま残っている (本 change のスコープ外、後続で確実に拾う)

**該当箇所**: `kasane/concepts/ios/api/transition-surface.md:66,73`、`kasane/concepts/ios/api/dialog-surface.md:67`、`skills/{en,ja}/ksdialogs-ios/references/transitions.md:15`・`toast.md:61`・`loading.md:92`、`skills/{en,ja}/ksdialogs-kmp/references/transitions.md:50`

**問題点**: これらは合意済みスコープで本 change の対象外 (concepts は蒸留、`skills/` は docs-refresh の責務) と決まっており、触れていないこと自体は正しい。ただし現時点でコードと文書が食い違っている状態であり、特に `transition-surface.md` には「接頭辞が付かない」旨の注記が残っているため、追随が漏れると公開面の説明が誤ったまま残る。`skills/` はルート README とともに一般公開する利用者向け成果物なので、影響は内部文書より大きい。

**推奨修正**: 実装側での対応は不要。アーカイブ時の蒸留で concepts 2 ファイル (注記の削除を含む) を、その後の docs-refresh で Skill 6 ファイル (en/ja) を必ず追随させること。exploration.md の「影響範囲 (追随先)」に列挙済みなので、その一覧を消化元として使えばよい。

## 確認した観点 (指摘に至らなかったもの)

- **公開 doc コメント内の ADR ID**: `DialogAttributeAttachment.swift:66` の `(core/ADR-0017)` をはじめ、comment-policy「公開メンバーの doc コメント」節に照らすと要確認となる箇所がある。ただし `--advisory` で 291 ファイル / 438 件が検出されるリポジトリ全体の既存状態であり、本 diff が新たに増やしたものではない (触れた行は識別子のみの変更)。本 change での是正はスコープを大きく越えるため指摘としない
- **負のコンパイル検査**: ios/ の `KSDIALOGS_NEGATIVE_CHECK_SHOW_TRANSITION` / `KSDIALOGS_NEGATIVE_CHECK_OPTIONS_TRANSITION` は `show(transition:)` と `DialogOptions.transition` の不在を固定するもので、検査ソースは改名対象の modifier を参照していない (走査で確認)。本改名では退行しえないため未実行
- **公開 API 形状の正の検査**: `DialogApiSurfaceCompileChecks.attachesTransitionToSwiftUIContent` と `KmpToastApiSurfaceCompileChecks` は非 `@testable` な利用者視点のコンパイル検証であり、新名でのビルド成功が公開面としての可用性を担保している

## アクションプラン

1. 実装の修正は不要。このままアーカイブへ進んでよい
2. 蒸留 (ksn-distill) で concepts の iOS 公開面 2 ファイルを追随させる — 特に `transition-surface.md` の「接頭辞が付かない」注記の削除
3. その後の docs-refresh で `skills/{en,ja}` の iOS / KMP transitions 系レシピ (計 6 ファイル) を再生成する
