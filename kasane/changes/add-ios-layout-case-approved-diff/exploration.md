# Exploration: add-ios-layout-case-approved-diff

## 課題 / 動機

レイアウトの共通ケース表 (`core/layout-spec/cases.json`) の OS 差は、該当ケースに置く `approvedDiff` (差し替える期待値 + `reason` + `approvedBy`) でしか表せない (core/ADR-0009、`kasane/concepts/core/architecture/layout-case-table.md`「OS 差の統制」)。この統制を読むのは Android のローダーだけで、iOS のローダーは `approvedDiff` を持たない。

- Android: `android/layout-case-fixtures/kotlin/jp/kamusoft/ksdialogs/support/DialogLayoutCaseLoader.kt:80` 付近で `approvedDiff` を解釈し、`reason` / `approvedBy` を欠くエントリを無効にしている
- iOS: `ios/Tests/KsDialogsTests/Support/DialogLayoutCase.swift` の `DialogLayoutCase` は `approvedDiff` を持たない。型は `Decodable` なので、表にこのフィールドがあっても黙って読み飛ばし、共通の期待値で検証する
- 現在の表には `approvedDiff` が 0 件なので、観測できる影響はまだない。最初の iOS 向け差分が承認された時点で、iOS だけが承認済みの差を無視して赤くなる (または承認外の分岐を検出できない)

発見の文脈: 2026-09-26 の ksn-drift ディープ検証 (layout-case-table.md の照合)。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

**未探索 (簡易起票)**。分かっている疑問点:

- `reason` / `approvedBy` を欠くエントリの扱い (無効として無視するか、テストを失敗させるか) を Android と揃えるだけでよいか
- Android の `approvedDiff` 解釈を固定しているテストがあるか。無ければ両 OS のローダーの自己テスト (表に 1 件の見本を置く・一時表を読む) をどう持つか
- Loading / Toast のケース表テスト (`ToastLayoutCaseTableTests.swift` など C23 を読むもの) も同じローダーを通るか

## UI 素材 (ui/references/ の一覧と注釈)

なし

## 変更級の推奨: 未判定 (暫定 S — iOS のテスト支援コードに閉じるなら)
