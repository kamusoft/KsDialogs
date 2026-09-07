---
id: 0009
title: 全形態は lockstep 単一バージョンで一斉リリースし、版間互換を提供しない
status: proposed
date: 2026-08-17
---

## Context

4形態の配布物は単一モノレポから生まれ (cross/ADR-0004)、KMP は static リンクで Swift 実体・Android 実体へ委譲し純 Native 利用者と同一レジストリを共有する (kmp/ADR-0002)。この構造では KMP と Native 実体の版がズレると実行時の契約不整合に直結する。

配布モデル (cross/ADR-0008) でモノレポの semver tag が SwiftPM バージョンを兼ねることが決まり、論点C の PoC で「KMP 発行 metadata の `exact("x.y.z")` が消費者の Package.resolved まで機械的に届いて固定される」ことを実測済み。バージョン整合をポリシー (お願いベース) でなく仕組みで強制できる土台がある。

## Decision

- 全 artifact (SwiftPM tag / Maven `jp.kamusoft:ksdialogs`・`jp.kamusoft:ksdialogs-compose`・`jp.kamusoft:ksdialogs-kmp` / MAUI NuGet) を**同一バージョン x.y.z で一斉リリース**し、モノレポの git tag と一致させる
- KMP→Swift パッケージは `swiftPackage(url(...), exact(x.y.z))`、KMP→Android Native は同版の厳密指定 Maven 依存で、版ズレを機械的に排除する
- 版が異なる組み合わせは非サポートとし、互換表 (バージョンマトリクス) は作らない。消費者への案内は「全部同じ番号を入れる」の1行で済ませる
- deployment target (iOS 17, cross/ADR-0002) の配布物での担保は「Swift パッケージの `platforms: .iOS(.v17)` 宣言 + KMP metadata の `iosMinimumDeploymentTarget=17.0`」を正とする (両方が消費者まで伝搬することを PoC で確認済み)。`-Xoverride-konan-properties` は klib (IR) には効かず、リポジトリ内で自らリンクする binary (テスト・Sample) 用の内部ビルド詳細であり、配布物の担保手段には数えない

## Alternatives Considered

- **形態ごとの独立バージョン + 互換 range**: 却下。static リンク + レジストリ共有の前提で版ズレ事故の余地を残し、互換マトリクスの維持コストが常時かかる。形態単位でリリースできる柔軟性は tag 1本のモノレポでは活きない
- **KMP↔Swift のみ exact、他は range の折衷**: 却下。ルールが2種類混在して複雑さだけ増える

## Consequences

- 正: 消費者の理解が「同じ番号を揃える」だけになり、互換表・組み合わせテストが不要になる
- 正: `exact` + 厳密指定により、版ズレはビルド時に機械的に検出される (実行時事故にならない)
- 正: モノレポ tag 1本がそのまま全形態のリリース単位になり、cross/ADR-0008 と整合する
- 負: 1形態だけの修正でも全形態のバージョンが上がる (変更のない形態も再発行する)。リリース CI は常に全形態一斉が前提になる (発行実装・運用整備への入力)
- 負: 「変更がないのに版が上がる」ことへの説明をリリースノート運用で受け持つ

出典: kasane/roadmaps/library-foundation/phases/phase-10-packaging-model/history.md (2026-08-17: 論点B)
