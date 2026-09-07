---
id: 0002
title: 公開 API は契約 interface + 既定 singleton エントリの両対応とし、命名は原典踏襲とする
status: accepted
date: 2026-08-13
---

## Context

移植元 AiForms.Maui.Dialogs の公開 API は `Dialog.Instance` / `Loading.Instance` / `Toast.Instance` の静的エントリだが、実体は `Lazy<IDialog>` 等でインターフェースを返しており、契約と入口は既に分離されている。KsDialogs は「ダイアログをいつでも呼び出せる」手軽さを製品のウリとしつつ、KMP 形態では共有層の Presenter / ViewModel からの呼び出しが主役であり、Presenter の単体テストで fake に差し替えられるテスタビリティが求められる。

## Decision

契約と入口の分離を3形態に一般化し、両対応とする:

- **契約**: 各形態のイディオムで interface / protocol として定義する (`IDialog` 相当)
- **既定エントリ**: 原典踏襲の singleton を提供する (MAUI: `Dialog.Instance` / Swift: `Dialog.shared` / Kotlin: `Dialog.instance` / KMP: commonMain の既定インスタンス)。既定エントリは DI へ登録できる実体を指す形 (class + static / companion のプロパティ) とし、インスタンスを構築できない純粋な static 専用型 (Kotlin の `object` 等) は採らない — 同じ契約実装を DI 側と共有できなくなるため
- **DI 利用**: 同じ契約 interface を DI コンテナ (Koin / MAUI DI 等) に登録して注入利用できる
- **命名ポリシー** (KsSettingsView maui/ADR-0008 の翻案): 対応概念がある公開面は原典の命名・使い心地を踏襲する。原典命名が非対称な箇所は対称性を優先して改める。原典契約に無い機能は互換 API として提供せず、必要なら Native 側から再設計する

既定 singleton と DI 登録インスタンスの同一性の担保は、DI 差し込み方式の決定 (別 ADR) で具体化する。

## Alternatives Considered

- **A. 静的 Instance のみ (原典方式)** — 却下。static 直呼びはテストで fake に差し替えられず、KMP 共有層 Presenter の単体テスト要件を満たさない
- **B. DI サービスのみ** — 却下。セットアップ必須になり「いつでも呼び出せる」手軽さを失う。Swift ネイティブで DI 前提の API は非イディオム

## Consequences

- 正: 手軽さ (singleton) とテスタビリティ (interface 注入) を両立する
- 正: 各形態がそれぞれのイディオムで自然な入口を持てる
- 正: 原典利用者に馴染む命名で学習・移行コストが下がる
- 負: 既定 singleton と DI 登録インスタンスの同一性管理という設計課題を負う (DI 差し込み方式の ADR で解決必須)
- 負: 入口が2系統になるため、利用ドキュメントで両方の使い分けを説明するコストが増える
- 負: 既定エントリは DI 注入可能な実体を指す形に制約され、各言語で最も簡潔な static 表現 (Kotlin の `object` 等) は選べない

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: 公開 API 形状) / artifacts/scout-origin-api-surface.md (原典 API 表面) / KsSettingsView maui/ADR-0008 (命名ポリシーの翻案元)

現行照合: 2026-08-15 確認。ios/Sources/KsDialogs/Presentation/Dialog.swift (`Dialog.shared`) と android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/Dialog.kt (`Dialog.instance`) が、契約 (`KsDialogs`) を実装しつつ DI へ注入可能な実体を既定エントリとして公開し、同じ `DialogViewRegistry.shared` を共有する。判定: 維持

現行照合: 2026-09-06 追記。契約の型名は縦串実装以来 `KsDialogs` (MAUI `IKsDialogs`) だったが、Loading / Toast の `KsLoading` / `KsToast` との非対称を解消するため core/ADR-0034 (proposed) で `KsDialog` / `IKsDialog` へ改名する決定を起票 (change: rename-dialog-contract-singular)。同 change で改名を完了し、4 形態のコード・テスト・公開 API 形状検査 (旧名の負の検査を 4 形態に追加) と concepts / skills / docs-refresh の禁止トークン lint まで追随済み。判定: 維持 (命名ポリシーはそのまま、適用結果の改名)
