# レビュー結果: add-kmp-typed-show (003 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

review-002 の Suggestion のうちオーナーが対応対象とした 2 件 (`TypedShowRegistryScopeTests` の共有レジストリ汚染 / `DialogEntryPointTests` に残った旧いレジストリ境界の記述) だけを対象とした限定スコープのレビュー。2 件とも推奨修正の意図どおりに解消していた。前者は Native レジストリへ登録する ViewModel 型をこのファイル専用の `private` 型 3 本に分けたことで、共有状態の汚染が型の可視性で構造的に閉じている (後続の書き手が同じ型に触れる経路が言語仕様上ない)。後者は現在の境界を名指す 1 行へ書き直され、同種の記述の残りは `git grep` で 0 件。

PB-KT-11 の 3 本は Scenario の要求 (Native の `registerViewModel` にだけ登録 → 3 機能とも「VM factory 未登録」の `DialogException`) を引き続き固定しており、型の差し替えで検出力は落ちていない。新たな問題 (型名の重複・doc への内部用語の混入・他テストとの干渉) は持ち込まれていない。kmp 151 件全件成功で件数は前回と同一、api-surface-check の正の検査も成功。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin 全ファイル)。今回の 2 件とも doc コメントの新規記述・書き換えを含むため、許容参照・禁止する記述類型・公開メンバー判定の 3 節を照合した |
| `kasane/handbook/cross/test-execution.md` | テストの実行と件数の得方 (kmp の `allTests` と `test-results/<ターゲット>/TEST-*.xml` 集計・api-surface-check・Scenario ID 網羅検査)、および件数表の追随要否 |

`kasane/handbook/cross/sample-parity.md` と `local-development-setup.md` は、今回の diff が `samples/` に触れず composite build の構成も変えないため適用外と判定した (前サイクルからの変更が kmp のテストソース 2 本に閉じている)。`kasane/lessons/process.md` (L-001 / L-002)、`kasane/lessons/impl.md`、および inbox の `review-must-not-accept-weakened-structural-guarantee` を読んだ。`kasane/lessons/code-review.md` は存在しないため「指摘しないこと」の制約はなし。deviation.md は無い。

## 実行したビルドとテスト

| 実行 | 結果 |
|---|---|
| `cd kmp && ./gradlew allTests --rerun-tasks` | BUILD SUCCESSFUL / **151 tests / 0 failures / 0 errors / 0 skipped** (iosSimulatorArm64Test 75 + testAndroidHostTest 76。`ksdialogs-kmp/build/test-results/<ターゲット>/TEST-*.xml` を集計) |
| `cd kmp && ./gradlew :api-surface-check:compileKotlinIosSimulatorArm64 --rerun-tasks` | BUILD SUCCESSFUL (17 tasks) |
| `python3 scripts/scenario-id-coverage.py` | 未網羅なし (exit 0)。PB-KT は 14/14 |
| `python3 scripts/comment-policy-lint.py` / `--advisory` | 禁止 0 件 (検査対象 960 ファイル)。advisory にも今回の 2 ファイルの行は 1 件も現れない |
| `python3 scripts/local-path-lint.py` / `identity-lint.py` | いずれも違反 0 件 (exit 0) |

`iosX64Test` は arm64 ホストのため SKIPPED (review-002 と同じ。未対応の Suggestion に該当するため再指摘しない)。`ios/` `android/` `maui/` は diff にファイルが 1 つも無いため回帰実行していない。件数表 (`kasane/handbook/cross/test-execution.md:24` の kmp 行 = 151 tests / iosSimulatorArm64 75 + androidHostTest 76 / 2026-09-07) は本レビューの実測と一致しており、今回の修正はテストを増減させていないため追随更新は不要。

## 対応した 2 件の解消状況

### 1. `TypedShowRegistryScopeTests` が Native の共有レジストリに後始末なしで登録する — 解消

**該当箇所**: `kmp/ksdialogs-kmp/src/androidHostTest/kotlin/jp/kamusoft/ksdialogs/kmp/TypedShowRegistryScopeTests.kt:39-40`・`:56-57`・`:76-77`・`:92`・`:95`・`:98`

推奨修正の 2 案 (専用型に分ける / クラス doc に 1 行残す) のうち、**強いほうの「専用型に分ける」**が採られている。確認したこと:

- 3 本の PB-KT-11 が `jp.kamusoft.ksdialogs.{Dialog,Loading,Toast}ViewRegistry.shared` に登録する型が、同ファイル末尾で宣言された `private class NativeOnlyDialogViewModel` / `NativeOnlyLoadingViewModel` / `NativeOnlyToastViewModel` (それぞれ `DialogViewModel<Boolean>` / `LoadingViewModel` / `ToastViewModel` を実装) に置き換わった
- `git grep NativeOnly` の結果はこのファイルの 13 行だけ。`private` の top-level 宣言なので、**他のファイルからこの型を参照する経路が言語仕様上存在しない** — 「クラス doc に 1 行残す」案が想定していた「後続の書き手が気づかずに同じ型を使う」事故が、規律ではなく型の可視性で閉じている
- 逆向きの汚染も同時に消えている。旧い形では `ConfigurableTestDialogViewModel` が `AndroidDialogGatewayContractTests:185` や commonTest の `TypedShowTests` でも (共有コード側のレジストリに) 登録される型だったため、いずれかの登録先を取り違えた実装変更が入ると実行順で結果が変わる余地があった。今の形ではそれも成立しない
- **他に `registerViewModel` を Native の `.shared` へ掛けている箇所は無い**。`.shared` を触る残りの 6 箇所 (`KmpViewModelSupplyTests:38`、`AndroidDialogGatewayContractTests:31`・`:161`・`:203`、`AndroidLoadingGatewayContractTests:34`・`:237`) はすべて View factory の `register` かハンドル同一性の `assertSame` で、ViewModel factory 表には触れない。したがってこの change が Native のプロセス全体レジストリの ViewModel factory 表に残す登録は、**このファイル専用の 3 型だけ**になった
- クラス doc (`:17-19`) が理由まで書いている — 「Native 側のレジストリはプロセス全体で 1 個で登録解除の口が無いため、ここで登録する ViewModel 型はこのファイル専用のもの (NativeOnly*) に分け、他のテストが同じ型を Native レジストリで見ないようにする」。ファイル単独で意味が通り、外部文書への依存もない
- 併せてこの書き方は既存のプロジェクト作法と一致する (`InteropBridgeContractTests.kt:26`・`:29` の `RegisteredProbeViewModel` / `UnregisteredProbeViewModel` が同じ「ファイル private の検証専用 VM」の形)

**検出力の照合** (lessons/inbox `review-must-not-accept-weakened-structural-guarantee`): Scenario PB-KT-11 が固定する性質は「Native への `registerViewModel` が共有コードの型指定 show に見えないこと」で、使う型が何であるかに依存しない。差し替え後の型は 3 機能とも本物の共有 VM 契約を実装しており Native レジストリのキーとして通用する (通用しなければコンパイルか登録で落ちる) ため、共有コード側が誤って Native の表を引く実装に変われば、失敗メッセージの `assertEquals` か `assertNull(failure.cause)` のどちらかが必ず落ちる。構造的な保証は弱まっていない。

### 2. 旧いレジストリ境界の記述が commonTest に 1 件残っている — 解消

**該当箇所**: `kmp/ksdialogs-kmp/src/commonTest/kotlin/jp/kamusoft/ksdialogs/kmp/DialogEntryPointTests.kt:9-10`

- 「実体のレジストリが Native ライブラリ側の1個だけであること自体は、OS ごとの委譲面のテストが受け持つ」→「View factory の紐付けが Native ライブラリ側の1個だけであること自体は、OS ごとの委譲面のテストが受け持つ (ViewModel factory の表は共有コードが持つ)」に書き換わっている。推奨修正の文面のとおり、Native 側に残っているものを名指す形になった
- **現在の境界と一致している**。`DialogViewRegistry.kt:9-12` の公開 doc (「View factory の登録は各 OS のネイティブ API で行う… 共有コードから登録できるのは ViewModel factory だけで、その表は共有コードが持つ」) と `Dialog.android.kt:4-8` の入口 doc に同じ割り方が書かれており、3 箇所の記述が揃った
- 「OS ごとの委譲面のテストが受け持つ」という委譲先の主張も実体がある — Android 側は `AndroidDialogGatewayContractTests` の `既定エントリのレジストリは Native ライブラリの共有レジストリを指す` が `assertSame(DialogViewRegistry.shared, handle.native)` で 1 個であることを直接押さえ、iOS 側は `InteropBridgeContractTests` が実 bridge 越しに Swift 側レジストリでの View factory 解決を見ている
- **comment-policy 適合**: 内部用語 (ADR ID・change 名・デルタスペック用語・`review-NNN` 等のローカル通番) の混入なし。「View factory」「ViewModel factory」「委譲面」はいずれもコード上の識別子または一般語で、このファイルだけを読む人に意味が通る。禁止する記述類型 (履歴記述・過去仕様の説明・MUST 等の構文キーワード) にも当たらない。なお両ファイルとも test であり公開メンバーではないため「公開メンバーの doc コメント」節の対象外だが、対象と仮定しても抵触しない
- 同種の記述の取り残しは無い。`grep -rn "実体のレジストリ|レジストリが Native ライブラリ側の1個"` の結果は書き直し後のこの 1 行のみ

## 指摘事項

なし (Critical / Major / Minor / Suggestion いずれも 0 件)。

## 確認して問題がなかった観点

- **足場の凍結**: `git diff HEAD --stat -- kasane/changes/add-kmp-typed-show/` は `tasks.md` 1 ファイルのみで、中身はチェックボックス 9 個の反転だけ (本文の書き換えなし)。`proposal.md` / `exploration.md` / `specs/` に差分なし。既存の証跡 (review-001 / 002 / verify-001 / second-opinion-code-001) にも変更なし
- **修正の範囲**: 前サイクルからの変更はテストソース 2 本 (`TypedShowRegistryScopeTests.kt` / `DialogEntryPointTests.kt`) の doc とテスト用 ViewModel 型に閉じている。公開 API・ビルド構成・本番コードには触れていない (`api-surface-check` の正の検査が引き続き成功することでも裏付けられる)
- **型名の重複**: `NativeOnly*` の 3 型は `kmp/` `samples/` 全体で他に宣言も参照もない。`private` のためモジュール内での衝突も起きない
- **既存テストへの干渉**: `ConfigurableTest*ViewModel` を使う残りのテスト (androidHostTest 3 本 + commonTest 3 ファイル) はすべて自前の `GatewayKs*` インスタンスか `entry.registry` 経由の共有コード側レジストリだけを見ており、今回 Native レジストリから外れた登録に依存していない。151 件全件成功で実測でも確認
- **`TypedShowRegistryScopeTests` の 1 本目** (`共有コードの ViewModel のクラス参照は Native の型のキーと等しくなる`) は `ConfigurableTestDialogViewModel` のままだが、これは KClass の等価性を見るだけで登録を行わないため、クラス doc の「ここで登録する ViewModel 型は…」という限定と矛盾しない。むしろ共有 support 側の型で見るほうが「共有コードの VM が Native の型キーとして通用する」という主張に合う
- **Scenario の固定**: PB-KT-11 の 3 本はいずれも `DialogException` と「ViewModel 型 <型名> の ViewModel factory が登録されていません。」の完全一致、および `assertNull(failure.cause)` (Native の失敗の載せ替えでないこと) を残しており、Scenario の THEN 3 機能ぶんをそのまま押さえている。Scenario ID 網羅検査でも PB-KT 14/14
- **観察 (指摘ではない)**: Loading の PB-KT-11 は `showFailure.cause` の null 検査はあるが `startFailure.cause` の検査はない (`assertEquals(showFailure.message, startFailure.message)` で代替している)。この非対称は今回の修正で入ったものではなく review-002 の対象範囲でも判定済みのため、限定スコープの本レビューでは指摘として立てない。メッセージ一致が Native 経由との区別をほぼ担っているので実害も見当たらない

## アクションプラン

なし。この change の完了を妨げる指摘は無い。未対応の Suggestion 2 件 (PB-KT-12 の最終アサーションの検出力 / `iosX64Test` へのヘッダ配線) はオーナー判断で見送り済みとして、本レビューでは扱っていない。
