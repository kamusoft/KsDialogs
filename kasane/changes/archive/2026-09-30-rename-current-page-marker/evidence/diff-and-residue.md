# 差分の形と旧名の残存 (tasks 1.5 / 2.5 / 3.3 / 4.1)

## 差分は旧名から新名への置き換えだけ (1.5 / 2.5 / 3.3)

ios/・android/・samples/ で変わった 23 ファイル (変更 22 + Android の印を定義するファイルの改名 1) について、改名前 (HEAD) の内容の `ksDialogCurrentPage` を `markAsDialogCurrentPage` に置き換えたものが改名後の内容と完全に一致し、行数も同じであることを機械的に確かめた (23 ファイルすべて一致)。したがって削除だけの行・追加だけの行は無い。

| 範囲 | ファイル数 | 変わった行 |
|---|---|---|
| ios/ (Sources 5・Tests 7) | 12 | 17 行 (Sources 6・Tests 11)、すべて置き換え |
| android/ (main 3・androidTest 2・api-surface-check 1・改名 1) | 7 | 置き換え 14 行 + 改名したファイル内の置き換え 3 行 |
| samples/ (4 ルート) | 4 | 10 行、すべて印の呼び出し (Swift 4・Kotlin 4) と Kotlin の import (2) |

- Android の印を定義するファイル: `compose/KsDialogCurrentPage.kt` → `compose/DialogCurrentPageModifier.kt`。中身の差分は関数名・KDoc の参照 `[markAsDialogCurrentPage]`・`InspectorInfo` の `name = "markAsDialogCurrentPage"` の 3 行だけ
- 台帳の診断メッセージ (英文のまま、名指しだけ新しい名前):
  - iOS `DialogCurrentPageLedger.swift`: `origin` = `"a view marked with markAsDialogCurrentPage()"`、`.notFound(reason: "No view marked with markAsDialogCurrentPage() is placed in the window presenting the dialog.")`
  - Android `DialogCurrentPageLedger.kt`: `"No view marked with markAsDialogCurrentPage() is placed in a window of the activity presenting the dialog."`
- MAUI の Sample (samples/maui) は変えていない

## 旧名が現行のソースに残らない

`git grep -n "ksDialogCurrentPage"` (大文字小文字を区別) の結果:

| 範囲 | 件数 |
|---|---|
| ios/ (Sources と Tests) | 0 |
| android/ (main・test・androidTest・api-surface-check) | 0 |
| samples/ | 0 |

## リポジトリ全体の残存 (4.1 — 蒸留への申し送り)

リポジトリ全体の検索で残ったのは次の箇所だけで、どれも経緯の記録か、蒸留で直す長命層である (この変更では書き換えていない)。

| 区分 | ファイル | 件数 |
|---|---|---|
| 経緯の記録 (進行中の本 change) | `kasane/changes/rename-current-page-marker/` の exploration.md・proposal.md・specs 3 本・tasks.md | 16 |
| 経緯の記録 (アーカイブ) | `kasane/changes/archive/2026-09-27-add-page-layout-area/` の evidence/distill-handoff.md・exploration.md・proposal.md・review-001.md・second-opinion-spec-001.md・specs/samples/spec.md・tasks.md・ui/brief.md・verify-001.md | 15 |
| 経緯の記録 (ADR) | `kasane/decisions/core/0045-ks-prefix-limited-to-view-attachments.md` | 1 |
| 蒸留で直す concepts | `kasane/concepts/ios/api/layout-surface.md` | 4 |
| 蒸留で直す concepts | `kasane/concepts/android/api/layout-surface.md` | 3 |
| 蒸留で直す handbook | `kasane/handbook/cross/sample-parity.md` | 1 |

`kasane/concepts/log.md` には旧名の出現は無い。上表以外 (skills/・README 群・kmp/・maui/・verification/ を含む) の出現は 0 件。
