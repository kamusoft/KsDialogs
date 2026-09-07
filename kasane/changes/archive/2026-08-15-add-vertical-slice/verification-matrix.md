# 検証対応表 (Requirement × 実行セル)

この変更のデルタスペックの全 Requirement (20 件) を、**6 つの実行セル**のどこで・どの手段で判定するかの対応表。tasks 完了時に**全セルが埋まっていること**をフェーズ完了の判定に使う (tasks 1.2 / 7.5)。

- Requirement の正は [specs/](specs/) 配下のデルタスペック。本表は Requirement 名で参照する (Scenario 単位の網羅は各 Scenario に対応するテストが担い、本表は Requirement × セルの判定手段の割り当てを担う)
- 多段表示のシナリオ ID (MD-a〜MD-d) と同名テスト規約は [共通仕様シナリオ表](common-spec-scenarios.md) が正
- テストの実行方法と件数の確認は [テスト実行規約](../../concepts/cross/conventions/test-execution.md)、実機観測を伴う判定は [実行時挙動の検証規約](../../concepts/cross/conventions/runtime-behavior-verification.md) に従う

## 実行セル

| 略号 | セル | 実体 |
|---|---|---|
| NI | Native iOS | ios/ の Swift 公開 API を直接使う経路 (samples/ios) |
| NA | Native Android | android/ の Kotlin 公開 API を直接使う経路 (samples/android) |
| MI | MAUI iOS | maui/ facade → Bridge → iOS Native (samples/maui の iOS 実行) |
| MA | MAUI Android | maui/ facade → Bridge → Android Native (samples/maui の Android 実行) |
| KI | KMP iOS | kmp/ facade → iosMain actual → iOS Native の互換面 (samples/kmp の iosApp) |
| KA | KMP Android | kmp/ facade → androidMain actual → Android Native (samples/kmp の androidApp) |

## 判定手段の凡例

| コード | 手段 | 意味 |
|---|---|---|
| **A** | 自動テスト | そのセルのビルドルートのテストで判定する。テスト名は対応する Scenario / シナリオ ID を冠する |
| **N** | Native 継承 + adapter 契約テスト | 挙動の実体は Native 実装が持ち、MAUI / KMP は委譲で継承する。継承の成立根拠は adapter 契約テスト (委譲で引数・結果・show 対応が保たれることの検証) に置く。継承が崩れる挙動を観察したらシナリオ表の記録欄に記録する |
| **M** | 手動確認 | 実機・Simulator / エミュレータでの操作で判定する。証跡は `ui/verification/` に保存する |
| **C** | コンパイル検証 | 契約違反 (VM の宣言結果型と異なる受け取り方) が**コンパイルできない**ことを確認する |
| **B** | ビルド検証 | 公開 product の参照だけでビルド・実行が成立することで判定する |
| **—** | 非該当 | そのセルに Requirement が適用されない (理由は「セル注記」に記す) |

セルの表記は `[ ] <コード><タスク番号>` で、コード直後の数字は判定の根拠となる tasks.md のタスク番号。**チェックボックスは実績欄**であり、そのセルの判定が実際に済んだ時点で `[x]` にする (tasks 7.5)。判定が期待どおりにならなかった場合はチェックせず、「実績メモ」に事実を書く。

## 対応表

### dialog-contract (全形態に適用)

| Requirement | NI | NA | MI | MA | KI | KA |
|---|---|---|---|---|---|---|
| 型付き結果の show | `[x] A2.4 C2.4 M7.2` | `[x] A3.3 C3.3 M7.2` | `[x] A5.5 C5.5 M7.2` | `[x] A5.5 C5.5 M7.2` | `[x] A4.6 C4.6 M7.2` | `[x] A4.6 C4.6 M7.2` |
| 契約 interface と既定 singleton の両対応とレジストリ共有 | `[x] A2.4` | `[x] A3.3` | `[x] A5.5` | `[x] A5.5` | `[x] A4.6 N2.4` | `[x] A4.6 N3.3` |
| 結果確定とダイアログの閉鎖 | `[x] A2.5 M7.2` | `[x] A3.4 M7.2` | `[x] N5.5 M7.2` | `[x] N5.5 M7.2` | `[x] N4.6 M7.2` | `[x] N4.6 M7.2` |
| 呼び出しコンテキストの契約 | `[x] A2.4 M7.2` | `[x] A3.3 M7.2` | `[x] A5.5 N5.5 M7.2` | `[x] A5.5 N5.5 M7.2` | `[x] N4.6 M7.2` | `[x] N4.6 M7.2` |
| 結果はちょうど1回だけ確定する | `[x] A2.4` | `[x] A3.3` | `[x] A5.5` | `[x] A5.5` | `[x] A4.6 N2.4` | `[x] A4.6 N3.3` |
| VM 型キーによる View 解決と毎回生成 | `[x] A2.4` | `[x] A3.3` | `[x] A5.5` | `[x] A5.5` | `[x] A4.4 A4.6 N2.4` | `[x] A4.6 N3.3` |
| 多段表示の基本保証 (MD-a / MD-c) | `[x] A2.5` | `[x] A3.4` | `[x] N5.5` | `[x] N5.5` | `[x] N4.6` | `[x] N4.6` |

### ios-native

| Requirement | NI | NA | MI | MA | KI | KA |
|---|---|---|---|---|---|---|
| Swift 公開 API での貫通 | `[x] A2.4 C2.4 M7.2` | — ※3 | — ※3 | — ※3 | — ※4 | — ※3 |
| KMP 委譲向け互換面の提供 | `[x] A2.4` | — ※3 | — ※5 | — ※5 | `[x] A4.4 A4.6` | — ※5 |

### android-native

| Requirement | NI | NA | MI | MA | KI | KA |
|---|---|---|---|---|---|---|
| Kotlin 公開 API での貫通 | — ※3 | `[x] A3.3 C3.3 M7.2` | — ※3 | — ※3 | — ※3 | — ※4 |
| 戻るボタンによるキャンセル (通常時) | — ※6 | `[x] A3.3 M7.2` | — ※6 | `[x] N5.5 M7.2` | — ※6 | `[x] N4.6 M7.2` |

### kmp-facade

| Requirement | NI | NA | MI | MA | KI | KA |
|---|---|---|---|---|---|---|
| commonMain からの show 貫通 | — ※3 | — ※3 | — ※3 | — ※3 | `[x] A4.6 M7.2` | `[x] A4.6 M7.2` |
| Swift 側登録とのキー同一性 | — ※3 | — ※3 | — ※3 | — ※3 | `[x] A4.4 M7.2` | — ※7 |
| Swift async からの直接呼び出し | — ※3 | — ※3 | — ※3 | — ※3 | `[x] A4.5 M7.2` | — ※7 |
| テスト差し替え | — ※3 | — ※3 | — ※3 | — ※3 | `[x] A4.6` | `[x] A4.6` |

### maui-binding

| Requirement | NI | NA | MI | MA | KI | KA |
|---|---|---|---|---|---|---|
| MAUI 公開 API での貫通 | — ※3 | — ※3 | `[x] A5.5 M7.2` | `[x] A5.5 M7.2` | — ※3 | — ※3 |
| 結果経路の platform 非依存検証 | — ※3 | — ※3 | `[x] A5.5` ※8 | `[x] A5.5` ※8 | — ※3 | — ※3 |

### samples

| Requirement | NI | NA | MI | MA | KI | KA |
|---|---|---|---|---|---|---|
| 4ルートの Basic Dialog デモ項目 | `[x] M7.2 (6.1)` | `[x] M7.2 (6.2)` | `[x] M7.2 (6.3)` | `[x] M7.2 (6.3)` | `[x] M7.2 (6.4)` | `[x] M7.2 (6.4)` |
| Sample の consumer 境界 | `[x] B6.1 B7.1` | `[x] B6.2 B7.1` | `[x] B6.3 B7.1` | `[x] B6.3 B7.1` | `[x] B6.4 B7.1` | `[x] B6.4 B7.1` |
| 4ルートのパリティ | `[x] M6.5 M6.6 M7.2` | `[x] M6.5 M6.6 M7.2` | `[x] M6.5 M6.6 M7.2` | `[x] M6.5 M6.6 M7.2` | `[x] M6.5 M6.6 M7.2` | `[x] M6.5 M6.6 M7.2` |

## セル注記

- **※1 (コンパイル検証の担い手タスクが未割当)**: 「型付き結果の show」は型不一致をコンパイル時に排除することを求めており、デルタスペックは**各形態でのコンパイル検証**を要求している。tasks.md に明示項目があるのは NI (2.4) と NA (3.3) のみで、MAUI / KMP 側には無い。**5.4 (C# facade) / 4.1 (commonMain 契約) の実装時に併せて実施**し、そのタスク番号でここを埋める (未実施のままフェーズを完了させない)。KMP 側は 4.6 で実施済みのため KI / KA のセルは `C4.6` に、MAUI 側は 5.5 で実施済みのため MI / MA のセルは `C5.5` に確定した
- **※2 (UI スレッド外からの show の担い手タスクが未割当)**: 「呼び出しコンテキストの契約」の Scenario「UI スレッド外からの show が成立する」に対応するテストが、tasks 2.4 / 3.3 の列挙に無い (実装は 2.2 / 3.2)。**2.4 / 3.3 のテスト実装時に追加**し、番号を確定させる。自動化できない場合は手動確認 + 証跡へ落とし、理由を実績メモに残す
- **※3**: その形態に属さない Requirement (形態固有の公開 API・facade の要求)
- **※4**: KMP セルの公開 API は KMP facade であり、Native の公開 API を直接使わない (Native 実装への到達は委譲経由で、判定は kmp-facade の Requirement が担う)
- **※5**: `@objc` 互換面は KMP iOS からの委譲経路専用 (MAUI は使い捨て Bridge、KMP Android は Kotlin 直接委譲)
- **※6**: iOS に戻るボタンは存在しない (結果通知のルール)
- **※7**: Swift 境界に関する Requirement であり Android ターゲットには適用されない
- **※8**: 素の net10.0 ターゲットのユニットテストで、platform 実装なしに 1 回実行すれば両セルの根拠になる (gateway seam。MI / MA で別々に実行する必要はない)

## 実績メモ

tasks 7.5 で全セルを実績で埋めるときに、次を記入する (チェックだけでは残らない情報の置き場)。

| 日付 | セル / Requirement | 記入内容 |
|---|---|---|
| 2026-08-14 | NI / 呼び出しコンテキストの契約 | ※2 (UI スレッド外からの show) の担い手を tasks 2.4 に確定。自動テスト「UI スレッド外からの show が成立する」で、UI スレッド以外からの呼び出しと View 生成が UI スレッドで行われることを確認。セルは手動確認 (7.2) が残るため未チェック |
| 2026-08-14 | NI / 多段表示の基本保証・結果確定とダイアログの閉鎖 | UIKit の提示遷移は UI シーンを持たないテストランナーで完走しないため、器の出し入れを内部の継ぎ目 (提示面) として切り出し、自動テストは差し替え実装で判定した。UIKit 実装側は提示先の解決 (key window → present の連なりの先端) と提示依頼までを自動テストで確認し、閉鎖の実挙動と重ね提示は 7.2 の手動確認で判定する |
| 2026-08-14 | NI / 型付き結果の show (C2.4) | 負のコンパイル検証を条件つきコンパイルで実施。`OTHER_SWIFT_FLAGS` に `-DKSDIALOGS_NEGATIVE_COMPILE_CHECK` を足した `build-for-testing` が、宣言結果型と異なる受け取り・異なる値での完了報告の2箇所でコンパイルエラーになることを確認 |
| 2026-08-14 | NI / 多段表示 (MD-b 調査ケース) | 差し替えた提示面での挙動は実環境の観測に当たらないため、iOS の実挙動記録は保留。7.2 の手動確認で観測する |
| 2026-08-15 | NA / 呼び出しコンテキストの契約 | ※2 (UI スレッド外からの show) の担い手を tasks 3.3 に確定。自動テスト「UI スレッド外からの show が成立する」で、UI スレッド以外のスレッドからの呼び出しと、View 生成が UI スレッドで行われることを確認。セルは手動確認 (7.2) が残るため未チェック |
| 2026-08-15 | NA / 多段表示の基本保証・結果確定とダイアログの閉鎖 | ダイアログのウィンドウ表示は画面を持たない JVM のテストで完走しないため、器の出し入れを内部の継ぎ目 (提示面) として切り出し、自動テストは差し替え実装で判定した。器そのものは実装のものを組み立てており、外側タップ・戻るボタンからのキャンセル報告は器の実コードで判定している。提示先 (resumed な画面) の追跡は追跡役の自動テストで確認し、覆いへのタップ認識と閉鎖の実挙動は 7.2 の手動確認で判定する |
| 2026-08-15 | NA / 型付き結果の show (C3.3) | 負のコンパイル検証を専用ソースの追加ビルドで実施。`./gradlew compileDebugUnitTestKotlin -Pksdialogs.negativeCompileCheck` が、宣言結果型と異なる受け取りと、異なる値での完了報告の2箇所でコンパイルエラーになることを確認 |
| 2026-08-15 | NA / 戻るボタンによるキャンセル (通常時) | 判定手段に手動確認 (7.2) を追加。自動テストが判定できるのは器のキャンセル報告経路までで、実際の戻るボタン押下からキャンセル通知が届く配線 (キーボード非表示時であること自体を含む) はエミュレータ / 実機の操作でしか観測できないため |
| 2026-08-15 | NA / 多段表示 (MD-b 調査ケース)・MD-d | 差し替えた提示面での挙動は実環境の観測に当たらないため、Android の実挙動記録は保留。MD-d は入力欄を持つ検証用 View も含めて 7.2 の手動確認で観測する |
| 2026-08-15 | KI / Swift 側登録とのキー同一性 (A4.4) | 疎通確認は成立。共有コードで定義した ViewModel のクラスを互換面へ登録キーとして渡し、その ViewModel での show が factory を解決できることを自動テストで確認した。提示先の画面を持たないテストランナーでは表示まで到達しないため、判定は「解決の後に起きる提示先不在の失敗」と「解決に失敗したときの未登録の失敗」の区別で行っている。ObjC クラス名はテスト実行形式では自動生成名 (`Test_kobjcc0` 等) になるため、Swift アプリが framework 越しに見る名前での同一性は 7.2 の手動確認 (samples/kmp) で最終確認する。セルは手動確認が残るため未チェック |
| 2026-08-15 | KI / Swift async からの直接呼び出し (A4.5) | 自動変換の粗を framework の生成ヘッダと Swift の型検査で実測。suspend な show は Swift から `try await dialogs.show(viewModel:)` で呼べ、戻り型は **非 optional の `any DialogResult`**、例外は `throws` (NSError) で届く。一方 **sealed の網羅分岐は失われ** (protocol + 具象クラスになるため `as?` 判別が必要)、**結果型のジェネリクスも消える** (`DialogResultCompleted<AnyObject>` / `value` は `AnyObject?`)。ジェネリクス消失は ObjC 面の制約であり、kmp/ADR-0001 のフォールバック (自前 completion ラッパー) でも解消しないため、フォールバックは発動していない。値が正しく届くかの最終確認は 7.2 の手動確認 (samples/kmp) で行うためセルは未チェック |
| 2026-08-15 | KI / 型消去輸送からの復元 | 互換面が `id` で運ぶ結果値の実測。Kotlin 由来の値も Swift 由来の `NSNumber` も、Kotlin 側では `Boolean` として受け取れることを ObjC コンテナ経由の往復テストで確認した (宣言結果型 Boolean への復元が成立) |
| 2026-08-15 | KI / 復元失敗の検出 (オーナー判断: 互換面での検査) | 検査の位置を委譲元 (共有コード) ではなく互換面に置いた。登録時に宣言結果型 (名前 + 判定手続き) を受け取り、結果報告の時点で値を確かめ、合わない報告は cancelled ではなく失敗の輸送区分で返す。共有コード側ではこれが `DialogException` になり、Swift へは NSError で届く。iOS 側は誤った型・nil を包んだ値・不一致後の再報告の3件を自動テストで確認。KMP 側は宣言結果型の判定が ObjC 境界越しに機能することを自動テストで確認 (提示先の画面を持たないテストでは結果報告まで到達しないため、報告時点の検査そのものは iOS 側のテストが受け持つ) |
| 2026-08-15 | KA / VM 型キーによる View 解決 (A4.6) | Android Native ライブラリの共有レジストリに、共有コードで定義した ViewModel のクラスで factory を登録し、KMP の既定エントリからの show が解決できることを自動テストで確認した。判定方法は KI と同じく提示先不在の失敗との区別による |
| 2026-08-15 | KI / KA / 多段表示・戻るボタン・結果はちょうど1回 (N セル) | 委譲面 (adapter) の契約テストで判定。show 1回につき委譲1回・結果1個であること、ViewModel が包み直されずそのまま Native へ渡ること、重ねた show が各自の結果を受け取ることを確認した。挙動そのものは Native 実装が持ち、KMP は素通しする |
| 2026-08-15 | KI / KA / 型付き結果の show (C4.6) | 負のコンパイル検証を専用ソースの追加ビルドで実施。`./gradlew :ksdialogs-kmp:compileTestKotlinIosSimulatorArm64 -Pksdialogs.negativeCompileCheck` と `... compileAndroidHostTest -Pksdialogs.negativeCompileCheck` が、宣言結果型と異なる受け取りでコンパイルエラーになることを確認 (KMP facade は登録面を持たないため、報告値の不一致の検証は Native 2実装が受け持つ) |
| 2026-08-15 | MI / MA / 型付き結果の show (C5.5) | 負のコンパイル検証を専用ソースの追加ビルドで実施。`dotnet build KsDialogs.Maui.Tests -p:KsDialogsNegativeCompileCheck=true` が、宣言結果型と異なる受け取り (CS0029)・異なる値での完了報告 (CS1503)・宣言と異なる結果型の報告口を要求する登録 (CS0311) の3箇所でコンパイルエラーになることを確認 |
| 2026-08-15 | MI / MA / 呼び出しコンテキストの契約 | 自動テスト「UI スレッド外からの show が成立する」で、ワーカースレッドからの呼び出しが成立することを確認。提示先不在は委譲面が失敗を返す経路 (`HostlessDialogGateway`) で判定し、View が生成されないことも確認した。UI スレッドへのマーシャリング自体は委譲面の実装 (Swift / Kotlin の互換面) が受け持つため素の net10.0 では観測できず、実挙動は 7.2 の手動確認で判定する。セルは手動確認が残るため未チェック |
| 2026-08-15 | MI / MA / 多段表示・結果確定と閉鎖・戻るボタン (N セル) | 委譲面 (adapter) の契約テストで判定。show 1回につき委譲1回・結果1個であること、委譲面へ渡る中身と結果チャネルがその show のものであること、重ねた show が各自の結果を受け取ることを確認した。挙動そのものは Native 実装が持ち、MAUI は platform view 化と結果チャネルの結び付けだけを行う。閉鎖の実挙動は 7.2 の手動確認で判定する |
| 2026-08-15 | MI / MA / ビルド連携 (tasks 5.1・5.2) | iOS は標準 `XcodeProject` アイテムで成立 (xcframework が device / simulator 両スライスで生成され、`NativeReference` として登録されることを manifest と resources.zip で確認)。Android は標準 `AndroidGradleProject` アイテムが不成立で、gradlew の直接呼び出しへフォールバックした。不成立の原因は SDK が生成する init script が Gradle 9 系の Kotlin DSL でコンパイルできないことで、単一モジュールの `android/` に同じ init script を適用しても同じ失敗になることを確認済み (maui/ADR-0003) |
| 2026-08-15 | NI / NA / KI / KA / Sample の consumer 境界 (tasks 6.1・6.2・6.4) | 公開 product の参照だけでビルド・実行・動作が成立することを実測。Native iOS は Local Swift Package、Native Android は composite build + `dependencySubstitution` 明示、KMP は composite build (shared が KMP facade を消費) で、いずれも Basic Dialog の起動 → 表示 → 完了 / キャンセル → 結果表示まで到達した。セルは 7.1 (BuildProbe 削除後の全ルートビルド) が残るため未チェック |
| 2026-08-15 | MI / MA / Sample の consumer 境界 (tasks 6.3) | ビルドは成立 (facade への ProjectReference 1本、`net10.0-android;net10.0-ios` 両 TFM)。当初は実行時に両 OS でダイアログの表示に至らなかった (iOS = 覆いだけが出て中身が寸法 0 のまま描かれない / Android = 中身の生成時に `MauiMaterialButton` のテーマ要求違反でプロセスごと落ちる)。**提示経路の修正 (画面スコープの文脈のみを使う / 測定・配置を仲介する器で包む) の後に再確認し、両 OS とも 起動 → 表示 → OK / キャンセル → 結果表示 まで成立**。セルは 7.1 (BuildProbe 削除後の全ルートビルド) が残るため未チェック |
| 2026-08-15 | MI / MA / 4ルートの Basic Dialog デモ項目 (再照合) | 修正後の実測。MAUI iOS は完了で `結果: completed(true)`、キャンセルで `結果: cancelled` を確認。MAUI Android も同じ2経路を確認し、logcat に未処理例外なし。証跡は `ui/verification/maui-ios-*.png` / `maui-android-*.png`。修正前の記録は `maui-ios-dialog-before-fix.png` に残した |
| 2026-08-15 | KI / Swift 側登録とのキー同一性 (tasks 6.4) | **実 framework 越しでの最終確認が成立**。共有モジュールが出す静的 framework で ObjC export 名は `SampleSharedBasicDialogViewModel` (Swift 名 `BasicDialogViewModel`) になり、Swift 側で `BasicDialogViewModel.self` を登録キーに渡した factory が、共有 Presenter からの show で解決された。結果値も Swift の `true` (NSNumber) が共有コード側で `Boolean` に復元され、`結果: completed(true)` が画面に出ることを確認。A4.4 で残していた「テスト実行形式では自動生成名になる」懸念は解消 |
| 2026-08-15 | NI / 型付き結果の show・呼び出しコンテキストの契約 (レビュー指摘の修正) | 器が提示の連なりから外れたのに未確定なら cancelled で確定させる手当てを入れ、MD-b で観測された宙吊り (show が返らない) を解消した (上に別の画面が全画面で重なっただけの場合は確定させない — 判定は提示関係の解除が済む次の機会に行う)。併せて呼び出し元の Task キャンセルでも同じ経路で cancelled 確定 + 器の閉鎖を行うようにし、提示起点の window は前面でアクティブなシーンの key window のみを採用する (無ければ提示先不在の失敗) よう絞った。ios の自動テストは 27 件 → 35 件。**手当て後の Simulator 実機再観測は未実施**で、次の手動確認の機会に MD-b の上の1枚が cancelled で返ることを確認する (→ 2026-08-15 に再観測済み。下の行を参照) |
| 2026-08-15 | 全セル / 4ルートのパリティ (tasks 6.5・6.6) | 文言・メニュー構成・SampleTheme の RGBA はコード上で4ルート一致を確認 (`samples/README.md` に一覧)。画面での突き合わせは**6セル全部で完了**し、証跡と照合結果を `ui/verification/` に保存した (承認モック mock-b に対し構造・トークン・意図が一致)。外側タップ・戻るボタン・多段表示の見え方と、一部セルのキャンセル経路は 7.2 送り |
| 2026-08-15 | MI / MA / 呼び出しコンテキストの契約 (レビュー指摘の修正後) | 提示先の解決を UI スレッドへ移した後、**ワーカースレッドからの show** を実機で実測。samples/maui を一時的に `Task.Run` から show する形にして iOS Simulator (iPhone 17 Pro / iOS 26.4) と Android Emulator の双方でダイアログが正しく表示されることを確認し、一時変更は撤去した。素の net10.0 側は「facade は呼び出しスレッドを変えずに委譲する」ことをテストで固定している (UI スレッドへ移すのは委譲面の責務) |
| 2026-08-15 | MA / 結果はちょうど1回だけ確定する (Android 互換面の失敗経路) | 互換面の閉鎖通知に catch-all を追加し、Kotlin ユニットテスト5件で「完了 / キャンセル / 提示先不在 / 中身の生成の例外 / コルーチンのキャンセル」のすべてでちょうど1つの通知が届く (キャンセルは通知に変換せず伝播する) ことを確認。実行は `cd maui/android/native && ./gradlew :ksdialogs-maui-bridge:test` (5 tests / 0 failures) |
| 2026-08-15 | KI / iOS deployment target (tasks 4.7) | 生成物の実測: `iosArm64` の release framework と `iosSimulatorArm64` の debug framework がいずれも `minos 17.0` を宣言することを確認 (非保証フラグを外すと `minos 15.0` になるため、宣言はこのフラグに依存している)。Kotlin 2.4 の Gradle DSL に deployment target の第一級指定は存在せず、正統な代替手段は見つからなかった。Swift パッケージ側の最低対象 OS は公開 DSL (`iosMinimumDeploymentTarget`) で 17.0 を指定できている |

| 2026-08-15 | 全セル / Sample の consumer 境界 (tasks 7.1) | BuildProbe (4ルート計8ファイル) を削除し、全ビルドルートのビルドと全件テストが通ることを確認。件数は ios 27 (Swift Testing 27 + XCTest 0)・android 23・kmp 31 (iosSimulatorArm64 16 + androidHostTest 15)・maui 18 で、いずれも削除前の値から BuildProbe ぶん (kmp は common のため 2 ターゲット分の 2 件) 減っており整合する。4ルートの Sample も BuildProbe 削除後の状態でビルド・インストール・実行まで成立したため、B セルをチェックした |
| 2026-08-15 | 全セル / 手動確認 (M7.2) の範囲 | 6セル全部で 表示 / 完了 (OK) / キャンセルボタン / 外側タップ を実機 (Simulator・エミュレータ) 操作で確認し、Android 3セルは戻るボタンも確認した。判定は「直前の結果を completed(true) にしてから操作し `結果: cancelled` へ遷移すること」まで撮って行った。証跡と一覧は `ui/brief.md`「最終照合の記録 (tasks 7.2)」。**「呼び出しコンテキストの契約」の手動確認が覆うのは、実アプリの経路で View が UI スレッド上に生成され表示・閉鎖まで到達することまで**であり、**UI スレッド外からの show 自体は自動テスト (A2.4 / A3.3 / A5.5) が根拠**である (Sample は UI スレッドから show を呼ぶため)。この区分けを承知のうえでセルをチェックした |
| 2026-08-15 | NI / NA / 多段表示の基本保証 (MD-a / MD-c の実環境観察) | 自動テストで提示面を差し替えていた分を実環境で裏取りした。MD-a・MD-c とも iOS / Android で同じ観察結果 (上だけ閉じる / 手前だけ cancelled で下は未確定) になり、OS 差はなかった。観察には samples への一時的な変更 (2枚重ね表示・入力欄つき検証用 View) を使い、**観測後に原本へ戻して samples 全 60 ファイルのチェックサム一致を確認済み**。詳細はシナリオ表の記録欄 |
| 2026-08-15 | MI / MAUI iOS のボタン当たり判定 (観察された差) | MAUI iOS のダイアログ内ボタンは、描画上の中心 (261,480 pt) への合成タップでは反応せず、上端寄り (280,470 pt / 140,470 pt) で反応した。**BuildProbe 削除前のビルドでも同じ**で本変更による退行ではない。カード本体へのタップは覆いへ抜けない (外タップ扱いにならない) ため、器の当たり判定ではなく中身の配置・測定側の疑い。セルの判定 (完了 / キャンセル経路の成立) 自体は満たすためチェックしたが、**レイアウト仕様化 (phase-5) で当たり判定込みの確認が要る** |
| 2026-08-15 | NI / 結果はちょうど1回だけ確定する・多段表示 (レビュー修正後の実機再観測) | iPhone 17 Simulator (iOS 26.5) で 3 点を実測。(1) **MD-b**: 下の 1 枚の notifier へ先行報告すると 2 枚とも消え、**上の show は cancelled で確定して返る** (手当て前の宙吊りは解消)。(2) **MD-a 順閉じ**: 2 枚重ねて上を OK で閉じると `上:completed(true)` だけが確定し、**下は表示されたまま未確定**で、続けて下をキャンセルすると `下:cancelled` になる (手当てによる巻き添えの誤 cancelled は起きない)。(3) **全画面提示の巻き添え防止**: ダイアログ表示中に利用者コード相当の `.fullScreen` 提示を重ね、閉じた後もダイアログは残って結果は未確定のままで、その後の OK 操作が `completed(true)` で返る。証跡 `ui/verification/ios-native-md-b-recheck-*.png` / `ios-native-md-a-recheck-*.png` / `ios-native-fullscreen-overlay-*.png`。**review-002 の Suggestion (「閉じられた側で提示関係が解ける」前提が実 UIKit で未検証) は (1) の観測で実証**された。ios の自動テストは 35 件全通過 (9 suites) |
| 2026-08-15 | NA / 結果はちょうど1回だけ確定する (器の閉鎖通知の実測) | **期待と相違**。Android Emulator API 35 (1080x2340) で、ダイアログ表示中に画面回転 (`settings put system user_rotation`) を起こして Activity を作り直させたところ、**ダイアログは画面から消えるが show は完了しなかった** (プロセスは同一のまま。結果表示も出ず、観測用のログにも完了行が出ない)。logcat には `android.view.WindowLeaked: Activity ... has leaked window ...[MainActivity]` が出ており、Activity の破棄ではウィンドウが取り除かれるだけで `Dialog.dismiss()` は呼ばれない → **`setOnDismissListener` が発火しない**。つまり器の閉鎖通知は「ライブラリ自身が閉じる」経路しか通らず、**画面の破棄という外の要因では未確定のまま残る**。証跡 `ui/verification/android-native-rotation-dialog-shown.png` / `android-native-rotation-after-rotate-unresolved.png`。観測には samples/android への一時変更 (画面の作り直しで打ち切られない場所から show を待ち、結果をログと再表示に流す) を使い、観測後に撤去済み。**修正は行っていない** (オーナー / phase 判断待ち) — ※この相違はその後の修正サイクル2周目で手当てされ、次行の再観測で解消を実測済み |
| 2026-08-15 | NA / 結果はちょうど1回だけ確定する・多段表示 (手当て修正後の再観測) | 上の相違に対する修正 (画面破棄の購読 + 自前の閉鎖 + ウィンドウ取り外しの検知) の後、同じ環境 (Android Emulator API 35) で 4 点を実測し、**すべて期待どおり**。(1) **回転**: ダイアログ表示中に画面回転させると **show が cancelled で完了**し、作り直された画面に `結果: cancelled` が出る。`WindowLeaked` の警告は**出なくなった** (0 件)。(2) **MD-b**: 2 枚重ねて下の show の notifier へ先行報告すると、**下だけが閉じて completed(false)、上は残って後から OK で completed(true)** — 手当て前の実測と同じで、Android の MD-b 挙動は変わっていない。(3) **通常経路の回帰**: 表示 → OK (`completed(true)`) / キャンセルボタン / 外側タップ / 戻るボタン (いずれも `cancelled`) の 4 経路が正常で、show 1回につき完了は1回だけ (ログ 4 行 / 二重確定なし)。(4) **上を先に閉じる回帰**: 2 枚重ねて上を OK で閉じても**下は未確定のまま残り**、その後の下への報告が completed(false) で届く (巻き添えの誤 cancelled なし)。証跡 `ui/verification/android-native-rotation-recheck-dialog-shown.png` / `android-native-rotation-recheck-cancelled.png` / `android-native-md-b-recheck-stacked.png` / `android-native-md-b-recheck-lower-first.png` / `android-native-md-b-recheck-upper-completed.png` / `android-native-recheck-normal-paths.png` / `android-native-md-a-recheck-lower-survived.png`。観測用の一時変更は撤去し、samples 配下 102 ファイルのチェックサム一致を確認済み |

記入の指針:

- 委譲経由で Native の挙動の継承が崩れた場合 (N セルの前提が成立しない場合) は、**シナリオ表の「MAUI / KMP での差」欄にも記録**し、本表のセルはチェックせずここに事実を書く
- 判定手段を計画から変更した場合 (自動テスト → 手動確認など) は、変更後のコードとタスク番号でセルを書き換え、理由をここに書く
- ※1 / ※2 の未割当セルは、担い手タスクが確定した時点でセル表記をそのタスク番号に更新する
