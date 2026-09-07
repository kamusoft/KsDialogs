# Sample 通しの証跡 — 新デモ3種 × 完了/キャンセル

tasks 7.2 の「パリティ準拠の Sample 通し」の記録 (2026-08-19)。

## 範囲

グループ6の実装時に4ルート (ios / android / kmp / maui) すべての3デモ × 完了/キャンセルを
通し済みのため、ここは**代表ルートの再通し**として ios と kmp (iosApp) の2ルートを撮り直した。
kmp を代表に選んだのは、本変更で登録コードを Swift 向け型付き公開 API へ差し替えた
ルート (tasks 6.2) であり、実行時の経路が最も変わっているため。

実行環境: iOS Simulator iPhone 17 Pro (booted)。
撮影は `xcrun simctl io <UDID> screenshot`、操作は device point 座標のタップ。

## 結果

| デモ | ルート | 完了 (OK) | キャンセル |
|---|---|---|---|
| Declarative Dialog | ios | `結果: completed(true)` | `結果: cancelled` |
| Declarative Dialog | kmp (iosApp) | `結果: completed(true)` | `結果: cancelled` |
| Text Input Dialog | ios | `結果: completed("Hello")` | `結果: cancelled` |
| Text Input Dialog | kmp (iosApp) | `結果: completed("Hello")` | `結果: cancelled` |
| Inline Dialog | ios | `結果: completed(true)` | `結果: cancelled` |
| Inline Dialog | kmp (iosApp) | `結果: completed(true)` | `結果: cancelled` |

いずれも ui/brief.md の文言表どおりの本文・ボタン文言・結果表記だった
(`こんにちは、KsDialogs!` / `メッセージを入力してください` + プレースホルダ `ここに入力` /
`インライン表示です`、ボタンは `キャンセル` / `OK`)。

入力文字列は Simulator の日本語 IME 経由になるため ASCII の `Hello` を使った
(文言表は初期入力値のみを定めており、利用者が入力する値は定めていない)。

キャンセル経路は直前の結果を completed にしてから操作し、
`completed(...)` → `cancelled` への遷移が起きたことまで撮っている。

## ファイル

`<ルート>-<デモ>-<状態>.png`。状態は `dialog` (表示中) / `completed` / `cancelled`。
ルートは `ios` と `kmp-ios`。
