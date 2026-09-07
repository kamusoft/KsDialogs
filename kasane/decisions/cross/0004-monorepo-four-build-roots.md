---
id: 0004
title: モノレポは4形態分離のビルドルートとし、KMP→Android Native は composite build で接続する
status: accepted
date: 2026-08-14
---

## Context

KsDialogs は Native (Swift / Kotlin) + MAUI + KMP の4形態を単一リポジトリで開発する。core/ADR-0001 により KMP の androidMain actual は Android Native 実装へ委譲するだけの薄い層であり、KMP モジュールは Android Native ライブラリへの依存を必ず持つ。両者はともに Gradle ビルドのため、この依存をどう張るかがリポジトリ構成を決める。

翻案元の KsSettingsView cross/0001 (accepted) は「platform 別独立ビルドルート・リポジトリルートに共通ビルドファイルなし」を決定しつつ、却下案の中で「KMP 風統合ビルドは KMP 導入時に再検討しうる」としており、最初から KMP を掲げる本プロジェクトがその再検討タイミングにあたる。

## Decision

ios/ (Package.swift) / android/ (settings.gradle.kts) / kmp/ (settings.gradle.kts) / maui/ (.slnx) の4つを独立ビルドルートとし、リポジトリルートには共通ビルドファイルを置かない (KsSettingsView cross/0001 の翻案)。

- KMP→Android Native の依存は composite build (includeBuild) で張る
- Kotlin / AGP のバージョン整合は、2つの Gradle ビルド間でバージョンカタログをファイル共有して緩和する

## Alternatives Considered

- **B. Gradle ビルドルートを1つに兼ね、その中に Android Native と KMP の2モジュールを置く案** — 却下。依存が `project(":...")` で単純になり、バージョンカタログ・Maven 公開設定も1箇所に集約できる利点はあるが、Gradle ルートが「android + kmp」の混成になり命名の工夫が要る。4形態との対称性と IDE ワーキングセットの独立を優先してオーナー判断で分離を採った
- **KMP モジュール自体を Android Native の配布物と兼ねる案** — 検討外。core/ADR-0001 の「Native 実装と actual (委譲層) を分ける」構図と衝突する

## Consequences

- 正: ios / android / kmp / maui の4ディレクトリが4形態と1対1対応し、構成が直感的になる
- 正: 各 IDE は自分のビルドルートを開くだけで独立して作業でき、Android 専業時に KMP が視界に入らない
- 正: CI がビルドルート単位で自然に分割できる
- 負: AGP を含む composite build 特有の癖を背負う
- 負: Kotlin / AGP のバージョンを2つの Gradle ビルドで揃え続ける必要がある (バージョンカタログ共有で緩和)
- 負: Maven 公開設定が android/ と kmp/ の2箇所に分かれる

出典: kasane/roadmaps/library-foundation/phases/phase-2-monorepo-scaffold/history.md (2026-08-14: ビルドルート構成)
