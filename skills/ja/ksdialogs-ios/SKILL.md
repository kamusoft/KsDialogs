---
name: ksdialogs-ios
description: KsDialogs の SwiftUI / UIKit API を使い、iOS の Dialog・Loading・Toast、型付き結果、layout、transition を実装する。
license: MIT
metadata:
  language: ja
  source: https://github.com/kamusoft/KsDialogs
---

# iOS 向け KsDialogs

KsDialogs は、アプリのどこからでも Dialog を呼び出せる UI ライブラリ。中身は自分で書いた View を登録しておくか呼び出し時に渡し、呼び出し側は表示を頼んで結果を待つだけでよい。表示できるのは 3 種類 — 利用者の応答を受け取る Dialog、処理中の操作をブロックする Loading、非対話の通知を出す Toast。この Skill が扱うのは iOS 版で、通常は共有入口 (`Dialog.shared`、`Loading.shared`、`Toast.shared`) から呼ぶ。テストやアプリの DI 構成では、`Dialog()`、`Loading()`、`Toast()` を `KsDialog`、`KsLoading`、`KsToast` 契約として注入できる。

共有入口から呼んでも契約を注入して呼んでも、届く先はプロセス内の同じ状態である。登録した content も Loading / Toast に与えた設定も、自分で作ったインスタンスと共有入口とで同じものを見る。content は SwiftUI の View でも UIKit の `UIView` でも書けるが、登録先が UI 技術ごとに分かれることはなく、どちらも同じレジストリに入る。

## 能力マップ

| やりたいこと | API | レシピ |
|---|---|---|
| Dialog を登録して表示する | `DialogViewModel`、`DialogViewRegistry`、`Dialog.shared.show`、`DialogResult`、`DialogNotifier` | [Dialog](references/dialogs.md) |
| ViewModel から結果を報告する | `notifier`、ViewModel factory、型指定 `show`、`DialogError` | [ViewModel](references/view-models.md) |
| 大きさ・配置・覆い・外側タップを制御する | `DialogOptions`、`DialogPlacement`、`DialogAlignment`、`DialogLayoutArea`、`DialogEdgeInsets` | [レイアウト](references/layout.md) |
| 出現と退出をアニメーションさせる | `DialogTransition`、`DialogTransitionEdge`、`ksDialogTransition` | [トランジション](references/transitions.md) |
| 処理中の操作をブロックする | `Loading.shared`、`LoadingViewRegistry`、`LoadingStyle`、`LoadingProgressReceiver` | [Loading](references/loading.md) |
| fire-and-forget の通知を表示する | `Toast.shared`、`ToastStyle`、`ToastViewRegistry` | [Toast](references/toast.md) |

## セットアップ

Swift package 依存として `https://github.com/kamusoft/KsDialogs-SPM` を https で追加し、公開済みの tag へ `exact` で固定する。tag は version 文字列そのもので、正式版は `X.Y.Z`、prerelease は `X.Y.Z-alpha.N` / `X.Y.Z-beta.N` / `X.Y.Z-rc.N` の形をとる。`from` は prerelease の tag を解決しないため、`exact` で固定する。

package の identity は `KsDialogs-SPM`、リンクする product は `KsDialogs`。Swift 6.3 以降の toolchain でビルドする iOS 17 以降の target にリンクする (ライブラリ自体は Swift 6 言語モードでコンパイルされている)。利用するファイルで `KsDialogs` を import する。

下の宣言の version は現在の公開版で、リリースのたびに更新される。

```swift
dependencies: [
    .package(
        url: "https://github.com/kamusoft/KsDialogs-SPM",
        exact: "0.1.0-beta.1"
    )
]
```

## 最小例

```swift
import SwiftUI
import KsDialogs

struct ContentView: View {
    var body: some View {
        Button("Show toast") {
            Toast.shared.show(message: "Saved")
        }
    }
}
```

## レシピを選ぶ

| やりたいこと | 読むレシピ |
|---|---|
| 登録、型付き結果、インライン content、多段表示、`DialogError` の診断 | [Dialog](references/dialogs.md) |
| `notifier`、ViewModel factory、表示前 configure、Dialog / Loading / Toast の型渡しの違い | [ViewModel](references/view-models.md) |
| 配置、margin、比率サイズ、overlay、外側タップキャンセル | [レイアウト](references/layout.md) |
| preset と非同期 custom hook | [トランジション](references/transitions.md) |
| 命令形・スコープ形の Loading、進捗、style、custom content、型渡しの `show` / `start` | [Loading](references/loading.md) |
| message、登録、インライン、型渡しの Toast 経路 | [Toast](references/toast.md) |
