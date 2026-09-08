---
id: 0036
title: MAUI Android の Dialog / Loading の中身供給も managed 側の預かり口を通し、3 面を対称化する
status: accepted
date: 2026-09-08
amends: 0033
---

## Context

core/ADR-0033 は、MAUI の managed/native 境界では例外を値に変えて渡し (C# 側の共通部品 `BridgeContentSupply` が provider 内の例外を捕捉して nullable なコンテンツとして native へ返す)、Loading / Dialog では退避した元例外を呼び出し元へ再送出すると決めた。ただし Android の Loading / Dialog の provider には `BridgeContentSupply` を通さず、「Kotlin 側の受け皿が `catch (Throwable)` で `JavaProxyThrowable` も捕まるため、観察可能挙動は現状で成立している」として対称化を採らず、将来課題とした (同 ADR の Alternatives と Consequences の派生行)。

MAUI パッケージングの前に公開面を固める作業で、1 行登録 (`RegisterForDialog` / `RegisterForLoading` / `RegisterForToast`) がライブラリ自身で組み立てる View の生成失敗を、専用の失敗種別 `DialogException.ViewCreationFailed` (元例外を InnerException に保持) として呼び出し元へ届ける要件が立った。このとき Android の現状を実機で再現すると、Kotlin 側が捕まえた `JavaProxyThrowable` は互換面の `onFailed(message)` を経由してメッセージだけの `InvalidOperationException` になり、例外の型と InnerException が失われる。「観察可能挙動は成立している」という却下理由は、届く例外の型を契約に含めた時点で成り立たない。

前提:
- iOS 側の Dialog / Loading は既に `BridgeContentSupply.CreateOrFail` + `BridgeContentFailure` (預かり口) を通しており、元例外が型・メッセージ・スタックを保って呼び出し元へ届く
- Kotlin 互換面の Toast の中身供給は既に nullable (`MauiDialogContent?`) で、null を失敗経路へ合流させる形を持つ
- Kotlin 互換面は輸送層であり利用者向けの公開契約ではない (maui/ADR-0001)

## Decision

core/ADR-0033 の決定のうち「Android の Loading / Dialog の provider には `BridgeContentSupply` を通さない (対称化は将来課題)」を本決定で置き換える。他の決定 (iOS の throws 化・境界で例外を値に変える原則・面ごとの合流先) は維持する。

- Android の `PlatformDialogGateway` / `PlatformLoadingGateway` の中身供給も、iOS と同じ `BridgeContentSupply.CreateOrFail` + `BridgeContentFailure` 経由にする
- 互換面から閉鎖 / 完了の失敗通知を受けたとき、預かり口に元例外があればそれを、無ければ互換面のメッセージを包んだ `InvalidOperationException` を呼び出し元へ投げる
- Kotlin 互換面の Dialog / Loading の中身供給 (`createContent()`) は Toast と同じ nullable にし、null は既存の失敗経路 (Dialog = 閉鎖通知の失敗、Loading = 完了通知の失敗) に合流させる。生の例外は境界を越えない
- 結果として、Dialog / Loading の中身生成の失敗は iOS / Android の実機経路でもユニットテストの fake gateway 経路でも同じ例外型で呼び出し元へ届く。1 行登録の View 生成失敗はその型として `ViewCreationFailed` を用いる (種別の意味と公開面は concepts の MAUI の DI 連携と登録糖衣・Dialog 公開面が持つ)

## Alternatives Considered

- **失敗種別 `ViewCreationFailed` は新設するが Android の預かり口は配線しない**: 却下。Android では JNI 境界で型が落ち、新設した型が呼び出し元へ届かない (`onFailed(message)` しか残らない)。core/ADR-0033 が定めた「呼び出し元が待っている面では元の例外を届ける」との乖離も残る
- **Android は Kotlin の `Throwable` を `JavaProxyThrowable` として C# へ戻し、元例外を復元する**: 却下。境界を生の例外が越える形で core/ADR-0033 の「例外を値に変えて渡す」に反し、Kotlin 側の catch-all と二重になる
- **現状維持 (対称化は将来課題のまま)**: 却下。却下理由「観察可能挙動は成立している」が、届く型を契約に含めた時点で実測 (メッセージだけの `InvalidOperationException`) により崩れている

## Consequences

- 正: Dialog / Loading の中身生成の失敗が、両 OS で同じ型・同じ InnerException で呼び出し元へ届く。concepts の失敗種別表が 1 対 1 で読める
- 正: core/ADR-0033 の「派生」として残っていた 3 面の非対称が解消し、MAUI の managed/native 境界の規則が iOS / Android で同じ形になる
- 負: Kotlin 互換面の Dialog / Loading の `createContent()` の戻り値が nullable に変わる。互換面は公開契約ではないが、facade と binding の生成コードは追随が要る
- 負: Android の失敗経路が「null を返す → 互換面が失敗通知 → 預かり口から元例外」の 3 段になり、預かり口が空のときのフォールバック文言 (互換面のメッセージ) は通常経路では利用者へ届かない。フォールバックの正しさは互換面のテストで見る

## Revisit When

- 前提 (Context) が崩れたとき — とくに .NET for Android が managed 例外を Java 側へ型を保って渡す手段を持ったとき (預かり口の必要性を見直す)

出典: kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/design.md (Decision 6 — 採用案・理由・代替案) / kasane/roadmaps/package-distribution/phases/phase-6-maui-packaging/agenda.md (決定事項「View 生成失敗は `DialogException.ViewCreationFailed` を新設して報告し…」) / kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/review-001.md (Minor 1 — core/ADR-0033 の却下案の採用にあたるとの指摘) / kasane/changes/archive/2026-09-08-add-maui-nuget-distribution/evidence/view-creation-failure/README.md (修正前後の A/B 実測)

現行照合: 2026-09-08 確認 (実装完了時)。maui/KsDialogs.Maui/Platforms/Android/PlatformDialogGateway.cs・PlatformLoadingGateway.cs の `ContentProvider` が `BridgeContentSupply.CreateOrFail` を通し、`ClosureListener.OnFailed` / `CompletionListener.OnFailure` が `contentFailure.Cause` を優先して投げる。Kotlin 互換面は maui/android/native/ksdialogs-maui-bridge の `MauiDialogBridge.kt` / `MauiLoadingBridge.kt` の `createContentView()` が null を `error(...)` で既存の失敗経路へ合流させる。判定: 維持

関連: core/ADR-0033 (境界の原則と iOS 側の配線 — 本 ADR が Android 側の一部を置き換える) / maui/ADR-0001 (互換面は輸送層で公開契約ではない)
