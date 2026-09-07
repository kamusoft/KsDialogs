---
id: 0005
title: Swift 向け show の呼び出し元キャンセルは、機械面の show ハンドル経由で当該ダイアログだけを閉鎖する
status: accepted
date: 2026-08-17
---

## Context

core の結果通知契約 (concepts/core/api/result-notification-semantics.md) は、呼び出し元のキャンセル時にダイアログを閉じて結果を cancelled で確定することを要求する。一方、KMP iOS の機械面 (`KsDialogsInteropBridge`) には show 単位の取り消し操作がなく、Kotlin 側 gateway は「キャンセルされても表示が残る」実装だった。Swift 向け型付き show (kmp/ADR-0004) を completion の async 化だけで作ると、この契約違反が Swift 公開面にも引き継がれ、純 Swift 経路 (キャンセルで閉じる) と挙動が割れる。

## Decision

- 機械面の show に **show 単位のハンドル (キャンセル操作)** を追加する
- Swift 向け show は呼び出し元 Task のキャンセル時、ハンドル経由で**当該ダイアログだけ**を閉じ、結果を cancelled で**ちょうど1回**確定する。提示開始前にキャンセルが来た場合も取りこぼさない (ハンドル到着時点で取り消す)

## Alternatives Considered

- **キャンセル時も表示を残す (現行 Kotlin gateway の挙動を追認する案)** — 却下。core の結果通知契約と矛盾し、純 Swift 経路と挙動が割れる

## Consequences

- 正: KMP Swift 面が core の結果通知契約に適合し、純 Swift 経路と挙動が揃う
- 正: show 単位ハンドルは Kotlin 側 suspend 経路 (`IosDialogGateway`) の契約追随にも流用できる (コルーチンのキャンセルから引く後続候補)
- 負: 機械面の ABI にハンドル型が1つ増える
- 負: Kotlin 側 suspend 経路は未追随のまま残り、契約不適合が Kotlin 経路に限定して継続する (後続変更での解消を要する)

出典: kasane/changes/archive/2026-08-19-expand-api-surface/design.md (Decision 9) / concepts/core/api/result-notification-semantics.md
現行照合: 2026-08-22 確認。`kmp/ksdialogs-kmp/src/iosMain/kotlin/jp/kamusoft/ksdialogs/kmp/IosDialogGateway.kt` — `suspendCancellableCoroutine` の `invokeOnCancellation` から機械面の show ハンドルを引き、Kotlin 側 suspend 経路も当該ダイアログだけを閉鎖して cancelled を 1 回確定する (テスト `IosDialogGatewayCancellationTests` / `KsDialogsKmpCancellationTests`)。判定: 維持 — Consequences の負「Kotlin 側 suspend 経路は未追随のまま残る」は add-presentation-behavior で解消済み (Decision の「Swift 向け」の範囲はそのまま。後続変更での解消という帰結どおりに推移した)
