---
kind: rule
applies-when:
  always: false
  paths: ["skills/**"]
  tasks: [docs-refresh の API 名網羅検査の仕分け]
title: 利用者向け Skill の API 掲載基準
description: skills/ の利用者向け Agent Skills に公開 API をどこまで載せるかを決める、簡潔でも網羅の方針と意図的な掲載除外の基準
timestamp: 2026-09-06
---

# 利用者向け Skill の API 掲載基準

この文書は、利用者向け Agent Skills (`skills/{en,ja}/`) に公開 API をどこまで掲載するかを定める。読むと、docs-refresh の API 名網羅検査が報告した未掲載名を、Skill へ追加すべき漏れと意図的な除外へ仕分けられる。公開契約の正本は concepts とコード・テストであり、本規約はそこから Skills へ何を載せるかだけを扱う。

## 方針: 簡潔でも網羅

利用者が使う機能について、公開 Skill は**簡潔でも網羅されている**状態を保つ。公開 API のプロパティ名・機能名は、対応する Skill のどこかで少なくとも 1 回見つからなければならない。詳細な解説やコード例をすべての名前へ付ける必要はないが、名前が一度も現れない機能は利用者のエージェントが存在に気づけないため、掲載漏れとして扱う。

docs-refresh の API 名網羅検査は未掲載名を報告するだけで、掲載・除外を自動決定しない。報告後に本規約の現行除外リストと照合し、リストに無い名前はオーナーが判断する。

## 意図的な掲載除外の基準

| 基準 | 説明 |
|---|---|
| 低頻度の細部 API | 通常の表示・登録・結果受け取りには使わない、レジストリの全消去や診断用の補助操作など。機能説明だけで利用できる場合は個々のメソッド名を載せない |
| 内部層・interop 層 | 利用者が直接触らない Native / KMP / MAUI 間の接続面。`KsDialogsInteropBridge` / `KsDialogsInteropResultType`、Handler、binding assembly などが該当する |
| 機械的に導出できる名前 | 1 つの公開規則から一意に導ける宣言群。たとえば「各 bindable property は対応する `FooProperty` を持つ」と説明できる場合は個別列挙を要しない |
| 可視性引き下げ候補 | 名前や配置が内部用で、利用者向けの入口から到達せず、実装・テストでも内部用途しか確認できない public 宣言。Skill への掲載ではなく internal 化の変更候補として扱う |
| 対象 Skill 外・機械検査由来 | 共有 concept にある別 platform・別 Skill の公開名を API 名網羅検査が拾ったが、対象 Skill の公開面には存在しない場合。concepts を契約 / 公開面へ分割した後は他 platform の公開名では起きないため、この基準に当たる報告は concepts の配置違反を疑う |
| 非 API token | そもそも API 名ではないのに識別子として拾われた語。platform / framework の標準型・標準定数、concept が書く見本型名、移植元ライブラリの旧 API 名、ライブラリ側 build 配線の DSL 名・ファイル名が該当する |

これらは仕分けの例であり、それ自体を現行除外リストへの登録とは扱わない。具体的な API 名を新たに除外するときは、実装上の利用経路と該当基準を示してオーナー判断を得る。

## 現行の除外リスト

この表は、concepts を「core = platform 非依存の契約 / `<platform>/api/` = 公開面」へ分割したあとの
API 名網羅検査の報告を、オーナーが仕分けた確定済みの除外である。表に無い未掲載名は引き続き自動で除外せず、
オーナー判断へ送る。

| platform | 除外 API | 基準 | 実装上の経路・理由 |
|---|---|---|---|
| iOS | `LoadingCoordinator` | 内部層・interop 層 | 表示合流を実装する internal coordinator で、利用者は Loading の facade / contract から間接利用する |
| iOS | `AnyObject`、`TimeInterval`、`UIHostingController`、`UITimingCurveProvider` | 機械的に導出できる名前 | Apple framework / Swift 標準型で、掲載済み公開署名・レシピから一意に分かる |
| Android | `LoadingCoordinator` | 内部層・interop 層 | 表示合流を実装する internal coordinator で、利用者は Loading の facade / contract から間接利用する |
| Android | `AbstractComposeView`、`LazyColumn` | 機械的に導出できる名前 | Android / Compose framework の標準型で、掲載済み公開署名・レシピから一意に分かる |
| Android | `AccelerateDecelerateInterpolator`、`LayoutParams` | 非 API token | Android framework の標準型で、公開面 concept が既定の補間器・添付時の親指定を説明するために書いているだけの名前 |
| MAUI | `IMauiInitializeService` | 内部層・interop 層 | `KsDialogsInitializer` が service provider を捕捉する MAUI 起動配線で、利用者は登録 extension から間接利用する |
| MAUI | `IServiceProvider.GetService` | 機械的に導出できる名前 | ViewModel fallback が使う .NET DI 操作で、`UseViewFallback` の provider 引数から導出できる |
| MAUI | `TimeSpan.MaxValue` | 機械的に導出できる名前 | .NET 標準型・標準定数で、掲載済み duration / transition 署名から一意に分かる |
| MAUI | `SetIocConfig`、`ShowResultAsync`、`UseCurrentPageLocation` | 非 API token | 移植元 AiForms.Maui.Dialogs の旧 API 名で、`maui/api/` の「移植元との対応」節にだけ現れる。MAUI 公開面は `AddKsDialogs` / 型付き `ShowAsync` / `Dialog.SetLayoutArea` を使う |
| KMP | `LoadingCoordinator` | 内部層・interop 層 | 表示合流を実装する internal coordinator で、利用者は Loading の facade / contract から間接利用する |
| KMP | `localSwiftPackage` | 内部層・interop 層 | KMP artifact 発行時の SwiftPM metadata 配線用 Gradle DSL で、通常の消費者 API ではない |
| KMP | `AbstractComposeView` | 機械的に導出できる名前 | Android / Compose framework の標準型で、掲載済み公開署名・レシピから一意に分かる |
| KMP | `UIView` | 機械的に導出できる名前 | Apple framework の標準型で、掲載済み公開署名・レシピから一意に分かる |
| KMP | `ConfirmContent`、`SharedConfirmViewModel` | 機械的に導出できる名前 | concept 内で消費者アプリが定義する例示型名であり、登録レシピから任意の利用者型として導出できる |
| KMP | `LayoutParams`、`suspendCancellableCoroutine` | 非 API token | Android framework / kotlinx.coroutines の標準名で、ホスト側の添付とブリッジ実装を説明するために公開面 concept が書いているだけの名前 |
| AiForms migration | `ActivatorUtilities.CreateInstance` | 低頻度の細部 API | 規約 fallback で任意 View を生成する補助経路で、通常の移行は明示登録を使う |
| AiForms migration | `IMauiInitializeService` | 内部層・interop 層 | MAUI startup で service provider を捕捉する内部配線で、移行者は登録 extension から間接利用する |
| AiForms migration | `IServiceCollection`、`IServiceProvider`、`IServiceProvider.GetService` | 機械的に導出できる名前 | .NET DI の基盤型と標準操作で、`AddKsDialogs` / `UseViewFallback` の署名から導出できる |
| AiForms migration | `Easing`、`TimeSpan`、`TimeSpan.MaxValue` | 機械的に導出できる名前 | .NET / MAUI の標準型・標準定数で、掲載済み duration / transition 署名から一意に分かる |
| AiForms migration | `CancellationToken`、`Easing.CubicInOut` | 非 API token | .NET / MAUI framework の標準型・標準定数で、MAUI 公開面 concept が署名と preset の既定を書くために使っているだけの名前 |

リストの更新は、API 名網羅検査の報告に対してオーナーが掲載または除外を決めたときに行う。除外リストを更新しても検査結果から名前が消えるわけではなく、次回から判断済みとして仕分けられるようになる。

## コード例のコメント

`skills/` 配下のコード例は原則としてコメントを書かない。やむを得ない最小限のコメントは英語で統一し、英語版と日本語版の対応するコードブロックを byte 一致させる。

## してはいけないこと

- 現行除外リストに無い未掲載 API を、エージェントが「低頻度」「内部向け」と独断で除外しない。新しい除外はオーナー判断後に表へ追加する。
- 除外 API を concepts から消さない。concepts は公開契約の正本であり、この規約は派生物である Skills の掲載範囲だけを絞る。
- API 名を列挙するだけで機能の入口や利用場面を説明したことにしない。少なくとも Skill 本文の機能一覧表か、`references/` の利用レシピから到達できる状態にする。

## 関連

- docs-refresh (`.agents/skills/docs-refresh/SKILL.md`) — API 名網羅検査と追従更新の手順
- [cross/ADR-0011](../../decisions/cross/0011-user-docs-as-agent-skills.md) — 利用者向けドキュメントを Agent Skills として提供する決定
