# Design: add-model-binding-di

## Context

phase-6 の議論で契約レベルの決定は ADR 5本 (core/ADR-0018〜0021・maui/ADR-0005) として確定済み。本書はそれらを4形態 (iOS / Android / MAUI / KMP) の実装構成に落とす際の実装レベルの判断を記録する。到達イメージは agenda の素材 (api-sketch-final-form.md / api-sketch-maui-fallback-resolver.md) を参照。

現状の実装骨格 (2026-08-24 調査): 4形態とも「VM 型キーで factory を引く → factory に `(VM, DialogNotifier)` を渡す → DialogResultChannel が1回だけ settle → show の戻り値に型復元」。notifier は型消去 factory を呼ぶ瞬間に生成される。VM 契約は空マーカー。MAUI は C# 側に独立した registry / presenter を持つ (maui/ADR-0001)。

## Goals / Non-Goals

proposal.md の What Changes / Non-Goals を正とする (二重管理しない)。

## Decisions

### Decision 1: サイドテーブルの形態別実装機構

**採用案:** notifier の紐付け表 (core/ADR-0018) は各形態の言語機構で実装する:

- **C# (MAUI)**: `ConditionalWeakTable<object, ...>` — 参照同一性キー・GC 連動で要件そのもの
- **Kotlin (Android)**: 同一性キーの弱参照マップを自作する (キーを `WeakReference` + identity hash で包む小さな実装)。`WeakHashMap` は equals ベースのため使わない (data class VM の等価衝突)
- **Swift (iOS)**: `ObjectIdentifier` キーの辞書 + VM への弱参照ボックス (lock 保護)。エントリはダイアログ撤去時に明示除去し、弱参照は除去漏れ時の安全網とする (`ObjectIdentifier` の再利用対策として、取得時に弱参照の生存を確認する)

除去は正常配送だけでなく、紐付け後の全終端経路 (factory 例外・提示失敗・キャンセル・器消失) で行う (terminal-path 契約 — dialog-contract 側で仕様化)。

**理由:** 各言語で最も素直な同一性・弱参照機構を使い、共通の観察可能な挙動 (show 中だけ引ける・配送後は消える) を合わせる。
**代替案:**
- **A: Swift で ObjC associated object** — 却下。純 Swift クラスへの適用はランタイム実装詳細への依存が強く、strict concurrency との相性検証も別途要る。明示辞書のほうが挙動が読める
- **B: Kotlin で WeakHashMap** — 却下。equals ベースで data class VM が衝突する (core/ADR-0018 の Consequences に記載済み)

### Decision 2: `vm.notifier` アクセサの形態別表現

**採用案:**

- **Swift**: `extension DialogViewModel` に読み取り専用プロパティ `notifier: DialogNotifier<Result>?` (protocol は AnyObject 制約化)
- **Kotlin**: 拡張プロパティ `val <R> DialogViewModel<R>.notifier: DialogNotifier<R>?`
- **C#**: 拡張メンバーで `vm.Notifier` を提供する。言語バージョン都合で拡張プロパティが使えない場合は拡張メソッド `vm.GetNotifier()` を下限とする (Open Question 1)
- **iOS KMP 面**: `Dialog.shared.kmp` に `notifier(for: vm, result: R.self)` を追加 (result 省略 = Bool)。KMP VM は Swift の VM protocol に準拠しないため専用アクセサが必要 (agenda ②-2 の決定)。登録時の結果型と不一致の `result:` 指定は typed error とし (既存 KMP 面の型不一致エラーと対称)、show 外の nil と区別できるようにする
- **KMP Android 面**: 追加なし — 共有 VM は Native 型の typealias のため Kotlin 拡張プロパティがそのまま効く

**理由:** 各言語の慣用的なアクセサ形で「VM 定義は空のまま」(core/ADR-0018) を保つ。
**代替案:**
- **A: 全形態で関数形 (`notifier(for:)`) に統一** — 却下。プロパティが書ける言語でわざわざ関数にする理由がなく、api-sketch の書き味 (`viewModel.notifier?.complete(true)`) から後退する

### Decision 3: レジストリの内部表現 (VM factory の追加)

**採用案:** レジストリのエントリを「View factory (既存の型消去表現) + 任意の VM factory (型消去)」の組に拡張する。VM factory の登録 API は View factory 登録と対の形 (`register(VM 型, viewModel: factory)` 相当) とし、MAUI の `RegisterForDialog` はエントリ登録時に View factory と VM factory (DI 解決) の両方を配線する。

再登録の規則は**スロット単位の後勝ち**とする: View factory / VM factory の再登録は該当スロットだけを置換し、他方のスロットは保持する (`RegisterForDialog` 後に低水準 Register で View だけ差し替えても VM factory は残る)。fallback (maui/ADR-0005) の判定もスロットごとに独立して「明示 → fallback → 失敗」を適用する。show 時の解決は呼び出し時点のエントリのスナップショットを使う。
**理由:** 型指定呼び出しの解決 (core/ADR-0021) を既存の型消去設計の延長で実装でき、レジストリ共有 (core/ADR-0002) の構造を変えない。
**代替案:**
- **A: VM factory を別レジストリに持つ** — 却下。VM 型キーが2つの表に分散し、登録漏れ検出と共有の一貫性が複雑になる

### Decision 4: 型指定 show の実行順序とシグネチャ

**採用案:** 実行順序を「VM factory 解決 → VM 生成 → configure 完了 (非同期含む) → notifier 紐付け → View factory 呼び出し → 提示」に固定する。VM factory と configure は View factory と同じ UI スレッド保証 (Swift: MainActor / Android: Main dispatcher / MAUI: UI スレッド) で実行する。VM factory・configure の例外 (Kotlin の CancellationException、C# の faulted / canceled Task を含む) は提示に進まず呼び出し元へ伝播し、結果には化けない (この時点では notifier 未紐付けのためサイドテーブルの後始末は不要)。シグネチャの形 (名前は spec 化仮):

- Swift: `show(_ type: VM.Type, placement: DialogPlacement? = nil, configure: (@MainActor (VM) async -> Void)? = nil) async throws -> DialogResult<VM.Result>`
- Kotlin: `suspend fun <R, VM : DialogViewModel<R>> show(type: KClass<VM>, placement: DialogPlacement? = null, configure: (suspend (VM) -> Unit)? = null): DialogResult<R>`
- C#: **既存 Register と同型の2型引数形** — bool 省略形 `ShowAsync<TViewModel>(Action<TViewModel>? configure = null, ...) where TViewModel : IDialogViewModel` + カスタム結果型 `ShowAsync<TViewModel, TResult>(...) where TViewModel : IDialogViewModel<TResult>`。非同期 configure は `Func<TViewModel, Task>` オーバーロード。C# は制約から型引数を導出できないため、結果型はメソッド型引数として明示させる (second-opinion-spec-001 #1)

**理由:** configure の完了を View 生成より前に保証する (core/ADR-0019 の仕様義務)。notifier 紐付けを View factory より前に置くことで、factory 本体からも `vm.notifier` が読める。UI バインドされる VM の状態変更を形態間で同じスレッド保証に載せる。
**代替案:**
- **A: configure と View 生成の並行実行** — 却下。configure が設定した状態を View 初期化が読めることを保証できない
- **B: C# で1型引数のまま結果型を制約から導出** — 却下。C# は部分的型引数推論を持たず成立しない (Critical 指摘)。動詞を分ける案も core/ADR-0020 に反するため、2型引数形で ShowAsync のまま解決する

### Decision 5: MAUI 糖衣の配線 (provider の入手経路)

**採用案:** `AddKsDialogs(options)` が IServiceCollection にライブラリの初期化サービス (IMauiInitializeService 相当) を登録し、アプリ起動時に IServiceProvider をライブラリ内のホルダに保持する。`RegisterForDialog<TView, TViewModel>` の自動 factory (View 生成 + BindingContext = vm、VM の DI 解決) と fallback resolver は、show 時にこのホルダ経由で解決する。`RegisterForDialog` 単独 (AddKsDialogs なし) でも動くよう、初期化サービスの登録は RegisterForDialog も冪等に行う。

生成規則 (second-opinion-spec-001 #6 の確定):

- `RegisterForDialog` は TView / TViewModel を **TryAdd で transient** としてサービス自動登録する (show 毎回生成モデル core/ADR-0005 と整合)。利用者の既存登録があればそれを尊重する
- TView の生成は**現在の VM インスタンスを明示引数に渡して** ActivatorUtilities 相当で行う — TView のコンストラクタが TViewModel を受ける構成では show 対象の VM がそのまま注入され、コンストラクタ側と BindingContext が同一インスタンス・VM 生成は1回になる
- fallback resolver が返した View にも、ライブラリが BindingContext = 現在の VM を設定する
- provider ホルダは最後に初期化されたアプリの provider を保持する。複数 MauiApp の並存は本変更の対象外 (制約として文書化)
**理由:** 登録は builder 時・解決は show 時という時間差を、MAUI 標準の初期化機構で埋める。
**代替案:**
- **A: RegisterForDialog がクロージャに provider を直接キャプチャ** — 却下。builder 時点で provider は未構築のためキャプチャできない
- **B: static な provider 設定 API を公開** — 却下。static 差し込み口の粗 (後勝ち・null 上書き) を fallback resolver で消した決定 (maui/ADR-0005) と矛盾する

### Decision 6: 構成ミス失敗の表現

**採用案:** 「VM factory 未登録の型指定 show」「同一 VM インスタンスの並行 show」は、各形態の既存の構成ミス失敗経路 (View 未登録と同系統の例外) に合流させる。cancelled 等の結果には化けさせない (core/ADR-0004 の原則)。
**理由:** 利用者から見た「構成ミス = 例外で即失敗」の一貫性。
**代替案:**
- **A: 専用の新例外型を形態ごとに追加** — 却下ではなく従属判断。既存例外の分類で表現できない形態のみ最小限の追加に留める (spec 化ではなく実装時判断)

### Decision 7: Scenario ID 体系

**採用案:** 本変更の Scenario ID は `MB-<領域>-<NN>`。領域: NI (notifier 注入) / TS (型指定呼び出し) / IO (iOS 面) / AN (Android 面) / MA (MAUI 糖衣) / KM (KMP 面) / SM (samples)。挙動系 Scenario はテスト名に ID を含めて対応付ける (core/ADR-0016)。
**理由:** 先例 (PB- 系) の踏襲。
**代替案:** なし (規約の適用)

## Risks / Trade-offs

- C# のオーバーロード解決 (2型引数形と既存インスタンス版の分離、configure の Action / Func 重複と既定引数の両立) が成立しない形が見つかるリスク → api-surface-check の正/負 compile 検査で早期に検証し、不成立なら core/ADR-0020 の supersede を起こす。Critical だった結果型導出は2型引数形の採用で解消済み
- Kotlin の value class 拒否は実行時検出 (登録・型指定 show 時の構成ミス失敗)。コンパイル時には限定できない
- Swift の strict concurrency とサイドテーブル (lock 保護辞書) の整合。VM 契約の AnyObject 制約化で Sendable との組み合わせが変わる
- KMP iOS 経路の notifier アクセサは実 framework 越しでしか完全検証できない (core/ADR-0004 の既知制約と同種)。検証手順は verification/ に証跡を残す
- レジストリ内部表現の変更は既存テスト (DialogRegistryTests 系) の広い触り直しを伴う

## Migration Plan

一般公開前のため互換 shim なし。破壊的変更は VM 契約の参照型限定のみで、リポジトリ内の全 VM は既に class (確認済み)。既存の2引数 factory 登録・インスタンス渡し show は非破壊で残る。

## Open Questions

1. C# の拡張プロパティ (`vm.Notifier`) が現行の言語バージョン設定で書けるか。書けなければ拡張メソッド形にフォールバック (Decision 2)
2. C# の非同期 configure オーバーロード (Action / Func) の重複解決が既定引数と両立するか (MB-MA-01 の compile 検査で確定。シグネチャの骨格 — 2型引数形 — は確定済みで、ここは重複解決の細部のみ)

## ADR 候補

なし — 本変更の ADR 級の判断は phase-6 の議論時に起票済み (core/ADR-0018・0019・0020・0021・maui/ADR-0005)。本書の Decision 1〜7 はいずれも局所的な実装判断であり、コード+テストに委ねる。
