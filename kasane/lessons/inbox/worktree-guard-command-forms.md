---
scope: process
kind: pain
severity: normal
count: 1
first-seen: 2026-10-20
last-seen: 2026-10-20
evidence:
  - deliver-swiftui-content-safe-area-ios (ksn-autopilot ios-safe-area-and-kmp-int-result の観測。worktree で動くフェーズ担当のコマンドのうち、`cd ... && xcodebuild ... > file` の一部の形と `git -C` が worktree の隔離のガードで拒否された。`cd ios && xcodebuild ... > <絶対パス>` は通った。同じ実行の事前点検でも、変数を展開するコマンドが拒否されることを観測している)
---

## ルール文

worktree で動く担当がビルド・テストを走らせて出力をファイルへ残すときは、`cd <ビルドルート> && <コマンド> > <絶対パス>` の形で書き、`git -C` と変数の展開を使わない。拒否されたら同じ形へ書き直して 1 回だけ再試行し、それでも拒否されたら報告に拒否されたコマンドの形を書く。守れたかは、担当の報告に同じ拒否が 2 回以上並んでいないことから判定する。

## 経緯

- 2026-10-20 deliver-swiftui-content-safe-area-ios: 拒否されたコマンドの形を試し直しながら、通る形を見つけた
