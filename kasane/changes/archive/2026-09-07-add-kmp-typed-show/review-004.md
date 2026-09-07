# レビュー結果: add-kmp-typed-show (004 回目)

**日付**: 2026-09-07
**判定**: APPROVED

## サマリー

review-003 で APPROVED になった後の **main 合流分だけ**を対象にした限定スコープのレビュー。競合 2 ファイルの両取り・診断文言の英語化・文言 assertion の追随・kmp 以外への波及の 4 点を確認し、いずれも問題を見つけなかった。競合解消は機械的に両側の差分を足した形になっており (ours / theirs と作業ツリーの差分で確認)、競合マーカーの残りはない。英語文言は cross/ADR-0015 の決定と先例 (iOS `DialogError` / Android `DialogException`) の文体に沿い、意味 (違反内容・登録キーの型名・生成物の型名) を落としていない。文言 assertion の追随はどれも検出力を保っており、iosTest の 2 件はむしろ強くなっている。kmp `allTests` は 151 tests / 0 failures で handbook の件数表と一致し、負の検査 2 本・`scenario-id-coverage.py`・`comment-policy-lint.py` もすべて期待どおり。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 英語化した 2 か所の周辺コメント・KDoc は日本語のまま保たれ、禁止参照 (作業文書パス・ローカル通番) の混入なし。`scripts/comment-policy-lint.py` は 965 ファイル検査で禁止 0 件
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・結果を報告するとき・完了を判定するとき) — 全件実行 + 件数確認、負の検査は 1 本ずつ、件数表の追随を本節の規約どおりに確認
- `kasane/decisions/cross/0015-diagnostic-messages-english-only.md` (accepted) — 診断文言の英語固定
- `kasane/lessons/impl.md` / `process.md` は参照。`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)

## 確認結果

### 1. 競合 2 ファイルの両取り

競合マーカーの残存: `grep '<<<<<<<|=======|>>>>>>>'` で 0 件。

**`kasane/handbook/cross/test-execution.md`** — 作業ツリー = ours + theirs を機械的に確認した。

| 由来 | 取り込まれている内容 |
|---|---|
| main (theirs) | ios 277 / android 68 / instrumented 333 (2026-09-07) / maui 155 / maui-android 31 / maui-macios 7 tests 4 suites、実測日の但し書きの書き換え、`iosSimulatorArm64Test` 節の `No screen is available to present the Dialog.` への文言差し替え、31 件・7 件の本文追随 |
| 本 change (ours) | kmp 行 151 tests (iosSimulatorArm64 75 + androidHostTest 76)、負の検査「62 本」(本文 2 か所 + 見出し)、`toastRegistration` 行の期待診断の置き換え (`register` + 理由付き)、`loadingRegistration` 行の追加、「`iosSimulatorArm64Test` は framework の ObjC ヘッダも作る」節 |

負の検査表の行数を数えると 62 行で本文の「62 本」と一致し、kmp の 12 行は `kmp/api-surface-check/src/negativeCheck*/` の 12 ディレクトリと、android の 15 行は同 15 ディレクトリと対応している。ヘッダの「2026-09-07 に全 7 ルートを実測した」は、kmp 行を本作業ツリーで、他 6 行を main で同日に実測したもので、後述 4 のとおり本 change は kmp/ と samples/ 以外を 1 バイトも動かさないため成立している。

**`scripts/scenario-id-coverage.py`** — 除外 ID と理由コメントの双方が両側から残っている。main 側の `DM-IO-03` / `DM-AN-02` / `DM-KM-04` / `DM-MA-05` (4 件 + 理由コメント 3 行)、本 change 側の `PB-KS-01` / `LD-KS-01` / `TS-KS-01` (3 件 + 理由コメント 2 行) が両方あり、ours / theirs との差分はそれぞれ相手側の追加分ちょうどに一致する。実行結果は「未網羅なし」(244/278、除外 34 件)。

### 2. 診断文言 2 件の英語

`kmp/ksdialogs-kmp/src/commonMain/kotlin/jp/kamusoft/ksdialogs/kmp/ViewModelFactoryStore.kt:36,43`。

| 失敗 | 英語文言 | 先例との対応 |
|---|---|---|
| VM factory 未登録 | `No ViewModel factory is registered for ViewModel type X.` | `android/.../DialogException.kt:26` の `ViewModelFactoryNotRegistered` および `ios/.../DialogError.swift:35` と**完全に同一の一文** |
| 型不一致 | `The registered ViewModel factory does not produce ViewModel type X. It produced Y instead. A ViewModel factory must return a ViewModel of the same class as its registration key.` | 第 1 文が `ios/.../DialogError.swift:37` と同一。第 2 文で生成物の型、第 3 文で守るべき規則を足している |

- **意味の保存**: 日本語版が持っていた「違反内容」「登録キーの型名」「生成物の型名」「factory は登録キーと同じクラスを返すべき」の 4 情報がすべて残っている。生成物の型名が残っていることは `PB-KT-13` の assertion (`contains("DerivedTestDialogViewModel")`) が実行時に担保している
- **文体**: 先例と同じく主語を「registry / factory」に置いた平叙文 + 末尾ピリオド。型名は補間で埋め、コード識別子を英訳していない
- **同一文言の衝突について**: 「VM factory 未登録」は Android / iOS の Native 文言と一字一句同じになるが、これは**合流前の日本語時点でも同じ**だった (合流前の `android/.../DialogException.kt` は「ViewModel 型 X の ViewModel factory が登録されていません。」、`ios/.../DialogError.swift:35` も同文)。共有コード由来か Native 由来かの識別は `TypedShowRegistryScopeTests` の `assertNull(failure.cause)` が担っており、英語化で識別力は変わっていない (新たな劣化ではない)
- **日本語リテラルの残存**: `commonMain` / `androidMain` / `iosMain` の `*.kt` をダブルクォート内の日本語で走査して 0 件。main の検証手順 (`kasane/changes/localize-dialog-error-messages/verification/japanese-literal-grep/README.md`) の grep コマンドを合流後の作業ツリーで再実行しても**出力 0 行**で、`DM-KM-04` を含む 4 Scenario が合流後も成立している。本 change が commonMain に新設した他の送出箇所 (`AndroidDialogGateway` / `IosToastGateway` 等) はすべて Native 文言か main 由来の英語定数の素通しで、共有コードが自前で書く文言はこの 2 件だけ (deviation の「2 件」の記述と一致)

### 3. 文言 assertion の追随と `[付随修正]`

追随した 11 の assertion (5 ファイル) すべてで、検出力の低下はない。

| 箇所 | 変更 | 検出力 |
|---|---|---|
| `kmp/.../commonTest/.../TypedShowTests.kt:140` / `LoadingTypedShowTests.kt:92,96` / `ToastTypedShowTests.kt:78` | `contains("ViewModel factory が登録されていません")` → `contains("No ViewModel factory is registered")` | 等価。型不一致の英語文言は `No ViewModel factory is registered` を含まないため、旧日本語断片と同じく 2 つの失敗型を分離できる。これらは Test gateway 経路で Native 文言が混入しない |
| `kmp/.../androidHostTest/.../TypedShowRegistryScopeTests.kt:47,69,84` | `assertEquals` の完全一致文字列を英語へ | 等価 (完全一致のまま)。共有コード由来であることの判定は同居する `assertNull(failure.cause)` が持ち、これは変更されていない |
| `kmp/.../iosTest/.../InteropBridgeContractTests.kt:93` | `contains("登録されていません")` → `contains("No View factory is registered for ViewModel type")` | **強化**。旧断片は VM factory 未登録の文言にも当たったが、新断片は View factory の未登録だけを名指す |
| `kmp/.../iosTest/.../InteropBridgeContractTests.kt:97,112,147` | `contains("提示できる画面がありません")` → `contains("No screen is available to present the Dialog.")` | 強化 (末尾ピリオドまで含む一文の部分一致) |
| `kmp/.../iosTest/.../InteropBridgeContractTests.kt:127` | main 由来の assertion 追加 (Native の未登録の説明が共有コードの `DialogException` へ素通しで届くこと) | 強化 (これまで `println` だけだった箇所に判定が付いた) |

**deviation の `[付随修正]` (`kmp/.../iosTest/.../InteropBridgeContractTests.kt` の `PB-KT-09`)** は ksn-core の同梱条件を満たしている: ① 本 change が新設したテスト (本務で触るファイル) ② 公開 API・スキーマ・ADR に触れない ③ 1 ファイル 1 行 ④ そのテスト自身が担保で `allTests` 全件成功 ⑤ ユーザー判断の分岐なし。記録の形式も `references/delta-spec.md` の `- [付随修正] <箇所>: <何を直したか>。理由: <一言> (YYYY-MM-DD)` に従い、パスもリポジトリ相対。1 件目の deviation (英語化) も、spec が文言の言語を定めていないこと・ADR-0015 に合わせた合意であることが読み取れる形で記録されている。

### 4. kmp 以外への波及

作業ツリーと main (`MERGE_HEAD`) の差分を `ios/` `android/` `maui/` `skills/` に限って取ると**差分 0** (1 バイトも動かしていない)。差があるのは `kmp/` と `samples/`(3 ファイル: `CustomToastViewModel.kt` / `ModelDialogViewModel.kt` / `SamplePresenter.kt`) だけで、いずれも本 change が元から担当していた範囲。`samples/` は ADR-0015 の対象外 (「ライブラリ本体 (samples / テストを除く)」) なので日本語リテラルが残っていて問題ない。

### テスト実行結果

| 検証 | コマンド | 結果 |
|---|---|---|
| kmp 全件 | `cd kmp && ./gradlew allTests --rerun-tasks` | **151 tests / 0 failures / 0 errors / 0 skipped** (iosSimulatorArm64Test 75 + testAndroidHostTest 76。XML 集計)。handbook の件数表と一致 |
| 負の検査 | `./gradlew :api-surface-check:compileKotlinIosSimulatorArm64 -Pksdialogs.negativeCheck.toastRegistration --rerun-tasks` | BUILD FAILED (期待どおり) / `RejectsToastRegistration.kt:15:24 Unresolved reference 'register'.` — 表の期待診断と一致 |
| 負の検査 | 同 `-Pksdialogs.negativeCheck.loadingRegistration` | BUILD FAILED (期待どおり) / `RejectsLoadingRegistration.kt:15:26 Unresolved reference 'register'.` — 表の期待診断と一致 |
| Scenario ID 網羅 | `python3 scripts/scenario-id-coverage.py` | exit 0 / 「未網羅なし」244/278 (除外 34 件) |
| コメント規約 | `python3 scripts/comment-policy-lint.py` | exit 0 / 禁止 0 件 (検査対象 965 ファイル) |
| 日本語リテラル grep | main の検証手順の grep を合流後に再実行 | 出力 0 行 |

`tasks.md` は 4 節すべて `[x]` で未チェック 0 件。上の実行結果と照らして虚偽のチェックは見つからなかった。

## 指摘事項

### [🟡 Minor] merge commit に未 stage の変更と未追跡ファイルが含まれない状態

**該当箇所**: `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropBridgeContractTests.kt` (状態 `MM`) / `kasane/changes/add-kmp-typed-show/deviation.md` (状態 `??`)

**問題点**: レビュー時点で、`ViewModelFactoryStore.kt`・commonTest 3 ファイル・`TypedShowRegistryScopeTests.kt`・`InteropBridgeContractTests.kt` の手直しは**未 stage**、`deviation.md` は**未追跡**。merge の途中でインデックスをそのまま commit すると、これらが merge commit から漏れる。漏れた場合、main には日本語の文言 assertion を持つテストと英語の実装が入り、`iosSimulatorArm64Test` と `testAndroidHostTest` が落ちる (今回の全件成功は作業ツリーの内容に対する結果であって、インデックスの内容に対する結果ではない)。

**推奨修正**: commit の前に該当 6 ファイル + `deviation.md` を `git add` し、`git diff --cached --stat` で merge commit の内容が作業ツリーと一致することを確かめる。git の状態変更は本レビューの担当外のため操作していない。

### [🔵 Suggestion] ADR-0015 が撤回すると決めた handbook の記述が残っている (main 由来・本 change の責務外)

**該当箇所**: `kasane/handbook/cross/user-skill-writing-style.md:47`

**問題点**: 「例外メッセージの列は、実装が日本語リテラルを持つ限り en でも日本語のまま引用する」が残っているが、`kasane/decisions/cross/0015-diagnostic-messages-english-only.md` の Decision は「handbook の『日本語リテラルを引用する』規約は撤回する」と明記している。実装が日本語リテラルを持たなくなったので条件節は空振りするが、規範層に撤回済みの規約が残る。

**推奨修正**: `kasane/changes/localize-dialog-error-messages` はまだ archive されておらず蒸留前なので、本 change ではなくその蒸留 (ksn-distill) で handbook を追随させるのが筋。**本 change を止める理由にはしない**。

### [🔵 Suggestion] Scenario ID `DM-KM-03` が 2 本のテストに付いている (main 由来・本 change の責務外)

**該当箇所**: `kmp/ksdialogs-kmp/src/iosTest/kotlin/jp/kamusoft/ksdialogs/kmp/InteropBridgeContractTests.kt:103` と同 `:118`

**問題点**: main のコミット (`6b689bf`) の時点で `DM-KM-03` が 2 つのテスト名に付いており、合流結果にもそのまま残る。`scenario-id-coverage.py` は ID の出現有無しか見ないため検出されない。片方は `DM-KM-01` 系の互換面検査、もう片方は共有コードの `DialogException` 検査で、対象が異なる。

**推奨修正**: main 側 (localize-dialog-error-messages) の付与ミスであり、本 change の合流で持ち込んだものではない。同じくその蒸留か別 change で正す。**本 change を止める理由にはしない**。

## アクションプラン

1. (合流を確定する前) 未 stage の 6 ファイルと未追跡の `deviation.md` を `git add` してから merge commit を作る — 漏れると main で kmp のテストが落ちる
2. (本 change の外) `kasane/changes/localize-dialog-error-messages` の蒸留時に、`kasane/handbook/cross/user-skill-writing-style.md:47` の撤回と `DM-KM-03` の重複付与を扱う
