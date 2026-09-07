# Proposal: add-monorepo-scaffold

## Why

phase-1 で確定したアーキテクチャ (Native 2実装 + MAUI / KMP 薄いラッパー、core/ADR-0001) を実装に移すには、4形態のビルドルートが疎通した器が必要。器づくりと API 設計を混ぜない方針のもと、空の scaffold を先に固める。

## What Changes

- ビルドルート4本の新設 (cross/ADR-0004): ios/ (Package.swift) / android/ (`:ksdialogs`) / kmp/ (`:ksdialogs-kmp`、includeBuild で android へ実依存) / maui/ (KsDialogs.slnx + `KsDialogs.Maui`)
- 公開識別子は写像表 (cross/ADR-0005) を適用する
- 各ルートにスモークテスト1本 (Swift Testing / JUnit 5 / kotlin.test / NUnit 4)
- 初期整備: README (英語主・最小限) / .gitignore (ルート1本) / LICENSE (MIT)
- 対象能力: repo-scaffold (新規1能力)

## Non-Goals

- API 設計・実装コード (phase-4 以降)
- Sample の器 (phase-4 で実物と同時に確定)
- CI・カバレッジ
- パッケージング・配布基盤 (ロードマップ非ゴール)
- KMP iOS→Swift の cinterop (Swift 実物が要るため phase-4)
- .editorconfig・lint 等の規約系 (実コードが生まれるフェーズで導入)

## Impact

- 破壊的変更なし (実装ゼロからの新設)。既存ソースには触れない。kasane/ への変更は、cross/ADR-0002 が自ら本フェーズに委ねた swift-tools-version の追記 (task 2.3) のみ
- リスク: AGP 込み composite build の癖 (cross/ADR-0004 で引き受け済み) が最小疎通で顕在化する可能性 — まさにそれを検証するのが本 change

## 級: M

ADR 化済み決定の機械的実体化で、公開 API 変更なし・単一能力のため

domain: cross
roadmap: library-foundation/phase-2-monorepo-scaffold
