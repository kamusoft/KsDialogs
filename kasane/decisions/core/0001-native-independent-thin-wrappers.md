---
id: 0001
title: プラットフォーム構成は Native 2実装を土台とし MAUI / KMP は薄いラッパーとする
status: accepted
date: 2026-08-13
---

## Context

KsDialogs は AiForms.Maui.Dialogs を Native (Swift / Kotlin) + MAUI + KMP の3形態で使えるダイアログライブラリとしてリビルドする。最初から3形態を掲げるため、「後から KMP を載せたら core 契約が合わなかった」が最大の失敗リスクであり、core 契約をどこに置くかが全アーキテクチャの前提になる。

KMP 形態の利用イメージは「KsAppKMP のような共有層の Presenter / ViewModel からダイアログを呼べること」であり、主役は commonMain の呼び出しインターフェースと、共有 ViewModel と Native View の紐付け機構。View の中身は Native View を直接利用する (CMP なら Compose)。MAUI 形態が持つ「MAUI View の実体化」に相当する重い層は KMP には不要。

## Decision

Swift / Kotlin の2つの Native 独立実装を土台とし、MAUI はそれを binding で、KMP は commonMain の呼び出し契約 + 薄い expect/actual ファサードで包む。4形態すべてが2つの Native 実装に収束する対称形とする。

core 契約は KMP commonMain 等の共有コードではなく、仕様 (concepts + 共通仕様テスト) として共有し、実装間の乖離は縦串スライス (最小機能の全形態貫通 — add-vertical-slice) で早期検出する。

- commonMain にはダイアログ呼び出しの契約のみを置き、UI 実装は一切持たない
- KMP の actual (android / ios) は各 Native 実装へ委譲するだけの薄い層とする
- ダイアログの中身 (カスタム View) は Native View を直接渡す。CMP の Compose コンテンツ対応は「Native View の代わりに Composable を中身にする」拡張として位置づける

## Alternatives Considered

- **A. 共有 core 方式 (KMP commonMain に core 契約・共通ロジックを置き、全形態の正とする)** — 却下。(1) ライブラリの実体は各 OS の提示機構に乗る UI であり、commonMain に置いて嬉しい共有ロジックがほぼ無い (共有できるのは契約の形とレイアウト計算式程度)。(2) MAUI 形態は C# のため commonMain を消費できず、共有 core にしても「全形態の正」になれない。(3) 純粋な iOS Native 利用者に Kotlin ランタイム同梱の framework を背負わせることになり、ダイアログライブラリとして重すぎる。(4) Kotlin→Swift export の癖 (default 引数・sealed 等) が Native API の質を下げ、リブランド方針「Native 主」と緊張関係になる

## Consequences

- 正: 各言語のイディオムで最良の Native API を設計できる (Native 主のリブランド方針と整合)
- 正: iOS Native 利用者は純 Swift パッケージとして利用でき、Kotlin ランタイム依存を負わない
- 正: MAUI / KMP が同じ「Native を包む」対称形になり、形態追加の構図が単純になる
- 正: KMP 層が薄く済む (呼び出し契約 + 委譲のみ)
- 負: 契約の一貫性が型として強制されず、仕様文書 + 縦串疎通 + 共通仕様テストでの担保になる (原典の iOS/Android 同型コード重複と同じ構図)
- 負: レイアウト計算アルゴリズムが Swift / Kotlin で重複実装になる (共通仕様化による緩和は別論点で扱う)
- 負: KMP iOS 側は Kotlin から Swift 実装を呼ぶため、Swift API に ObjC 互換面 (または Swift Export 対応) の設計制約が生じる

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: KMP の位置づけ)
