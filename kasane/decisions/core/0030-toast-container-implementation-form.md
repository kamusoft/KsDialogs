---
id: 0030
title: Toast の器は Loading の器の非モーダル派生とし、1 Toast 1器・重なりは追加順・Loading が常に前面とする
status: accepted
date: 2026-08-27
---

## Context

Toast の基本要件は「常に Window 最上位 (ページをまたぐ)・非モーダル (表示中もページを通常どおり操作可能)・多重起動可 (起動順に重なるだけ)・配置と演出は Dialog / Loading 踏襲」(phase-8 決定事項)。これを成立させる器の実装形を決める必要がある。

- Dialog の器は OS 提示スタックに参加する (iOS = present 連鎖の先端に .overFullScreen / Android = 1枚ごとの全画面透過 Window)
- Loading の器は提示スタックに参加しない (iOS = key window 直貼り / Android = 専用の全画面透過 Window — ADR-0026)。レイアウト (DialogLayoutApplier 系) と演出 (DialogTransitionRunner 系) は Dialog と共有部品化済み
- ただし Loading の器はモーダル (背後のタッチを止める) 前提であり、Toast はここが真逆

## Decision

Toast の器は Loading の器 (ADR-0026 の形) を土台にした、タッチを奪わない非モーダル派生とする。

- 提示スタックに参加しない器 (iOS = key window 直貼り / Android = 全画面透過 Window) を使い、レイアウト・演出は Dialog / Loading と同じ共有部品を通す
- 非モーダル化は器の一点変更で実現する: iOS は器 View をヒットテスト透過にし、Android は Window に非フォーカス + 器面のタッチ素通しを設定する
- Toast 1つにつき器を1つ生成し、多重起動の重なり順は Window / subview の追加順 (= 起動順) とする。自前のスタック管理は持たない
- **Loading と Toast が同時に表示される場合は、追加順によらず常に Loading を前面とする**。Loading はモーダルで操作を止める覆いであり、その上に通知が乗ると「操作できない状態の提示」という覆いの意味が崩れるため、単純な追加順ではなく明示的な順序規則とする
- Dialog と Toast の前後関係は ADR-0006 (多段表示は OS 委譲・前後関係は契約で保証しない) の線を維持する

## Alternatives Considered

- **OS ネイティブ Toast への委譲 (Android)** — 却下: カスタム View は API 30 で非推奨 (原典 Toast の死因の状況証拠)、duration が実質 3.5 秒にクランプされる、配置が SetGravity の制約を受け Dialog 踏襲にならない、多重は OS キューで直列化され「起動順に重なる」を満たせない。iOS はどのみち自作になる
- **単一共有器に複数 Toast を子として積む** — 却下: 多重管理を自前で持つことになり、共有部品 (レイアウト・演出) が「器 = 1 コンテンツ」前提のため改造が要る。1 Toast 1器なら重なり順が追加順で自然に成立する

## Consequences

- 正: 「配置・演出は Dialog 踏襲」の要件が共有部品の再利用でそのまま満たせ、検証様式 (ADR-0009・0016) も揃う
- 正: 多重起動の重なり順に自前の状態管理が不要
- 負: Toast の枚数分だけ器 (iOS: subview / Android: Window) が生成される (通常の利用枚数では実害は小さい見込み)
- 負: 「Loading が常に前面」の順序規則は追加順だけでは成立しないため、器の載せ替え・挿入位置の制御が実装に必要になる
- 派生: Android の Activity 再生成 (回転等) では Loading と同様、器を使い捨てて状態から再取り付けする方式 (ADR-0026・0027 の型) が Toast にも要るかが実装論点になる
- 実装で確定した帰結 (出典: 実装結果):
  - 多重管理はプロセス内 `ToastCoordinator` (ADR-0027 の型の複数表示版) が表示リストと各表示の duration タイマーを一元管理する。Android の Activity 再生成では全表示を起動順のまま新しい resumed Activity へ演出なしで再取り付けし、計時は coordinator 側にあるため残り duration は器の作り直しに影響されない
  - 「Loading が常に前面」の実現形: iOS は Toast の器を Loading の器の下へ挿入 (`insertSubview(_:belowSubview:)`)、Android は Toast の Window を出した直後に Loading へ演出なし再前面化 (既存の再取り付け機構の流用) を依頼する。依頼の向きは Toast → Loading の一方向で、Loading は Toast の存在を知らない。Loading の入りの演出中に依頼が来た場合は演出の完了を待ってから載せ替える (演出途中の載せ替えは中身が透明のまま固まるため)

出典: kasane/roadmaps/library-foundation/phases/phase-8-toast-rebuild/history.md (2026-08-27 器の実装形) / kasane/changes/archive/2026-08-28-add-toast/design.md (Decision 4・5) / kasane/changes/archive/2026-08-28-add-toast/review-001.md (Major 1 — 演出中の再前面化)
