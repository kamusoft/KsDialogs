---
id: 0025
title: カスタム Loading は専用レジストリで登録し、進捗は VM の進捗受け口 interface へ転送する
status: accepted
date: 2026-08-25
---

## Context

カスタム View 版の Loading を、どの登録機構で登録しどう表示するかを決める必要がある。

- Dialog には VM 型キー → View factory の明示レジストリ (ADR-0004) と、従来 View 系 / 宣言的 UI 系の技術別オーバーロード登録 (ADR-0011) が確立している。ただし Dialog の factory 署名は結果報告チャネル (resultChannel) を受け取る前提で、結果を返さない Loading のライフサイクルには合わない
- Loading のスコープ形 (処理ブロックを渡す形) には進捗報告口が渡る。既定ローディングは進捗をメッセージ表示に反映するが、カスタム View がその進捗をどう受け取るかも決める必要がある
- VM を状態の運び手とする方針は Dialog 側で確立済み (ADR-0018 系)

## Decision

Dialog のレジストリとは別に、**Loading 専用レジストリ** (VM 型キー → View factory、使い捨て生成 — ADR-0005 踏襲) を持つ。登録形は Dialog と同型の技術別オーバーロード (ADR-0011 踏襲) だが、factory 署名に resultChannel は無い。表示は VM を渡す show / スコープ形とインライン factory 版を持つ。

進捗のカスタム View への配送は、**VM が進捗受け口 interface (`LoadingProgressReceiver` — `onProgress(Double)` 1メソッド) を実装している場合にライブラリが転送する** (未実装なら転送なし)。宣言的 UI では VM の観測がそのまま表示更新になる。

## Alternatives Considered

- **Dialog レジストリに相乗り** — 却下: factory 署名 (resultChannel) が合わず、同じ VM 型を Dialog と Loading の両方に登録したいケースで衝突する
- **factory 引数で進捗ソースを渡す** — 却下: 従来 View 系で購読・解除の管理が利用者コードに漏れる。VM 経由なら受けたい者だけが interface を実装すればよい
- **カスタム View への進捗転送なし (利用者が action 内で自分の VM を直接更新)** — 却下: 既定とカスタムで進捗報告の意味が変わり、契約が2枚舌になる

## Consequences

- 正: 「Loading として登録した VM を Loading として出す」対応が型で閉じ、Dialog 用と Loading 用の登録が衝突しない
- 正: 進捗を受けたい VM だけが interface を実装すればよく、購読管理が利用者に漏れない
- 負: レジストリと登録 API の面が2系統 (Dialog / Loading) になり、公開面が増える
- 負: 進捗受け口は「実装していなければ何も起きない」ため、実装忘れは静かな未配送になる (契約は Scenario テストで固定し、利用例はドキュメント側で示す)

出典: kasane/changes/archive/2026-08-26-add-loading/design.md (Decision 3)
