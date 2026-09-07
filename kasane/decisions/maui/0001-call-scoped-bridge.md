---
id: 0001
title: MAUI Bridge は Store 非搭載の呼び出しスコープとし、MAUI レジストリは C# 層に持つ
status: accepted
date: 2026-08-14
---

## Context

MAUI 形態は Native 2実装への薄い binding (core/ADR-0001)。先例 KsSettingsView の MAUI Bridge (maui/ADR-0001・0005・0007) は Native 側に Store を内部所有し Host の生成・解放を管理する構造だが、これは「画面に常駐する View の状態同期」のための判断である。KsDialogs のダイアログは「show → 結果1個 → 消滅」の使い捨てモデル (core/ADR-0005) で、常駐状態を持たない。DI 差し込みは VM 型キー → View factory レジストリ (core/ADR-0004)。

## Decision

- Bridge は Store 非搭載の呼び出しスコープとする。常駐 handle を持たず、show 1回ごとに完結する
- Bridge 表面は show / dismiss 程度の操作 1:1 とする (KsSettingsView maui/ADR-0002 の原則を踏襲)
- 結果通知は show ごとの completion 1本とする (同 maui/ADR-0003 の通知集約の縮退形。常駐 delegate は持たない)
- 公開 API は MAUI 慣例型で公開し、interop DTO は非公開の輸送表現とする (同 maui/ADR-0004 踏襲)
- MAUI 側レジストリは C# 層に持つ (VM 型 → MAUI View factory)。show 時に MAUI View を platform view へ実体化して Native lib に渡す。Native lib 側レジストリ (kmp/ADR-0002 の共有レジストリ) とは層が別であり、**利用者の VM 型は Native レジストリに入らない** — 互換面が自分専用の VM 型を1つだけ Native 共有レジストリに登録し、その中身 (MAUI 由来の platform view) を show ごとに MAUI 側から供給する。Native lib の公開 API を MAUI 都合で変えないための構造である

## Alternatives Considered

- **KsSettingsView の Bridge 構造 (内部所有 Store + Host 所有 + releaseHost) をそのまま移植**: 却下。常駐 View の状態同期のための装備であり、使い捨て寿命モデルのダイアログには Store / Host 管理が過剰装備になる。寿命モデルが合わない構造の移植は判断の前提ごと持ち込む誤りで、原則 (操作 1:1・通知集約・慣例型公開) だけを借りる

## Consequences

- 正: Bridge の寿命管理 (生成・解放・再接続) が不要になり、interop 表面が show / dismiss に縮小する
- 正: MAUI レジストリと Native レジストリの責務が層で分離し、MAUI 経由ではキー同一性問題 (ObjC クラス同一性) が発生しない
- 負: MAUI レジストリと Native レジストリの2系統が存在するため、利用者向けドキュメントで「どの形態はどのレジストリに登録するか」の説明が必要になる
- 負: show のたびに MAUI View の実体化 (platform view 変換) が走る (使い捨てモデル採用時に織り込み済みのコスト。core/ADR-0005)
- 負: Native の共有レジストリには互換面専用の VM 型が常に1つ登録された状態になり、レジストリの内容が純 Native 利用者の登録だけで閉じない

出典: kasane/roadmaps/library-foundation/phases/phase-4-vertical-slice/history.md (2026-08-14: MAUI binding 構成) / 同 artifacts/scout-kssettingsview-precedents.md

現行照合: 2026-08-15 確認。maui/macios/native/KsDialogsMauiBridge/MauiDialogBridge.swift と maui/android/native/ksdialogs-maui-bridge/src/main/kotlin/jp/kamusoft/ksdialogs/maui/MauiDialogBridge.kt が、互換面専用の `MauiDialogViewModel` 1型のみを Native 共有レジストリへ登録し、show ごとの completion で結果を返す。判定: 維持
