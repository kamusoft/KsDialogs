# レビュー結果: fix-android-instrumented-toast-ime-hide-flake (005 回目)

**日付**: 2026-09-09
**判定**: CHANGES_REQUESTED

## サマリー

スコープ拡張後の全体 (共通プリミティブ・handbook 規約・skip の仕組みと lint・Loading 3 テストと BV-MA-03 の観測修正・workflow 3 本) を見た。review-004 の Major (失敗時に IME を出したまま抜ける巻き添え) は `restoreHidden` の新設と受け側の `ImeWindowCleanup` で塞がれており、Minor 5 件もコード側はすべて入っている。Loading 3 テストの観測修正は Scenario の保証を落としておらず、`assertNotSame` を待ちへ移した LD_WN_01 も検出力は同値。ビルド・lint はすべて通る。

一方で、この change の核心である「エージェントがスキップに逃げるのを防ぐ歯止め」は、承認された正規の印だけを検査しており、`@Ignore` と素の `.disabled(...)` という**より安い抜け道が検査ゼロで開いたまま**であることを実測で確認した。加えて、BV-MA-03 の対処が期限の 2 倍化にとどまり、その根拠が手元 1 回の実測 — この change が新設した切り分けフロー自身が求める「手元で 10 回以上」を、change 自身が満たしていない。新設した規約の 3 要素 (合意・非進行・安定) と履歴の要求も、規約が名指しする共通プリミティブでは満たされていない。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (Kotlin / Swift / Python / workflow のコメントを新設・改稿している) |
| `kasane/handbook/cross/ci-flaky-test-policy.md` | 本 change が新設。レビュー対象であり、かつ観測・切り分け・skip の照合規約 |
| `kasane/handbook/cross/verification-ci.md` | `.github/workflows/**` の変更・CI の失敗の切り分け・完了判定 |
| `kasane/handbook/cross/test-execution.md` | instrumented / Swift テストの変更と件数の確認 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | IME・入力の宛先という実行時挙動が絡む調査と完了判定 |
| `kasane/handbook/cross/ci-script-deletion.md` | `scripts/**` の新設 (`ci-skip-lint.py` に削除操作なし。抵触なし) |
| ksn-core `references/handbook.md` / `doc-structure.md` / `evidence.md` / `paths.md` | handbook 新設・証跡の設置・成果物のパス記述 |
| `kasane/lessons/process.md` L-001 / L-002 | 姉妹面の照合 (ios / maui の Swift 実装 2 本を照合) / 主張の範囲を実証範囲に限定 |
| `kasane/lessons/inbox/review-must-not-accept-weakened-structural-guarantee.md` | 観測修正が Scenario の保証を薄めていないかの照合 |

`kasane/lessons/code-review.md` は不在 (重点観点・指摘しないことの指定なし)。

## 実行した検査

| 検査 | 結果 |
|---|---|
| `./gradlew :ksdialogs-core:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL |
| `python3 scripts/ci-skip-lint.py --selftest` | 11 項目すべて OK (exit 0) |
| `python3 scripts/ci-skip-lint.py` | 印 0 件 / 許可リスト 0 件、違反なし (deviation の「skip は 1 件も適用していない」と一致) |
| `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` / `scenario-id-coverage.py` | いずれも exit 0 |
| `doc-structure-lint.py` | exit 0。新設した `ci-flaky-test-policy.md` は違反・警告とも 0 件 |
| `actionlint .github/workflows/*.yml` | 指摘 3 件はいずれも HEAD 時点の同一ファイルでも出る既存分 (`ci.yml:61` / `verify-ios.yml:34` / `verify-maui.yml:37`)。本変更で増えた指摘なし。`verify-android-instrumented.yml` は 0 件 |
| YAML パース (workflow 6 本) | 全件 OK。`workflow_call:` の入力撤去後も構文は成立 |
| `log-sanitize.py` (evidence 2 本・deviation) | 置換対象なし。ローカル絶対パス・端末個体・個人情報の混入なし |
| 本体の無改変 | `git status` に `src/main/**` の変更なし。差分はテスト・support・workflow・lint・handbook・change 配下のみ |
| lint の抜け道の実測 | 一時ディレクトリに再現した `.skipOnCi` は「承認が無い skip」として違反になる一方、同じ場所の `.disabled("面倒なので CI で止める")` は**印として 1 件も検出されない** (指摘 1) |
| lint の識別子解決 | 入れ子 suite (`extension MauiBridgeSuite { struct ContentSupplyTests }`) は `ContentSupplyTests.<テスト名>` に解決される (handbook の「suite 名.テスト名」とはずれる。指摘 7) |
| Swift 新設ファイルのビルド取り込み | `KsDialogsMauiBridge.xcodeproj` は `PBXFileSystemSynchronizedRootGroup` を使っており、`ios/` は SwiftPM のパス規約。どちらも pbxproj を触らずに新規ファイルがターゲットへ入る |
| `ios/Tests/.../DialogTestCiSkip.swift` の適用先なし | deviation 記録済みの合意差分 (仕組みの対称性)。違反として扱わない |
| 残留状態の巻き添え | `ToastTestHarness` は既定シングルトンと別の `ToastCoordinator` を持つため、`LONG_DURATION_MILLIS` 60 秒の期限が後続テストへ漏れない |

## review-004 指摘の解消確認

| review-004 の指摘 | 状態 | 確認箇所 |
|---|---|---|
| 🟠 失敗時に IME を出したまま抜ける巻き添え | 解消 | `ToastSystemInputTests.kt:76-80` の `finally` が `ime.restoreHidden()` を呼び、`ImeSettleWaiting.restoreHidden` は `clearFocus` + `hideSoftInputFromWindow` + `insetsController.hide(ime())` + 判定しない 2 秒待ち。受け側も `ToastSystemInputTests.kt:90` で `ImeWindowCleanup.ensureHidden` を通す (両案とも採用) |
| 🟡 硬化が効いている機構の帰属 | コードは解消 / evidence は未対応 | `ImeSettleWaiting.kt` の `State.isSettled` doc が「要はアニメーション非進行」「枠の高さは要求可視性とほぼ同値で単独では偽の合格を弾けない」と書き分け済み。evidence 側の A/B の書き直しは入っていない (指摘 5) |
| 🟡 観測の取り付けが挙動を変える | コードは解消 / evidence は未対応 | `ImeSettleWaiting.attach` の doc に「コールバックの登録自体は framework から見えており、観測を付けた版と付けない版の所要時間を比べる用途には使えない」を明記。evidence の但し書きは入っていない (指摘 5) |
| 🟡 `animating` の stale ガード | 解消 | `ImeSettleWaiting.animatingNow()` が `STALE_ANIMATION_MILLIS` (2 秒) 超過の走行を外し、外したことを履歴に残す |
| 🟡 履歴の打ち切り方向 | 解消 | `StateHistory` が head 100 / tail 400 の二段構えになり、捨てた区間を `format()` に明示 |
| 🟡 workflow ヘッダの実測との食い違い | 解消 | `verify-android-instrumented.yml:11-18` を「なぜこの job だけ観測を常設するか」の現在形へ書き直し。当時の再現性の見立ては落ちている |
| 🔵 一時 workflow の削除 | 解消 | `repeat-android-instrumented.yml` 削除済み。`artifact-suffix` 入力と成果物名の接尾辞も撤去され、成果物名は固定 |
| 🔵 `awaitContainersDetached` の doc | 解消 | 「表示そのものが期限で終わることは見ない (別のテストが担保する)」を明記 |
| 🔵 残存モードの持ち出し | 未対応 (蒸留へ申し送り) | 「Toast を出す前から IME が出ない」の署名がアーカイブされない側に残っていない (指摘 10) |

## 指摘事項

### [🟠 Major] skip の歯止めが正規の印しか見ておらず、`@Ignore` / 素の `.disabled(...)` という無承認の抜け道が検査ゼロで残る

**該当箇所**: `scripts/ci-skip-lint.py:51-57`、`kasane/handbook/cross/ci-flaky-test-policy.md:77-92`

**問題点**: 新設した歯止めは「`@SkipOnCi(` と `.skipOnCi(` という文字列を探し、許可リストと突き合わせる」形になっている。歯止めの目的は handbook が自ら「skip は、直せない失敗と直せる失敗の区別がつかないまま赤を消せてしまう手段であり、作業を進める側にとって常に最短の出口として見える」と述べているとおり、**最短の出口を塞ぐこと**にある。ところが JUnit4 の `@Ignore` と Swift Testing の素の `.disabled(...)` / `@Test(.disabled(...))` は、印としても違反としても 1 件も検出されない。どちらも承認手順を一切通らず、結果 XML では skipped に数えられるため「実行数 = tests - skipped」で見る件数検査も素通りする。つまり、承認された道より安く・痕跡が残らない道が開いている。

実測で確認した (一時ディレクトリに再現):

```
MARK: .../S.swift:4 -> ContentSupplyTests.BV_MA_03_toastContentUnavailable
violations: ['.../S.swift:4: ... — オーナー承認 (config の lint.ci-skip.allow) が無い skip']
warnings: []
```

同じディレクトリに置いた `.disabled("面倒なので CI で止める")` は MARK にも violations にも現れない。

現在のリポジトリには対象範囲の `@Ignore` が 0 件、`.disabled(` は skip ヘルパ 2 本の定義行だけ (`samples/**` の SwiftUI 修飾子 2 件は走査根の外) なので、検出を足しても既存債務は発生しない。

**推奨修正**: `SCAN_ROOTS` と同じ走査で `@Ignore` (Kotlin) と `.disabled(` / `@Suite(` 内の disable (Swift) も拾い、「承認の無い skip」として同じ違反にする。ヘルパ自身の定義 (`ios/Tests/KsDialogsTests/Support/DialogTestCiSkip.swift` / `maui/macios/native/KsDialogsMauiBridgeTests/Support/BridgeTestCiSkip.swift`) だけを除外する。handbook の「skip の書き方」にも「これ以外の手段 (`@Ignore`・素の disable) で CI の実行から外さない」を明記する。

### [🟡 Minor] BV-MA-03 の対処が期限の 2 倍化にとどまり、根拠が手元 1 回の実測 — 新設した切り分けフロー自身の要求を満たしていない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridgeTests/MauiBridgeContentSupplyTests.swift:20`、`evidence/ci-flake-triage.md` の切り分けの表

**問題点**: 新設した `ci-flaky-test-policy.md` の切り分けフローは「3. 手元で反復する。同じテストを 10 回以上、CI と同じ実行条件で回し、失敗の有無を記録する」を要求している。BV-MA-03 の行は手元 (対応前) が「1 回中 0 回」、対応後も「1 回中 0 回」で、この change が同じ回で書いた規約を BV-MA-03 では満たしていない。また記録された「所要 3.26 秒 / 期限 3.0 秒」が観察の窓の所要なのかテスト全体 (期限切れを待つ cleanup を含む) の所要なのかが書かれておらず、6.0 秒という新しい値がどれだけの余裕を持つのかを読み取れない。手元より遅い CI ランナーで観察が 6 秒に届けば、同じ落ち方が戻る。

同じ change の Android 側は同型の問題 (期限切れが観察に追いつく) を、期限を 60 秒へ広げたうえで**期限切れを待たずに後始末で撤去する**構造で解いており (`ToastSystemInputTests.kt:172` と `detachToasts`)、Swift 側だけが余裕の 2 倍化に留まっている。Swift 側で同じ手が取れないのは cleanup が `overlays.added.isEmpty` を 10 秒上限で待つ (同ファイル `:141`)、つまり期限切れに依存して片付けているためで、ここが期限を伸ばせない縛りになっている。

**推奨修正**: どちらかを行う。(a) cleanup を期限切れ待ちから明示的な撤去へ変え、期限は観察の所要に左右されない値にする (Android 側と同じ解き方)。(b) 6.0 秒のまま進めるなら、手元の反復を 10 回以上行い、観察の窓の所要 (最大値) を evidence に記録して余裕を示す。あわせて `evidence/ci-flake-triage.md` の「所要 3.26 秒」が何の所要かを明記する。

### [🟡 Minor] 新設規約の「合意」と「時間切れに履歴を添える」が、規約が名指しする共通プリミティブで満たされていない

**該当箇所**: `kasane/handbook/cross/ci-flaky-test-policy.md:22-30`、`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/support/InstrumentedStateSettling.kt:62-72`、`android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingAttributeTests.kt:133-137`

**問題点**: 規約は判定を「合意 (独立した複数の観測が一致) + 非進行 + 安定」の 3 つを揃えて行うと定め、「この待ちは共通プリミティブとして置き」「時間切れの説明文には、単調時計付きの観測履歴 (`StateHistory`) を添える」と述べている。しかし規約が名指しする `InstrumentedStateSettling.awaitWindowFocused` は単一の観測 (`hasWindowFocus`) だけで判定し、履歴も持たない。LD_AT_03 / LD_AT_04 / LD_WN_01 の新しい待ちが時間切れになると、残るのは「前提: Loading の器が入力の宛先になっている」という一行だけで、規約が「事後には読めない」と述べた経過はどこにも残らない。`ImeSettleWaiting.State.isSettled` も、doc 自身が「枠の高さは要求可視性とほぼ同値」と認めており、3 条件のうち独立な観測は実質 1 つ。つまり「合意」も名前どおりには成立していない。

規範層はコードが従う側なので、この食い違いは放置すると「規約は満たしていないが動いている」状態を既定にする。

**推奨修正**: 規約側を実態に合わせて書き直すのが軽い。「合意」を必須要素ではなく「独立な観測が 2 つ以上取れる対象では揃える」と条件付けし、履歴の要求も「時間切れの説明が現象を特定できない待ち」に限定する。逆に規約を保つなら、`awaitWindowFocused` に `StateHistory` を持たせ、時間切れの assertion メッセージへ添える。

### [🟡 Minor] LD_CO_13 で「合流1件を重ねた」ことが検査されておらず、メッセージ非引き継ぎの検査が空振りしうる

**該当箇所**: `android/ksdialogs-core/src/androidTest/kotlin/jp/kamusoft/ksdialogs/LoadingCoalescingTests.kt:347`, `:372`

**問題点**: 第 1 世代をカスタム View にしたことで、`harness.builtinText` は第 1 世代の間つねに `null` になる (`builtinContentView` は `LoadingContentRequest.Builtin` のときだけ設定されるため)。第 2 世代は既定ローディングなので、`assertNull("旧世代のメッセージを引き継がない", harness.builtinText)` は「新世代が `latestMessage` を引き継いでいたら "A" が出る」という形で検出力自体は保たれている。しかしその成立は「`show(message = "A")` が第 1 世代へ合流し、`latestMessage` に "A" が入っていること」に依存しており、そこを確かめる assertion が無い。合流が起きなかった (または message が記録されなかった) 回は、検査が何も見ずに通る。

同じファイルは他所 (`:92`, `:213`, `:401`) で `coalescedUseCount` を待って合流の成立を確かめており、ここだけ前提が裸になっている。

**推奨修正**: `harness.loading.show(message = "A")` の直後に `assertEquals(2, harness.coalescedUseCount)` (または `InstrumentedDialogWaiting.waitUntil { harness.coalescedUseCount == 2 }`) を足し、「合流1件を重ねてメッセージを持たせた」ことを前提として固定する。

### [🟡 Minor] review-004 の Minor 2 件が evidence 側では未対応で、2 本の evidence が「対応後」に別々の数字を出している

**該当箇所**: `evidence/ci-repeat-signature.md`「旧条件と硬化後の A/B」、`evidence/ci-flake-triage.md` の切り分けの表

**問題点**: review-004 は「evidence の A/B の説明も、硬化後 0 件を 3 条件全体に帰属させるのではなく、どの条件が何を塞いだかで書き直す」「A/B の但し書きとして、2 版が待ち条件と観測の有無の両方で異なることを明記する」を求めていた。コード側の doc は両方入ったが、evidence の該当節は「硬化後 (要求可視性 + 枠の高さ + アニメーション停止)」のままで、帰属の書き分けも観測の有無の但し書きも入っていない。

さらに、同じテストの「対応後」の数字が 2 本の evidence で食い違う — `ci-repeat-signature.md` は 50 回中 2 件が残ると記録し、`ci-flake-triage.md` は 30 回中 0 回と記録している。差は「最初の要求前の落ち着き待ち (`assertIdleBeforeFirstRequest`) を入れる前/後」で説明がつくが、どちらの版を測ったのかがどちらにも書かれていない。読む側は「対応後もまだ 2 件落ちるのか」と読み違える。これは L-002 (主張の範囲を実証範囲に限定する) の対象。

**推奨修正**: A/B の表に版の定義を書き足す (何を待つ版か・観測を付けているか)。硬化後 50 回の測定が落ち着き待ちを入れる前の版であること、残った 2 件はその後の落ち着き待ちで塞がれ 30 回中 0 回になったことを、どちらの evidence からも辿れる形で明記する。

### [🟡 Minor] 許可リストをエージェント自身が書く経路が、手順としてもレビュー項目としても塞がれていない

**該当箇所**: `kasane/handbook/cross/ci-flaky-test-policy.md:98-104`、`kasane/config.yaml:31-38`

**問題点**: 承認の実体は「オーナーが `lint.ci-skip.allow` に項目を足すこと」と定義されているが、機械が見分けられるのは「印に対応する項目があるか」だけで、その項目を誰が書いたかは判定できない。handbook の「レビューでの照合」も「印の付いたテストが `lint.ci-skip.allow` にあり、`approved` / `review-by` が入っている」までしか求めていないため、印と許可リスト項目を同じ回に自分で書いたエージェントは、lint もレビューも通ってしまう。config のコメントに「エージェントは自分で足さない」と書いてあることが唯一の歯止めになっている。

**推奨修正**: 「レビューでの照合」の 1 項目目を「許可リストの項目がオーナーの判断として `evidence/` に残っている (依頼と回答の記録があり、`approved` の日付と一致する)」へ強める。実測 (2 項目目) と機構の診断 (3 項目目) は既にあるので、追加は 1 行で済む。

### [🔵 Suggestion] `ci.yml` 冒頭の lint 列挙に新しい検査が入っていない

**該当箇所**: `.github/workflows/ci.yml:9-11`

**問題点**: 冒頭コメントは「`paths-ignore` は使わない。lint (gitleaks / identity-lint / scenario-id-coverage) は `kasane/**` も入力にするため」と述べる。今回足した `ci-skip-lint.py` は `kasane/config.yaml` を入力にする lint なので、この理由付けの列挙に当てはまるのに載っていない。列挙が網羅でないなら「等」を添えるだけでもよい。

**推奨修正**: 列挙に加えるか、網羅でないことを示す。

### [🔵 Suggestion] 許可リストに書く `test` の形が handbook の説明と lint の解決結果でずれる

**該当箇所**: `kasane/handbook/cross/ci-flaky-test-policy.md:77-92`、`scripts/ci-skip-lint.py:216-236`

**問題点**: handbook と config のコメントは「Swift は suite 名.テスト名」と説明するが、`_enclosing_type` は印より前の**直近の型宣言**を取るため、`extension MauiBridgeSuite { struct ContentSupplyTests { ... } }` の形では `ContentSupplyTests.<テスト名>` に解決される (実測)。オーナーが説明どおり `MauiBridgeSuite.<テスト名>` と書くと対応が付かず、承認済みなのに「承認が無い skip」として落ちる。落ち方は安全側だが、承認の場面で詰まる。

**推奨修正**: handbook に「`test` に書く値は `python3 scripts/ci-skip-lint.py --list` が表示する識別子をそのまま使う」と書く (`--list` は既にある)。

### [🔵 Suggestion] `ci-skip-lint.py` の走査が追跡外のビルド生成物も歩く

**該当箇所**: `scripts/ci-skip-lint.py:181`

**問題点**: 除外は `.git` / `build` / `obj` / `bin` のみで、`maui/macios/native/DerivedData/` と `ios/DerivedData/` は歩く。現状は所要 0.2 秒で誤検出も無いが、テストソースの写しが生成物側に入ると、追跡されていないパスの印を違反として報告する。他の lint (`local-path-lint.py` 等) が追跡ファイルを起点にするのと揃わない。

**推奨修正**: 除外に `DerivedData` を足す (または `.gitignore` 相当の判定を通す)。

### [🔵 Suggestion] evidence / deviation の handbook 参照がリポジトリ相対になっていない

**該当箇所**: `evidence/ci-flake-triage.md` の冒頭、`deviation.md` の BV-MA-03 の項

**問題点**: `handbook cross/ci-flaky-test-policy.md` / `handbook cross/verification-ci.md` という書き方で、リポジトリルートから辿れるパスになっていない (ksn-core `references/paths.md` は `kasane/handbook/cross/...` を求める)。`evidence/` 内の隣接ファイルを `ci-repeat-signature.md` と裸で書いている箇所も、change 相対なら `evidence/ci-repeat-signature.md`。

**推奨修正**: リポジトリ相対 / change 相対へ直す。

### [🔵 Suggestion] 硬化後も残る別モードの署名が、アーカイブされない側へ持ち出されていない

**該当箇所**: `evidence/ci-repeat-signature.md`「旧条件と硬化後の A/B」

**問題点**: review-004 が蒸留への申し送りとして挙げた「Toast を出す前から IME が出ない」(Activity 起動に伴う server 起源の `HIDE_UNSPECIFIED_WINDOW` が最初の show を潰す) の見分け方が、まだ evidence にしかない。`ci-flaky-test-policy.md` にも `verification-ci.md` の見分け表にも入っていないため、次に同じ形で赤くなると切り分けが最初からやり直しになる。

**推奨修正**: 蒸留の際に `verification-ci.md` の見分け表か `kasane/lessons/inbox/` へ 1 行残す (review-004 の申し送りどおり、この回で入れる必要はない)。

## アクションプラン

1. **Major 1 件を塞ぐ**: `ci-skip-lint.py` に `@Ignore` / 素の `.disabled(` の検出を足し、handbook の「skip の書き方」に「これ以外の手段で CI の実行から外さない」を明記する。既存債務が発生しないことは確認済み
2. Minor 2 件 (BV-MA-03 の根拠 / LD_CO_13 の前提 assertion) はコードと evidence の両方に触る。BV-MA-03 は (a) cleanup の作り替えか (b) 手元 10 回の実測のどちらを取るかを先に決める
3. Minor 1 件 (規約と共通プリミティブの食い違い) は、規約を実態へ寄せるか プリミティブに履歴を足すかの選択。規約側の 2 文の書き直しで済む方を推す
4. Minor 2 件 (evidence の帰属と版 / 許可リストの provenance) は文書だけの修正。2 と同じ回で入る
5. Suggestion 5 件のうち、`--list` の案内 (指摘 7) と `DerivedData` 除外 (指摘 8) は承認運用が始まる前に入れておくと詰まらない。残り 3 件は蒸留でよい
