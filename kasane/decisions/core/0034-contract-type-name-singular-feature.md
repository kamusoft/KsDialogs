---
id: 0034
title: 契約の型名は「Ks + 機能名 (単数)」で揃え、Dialog の契約は KsDialogs から KsDialog へ改名する
status: accepted
date: 2026-09-06
---

## Context

公開 API は契約 interface + 既定 singleton エントリの両対応 (core/ADR-0002) で、既定エントリの class 名は原典踏襲の `Dialog` / `Loading` / `Toast` である。契約側の型名は Loading が `KsLoading`、Toast が `KsToast` (MAUI は `IKsLoading` / `IKsToast`) と「Ks + 機能名 (単数)」で揃っているのに対し、Dialog の契約だけが `KsDialogs` / `IKsDialogs` と複数形になっている。

この複数形は 2026-08-14 の phase-4 (縦串) 議論で kmp/ADR-0002 の本文に `interface KsDialogs` として現れたのが初出で、命名そのものを検討した記録は phase 履歴・ADR・セッション記録のいずれにもない。既定エントリの class に原典名 `Dialog` を先に割り当てたため Swift / Kotlin で契約に同名を使えず、モジュール名 (製品名 `KsDialogs`) がそのまま契約名に流用されたと推定される。オーナーは 2026-09-06 の探索で「エージェント主導で見逃したもので、意図した複数形ではない」と確認した。

ADR-0002 の命名ポリシーは「原典命名が非対称な箇所は対称性を優先して改める」であり、自ライブラリ内の非対称にも同じ基準を適用する。

## Decision

- 契約の型名は全形態で **`Ks` + 機能名の単数形** とする (Swift / Kotlin / KMP: `KsDialog` / `KsLoading` / `KsToast`、MAUI: `IKsDialog` / `IKsLoading` / `IKsToast`)
- Dialog の契約 `KsDialogs` / `IKsDialogs` を `KsDialog` / `IKsDialog` に改名する。旧名は残さない (cross/ADR-0001 互換 shim なし、cross/ADR-0009 lockstep で版間互換なし)
- 製品名としての `KsDialogs` (Swift モジュール名・SwiftPM パッケージ名・Kotlin パッケージ `jp.kamusoft.ksdialogs`・C# namespace・NuGet ID `KsDialogs.Maui`) は cross/ADR-0005 のとおり変えない。複数形が残るのは製品名だけになる

## Alternatives Considered

- **Loading / Toast 側を複数形 (`KsLoadings` / `KsToasts`) にして揃える** — 却下。英語として不自然で、揃えるべき側が逆
- **現状維持で「製品名の流用」という経緯だけ記録する** — 却下。非対称が公開 API に残り続け、一般公開後に直すコストが上がる

## Consequences

- 正: 3機能の契約名が同じ規則で読め、DI 登録・fake 差し替えのコードが機能間で対称になる
- 正: 型名 `KsDialog` と製品名 `KsDialogs` の役割が単複で区別される
- 負: 公開 API の破壊的改名 (一般公開前のため利用者影響はなし)
- 負: docs-refresh の禁止トークン lint が「単数形 `KsDialog` = 誤表記」を前提にしており、前提を逆転させる改修が要る
- 負: concepts / handbook / README / skills の型名表記を一斉に追随させる
- 正 (実装結果): 旧名 `KsDialogs` / `IKsDialogs` が型として解決できないことは、4 形態の負のコンパイル検証 (handbook/cross/test-execution.md の負の検査表) で固定した
- 負 (実装結果): 契約を受ける変数・引数名 (`dialogs: KsDialog` / `IKsDialog dialogs`) と KMP テストの `FakeDialogsSubstitutionTests` は識別子名であり、改名の境界 (型名のみ) の外として複数形のまま残った。揃えるなら別の変更で扱う

## Revisit When

- 4 機能目以降の表示系契約を足すとき、または既存の機能名が単数形で表せない概念に変わったとき (前提 (Context) の「Ks + 機能名 (単数)」が崩れたとき)

出典: kasane/changes/archive/2026-09-06-rename-dialog-contract-singular/exploration.md (課題 / 動機・検討した選択肢・決定事項) / kasane/changes/archive/2026-09-06-rename-dialog-contract-singular/review-001.md (実装結果: 負の検査と残る複数形の識別子) / kasane/decisions/core/0002-public-api-shape.md (命名ポリシー) / kasane/decisions/kmp/0002-thin-facade-native-registry.md (契約名の初出)
