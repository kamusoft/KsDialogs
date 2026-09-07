# Design: add-loading

## Context

phase-7 の議論で Loading の契約レベルの設計は確定済み (core/ADR-0022〜0024 + agenda 決定事項)。本書はそれを現状コードに落とすための実装設計判断を扱う。前提となる現状構造:

- 公開面の流儀: 契約 interface + 既定シングルトン (core/ADR-0002)。Dialog は `KsDialogs` protocol/interface + `Dialog.shared` / `Dialog.instance` / `Dialog.Instance` / `expect object Dialog`
- 共有部品の実体: コンテンツホスティングは `DialogViewFactory` → `DialogContent` (iOS) / `DialogViewFactory` → `View` (Android) の型消去。レイアウトは `DialogLayoutResolver` (両 OS)。トランジションは `DialogTransition` 契約 + `DialogTransitionAnimator` だが、フック実行・覆いフェードは器 (`DialogContainerViewController` / `DialogContainer`) の内部に埋まっている
- 器: iOS は present 連鎖の先端に `DialogContainerViewController` を present、Android は `android.app.Dialog` 派生の `DialogContainer`。Loading はこのスタックに載せない (core/ADR-0022)

## Goals / Non-Goals

proposal.md のとおり (Goals = What Changes、Non-Goals = proposal の Non-Goals)。本書は「どう作るか」の判断のみ扱う。

## Decisions

### Decision 1: 公開面は Loading 専用の契約 + シングルトン (Dialog と別面)

**採用案:** 契約 `KsLoading` (iOS protocol / Android・KMP interface / MAUI `IKsLoading`) + 既定シングルトン `Loading.shared` / `Loading.instance` / `Loading.Instance` / `expect object Loading` を新設する。命名は原典 (`Loading.Instance`) 踏襲。
**理由:** core/ADR-0002 (契約 interface + 既定シングルトン・命名は原典踏襲) の直接適用。Loading は結果を返さないライフサイクルで、`KsDialogs` の show (結果をちょうど1回返す — core/ADR-0003) と契約の性質が異なるため、同居させると interface の意味が濁る。
**代替案:**
- **A: `KsDialogs` (Dialog 面) に Loading API を同居** — 却下: 結果契約と結果なしライフサイクルが1つの interface に混在し、テスト差し替え (契約 interface のモック) の単位も不自然になる。原典も `IDialog` / `ILoading` を分けていた
- **B: シングルトンなしのフリー関数** — 却下: core/ADR-0002 の「契約 interface + 既定シングルトンの両対応」と不整合。DI での差し替え口が消える

### Decision 2: 呼び出し面は show / hide / setMessage + 値を返すスコープ形

**採用案:** 命令形 `show(message:placement:)` / `hide()` / `setMessage(_:)` と、スコープ形 `start(message:placement:action:)` (原典 StartAsync 相当) を持つ。スコープ形はジェネリクスで action の戻り値をそのまま返す。show は合流1件の開始、hide は合流数によらず即閉じ (core/ADR-0024)。setMessage は合流に関与しない表示中のみのメッセージ更新 (後勝ち)。placement は show 引数の供給規則 (core/ADR-0015) をそのまま使う。
**理由:** 原典の呼び出し面 (Show / Hide / SetMessage / StartAsync) の継承。スコープ形は開始・終了が対になり合流カウントと噛み合う (phase-7 論点3)。値返しは「処理の結果を受け取りつつローディングを出す」頻出形を1行にする低コストな一般化で、契約は変えない。
**代替案:**
- **A: スコープ形のみ (命令形なし)** — 却下: 進捗を伴わない単純な出し入れ (画面遷移をまたぐ表示など) がクロージャ強制になり、原典の Show/Hide 利用パターンを継承できない
- **B: setMessage 廃止 (show の後勝ちで代用)** — 却下: show は合流1件の開始なので hide との対応が要る。メッセージ更新のたびに合流が増えるのは誤用の温床。原典 Sample も StartAsync 中の SetMessage を実用している

### Decision 3: カスタム View 版は Loading 専用レジストリ + VM の進捗受け口 interface

**採用案:** Dialog のレジストリとは別に Loading 用レジストリ (VM 型キー → View factory、使い捨て生成) を持ち、登録形は Dialog と同型 (従来 View 系 / 宣言的 UI 系の技術別オーバーロード — core/ADR-0011 踏襲、ただし resultChannel なし)。表示は `show(vm:...)` / `start(vm:...)` とインライン factory 版。進捗のカスタム View への配送は、VM が進捗受け口 interface (`LoadingProgressReceiver` 相当 — `onProgress(Double)` 1メソッド) を実装している場合にライブラリが転送する (未実装なら転送なし)。
**理由:** Dialog レジストリの factory 形は resultChannel 前提で Loading に合わない。別レジストリなら「Loading として登録した VM を Loading として出す」対応が型で閉じる。進捗の VM 経由配送は、VM を状態の運び手とする既存方針 (core/ADR-0018 系) と一貫し、宣言的 UI では VM の観測がそのまま表示更新になる。
**代替案:**
- **A: Dialog レジストリに相乗り** — 却下: factory 署名 (resultChannel) が合わず、同じ VM 型を Dialog と Loading の両方に登録したいケースで衝突する
- **B: factory 引数で進捗ソースを渡す** — 却下: 従来 View 系で購読・解除の管理が利用者コードに漏れる。VM 経由なら受けたい者だけが interface を実装すればよい
- **C: カスタム View への進捗転送なし (利用者が action 内で自分の VM を直接更新)** — 却下: 既定とカスタムで start の進捗報告の意味が変わり、契約が2枚舌になる

### Decision 4: 器は iOS = key window 直貼り / Android = 専用の全画面透過 Window とし、部品は器から切り出して共有する

**採用案:** iOS は key window への `addSubview` (原典実証済みの方式) で Loading 専用オーバーレイを新設する。Android は **Loading 専用の全画面透過 Window** (原典 LoadingPlatformDialog と同方式) を最前面に出す — 既存の Dialog 器 (`DialogContainer` = `android.app.Dialog`、別 Window) より手前に表示するため。この Window は KsDialogs の Dialog 機構の提示経路・多段表示の意味論には参加しない (core/ADR-0022 の本質は「Dialog 機構の提示スタックと意味論に絡めない」ことであり、OS の Window を使うこと自体ではない)。Android の回転 (Activity 再生成) では、coordinator (Decision 8) が保持する合流状態から新 Activity へ再取り付けする (`ResumedActivityTracker` を利用)。「ダイアログ表示中に Loading が視覚・入力の双方で最前面になる」ことは両 OS の実提示 Scenario (LD-AT-04) で成立性を固定する。レイアウト適用 (`DialogLayoutResolver` の呼び出しと制約/測定への反映) とトランジション実行 (フック起動・覆いの別レイヤフェード) は、現在 Dialog の器の内部にある実装を共有可能な部品へ切り出し、両方の器から使う。切り出しは挙動を変えないリファクタリングとして先行タスク化し、既存テスト (Scenario 97本 + レイアウト共通ケース表) を回帰ガードにする。
**理由:** iOS の直貼りは原典で実証済み。Android の decorView 直貼り (当初案) は、既存ダイアログが別 Window に出る現行構造では Loading が背面に沈み「ダイアログより手前」を満たせない (second-opinion spec-001 C1 で判明)。専用 Window は原典が同要件を満たした実証済み方式。部品の切り出しは「二重実装を避ける」という ADR-0022 の採用理由そのもの。
**代替案:**
- **A: Android も decorView への addView** — 却下: `android.app.Dialog` ベースの既存 Dialog 器は Activity と別の Window に表示され、decorView の子はその背面になる。「ダイアログより手前・背後の操作遮断」を満たせない
- **B: iOS も専用 UIWindow / Android は WindowManager 直接 addView** — 却下: ウィンドウレベル管理・キーウィンドウ切り替え・トークンとライフサイクルの自前管理が増える。iOS は直貼り、Android は Dialog ベースの透過 Window という原典方式で要件は満たせる
- **C: 部品を切り出さず Loading 器に同型実装を持つ** — 却下: core/ADR-0022 の却下案 (完全独立機構) の縮小再生産。トランジション・レイアウト適用の修正が常に2箇所になる

### Decision 5: テストは新領域プレフィックス + レイアウト共通ケース表の全量適用

**採用案:** Loading の挙動 Scenario は新しい領域プレフィックス `LD` (例: `LD-SG-NN` 合流・単一性 / `LD-PR-NN` 進捗 / `LD-TR-NN` 演出) で採番し、`scripts/scenario-id-coverage.py` の対象に載せる。両 Native ミラー必須領域 (MIRROR_AREAS) に LD 系を追加する。レイアウトは `core/layout-spec/cases.json` を Loading 器でも全量回す (isCanceledOnTouchOutside のケースのみ「Loading では無効」の期待に読み替え)。公開 API 形状検査は既存の api-surface-check (正・負) に Loading 面を追加する。
**理由:** core/ADR-0016 (Scenario ID) と core/ADR-0009 (ケース表の実測全量検証 — 「resolver の単体検証では反映漏れを見逃す」) の直接適用。器が変われば適用漏れの形も変わるため、共有 resolver でも器ごとの実測が要る。
**代替案:**
- **A: ケース表は代表サブセットのみ** — 却下: 「実測でなければ受け入れ条件を満たさない」とした ADR-0009 の判断と不整合。サブセット選定という新しい恣意も持ち込む
- **B: Scenario を既存領域 (PB 系) に混ぜる** — 却下: 領域プレフィックスは機能面の対応で引くのが既存運用 (PB=presentation-behavior, MB=model-binding)。Loading は独立機能面

### Decision 6: スタイルと既定コンテンツ用 options はシングルトンの設定プロパティ、KMP 公開面には置かない

**採用案:** `LoadingStyle` 値オブジェクト (インジケータ色・メッセージのフォントサイズと色・既定メッセージ・進捗フォーマット) を各 Native / MAUI に定義し、`Loading` シングルトンの設定プロパティ (`Loading.shared.style` 等) で一括設定する。あわせて、**既定ローディング用の器メタ属性も既存 `DialogOptions` を再利用した設定プロパティ** (`Loading.shared.options` 相当) で受ける — 既定ローディングには利用者が属性を添付する View がなく、表示 API からは placement しか渡せないため、これが既定コンテンツへの「添付」相当になる (isCanceledOnTouchOutside は設定されても Loading では無効のまま)。器は style・options とも各表示の開始時に読む (core/ADR-0023 と同じ採用時点)。KMP の commonMain には style / options API を公開しない (色が境界を渡らない — DialogOptions と同じ非対称)。
**理由:** ADR-0023 の決定 (一括設定・各表示開始時に読む) の最小実装。シングルトンのプロパティなら新しいグローバル設定機構を作らずに済み、契約 interface のモックでも差し替えられる。
**代替案:**
- **A: 独立した Configurations 的グローバル設定型** — 却下: 原典 `Configurations` の再生産。設定の置き場が公開面の外に増え、DI 差し替えから外れる
- **B: KMP commonMain に色抜きの部分 style を公開** — 却下: 「同じ名前で片方だけ効く」半端な面になる。styling は各 OS 側で設定する非対称を DialogOptions と揃える

### Decision 7: 進捗フォーマットは書式文字列ではなくフォーマット関数

**採用案:** `LoadingStyle` の進捗フォーマットは各形態の関数型 (`(message, progress) -> String`。progress は未報告時 nil/null) とし、既定実装 (メッセージ + 改行 + パーセント表示 — 原典 `"{0}\n{1:P0}"` 相当) をライブラリが持つ。
**理由:** .NET の複合書式文字列は Swift / Kotlin に同義の構文がなく、書式文字列を契約にすると「意味と挙動は全形態で同一」が崩れる。関数なら各形態のイディオムのまま同じ観察可能結果を定義できる。
**代替案:**
- **A: .NET 書式文字列を全形態で解釈** — 却下: 書式パーサの自前実装が必要になり、ロケール・書式差のバグ面が増える
- **B: フォーマット固定 (カスタマイズ不可)** — 却下: 原典 LoadingConfig の ProgressMessageFormat 相当を削ることになる (styling を持つ決定 — ADR-0023 と不整合)

### Decision 8: プロセス内 Loading coordinator を状態の唯一の正とし、全入口が委譲する

**採用案:** 1 OS プロセス内に Loading coordinator (合流カウント・世代・表示中コンテンツ・最新メッセージ/進捗の保持者) を1つ置き、既定シングルトン・契約 interface から構築した実装インスタンス (DI 用 — core/ADR-0002)・MAUI bridge・KMP actual のすべてがそこへ委譲する。状態操作 (開始・終了・hide・メッセージ・進捗) は UI スレッド (main) 上で受理順に直列化し、「後勝ち」は coordinator の受理順で定義する。呼び出し面は任意スレッドから呼べる (内部で main へ移す — Dialog 面と同じ)。進捗受け口 (VM) とフォーマット関数の呼び出しも UI スレッド上で行う。
**理由:** 入口ごとに状態を持つと、同一 OS 上に複数の Loading が表示され単一表示の契約 (core/ADR-0024) が破れる (second-opinion spec-001 M4)。直列化と受理順を規定しないと「最新の報告」が実装依存になる (同 M7)。
**代替案:**
- **A: 各入口 (シングルトン / bridge / KMP actual) が独立に状態を持つ** — 却下: 入口をまたぐ利用で単一表示・合流の契約が成立しない
- **B: スレッド規則を実装任せにする** — 却下: 並行報告の「最新」が OS・形態ごとにぶれ、Scenario テストの期待値が固定できない

## Risks / Trade-offs

- **切り出しリファクタリングの回帰リスク**: トランジション実行・レイアウト適用は器と密結合 (iOS `DialogContainerViewController` 779行)。切り出しを独立タスクとして先行させ、既存テスト全通過を器の変更前後で確認する
- **Android の回転再取り付け**: Activity 再生成をまたぐ専用 Window の持ち越しは原典 (FragmentManager 復元) と別方式になる。合流状態・スタイル・進捗の唯一の正を coordinator (Decision 8) に置き、器 (Window) は使い捨てにすることで再取り付けを単純化する
- **合流とキャンセルの絡み**: action が例外を投げた場合も合流1件の終了として数える (でないと表示が閉じなくなる)。契約はデルタスペックの Scenario で固定する
- **bridge ABI の広がり**: MAUI bridge / KMP interop に Loading 面一式が乗る。公開 API の全値 (placement・style・進捗) が境界を渡ることを API 形状検査 (正) で担保する

## Migration Plan

新機能の純追加のため利用者移行はなし。実装順は tasks.md のとおり「部品切り出し (回帰ガード付き) → core 契約と Native 2実装 → ラッパー2形態 → Sample」。

## Open Questions

なし (残っていた実装判断は Decision 1〜7 で確定)。

## ADR 候補

- **Decision 3** (Loading 専用レジストリ + VM 進捗受け口): 登録機構の境界を増やす決定で、Dialog レジストリとの役割分担を将来にわたり制約する
- **Decision 4** (iOS 直貼り / Android 専用 Window + 部品切り出し): core/ADR-0022 の実装形の確定。覆すコストが高い
- **Decision 8** (プロセス内 coordinator への全入口委譲): 形態の境界を越えて単一表示契約を支える構成で、将来の入口追加を制約する
- Decision 1・2 は core/ADR-0002・0024 の適用、Decision 5 は ADR-0009・0016 の適用、Decision 6 は ADR-0023 の帰結、Decision 7 は局所 API 詳細のため候補にしない
