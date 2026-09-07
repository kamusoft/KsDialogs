---
id: 0022
title: Loading は器を OS 提示スタックから分離し、コンテンツ載せ・レイアウト・演出の部品を Dialog と共有する
status: accepted
date: 2026-08-25
---

## Context

Loading 機能 (既定ローディングとカスタム View 版) を実装するにあたり、既存の Dialog 機構とどこまで実装・契約を共有するかを決める必要がある。

- 移植元 (AiForms.Maui.Dialogs) では Loading は Dialog 機構とほぼ独立: iOS は UIViewController を使わず Window (または現在ページの View) への直貼り、Android は Dialog とは別系統の DialogFragment。共有は View ホスティングとレイアウト補助関数程度
- 一方 KsDialogs の core には、Dialog 向けにコンテンツホスティング (従来 View 系 / 宣言的 UI 系の両対応 — ADR-0010/0011)・レイアウト規則 (配置 + オフセット — ADR-0007 系)・演出フックの添付 (ADR-0017) の仕様が既に確立している
- Loading のライフサイクルは「開ける → 進捗を流す → 呼び出し側が閉じる」で結果を返さず、Dialog の「show が結果をちょうど1回返す」(ADR-0003)・使い捨て (ADR-0005) とは根本的に異なる
- Loading はダイアログ表示中に出し入れされる (ダイアログより手前に出る・出したまま下のダイアログが閉じる) ため、重なり順の管理から独立している必要がある

## Decision

Loading の器 (画面への載せ方) は Loading 専用のオーバーレイとし、Dialog 機構の提示経路 (iOS の present 連鎖 / Android の Dialog 器の積み重ね) と多段表示の意味論には参加させない。この決定の本質は「Dialog 機構との絡みを断つ」ことであり、OS のウィンドウ機構の利用自体を禁じるものではない — Android では既存の Dialog 器 (別ウィンドウ) より手前に出すため、Loading 専用の全画面透過ウィンドウを用いる (移植元と同方式。2026-08-25 提案レビューでの明確化)。一方、コンテンツの載せ方 (従来 View 系 / 宣言的 UI 系のホスティング)・レイアウト規則 (配置 + オフセット)・演出の添付は Dialog の core 仕様を再利用する。

器メタ属性の適用範囲: レイアウト系の器メタ属性 (layoutArea・dialogMargin・比率サイズ・overlayColor・placement) は、Dialog と同じ意味・同じ供給経路 (コンテンツ添付 + 表示 API の引数は placement のみ — ADR-0015 踏襲) で Loading にも適用する。既定ローディング (内蔵コンテンツ) は利用者の添付面を持たないため、配置・オフセットは表示 API の placement 引数で指定する。isCanceledOnTouchOutside は Loading では常に無効 (添付されても効かない) とする — Loading はユーザー操作では閉じない。

演出の適用範囲: カスタム View 版の Loading は Dialog と同じ演出の添付スロット (DialogTransition) で差し替えられ、両フック (ホスト View を受け取る統一形・完了通知つき) と既定クロスフェード・覆いの別レイヤ常時フェード (ADR-0017) がそのまま効く。「結果のラッチと配送」は結果を持たない Loading には対象外。既定ローディング (内蔵コンテンツ) は利用者の添付面を持たないため、ライブラリ既定のクロスフェード固定とする (移植元もカスタムのみ差し替え可・既定は固定フェードで、同じ構造)。

## Alternatives Considered

- **原典踏襲の完全独立機構** — Loading を Dialog と切り離した軽量オーバーレイとして専用実装する。却下: レイアウト・演出・コンテンツホスティングを Native 2実装 (iOS / Android) で二重実装することになり、利用者から見た属性・演出の書き方も Loading 専用の流儀に分裂する
- **Dialog 機構への統合** — Loading を「閉じられない・結果を返さない特殊ダイアログ」として Dialog と同じ提示経路に載せる。却下: OS の提示スタックに入ると多段表示の意味論 (ADR-0006) に巻き込まれ、ダイアログの出入りと Loading の出入りが干渉する。また結果通知契約 (ADR-0003) と使い捨てモデル (ADR-0005) に Loading の結果なしライフサイクルが適合しない

## Consequences

- 正: Loading の表示・非表示がダイアログの重なり順管理から独立し、挙動が予測可能になる
- 正: レイアウト・演出・コンテンツホスティングの仕様と実装を Dialog と共有でき、Native 2実装の重複と利用者の学習コストが減る
- 負: 器が Dialog 用・Loading 用の2系統になり、各 Native 実装に Loading 専用の器コード (オーバーレイの生成・破棄) が必要になる
- 負: 共有する部品の仕様 (器メタ属性・演出) それぞれについて「Loading にはどこまで効くか」の適用範囲を個別に定義・文書化する必要が生じる

出典: kasane/roadmaps/library-foundation/phases/phase-7-loading/history.md (2026-08-25 カスタム View 版と Dialog 機構の共有範囲)
