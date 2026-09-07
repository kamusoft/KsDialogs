# Design: add-vertical-slice

## Context

phase-4-vertical-slice のフェーズ議論 (全7論点) で構造判断は確定済みで、ADR 6件 (kmp/0001・0002、maui/0001・0002、cross/0006・0007) が proposed で起票されている。本 design はそれらを実装単位に落とし、フェーズ議論で扱わなかった実装レベルの決定 (API の最小面・エラー方針) を追加で確定する。経緯の一次情報は [phase-4 history.md](../../roadmaps/library-foundation/phases/phase-4-vertical-slice/history.md)、先例調査は [scout-kssettingsview-precedents.md](../../roadmaps/library-foundation/phases/phase-4-vertical-slice/artifacts/scout-kssettingsview-precedents.md)。

契約の意味論は concepts が正: [結果通知のルール](../../concepts/core/api/result-notification-semantics.md) / [多段表示のルール](../../concepts/core/api/multi-display-semantics.md)。

## Goals / Non-Goals

**Goals:** 最小カスタム View ダイアログ1本を4形態 (iOS Native / Android Native / MAUI / KMP) に貫通させ、受け入れ条件 (手動確認 + 結果経路の自動テスト + BuildProbe 削除後のビルド成立) を満たす。phase-1 申し送りの最優先疎通確認 (レジストリキー同一性・Swift async 変換) を実測する。

**Non-Goals:** proposal.md の Non-Goals に同じ。

## Decisions

### Decision 1: 縦串の公開 API は「VM が結果型を宣言する」show 1系統のみ

**採用案:** show は1系統のみとし、結果型は呼び出し側でなく **VM 側が宣言する**。VM は `DialogViewModel<R>` (相当の interface / protocol) に準拠して結果型 `R` を宣言し、show は VM の宣言から `R` を導出して `DialogResult<R>` (completed(値) / cancelled) を返す。レジストリ登録の factory は `(vm, notifier)` を受け取り、**notifier は VM の宣言結果型に固定された「完了(値) / キャンセル」の2操作を持つ部品** — 名前は原典踏襲で **`DialogNotifier`** とする (core/ADR-0002 の命名原則。concepts の「名前未定」をここで確定)。

```kotlin
class BasicDialogViewModel(val message: String) : DialogViewModel<Boolean>
registry.register(BasicDialogViewModel::class) { vm, notifier ->
    BasicDialogView(vm, onOk = { notifier.complete(true) }, onCancel = { notifier.cancel() })
}
val r: DialogResult<Boolean> = dialogs.show(BasicDialogViewModel("..."))  // R は VM から推論
```

Swift は `protocol DialogViewModel { associatedtype Result }`、C# は `IDialogViewModel<TResult>` で同型。移植元の2系統 (`ShowAsync` 系 / `ShowResultAsync` 系) への割り当ては phase-5 で原典 API 表面と突き合わせて確定する。

**将来拡張の担保 (View 直接渡し系統):** 原典の `ShowAsync<TView>` 相当 — View インスタンス/型を直接渡すレジストリ不要の起動モード (Native 利用の主流候補) — は縦串では実装しないが、本 Decision の原則「**結果型は呼び出し側でなく表示物側が宣言する**」の View 版 (`DialogView<R>` 相当に View が準拠し、notifier を受け取る) として非破壊のオーバーロード追加で成立する。DialogNotifier・DialogResult・host 自動解決 (Decision 10)・使い捨てモデルは VM 経路と共通。KMP 形態には適用されない (共有コードは Native View を参照できないため、Native / MAUI の形態別 API)。phase-5 で原典 API 表面との突き合わせ時に追加する。
**理由:** `show<VM, R>(vm)` のように R を呼び出し側が指定する形では、レジストリが VM 型しか見ないため同じ VM を異なる R で show でき、core/ADR-0003 の「報告型と受取型の不一致をコンパイル時に防ぐ」が守れない (second-opinion spec-001 指摘1)。VM 宣言方式なら型不一致がコンパイル時に成立しない。notifier を factory 引数で渡すのは、show 1回ごとに新しい notifier が渡ることで「同一 VM を2枚出したとき1個の notifier を奪い合う」問題を構造的に消すため (Decision 11 と整合)。exactly-once (二重報告 no-op) は DialogNotifier が実装する。
**代替案:**
- **A: 2系統 (簡易版 + 結果つき版) を最初から作る** — 縦串の細さに反し、検証価値を増やさずに表面積だけ増える。却下
- **B: `show<VM, R>(vm)` (R を呼び出し側指定)** — 型不一致がコンパイルを通る。core/ADR-0003 違反。却下 (second-opinion で検出)
- **C: VM が notifier をプロパティで持つ** — VM が可変になり、同一 VM の重ね出しで notifier を奪い合う。却下

### Decision 2: 構成エラー (未登録 VM 型・host 不在) は throw 系チャネルで fail-fast

**採用案:** レジストリに View factory が未登録の VM 型で show した場合、および提示 host が不在の場合 (Decision 10)、結果 (cancelled 等) を返さず失敗する。失敗チャネルは**テストで判定可能な throw 系に統一**する: Swift = `async throws` の throw / Kotlin (Android Native・KMP) = 例外 / MAUI = faulted Task。KMP → Swift 境界では NSError へ変換されて届く。trap (precondition / fatalError) は使わない。エラー時に View は生成・表示されない。
**理由:** 登録漏れ・host 不在は構成ミス (プログラミングエラー) であり、cancelled や no-op に化けると画面上で観察不能になる。「開発時に検出可能なエラー」という表現ではテスト判定できず (second-opinion spec-001 指摘3)、trap はユニットテストから検証困難なため、throw 系に固定する。
**代替案:**
- **A: cancelled を返す** — 構成ミスと利用者操作が区別できなくなり、結果通知ルールの意味論も汚す。却下
- **B: 何も表示せず正常終了 (no-op)** — show が永遠に完了せず呼び出し元が hang する。却下
- **C: trap (precondition / fatalError)** — ユニットテストで検証困難。却下 (second-opinion で検出)

### Decision 3: KMP の Swift async 変換は素の suspend 直接公開 (kmp/ADR-0001)

**採用案:** ADR のとおり。疎通確認で実測する粗 = 戻り型の nullable 化・sealed `DialogResult` の Swift からの見え方・エラーチャネル。受け入れ条件 (正しい型・値で返る) を割ったら自前 `@objc` completion handler ラッパーへフォールバックする。
**理由・代替案:** [kmp/ADR-0001](../../decisions/kmp/0001-swift-interop-plain-suspend.md) を参照 (KMP-NativeCoroutines は公開 API への第三者依存漏れで却下)。

### Decision 4: KMP は契約のみ commonMain、実体は Native lib へ全委譲 (kmp/ADR-0002)

**採用案:** ADR のとおり。commonMain = `interface KsDialogs` + `sealed DialogResult` + レジストリ契約。expect/actual は既定 singleton エントリのみ。androidMain は Android Native lib へ直接委譲、iosMain は iOS Native lib の `@objc` 互換面へ cinterop 委譲。レジストリ実体は OS ごとに Native lib 側1個。
**理由・代替案:** [kmp/ADR-0002](../../decisions/kmp/0002-thin-facade-native-registry.md) を参照。

### Decision 5: MAUI Bridge は使い捨て、MAUI レジストリは C# 層 (maui/ADR-0001)

**採用案:** ADR のとおり。Bridge 表面は show / dismiss の操作 1:1、結果は show ごとの completion 1本。C# レジストリ (VM 型 → MAUI View factory) で解決した MAUI View を platform view に実体化して Native lib へ渡す。MAUI 経由の show は Native レジストリを使わない。**dismiss は Bridge の内部操作であり公開 API にしない** — 利用者から見た閉鎖経路は DialogNotifier の完了/キャンセルのみ (プログラムからの dismiss 公開 API は縦串では作らない。必要になったら phase-5 で結果の意味論込みで設計する)。
**理由・代替案:** [maui/ADR-0001](../../decisions/maui/0001-call-scoped-bridge.md) を参照 (KsSettingsView の Store/Host 構造は寿命モデル不適合で不採用)。

### Decision 6: MAUI facade は net10.0 TFM + internal gateway (maui/ADR-0002)

**採用案:** ADR のとおり。TargetFrameworks は現行 scaffold の `net10.0;net10.0-ios;net10.0-android` を維持し、Bridge 呼び出しは internal gateway 越し。結果経路のユニットテストは素の net10.0 で fake gateway を注入して行う。
**理由・代替案:** [maui/ADR-0002](../../decisions/maui/0002-net10-tfm-gateway-seam.md) を参照。

### Decision 7: MAUI ビルド連携は標準アイテム優先、実測でフォールバック判定

**採用案:** iOS は標準 `XcodeProject` アイテム (+ 必要に応じ `CreateNativeReference=false` + 手動 `NativeReference`)。Android は標準 `AndroidGradleProject` アイテムをまず実測する — KsDialogs の android/ は単一モジュール (`:ksdialogs`) で、先例 KsSettingsView maui/ADR-0006 が実証した制約 (project 依存を持つ複数モジュール構成での SDK init script 衝突) に非該当の見込み。失敗した場合のみ gradlew Exec + `AndroidLibrary` (Bind=true) 束縛にフォールバックする。
**理由:** 標準アイテムが成立すれば SDK 内部ターゲット依存 (先例の負の帰結) を回避できる。実測結果がどちらでも、結果込みで maui ドメインの ADR を実装中に起票する (成立時は KsSettingsView への知見還元も行う)。
**代替案:**
- **A: 最初から gradlew Exec 方式 (先例踏襲)** — 単一モジュールで標準アイテムが成立する可能性を検証せずに内部ターゲット依存を受け入れることになる。却下 (フォールバックとして保持)
- **B: iOS も Exec 方式** — 先例の再調査で「SDK 制約」主張は誤りと判明済み。却下

### Decision 8: Sample は集約 samples/ 4ルート + parity は「デモ項目」単位 (cross/ADR-0006・0007)

**採用案:** ADR のとおり。samples/ios (Local Swift Package) / samples/android (composite build + dependencySubstitution) / samples/maui (facade への ProjectReference 1本) / samples/kmp (shared + androidApp + iosApp)。縦串の初版はデモ項目「Basic Dialog」1個で、4ルート同一メニュー構成・同一文言・共通 SampleTheme の同一 RGBA。KMP Sample の Native View は samples/ios・android と共有せず各自書く。
**理由・代替案:** [cross/ADR-0006](../../decisions/cross/0006-samples-aggregated-consumer-boundary.md) / [cross/ADR-0007](../../decisions/cross/0007-sample-parity-demo-item-unit.md) を参照。

### Decision 9: 共通仕様テストの器の初版はシナリオ表 + 同名テスト規約

**採用案:** 共通仕様シナリオ表 (シナリオ ID + 前提 + 操作 + 期待される観察可能な結果 + OS 差の記録欄) を core の仕様文書として作成し、各 platform はシナリオ ID を冠した同名テストで実装する。UI 操作が要るものは手動確認 + artifacts 記録可。初版シナリオ: MD-a (順閉じ = 期待値あり) / MD-b (下を先に閉じる = 実挙動記録枠) / MD-c (重ね中の外タップ = 期待値あり) / MD-d (Android 戻るボタンのキーボード表示中無視 = 判断待ち枠)。
**理由:** レイアウト属性セット未確定の段階でデータ駆動の器を作ると作り直しになる。シナリオ ID を安定キーとして確立し、B (テストベクター化) への進化余地を残す (phase-4 history 参照)。
**代替案:**
- **A: 共有テストベクター + 各 platform ランナー** — 属性セット確定前ではスキーマ再設計リスク。却下 (phase-5 以降に再検討)
- **B: KMP commonTest に集約** — MAUI・純 Native を覆えない。却下

### Decision 10: 提示 host はライブラリが自動解決、show は任意スレッドから呼べる

**採用案:** show に提示 host の引数を持たせない。iOS は key window の最前面 ViewController (present の連なりの先端) から提示し、Android は `ActivityLifecycleCallbacks` で現在の resumed Activity をライブラリが自動追跡する (利用者の初期化コード不要)。MAUI / KMP は Native 実装への委譲によりこの解決を継承する。show は任意スレッドから呼び出せ、提示処理は内部で UI スレッドへマーシャリングする。アクティブな提示 host が存在しない場合は Decision 2 の throw 系チャネルで即失敗する (キューイングしない)。
**理由:** 「いつでも呼び出せる」が本ライブラリの存在意義であり、原典も host 自動解決である。host 不在時のキューイングは「ダイアログが突然遅れて出る」予測不能挙動になるため fail-fast に倒す (second-opinion spec-001 指摘5)。
**代替案:**
- **A: `show(vm, host:)` の明示引数** — 安全だが「いつでも呼び出せる」の書き味を壊す。必要になれば後から非破壊でオーバーロード追加できる。却下 (縦串では作らない)
- **B: host 不在時はキューイングして次の画面で表示** — 予測不能挙動。却下

### Decision 11: 同一 VM インスタンスの再 show は独立した重ね出しとして扱う

**採用案:** 表示中のダイアログと同じ VM インスタンスで再度 show した場合、独立した新規ダイアログ (重ね出し) として扱う。重複検出・ガードはしない。View も DialogNotifier も show ごとに新規なので、各 show の結果は独立に確定する。二度押し等の UX ガードはアプリ側の責務とする。
**理由:** 使い捨てモデル (core/ADR-0005) では show ごとに View と結果チャネルが新規であり、「同じ VM」は同じ入力データの再利用にすぎない — 特別扱いする状態が存在しない。移植元は Android (即キャンセル相当) と iOS (防御なし) でバラバラであり、本案だけが両 OS 同一挙動をライブラリの意味論として保証できる。concepts の「まだ決めていないこと」をここで確定する (足場凍結の規約上、実装時確定は許されない — second-opinion spec-001 指摘8)。
**代替案:**
- **A: 2回目は即 cancelled を返す (移植元 Android 踏襲)** — platform 由来のガードを契約に持ち込み、「なぜこの挙動か」の説明がつかない。却下
- **B: 実装時に確定 (現状維持)** — 足場凍結と矛盾し、公開契約を実装者判断に委ねることになる。却下

### Decision 12: KMP↔iOS Native の内部 Bridge は型消去輸送、型復元は KMP 側

**採用案:** iOS Native の `@objc` 互換面 (kmp/ADR-0002 の委譲先) は、ジェネリックな `DialogResult<R>` や associated value 付き Swift enum をそのまま運べないため、**非ジェネリックな型消去輸送表現** (completed の値は `Any`/`id` 相当 + completed/cancelled/error の判別) で受け渡す。VM の宣言結果型への復元は KMP 側 (iosMain actual) が担い、復元失敗は Decision 2 のエラーチャネルで報告する。exactly-once (二重報告 no-op) の保証は Native 側 DialogNotifier が持ち、Bridge はそれを素通しする。Swift ネイティブ利用者向けの公開 API (型付き) と内部 Bridge (型消去) は明確に分離し、互換面は internal 扱いの命名にする。
**理由:** ジェネリクスと Swift enum の associated value は ObjC 表現に出せない (second-opinion spec-001 指摘7)。公開 API 面では常に型が結ばれ (Decision 1)、境界内部だけ型消去する二層構造で、型安全の約束と interop 制約を両立する。
**代替案:**
- **A: 「同じ挙動」とだけ規定して輸送表現は実装任せ** — 型復元・エラー変換・exactly-once の責務が曖昧なまま実装に入り、境界で壊れる。却下 (second-opinion で検出)

## Risks / Trade-offs

- **キー同一性が成立しない場合** (commonMain VM の ObjC クラスが Swift lib のレジストリキーとして同一性を保てない): kmp/ADR-0002 の見直しに波及する。縦串で最優先に実測し、崩れたら実装を止めて設計に戻る (これこそ縦串で発見したい故障)
- **Swift async 変換の粗が受け入れ条件を割る場合**: kmp/ADR-0001 のフォールバック (自前 completion ラッパー) を発動。判定基準は「completed / cancelled が正しい型・値で Swift 側に届くか」
- **Android 標準ビルドアイテムが不成立の場合**: gradlew Exec フォールバック (先例実証済み経路) へ。縦串の完了は阻害しない
- **KMP deployment target の非保証フラグ依存** (phase-2 申し送り): cross/ADR-0002 (accepted) の iOS 17 が正。**「KMP の生成物が iOS 17 を deployment target として宣言していること」を受け入れ条件に含める** — 正統手段が見つからず満たせない場合は TODO で流さず、完了不可またはオーナー合意の deviation として扱う (second-opinion spec-001 指摘8)

## Migration Plan

新規実装のみで移行はない。BuildProbe (ios / android / kmp / maui の4ルート、internal) を実 API 実装時に削除する (phase-2 申し送りの消化)。

## Open Questions

なし (second-opinion spec-001 の指摘を受け、通知役の名前と公開の形は Decision 1、再 show の挙動は Decision 11 で提案時点に確定した。deployment target は Risks の受け入れ条件へ移動)

## ADR 候補

- 起票済み (proposed): kmp/ADR-0001・0002、maui/ADR-0001・0002、cross/ADR-0006・0007。実装での検証を経て accepted 昇格を判断
- **実装中に起票**: Decision 7 のビルド連携 (Android 標準アイテムの実測結果込みで maui ドメインへ)
- Decision 1・2・9・10・11・12 は ADR 新規起票なし (契約の具体化・フェーズ足場。Decision 1・10・11 は concepts の結果通知/多段表示ルールへの追随候補として蒸留へ申し送る — 特に Decision 11 は concepts「まだ決めていないこと」の解消)
