# Design: add-presentation-behavior

## Context

phase-5-3 の決定8件 (agenda 決定事項) を実装形に落とす。土台は core/ADR-0016 (挙動系 Scenario テスト)・core/ADR-0017 (DialogTransition 添付スロット)。多段表示・回転の挙動は実装済みでテスト固定が主、アニメーションは公開 API 追加を伴う新規実装、immersive と KMP キャンセル追随は既存機構の完成。

改訂 (2026-08-21): 相方スペックレビュー (second-opinion-spec-001) の採用指摘を反映 — 状態機械と採用時点 (Decision 10)、結果のラッチと配送・閉鎖原因・フック失敗・形態別キャンセル (Decision 5)、公開 API 表 (Decision 3)、MAUI ブリッジ (Decision 8)、システムバー保証表 (Decision 9)、既存テストの扱いと Scenario ID (Decision 7)。

再改訂 (2026-08-21、second-opinion-spec-002): OS 発消失時のフックのキャンセルと即配送 (Decision 5-2)、終了しないフックの前提条件 + 脱出口 + デバッグ警告 (Decision 5-8、オーナー決定 案C)、Swift フックの throws 化と失敗時保証の弱化 (Decision 1・5-5)、完全シグネチャと duration 特殊値 (Decision 3)、状態×閉鎖原因の遷移表と入力可否 (Decision 10)、PB-MD の1対1再編とメタ Scenario 廃止 (Decision 7)、システムバーの1回採用検証と受け入れ方式表 (Decision 9)。

## Goals / Non-Goals

- Goals: proposal の What Changes 7項目
- Non-Goals: proposal の Non-Goals に同じ (KeyboardListener 移植・AutoRotateForIOS・B案 phase 通知形・キーボード回避・Loading/Toast 適用・Reduce Motion 自動縮退)

## Decisions

### Decision 1: DialogTransition は形態ごとのネイティブ型で、両フックとも省略可能とする

**採用案:** 各形態に `DialogTransition` 型を置く。presentation / dismissal の2フックを持ち、それぞれ省略可能。フック型は統一形 (ADR-0017):

- Swift: `@MainActor (UIView) async throws -> Void` (throws にするのは Kotlin の suspend / MAUI の Task と失敗の表現力を揃え、失敗 Scenario を3形態で同型に構成するため)
- Kotlin: `suspend (View) -> Unit` (Main ディスパッチャで開始)
- MAUI: `Func<VisualElement, Task>` (UI スレッドで開始)

未指定側のフックには器の既定トランジション (fade プリセット相当) が適用される。

**理由:** 片側だけ演出したい用途 (入場だけ凝る等) は自然に多く、両方必須にすると既定再現の定型文を利用者に書かせることになる。

**代替案:**
- **A: 両フック必須** — 却下。未指定側の「既定と同じ」をユーザーが書けない (既定実装は内部)
- **B: 形態横断の共通型を core に置く** — 却下。フックが受け取る View もアニメ API も形態固有で、共通化すると型消去とブリッジだけが増える (ADR-0007 と同じ「観察可能な規則だけ共通化」の構図)

### Decision 2: 添付された DialogTransition は既定トランジションを置き換える。オーバーレイはコンテンツと別レイヤで、器が常時フェード駆動する

**採用案:**

- コンテンツ (ホスト View) の入退場演出は「添付フックがあればフックのみ、なければ既定クロスフェードのみ」の排他
- オーバーレイ (背景の覆い) はコンテンツのホスト View の**兄弟レイヤ** (親子にしない) で、出現/消滅フェードはフックの有無・プリセットの種類と無関係に器が常に駆動する。オーバーレイのフェード**時間はコンテンツのトランジションに揃える**: DialogTransition が `overlayDuration` を持ち、プリセットは自身の duration を自動で設定、カスタムフック構築時は省略可 (省略 = 器の既定 250ms、明示指定も可)。覆いのイージングは器の標準固定 (オーナー要望 2026-08-21: 覆いと本体の時間を揃える)
- フックが受け取るのはコンテンツのホスト View だけで、オーバーレイには触れない
- `none` プリセットは**コンテンツ側のみ無演出 (フック即完了)**。オーバーレイは既定どおりフェードし、器は退出時にオーバーレイのフェード完了を待ってから撤去する (Decision 2 の例外にしない)

**理由:** プリセット (slide 等) が既定フェードと合成されると指定したとおりの動きにならず、予測可能性が壊れる。オーバーレイを別レイヤにするのは、親子にすると覆いのフェードがコンテンツの演出を巻き込み、コンテンツ側の duration / easing が観察できなくなるため (動的モックで実証)。覆いの出現はモーダル化の表現であってコンテンツの演出とは責務が別。

**代替案:**
- **A: 原典同様フックを既定アニメと並走させる** — 却下。原典のフックは「コンテンツ内部の追加演出」だったが、本設計のフックは「入退場そのものの差し替え」であり、合成すると slide 指定でもフェードがかかる
- **B: オーバーレイもフックに渡す** — 却下。覆いの演出まで利用者責務になると、既定の見た目の一貫性 (モーダル感) が崩れやすい。需要が出たら非破壊で拡張できる
- **C: none をオーバーレイ含む全演出の無効化にする** — 却下。覆いが瞬時に消えるとモーダル化の表現が OS 間で割れ、none の用途 (テスト・演出抑制) に覆いの扱いまで混ざる。覆いまで止めたい要求は現時点で無い

### Decision 3: プリセットは fade / slide (4方向) / zoom / none で出発し、none 以外は duration / easing 引数を持つ。公開 API は下表で固定する

**採用案 (公開 API 表):**

| 項目 | iOS (Swift) | Android (Kotlin) | MAUI (C#) |
|---|---|---|---|
| 型 | `public struct DialogTransition` | `public class DialogTransition` | `public sealed class DialogTransition` |
| フック | `presentation` / `dismissal`: `(@MainActor @Sendable (UIView) async throws -> Void)?` | `presentation` / `dismissal`: `(suspend (View) -> Unit)?` | `Presentation` / `Dismissal`: `Func<VisualElement, Task>?` |
| 生成 | `init(presentation:dismissal:overlayDuration:)` 全省略可 (`overlayDuration: TimeInterval? = nil` = 既定) | 主コンストラクタ (`overlayDuration: Duration? = null`)、全省略可 | コンストラクタ (`TimeSpan? overlayDuration = null`)、全省略可 |
| fade | `static func fade(duration: TimeInterval = 0.25, easing: UITimingCurveProvider = .standard)` | `fade(duration: Duration = 250.milliseconds, easing: Interpolator = AccelerateDecelerateInterpolator())` | `Fade(TimeSpan? duration = null, Easing? easing = null)` (既定 250ms / `Easing.CubicInOut`) |
| slide | `static func slide(from: DialogTransitionEdge, duration:, easing:)` | `slide(from: DialogTransitionEdge, duration, easing)` | `Slide(DialogTransitionEdge from, duration, easing)` |
| zoom | `static func zoom(duration:, easing:)` — 開始倍率 0.8 | 同左 | 同左 |
| none | `static func none()` — 引数なし | `none()` | `None()` |
| 方向 | `enum DialogTransitionEdge { top, bottom, leading, trailing }` | `enum class DialogTransitionEdge { TOP, BOTTOM, START, END }` | `enum DialogTransitionEdge { Top, Bottom, Start, End }` |
| 添付 (従来 View 系) | `extension UIView { var ksDialogTransition: DialogTransition? }` | `var View.ksDialogTransition: DialogTransition?` | `Dialog.SetTransition(view, transition)` / `GetTransition` (BindableProperty `Dialog.TransitionProperty`) |
| 添付 (宣言的 UI) | `View.dialogTransition(_:)` modifier | `KsDialogAttributes(options, placement, transition)` の第3引数 | — (MAUI は従来 View 系のみ) |

**完全シグネチャ (コピー可能な形。api-surface-check の期待値):**

```swift
// iOS
public struct DialogTransition: Sendable {
    public typealias Hook = @MainActor @Sendable (UIView) async throws -> Void
    public let presentation: Hook?
    public let dismissal: Hook?
    public let overlayDuration: TimeInterval?
    public init(presentation: Hook? = nil, dismissal: Hook? = nil, overlayDuration: TimeInterval? = nil)
    public static func fade(duration: TimeInterval = 0.25, easing: any UITimingCurveProvider = .standard) -> DialogTransition
    public static func slide(from edge: DialogTransitionEdge, duration: TimeInterval = 0.25, easing: any UITimingCurveProvider = .standard) -> DialogTransition
    public static func zoom(duration: TimeInterval = 0.25, easing: any UITimingCurveProvider = .standard) -> DialogTransition
    public static func none() -> DialogTransition
}
public enum DialogTransitionEdge: Sendable { case top, bottom, leading, trailing }
extension UITimingCurveProvider where Self == UICubicTimingParameters { public static var standard: UICubicTimingParameters { get } }
extension UIView { public var ksDialogTransition: DialogTransition? { get set } }
extension View { public func dialogTransition(_ transition: DialogTransition) -> some View }
```

```kotlin
// Android
public class DialogTransition(
    public val presentation: (suspend (View) -> Unit)? = null,
    public val dismissal: (suspend (View) -> Unit)? = null,
    public val overlayDuration: Duration? = null,
) {
    public companion object {
        public fun fade(duration: Duration = 250.milliseconds, easing: Interpolator = AccelerateDecelerateInterpolator()): DialogTransition
        public fun slide(from: DialogTransitionEdge, duration: Duration = 250.milliseconds, easing: Interpolator = AccelerateDecelerateInterpolator()): DialogTransition
        public fun zoom(duration: Duration = 250.milliseconds, easing: Interpolator = AccelerateDecelerateInterpolator()): DialogTransition
        public fun none(): DialogTransition
    }
}
public enum class DialogTransitionEdge { TOP, BOTTOM, START, END }
public var View.ksDialogTransition: DialogTransition?
@Composable public fun KsDialogAttributes(options: DialogOptions? = null, placement: DialogPlacement? = null, transition: DialogTransition? = null)
```

```csharp
// MAUI
public sealed class DialogTransition
{
    public Func<VisualElement, Task>? Presentation { get; }
    public Func<VisualElement, Task>? Dismissal { get; }
    public TimeSpan? OverlayDuration { get; }
    public DialogTransition(Func<VisualElement, Task>? presentation = null, Func<VisualElement, Task>? dismissal = null, TimeSpan? overlayDuration = null);
    public static DialogTransition Fade(TimeSpan? duration = null, Easing? easing = null);   // 既定 250ms / Easing.CubicInOut
    public static DialogTransition Slide(DialogTransitionEdge from, TimeSpan? duration = null, Easing? easing = null);
    public static DialogTransition Zoom(TimeSpan? duration = null, Easing? easing = null);
    public static DialogTransition None();
}
public enum DialogTransitionEdge { Top, Bottom, Start, End }
public static partial class Dialog
{
    public static readonly BindableProperty TransitionProperty;
    public static DialogTransition? GetTransition(BindableObject view);
    public static void SetTransition(BindableObject view, DialogTransition? value);
}
```

- `.standard` (iOS) は `UICubicTimingParameters(animationCurve: .easeInOut)` を返す公開の既定値
- leading/trailing・START/END は**レイアウト方向に追随** (RTL で反転)。top/bottom は物理方向
- **duration の規則**: 有効範囲外なら (0・負値・NaN・±Infinity・`Duration.INFINITE`、および形態のアニメーション API のミリ秒表現に変換できない大きさ — MAUI は総ミリ秒が `uint.MaxValue` を超える `TimeSpan` (`TimeSpan.MaxValue` を含む) — ) そのプリセットのフックは演出なしで即完了する (例外にしない、丸めもしない)。overlayDuration も同じ規則。MAUI は `TimeSpan` に NaN / 無限大がないため、範囲超過がその代わりになる
- 1つのプリセットは presentation / dismissal の対称ペア (slide-in bottom なら slide-out は bottom へ)。プリセットは `overlayDuration` に自身の duration を設定する (none は既定 250ms)
- **禁止する形**: show 引数で transition を受けない (添付のみ。placement と違い呼び出しごとに変える需要がなく、供給点を増やさない) / DialogOptions にトランジション系プロパティを持たない (ADR-0015) / none に引数を持たせない
- MAUI のプリセットは C# 側で MAUI アニメーション API (`TranslateTo` / `FadeTo` / `ScaleTo`) により実装し、カスタムフックと同じブリッジ経路 (Decision 8) を通す

**理由:** 原典サンプル・README の実用途 (フェード・スライド・スケール) を網羅する最小集合。none は挙動テストと「演出を切りたい」要求の受け皿。API 表を固定するのは api-surface-check (tasks 3.6) の期待値を一意にするため。

**代替案:**
- **A: fade のみで出発** — 却下。プリセットの主目的が「定番を一発で」であり、fade は既定と同じで単独では価値が薄い
- **B: バウンス・スプリング等まで初期提供** — 却下。パラメータ設計が OS 間で揃えにくく需要も未確認。非破壊追加できる (iOS の easing を `UITimingCurveProvider` にしたのは spring を後から受けられるようにするため)
- **C: MAUI のプリセットをネイティブプリセットへ写像する** — 却下。ブリッジに「プリセット記述子」DTO が増え、カスタムフックと2経路になる。C# 実装なら1経路で済む

### Decision 4: 器の入退場アニメは両 OS とも自前駆動に統一し、既定クロスフェードも内部的にプリセット fade と同じ経路に載せる

**採用案:** iOS は `modalTransitionStyle = .crossDissolve` をやめ、`present(animated: false)` / `dismiss(animated: false)` + 器自身が駆動するコンテンツ / オーバーレイのアニメーションに切り替える。Android も `Dialog` のウィンドウアニメに頼らず、show 後 / dismiss 前に器がコンテンツ / オーバーレイのアニメーションを駆動する。既定クロスフェードは内部的に「fade プリセット相当のフック」として同じ実行経路を通す。

**理由:** 退出フックの完了を待ってから閉じる (ADR-0017) には、器がアニメーションのタイミングを握る必要がある。OS のトランジションと自前フックの二重駆動は原典の脆さ (時間の暗黙同期) の再生産になる。既定を同じ経路に載せることで、フック実行経路が常時使われテストされる。

**代替案:**
- **A: iOS は crossDissolve を維持しフック時のみ自前駆動** — 却下。既定とフックで閉鎖経路が2系統になり、退出待ちの検証対象が倍になる
- **B: iOS は UIViewControllerTransitioningDelegate で実装** — 却下。提示機構の委譲 (ADR-0006) に自前 presentation controller を重ねる原典構成への回帰で、多段表示の OS 差の前提が変わるリスクを負う

### Decision 5: 結果は最初の報告でラッチし配送は撤去後とする。退出フックはライブラリ発の閉鎖経路で高々1回実行し、閉鎖原因は内部で識別する。フックの失敗・スレッド・呼び出し元キャンセルの規則を固定する

**採用案:**

1. **ラッチと配送の分離**: 結果 (outcome) は最初の報告 (completed / cancelled) で**不可逆にラッチ**する (既存の結果チャネルの実挙動のまま。以後の報告は no-op)。show の呼び出し元への**配送**は、dismissal フック完了 + オーバーレイのフェード完了 + 器の撤去の**後**。退出中の二重報告・退出中の OS 発器消失でも、ラッチ済み outcome が配送される
2. **実行する閉鎖経路 (ライブラリ発)**: DialogNotifier の報告 (completed / cancelled)・外側タップ・Android 戻るボタン・呼び出し元キャンセル。**実行しない経路 (OS 発の器消失)**: 画面破棄・提示関係の外部からの解除 (ADR-0006 規則6) — 器が既に画面を失っており演出不能。未ラッチなら cancelled をラッチし、**実行中のフック (presentation / dismissal とも) があれば器が所有する Task / Job をキャンセルして参照を切り**、即座に撤去 (ホスト View と宣言的 UI ホストの解放) と配送を行う。この経路だけは Decision 5-1 の「フック完了・オーバーレイ消滅の後」の配送順から除外される (removed へ直行)。MAUI はフックの Task をキャンセルできないため、参照を切って待つのをやめる (Task 自体は利用者コードの責任で完走する)
3. **閉鎖原因の識別**: 公開結果とは別に、内部の閉鎖原因 (`DismissalOrigin`: report / outsideTap / backPress / callerCancellation / hostLost) を結果チャネルに運ぶ。提示層は origin で「フックを実行する閉鎖か」を判定する。公開 API には出さない (結果通知ルール「どの操作でキャンセルされたかの内訳は返さない」を維持)
4. **dismissal フックは高々1回**: 複数の閉鎖信号 (報告直後の外側タップ等) が来ても、最初の信号が退出を開始し、以後の信号は no-op
5. **フックの実行環境と失敗**: フックは UI スレッド (iOS = MainActor / Android = Main / MAUI = UI スレッド) で開始する。フックの throw / fault / cancel は「演出の失敗」であって結果の失敗ではない — presentation フックの失敗でも状態機械は `shown` へ進み報告を受け付ける (フックが途中まで変えた見た目はライブラリが復元しない — 利用者責任)。dismissal フックの失敗は撤去を続行してラッチ済み outcome を配送する。失敗はデバッグログのみで、show を失敗させない
6. **呼び出し元キャンセルの観察結果 (形態別、既存挙動の契約化)**: Swift は show が `.cancelled` を返す (throw しない、既存の `withTaskCancellationHandler` 経路) / Kotlin (Android Native・KMP) はコルーチン規約どおり `CancellationException` が伝播し、内部 outcome は cancelled をラッチ / MAUI は `Task<T>` に CancellationToken を受けないため、呼び出し元キャンセルの経路が存在しない (契約対象外)。Kotlin ではキャンセル済みコルーチンから dismissal フックと撤去を完遂するため、退出処理は `NonCancellable` 相当の文脈で実行する
7. **多段表示との関係**: iOS で下の段を先に閉じたときの上の段の消失は OS 発 (提示関係の解除) なのでフック非実行・即 cancelled (multi-display-semantics.md の差分表どおり)
8. **終了しないフック (前提条件 + 脱出口 + デバッグ警告、オーナー決定 案C)**: フックが有限時間で完了することは**利用者の責務 (契約の前提条件)** であり、タイムアウトは設けない。完了しないフックは契約違反で、その間ダイアログは撤去されず結果も配送されない (症状が画面に見える)。ただし契約違反に当たっても次の2経路では器がフックを見捨てて先に進む — (a) **呼び出し元キャンセル** (Swift / Kotlin): 実行中フックの Task / Job をキャンセルし、完了を待たずに撤去・配送する。(b) **OS 発の器消失**: 5-2 のとおり。MAUI はフックの Task をキャンセルできず show に CancellationToken もないため、脱出口は (b) のみ (文書に明記)。開発支援として、デバッグビルドではフックが一定時間 (既定 5 秒、契約ではなく警告閾値) を超えた時点で警告ログを出す (打ち切りはしない)

**理由:** 「閉じたときの結果を受け取れる」の「閉じた」を画面から消えた状態に揃える。撤去前に配送すると、次のダイアログを出す典型フローで前のダイアログがまだ見えている競合が生まれる。ラッチを最初の報告に置くのは、退出中の二重報告で結果が変わる穴を塞ぐため。origin を内部に留めるのは公開契約を増やさないため。

**代替案:**
- **A: 結果は即配送し、アニメは並走** — 却下。上記の競合と、「show が返った = 消えた」という素朴な期待の裏切り
- **B: フックに安全弁タイムアウトを設ける (上限超過で打ち切り・撤去・配送)** — 却下 (オーナー判断)。宙吊りを構造的に排除できる代わりに、正しい長い演出まで上限で打ち切られる新しい誤動作が生まれ、原典の「250ms を超えると中断」の脆さを値を変えて再生産する。上限値が契約になり API も増える。バグのあるフックは「ダイアログが閉じない」という見える症状で発見できるため、前提条件 + 脱出口 + デバッグ警告 (採用案 5-8) の方が公開ライブラリとして筋が良い
- **B': opt-in タイムアウト引数** — 却下。API 表面が増え、MAUI では打ち切れない非対称が残る。需要が出たら非破壊追加できる
- **C: OS 発消失でもフックを実行する (ADR-0017 初版の文言)** — 却下。器が画面を失った後に演出は物理的に不能で、実行しても観察できない。ADR-0017 の文言を「ライブラリ発の全閉鎖経路」に訂正する (ADR 候補)
- **D: フックの失敗で show を失敗させる** — 却下。利用者は結果 (OK/キャンセル) を既に報告済みで、演出のバグで業務結果が失われるのは本末転倒
- **E: Kotlin でも `.cancelled` を返す (CancellationException を握りつぶす)** — 却下。コルーチン規約違反で、構造化並行性 (親のキャンセル伝播) を壊す

### Decision 6: MAUI の糖衣 (IDialogTransitionAware 等) は見送り、添付プロパティの code-behind 供給のみとする

**採用案:** `Dialog.SetTransition(view, transition)` 添付プロパティのみ提供。interface 実装や仮想メソッド形の糖衣は入れない。

**理由:** クロージャは XAML に書けないため code-behind 供給は必須で、糖衣はそれに対する別表記にすぎない。4形態の添付面が同型 (第3スロット) である一貫性を優先する。

**代替案:**
- **A: IDialogTransitionAware interface** — 却下。添付と interface の2経路になり優先順位規則が必要になる
- **B: 原典同様の仮想メソッド** — 却下。基底クラス継承の強制は KsDialogs の登録モデル (任意の View を factory で返す) と衝突する

### Decision 7: 挙動 Scenario テストは ADR-0016 の同名ミラーとし、既存テストは温存・実ウィンドウが要る挙動だけ instrumented に追加する。全 Scenario に安定 ID を付ける

**採用案:**

- 全 Scenario に `PB-<領域>-<NN>` の安定 ID を付け (TR = トランジション / MD = 多段表示 / WN = ウィンドウ変化 / SB = システムバー / KC = KMP キャンセル / IA / AA / MA = 添付面 / SM = Sample)、両 Native のテスト名に ID を含めて機械的に対応付ける
- **既存テストの分類 (1対1)**: iOS `DialogMultiDisplayTests.swift` / Android `DialogMultiDisplayTests.kt` (JVM) の各4本を**温存し ID を1対1で付与** (改名のみ): 「結果確定で自分のダイアログだけが閉じる」= PB-MD-01 / 「MD-a 上から順に閉じる」= PB-MD-02 / 「MD-c 外側タップは手前のみ」= PB-MD-03 / 「MD-b 下を先に閉じる」= PB-MD-04。PB-MD-04 は**共通 ID** で、THEN だけが multi-display-semantics.md の OS 差分表に従って割れる (同名ミラーの原則を保つ)。Android の JVM テストは契約ロジックの検証として残す
- **メタ Scenario は置かない**: 「全 ID のテストが存在し green」のような自己参照 Scenario は spec に書かず、ID 網羅性はテスト外の検査スクリプト (テスト名から PB-ID を抽出し specs と突合) で検査する (tasks)
- **instrumented に追加する Scenario**: 実ウィンドウ・実提示機構が要るもののみ — 下先閉じの OS 差 (PB-MD-04 の実ウィンドウ版)・器消失 (PB-MD-05)・ウィンドウ変化 (PB-WN)・トランジション順序と完了待ち (PB-TR)・システムバー (PB-SB)
- MAUI / KMP は添付のパススルー検証のみ (ADR-0009 の層別)。アニメーションの見た目 (実際の動き) は実機確認 + 証跡記録で、テストは順序と完了待ちのみ検証する

**理由:** ADR-0016 の初適用。既存テストを作り直すと旧テストとの期待値分裂が起きる。見た目の検査を自動テストから外すのは、描画結果の比較が脆く、契約が「順序と完了」だけだから。

**代替案:**
- **A: 既存テストを捨てて instrumented に全面移行** — 却下。契約ロジックの高速な JVM 検証を失い、CI コストだけ増える
- **B: スクリーンショット比較でアニメも自動検証** — 却下。中間フレームの比較は端末・タイミング依存で flaky の温床

### Decision 8: MAUI のフックはブリッジの完了コールバック型プロトコルで Native が待つ

**採用案:**

- 両 Native のブリッジ (iOS ObjC 互換面 / Android Java 互換面) に、コンテンツ単位のトランジション実行口を追加する: `runPresentation(platformView, completion)` / `runDismissal(platformView, completion)` 相当。Native の器は添付された MAUI トランジションを「この実行口を呼ぶフック」として扱い、completion が呼ばれるまで待つ
- C# アダプタが `Func<VisualElement, Task>` を実行口に変換する: Task 完了 (成功 / fault / cancel のいずれでも) で completion を**ちょうど1回**呼ぶ。fault / cancel はログに残し、completion は成功と同じく呼ぶ (Decision 5-5)
- delegate (C# 側のフック) はブリッジ内容物 (`MauiDialogContent` 相当) が**ダイアログの寿命の間**保持し、撤去時に解放する。GC に回収されないよう Native 側の内容物からの強参照で寿命を揃える
- 実行口の呼び出しは Native の UI スレッドで行い、C# 側は UI スレッド (MAUI の `MainThread`) で Task を開始する

**理由:** 現行ブリッジは View・options・placement と閉鎖通知しか運ばず、Task の完了を Native へ返す面が無い。完了コールバック型にするのは、ObjC / Java 互換面で async を直接表現できないため。

**代替案:**
- **A: MAUI 側で Task 完了を待ってから Native の閉鎖を呼ぶ (Native は待たない)** — 却下。presentation 側は Native が表示を駆動するため MAUI 側で待てず、入退場で機構が割れる。退出でも外側タップ等の Native 発閉鎖に MAUI が割り込めない
- **B: プリセット記述子だけ渡してカスタムフックは MAUI 非対応** — 却下。4形態パリティ (オーナー決定の「非対称は使いにくい」) に反する

### Decision 9: システムバーの引き継ぎは API レベル別の保証表で固定し、採用は show 時の1回とする

**採用案:**

| API レベル | 経路 | 引き継ぐもの |
|---|---|---|
| 30 以上 | `WindowInsetsController`: `rootWindowInsets` の可視判定 → `hide(type)` / `show(type)`、`systemBarsBehavior` のコピー | status / navigation **それぞれ**の可視状態、behavior、appearance (既存) |
| 24〜29 | `systemUiVisibility` のホストからの丸ごとコピー (既存経路。immersive フラグを暗黙に含む) | 可視状態 (フラグ経由)、appearance (既存) |

- 採用時点はダイアログのウィンドウが画面に載った時点の**1回のみ**。表示中にホストがシステムバー状態を変えても追随しない (多段表示中の段ごとの引き継ぎ元は提示先の画面)
- **1回採用の検証**: 表示後にホストの可視状態 (PB-SB-06) と behavior (PB-SB-07) をそれぞれ変え、ダイアログ側が追随しないことを別 Scenario で固定する (片方だけ追随しない実装を排除)
- **受け入れ方式 (SHALL の Scenario を実装者判断で省略させない)**:

| ID | 自動 (instrumented) | 実機証跡 |
|---|---|---|
| PB-SB-01〜03 (API 30+ の可視状態・behavior) | ○ API 30+ エミュレータで自動 | ○ 実機1台 (API 30+) |
| PB-SB-04 (API 24〜29 旧経路) | ○ API 29 エミュレータで自動 | — |
| PB-SB-05 (通常表示) | ○ | — |
| PB-SB-06 / 07 (表示後の可視状態 / behavior の変更に追随しない) | ○ API 30+ (両方とも) | — |

- **iOS**: 裏取り済み — 器は `.overFullScreen` 提示で `modalPresentationCapturesStatusBarAppearance` を上書きしておらず (既定 false)、ステータスバーの制御は提示元が保つ。よって iOS は追加実装なしで「ダイアログ表示がステータスバーの表示状態を変えない」が成立する。これを ios-native の Scenario (PB-IA-03) として固定し、申し送りにしない

**理由:** DialogWindowSystemBars の宣言済み目的「見えを変えない」の完成。新旧経路の非対称 (旧は丸ごとコピーで暗黙継承、新は appearance のみ) を解消する。採用を1回にするのは、追随させると提示先と器の状態同期という二重管理になるため (ADR-0006 の委譲哲学)。

**代替案:**
- **A: ホストの状態変化に追随する** — 却下。二重管理。需要も未確認
- **B: API <30 も InsetsController 互換 (androidx.core) へ寄せる** — 却下。既存経路が既に暗黙継承しており、変更は退行リスクだけ増える

### Decision 10: 器の状態機械を `created → attached → presenting → shown → dismissing → removed` とし、トランジションは属性と同じ時点で採用する

**採用案:**

- **created**: show 呼び出し。ホスト View を生成
- **attached**: ホスト View をウィンドウへ載せレイアウトする。この間コンテンツのホスト View は**非表示** (alpha 0 相当) で、オーバーレイも未表示。初回レイアウトパス完了で options / placement と**同じ時点・同じ規律** (初回の組み立てで実行される位置に宣言、以後の変更は無視) で transition を採用 (スナップショット)。以後 transition インスタンスは退出まで同一
- **presenting**: オーバーレイのフェード開始と同時にホスト View を表示し presentation フックを開始 (フック開始時点でホスト View はウィンドウ上にあり、レイアウト済み・属性凍結済み)。フック完了で shown
- **shown**: 利用者操作・報告を受ける
- **dismissing**: ライブラリ発の閉鎖信号で開始。presentation 中に閉鎖信号が来た場合は **presentation を完走させてから** dismissing へ (直列。中断しない)。dismissal フックとオーバーレイのフェードを並行実行し、両方の完了で removed
- **removed**: 器を撤去し、ラッチ済み outcome を配送
- OS 発の器消失はどの状態からでも removed へ直行 (実行中フックはキャンセル、Decision 5-2)

**状態 × 閉鎖信号の遷移表:**

| 状態 \ 信号 | 報告 (factory 内即報告を含む) | 外側タップ / 戻る | 呼び出し元キャンセル | OS 発の器消失 |
|---|---|---|---|---|
| created / attached | ラッチ → **演出なし**で removed (両フックとも実行しない) | (まだ受け付けない) | ラッチ (cancelled) → 演出なしで removed | removed へ直行 |
| presenting | ラッチ → presentation (フック + オーバーレイ出現) を**完走**させてから dismissing | 同左 (信号として受け付け、直列) | ラッチ → presentation フックをキャンセルし dismissing (脱出口、Decision 5-8) | 実行中フックをキャンセルし removed |
| shown | ラッチ → dismissing | ラッチ → dismissing | ラッチ → dismissing | removed へ直行 |
| dismissing | no-op (ラッチ済み) | **無視** (入力を受け付けない) | 実行中 dismissal フックをキャンセルし removed (脱出口) | 実行中フックをキャンセルし removed |

- **presenting の完了条件**: presentation フック完了 **かつ** オーバーレイの出現フェード完了 (none ではコンテンツ側が即完了するため、オーバーレイ完了まで presenting に留まる)
- **入力可否**: presenting 中はコンテンツとオーバーレイへの入力を受け付ける (外側タップは閉鎖信号として直列処理)。dismissing 中は入力を受け付けない

**理由:** 採用時点を属性と揃えることで、宣言的 UI の添付 (初回レイアウト中に届く) を取りこぼさず、初回フレームのちらつき (フック開始前に最終位置が一瞬見える) も構造的に防げる。presentation 中の閉鎖を直列にするのは、中断時の中間状態からの退出演出が定義しにくく、数百 ms の待ちで済むため。

**代替案:**
- **A: transition を show 呼び出し時点で読む** — 却下。宣言的 UI の添付が初回レイアウト中に届くため取りこぼす
- **B: presentation 中の閉鎖で presentation を中断し即退出** — 却下。中断位置からの退出演出が定義困難で、プリセットの対称ペアが崩れる

## Risks / Trade-offs

- **iOS の提示アニメ自前化 (Decision 4)**: crossDissolve → animated:false + 自前駆動の切り替えで、既存のレイアウトスナップショット (初回パス凍結) やタップ透過のタイミングが変わらないことを既存テスト全通しで確認する
- **配送の遅延 (Decision 5)**: 添付なしでも全 show の完了時刻が既定退出分 (約 250ms) 遅れる。既存の結果通知テスト (正17本等) が即時配送を前提にしていれば更新が要る
- **終了しないフック (Decision 5-8)**: 利用者のバグで「ダイアログが閉じない」症状が出る経路を契約の前提条件として受け入れる。脱出口 (呼び出し元キャンセル / OS 発消失) とデバッグ警告で被害を限定するが、MAUI は脱出口が OS 発消失のみ
- **MAUI ブリッジの ABI 追加 (Decision 8)**: iOS ObjC binding / Android Java binding の再生成を伴う
- **immersive の実機依存**: API レベルごとに挙動差が出る可能性。証跡は 24〜29 / 30+ の両方で取る

## Migration Plan

新規追加が主で API 破壊なし。観察可能な変化は2点 — 既定トランジションの見た目 (OS 既定 → ライブラリクロスフェード) と show の完了時刻 (撤去後配送)。Sample の実機確認で承認を取る。

## Open Questions

- OS の「視覚効果を減らす」(Reduce Motion) 設定の尊重 — 今回は対象外とし、必要なら後続で `none` プリセット相当への自動縮退を検討 (公開前の accessibility 棚卸しで再訪)

## ADR 候補

- **Decision 2 (置き換え + オーバーレイ別レイヤ常時駆動 + none の意味)・Decision 5 (ラッチと配送の分離・閉鎖原因・フック失敗・形態別キャンセル・終了しないフックの前提条件と脱出口)・Decision 10 (状態機械と採用時点)**: ADR-0017 の増分。ADR-0017 本文の「全閉鎖経路」は「ライブラリ発の全閉鎖経路 (OS 発の器消失を除く)」へ訂正済み (2026-08-21、proposed のため本 change で訂正)。蒸留時に 0017 へ統合する
- Decision 3 (プリセット一覧・API 表)・4 (自前駆動)・6 (糖衣見送り)・7 (テスト対象)・8 (MAUI ブリッジ)・9 (システムバー保証表) は実装詳細または既存 ADR (0016/0017) の適用で、単独 ADR 不要
