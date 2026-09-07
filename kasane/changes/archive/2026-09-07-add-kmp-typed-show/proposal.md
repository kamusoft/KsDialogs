# Proposal: add-kmp-typed-show

## Why

KMP 共有コード (commonMain) の show は Dialog / Loading / Toast ともインスタンス渡しのみで、ViewModel の型だけを渡す型指定 show (core/ADR-0019〜0021) が無い。共有コードの `DialogViewRegistry` は同一性確認だけの空のハンドルで登録の面を持たず、Loading / Toast にはレジストリのハンドル自体が無い。phase-6 (add-model-binding-di) で「需要が出たら別変更で非破壊追加」と見送られていたものに、利用者 (オーナー) から需要が出た。

決定は探索で確定済み: **VM factory は共有コードのレジストリに登録し、型指定 show は共有コード内で VM を生成・configure してから既存のインスタンス渡し show に流す**。Swift 向け KMP 面の型指定 show は設けない — [kmp/ADR-0006](../../decisions/kmp/0006-common-vm-factory-typed-show.md) (accepted 2026-09-06、amends kmp/ADR-0002)。経緯は [exploration.md](exploration.md)。

## What Changes

- **共有コードのレジストリに VM factory の登録口を設ける** (3 機能)。`DialogViewRegistry` に `registerViewModel(viewModelClass, factory)` を追加し、Loading / Toast には同型の `LoadingViewRegistry` / `ToastViewRegistry` (commonMain の interface) を新設して `KsLoading.registry` / `KsToast.registry` で公開する。名前は Native と同じ (Native のレジストリも View + VM の 2 スロットで同名)。VM factory の実体は commonMain が持ち、OS ごとの分岐は無い。再登録は後勝ち、show 時は呼び出し時点のスナップショット解決。View factory の登録は引き続き各 OS 側 (kmp/ADR-0002 の View レジストリ委譲は維持)
- **共有コードの型指定 show を 3 機能に追加する**。型は `KClass<VM>` で渡し、configure は任意。Dialog: `show(viewModelClass, placement, configure: suspend)` / Loading: `show(viewModelClass, placement, configure: suspend)` と `start(viewModelClass, placement, configure: suspend, action)` / Toast: `show(viewModelClass, durationMs, placement, configure: 同期)`。動詞は show 1 本 (core/ADR-0020)。引数の並びは Android Native の `KsLoading` / `KsToast` の型指定 show / start (core/ADR-0035、2026-09-06 実装済み) と同じ
- **解決の流れ**: commonMain で VM factory から VM を生成 → configure 完了 → 既存のインスタンス渡し show に流す。Native への橋渡し (Android の typealias 委譲・iOS の cinterop 互換面) は変えない。順序保証 (VM 生成 → configure → 報告口紐付け → View → 提示) はインスタンス渡し show に入る前に生成・configure が済むことで Native と同じになる
- **VM factory と configure の実行文脈**: 共有コードは UI スレッドの概念を持たないため、**呼び出し元の文脈 (suspend 関数なら呼び出し元のコルーチン文脈、Toast の同期 show なら呼び出しスレッド) でそのまま実行する**。「利用者が VM を作ってからインスタンス渡し show する」のと同じ形で、Native の「VM factory と configure は UI スレッド」保証は共有コードの面には及ばない (UI スレッドが要る処理は各 OS 側の View factory で行う)
- **失敗契約**: VM factory 未登録は構成ミスとして `DialogException` で失敗し表示は行われない (core/ADR-0021)。VM factory / configure の例外は提示に進まず呼び出し元へ伝播する — **Toast も同じ** (呼び出しスレッドで実行するため同期に伝播できる。Native の Toast が「受理後の失敗」に分類するのは UI スレッド実行が理由で、共有コードにはその制約が無い)。Loading の合流との関係は Native と同じ (生成・configure 後にインスタンス渡しと同じ合流判定)
- **型の一致と Swift からの可視性**: VM factory の生成物の実行時クラスは登録キーと一致していなければならず、違えば型不一致として失敗する (橋渡しを変えないための制約)。型指定 show のオーバーロードは `@HiddenFromObjC` で Swift / ObjC から隠す (共有 Kotlin コード専用。`@Throws` の宣言問題を回避)
- **登録先の規則**: 共有コードから型指定 show するなら VM factory は共有コードで登録する。Android Native の登録口 (`DialogViewRegistry` / `LoadingViewRegistry` / `ToastViewRegistry` の `registerViewModel` — Loading / Toast 分は 2026-09-06 実装済み) で登録した VM factory は共有コードの型指定 show から見えない (共有コードのレジストリが正)。3 機能ともテストで固定する (PB-KT-11)
- **samples/kmp**: 共有コードの `SamplePresenter` の登録経路デモ (`Model Dialog` / `Custom Loading` / `Custom Toast`) を型指定 show に差し替え、VM factory を共有コードで登録する (観察できる挙動は不変)。Native 3 ルートの `Custom Loading` / `Custom Toast` は 2026-09-06 に型指定経路へ差し替え済みで、本 change で 4 ルートの経路がそろう
- **概念文書の追随** (蒸留時): core/api/model-binding-semantics.md の「KMP の共有コードはこの経路を公開しない」「KMP での見え方」に加え、「Dialog / Loading / Toast で同型」の表と「configure の順序保証」(KMP の共有コードは呼び出し元の文脈で実行し、Toast も同期に伝播する — 形態差として 1 行足す)、core/api/loading-semantics.md「型指定の形と合流」/ toast-semantics.md「失敗モデル」の 3 機能の違いの注記 (KMP 行)、kmp/api/dialog-surface.md の「型指定 show は公開しない」、loading-surface.md / toast-surface.md の「この面に無いもの (レジストリのハンドル)」、ios-host-integration.md

影響する能力: kmp-facade (共有コードの公開面)・samples。core 契約 (model-binding-semantics の「VM factory と configure は UI スレッド保証」) に対しては **KMP 共有コードの面だけ呼び出し元の文脈で実行する例外**を導入する — kmp/ADR-0006 に追記済みで、蒸留時に core/api/model-binding-semantics.md の「KMP での見え方」へ書く

## Non-Goals

- **Swift 向け KMP 面 (`Dialog.shared.kmp` 等) の型指定 show** — 探索で見送り (kmp/ADR-0006)。Swift 面は KMP framework に依存せず commonMain の VM factory 表に届かないため、別の登録口が要る。需要が出たら非破壊追加
- **Native の Loading / Toast 型指定 show** — 別 change [add-loading-toast-typed-show](../archive/2026-09-06-add-loading-toast-typed-show/proposal.md) で実装・archive 済み (core/ADR-0035)。本 change は Native の型指定経路を使わない (共有コードで生成してインスタンス渡し show に流す) ため、Native 側の VM factory スロットには依存しない。Native 側のスロットは「Native 側登録は共有コードから見えない」の検証 (PB-KT-11) に 3 機能で使うだけ
- **共有コードからの View factory 登録** — kmp/ADR-0002 のとおり View の型は OS ごとに違うため各 OS 側のまま
- **共有コードでの VM factory の UI スレッド実行保証** — 上記のとおり呼び出し元の文脈で実行する。Main dispatcher への hop を共有コードに持ち込むと Fake 差し替えのテスト容易性と kmp/ADR-0002 の「最薄のファサード」を損なう

## Impact

- **破壊的変更なし (呼ぶ側のソース互換)**: 既存のインスタンス渡し show / start の呼び出しはそのまま通る。実証は既存の commonTest / androidHostTest / iosTest と api-surface-check の正の検査を無改変で通すこと。**カバーしない面**: 型指定 show / start と `registry` は `KsDialog` / `KsLoading` / `KsToast` の抽象メンバーとして増えるため、これらの interface を自前で実装する利用者コード (テストダブル・adapter) には追加実装が要る (本 change でも commonTest の Fake と androidHostTest の test double に足す)。既定実装で吸収する形は採らない — Dialog / Native Loading・Toast の型指定 show と同じ扱い (core/api/model-binding-semantics「「非破壊の追加」が指す範囲」)。一般公開前のため互換 shim は作らない (cross/ADR-0001)
- **波及範囲**: kmp/ksdialogs-kmp の commonMain (契約 3 本・レジストリ interface 3 本・型指定 show の共通前段 (Loading / Toast のデコレータ新設)・VM factory 表)、androidMain / iosMain (gateway を委譲先に組み替え)、commonTest / androidHostTest / iosTest、api-surface-check (正の検査追加と Toast 負検査の置き換え・Loading 負検査の追加)、samples/kmp の共有コード、handbook/cross/test-execution.md の件数表 (kmp 行) と負検査表 (`toastRegistration` 行の置き換えと Loading 行の追加・本数) — 同じ change の中で実測更新する (lessons inbox: test-count-table-must-follow-in-change)
- **リスク**: iOS で VM factory が生成した Kotlin オブジェクトの ObjC クラスが Swift 側レジストリのキーと同一性を保つこと — インスタンス渡しと同じ経路なので kmp/ADR-0002 の既存前提のまま。実 framework 越しの確認は KMP Sample の iOS ルートで行う (`iosSimulatorArm64Test` では実提示が起きない — handbook/cross/test-execution)
- **リスク**: Kotlin の `KClass` を commonMain のレジストリキーにするとき、Android では typealias 先の Native 型と同じ `KClass` になる (同一性の確認をテストで固定)

## 級: M

3 機能の公開 API 追加 (非破壊)、共有コードのレジストリの性格変更 (空のハンドル → VM factory 登録口)、kmp/ADR-0002 の一部改訂 (起票済み)、KMP の概念文書 4 本の追随。iOS は実 framework 越しの検証が要る。複数能力にまたがるため S ではなく、設計判断は ADR で確定済みのため L に届かない (オーナー確定 2026-09-06)。

domain: kmp
