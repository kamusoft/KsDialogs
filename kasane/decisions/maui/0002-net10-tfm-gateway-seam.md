---
id: 0002
title: MAUI facade は素の net10.0 TFM を持ち、Bridge 呼び出しは internal gateway 越しにする
status: accepted
date: 2026-08-14
---

## Context

縦串実装 (add-vertical-slice) の受け入れ条件は「結果経路 (completed / cancelled の型・値) の自動テスト」を含む。Bridge を運ぶ Binding assembly は platform TFM でしか参照できず、素の `dotnet test` から到達できない。先例 KsSettingsView は同じ問題を maui/ADR-0009 (net10.0 TFM + internal gateway 抽象) で解決し、facade ロジックの高速回帰を実証している。

## Decision

KsSettingsView maui/ADR-0009 の判断型を踏襲する:

- MAUI facade の TargetFrameworks に素の `net10.0` を含める (`net10.0;net10.0-ios;net10.0-android`)
- レジストリ解決・結果型変換などの純ロジックは platform 非依存コードに置き、Bridge 呼び出しは internal な gateway インターフェース越しにのみ行う
- platform TFM だけが Binding 参照と gateway 実装を持つ
- ユニットテストは素の net10.0 で fake gateway (completed / cancelled を返す) を注入して結果経路を検証する

## Alternatives Considered

- **platform TFM のみとし、結果経路テストはシミュレータ/デバイステストで行う**: 却下。テストが遅く実機ビルド必須になり、受け入れ条件の自動テストを高速に回せない。gateway seam の後付けは公開 API 整理後の改修になり高くつくため、初めから入れる

## Consequences

- 正: 結果経路の自動テストがシミュレータなしの素の `dotnet test` で回り、以降のフェーズも同じ戦略に乗れる
- 負: 素の net10.0 はアプリ実行に使われない「テスト用の顔」であり、パッケージング時 (配布フェーズ) に TFM 構成の再検討が必要 (先例と同じ制約)
- 負: platform 固有経路 (提示先の解決・MAUI View の platform view 化と測定・UI スレッドへのマーシャリング) は gateway テストから到達不能であり、そこに欠陥があっても検出は Sample 統合まで遅れる。gateway テストが固定できるのは facade 側の純ロジックまでである

出典: kasane/roadmaps/library-foundation/phases/phase-4-vertical-slice/history.md (2026-08-14: MAUI binding 構成) / 同 artifacts/scout-kssettingsview-precedents.md

現行照合: 2026-08-15 確認。maui/KsDialogs.Maui/KsDialogs.Maui.csproj の TargetFrameworks に素の net10.0 が含まれ、maui/KsDialogs.Maui/Internals/DialogGateway.cs の internal gateway 越しにのみ Bridge を呼ぶ構造 (platform 実装は Platforms/iOS ・ Platforms/Android の PlatformDialogGateway.cs) になっている。判定: 維持
