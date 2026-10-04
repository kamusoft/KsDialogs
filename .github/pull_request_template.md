## Changes

- none

## 概要

<!-- 変更の目的と背景。レビュー用であり Release ノートには載らない。 -->

<!--
記入の仕方 (このコメントは表示されず、Release ノートにも載らない。本文から消さなくてよい)

`main` を base とする pull request の `## Changes` は、その版の Release ノートになる。
`## Changes` の節には次の形の行だけを置く (見出し・地の文・別記法の行があると release は publish の前に止まる)。

- `- <種別>: <説明>` — 種別は `breaking` (破壊的変更) / `feature` (機能追加) / `fix` (不具合修正) / `docs` (ドキュメント)
- `- none` — 利用者向けの変更が無いとき。単独で書く (他の項目と併記しない)

記入例:

- breaking: Dialog color properties are now non-nullable
- feature: Toast position can be specified per call
- fix: Loading overlay is no longer left on screen after a fast dismiss

説明は利用者の言葉で、英語で書く (Release ページは閲覧者の言語圏を仮定しない公開物であり、種別の見出しと定型文言も英語で出るため)。内部の作業やハーネスの整備は利用者向けの変更ではないので載せない。

この案内は `## Changes` の後ろの節 (ここ) に置く。`## Changes` の範囲 (見出しの直後から次の見出しまで) に入れると、認識できない行として release が止まる。
-->
