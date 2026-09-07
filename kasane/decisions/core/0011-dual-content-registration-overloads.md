---
id: 0011
title: 両対応の登録 API は技術別オーバーロードを公開面とし、内部は単一の型消去表現に収束する
status: accepted
date: 2026-08-17
---

## Context

core/ADR-0010 がコンテンツ View 技術の両対応 (Compose / SwiftUI 必須 + Android.View / UIView 必須) を宣言し、具体設計を本 ADR に委ねた。決定当時の登録 API は4形態とも factory 戻り値が従来 View 系1本 (iOS `UIView` / Android `android.view.View` / MAUI `Maui.View`) で、宣言 UI の受け口が無かった。参考指定の KsSettingsView は両対応済みだが、その形は用途別の使い分け — 装飾領域は sealed 型消去ラッパ `KsAnyView` (値として差分検出パイプラインを通すため)、行は宣言 UI 専用 builder + 従来 View は公式 interop 丸投げ — であり、「1つの登録 API が2技術を受ける」先例ではない (調査記録 scout-registration-api-and-kssettingsview.md、出典参照)。

## Decision

公開面は**技術別オーバーロード**とし、内部で単一の型消去表現に収束する:

- **iOS**: 現行 `register { vm, notifier -> UIView }` に加え、SwiftUI 用 `register { vm, notifier -> some View }` を追加。内部で `UIHostingController` に包んで提示する
- **Android**: 現行 `register { vm, notifier -> View }` に加え、Compose 用 (`@Composable` ラムダを取る形) を追加。内部で `ComposeView` に包む。公開名は登録・インライン show とも**一貫して別名** (`registerCompose` / `showCompose`) とする — Kotlin では `@Composable` 付き関数型と通常関数型の同名オーバーロードが呼び出し側の型推論で衝突しやすく、登録だけ別名にしてインラインを同名にする非対称は混乱のもとになるため。Compose 系 API の配布単位は本体と分離する (android/ADR-0001)
- **MAUI**: 変更なし (`Maui.View` 一本)。従来 View 系経路の存在が MAUI 連携の前提 — ADR-0010 の「Android.View / UIView も必須」の実装上の意味
- **KMP**: Android 側は Native API がそのまま使えるため自動的に両対応。iOS 側は Swift 向け KMP 面 (kmp/ADR-0004) の登録 API に SwiftUI 受け口を含める — 本決定がその入力

ダイアログでは View がモデル値としてパイプラインを旅しない (登録時に技術が確定し、show 時に提示器が直接消費する) ため、KsSettingsView が sealed ラッパを必要とした動機が存在しない。show 毎回生成の使い捨てモデル (core/ADR-0005) により、ホスティングのライフサイクルは「閉じたら破棄」で済み、View 再利用に伴う複雑さも生じない。

## Alternatives Considered

- **sealed 公開ラッパ (KsAnyView 型) を factory 戻り値にする案** — 却下。毎回 `.swiftUI { }` / `.Compose { }` で包む ceremony を利用者に課す一方、その対価である「値としての運搬」の動機がダイアログに無い。「View 直接渡しの show 系統」の議論で値運搬が必要になったら再考する
- **従来 View のみ + 公式 interop (ComposeView / UIHostingController) 丸投げ案** — 却下。宣言 UI が一級市民にならず、ADR-0010 の必須要件と矛盾する
- **Android の Compose 系を iOS と同じ同名オーバーロードにする案** — 却下。呼び出し側でラムダに `@Composable` を明示しないと解決が曖昧になるケースがあり、Swift (クロージャ戻り値型・`some View` による解決が安定) と言語事情が異なる

## Consequences

- 正: 利用者は各技術の自然な型をそのまま返せる (ceremony ゼロ)。学ぶべき公開ラッパ型が無い
- 正: MAUI / 既存の従来 View 利用者には非破壊 (既存シグネチャは不変)
- 負: ホスティング機構 (UIHostingController / ComposeView) を内部に2系統×2OS で新設する実装コスト
- 負: Kotlin の言語都合により Android だけ Compose 系が別名 (`registerCompose` / `showCompose`) となり、iOS (同名オーバーロード) と公開面の対称性が崩れる

出典: kasane/roadmaps/library-foundation/phases/phase-5-2-api-surface/history.md (2026-08-17: コンテンツ View 技術両対応の登録 API の見せ方) / artifacts/scout-registration-api-and-kssettingsview.md
