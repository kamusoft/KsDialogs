# Exploration: strip-adr-ids-from-ios-public-docs

## 課題 / 動機

`kasane/handbook/cross/comment-policy.md`「公開 doc コメント」は、利用者から見える公開メンバーの doc コメントに ADR ID を含む内部用語を使わないと定めているが、`ios/Sources/KsDialogs/` の公開 doc コメントに ADR ID (`kmp/ADR-0004`・`core/ADR-0012` など) が残っている。

発見の文脈: ksn-autopilot のロードマップ ios-safe-area-and-kmp-int-result の phase-2 (change guard-kmp-swift-int-result-type) の実装で、`ios/Sources/KsDialogs/Kmp/KsDialogsKmp.swift` を触ったワーカーが `comment-policy-lint.py --advisory` の要確認 8 件 (どれも今回足した行ではない) を報告した。`///` の行に ADR ID を含むファイルは `ios/Sources/KsDialogs/` 配下に 80 前後ある (2026-10-20、`grep -rlE '/// .*ADR-[0-9]{4}' ios/Sources`)。ただし internal / private の宣言の doc コメントは規約の対象外なので、公開メンバーに当たるものはこれより少ない。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- 公開メンバーの doc コメントに当たる箇所の数 (lint の advisory はファイル単位で修飾子から推定する)
- 根拠を残す置き場 (規約は非公開の実装側コメントへ移すとしている) と、Android / KMP / MAUI の公開 doc にも同じものがあるか

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: S (暫定。コメントだけの整理で挙動と公開 API を変えない)
