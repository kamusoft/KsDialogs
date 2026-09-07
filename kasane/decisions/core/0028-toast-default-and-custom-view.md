---
id: 0028
title: Toast は既定 View とカスタム View の両対応とし、メッセージだけで出せる入口を設ける
status: accepted
date: 2026-08-27
---

## Context

原典 (AiForms.Maui.Dialogs) で Obsolete 宣言されていた Toast を、コンセプトから再設計して復活させる (phase-8)。その入口として、既定 View (メッセージだけ渡せば出せる標準見た目) を用意するか否かを決める必要がある。

- 原典の Toast は完全にカスタム View 前提で、メッセージ文字列を渡す API は存在しなかった (`IToast` は `Show<TView>()` のみ)。「一言出したい」だけでも利用者が View を自作する必要があった
- 原典の Obsolete 宣言の理由は README・コードのどこにも書かれていない (「廃止予定」の宣言のみ)
- KsDialogs には既に型紙がある: Loading は既定 View あり (ADR-0023 — 共有経路上の内蔵コンテンツ + スタイル値オブジェクトによる一括設定 + `show(message:)` 入口)。Dialog は既定 View なし (中身は必ず利用者供給)
- Android の OS 標準 Toast は元々「テキストを渡すだけ」で出せる。それより手数の多いライブラリ Toast は選ばれにくい

## Decision

Toast は既定 View とカスタム View の両対応とする。

- メッセージ文字列だけで表示できる入口を設け、ライブラリ同梱の既定見た目で表示する (Loading の ADR-0023 と同型の構成)
- カスタム View による表示も併設し、見た目の自由度を確保する
- 原典の「カスタム View 前提・message API なし」という形は継承しない

既定見た目の具体 (デザイン・styling の受け口) とカスタム View の供給経路の詳細は、phase-8 の後続論点で決める。

## Alternatives Considered

- **カスタム View のみ (原典踏襲)** — 却下: 一言出すにも View 自作が必須という原典の明確な使いにくさをそのまま継承する。再設計で復活させる意義が薄く、Android の素の OS Toast より手数が多くなる
- **既定 View のみ (カスタムなし)** — 却下: 実装は最小になるが見た目の自由度がなく、カスタム View を軸とする本ライブラリの提供価値 (Dialog / Loading との一貫性) に反する

## Consequences

- 正: 最頻ユースケース (メッセージを一言出す) が1引数で済む
- 正: Loading と API が対称になり (message 入口 + 既定内蔵コンテンツ + カスタム併設)、ADR-0023 の実装型紙 (Native 内蔵コンテンツ・MAUI / KMP はラッパーが薄いまま) を再利用できる
- 負: 既定見た目が公開 API 表面になり、変更に互換性の配慮が必要になる (ADR-0023 と同じ性質)
- 派生: 既定見た目のデザインと styling の受け口、カスタム View の供給経路が後続の論点になる

出典: kasane/roadmaps/library-foundation/phases/phase-8-toast-rebuild/history.md (2026-08-27 デフォルト View を用意するか否か)
