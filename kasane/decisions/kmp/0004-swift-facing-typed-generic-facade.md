---
id: 0004
title: Swift 向け KMP 面は型付きジェネリック糖衣で構成し、show も Swift パッケージ側・型不一致は型付きエラーとする
status: accepted
date: 2026-08-17
---

## Context

kmp/ADR-0003 で「KMP 利用者向けの公開登録 API は Swift パッケージ側に置く」ことが決定済みで、API の具体的な形は本 ADR に委ねられた。決定当時の登録入口は機械面 `KsDialogsInteropBridge` のみで、(1) VM が `Any` に型消去され型安全でない (2) 結果型を `KsDialogsInteropResultType` の enum で手動申告する (kmp/ADR-0002 の自己申告制約) (3) KMP iOS Sample が内部用の機械面を直接使っている (add-vertical-slice verify-001 ❌3)。また add-vertical-slice の deviation で「Swift から KMP の show を直接 await する」経路は未実証のまま申し送られ、実測済みの粗 (ObjC 境界でジェネリクス消失・sealed 網羅性喪失) が入力として記録されていた。型不一致時に内部の印が Swift 型付き入口の結果表示に漏れる件も未解消だった。

## Decision

Swift パッケージ内の KMP 向け公開面を次の3点セットで構成する:

1. **登録はジェネリック糖衣**: `register(SharedConfirmViewModel.self, result: Bool.self) { vm, notifier in ... }` の形。結果型の自己申告 (kmp/ADR-0002 の構造的制約) は型引数 `result:` から自動導出し、手動 enum 申告を廃止する — 申告ミスの余地を「実行時まで検出不能」から「コンパイル時に気づける型引数の不一致」に縮小する。notifier は結果型に型付けされる。SwiftUI 用オーバーロード (core/ADR-0011) を含む
2. **Swift からの直接 show は Swift パッケージに型付き入口を置く**: `try await Dialog.shared.kmp.show(vm, result: Bool.self)` の形で `DialogResult<Bool>` を型付きで返す。`result:` を省略した場合は Bool とするオーバーロードを設ける (core/ADR-0012 の既定結果型。登録側も同様)。notifier は iOS Native と同じ `DialogNotifier<R>` に一本化する (専用の型消去ラッパを設けない)
3. **型不一致は型付きエラーを throw**: 登録時の申告型と実際に報告された結果の型が食い違った場合、型付きエラー (`resultTypeMismatch`: 期待型・実際型を保持) を throw し、内部表現を利用者に見せない。公開エラー型の case は宣言した集合に限定し、それ以外の失敗は既存の公開契約型 (`DialogError`) のまま伝播させる — 公開 enum の case 集合は利用者の網羅的 `switch` に影響するため無断で広げない

利用者面は `Dialog.shared.kmp` 配下の KMP 向けサブ面として一本化し、KMP iOS Sample は公開面のみで登録・表示を完結させる。機械面 `KsDialogsInteropBridge` の Swift access level は **public のまま維持する** — `@objc public` は KMP cinterop がリンクするための ABI 面であり、internal 化するとリンクが成立しない。利用者向けでないことは、ドキュメントコメント (「KMP cinterop 委譲専用」) と利用者向け入口の一本化で表現する。

## Alternatives Considered

- **結果型の手動 enum 申告の継続** — 却下。申告ミスを実行時まで検出できない
- **Swift の show を KMP framework の直接 await に委ねる案** — 却下。ObjC 境界でジェネリクスが消え sealed の網羅性も失われることが実測済み (add-vertical-slice)
- **機械面 (KsDialogsInteropBridge) の Swift access level を internal へ下げる案** — 却下。KMP cinterop のリンクが成立しなくなる。「内部化」の実体はアクセスレベルではなく利用者向け導線からの退去である
- **型不一致の内部印を結果表示に漏らす現状の維持** — 却下。利用者に内部表現を晒す

## Consequences

- 正: KMP iOS 消費者の登録・show・結果受け取りがすべて Swift の型システムの中で完結する
- 正: 機械面 (KsDialogsInteropBridge) が利用者向け導線から退き、Sample の消費者境界違反が構造的に解消する (ABI としては public のまま)
- 負: 自己申告の構造自体は残る (kmp/ADR-0002 の既知の負)。糖衣が縮めるのは申告ミスの検出タイミングであり、悪意ある/強引な誤登録までは防げない
- 負: Swift パッケージの公開面が KMP 向けサブ面 (registry + show + エラー型) の分だけ広がり、lockstep バージョン (cross/ADR-0009) の互換管理対象が増える

出典: kasane/roadmaps/library-foundation/phases/phase-5-2-api-surface/history.md (2026-08-17: Swift 向け KMP 面の一括設計) / artifacts/scout-registration-api-and-kssettingsview.md
