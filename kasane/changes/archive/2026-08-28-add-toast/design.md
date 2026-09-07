# Design: add-toast

## Context

phase-8 で確定した設計決定 (core/ADR-0028〜0032) を4形態に実装する。器・レイアウト・演出の共有部品 (core/ADR-0026 で切り出し済み) と Loading の実装型紙 (coordinator = core/ADR-0027、内蔵コンテンツ = core/ADR-0023、レジストリ = core/ADR-0029 の先例 0025) を最大限再利用する。

実現経路の裏取り (lessons/spec-review L-001):

- iOS の器: `LoadingContainerViewController` (key window 直貼り・`DialogLayoutApplier`/`DialogTransitionRunner` 共有) が型紙。非モーダル化はルート View のヒットテスト透過で実現可能
- Android の器: `LoadingContainer` (`android.app.Dialog` 派生の全画面透過 Window) が型紙。非モーダル化は Window フラグ (`FLAG_NOT_FOCUSABLE` + `FLAG_NOT_TOUCHABLE`) で実現可能
- Loading 前面化: Android は `LoadingCoordinator` に `detachForReattach()` + 演出なし再取り付け (`attach(playsPresentation = false)`) が実装済みで流用可能。iOS は subview 挿入位置 (`insertSubview(belowSubview:)`) で制御可能
- MAUI / KMP: Loading と同じ bridge / gateway 面 (`MauiLoadingBridge` / `AndroidLoadingGateway` / `IosLoadingGateway`) の隣に Toast 面を増設する。新しい interop 機構は不要

## Goals / Non-Goals

proposal.md の What Changes / Non-Goals を正とする (二重管理しない)。

## Decisions

### Decision 1: KsToast 契約の形 — KsLoading 同型のメソッド面から合流・進捗・スコープ形を落とす

**採用案:** 契約 protocol / interface `KsToast` + 既定 singleton エントリ `Toast.shared` (core/ADR-0002)。メソッド面 (iOS の例):

```swift
public protocol KsToast: AnyObject, Sendable {
    var registry: ToastViewRegistry { get }
    var style: ToastStyle { get set }

    /// デフォルト View でメッセージを表示する。fire-and-forget (core/ADR-0031)
    func show(message: String, duration: Int?, placement: DialogPlacement?)

    /// 登録済みカスタム Toast View を表示する (core/ADR-0029 レジストリ経路)
    func show<ViewModel: ToastViewModel>(
        _ viewModel: ViewModel, duration: Int?, placement: DialogPlacement?) throws

    /// 登録せずその場の factory で表示する (core/ADR-0013 インライン経路)。
    /// 従来 View 系 + SwiftUI の技術別オーバーロード (core/ADR-0010・0011)
    func show<ViewModel: ToastViewModel>(
        _ viewModel: ViewModel, duration: Int?, placement: DialogPlacement?,
        factory: @escaping @MainActor @Sendable (ViewModel) -> UIView) throws
    // + SwiftUI 版 / 省略形 extension は KsLoading と同じ流儀
}
```

- 形態別の入口の形 (完全なシグネチャはこの表を正として各形態のイディオムで写す):

| 形態 | エントリ | message 入口 | カスタム (登録 / インライン) | style |
|---|---|---|---|---|
| iOS | `Toast.shared: KsToast` | `show(message:duration:placement:)` | `show(_:duration:placement:)` / `+factory` (UIKit・SwiftUI オーバーロード) | `style: ToastStyle` |
| Android | `Toast` object (`KsToast` 実装) | `show(message, durationMs, placement)` | 同左 (従来 View 系。Compose は ksdialogs-compose の拡張) | `style: ToastStyle` |
| MAUI | `Toast` 静的クラス (`IKsToast`) | `Show(message, durationMs, placement)` | C# 層レジストリ登録 (DI 1行含む) / インライン (VisualElement factory) | `Style: ToastStyle` |
| KMP | commonMain `Toast` (`KsToast`) | `show(message, durationMs, placement)` | 型キーのみ (factory 登録は OS 側 / Swift 登録は Swift パッケージ側) | commonMain 非公開 |

- レジストリの共有境界: Native の入口 (シングルトン・interface インスタンス) と KMP は同一レジストリを共有 (kmp/ADR-0002)。**MAUI は C# 層に自分のレジストリを持ち、互換面専用の Toast 用 VM 型1つだけを Native レジストリに登録する** (maui/ADR-0001 の構造踏襲)
- show は同期 (非 async)・戻り値なし。表示は受理順に UI スレッドへ直列化して開始する。器は factory・カスタム View・演出フックへの参照を撤去完了まで保持する (fire-and-forget でも途中解放しない)
- `hide` / `setMessage` / スコープ形 (`start`) / 進捗口は持たない — fire-and-forget (core/ADR-0031) に存在しない概念
- `DialogOptions` プロパティも持たない — Toast には覆い (overlayColor) も外側タップ閉じ (isCanceledOnTouchOutside) も存在せず、器メタ属性で意味を持つものがない (core/ADR-0014 の「器にしか実現できない属性のみ」を Toast に適用すると空集合)

**理由:** KsLoading (ios/Sources/KsDialogs/Presentation/KsLoading.swift) のメソッド面から fire-and-forget に不要なものを機械的に落とした形が、4形態の既存イディオム (MAUI の C# interface / KMP commonMain interface) にそのまま写せる。同期 show は「表示完了を待つ手段を契約に設けない」(core/ADR-0031) の素直な表現。

**代替案:**
- **A: show を async にして器の取り付け完了まで待つ (Loading 同型)** — 却下: Loading の async show は「戻った時点で操作ブロック有効」という契約に意味があった。Toast は何もブロックしないため待つ意味がなく、fire-and-forget の宣言 (ADR-0031) と食い違う
- **B: ToastViewModel を設けず factory 引数のみにする** — 却下: レジストリの型キー (core/ADR-0029) に VM 型が必要。Loading と同じ「VM = データ運搬体 + 型キー」の最小 protocol が一貫する

### Decision 2: duration は ms の整数で全形態統一し、nil は ToastStyle の既定へ委譲する

**採用案:** `duration: Int?` (ミリ秒)。nil = `ToastStyle` の既定 duration (初期値 1500)。0 以下の値は無効値として style 既定に丸め、警告ログを出す (style の既定自体が 0 以下に設定された場合は内蔵既定 1500 へ丸める)。上限クランプなし (core/ADR-0031)。計時は show の受理時点から単調時計で行い実時間で消費する (背面中も進む)。duration 到達で出の演出を開始し (入りの途中でも移る)、撤去はフックの完了通知後 (core/ADR-0017)。

**理由:** KMP commonMain から呼べる必要があるため、プラットフォーム固有の時間型 (TimeInterval / kotlin.time.Duration) ではなく素の整数 ms が最小公倍数。原典の Duration (ms) とも一致し移行者の直感に合う。

**代替案:**
- **A: 形態ごとのイディオム時間型 (Swift = TimeInterval 秒, Kotlin = Duration)** — 却下: 同じ値が形態で単位違いになり、4ルート Sample のパリティ (cross/ADR-0007) で混乱の種になる。糖衣として将来足すのは互換
- **B: 0 以下を fail-fast (例外)** — 却下: fire-and-forget の同期 show に例外経路を作ると呼び出し側の扱いが重くなる。構成ミス (未登録 VM) とは性質が違う入力値の問題であり、既定への丸め + ログで十分

### Decision 3: 器 ToastContainer — Loading の器の非モーダル派生 (core/ADR-0030 の実装形)

**採用案:**

- iOS: `LoadingContainerViewController` と同構造の `ToastContainerViewController`。ルート View を `PassthroughView` (hitTest で自分と覆い相当を素通しし、すべてのタッチを nil 返し) にする。覆いは持たない (overlay なし)。key window へ `addSubview`
- Android: `LoadingContainer` と同構造の `ToastContainer` (`android.app.Dialog` 派生・全画面透過 Window)。`window.addFlags(FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE)` でタッチとフォーカスを一切奪わない。覆いは持たない
- レイアウトは `DialogLayoutApplier` / `DialogLayoutHost`、演出は `DialogTransitionRunner` をそのまま通す (デフォルト View は既定クロスフェード、カスタムは `ksDialogTransition` 添付 — phase-8 決定事項)

**理由:** core/ADR-0030 の決定そのまま。共有部品の再利用で「配置・演出は Dialog 踏襲」が検証様式 (core/ADR-0009・0016) ごと揃う。

**代替案:** (core/ADR-0030 の Alternatives — OS Toast 委譲・単一共有器 — で棄却済み。ここでは実装形の細部のみ)
- **A: iOS で `isUserInteractionEnabled = false`** — 却下: 将来の opt-in 対話拡張 (ADR-0031 の派生) で全面書き換えになる。hitTest 素通しなら「Toast の面だけ受ける」への変更が局所で済む
- **B: Android で `FLAG_NOT_TOUCHABLE` を使わず touchable region を空にする** — 却下: API が煩雑で、全画面素通しなら単純なフラグ2つで表現でき意図も読める

### Decision 4: 多重管理は ToastCoordinator (core/ADR-0027 同型) が持ち、表示中リストと各表示のタイマーを一元管理する

**採用案:** プロセス内 `ToastCoordinator` を状態の唯一の正とし、全入口 (Native 直接呼び出し・MAUI bridge・KMP gateway) が委譲する。coordinator は表示中 Toast のリスト (起動順) を持ち、各表示ごとに: 器の生成と取り付け → duration タイマー → 出の演出 → 撤去、を独立に進める。Android の Activity 再生成では全表示を起動順のまま新しい resumed Activity へ再取り付けし (演出なし)、残り duration はタイマー継続 (coordinator 側で計時しているため器の作り直しに影響されない)。

**理由:** Loading の `LoadingCoordinator` (単一表示) のリスト版。再取り付け・ホスト監視 (`ResumedActivityTracker`) の機構をそのまま複数枚に広げる形で、新規機構がない。

**代替案:**
- **A: 各 Toast が自分の器とタイマーを自己管理 (coordinator なし)** — 却下: Activity 再生成の再取り付けと「Loading 常時前面」の並べ替えは全表示を知る単一の管理点がないと成立しない。core/ADR-0027 が Loading で同じ結論
- **B: タイマーを器 (View) 側に持たせる** — 却下: Android の器は再生成で使い捨てるため、タイマーが器に紐づくと残り時間が巻き戻る

### Decision 5: 「Loading 常時前面」の実現 — iOS は挿入位置、Android は Loading の演出なし再前面化

**採用案:**

- iOS: Toast の取り付け時、Loading の器 View が同じ host に載っていればその**下**に挿入する (`insertSubview(_:belowSubview:)`)。Loading の取り付けは従来どおり最前面 `addSubview` (後から出た Loading は自然に上)
- Android: Window の重なりは追加順でしか決まらないため、Toast の Window を出した直後に Loading が表示中なら `LoadingCoordinator` へ「演出なし再前面化」を依頼する — 既存の再取り付け機構 (`detachForReattach()` + `attach(playsPresentation = false)`、中身と実効値は同じものを載せ替え) の流用で、Loading の見た目は連続する
- この依頼の向きは Toast → Loading の一方向。Loading 側は Toast の存在を知らない (依存を増やさない)

**理由:** 両 OS とも既存機構の組み合わせだけで成立し、新しい Window 種別や z-order API に手を出さない。Android の再取り付けは回転対応で実証済みの経路 (core/ADR-0026)。

**代替案:**
- **A: Android で Toast の Window type を下位層にする** — 却下: アプリが使える Window type に「アプリ Window より上・Dialog より下」のような都合の良い層はなく、type 変更は権限・OS バージョン差の沼
- **B: Loading 表示中は Toast の表示を保留し、Loading 終了後に出す** — 却下: 「多重起動可・起動順に重なるだけ」(基本要件) に反する。Toast は Loading の背後で出て時を刻むのが要件の素直な解釈
- **C: 前後関係を保証しない (Dialog と同じ core/ADR-0006 の線)** — 却下: オーナー決定 (2026-08-27) で「Loading が最上位」を明示的な順序規則とした

### Decision 6: デフォルト View とToastStyle — Loading の内蔵コンテンツ型紙 (core/ADR-0023) + Toast 固有項目

**採用案:** `ToastDefaultContentView` を Native 2実装の internal として同梱 (iOS: UIView / Android: View)。半透明ダークグレー背景 + 白文字の角丸ピルを**自分で描く** (Toast は覆いを持たないため — core/ADR-0032)。MAUI は `contentProvider` nil = 既定という Loading と同じ表現、KMP は契約のみ commonMain。

`ToastStyle` の項目: 背景色 / 文字色 / フォントサイズ / 角丸半径 / 既定 duration / アプリ既定配置 (`defaultPlacement: DialogPlacement?`)。規律は LoadingStyle と同じ — show 引数にしない・各表示の開始時に読む・KMP commonMain からは設定不可。

配置の優先順は「show 引数 > (カスタム View の) 添付 > ToastStyle のアプリ既定配置 > Toast 契約既定値 (visibleArea 下部中央 + 上方向オフセット)」。契約既定オフセットは論理単位で **80** (Material ボトムナビ 80dp と iOS タブバー 49pt + 余白をカバーする単一値。共通ケース表の値として仕様化)。

**理由:** core/ADR-0032 の決定そのまま。優先順は core/ADR-0015 の既存優先順 (show 引数 > 添付 > 契約既定) の「契約既定」の手前に style 既定を1段挟むだけで、既存の供給機構を変えない。

**代替案:**
- **A: 既定オフセットを OS 別の値にする (iOS 60 / Android 80)** — 却下: レイアウト共通仕様 (core/ADR-0007・0009) は OS 差を挙動に持ち込まない方針。単一値 80 で両 OS の標準ボトムバーを回避できる
- **B: アプリ既定配置を ToastStyle でなく独立プロパティにする** — 却下: 「アプリ全体で一度設定する静的な性質」は style の規律 (各表示開始時に読む) と完全に一致し、受け口を2つに割る理由がない

### Decision 7: Scenario 領域プレフィックスは TS (契約挙動)・既存の検証様式に載せる

**採用案:** 挙動 Scenario は安定 ID `TS-xx` 系 (core/ADR-0016)、レイアウトは共通ケース表 (core/layout-spec/cases.json) に Toast 既定配置のケースを追加 (core/ADR-0009)、公開 API 形状検査 (正・負) を4形態に拡張。

**理由:** Loading (LD 系) と同じ流儀。検証の置き場を増やさない。

**代替案:**
- **A: Toast 専用のテスト様式を新設** — 却下: 既存様式で全て表現でき、様式の増殖は drift の温床

## Risks / Trade-offs

- Android の `FLAG_NOT_TOUCHABLE` な全画面透過 Window は本プロジェクト初の組み合わせ。IME 表示・システムジェスチャ (戻る・ホーム) との干渉は Sample の機能間デモ + 実機確認で検証する (tasks に明記)
- Android の Loading 再前面化 (Decision 5) は再取り付けの流用だが、「Toast 表示のたびに Loading の Window を作り直す」ことになる。高頻度 Toast + Loading 併用でちらつきが出る場合は、再前面化を「Loading 表示中の初回 Toast のみ」に間引く余地がある (挙動契約は変わらない)
- 多重 Toast の同時大量表示 (器 = Window の量産) は想定利用 (数枚) を超えると重い。契約上の上限は設けず、Sample でも常識的な枚数に留める
- デフォルト View のピルの見た目は公開 API 表面 (core/ADR-0028 の負の帰結)。mock 承認 (ui/) を経て固定する

## Migration Plan

新機能追加のみで破壊的変更なし。原典 `IToast.Show<TView>()` からの移行者向けの対応表は phase-9 (docs) の責務。

## Open Questions

なし (phase-8 の論点は全て解消済み。上記 Decision は agenda 決定事項の実装形への落とし込み)

## ADR 候補

なし — 選別3基準に該当する決定は phase-8 の議論で core/ADR-0028〜0032 として起票済み。本 design の Decision 1〜7 はそれらの実装形への適用であり、コード + テストと本ファイルで追える
