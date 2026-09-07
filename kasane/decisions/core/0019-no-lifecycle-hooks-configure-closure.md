---
id: 0019
title: VM 契約にライフサイクルフックを持ち込まず、型指定呼び出しの初期化は configure クロージャで行う
status: accepted
date: 2026-08-24
---

## Context

移植元の `IDialogViewModel<T>` は初期化・破棄の2フックを持つが、実物確認の結果、どちらも汎用のライフサイクル契約ではなかった: `DialogInitializeAsync(T parameter)` はライブラリが DI コンテナから VM を解決する型指定呼び出し (ShowFromModel 系) 専用のパラメータ配達で、呼び出し側が VM インスタンスを渡す経路では呼ばれない。`Destroy()` は View 再利用機構 (ReusableDialog) の dispose 時後始末である。

KsDialogs では両フックの存在理由が既存決定で消えている: 主経路は呼び出し側が VM を生成して show に渡す形 (パラメータはコンストラクタで渡せる)、View 再利用機構は契約に持ち込まず show は使い捨てモデル (core/ADR-0005)、結果は async 単発の show 戻り値で受ける (core/ADR-0003) ため後始末は await 後の呼び出し側で書ける (キャンセル時も finally / defer で拾える)。

ただし型指定呼び出し (ライブラリが VM を解決する経路) は移植対象の原典機能であり提供する。この経路では呼び出し側が VM インスタンスを握らないため、コンストラクタによるパラメータ渡しが使えず、届け方の設計が必要になる。なお原典方式には「`vm is IDialogViewModel<TParameter>` の実行時判定により、パラメータ型を間違えると初期化がサイレントにスキップされる (コンパイルは通る)」という粗がある。

## Decision

**VM 契約にライフサイクルフックを持ち込まない。型指定呼び出しの初期化は configure クロージャで行う。**

- VM 契約はマーカーのままとし、初期化・破棄のメンバー・opt-in interface を追加しない
- 初期化は「呼び出し側 new のコンストラクタ」または「型指定呼び出しの configure クロージャ」で行う。書き味は `ShowAsync<TVm>(vm => ...)` 相当 (各言語で async クロージャ可)。configure は VM 型に対してコンパイル時に型付けされ、原典のサイレントスキップの粗は構造的に消える
- configure はパラメータ渡しに限らない汎用セットアップ地点 (表示前の非同期データロード・VM 参照の捕獲・コールバック配線) として位置づける
- configure の完了は View 生成・提示より前であることを仕様として保証する
- 後始末は show の await 後に呼び出し側が行う。破棄フックが将来必要になった場合は opt-in interface として非破壊追加できる
- VM の解決元 (DI コンテナ連携のどこに載せるか) と各形態のシグネチャは別決定とする

## Alternatives Considered

- **原典踏襲の init フック (型指定呼び出し + `DialogInitializeAsync` 相当)** — 却下。フック interface が VM 契約に入り契約の重さが増すうえ、パラメータ型不一致の粗が残る (実行時判定を直しても実行時エラー止まりで、コンパイル時型安全にはならない)
- **破棄フックのみ opt-in 提供 (実装した VM だけ撤去後に呼ぶ)** — 却下。async 単発 + 使い捨てモデルにより await 後の呼び出し側後始末で代替でき、需要が未実証。結果配送・撤去 (core/ADR-0017 のラッチ・撤去後配送) との順序保証を仕様化する義務も生じる。必要になれば非破壊追加できる
- **型指定呼び出しを提供しない (DI 解決は呼び出し側の責務)** — 却下。移植対象の原典機能が欠け、機能移植完了の基準 (原典が機能・仕様の正) に反する

## Consequences

- 正: VM 契約がマーカーの純度を保ち、利用者の VM は空定義のまま任意の基底クラスを継げる (core/ADR-0018 と整合)
- 正: パラメータ渡しがコンパイル時型安全になり、原典のサイレントスキップの粗が消える
- 正: configure が汎用セットアップ地点になり、契約フック1本より表現力が高い
- 負: 原典の `ShowFromModelAsync<TViewModel, TParameter>(parameter)` と形が変わるため、原典利用者向けの移行説明が必要 (命名・使い心地踏襲ポリシー core/ADR-0002 からの意図的乖離)
- 負: configure の実行タイミング (View 生成・提示より前) を仕様・テストで規定する必要がある
- 負: 破棄はライブラリが関与しないため、呼び出し側が await の戻りを取りこぼす書き方 (fire-and-forget) では後始末の置き場がない — 利用ドキュメントでの注意喚起が要る

出典: kasane/roadmaps/library-foundation/phases/phase-6-model-binding-di/history.md (2026-08-24: ライフサイクルフックと型呼び出しのパラメータ渡し) / AiForms.Maui.Dialogs リポジトリ: IDialogViewModel.cs・Dialog/Dialog.cs (ShowFromModelAsync)・Dialog/ReusableDialog.iOS.cs (Destroy 呼び出し)
