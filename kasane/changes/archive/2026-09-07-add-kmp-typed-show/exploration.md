# Exploration: add-kmp-typed-show

## 課題 / 動機

KMP 共有コード (commonMain) の show は Dialog / Loading / Toast ともインスタンス渡しのみで、型指定 show が無い (2026-09-06 コード確認: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/KsDialogs.kt:29` (同日の rename-dialog-contract-singular で `KsDialog.kt` / 契約 `KsDialog` に改名済み)・`KsLoading.kt:44`・`KsToast.kt:52`)。共有コードの `DialogViewRegistry` は同一性確認だけの空のハンドルで、登録の面を持たない (`DialogViewRegistry.kt:12`)。

KMP commonMain への型指定 show の公開は、phase-6 の変更 (add-model-binding-di) の Non-Goals で「需要が出たら別変更で非破壊追加」と明示的に見送られていた (`kasane/changes/archive/2026-08-25-add-model-binding-di/proposal.md:26`)。技術的に不可能とする記録は無い。今回はその需要 (オーナー要望: KMP でも Dialog を型引数で呼びたい。Loading / Toast も揃える) が出た形。

Native 側の Loading / Toast 型指定 show は別 change [add-loading-toast-typed-show](../archive/2026-09-06-add-loading-toast-typed-show/exploration.md) で扱う (2026-09-06 実装・archive 済み、core/ADR-0035)。本 change は Native の型指定経路を使わないため、両者は独立。

## 検討した選択肢 (却下案と理由を含む)

論点 2: VM factory をどこに登録するか

- **A. 各 OS の Native 面に VM factory を登録 (Dialog Native と同じ場所)** — 却下。Android は Native の登録がそのまま効くが、iOS は Swift の KMP 面に「Kotlin の VM を作る factory」の登録口を新設し、さらに型指定 show の引数 (`KClass`) から ObjC クラスを引く橋渡しが必要 (Kotlin/Native に素直な公開 API が無く要検証)。同じ VM の factory を OS ごとに 2 か所書くことになる
- **B. 共有コードのレジストリに VM factory を登録し、型指定 show は共有コード内で VM を生成・configure してから既存のインスタンス渡し show に流す — 採用**。VM は共有コードの Kotlin クラスなので生成手順 (Koin の `get()` 等) も共有コードに書くのが自然で factory は 1 か所。iOS の橋渡し追加が不要。順序保証 (VM 生成 → configure → 報告口紐付け → View → 提示) はインスタンス渡し show に入る前に生成・configure が済むので Native と同じになる

論点 3: KMP の Swift 面 (iOS ホストから共有 VM を型で出す経路 `Dialog.shared.kmp`) も対象にするか

- **A. Swift 面にも型指定 show を出す** — 却下。Swift 面は Native iOS ライブラリ (Swift パッケージ) 側にあり KMP framework に依存しないため、共有コード側の VM factory 表に届かない。Swift 側にも VM factory 登録口が要り、論点 2 で避けた factory の二重化が戻る
- **B. 見送り — 採用**。今回の需要は共有コードからの呼び出し。iOS ホストは Kotlin の VM を Swift で生成してインスタンス渡し show すれば済む。後から非破壊で追加できる

## 決定事項

- 共有コードの型指定 show を Dialog / Loading / Toast の 3 機能に追加する。型は `KClass<VM>` で渡し、configure クロージャ (Dialog / Loading は `suspend`、Toast は同期) を任意で受け取る。動詞は show 1 本 (core/ADR-0020)
- 共有コードのレジストリ (`DialogViewRegistry` と Loading / Toast の相当物) に **VM factory の登録口**を設ける。View factory の登録は引き続き各 OS の Native 面 (kmp/ADR-0002 の View レジストリ委譲は維持)
- VM factory の実体は共有コード側 (commonMain) が持つ。OS ごとの分岐は無い
- 未登録の型指定 show は構成ミスとして `DialogException` 系で失敗する (core/ADR-0021 と同じ扱い)。VM factory / configure の例外は提示に進まず呼び出し元へ伝播する
- **共有コードから型指定 show するなら共有コードで VM factory を登録する**。Android Native の登録口で登録した VM factory は共有コードの型指定 show からは見えない (共有コードのレジストリが正) — 公開面の文書で明記する
- Swift 面 (`Dialog.shared.kmp` 等) の型指定 show は今回対象外
- Native 側の実装 (archive 2026-09-06) との整合 (2026-09-07 照合): 型指定 show / start の引数の並び (Loading: `viewModelClass, placement, configure` / start はさらに `action`、Toast: `viewModelClass, durationMs, placement, configure`) は Android Native の `KsLoading` / `KsToast` と同じにする。失敗の種類は Native が「VM factory 未登録」と「View factory 未登録」を区別するのに対し、共有コードの `DialogException` は説明文だけを運ぶ現行の形のまま (内訳は message で伝える)。Android Native の `LoadingViewRegistry` / `ToastViewRegistry` に `registerViewModel` が実装されたため、「Native 側登録は共有コードから見えない」の検証は Dialog だけでなく 3 機能で行える (spec の PB-KT-11 を拡張)

## ADR 候補

- 作成済み: kmp/ADR-0006 (accepted 2026-09-06, amends kmp/ADR-0002) — 共有コードの型指定 show は commonMain の VM factory レジストリで解決し、View レジストリの Native 委譲は維持する

## 未決の論点

- 共有コードのレジストリ型名: 現行 `DialogViewRegistry` が VM factory も持つようになるため、名前をそのままにするか (Native の `DialogViewRegistry` も View + VM の 2 スロットで同名) — propose で決める
- Loading / Toast の共有コードに現在レジストリのハンドルが無い場合、新設する形 (Dialog と同型に `registry` を生やすか)
- ~~Loading のスコープ形 (start) の型指定版の要否 (Native 側の change と揃える)~~ → 解決 (2026-09-07): Native 側は型指定 start を持って実装済み (core/ADR-0035) のため、共有コードにも型指定 start を含める (spec の LD-KT-02)
- 概念文書の追随: core/api/model-binding-semantics.md の「KMP の共有コードはこの経路を公開しない」「KMP での見え方」節に加え、蒸留 (2026-09-06) で増えた「Dialog / Loading / Toast で同型」の表 (VM factory / configure の例外の扱い — KMP は Toast も同期伝播)・「configure の順序保証」の UI スレッド保証 (KMP は呼び出し元の文脈)・「「非破壊の追加」が指す範囲」(本 change も同じ意味で書く)、core/api/loading-semantics.md「型指定の形と合流」、toast-semantics.md「失敗モデル」の「Dialog / Loading の型指定 show では呼び出し元へ伝播する — 3 機能の違い」の注記 (KMP 行)、kmp/api/dialog-surface.md の「型指定 show は公開しない」節、loading-surface.md / toast-surface.md の「この面に無いもの」(レジストリのハンドル)、ios-host-integration.md (登録するものの一覧)
- kmp/ADR-0002 の Consequences にある「iosMain actual は commonMain VM の ObjC クラスがキーとして同一性を保つことに依存」は本 change でも変わらない (生成後はインスタンス渡し経路) — 検証時に確認

## UI 素材

なし (UI 変更なし)

## 変更級の推奨: M

3 機能の公開 API 追加 (非破壊)、共有コードのレジストリの性格変更 (空のハンドル → VM factory 登録口)、kmp/ADR-0002 の一部改訂、KMP の概念文書 4 本の追随。iOS は実 framework 越しの検証 (KMP Sample) が要る。複数能力にまたがるため S ではない (オーナー確定 2026-09-06)。
