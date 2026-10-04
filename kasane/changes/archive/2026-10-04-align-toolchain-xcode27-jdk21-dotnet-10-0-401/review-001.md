# レビュー結果: align-toolchain-xcode27-jdk21-dotnet-10-0-401 (001 回目)

**日付**: 2026-10-04
**判定**: APPROVED

## サマリー

`exploration.md` の「実装するもの」9 項目は diff に漏れなく入っており、「触らないもの」への変更は無い。テスト 6 ルートを全件で回し直して失敗 0 を確かめ、証跡の件数とも一致した。Critical / Major は無く、残るのは完了判定までに片づける Minor 3 件 (instrumented の未実行、未追跡の `module.modulemap`、iOS テストの原因調査の証跡) と Suggestion 2 件。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always)
- `kasane/handbook/cross/test-execution.md` (テストの実行・完了判定)
- `kasane/handbook/cross/verification-ci.md` (`.github/workflows/` の変更)
- `kasane/handbook/cross/ci-flaky-test-policy.md` (`ios/Tests/**` で状態遷移を観測するテストの変更)
- `kasane/lessons/code-review.md` (L-001 / L-002)
- 決定: maui/ADR-0004、maui/ADR-0007 (proposed。決定としては扱わず、合意スコープの記述と照合)、cross/ADR-0018、cross/ADR-0002

ロードしたスキル: ksn-review, swift-ui-impl-skill, kotlin-impl-skill

## 確認した観点

### 仕様充足 (合意スコープとの照合)

| 決定事項 | 結果 |
|---|---|
| `global.json` を SDK 10.0.401 / workload 10.0.401.1 (`rollForward: disable` は不変) | 一致 |
| macOS job 9 か所を `xcode-27` に | 9 か所 (`release.yml` 3、verify 系 6)。`macos-26` の残りはコメント内の説明 2 行だけ |
| Xcode の版指定 8 か所を 27.0 に | 8 か所 |
| `setup-java` 10 か所を 21 に | 10 か所 |
| `-collect-test-diagnostics never` 2 か所 | `verify-ios.yml:106`、`verify-maui.yml:266` |
| job 名の不変 | diff に `name:` 行の変更なし |
| Kotlin 2.4.20 (catalog と Sample の直書き) | 2 か所。追跡ファイルに 2.4.10 の残りは README 2 枚と `skills/` だけ (触らないもの) |
| `jvmToolchain` を足さない・配布物の対象を変えない | `jvmToolchain` は無し。生成 class は major version 55 (実測) |
| MAUI 本体と Sample の `MauiVersion` を 10.0.20 のまま | 値は不変、コメントだけ更新 |
| iOS のテスト 1 件の修正 | 後述 |
| コメントを変更後の内容に合わせる | 4 ファイル。旧版の値 (26.5 / 10.0.300 / temurin 17) を書いたコメントの残りは無し |

- 触らないもの: `ios/Package.swift`・`*.pbxproj`・README 2 枚・`skills/` に diff 無し。TFM の platform 版の明示も無し
- 足場 (`exploration.md`) の書き換え: 実装 diff からは確認できる範囲で無し
- `deviation.md`: 蒸留送り 4 行と乖離 1 行 (自 assembly 用 aar)。乖離の行は証跡の記述と一致し、合意済みの差分として扱った。`[付随修正]` の行は無い

### テスト (レビューで回し直した結果)

環境: Xcode 27.0 / iOS 27.0 Simulator (iPhone 17)、JDK 21、.NET SDK 10.0.401、Kotlin 2.4.20。`kasane/handbook/cross/test-execution.md` の全件実行表の 7 行と突き合わせた。

| 表の行 | 件数 | 証跡との一致 |
|---|---|---|
| ios/ | `Test run with 372 tests in 61 suites passed` / `Executed 0 tests` | 一致 |
| android/ | 109 tests / 0 failures / 0 skipped | 一致 |
| android/ (instrumented) | **未実行** (証跡でも未検証。指摘 1) | — |
| kmp/ | `iosSimulatorArm64Test` 86 / 0、`testAndroidHostTest` 83 / 0 (metadata compile 2 本も成功) | 一致 |
| maui/ | 合計 206 / 失敗 0 | 一致 |
| maui/android/native/ | 41 tests / 0 failures | 一致 |
| maui/macios/native/ | `Test run with 17 tests in 7 suites passed` / `Executed 0 tests` | 一致 |

負のコンパイル検証 (証跡では未検証。コンパイラが 3 系統とも替わるためレビューで回した): ios 16 本・android 15 本・kmp 12 本・maui 19 本をフラグごとに 1 本ずつ回し、すべて期待した診断で失敗した。診断文言の差は指摘 5 の所見を参照。

CI の lint job が回す検査のうち 9 本 (ローカル絶対パス・個体情報・コメント規約・Scenario ID 網羅・CI 限定スキップ・README 最小例・インストール例・待ちの時間予算・publish の step 順序) を手元で回し、すべて違反なし。

レビューで回していないもの (証跡の記述に依った): Sample 4 形態のビルド、消費者検証 4 本、pack した nupkg の TFM group と aar、klib の manifest。

### iOS のテスト修正が検査を弱めていないか

弱めていないと判断した。

- 検査の式と許容 (`<= 1`) は変わっていない。変わったのは、バーの枠を読む時点だけ (台帳に候補が載った直後の 1 回読み → 値が続けて同じになるまで待ってから読む)
- 比べる相手 (ナビゲーションバーの枠) は、製品が配置に使う台帳の矩形とは独立の観測のまま。製品が modifier の枠を無視すれば、ダイアログの上端は落ち着いたバーの下端からずれて従来どおり失敗する。待ちの機構を外すと、iOS 27 では途中の枠 (106) を読んで失敗する (探索の実測: 上端 104 に対しバー下端 156)
- 落ち着き待ちは共通プリミティブ `DialogTestWaiting.awaitSettled` を使い、時間切れは `#require` で失敗になる (黙って先へ進まない)。待ちの延長や許容の拡大で吸収する形ではない
- 修正後にテストが通ること自体が、落ち着いたバーの下端とダイアログの上端が一致する (製品の表示位置はバーに重ならない) ことを示す。探索の「製品の挙動の不具合なら止めて報告」には当たらない

### 設計品質

- コメント規約: 追加コメントに作業文書のパス・ローカル通番・履歴記述は無い (lint 0 件、目視でも該当なし)。ADR は ID 形式で参照している
- 追加コメント 3 か所が maui/ADR-0007 を参照するが、同 ADR は proposed。合意スコープが「maui/ADR-0007 に合わせる」と明示しているため指摘にはしない。蒸留で accepted にならなかった場合は、この 3 か所 (`maui/Directory.Packages.props:13-16`、`samples/maui/KsDialogs.Sample.Maui/KsDialogs.Sample.Maui.csproj:21-23`、`verification/maui/VerificationApp.csproj:49-50`) の書き直しが要る
- オーバーエンジニアリング: 無し。テストの補助は private 関数 1 つで、新しい抽象は足していない
- Swift: 強制アンラップ無し、`@MainActor` の中で完結、タプルの比較は標準の `==` で成立
- 機密情報・ローカル絶対パス: 追加行に無し

## 指摘事項

### [🟡 Minor] Android の instrumented テストが未実行のまま

**該当箇所**: `evidence/completion-checks.md:66` (未検証の節)
**問題点**: `kasane/handbook/cross/test-execution.md` の全件実行表 7 行のうち、`android/ (instrumented)` の行だけ件数が無い。Kotlin を 2.4.20 に上げた変更は `androidTest` のソースを作るコンパイラも替えるため、この行は本変更に該当する。証跡は未検証と正直に書いており、全件成功を主張してはいないが、完了判定の時点では行が欠けたままになる。`exploration.md` の完了条件にこの行が無いことも、欠けの原因になっている。
**推奨修正**: develop への push で CI の android-instrumented job (API 36) の件数・failures・skipped を証跡に足す。API 29 側は CI に載らないので、手元で回すか、回さないならその判断を `deviation.md` に残す。件数が載ると「instrumented が実際に走った」ことが分かる (載らなければ、`No connected devices` 等で 1 件も走っていない場合と区別できない)。

### [🟡 Minor] Kotlin 2.4.20 が生成する `module.modulemap` 6 件が未追跡

**該当箇所**: `kmp/.swiftpm-locks/default/swiftImport/` 配下 4 件、`samples/kmp/iosApp/KotlinMultiplatformLinkedPackage/` 配下 2 件 (いずれも 0 バイト)
**問題点**: 同じ合成 package の他のファイル (`Package.swift`・`.m`・`.h`) は追跡されている。追跡しないままコミットすると、clone し直した環境で `samples/kmp/iosApp` の 1 回目のビルドが「Synthetic project regenerated」で止まり (証跡 `evidence/completion-checks.md:49-58` の実測)、`kmp/` のテストを回すたびに未追跡ファイルが作業ツリーに現れる。
**推奨修正**: 既存の追跡ファイルと同じ扱いで 6 件を追跡する (オーナーに確認中の件。レビューとしては追跡を推す)。追跡すれば、`deviation.md:4` の蒸留送り (Kotlin を上げた後は合成 package を再生成する) と整合する。なお `verification/kmp/iosApp/KotlinMultiplatformLinkedPackage/` の追跡分にも同じファイルは無いが、消費者検証は作業コピーの中で再生成するため影響しない (`verification/kmp/build-consumer.sh:97-102`)。

### [🟡 Minor] iOS のテスト 1 件の原因調査が証跡に残っていない

**該当箇所**: `evidence/completion-checks.md` (該当の節なし)、`ios/Tests/KsDialogsTests/DialogCurrentPageSwiftUITests.swift:48-53`
**問題点**: 「テストの測り方の問題で、製品の不具合ではない」という判断の根拠 (途中の枠 106 → 次のレイアウトパスで 54、106 の枠は一度も描画されない) が、ソースコメントと `deviation.md:3` にしか無い。とくに「一度も描画されない」は、どう確かめたかが読めない。合意スコープは製品の不具合なら止めて報告すると定めており、止めなかった判断の裏づけは証跡に要る。
**推奨修正**: 調査時の観測 (バーの枠と content の safe area の時系列、または落ち着き待ちの観測履歴) を `evidence/` に足す。「描画されない」を確かめていないなら、コメントの該当句を実測した範囲 (次のレイアウトパスで 54 になる) に留める。

### [🔵 Suggestion] バーの枠の落ち着き判定が「同じ値が続く」だけ

**該当箇所**: `ios/Tests/KsDialogsTests/DialogCurrentPageSwiftUITests.swift:60-72`
**問題点**: `kasane/handbook/cross/ci-flaky-test-policy.md` は終端の判定に「独立した観測の合意・非進行・安定」を求める。ここでの判定は安定 (48 ms 同じ値) だけで、途中の枠が 48 ms を超えて残る環境 (容量の小さい CI ランナー) では、途中の枠を終端と読んで失敗する余地がある。偽の合格にはならず、失敗側に倒れる。
**推奨修正**: `xcode-27` ランナーでの最初の実行でこのテストを見る。落ちたら待ちを延ばすのではなく、合意の条件を足す (例: バーの下端が content の safe area の上端と一致する)。この条件があると、途中の枠 (バー 106 / safe area 54) は長く残っても落ち着いたと読まれない。

### [🔵 Suggestion] 蒸留への申し送りに足したいもの

**該当箇所**: `deviation.md:6`
**問題点と推奨修正**:
- docs-refresh の対象が README 2 枚だけになっている。`skills/{en,ja}/ksdialogs-android/SKILL.md:34,60,64` と `skills/{en,ja}/ksdialogs-kmp/SKILL.md:43` も Kotlin 2.4.10 (ビルドに使う版・検証済みの版・Compose compiler plugin の例) を書いているので、同じ行に足す
- 負のコンパイル検証の診断が `kasane/handbook/cross/test-execution.md` の表と 2 点違った (Kotlin 2.4.20 での実測。2.4.10 でも同じだったかは未確認): kmp/ の `Unresolved reference` 系 5 本 (`loadingStyleProperty`・`toastStyleProperty`・`toastRegistration`・`loadingRegistration`・`toastHide`) に `on receiver of type '<型>'` が付く。android/ の `toastShowResult` は `Initializer type mismatch` に加えて `Type mismatch: inferred type is 'Unit', but 'String' was expected.` の 2 件が出る。どちらも期待した誤りを弾いており検査は有効だが、表の文言の更新を蒸留送りに足す

## アクションプラン

1. `module.modulemap` 6 件の扱いをオーナーの回答で確定する (追跡を推奨)
2. develop への push 後、CI の 5 形態の本体検証と lint の結果を証跡に足す。android-instrumented の件数を含める。iOS job では `DialogCurrentPageSwiftUITests` の修正したテストを確認する
3. instrumented の API 29 側を回すか、回さない判断を `deviation.md` に残す
4. iOS のテストの原因調査の観測を `evidence/` に足す
5. `deviation.md` の蒸留送りに `skills/` の Kotlin 版と負のコンパイル検証の診断文言を足す
