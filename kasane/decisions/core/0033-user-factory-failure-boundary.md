---
id: 0033
title: 利用者 View factory の失敗は言語境界の内側で捕捉し、各機能の既存失敗契約へ合流させる
status: accepted
date: 2026-08-28
amended-by: 0036
---

## Context

カスタム View の factory は利用者コードであり、例外を投げうる。Toast の契約 (受理後の失敗モデル) は「factory の例外は警告ログを出してその表示だけを破棄し、後続表示を継続する」ことを求めるが、実装当初はこの失敗が経路上で捕捉できない形態があった:

- **iOS native**: factory 閉包の契約が非 throwing (`(ViewModel) -> UIView`) のままでは、Swift の factory が失敗を表明する手段がなく、契約の失敗条項が到達不能 (Kotlin / C# の factory が自然に例外を投げられることとの非対称)
- **MAUI iOS**: C# の factory 例外が managed/native (ObjC ブロック) 境界を生で越えると未処理障害 = プロセス終了になる。Swift 側 provider が非 throwing なため、例外を Swift の `Error` として受ける経路がなかった (second-opinion-code-001 Major 2)
- **MAUI Android**: .NET for Android は managed 例外を `android.runtime.JavaProxyThrowable` (**`java.lang.Error` 派生**) に包んで Java へ送出するため、Kotlin 側の `catch (Exception)` を素通りし、同じくプロセス落ちになる (review-003 Major — javap と生成バインディングで実証)

さらに、この修正の過程で **Loading / Dialog の既存面にも同型の穴** (MAUI iOS の C# provider が例外未捕捉・Swift 側非 throwing) があることが判明し、オーナー決定により本修正へ同梱された (Toast で作った修正の型がそのまま型紙になるため)。

## Decision

利用者 View factory の失敗は、言語境界を生の例外が越えない形で捕捉し、各機能の既存失敗契約へ合流させる。

- **iOS native の factory 閉包契約は throws** (`(ViewModel) throws -> UIView`)。Toast だけでなく Loading / Dialog も同じ形に統一する。失敗の合流先は各機能の既存失敗契約 — Toast = 警告ログ + その表示 1 枚だけの破棄と資源解放 (後続継続) / Loading = 開始そのものの失敗 / Dialog = 閉鎖通知の Failed
- **MAUI の managed/native 境界は例外を値に変えて渡す**: C# 側の共通部品 `BridgeContentSupply` が provider 内の例外をすべて捕捉し、nullable なコンテンツとして native へ返す。native 側 (Swift / Kotlin) は null を通常の失敗 (Swift = 型付きエラー / Kotlin = `Exception` 派生) に変換して既存の失敗経路に合流させる。入口は面ごとの結末で 2 つに分ける — `CreateOrDiscard` (Toast: 破棄系) / `CreateOrFail` (Loading・Dialog: 失敗系 + 元例外の退避 `BridgeContentFailure`)
- **呼び出し元が待っている面では元の例外を届ける**: Loading / Dialog は呼び出し元が同じ C# スコープで待っているため、退避した元例外を型・メッセージ・スタックを保ったまま呼び出し元へ再送出する。Toast (fire-and-forget) は呼び出し元が既に居ないため警告ログのみ

## Alternatives Considered

- **iOS の factory 閉包を非 throwing のまま維持する** — 却下: 契約の受理後失敗モデルが iOS で到達不能になり、MAUI bridge の例外境界修正も成立しない (deviation.md 記録)
- **Kotlin 側を nullable 化せず、null を NPE として偶然 `catch (Exception)` に掛ける** — 却下: 偶然に頼った経路で、ログにも実態と食い違う NPE が残る (review-003)
- **Android の Loading / Dialog の provider にも `BridgeContentSupply` を通して 3 面を対称化する** — 採らず: Kotlin 側の受け皿が `catch (Throwable)` であり `JavaProxyThrowable` (`Error` 派生) も捕まるため、観察可能挙動は現状で成立している (review-003 — 対称化はオーナー判断でよいと整理)

## Consequences

- 正: 利用者 factory の例外が、どの形態・どの機能でもプロセス落ちにならず、既存の失敗契約として観察できる (失敗系は実機観測まで実施 — MAUI iOS の Toast / Loading / Dialog、MAUI Android の Toast)
- 正: Loading / Dialog では元の例外が型・メッセージ・スタックを保って呼び出し元へ届く (型付き例外 `DialogException` 系もそのまま)
- 負: iOS の公開 protocol (`KsToast` / `KsLoading` / `KsDialogs`) の要件シグネチャ変更は、**外部でこれらに準拠していた型に対して breaking** (Swift のプロトコル witness マッチングは引数位置の関数型に反変の緩和を持たない — review-003 が最小再現で確認)。呼び出し側は throws 共変性によりソース互換 (samples 無改変ビルドで実証)。ライブラリは一般公開前であり既知の外部準拠は無いが、リリースノートに記載すべき変更として記録する
- 負: Toast では元の例外が警告ログにしか残らない (fire-and-forget の構造上の制約)
- 派生: Android の Loading / Dialog の C# provider は Kotlin 側の `Throwable` catch に依存したまま (`BridgeContentSupply` 未適用)。失敗理由が文字列でしか渡らない制約も残っており、3 面の対称化は将来課題

出典: kasane/changes/archive/2026-08-28-add-toast/deviation.md (throws 化・スコープ追加・失敗系実機観測の各項) / kasane/changes/archive/2026-08-28-add-toast/second-opinion-code-001.md (Major 2) / kasane/changes/archive/2026-08-28-add-toast/review-003.md (Major・Minor 1) / kasane/changes/archive/2026-08-28-add-toast/review-004.md (確認 (a)(b))
