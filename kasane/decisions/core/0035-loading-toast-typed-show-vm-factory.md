---
id: 0035
title: Loading / Toast のレジストリに VM factory スロットを追加し、型指定 show を Dialog と同型で提供する
status: accepted
date: 2026-09-06
---

## Context

型指定 show (ViewModel の型だけを渡し、ライブラリが VM factory で生成して configure を適用してから表示する経路 — core/ADR-0019〜0021) は Dialog にだけあり、iOS Native / Android Native / MAUI の 3 形態で提供されている。Loading / Toast の呼び出し面はインスタンス渡しとインライン factory 版のみで、専用レジストリ (core/ADR-0025・0029) は View factory スロットしか持たない (2026-09-06 コード確認)。

利用者 (オーナー) から「Loading / Toast も Dialog と同様に VM の実体を渡さない型指定のオーバーロードで呼びたい」という要望が出た。core/ADR-0021 は VM 解決を「全形態で同型の、登録が正のモデル」と定めているが、その適用範囲は Dialog に限られていた。

## Decision

- Loading / Toast の専用レジストリに **VM factory スロット**を追加し、Dialog レジストリと同型の 2 スロット構成 (View factory + VM factory、再登録はスロット単位の後勝ち、show 時はスナップショット解決) とする
- iOS Native / Android Native / MAUI の Loading / Toast に**型指定 show** を追加する。動詞は show 1 本のまま、経路の違いは引数の形で表す (core/ADR-0020)。置き場所 (Loading / Toast) と duration (Toast) は型指定 show でも引数で渡せる
- 未登録の型指定 show は構成ミスとして失敗する。暗黙の既定コンストラクタ生成は採らない (core/ADR-0021 と同じ)
- 順序保証は Dialog と同じ: VM factory で生成 → configure 完了 → (Loading は進捗受け口の紐付け) → View factory → 提示。VM factory / configure の例外は提示に進まず呼び出し元へ伝播する。**Toast だけは例外**: show が同期・任意スレッド呼び出しで VM factory / configure は UI スレッドで実行されるため呼び出し元へ返せず、Toast の失敗モデルの「受理後の失敗」(警告を残してその 1 枚だけ破棄 — core/ADR-0033) に分類する。VM factory 未登録の同期失敗は Toast でも呼び出し時点で起きる
- configure の同期性は機能の性質に合わせる: Loading は非同期 configure 可、Toast は同期 configure のみ (show が fire-and-forget の同期呼び出し — core/ADR-0031)

## Alternatives Considered

- **Toast の型指定 show だけ非同期 (await できる show) にして例外を伝播させる** — 却下。fire-and-forget の Toast で型指定経路だけ戻り値待ちになる非対称を作る
- **Toast の VM factory / configure を呼び出しスレッドで実行して同期に伝播させる** — 却下。Dialog / Loading の UI スレッド保証と食い違う
- **型だけで生成する (既定コンストラクタ規約 + configure)** — 却下。DI 登録を忘れた依存未注入の VM が黙って生まれる。Swift はリフレクション不足で同型に表現できず形態間で割れる (core/ADR-0021 が却下した理由と同じ)。Dialog だけ構成ミスを失敗させ Loading / Toast は黙って通す非対称になる

## Consequences

- 正: 3 機能の呼び出し面が対称になり、共有層 (MAUI の VM 層など) から Loading / Toast も型だけで呼べる
- 正: VM 解決のモデルが 3 機能で同じになり、利用者が覚える規則が増えない
- 負: Loading / Toast の登録 API とレジストリ内部表現が VM factory ぶん増える
- 負: 型指定 show を使うには VM factory の明示登録が 1 行増える (インスタンス渡し show だけなら不要 — Dialog と同じ)

## Revisit When

- Loading / Toast に結果通知や進捗以外の VM とのやり取りが増え、Dialog と同型の 2 スロット構成では表現できなくなったとき
- core/ADR-0021 の VM 解決モデル自体が改訂されたとき (本決定はそのモデルの適用範囲拡張)

出典: kasane/changes/archive/2026-09-06-add-loading-toast-typed-show/exploration.md (課題 / 動機・検討した選択肢・決定事項) / kasane/changes/archive/2026-09-06-add-loading-toast-typed-show/second-opinion-spec-001.md (Toast の失敗分類の論点、2026-09-06 提案レビューで確定) / kasane/decisions/core/0021-vm-factory-registry-resolution.md (VM 解決モデル) / kasane/decisions/core/0025-loading-dedicated-registry-vm-progress-receiver.md・0029-toast-registry-and-inline-factory.md (拡張対象のレジストリ)

現行照合: 2026-09-06 確認 (実装完了時)。2 スロットのエントリは ios/Sources/KsDialogs/Registry/LoadingRegistryEntry.swift・android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/LoadingRegistryEntry.kt・maui/KsDialogs.Maui/Registry/LoadingRegistryEntry.cs (Toast も同名)、型指定 show / start は `KsLoading` / `KsToast` / `IKsLoading` / `IKsToast` の抽象メンバー、Toast の失敗 2 分類は Toast.swift / Toast.kt / ToastPresenter.cs の同期失敗と各 ToastCoordinator の受理後失敗で実装されている。実装で判明した帰結 2 点: (1) 契約への抽象メンバー追加により、契約を自前で実装する型は再準拠が要る (「非破壊」は呼ぶ側の互換の意 — 同 change の deviation.md) (2) Android の Toast は中身の生成を提示先の確保後に行うため、型指定経路の VM factory / configure もその時点で走り、提示先が現れないまま満了した表示では一度も呼ばれない (iOS は受理時点で走る)。判定: 維持
