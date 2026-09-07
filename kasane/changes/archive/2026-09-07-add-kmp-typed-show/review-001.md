# レビュー結果: add-kmp-typed-show (001 回目)

**日付**: 2026-09-07
**判定**: CHANGES_REQUESTED

## サマリー

設計は kmp/ADR-0006 のとおりに実装されており、「VM factory 表は commonMain・View factory は Native 委譲」「生成 → configure → 既存のインスタンス渡し show」の骨格は 3 機能で揃っている。Loading / Toast に commonMain デコレータを新設して本番の前段を commonTest から検証できる形にした点、`AtomicReference` + CAS でスナップショット解決を実装し並行再登録のテストまで置いた点、負の compile 検査を「レジストリが無い」から「View factory 登録 API が無い」へ正しく作り替えた点は、いずれも spec の要求を過不足なく満たしている。テストは kmp 147 件全件成功、階層化 metadata compile と consumer 側 metadata compile も通り、証跡 12 枚も notes.md の記述どおりだった。

一方で、**公開 ObjC / Swift 面に `registerViewModel` が実用不能なまま露出している**点が Major。型指定 show を `@HiddenFromObjC` で隠した理由 (kmp/ADR-0006 の「`KClass` 引数は Swift から扱えず」) がそのまま登録口にも当てはまるのに、登録口だけが 3 protocol 分そのまま ObjC ヘッダに出ており、Swift からは呼べない (引数の `id<KotlinKClass>` を Swift 側で得る手段が framework に無い) にもかかわらず、一般公開後は取り下げが破壊的変更になる。ほかに Minor 2 件・Suggestion 2 件。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin / Gradle / Python の全ファイル) |
| `kasane/handbook/cross/test-execution.md` | テストの実行・テスト結果の報告・完了判定 (kmp の `allTests`・負の compile 検査・Scenario ID 網羅検査・件数表と負検査表の更新) |
| `kasane/handbook/cross/sample-parity.md` | `samples/kmp/**` を触るため (デモ項目・文言・撮影支援の起動引数が不変であることを確認) |
| `kasane/handbook/cross/local-development-setup.md` | kmp の composite build と Sample のビルドを回すため |

`kasane/lessons/process.md` (L-001 姉妹面照合 / L-002 互換主張の範囲)、`kasane/lessons/impl.md` (L-001 証跡と説明文の突き合わせ) も読んだ。`kasane/lessons/code-review.md` は存在しないため「指摘しないこと」の制約はなし。関連 ADR は kmp/ADR-0006 (accepted)・kmp/ADR-0002・0003・core/ADR-0019〜0021・0029・0031。

## 実行したビルドとテスト

| 実行 | 結果 |
|---|---|
| `cd kmp && ./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL / **147 tests / 0 failures / 0 skipped** (iosSimulatorArm64Test 73 + testAndroidHostTest 74) |
| `cd kmp && ./gradlew :ksdialogs-kmp:testAndroidHostTest :ksdialogs-kmp:compileIosMainKotlinMetadata :ksdialogs-kmp:compileCommonMainKotlinMetadata --rerun-tasks` | BUILD SUCCESSFUL |
| `cd samples/kmp && ./gradlew :shared:compileCommonMainKotlinMetadata :androidApp:assembleDebug --rerun-tasks` | BUILD SUCCESSFUL |
| 負の compile 検査 `toastRegistration` / `loadingRegistration` を 1 本ずつ | いずれも期待どおり BUILD FAILED、診断は `Unresolved reference 'register'.` 各 1 件 |
| `python3 scripts/scenario-id-coverage.py` | 未網羅なし (exit 0) |
| `comment-policy-lint.py` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | 違反 0 件 (comment-policy の advisory 39 件はすべて本 change 以前からの既存分) |

`ios/` と `android/` は diff にファイルが 1 つも無いため回帰実行していない (tasks 4.2 の範囲)。

## 指摘事項

### [🟠 Major] 共有コードのレジストリの `registerViewModel` が Swift から呼べないまま ObjC 面に露出している

**該当箇所**:
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogViewRegistry.kt:26`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingViewRegistry.kt:26`
- `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/ToastViewRegistry.kt:26`

**問題点**: 生成された framework の ObjC ヘッダ (`kmp/ksdialogs-kmp/build/bin/iosSimulatorArm64/debugFramework/KsDialogsKmp.framework/Headers/KsDialogsKmp.h`) を本レビューで確認したところ、3 本とも `registerViewModel` が公開 protocol のメンバとして出ている:

```
- (void)registerViewModelViewModelClass:(id<KDKKotlinKClass>)viewModelClass factory:(id<KDKDialogViewModel> (^)(void))factory   // DialogViewRegistry
- (void)registerViewModelViewModelClass:(id<KDKKotlinKClass>)viewModelClass factory_:(...)  // LoadingViewRegistry
- (void)registerViewModelViewModelClass:(id<KDKKotlinKClass>)viewModelClass factory__:(...) // ToastViewRegistry
```

3 点の実害がある。

1. **呼べない API が公開面に増える**。第 1 引数の `id<KDKKotlinKClass>` を返す API がこの framework に 1 つも無いため (ヘッダ全体で `KotlinKClass` は引数型としてしか現れない)、Swift 側からこのメソッドを呼ぶ手段が無い。kmp/ADR-0006 が型指定 show を `@HiddenFromObjC` にした理由「`KClass` 引数は Swift から扱えず」がそのまま当てはまるのに、登録口だけが例外扱いになっている。
2. **Swift 名が 3 本で不揃いになる** (`factory:` / `factory_:` / `factory__:`)。オーバーロード回避のためのアンダースコア付与で、利用者の補完に意味の取れない項目が並ぶ。
3. **取り下げが後で高くつく**。本リポジトリは一般公開予定 (cross/ADR-0001 が互換 shim を作らない前提を置いているのは公開前だから) で、公開後に ObjC 面のメンバを削るのは破壊的変更になる。隠すなら今が最も安い。

spec は「Swift から見える面 (インスタンス渡し show・`registry` プロパティ) は従来どおり」としか書いておらず、`registerViewModel` の可視性は定めていない。したがって**仕様違反ではない**が、変更前の `DialogViewRegistry` は ObjC 面ではメンバゼロの protocol だったため、これは本 change が新たに増やした公開面である。`registry` プロパティ自体は spec が明示的に見せると決めているので、そこは触らなくてよい。

**推奨修正**: 3 本の `registerViewModel` に型指定 show と同じ `@OptIn(ExperimentalObjCRefinement::class) @HiddenFromObjC` を付ける。3 protocol は変更前の `DialogViewRegistry` と同じ「ObjC からはメンバの無いハンドル」に戻り、Kotlin 側の Scenario (PB-KT-01・TS-KT-02・PB-KT-10) は 1 つも変わらない。あわせて `ObjCApiSurfaceTests` の検査を「ヘッダ中に `ViewModelClass:` を含む宣言行が 0 本」に強めれば、show と登録口の両方が 1 本の検査で固定できる。
「露出したままにする」判断を採る場合は、そう決めた理由 (Swift 側から Kotlin の `KClass` を得る道を将来作る、など) を kmp/ADR-0006 の Consequences か deviation.md に残してほしい — 今の成果物には判断の跡がなく、見落としと区別がつかない。

### [🟡 Minor] 型不一致の失敗メッセージが「何が違反なのか」を伝えていない

**該当箇所**: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/ViewModelFactoryStore.kt:42-46`

**問題点**: 生成物の実行時クラスがキーと違うときのメッセージが「ViewModel 型 A の ViewModel factory が B を生成しました。」で、観測した事実しか述べていない。未登録側 (`:37`「〜の ViewModel factory が登録されていません。」) は違反内容がそのまま読めるのに対し、こちらは初見の利用者が「なぜこれが例外なのか」「どう直すのか」を読み取れない。この失敗は「サブクラスを返す factory を書いた」という踏みやすい構成ミスに対する唯一の手がかりになるため、説明が届く価値が高い。

**推奨修正**: 違反している要求を 1 文足す。例: `"…が ${created::class.displayName} を生成しました。ViewModel factory は登録キーと同じクラスの ViewModel を返してください。"`。既存テスト (`TypedShowTests.kt` / `LoadingTypedShowTests.kt` / `ToastTypedShowTests.kt` の PB-KT-13) は生成物の型名の部分一致で見ているため、そのまま通る。

### [🟡 Minor] Loading の型指定経路だけ VM factory / configure の例外伝播が未検証

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingTypedShowTests.kt` (該当テストなし)、対象実装は `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/LoadingGateway.kt:102-109`

**問題点**: Scenario PB-KT-06 (「VM factory と configure の失敗は提示に進まず伝播する」) は機能を限定していないが、テストがあるのは Dialog (`TypedShowTests.kt` の 2 本、キャンセル含む) と Toast (`ToastTypedShowTests.kt` の TS-KT-01 に configure 例外) だけで、Loading にはない。Loading は `configured()` が `show` と `start` の共通前段になっており、特に `start` は「失敗しても処理 (`action`) を実行しない」という他 2 機能に無い保証を持つ (spec の Requirement 本文「ViewModel factory と [configure] が投げた例外も同じく処理を実行せずに呼び出し元へ伝わる」)。この保証は未登録ケース (PB-KT-05) でしか固定されておらず、`configured()` の呼び出し位置が `gateway.start(...)` の引数評価から動くと黙って崩れる。

**推奨修正**: `LoadingTypedShowTests` に 1 本足す — configure が例外を投げる型指定 `show` と型指定 `start` を呼び、例外がそのまま伝播し `gateway.shownViewModels` が空・`scopedStartCount` が 0 のままであることを見る。

### [🔵 Suggestion] レジストリの持ち主が Dialog と Loading / Toast で非対称なことの説明がない

**該当箇所**: `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/DialogGateway.kt:53-54` と `LoadingGateway.kt:47` / `ToastGateway.kt:26`

**問題点**: `GatewayKsDialog` はレジストリを持たず `gateway.registry` を返すのに対し、`GatewayKsLoading` / `GatewayKsToast` は自前で `Shared*ViewRegistry()` を生成して持つ。理由は「Dialog だけ Android で Native ハンドルの同一性を保つ必要がある」(`AndroidDialogViewRegistry` が `native` を抱える) からで、動作としては正しい (既定エントリはいずれも `object` の単一インスタンスなので PB-KT-01 は成立する)。ただし同じ層に並ぶ 3 本で持ち主が違う理由は、`DialogGateway.kt` と `LoadingGateway.kt` を並べて読んでも分からない。

**推奨修正**: `GatewayKsLoading` / `GatewayKsToast` の `sharedRegistry` に一言添える (例: 「Dialog と違い Native レジストリのハンドルと同一性を合わせる必要がないため、表の持ち主はこの前段になる」)。コード変更は不要。

### [🔵 Suggestion] `ObjCApiSurfaceTests` の検出条件が ObjC 名の綴りに依存している

**該当箇所**: `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/ObjCApiSurfaceTests.kt:36-39`

**問題点**: 「`- (` で始まり `)showViewModelClass:` または `)startViewModelClass:` を含む行が 0 本」という条件で判定している。実際の Kotlin/Native の名前づけとは合っているが、`@HiddenFromObjC` が外れたときに生成される名前が想定と 1 文字でも違えば、検査は失敗せず黙って素通りする。ヘッダを読めないときは失敗させる設計 (`readGeneratedHeader`) や、実例渡し show と `registry` の存在を先に確かめてヘッダ取り違えを防いでいる点は良いので、判定条件だけが弱い。

**推奨修正**: Major を対応して登録口も隠すなら、条件を「`ViewModelClass:` を含む宣言行が 0 本」に置き換えられる (現状ヘッダで `ViewModelClass:` を含むのは登録口 3 本だけ)。露出を残す判断なら、登録口 3 本が存在することを正の assert として足し、それ以外の `ViewModelClass:` 行が無いことを見る形にすると同じ強さになる。

## 確認して問題がなかった観点

- **足場の凍結**: proposal.md / specs / exploration.md に diff なし。tasks.md はチェックボックスのみの更新で、虚偽チェックなし
- **公開 API 面 (Kotlin 側)**: 新規 public は `LoadingViewRegistry` / `ToastViewRegistry` と各契約への追加メンバのみ。`ViewModelFactoryStore` / `Shared*ViewRegistry` / `GatewayKs*` / `*Gateway` はすべて internal で、意図しない public 化はない
- **スナップショット契約と時間差の読み出し口** (lessons/inbox `review-check-snapshot-contract-on-accessors` の観点): 解決は `ViewModelFactoryStore.create` の 1 回の `load()` に閉じており、show 後に呼べる公開アクセサ (`registry`) は登録口しか持たないため、表示中に共有状態を書き換えてから読み出して破れる経路がない。PB-KT-02 が「進行中の show は最初の産物・次回から新しい産物」を実測で固定している
- **同期ダブルに隠れる非同期境界** (同 `review-check-async-boundary-hidden-by-sync-double` の観点): PB-KT-04 が `CompletableDeferred` で configure の「開始」と「完了」を別々に進められる形になっており、即完了ダブルでの素通りにはなっていない。PB-KT-07 も `CountingDispatcher` の dispatch 回数と `ContinuationInterceptor` の 2 軸で「hop していない」を見ており、片方だけでは通らない
- **姉妹面の照合** (lessons/process.md L-001): Loading / Toast / Dialog の 3 面で「未登録」「型不一致」「Native 側登録が見えない」「Native への委譲経路」を機能ごとに読み比べ、実装・テストとも欠けは Minor 3 の Loading の例外伝播 1 点だけだった
- **互換主張の範囲** (同 L-002): proposal.md の Impact は「呼ぶ側のソース互換」と限定し、実証手段 (既存テストと正の検査の無改変通過) とカバーしない面 (契約を自前実装する利用者コードへの追加実装) を分けて書いている。Fake / test double 側に実際に追加実装が入っていることも確認した。契約への抽象メンバー追加そのものは Dialog の先例 (core/ADR-0019〜0021) と同型で、オーナー判断で確定済みの形のため指摘しない (lessons/inbox `additive-contract-member-follows-accepted-precedent`)
- **KT-88548 まわり**: `IosLoadingGateway` / `IosToastGateway` の `@Throws` 回避コメントが「internal なので書かない」に置き換わり、`@Throws` は公開契約を実装する `GatewayKsLoading` / `GatewayKsToast` 側に移っている。`compileIosMainKotlinMetadata` / `compileCommonMainKotlinMetadata` / consumer 側の `:shared:compileCommonMainKotlinMetadata` をすべて `--rerun-tasks` で走らせて成功を確認した (lessons/inbox `kmp-completion-must-run-hierarchical-metadata-compile`)。生成ヘッダで実例渡し show の error 引数が残っていることも確認済み
- **handbook の追随** (lessons/inbox `test-count-table-must-follow-in-change`): 件数表の kmp 行 (147 = 73 + 74) と負検査の本数 (62) が本レビューの実測と一致。負検査表の `toastRegistration` の期待診断の置き換えと `loadingRegistration` の追加も、1 本ずつ実行して診断まで一致することを確認した
- **Sample パリティ**: `SampleText` に diff なし、デモ項目 14 件・文言・安定デモ ID・撮影支援の起動引数はいずれも不変。`ModelDialogViewModel` / `CustomToastViewModel` の無引数化は共有コード内の生成手段の変更に閉じており、各 OS の View factory (`::ModelDialogCardView` 等) の登録は Native レジストリのままで影響なし
- **証跡** (lessons/impl.md L-001): 12 枚の md5 がすべて相異。notes.md の観察結果表の各行が指す実体を抜き取りで開き (android-01 / 03 / 04・ios-05)、文言・進捗率 50%・`結果: 完了`・2 枚の重なりが記述どおりであることを確認した。「android の 02・04・05・06 で状態バーが写っていない」という限界の開示も実体と一致している
- **コメント規約**: 新規に書かれた公開 doc コメントに ADR ID・change 名・デルタスペック用語の混入なし。ADR ID を持つコメントはすべて internal 宣言か実装側の行コメント。advisory 39 件は本 change 以前からの既存分のみ

## アクションプラン

1. **Major** — `registerViewModel` 3 本の ObjC 露出をどうするか決める。隠すなら `@HiddenFromObjC` を付けて `ObjCApiSurfaceTests` の条件を `ViewModelClass:` 0 本に強める (Suggestion 2 も同時に解消)。残すなら理由を kmp/ADR-0006 の Consequences か deviation.md に記録する
2. **Minor** — `ViewModelFactoryStore` の型不一致メッセージに「登録キーと同じクラスを返してください」を足す
3. **Minor** — `LoadingTypedShowTests` に configure 例外の伝播 (show / start、`action` 未実行) を 1 本足す
4. **Suggestion** — `GatewayKsLoading` / `GatewayKsToast` の `sharedRegistry` に、Dialog と持ち主が違う理由の 1 行を添える
