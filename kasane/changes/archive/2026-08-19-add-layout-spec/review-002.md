# レビュー結果: add-layout-spec (002 回目)

**日付**: 2026-08-19
**判定**: CHANGES_REQUESTED

## サマリー

core/ADR-0014・0015 に沿った VM 経路の撤去と「コンテンツ添付 + show 引数 (placement のみ)」への置換は、4ルートすべてで一貫して実装されている。旧経路 (`DialogLayoutProviding` / `IDialogLayoutProviding` / `MauiDialogLayoutAttributes`) と廃止属性 (明示サイズ・角丸・枠線) はコードから完全に消えており、KMP 公開面は `DialogPlacement` のみ、show に options 引数を作らないことは4ルートの負のコンパイル検査で構造的に固定されている。ビルド・テストは5ビルドルートすべて green (下表)、実環境の証跡 (透明オーバーレイの輝度実測・MAUI hit-test 再確認・4ルート Sample 通し) も水準が高い。

一方で、**契約が定めるスナップショット時点 (初回レイアウトパス完了時点) と、Native 2実装が実際に添付を読む時点 (器の構築時) がずれている**。現在のスコープでは観測できないが、concepts が明文で保証している範囲を満たしておらず、design が expand-api-surface へ申し送った SwiftUI / Compose の供給機構 (いずれもレイアウトパス中に値が届く前提) はこの実装では取りこぼす。deviation.md にも記載がないため Major として起票する。

### 実行した検証

| ルート | コマンド | 結果 |
|---|---|---|
| ios/ | `xcodebuild test -scheme KsDialogs -destination 'platform=iOS Simulator,id=76EB1CA1-…'` | 55 tests / 14 suites passed (XCTest 側は 0 件 = Swift Testing のみ) |
| android/ | `./gradlew test --rerun-tasks` | 40 tests / 0 failures |
| android/ (instrumented) | `./gradlew connectedDebugAndroidTest` (Pixel 4a - 13 実機) | 60 tests / 0 failed |
| kmp/ | `./gradlew allTests --rerun-tasks` | 48 tests / 0 failures (iosSimulatorArm64 + androidHostTest) |
| maui/ | `dotnet test` | 26 tests / 0 failures |
| maui/android/native/ | `./gradlew :ksdialogs-maui-bridge:test --rerun-tasks` | 9 tests / 0 failures |
| 負のコンパイル検査 | 4ルートすべて | 期待どおり失敗。属性検査の指摘箇所も確認済み (iOS のみ注意点あり → 🟡 Minor 3) |
| コメント規約 lint | `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 357 ファイル |
| ケース表の同一性 | `core/layout-spec/cases.json` ↔ `specs/dialog-contract/layout-cases.json` | 完全一致 (19ケース) |

deviation.md の2点 (パネルの戻る導線・パネル内結果表示) は合意済み差分として扱い、指摘に含めていない。

## 指摘事項

### [🟠 Major] 添付のスナップショット時点が契約より早い (器の構築時に読んでいる)

**該当箇所**:
- `ios/Sources/KsDialogs/Presentation/DialogContainerViewController.swift:44-47`
- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogContainer.kt:33` (`DialogLayout.snapshotOf`)
- `android/ksdialogs/src/main/kotlin/jp/kamusoft/ksdialogs/DialogLayout.kt:64-68`

**問題点**:
デルタスペック `specs/dialog-contract/spec.md:23` は「器が採用する実効値は**初回レイアウトパス完了時点で添付されている値のスナップショット**とし、以降の添付変更は表示に反映しないこと (SHALL)」と定める。改訂した concepts `kasane/concepts/core/api/layout-semantics.md:88-89` はこれをさらに明示して「実装がいつ読み取るかは自由だが、どのタイミングで読んでも**この時点の添付値と一致する**ことが契約」「したがって、**show を呼んだあとでもこの時点までに書き換えた値は採用され得る**」と書いている。

しかし iOS / Android とも、実効値は**器のコンストラクタ内で1回だけ**読まれ、以後 `let` / `val` として固定される。読み取り点は「factory が View を返した直後」であり、初回レイアウトパス完了時点より前である。したがって次の区間の書き込みは契約上採用されるはずなのに黙って落ちる:

- `UIView.didMoveToWindow` / `layoutSubviews`、`View.onAttachedToWindow` / `onMeasure` の中での添付
- design.md Decision 6 が expand-api-surface へ申し送った SwiftUI (「PreferenceKey 方式が表示中 window + `layoutIfNeeded()` 中に**同期到達**」) と Compose (「`SideEffect` 書き込み + `doOnPreDraw` 読み」) の供給機構 — どちらも値が届くのはレイアウトパスの最中であり、構築時読みでは1回も届かない

現状スコープの供給経路 (View の init / factory 内での添付、MAUI の `makeContentView` / `createContentView` での添付、show 引数) はいずれも構築前に確定するため**現在のテストでは観測できず、実害も出ていない**。テスト (`DialogAttributeSupplyTests` の「初回レイアウト完了後の添付変更は反映されない」) も、レイアウト**後**の書き換えだけを見ており、構築〜レイアウト完了の区間を突いていない。そのため回帰としても検出されない。iOS / Android の解釈は互いに一致しているので、ルート間の食い違いではない。deviation.md にこの差分の記録はない。

**推奨修正**: 次のいずれかを選び、選んだ結果を証跡に残すこと。
1. 読み取りを初回レイアウトパスへ寄せる — iOS は `prepareForPresentation` / 初回 `viewWillLayoutSubviews` の中で1回だけ合成して以後固定、Android は `DialogLayoutHost` の初回 `onMeasure` (または `doOnPreDraw`) 時点で1回だけ合成して以後固定。あわせて「構築後・初回レイアウト完了前の添付が採用される」ことを iOS / Android 双方の供給テストに1本ずつ足す (現状の「レイアウト後の変更は無視」テストと対になる)
2. 「採用点は器の構築時 (= factory がコンテンツを返した直後)」を正とするなら、オーナー合意のうえ deviation.md に記録し、`layout-semantics.md:84-92` の「いつの値が使われるか」を蒸留フェーズで実装に合わせて改訂する。この場合、SwiftUI / Compose の供給機構が成立しないことを expand-api-surface への申し送りとして明記する必要がある (design.md Decision 6 のプローブ結果が前提から崩れるため)

なお 1 を採る場合も、器が構築時に読む値をそのまま使う MAUI / KMP 経路には影響しない (どちらも Native の添付面へ書いてから提示に入るため)。

### [🟡 Minor] iOS の外側タップが「実タップ経路」で自動検証されていない

**該当箇所**: `ios/Tests/KsDialogsTests/DialogOutsideTapTests.swift:18-30, 34-46`

**問題点**:
ios-native spec の Scenario「外側タップの結果経路」は「オーバーレイ領域を**タップする**」を WHEN に置くが、iOS のテストは `container.reportOutsideTap()` を直呼びしており、`hitTest` → `UITapGestureRecognizer` → `gestureRecognizer(_:shouldReceive:)` → ハンドラの経路を通していない。デリゲートについて検証しているのは `recognizer.delegate === container` という結線の同一性だけで、**内側/外側を判別する述語 (`touchedView.isDescendant(of: contentView)`, `DialogContainerViewController.swift:224-225`) の中身は1件も実行されていない**。この述語が反転しても全テストが green のままになる (= ダイアログ内のどこを触っても閉じる回帰を検出できない)。

同じ要件の Android 側は `DialogTouchInjection.tap` で実入力を注入し、テストの docstring にも「覆いのヒットテストとクリック検出を通した経路でしか確かめられないため、器の関数は直に呼ばない」と明記されている (`android/ksdialogs/src/androidTest/.../DialogOutsideTapTests.kt:34-38`)。同一要件で2ルートの検証強度が非対称になっている。

Scenario 自体は `verification/sample-walkthrough/notes.md` の「確認3」(4ルートで実座標タップ → cancelled) と「確認2」(ダイアログ内 OK 押下 → completed) で手動確認されているため、**未達ではなく回帰保護の穴**である。

**推奨修正**: iOS 側にも実タップ相当の1本を足す — 最小でも `container.gestureRecognizerShouldReceive` 相当を、中身の上の点と覆いの上の点の2つで直接呼んで真偽を確かめる (デリゲートメソッドを直接呼ぶだけなら Simulator テストで完結する)。あわせて「内側タップでは cancelled にならない」を iOS / Android 双方に1本ずつ足すと、両ルートの強度がそろう。

### [🟡 Minor] KMP ルートの Android 側だけ視覚照合が未実施のまま tasks 6.3 が完了扱い

**該当箇所**: `kasane/changes/add-layout-spec/ui/brief.md:59`、`kasane/changes/add-layout-spec/tasks.md:37`

**問題点**:
brief.md の照合結果が自認しているとおり「KMP / MAUI は iOS 側で撮影しており、両ルートの Android 側はビルド green までの確認にとどまる」。MAUI については次段落で Pixel 6a による追撮・補完照合が行われ、その過程で実際に2件の見た目の不備 (トグルの色・移動量欄の下線) が見つかって修正されている。**同じ穴が KMP ルートの Android 側には残ったまま**で、`ui/verification/` にも `kmp-*` は iOS 側の4枚しかない。

`samples/kmp/androidApp/` は `samples/android/app/` とは別のソース (`LayoutDialogCardView.kt` / `SampleAlignmentSegmentsView.kt` / `SampleLayoutPanelView.kt` などが本変更で新規追加されている) なので、Android Native ルートの照合結果では代替できない。MAUI Android で実際に不備が出た事実が、この穴の危険度を裏づけている。それでも tasks 6.3 は `[x]` になっている。

**推奨修正**: KMP の androidApp を実機へ配備し、パネル初期と Show 後 (End/End) の2状態を `ui/verification/kmp-android-layout-panel-*.png` として撮って `approved-layout-panel.png` に照合する (MAUI Android と同じ手順)。実施しない判断をするなら、tasks 6.3 のチェックを外すか、照合対象外とする理由を brief.md の照合結果に明記すること。

### [🟡 Minor] iOS の負のコンパイル検査は既定の並列ビルドでは属性検査が空振りする

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md:84`、`ios/Tests/KsDialogsTests/DialogAttributeCompileChecks.swift:37-48`

**問題点**:
規約どおりのコマンド (`xcodebuild build-for-testing … OTHER_SWIFT_FLAGS='$(inherited) -DKSDIALOGS_NEGATIVE_COMPILE_CHECK'`) を実行すると、ビルドは失敗するものの、報告されるエラーは既存の `DialogTypedResultCompileChecks.swift` の2件だけで、**本変更が追加した属性検査 (`DialogAttributeCompileChecks.swift`) のエラーは1件も出ない**。Xcode が最初の失敗を検出した時点で他バッチのコンパイルジョブをキャンセルするためで、ログ上も当該バッチはコマンド本体が空のまま終わっている。

規約は「成功したら検証は失敗である。判定を誤らないよう、**期待するコンパイルエラーが出た箇所まで確認する**」と定めているが、この手順のままでは属性検査が実際に効いているかを判別できない。実装自体は正しく、`-jobs 1` を付けて直列化すると期待どおり2件が出る (実測):

```
DialogAttributeCompileChecks.swift:40:51: error: value of type 'BasicTestDialogViewModel' has no member 'proportionalWidth'
DialogAttributeCompileChecks.swift:45:84: error: extra argument 'options' in call
```

**推奨修正**: test-execution.md の iOS 行に `-jobs 1` を加える (または「他の負の検査と同居するため、属性検査の指摘箇所を確認するときは直列化する」旨を注記する)。MAUI 行と同様に、iOS でも期待するエラー箇所を列挙しておくと空振りに気づける。

### [🟡 Minor] test-execution.md の実測件数と実行手順が本変更後の実態に追随していない

**該当箇所**: `kasane/concepts/cross/conventions/test-execution.md:6, 17-23, 36-46`

**問題点**:
同規約は「テスト構成が育って実態が変わったら本規約を実測で更新する」と自ら定めているが、本変更で全ルートの件数が増えたのに表は 2026-08-15 実測のまま (timestamp も同日)。実測値との差は次のとおり: ios 38 → 55 / android 34 → 40 / kmp 33 → 48 / maui 19 → 26 / maui-bridge 5 → 9。

より実務的な問題として、**Android の instrumented test の実行手順が表にも `android/` 節にも無い**。本変更では「ケース表の全量検証が通る (実 View)」「外側タップ (Android)」「透明オーバーレイ」の受け入れが instrumented test (実機 60 件) に依存しており、`./gradlew test` だけを回すと**この 60 件が1件も走らないまま BUILD SUCCESSFUL になる**。まさに同規約が防ごうとしている「黙って空振りする範囲」に該当する。

**推奨修正**: 表に `android/ (instrumented)` 行 (`./gradlew connectedDebugAndroidTest`、実機/エミュレータ接続と `adb devices` 確認が前提、結果は `ksdialogs/build/outputs/androidTest-results/connected/` の XML) を追加し、5ルート分の件数を実測値へ更新して timestamp を改める。concepts の更新なので、蒸留フェーズでまとめて行う判断でもよい。

### [🔵 Suggestion] MAUI の `DialogOptions` が internal である根拠を残す

**該当箇所**: `maui/KsDialogs.Maui/Internals/DialogOptions.cs:19`

design.md Decision 6 の公開シグネチャ表は MAUI の型として `DialogOptions` / `DialogPlacement` を並べて挙げており、字面だけ見ると両方が公開型に読める。実装は `DialogPlacement` を public、`DialogOptions` を internal にしている。同じ表が MAUI の供給面を「添付プロパティは**スカラー10個** … 内部で2オブジェクトに束ねて Native へ写像」と定めていること、および KMP で「供給経路のない公開型は API 形状の誤り検出を妨げる」として `DialogOptions` を非公開にした ADR-0015 の判断と揃えると、internal は妥当な帰結である (レビューとしても現状の形が良いと考える)。ただしこの読み替えはコードからは辿れないので、`DialogOptions` の `<remarks>` に「供給面は `Dialog.*` の添付プロパティで、この型は Native への輸送単位として internal に保つ」旨を1文足しておくと、後から公開型に「昇格」される事故を防げる。

### [🔵 Suggestion] 互換面の既定値の二重定義に、iOS 側だけパリティテストがない

**該当箇所**: `maui/macios/native/KsDialogsMauiBridge/MauiDialogAttributes.swift:34-56`

`KSDMauiDialogOptions` は既定値 (余白 24 / 比率 -1 / 覆い `0x66000000` / 外側タップ true) を Native の `DialogOptions` と独立に持っており、ズレても気づく仕組みがない。Android 側の互換面には `MauiDialogLayoutPassthroughTests.kt:69` に「何も設定しない属性は Native ライブラリの既定値と同じ値で渡る」というパリティテストがあるが、iOS 側の Swift 互換面には対応するものがない。

実運用では MAUI facade が `AttachedOptions` で常に全項目を埋めて渡すため既定値が使われる経路は無く、実害は無い。とはいえ Android と同じテストを1本置くか、既定値を書かずに `DialogOptions()` から引く形にすると、定義箇所が増えたときの綻びを防げる。

## アクションプラン

1. **🟠 Major (スナップショット時点)** — 実装を初回レイアウトパスへ寄せるか、構築時読みを正としてオーナー合意のうえ deviation.md に記録するかを決める。後者を選ぶ場合は expand-api-surface へ SwiftUI / Compose の供給機構が成立しない旨を申し送る必要があるため、先に方針を確定させること
2. **🟡 Minor (iOS 外側タップ)** — デリゲート述語を実行する検証を1本追加。あわせて「内側タップでは閉じない」を iOS / Android 双方に追加
3. **🟡 Minor (KMP Android 照合)** — 追撮して照合するか、tasks 6.3 のチェックを外して理由を brief.md に残す
4. **🟡 Minor (iOS 負のコンパイル検査 / test-execution.md)** — 4 と 5 は同じ concepts ファイルへの手入れなのでまとめて実施。蒸留フェーズへ送る判断も可
5. **🔵 Suggestion 2件** — 任意。1〜4 の対応時についでに拾える範囲

## 確認して問題がなかった観点

- **旧 VM 経路の残骸**: `DialogLayoutProviding` / `IDialogLayoutProviding` / `MauiDialogLayoutAttributes` / `LayoutTestDialogViewModel` はコード全域から消えている。廃止属性 (明示サイズ・角丸・枠線) の名前が残るのは samples の View 側の意匠のみで、これは ADR-0014 が意図した「View の責務」に一致する
- **オブジェクト単位置換 (フィールドマージ禁止)**: 4ルートすべてに専用テストがあり、フィールド合成した場合に落ちる期待値 (垂直 End と Offset が残らないこと) を明示的に置いている
- **正規化規則**: 比率 (0 以下 → 未指定 / 1 超 → 1 / 非有限 → 未指定)・Offset (非有限 → 0)・Margin (負 → 0 / 非有限 → 既定 24) は iOS / Android で同一のロジックと同一の期待 rect (72, 0, 280, 800) で検証されており、MAUI は「丸めずにそのまま渡す」ことを別テストで固定している。責務分担がコードとテストの両方で一致
- **KMP 公開面**: commonMain の公開型は `DialogPlacement` / `DialogAlignment` のみ。`DialogOptions` が存在しないことと show に options 引数が無いことを負のコンパイル検査が両ターゲットで固定している。`placement` 省略時に既定値オブジェクトへ化けず `null` のまま委譲されること (= 添付が効くこと) も専用テストで押さえてある
- **ケース表**: `core/layout-spec/cases.json` は凍結版と完全一致。C22 (空有効領域で x=300 / w=0) を含む19ケースが iOS の実 frame・Android の実 View rect の両方で green。`approvedDiff` は1件も使われておらず、両 OS が共通期待値で一致している
- **4ルートの契約解釈**: 優先順位 (show > 添付 > 既定)・既定値・enum の写像 (start/center/end/fill)・色の輸送 (ARGB 32bit) が全ルートで一致。MAUI は互換面の既定値と Native 既定値の一致まで (Android 側は) テスト済み。KMP / MAUI とも Native の添付面へ書いてから提示に入るため、Major の読み取り時点の問題の影響を受けない
- **isCanceledOnTouchOutside の実装**: iOS はジェスチャデリゲートで、Android は `DialogContentHolder.isClickable = true` で内側タップを吸収する。false のときイベントが背後へ透過しないことは iOS (hitTest が覆いを返す) と Android (背後にタップ計数用の面を敷いて 0 件を確認) の双方で検証されている
- **実環境の証跡**: 透明オーバーレイは陰性対照 (既定の覆いで -97.55) を取ったうえで差 0.00 を示しており、測定手段の妥当性まで含めて示せている。MAUI hit-test も新経路で描画中心タップを撮り直し済み。Android の色空間差について「数値一致の主張はしない」と限界を明記しているのは誠実な記録
- **足場の凍結**: proposal / design / specs / layout-cases.json は実装期間中に書き換えられていない (tasks.md のチェック更新と、ui/brief.md への「実装時の合意事項」「照合結果」の追記のみ。後者は ui-artifacts.md が brief.md に書くよう定めている内容)
- **tasks.md の虚偽チェック**: 1.1〜7.3 のすべてについて対応する実装・テスト・証跡を確認した。虚偽なし (6.3 のみ上記 Minor の限定つき)
- **コメント規約**: 機械検査 0 件。コメントは ADR ID に加えて内容を自然文で説明しており、ID だけに依存した説明にはなっていない
