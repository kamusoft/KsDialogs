---
id: 0005
title: View 再利用機構は契約に持ち込まず、show は毎回生成の使い捨てモデルとする
status: accepted
date: 2026-08-13
---

## Context

移植元には `IReusableDialog` (Create で作り Dispose まで何度でも Show できるハンドル) と、その内部機構 `OnceInitializeAction` (最初の Show 時に1回だけ Handler 生成・Measure・ViewController 構築を行う遅延初期化フック。Loading の iOS/Android でシグネチャが割れている) がある。存在理由は MAUI View の実体化が重いことによる再利用ニーズ。

KsDialogs では core/ADR-0001 により実体が Native View (UIView / Android View / Compose) になり、生成コストの前提が消えた。また原典作者自身の実体験として、Reusable は実利用されず、使い捨てでもパフォーマンス問題は起きなかった。

## Decision

View 再利用機構 (`IReusableDialog` / `Create` 系 API / `OnceInitializeAction` 相当) は core 契約に持ち込まない。show は毎回 View factory (core/ADR-0004 のレジストリ) で View を生成する**使い捨てモデル**に一本化する。

将来、実測でパフォーマンス上の必要が生じた場合は、原典 API の模倣ではなく Native 起点で再設計し、**非破壊の追加機能**として導入する。

## Alternatives Considered

- **原典機構ごと契約化 (ハンドル + 遅延初期化)** — 却下。OnceInitializeAction は MAUI アプリのロードタイミング都合が動機の実装パターンであり、契約に載せると各 platform に不要な制約を輸出する
- **再利用ハンドルのみ契約化 (create → show 複数回 → dispose)** — 却下。原典での実利用実績がなく、動機 (MAUI View の重さ) も Native 化で消滅している。公開ライブラリの契約は永久保守対象であり、「後から足すのは非破壊、後から消すのは破壊的」の非対称性から、今は載せないのが可逆側の選択

## Consequences

- 正: IDisposable 契約・「Dispose 後の Show」等のエラー意味論・KMP でのハンドル型ブリッジ・遅延初期化フックが連鎖的に不要になり、契約と KMP 層が薄くなる
- 正: View のライフサイクルが「生成 → 表示 → 破棄」の単純な一方向になる
- 正: 同一 VM インスタンスでの再 show も独立した重ね出しになる。View も通知役も show ごとに新規のため、2つの show が1つの通知役を奪い合う状態が構造的に存在しない
- 負: 重複表示 (二度押し等) のガードをライブラリが持たないため、抑止が要る場合はアプリ側の責務になる
- 負: 激重カスタム View を高頻度で出し直すケースでは生成コストが乗る (実測で問題化したら非破壊追加で対応)
- 負: 原典の `Create` / `IReusableDialog` 利用者は移行時に書き換えが必要 (cross/ADR-0001 の互換非提供方針の範囲内)

出典: kasane/roadmaps/library-foundation/phases/phase-1-architecture-research/history.md (2026-08-13: View 再利用機構の抽象) / artifacts/scout-origin-api-surface.md (IReusableDialog / OnceInitializeAction の実装詳細)

現行照合: 2026-08-15 確認。android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogPresenter.kt が show ごとに factory で View と DialogNotifier を新規生成しており、再利用ハンドル・遅延初期化フックに相当する型は ios/ ・ android/ のいずれにも存在しない。判定: 維持
