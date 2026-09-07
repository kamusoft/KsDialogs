---
id: 0014
title: ダイアログ契約は器にしか実現できないメタ属性のみとし、View で表現可能な属性は契約から外す
status: accepted
date: 2026-08-18
---

## Context

core/ADR-0008 は原典 AiForms.Maui.Dialogs のコア属性 (Proportional サイズ・明示サイズ・LayoutAlignment・Offset・CornerRadius・Border・DialogMargin・OverlayColor) を「原典踏襲で全て残す」とした (起票時)。その後、初版設計「属性は ViewModel 契約のオプションメンバとする」が実装フェーズでオーナー差し戻しになった再設計議論の中で、オーナーから「『レイアウト属性』という括りに性質の異なる要素が混在している」との指摘があり、属性を性質 (レイアウト / スタイル / UI 属性 / 機能)・メタ性 (素の Native / MAUI の View で表現可能か)・静的/動的 (呼び出し時に可変したいニーズの有無) の3軸で分類し直した。

## Decision

契約に残す属性の判定原則を「**器 (ダイアログコンテナ) にしか実現・計算できないメタ要素だけを契約に持つ。View 自身が表現できるものは View の責務とし契約から外す**」と定める。この原則による分類と取捨:

| 区分 | 属性 | 静的/動的 | 契約 |
|---|---|---|---|
| レイアウト (動的メタ) | horizontalAlignment / verticalAlignment / offsetX / offsetY | 動的 — 呼び出し時可変ニーズあり | 残す |
| レイアウト (静的メタ・領域定義) | layoutArea / dialogMargin | 静的 | 残す |
| サイズメタ (静的) | proportionalWidth / proportionalHeight | 静的 — 「画面の何割」は器しか知らない | 残す |
| スタイルメタ (静的) | overlayColor | 静的 — scrim は器の持ち物 | 残す |
| 機能メタ (静的) | isCanceledOnTouchOutside | 静的 — 外側タップの入力処理は器 | 残す (ADR-0008 で別論点送りだったが要件漏れと判定し contract へ編入) |
| UI 属性 (View で表現可能) | width / height (明示サイズ) / cornerRadius / borderWidth / borderColor | — | **廃止** |

- dialogMargin はレイアウトのメタ要素と位置づける。実体は「見た目の余白」ではなく**有効領域の定義** (基準領域 R − margin = 有効領域 A) であり、クランプ・Fill・Start/End anchor がすべて A 基準で決まる計算入力。R (画面) を知る器にしか計算できない
- 明示サイズの廃止により、サイズ決定の優先順位 (ADR-0007 系規則の「比率 > Fill > 明示 > 内容」) は「比率 > Fill > 内容」に簡素化される。View 自身のサイズ宣言は「内容サイズ」経路に吸収される
- 静的/動的の区分は属性の供給点 (登録時 / 呼び出し時) の設計判断の入力として使う (供給点の決定は本 ADR の範囲外)

ADR-0008 との関係: 「コア属性を全て残す」の部分を本 ADR が改訂する。意図的乖離のうち Border 内側描画統一 (0008 Decision 3) は対象属性の廃止により無効。その他の乖離 (Offset 座標系統一・DialogMargin の位置決め適用・LayoutArea 再設計・OverlayColor 正式対応・初期サイズ確定) は存続する。

## Alternatives Considered

- **原典コア属性の全維持 (ADR-0008 現行)** — 却下。cornerRadius / border / 明示サイズは素の Native / MAUI の View が自力で表現でき、契約に持つと責務が重複する。View の見た目の決定権は View 側にあるべき
- **dialogMargin を View 側の native margin で表現する案** — 却下。器の配置計算 (クランプ・Fill・anchor) の入力であり、View 技術ごとの margin 意味論に委ねると規則の一貫性が壊れる

## Consequences

- 正: 契約が「器のメタ情報」だけに痩せ、全属性が単一原則で説明できる。VM / View / 契約の責務境界が明確になる
- 正: 凍結済みケース表と規則文から明示サイズ・描画系の分岐が消え、仕様がシンプルになる
- 負: 原典ユーザーの cornerRadius / border / 明示サイズ指定は移行時に View 側実装へ書き換えが必要になる (移行ガイドでの明記が必要)
- 負: 初版設計で実装済みだった明示サイズ・描画属性のコード・テスト・ケース表該当分は改訂・撤去の手戻りが発生する

出典: kasane/changes/add-layout-spec/exploration-redesign.md (初版設計差し戻し後の再設計議論、2026-08-18 オーナー分類指示) / kasane/changes/add-layout-spec/design.md (Decision 5・6)
