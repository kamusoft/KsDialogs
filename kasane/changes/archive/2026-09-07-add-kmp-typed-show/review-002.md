# レビュー結果: add-kmp-typed-show (002 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

前回 (review-001 / second-opinion-code-001) で対応対象とした 8 件はすべて解消していた。とりわけ `registerViewModel` の ObjC 露出は、生成 framework のヘッダを実測して「`ViewModelClass:` を含む宣言 0 件・3 つの registry protocol はメンバゼロのハンドル・インスタンス渡し show と `registry` は存続」まで確認できており、`ObjCApiSurfaceTests` の判定条件も綴り依存から `ViewModelClass:` の有無へ強められている。PB-KT-07 は factory / configure が呼び出し元の実行区間の**中**で呼ばれたことを直接観測する形になり、Dialog に加えて Loading の show / start の 3 経路で固定された。PB-KT-12 は開始ゲートと相互の進行観測で再登録と解決が実際に重なるようになり、Scenario の要求 (未登録の失敗や混ざった状態にならない) を満たしている。

修正が新しい問題を持ち込んだ形跡はない。kmp 151 件全件成功、階層化 metadata compile と consumer 側 (`samples/kmp`) のビルドも通り、正の compile 検査は成功・負の 2 本は期待どおりの診断で失敗した。件数表 (151 = 75 + 76) と負検査本数 (62) も本レビューの実測と一致する。残るのは Suggestion 4 件のみ。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin / Gradle / Python の全ファイル)。特に「公開メンバーの doc コメント」節 — 今回の修正が公開 KDoc を書き直しているため |
| `kasane/handbook/cross/test-execution.md` | テストの実行・結果の報告・完了判定 (kmp の `allTests`・負の compile 検査・Scenario ID 網羅検査・件数表と負検査表の実測更新) |
| `kasane/handbook/cross/sample-parity.md` | `samples/kmp/**` が diff に含まれるため (デモ項目・文言・撮影支援の起動引数の不変を確認) |
| `kasane/handbook/cross/local-development-setup.md` | kmp の composite build と Sample のビルドを回すため |

`kasane/lessons/process.md` (L-001 姉妹面照合 / L-002 互換主張の範囲)、`kasane/lessons/impl.md` (L-001 証跡と説明文の突き合わせ) も読んだ。`kasane/lessons/code-review.md` は存在しないため「指摘しないこと」の制約はなし。関連 ADR は kmp/ADR-0006 (accepted)・kmp/ADR-0002・0003・core/ADR-0019〜0021・0029・0031。deviation.md は存在しない。

## 実行したビルドとテスト

| 実行 | 結果 |
|---|---|
| `cd kmp && ./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL / **151 tests / 0 failures / 0 errors / 0 skipped** (iosSimulatorArm64Test 75 + testAndroidHostTest 76。件数は `ksdialogs-kmp/build/test-results/<ターゲット>/TEST-*.xml` を集計) |
| `cd kmp && ./gradlew :ksdialogs-kmp:compileCommonMainKotlinMetadata :ksdialogs-kmp:compileIosMainKotlinMetadata --rerun-tasks` | BUILD SUCCESSFUL |
| `cd samples/kmp && ./gradlew :shared:compileCommonMainKotlinMetadata :androidApp:assembleDebug --rerun-tasks` | BUILD SUCCESSFUL (133 tasks) |
| `cd kmp && ./gradlew :api-surface-check:compileKotlinIosSimulatorArm64 --rerun-tasks` (正の検査) | BUILD SUCCESSFUL / 警告なし |
| 負の検査 `ksdialogs.negativeCheck.toastRegistration` を単独で | BUILD FAILED / `RejectsToastRegistration.kt:15:24 Unresolved reference 'register'.` 1 件 (表と一致) |
| 負の検査 `ksdialogs.negativeCheck.loadingRegistration` を単独で | BUILD FAILED / `RejectsLoadingRegistration.kt:15:26 Unresolved reference 'register'.` 1 件 (表と一致) |
| `python3 scripts/scenario-id-coverage.py` | 未網羅なし (exit 0) |
| `comment-policy-lint.py --advisory` / `local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` | 禁止 0 件 / 違反 0 件 (詳細は下記) |

`ios/` と `android/` は diff にファイルが 1 つも無いため回帰実行していない (tasks 4.2 の範囲)。

## 前回指摘の解消状況

| 出所 | 指摘 | 状態 | 確認したこと |
|---|---|---|---|
| review-001 🟠 Major | `registerViewModel` が Swift から呼べないまま ObjC 面に露出 | **解消** | `DialogViewRegistry.kt:30-31` / `LoadingViewRegistry.kt:30-31` / `ToastViewRegistry.kt:30-31` に `@OptIn(ExperimentalObjCRefinement::class) @HiddenFromObjC`。生成ヘッダ (`kmp/ksdialogs-kmp/build/bin/iosSimulatorArm64/debugFramework/KsDialogsKmp.framework/Headers/KsDialogsKmp.h`) を実測し、`ViewModelClass:` を含む行は 0 件、`KDKDialogViewRegistry` / `KDKLoadingViewRegistry` / `KDKToastViewRegistry` はいずれも `@required` だけのメンバゼロ protocol。`swift_name("registry")` 3 本と実例渡し show 3 本は残存 |
| review-001 🟡 Minor | 型不一致の失敗メッセージが違反内容を伝えていない | **解消** | `kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/ViewModelFactoryStore.kt:46` に「ViewModel factory は登録キーと同じクラスの ViewModel を返してください。」を追加。3 機能の PB-KT-13 は生成物の型名の部分一致で見ているため通っている |
| review-001 🟡 Minor | Loading だけ VM factory / configure の例外伝播が未検証 | **解消** | `LoadingTypedShowTests.kt:104-139` の PB-KT-06 が factory 例外 × (show / start)・configure 例外 × (show / start) の 4 経路を見て、`shownViewModels` が空・`scopedStartCount` が 0・`actionRan` が false であることまで確認している |
| review-001 🔵 Suggestion | レジストリの持ち主が Dialog と Loading / Toast で非対称な理由の説明がない | **解消** | `LoadingGateway.kt:47-48` / `ToastGateway.kt:26-27` に理由の行コメント |
| review-001 🔵 Suggestion | `ObjCApiSurfaceTests` の検出条件が ObjC 名の綴りに依存 | **解消** | `ObjCApiSurfaceTests.kt:36-47` が「`- (` で始まり `ViewModelClass:` を含む宣言行が 0 本」に置き換わり、綴りの列挙が消えた。ヘッダ取り違え防止の正の assert (実例渡し show・`registry`) は維持 |
| second-opinion 🟠 Major | 公開 KDoc が新しいレジストリ境界と矛盾 | **解消** | 契約 3 本のクラス doc (`KsDialog.kt:8-15` / `KsLoading.kt:8-22` / `KsToast.kt:7-27`)、actual entry 6 本 (`{Dialog,Loading,Toast}.{android,ios}.kt`)、`AndroidDialogGateway.kt` / `AndroidToastGateway.kt` / `IosDialogGateway.kt` / `IosToastGateway.kt`、`SamplePresenter.kt` が「ViewModel factory の表は共有コード側・View factory と表示状態は Native 側・Native の登録とは別勘定」に書き換わっている。相方が挙げた 5 箇所はすべて是正済み (同種の記述の取り残しが 1 件あり、下の Suggestion に挙げた)。書き直したブロックからは ADR ID も落ちている (例: KsToast.kt のクラス doc は `-` 側にのみ 5 件の ADR 参照があり `+` 側に無い) |
| second-opinion 🟠 Major | PB-KT-07 が VM factory の別文脈実行を検出できない | **解消** | `support/ExecutionMarkingDispatcher.kt` が自分の block 実行区間を印で示し、`TypedShowTests.kt:185-207` と `LoadingTypedShowTests.kt:142-183` が factory / configure を「印が立っている間に呼ばれたか」で直接判定する。別 dispatcher へ移せば `withContext` の中断で block から抜けるため印が下りる — 相方が挙げた「factory だけ別 dispatcher」の実装は通らない。`ContinuationInterceptor` の一致確認も併存し、Loading は show / start の 2 経路を順序つきで見ている |
| second-opinion 🟠 Major | PB-KT-12 が再登録と解決の並行実行を成立させていない | **解消 (Scenario の要求に対して)** | `TypedShowTests.kt:222-268` が `startGate` で両者を同時に走り出させ、index 0 で互いの進行 (`reregisteringInProgress` / `resolvingInProgress`) を待ち合わせてから残り 199 回を回す。片方が先に走り切る逐次実行ではこの待ち合わせを抜けられない。デッドロックの余地はない (各側が自分の完了を先に立ててから相手を待つ) |
| second-opinion (降格) | 証跡が `verification/` にあり媒体ホワイトリスト外 | **対応なし (合意済み)** | 置き場は前例どおり `verification/`。`kasane/config.yaml` の `lint.exclude` もこのパスを前提にしているため、再指摘しない |

## 指摘事項

### [🔵 Suggestion] PB-KT-12 の最終アサーションは構造上必ず成立する

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowTests.kt:262-267`

**問題点**: `presented.all { it.message == "登録 ${it.generation}" && it.generation in 0..showCount }` は、`message` と `generation` が同じ factory クロージャの中で対にして作られている以上、どんな生成物でも真になる (Kotlin のクロージャが「別々の登録の値を混ぜる」ことは起こり得ない)。したがってこのテストの実際の検出力は `assertEquals(showCount, gateway.presentedViewModels.size)` (= 解決が 1 度も未登録で倒れなかったこと) に集約されていて、アサーション本文が「混ざらないことを見ている」ように読めるぶん、実際より強い保証があるように見える。並行性の担保 (待ち合わせ) 自体は成立しているので実害はない。

**推奨修正**: 「重なりが実際に起きた」ことを言える軽い主張へ差し替える。例えば `presented.map { it.generation }.distinct()` が 2 件以上あること — 待ち合わせを外すと (逐次実行になると) 落ちうる形になり、待ち合わせが効いていることまで 1 本で固定できる。もしくは、常に真であることを前提に「混ざらないことは型の構造で保証され、ここで見るのは解決の失敗が無いことだけ」と assert のメッセージ側で明記する。

### [🔵 Suggestion] ObjC ヘッダの受け渡し配線が `iosSimulatorArm64Test` にしか入っていない

**該当箇所**: `kmp/ksdialogs-kmp/build.gradle.kts:83-93`、`kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/ObjCApiSurfaceTests.kt:51-56`

**問題点**: `iosX64()` もターゲットとして宣言されており (`build.gradle.kts:45`)、`allTests` には `iosX64Test` が含まれる (arm64 ホストでは SKIPPED、本レビューの実行でも SKIPPED だった)。ヘッダの場所を渡す環境変数の配線は `iosSimulatorArm64Test` にだけ入っているため、Intel ホストで `allTests` を回すと `iosX64Test` 側で `ObjCApiSurfaceTests` が走り、「ヘッダの場所が渡されませんでした」で失敗する。「読めなければ失敗させる」設計自体は正しく、素通りにはならないので安全側の壊れ方ではあるが、公開面の変更と無関係な環境差で赤くなる。

**推奨修正**: 配線を `iosSimulatorArm64Test` / `iosX64Test` の両方 (`withType<KotlinNativeSimulatorTest>`) に掛けるか、環境変数が無いときは「このターゲットは検査対象外」として明示的にスキップする (その場合、arm64 ホストでの実行時に必ず走ることを別の手段で担保する)。

### [🔵 Suggestion] `TypedShowRegistryScopeTests` が Native の共有レジストリに後始末なしで登録する

**該当箇所**: `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowRegistryScopeTests.kt:38`・`:55`・`:75`

**問題点**: PB-KT-11 の 3 本は `jp.kamusoft.ksdialogs.{Dialog,Loading,Toast}ViewRegistry.shared` (プロセス全体で 1 個) に `ConfigurableTest*ViewModel` の ViewModel factory を登録し、テスト後に取り除いていない。Native のレジストリに登録解除の API が無いため今の形が取れる唯一の書き方でもあり、同じ型を使う他のテスト (`AndroidDialogGatewayContractTests` の PB-KT-09 等) はいずれも double 経由で本物のレジストリを見ないため、現状で干渉は起きていない (本レビューの全件実行でも 0 failures)。ただし将来「この型が Native レジストリに未登録であること」を前提にするテストを同じ JVM に足すと、実行順に依存して黙って結果が変わる。

**推奨修正**: 登録に使う ViewModel 型を各テストメソッド専用の型に分ける (この 3 本でしか使わない型を新しく置く) と、共有状態の汚染が型の単位で閉じる。コード上の対応が難しければ、クラス doc に「このテストはプロセス全体の Native レジストリに登録を残す」と 1 行残すだけでも、後続の書き手に届く。

### [🔵 Suggestion] 旧いレジストリ境界の記述が commonTest に 1 件残っている

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/DialogEntryPointTests.kt:9-10`

**問題点**: クラス doc に「実体のレジストリが Native ライブラリ側の1個だけであること自体は、OS ごとの委譲面のテストが受け持つ」とあり、本 change の後の境界と食い違う (ViewModel factory の表は共有コードが持つ)。今回の公開 KDoc の一巡が契約と actual entry を対象にしたためテスト側が残ったもので、公開面ではないので利用者に届く誤りではないが、テストを読む側にとっては何を委譲面のテストへ委ねているのかが読めなくなる。同種の記述が他に残っていないことは `git grep` で確認した (残るのは同じテストの見出し文言だけで、そちらは正しい)。

**推奨修正**: 「View factory の紐付けが Native ライブラリ側の1個だけであること」のように、Native 側に残っているものを名指す形へ 1 行を書き直す。

## 確認して問題がなかった観点

- **足場の凍結**: `git diff HEAD -- kasane/changes/add-kmp-typed-show/proposal.md specs exploration.md` が空。tasks.md の diff はチェックボックス 9 個のみで、本文の書き換えなし。既存の証跡 (review-001 / verify-001 / second-opinion-code-001) にも変更なし
- **虚偽チェック**: tasks 1.1〜4.2 の 9 件すべてに対応する実体を diff とビルド結果で確認した。4.1 が要求する件数表の実測更新 (kmp 行 151 = 75 + 76、実測日 2026-09-07) と 2.4 が要求する負検査表の更新 (`toastRegistration` の期待診断の置き換え・`loadingRegistration` 行の追加・「負の検査 62 本」) はいずれも本レビューの実測と一致する。表の行数 62・kmp 行 12 は `kmp/api-surface-check/build.gradle.kts` の 12 フラグ・12 ソースディレクトリとも一致
- **公開 API 面 (Kotlin 側)**: 新規 public は `LoadingViewRegistry` / `ToastViewRegistry` と各契約への追加メンバのみ。`ViewModelFactoryStore` / `Shared*ViewRegistry` / `GatewayKs*` / `*Gateway` はすべて internal のまま。`explicitApi()` 下でビルドが通っている
- **スナップショット契約** (lessons/inbox `review-check-snapshot-contract-on-accessors`): 解決は `ViewModelFactoryStore.create` の 1 回の `load()` に閉じ、CAS ループは不変 Map の置換のみ。公開アクセサ (`registry`) は登録口しか持たないため、表示中に読み出して破れる経路がない。PB-KT-02 が「進行中の show は最初の産物・次回から新しい産物」を固定している
- **同期ダブルに隠れる非同期境界** (同 `review-check-async-boundary-hidden-by-sync-double`): PB-KT-04 が `CompletableDeferred` で configure の開始と完了を別々に進める形を保っている。今回強化された PB-KT-07 は「実行区間の印」と `ContinuationInterceptor` の 2 軸で、片方だけでは通らない
- **姉妹面の照合** (lessons/process.md L-001): Dialog / Loading / Toast の 3 面で「未登録」「型不一致」「Native 側登録が見えない」「Native への委譲」「呼び出し元文脈での実行」「factory / configure の例外伝播」を読み比べた。前回の唯一の欠け (Loading の例外伝播) が埋まり、今回は 6 観点すべてが 3 面そろっている (Toast は非 suspend のため実行文脈の観点は同期伝播の形で TS-KT-01 が担う)
- **互換主張の範囲** (同 L-002): proposal.md の Impact は「呼ぶ側のソース互換」に限定し、実証手段とカバーしない面 (契約を自前実装する利用者コードへの追加実装) を分けて書いている。`FakeKsDialog` / `FakeKsLoading` / `FakeKsToast` に実際に `registry` と型指定 show の実装が入っており、主張のとおり「自前実装型には追加実装が要る」ことが成果物の側でも示されている
- **KT-88548 まわり** (lessons/inbox `kmp-completion-must-run-hierarchical-metadata-compile`): `IosLoadingGateway` / `IosToastGateway` の `@Throws` は internal 化にあわせて外され、公開契約側の `GatewayKsLoading` / `GatewayKsToast` に移っている。`compileCommonMainKotlinMetadata` / `compileIosMainKotlinMetadata` と consumer 側の `:shared:compileCommonMainKotlinMetadata` をすべて `--rerun-tasks` で通した
- **iOS Sample への波及**: `@HiddenFromObjC` が framework の ObjC 面から `registerViewModel` を落とすが、`samples/kmp/iosApp` の Swift は View factory を Native の Swift パッケージ側 (`Dialog.shared.kmp.register(...)` 等) に登録しており、KMP framework の registry protocol を一切参照していない。したがって今回の修正で iOS Sample のビルドと通しが変わることはなく、`verification/sample-walkthrough` の 12 枚は引き続き有効 (samples の最終更新は review-001 より前で、修正サイクルでは samples に触れていない)
- **証跡** (lessons/impl.md L-001): 12 枚の md5 に重複なし。notes.md の観察結果表・限界の開示 (android の 02・04・05・06 で状態バーが写っていない) は review-001 / verify-001 が実体で確認済みで、本サイクルで証跡・notes とも変更がない
- **Sample パリティ**: `SampleText` に diff なし。デモ項目・文言・安定デモ ID・撮影支援の起動引数は不変。`ModelDialogViewModel` / `CustomToastViewModel` の無引数化 + 可変プロパティ化は共有コード内の生成手段の変更に閉じ、各 OS の View factory 登録 (Native レジストリ) には影響しない
- **コメント規約**: `comment-policy-lint.py` の禁止検出 0 件。advisory 441 件はリポジトリ全体の既存分が大半で、本 change が新しく足したのは `DialogGateway.kt:18` の 1 件のみ。これは `internal interface DialogGateway` のメンバに付いた doc であり、可視性ヒューリスティック (直後の宣言行に非公開修飾子が無い) の偽陽性で、規約本文の「ライブラリ利用者から見える公開メンバー」には当たらない。今回書き直した公開 doc コメントには ADR ID・change 名・デルタスペック用語の混入がなく、ADR ID を持つ新規コメントはすべて internal 宣言か実装側の行コメント (`SamplePresenter.kt` の `init` 内・`GatewayKsLoading.configured` の private doc など)
- **verify-001 との整合**: 対応表の Scenario はすべて引き続き成立する。PB-KT-06 の行に付いていた留保 (「Loading の例外伝播は未検証」) は本サイクルで解消し、PB-KT-07 の行が挙げるヘルパ名 (`CountingDispatcher`) は `ExecutionMarkingDispatcher` に置き換わっている — 検証の結論 (VALID) を覆す変化ではなく、いずれも検出力が上がる方向の差分なので再検証は不要と判断した

## アクションプラン

Critical / Major / Minor なし。以下はいずれも任意で、着手しなくてもこの change の完了を妨げない。

1. **Suggestion** — PB-KT-12 の最終アサーションを「常に真」から「重なりが起きたことを示す主張」へ (`generation` の distinct 件数など)
2. **Suggestion** — ObjC ヘッダの環境変数配線を `iosX64Test` にも掛ける、または環境変数不在時を明示的に対象外にする
3. **Suggestion** — `TypedShowRegistryScopeTests` が Native の共有レジストリに残す登録を、専用 ViewModel 型で閉じるか、クラス doc に 1 行残す
4. **Suggestion** — `DialogEntryPointTests` のクラス doc に残った旧いレジストリ境界の記述を書き直す
