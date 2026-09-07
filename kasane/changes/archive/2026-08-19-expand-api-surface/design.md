# Design: expand-api-surface

改訂履歴: 2026-08-17 相方スペックレビュー (second-opinion-spec-001) の採用指摘を反映 — 公開 API 宣言表 (Decision 6)・Compose 配布単位の確定 (Decision 7、オーナー判断)・ホストのライフサイクル契約 (Decision 8)・KMP show のキャンセル設計 (Decision 9)・機械面の可視性の言い換え (Decision 4 追記)。
2026-08-19 add-layout-spec 完了分の反映 — SwiftUI / Compose 添付 DSL の実装方式 (Decision 10・11、実現可能性プローブ 2026-08-18 の結果を採用)・宣言表への DSL 追加 (Decision 6)。
2026-08-19 相方スペックレビュー (second-opinion-spec-002) の採用指摘を反映 — インライン / Swift 直接 show への placement 引数追加 (Decision 6、供給契約との整合)・SwiftUI preference 解決の上限と終了状態の規定 (Decision 10)。

## Context

phase-5-2 で確定した API 表面の決定群 (core/ADR-0011・0012・0013、kmp/ADR-0004) の実装設計。方針レベルは ADR で確定済みのため、本 design は実装形の決定に絞る。

## Goals / Non-Goals

- Goals: 両 View 技術の登録・インライン show・bool 既定・Swift 向け KMP 型付き面を、既存 API 非破壊で導入する
- Non-Goals: proposal の Non-Goals に同じ

## Decisions

### Decision 1: 宣言的 UI のホスティングは内部の型消去表現に収束させる (ADR-0011 の実装形)

**採用案:** 各 Native の registry 内部表現を「(vm, notifier) → 提示可能コンテンツ」の1本に統一し、従来 View 系 factory はそのまま、宣言的 UI factory は登録時にホスティングラッパへ変換して同じ内部表現に落とす。提示器 (container) は内部表現だけを扱う。ホストの所有・破棄・サイズの契約は Decision 8。
**理由:** 提示器・結果経路・レイアウト適用を1系統に保ち、技術数 × 経路数の組み合わせ爆発を防ぐ。
**代替案:**
- **A: 提示器を技術別に2系統持つ** — 却下。show・結果・レイアウトの全経路が2倍になり、共通仕様テストの器も割れる
- **B: 宣言的 UI を利用者側で包ませる (interop 丸投げ)** — 却下済み (ADR-0011)

### Decision 2: Android の Compose 系 API は別名 (`registerCompose` / `showCompose`) とする

**採用案:** `@Composable` ラムダを取る API は登録・インライン show とも別名にする (`registerCompose` / `showCompose`)。
**理由:** Kotlin では `@Composable` 付き関数型と通常関数型のオーバーロードが呼び出し側の型推論で衝突しやすい。登録だけ別名にしてインラインを同名にする非対称は混乱のもとなので、Compose 系は一貫して別名とする (相方レビュー指摘 #1 の採用)。
**代替案:**
- **A: 同名オーバーロード** — 却下。呼び出し側でラムダに `@Composable` を明示しないと解決が曖昧になるケースがある
- **B: sealed ラッパ引数で1本化** — 却下済み (ADR-0011 の代替案)

### Decision 3: Swift 面は同名オーバーロード + result ラベルで構成する

**採用案:** iOS Native / KMP 向け Swift 面とも、登録・show は同名で (a) UIView 版 (b) SwiftUI 版 (`@ViewBuilder`) を提供し、KMP 面のカスタム結果型は `result:` ラベル引数 (省略 = Bool) で表す。
**理由:** Swift はクロージャ戻り値型・`some View` によるオーバーロード解決が安定しており、別名に割る理由がない。
**代替案:**
- **A: Kotlin に合わせて別名 (registerSwiftUI 等)** — 却下。Swift 側に衝突の実害がなく、名前の増殖だけが残る

### Decision 4: 型不一致エラーは show 側で throw する型付きエラーとし、機械面は「ABI 公開・利用者非公開」とする

**採用案:** 登録申告型と実結果の不一致は、KMP 向け Swift 面の show が型付きエラー (`resultTypeMismatch`: 期待型・実際型を保持) を throw する。内部印 (interop の marker) は公開面に出さない。`KsDialogsInteropBridge` は **Swift の access level は public のまま維持する** — `@objc public` は KMP cinterop がリンクするための ABI 面であり、internal 化すると KMP 側のリンクが成立しない。利用者向けでないことは、ドキュメントコメント (「KMP cinterop 委譲専用。アプリコードから直接使用しない」) と、利用者向け API を同じ入口 (Dialog 名前空間) に揃えることで表現する。
**理由:** ABI 要件と「内部化」の混同は実装を壊す (相方レビュー指摘 #6 の採用)。検証は「KMP iOS Sample が公開 API のみで完結する」ことで行う。
**代替案:**
- **A: cancelled に丸める** — 却下。プログラミングエラーが利用者キャンセルと区別できない
- **B: Swift access level を internal へ下げる** — 却下。KMP cinterop のリンクが成立しなくなる

### Decision 5: インライン show は registry を経由しない一時 factory 実行とする

**採用案:** インライン show は渡された factory をその場で内部表現に変換して提示する。レジストリへの登録・削除は一切行わない。よって (a) 同じ VM 型の既存登録はインライン show の前後で不変 (b) 同じ VM 型の並行インライン show はそれぞれの factory / notifier / 結果が独立する — この2点を契約 Scenario にする (相方レビュー指摘 #7 の採用)。
**理由:** 一時登録方式はキー衝突と削除タイミングの問題を持ち込むだけ。
**代替案:**
- **A: 一時登録 + show + 登録解除** — 却下。同一 VM 型の並行 show や既存登録との衝突が発生する

### Decision 6: 公開 API 宣言表 (spec 凍結対象)

各形態の追加 API を宣言レベルで確定する (名前の微修正は deviation として記録)。新 API は契約 interface / protocol (core/ADR-0002 の契約 + 既定 singleton 両対応) に追加する:

**iOS (Swift)** — 既存 protocol / registry に追加:

```swift
// 登録 (SwiftUI 版追加)
func register<VM: DialogViewModel, Content: View>(
    _ viewModelType: VM.Type,
    @ViewBuilder factory: @escaping @MainActor @Sendable (VM, DialogNotifier<VM.Result>) -> Content)
// インライン show (UIView 版 / SwiftUI 版)。placement は登録済み show と同じ意味
// (添付 placement のオブジェクトまるごと置換上書き、core/ADR-0015)。省略形は既存 show と同じ
// extension 供給パターン
func show<VM: DialogViewModel>(
    _ viewModel: VM,
    placement: DialogPlacement?,
    factory: @escaping @MainActor @Sendable (VM, DialogNotifier<VM.Result>) -> UIView
) async throws -> DialogResult<VM.Result>
func show<VM: DialogViewModel, Content: View>(
    _ viewModel: VM,
    placement: DialogPlacement?,
    @ViewBuilder factory: @escaping @MainActor @Sendable (VM, DialogNotifier<VM.Result>) -> Content
) async throws -> DialogResult<VM.Result>
// bool 既定 (core/ADR-0012): protocol DialogViewModel { associatedtype Result: Sendable = Bool }
// 添付 DSL (core/ADR-0015。名前は ADR で確定済み)
extension View {
    public func ksDialogOptions(_ options: DialogOptions) -> some View
    public func ksDialogPlacement(_ placement: DialogPlacement) -> some View
}
```

**Android (Kotlin)** — 既存 interface に追加 + Compose 系は ksdialogs-compose モジュールの拡張関数:

```kotlin
// bool 既定の顔 (ksdialogs 本体)
typealias SimpleDialogViewModel = DialogViewModel<Boolean>   // 名前は仮 (deviation 可)
// インライン show (ksdialogs 本体)。placement は登録済み show と同じ意味 (既定 null)
suspend fun <R, VM : DialogViewModel<R>> show(
    viewModel: VM, placement: DialogPlacement? = null,
    factory: Context.(VM, DialogNotifier<R>) -> View): DialogResult<R>
// Compose 系 (ksdialogs-compose モジュール、拡張関数)
fun <R, VM : DialogViewModel<R>> DialogViewRegistry.registerCompose(
    viewModelClass: KClass<VM>, content: @Composable (VM, DialogNotifier<R>) -> Unit)
suspend fun <R, VM : DialogViewModel<R>> KsDialogs.showCompose(
    viewModel: VM, placement: DialogPlacement? = null,
    content: @Composable (VM, DialogNotifier<R>) -> Unit): DialogResult<R>
// 添付 DSL (core/ADR-0015。ksdialogs-compose モジュール、composable 冒頭で宣言)
@Composable
fun KsDialogAttributes(options: DialogOptions? = null, placement: DialogPlacement? = null)
```

**MAUI (C#)** — 既存 interface に追加:

```csharp
// bool 既定
public interface IDialogViewModel : IDialogViewModel<bool> { }
void Register<TViewModel>(Func<TViewModel, DialogNotifier<bool>, View> factory)
    where TViewModel : IDialogViewModel;
// インライン show。placement は登録済み ShowAsync と同じ意味 (既定 null)
Task<DialogResult<TResult>> ShowAsync<TViewModel, TResult>(
    TViewModel viewModel, Func<TViewModel, DialogNotifier<TResult>, View> factory,
    DialogPlacement? placement = null)
    where TViewModel : IDialogViewModel<TResult>;
Task<DialogResult<bool>> ShowAsync<TViewModel>(
    TViewModel viewModel, Func<TViewModel, DialogNotifier<bool>, View> factory,
    DialogPlacement? placement = null)
    where TViewModel : IDialogViewModel;
```

**KMP 向け Swift 面** — Swift パッケージの新公開型 (名前は仮):

```swift
public enum KsDialogsKmpError: Error {
    case notRegistered(viewModelType: String)
    case resultTypeMismatch(expected: String, actual: String)
}
// 登録 (result 省略 = Bool。UIView 版 / SwiftUI 版 × result 有無 = 4形)
public func register<VM: AnyObject>(_ viewModelClass: VM.Type,
    factory: @escaping @MainActor (VM, KmpDialogNotifier<Bool>) -> UIView)
public func register<VM: AnyObject, R>(_ viewModelClass: VM.Type, result: R.Type,
    factory: @escaping @MainActor (VM, KmpDialogNotifier<R>) -> UIView)
// (SwiftUI 版は同形で @ViewBuilder factory: ... -> Content)
// Swift からの型付き show (result 省略 = Bool)。placement は登録済み show と同じ意味 (既定 nil)
public func show<VM: AnyObject>(
    _ viewModel: VM, placement: DialogPlacement? = nil) async throws -> DialogResult<Bool>
public func show<VM: AnyObject, R>(
    _ viewModel: VM, result: R.Type, placement: DialogPlacement? = nil) async throws -> DialogResult<R>
```

**理由:** 宣言レベルまで決めないと互換性のない複数実装が同じ Scenario を満たせる (相方レビュー指摘 #1 の採用)。各形態の spec にこの宣言表への compile Scenario を置く。

### Decision 7: Compose 依存は別モジュール `ksdialogs-compose` に分離する (オーナー判断 2026-08-17)

**採用案:** Android の Compose 系 API (registerCompose / showCompose とホスティング) は新モジュール `ksdialogs-compose` (Maven: `jp.kamusoft:ksdialogs-compose`、`ksdialogs` に依存) に置く。`ksdialogs` 本体は Compose に依存しない。Compose でコンテンツを書く消費者だけがこのモジュールを追加する。KMP androidMain は本体のみに依存し続ける (Compose 登録はアプリ層で行う)。
**理由:** View 系だけ使う消費者 — 特に MAUI Android がバインディング経由で ksdialogs を取り込む経路 — に compose-ui の推移的依存を持ち込まない。
**代替案:**
- **A: 本体 `ksdialogs` に同梱** — 却下。全 Android 消費者 (MAUI 含む) に compose-ui が推移する
**影響:** 配布物が1つ増えるため、cross/ADR-0008 (標準3チャネル) への追記を蒸留時の申し送りとする。バージョンは lockstep (cross/ADR-0009) に含める。

### Decision 8: 宣言的 UI ホストの所有・破棄・サイズ契約

**採用案:**
- **iOS**: SwiftUI コンテンツは `UIHostingController` で包み、提示コンテナの **child view controller として containment に組み込む** (appearance lifecycle を通す)。ダイアログの全閉鎖経路 (completed / cancelled / 呼び出し元キャンセル / コンテナ破棄) で child から外して解放する
- **Android**: `ComposeView` は `LifecycleOwner` / `SavedStateRegistryOwner` が伝播する View tree に載せ (Dialog ウィンドウの decor に owner を設定)、全閉鎖経路で composition を dispose する
- **サイズ**: 宣言的 UI 経由のコンテンツも add-layout-spec のレイアウト規則・ケース表適合の対象 (ホストの intrinsic size がケース表の contentSize として振る舞うこと)
**理由:** 「破棄も同一」の意味が Scenario に落ちておらず、hosting controller の保持や composition の dispose 漏れを受け入れテストが検出できない (相方レビュー指摘 #4 の採用)。
**代替案:**
- **A: UIHostingController.view だけを既存コンテナに載せる** — 却下。controller が保持されず appearance lifecycle も通らない (Apple のガイダンスに反する)

### Decision 9: KMP Swift show のキャンセルは bridge の show ハンドル経由で閉鎖する

**採用案:** 機械面に show 単位のハンドル (キャンセル操作) を追加し、Swift 面の show は呼び出し元 Task のキャンセル時にハンドル経由で当該ダイアログだけを閉じ、結果を cancelled でちょうど1回確定する。
**理由:** core 契約 (concepts/core/api/result-notification-semantics.md) は呼び出し元キャンセルでの閉鎖と cancelled 確定を要求するが、現行 KMP iOS gateway は「キャンセルされても表示が残る」実装であり、completion の async 化だけでは契約違反が残る (相方レビュー指摘 #5 の採用)。
**代替案:**
- **A: キャンセル時も表示を残す (現行 KMP gateway の挙動を追認)** — 却下。core 契約と矛盾し、純 Swift 経路 (キャンセルで閉じる) と挙動が割れる

### Decision 10: SwiftUI 添付 DSL は PreferenceKey 方式とし、初回提示前の到達待ちでスナップショット契約に収束させる

**採用案:** `.ksDialogOptions(...)` / `.ksDialogPlacement(...)` は SwiftUI の PreferenceKey で値をホスト (UIHostingController) へ運ぶ。ホストは表示中 window への接続 + `layoutIfNeeded()` の中で値を読み取り、初回ネイティブレイアウトパス完了時点の値をスナップショットとして採用する (core/ADR-0015 の契約点)。preference の値型は `Equatable & Sendable` とする (Swift 6 の `onPreferenceChange` 要件)。preference 解決の終了状態は次の3つで閉じる:
- **同期解決 (正常系)**: 初回オンスクリーンレイアウトパスの中で preference が解決される (添付なしのコンテンツも preference が既定値のまま同期解決されるため、待ちは発生せず提示は遅延しない)
- **遅延到達**: 同期解決しなかった場合、次の main runloop サイクルでの**追加レイアウトパス1回を上限**として到達を待ち、契約の「初回ネイティブレイアウトパス完了時点」をその追加パス完了まで繰り下げてから提示する — 提示後の再適用は契約違反のため行わない
- **上限到達**: 上限後も値が無ければ添付なしとして契約既定値を採用し、警告ログを残す (add-layout-spec の収束上限警告と同型)
**理由:** 実現可能性プローブ (2026-08-18、判定 YES) で表示中 window 接続 + `layoutIfNeeded()` 中の同期到達を実測済み。ただし同期発火は公式保証のない実装挙動のため、上限付きの到達待ちフォールバックで観察可能な契約 (初回表示に添付値が反映される) を OS バージョン非依存に保証しつつ、無限待ちによる提示停止を構造的に排除する。
**代替案:**
- **A: 同期到達を前提に到達待ちを持たない** — 却下。同期発火は実装挙動であり、崩れた場合に「初回表示に添付値が効かない」契約違反が利用者に漏れる
- **B: 登録 / show の引数で属性を渡す** — 却下済み (core/ADR-0015: 供給はコンテンツ添付、API 引数は作らない)

### Decision 11: Compose 添付 DSL は staticCompositionLocalOf の collector + SideEffect 書き込みとし、ホストは doOnPreDraw で読む

**採用案:** `KsDialogAttributes(options, placement)` は、ホストが `staticCompositionLocalOf` で供給する collector へ `SideEffect` で値を書き込む。ホスト (ComposeView) は `doOnPreDraw` で読み取り、初回レイアウトパス完了時点のスナップショットとして採用する。**Lazy スコープ (LazyColumn 等) の中に書かれると初回 composition で実行されず添付が初回表示に効かないため、この制約を DSL の契約 (ドキュメントコメント) に明記する**。
**理由:** 実現可能性プローブ (2026-08-18、判定 YES) で attach または初回 measure で composition が同期実行される構造保証を実機2台で確認済み。SideEffect は composition 確定後に走るため、投機的 composition の値を拾わない。
**代替案:**
- **A: registerCompose / showCompose の引数で属性を渡す** — 却下済み (core/ADR-0015: 供給はコンテンツ添付、API 引数は作らない)

## Risks / Trade-offs

- SwiftUI / Compose ホストの intrinsic size 測定が add-layout-spec のケース表適合に含まれる — 宣言的 UI 側のサイズ取得は OS 機構依存で、実装時の検証コストが読みにくい。添付 DSL 経由の属性値の適合 (Decision 10・11) も同じ検証範囲に含まれる
- `ksdialogs-compose` の新設で Android の配布物・バージョン整合の管理対象が増える (lockstep に吸収)

## Migration Plan

すべて追加のため移行なし。KMP iOS Sample の機械面直接利用は新 API へ差し替える (利用者向け移行はまだ発生しない — 未公開)。

## Open Questions

なし (Compose 配布単位は Decision 7 で確定済み)

## ADR 候補

- Decision 2 (Compose 系 API の別名): 公開 API 命名 — 将来を制約
- Decision 5 (インライン show の非登録実行): 挙動保証 (レジストリ不干渉) — 覆すコスト高
- Decision 7 (ksdialogs-compose 分離): 配布単位の変更 — 境界を越える (cross/ADR-0008 の追記対象)
- Decision 9 (KMP show のキャンセル閉鎖): 契約適合の実装形 — 覆すコスト高
- Decision 10・11 (添付 DSL の実装方式): 候補としない — 供給契約と DSL 名は core/ADR-0015 (accepted) で確定済みで、本 Decision は各 OS 内部の実装形にとどまるため
