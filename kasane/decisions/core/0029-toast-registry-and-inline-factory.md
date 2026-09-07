---
id: 0029
title: カスタム Toast は型指定レジストリとインライン factory の両対応とし、レジストリは共有層からの呼び出し経路を担う
status: accepted
date: 2026-08-27
---

## Context

Toast は既定 View とカスタム View の両対応とすると決めた (ADR-0028)。その上で、カスタム View の供給経路をどうするかを決める必要がある。

- Dialog / Loading の供給経路は2系統ある: 型指定レジストリ (Dialog: VM 型キー → View factory、ADR-0004 / Loading: 専用レジストリ、ADR-0025) と、登録不要のインライン factory (ADR-0013)
- レジストリの本質的な役割は再利用の便利機能ではなく、**共有層 (KMP の commonMain・MAUI の VM 層) が UI 型に触れずに表示を呼ぶための間接層**である。共有コードは型キーで show を呼ぶだけで、View factory の登録はプラットフォーム側が行う (契約は commonMain・レジストリ実体は Native 委譲 — kmp/ADR-0002)
- Toast は結果を返さない一発通知であり、Dialog のレジストリが担う VM 解決・結果型・notifier 紐付け (ADR-0018・0021) や Loading の進捗受け口転送 (ADR-0025) に相当する仕掛けは不要

## Decision

カスタム Toast の供給経路は、型指定レジストリとインライン factory の両対応とする (Dialog / Loading と同型)。

- **型指定レジストリ**: 共有層からの呼び出し経路。型キー → View factory をプラットフォーム側で登録し、共有コードは UI 型に触れずにカスタム Toast を表示できる。Toast は結果を返さないため、notifier / 結果型まわりを落とした軽い形とする
- **インライン factory**: UI 層の中で完結する単発表示向けの近道 (ADR-0013 の位置づけそのまま)

## Alternatives Considered

- **インライン factory のみ (レジストリなし)** — 却下: カスタム Toast を出したい呼び出し点が必ず View を組み立てることになり、KMP の commonMain からはそもそも呼べず (プラットフォーム View 型を参照できない)、MAUI でも VM / サービス層に UI 構築コードが漏れる。既定 View の message 入口は共有層から呼べるため、カスタム Toast だけが共有層から呼べない非対称が生じる

## Consequences

- 正: 共有層 (commonMain・VM 層) が UI 型に触れずにカスタム Toast を表示でき、Dialog / Loading と層の境界の引き方が揃う
- 正: UI 層内の単発表示はインライン factory で登録なしに書ける
- 負: レジストリ + 登録 API を4形態に設ける分、API 表面と実装が増える
- 派生: 登録 API には技術別オーバーロード (従来 View 系 + Compose / SwiftUI — ADR-0010・0011) が Toast にも及ぶ

出典: kasane/roadmaps/library-foundation/phases/phase-8-toast-rebuild/history.md (2026-08-27 カスタム View の供給経路)
