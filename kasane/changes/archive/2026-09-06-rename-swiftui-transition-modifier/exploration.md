# Exploration: rename-swiftui-transition-modifier

## 課題 / 動機

iOS の SwiftUI 添付面で、modifier の命名が非対称になっている (`ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift:52,60,68`): 静的メタ属性と配置は `.ksDialogOptions(_:)` / `.ksDialogPlacement(_:)` と接頭辞 `ks` を持つのに、演出だけ `.dialogTransition(_:)` で接頭辞が無い。公開 API なので改名は破壊的変更になる。事実としては `kasane/concepts/ios/api/transition-surface.md` に注記済みで、利用者向け Skill (`skills/{en,ja}/ksdialogs-ios/references/transitions.md`、`ksdialogs-kmp/references/transitions.md`) も現行名で案内している。

発見の文脈: split-concepts-platform-surface の tasks 2.7 (`ios/api/transition-surface.md` の新設) で公開面を書き出したときに気づいたもの。同 change は文書の再構成で公開 API を動かさない方針のため見送り。

### 探索で確認した現状 (2026-09-06、コードが正)

非対称は iOS SwiftUI の演出 modifier 1 箇所だけ。他の添付面はすべて `ks` 付きで揃っている。

| 添付面 | 静的属性 | 置き場所 | 演出 |
|---|---|---|---|
| iOS UIKit (extension プロパティ、`ios/Sources/KsDialogs/Contract/UIViewDialogAttributes.swift`) | `ksDialogOptions` | `ksDialogPlacement` | `ksDialogTransition` |
| iOS SwiftUI (modifier、`ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift`) | `.ksDialogOptions` | `.ksDialogPlacement` | **`.dialogTransition`** |
| Android View (拡張プロパティ、`android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/ViewDialogAttributes.kt`) | `ksDialogOptions` | `ksDialogPlacement` | `ksDialogTransition` |
| Android Compose (`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/KsDialogAttributes.kt`) | `KsDialogAttributes(options, placement, transition)` 1 本 | | |

- リポジトリに git remote は無く、ロードマップ package-distribution の public 化 (phase-3-public-readiness) は pending。外部利用者はまだいない
- Sample (`samples/ios`、`samples/kmp/iosApp`) は UIKit 側の `ksDialogTransition` を使っており SwiftUI modifier は未使用

## 検討した選択肢 (却下案と理由を含む)

| 案 | 内容 | 判定 |
|---|---|---|
| A | `.ksDialogTransition(_:)` へ改名し、旧名 `.dialogTransition(_:)` は残さない | **採用** |
| B | 新名を追加し、旧名を `@available(*, deprecated, renamed:)` で併存させる | 却下。猶予の受け手 (利用者) がいない状態で、初回リリースの公開面に非推奨 API を 1 個載せることになる。lockstep 単一バージョンで版間互換を提供しない方針 (cross/ADR-0009、proposed) とも噛み合わない |
| C | 現状維持。concepts の注記で非対称を説明し続ける | 却下。演出だけ綴りが違い、注記を読まないと気づけない公開面のまま一般公開することになる |
| D | 逆に `ks` を外す (`.dialogOptions` / `.dialogPlacement`) | 候補外。非対称は SwiftUI の演出 1 箇所だけで、外す方向は UIKit / Android View / Compose の 3 面をすべて動かす (L 級) |

## 決定事項

- 2026-09-06: **案 A** を採用。一般公開前 (利用者ゼロ) の今、改名だけ行い旧名は残さない
- ADR は起票しない。改名は局所的で覆すコストが低く、SwiftUI 添付 modifier を `ks` 付きで名指しする流儀は core/ADR-0015 (添付による属性供給) が既に持っている。core/ADR-0002 の「命名は原典踏襲」は、原典に SwiftUI modifier が無いため拘束しない
- 起票時に「cross/ADR-0005 の領分」と書いたのは訂正。同 ADR はモジュール名・パッケージ名・配布 ID の写像表で、メンバー名 (modifier 名) は扱っていないため追記不要

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

なし (変更級の確定待ち)。

## 影響範囲 (追随先)

- ライブラリ本体: `ios/Sources/KsDialogs/SwiftUI/DialogAttributeAttachment.swift:68` の modifier 定義。`ios/Sources/KsDialogs/Kmp/KsToastKmp.swift:48` の doc コメント
- テスト: `ios/Tests/KsDialogsTests/DialogTransitionAttachmentTests.swift:43`、`ios/Tests/KsDialogsTests/DialogApiSurfaceCompileChecks.swift:156`
- concepts: `kasane/concepts/ios/api/transition-surface.md` (SwiftUI での添付の表と「接頭辞が付かない」注記の削除)、`kasane/concepts/ios/api/dialog-surface.md:67`
- 利用者向け Skill (docs-refresh で再生成): `skills/{en,ja}/ksdialogs-ios/` の SKILL.md・references/transitions.md・toast.md・loading.md、`skills/{en,ja}/ksdialogs-kmp/references/transitions.md`
- Sample: 追随不要 (SwiftUI modifier 未使用)

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: S (理由)

iOS 単一ビルドルート内の公開識別子の改名で、ADR 改訂なし・可逆・UI なし。公開 API 変更ではあるが未公開で利用者がおらず、影響は本体 1 箇所 + テスト 2 ファイル + 文書追随に収まる。独立レビューは必須。docs-refresh の再生成対象は iOS Skill と KMP Skill の transitions 系レシピ。
