---
id: 0006
title: Sample は集約 samples/ に置き、利用者と同じ側から公開 product を参照する
status: accepted
date: 2026-08-14
---

## Context

モノレポは4形態分離のビルドルート (cross/ADR-0004)。最初の実物 (add-vertical-slice の最小 Sample) 以降、Sample は常に並走し、機能変更の完了条件に「パリティ準拠の Sample 通し」が含まれる。Sample の置き場所と公開 product への参照方式が、consumer 境界 (利用者アプリと同じ側から参照しているか) の成立を左右する。先例 KsSettingsView は集約 samples/ 方式で、参照方式の落とし穴まで実測済み。ただし KMP 形態の Sample は先例に実体がない。

## Decision

- Sample はトップレベル `samples/` に集約する: `samples/ios` / `samples/android` / `samples/maui` / `samples/kmp` (各 README 付き)
- Sample は配布物ではなく「利用者アプリと同じ側から公開 product を組み合わせる実行可能な reference」とする。参照方式は形態別に:
  - iOS: `ios/` を Local Swift Package として参照する
  - Android: `android/` を Gradle composite build (`includeBuild`) で参照し、GAV → included project の `dependencySubstitution` を Sample 側に明示する (AGP は Maven publication を生成せず自動置換が発火しないため)
  - MAUI: facade への ProjectReference 1本とし、Binding 層は推移参照。TFM は platform のみ (テスト用の素の net10.0 は含めない)
  - KMP: `samples/kmp/` を shared モジュール + androidApp + iosApp の3点構成とする。shared の Presenter が KMP facade (公開 product) を消費し、iosApp は Swift 側から VM → View の登録を行う
- Sample を配布物・挙動契約の SSoT・自動テストの代替として扱わない。local source reference の成功を公開 repository からの配布成立と説明しない

## Alternatives Considered

- **各ビルドルート内に Sample を置く**: 却下。本体ビルドと混ざり consumer 境界が曖昧になる。参照方式の先例実証もない
- **Sample を別リポジトリに分離**: 却下。参照に配布基盤 (パッケージ registry) が必要になり、ロードマップ非ゴール「パッケージング・配布基盤」と正面衝突する。パリティ検証もリポジトリ跨ぎになり運用が重い

## Consequences

- 正: 「利用者と同じ側」が物理配置でも見え、参照方式は先例の実測済み構成に乗れる
- 正: 4形態の Sample が横並びになり、パリティ検証 (sample-parity 規約) の比較がしやすい
- 正: KMP Sample の iosApp が Swift 側登録を持つため、レジストリキー同一性の疎通確認 (立ち上げ期の申し送り) を Sample 自体が踏む
- 負: KMP Sample (shared + 2アプリ) は先例のない構成であり、参照方式 (KMP facade の framework 参照・composite build) の妥当性は本リポジトリ自身で担保するほかない
- 負: Sample が増えるたびに4形態分の維持コストがかかる (パリティ規約の追跡義務と合わせて織り込み)
- 負: KMP iOS の消費者には型付きの公開登録経路が存在せず、iosApp は内部の互換面 (`KsDialogsInteropBridge`) を直接使う暫定経路になる — この形態だけ「公開 product のみを参照する」consumer 境界が成立しない

出典: kasane/roadmaps/library-foundation/phases/phase-4-vertical-slice/history.md (2026-08-14: Sample 構成の確定) / 同 artifacts/scout-kssettingsview-precedents.md

現行照合: 2026-08-15 確認。samples/ios ・ samples/android ・ samples/maui ・ samples/kmp の4ルートが Decision の参照方式 (Local Swift Package / composite build + dependencySubstitution / facade への ProjectReference 1本 / composite build) で公開 product を参照している。samples/README.md に参照方式の一覧がある。判定: 維持

現行照合: 2026-09-05 確認。本 change で各 Sample README を廃止したため、Decision の「各 README 付き」と過去の現行照合にある一覧の所在は履歴となった。4ルートの参照方式とビルド・実行手順は kasane/handbook/cross/local-development-setup.md「Sample のビルドと実行」で照合でき、Sample の配置と consumer 境界は変わらない。判定: 維持
