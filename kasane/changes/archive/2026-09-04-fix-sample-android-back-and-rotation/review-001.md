# レビュー結果: fix-sample-android-back-and-rotation (001 回目)

**日付**: 2026-09-04
**判定**: APPROVED

## サマリー

合意済みスコープ (`exploration.md` の決定事項 論点1=案B / 論点2=案A) に対して、実装は過不足なく一致している。`ComponentActivity` + `OnBackPressedCallback` への移行、`onBackPressed` の撤去、両ルートの `configChanges` 集合 (MAUI Android Sample と同一) はいずれも確認でき、`GestureBackNavigation` の lint 指摘は baseline ではなく実装で解消されている。依存追加も共有カタログの既存方針に沿い、宣言済みの `androidx-lifecycle` / `androidx-savedstate` を押し上げないことを解決グラフで実測確認した。指摘は証跡の記録精度に関する Minor 2 件と Suggestion 5 件で、実装コードの欠陥は見つからなかった。

## 実行した検証

| 対象 | コマンド | 結果 |
|---|---|---|
| Android Sample | `samples/android` で `./gradlew --offline :app:assembleDebug :app:lintDebug` | BUILD SUCCESSFUL。lint 17 件 (`AndroidGradlePluginVersion` 2 / `MissingApplicationIcon` 1 / `ViewConstructor` 14) — いずれも既存分で、`GestureBackNavigation` は消えている |
| KMP Sample (Android) | `samples/kmp` で `./gradlew --offline :androidApp:assembleDebug :androidApp:lintDebug` | BUILD SUCCESSFUL。lint 15 件 (`MissingApplicationIcon` 1 / `ViewConstructor` 14)。同じく `GestureBackNavigation` なし |
| 本体 (共有カタログの巻き添え確認) | `android` で `./gradlew --offline test` | 67 tests / 0 failures (規約の実測値と一致) |
| 依存解決 | `samples/android` で `:app:dependencies --configuration debugRuntimeClasspath` | `androidx.activity:activity:1.10.1` / `lifecycle-runtime:2.8.7` / `savedstate:1.2.1` — カタログの宣言値は据え置き |
| 標準 lint | `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` (未追跡の `verification/` は `--paths` で明示) | いずれも違反 0 件 |
| Compose 登録経路 (Suggestion 4 の裏取り) | API 36 実機で 2 ルートとも `--es demo declarative-dialog` を起動 | 両ルートとも Declarative Dialog が正常に描画。`ComponentActivity` 化による影響なし (画像はスクラッチ保管、証跡化はしていない) |

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 追加コメントに作業文書パス・変更 ID・履歴記述なし。適合
- `kasane/handbook/cross/sample-parity.md` (`samples/**` を触るため) — 文言・色トークン・デモ項目の構成に変更なし。2 ルートの `MainActivity` はコード共有なしで同形 (追加ブロックは字面一致を確認)
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動の不具合修正の完了判定) — 修正前の症状再現 (`verification/android-a36-before.log`)・修正後の同一手順での解消・証跡の change 配下保管の 3 点を満たす
- `kasane/handbook/cross/test-execution.md` (テスト実行・完了判定) — 上表のとおり件数まで確認
- `kasane/lessons/process.md` L-001 (姉妹面の横断照合) — 「targetSdk 36 の予測型戻りでパネルの戻る導線が効かない」という穴について、残る姉妹面 `samples/maui` を読み取りで照合した。MAUI 側はパネルを `Navigation.PushModalAsync` で開いており (`samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs:434,442`)、戻るの受け口は自前ではなくフレームワーク側にある。自前の `OnBackPressed` / `OnBackButtonPressed` は 1 つも無く、同型の穴は存在しない
- 関連 ADR: `android/ADR-0001` (本体の Compose 非依存) — `androidx.activity` は本体の依存グラフに入らず、`:ksdialogs:verifyNoDeclarativeUiDependency` を含む `android/` の `test` も通過。抵触なし

## 指摘事項

### [🟡 Minor] kmp-a36 の項目3 だけログの遷移先が他3本と違い、README に説明がない

**該当箇所**: `verification/kmp-a36.log` (「戻る2回目」の行) と `verification/README.md` の結果表・項目 3

**問題点**: 他 3 本のログは「戻る2回目」で `com.google.android.apps.nexuslauncher/.NexusLauncherActivity` へ遷移しているが、`kmp-a36.log` だけ `jp.kamusoft.ksdialogs.samples.android/.MainActivity` になっている。README の結果表は 4 列すべて「終了」と書いており、この 1 本だけ形が違う理由が本文のどこにも書かれていない。kmp Sample が前面から消えている以上「アプリが終了した」という主張自体は成立するが、証跡だけを読む立場 (レビュー・蒸留) からは「ランチャーに戻っていない = 終了していない」と読めてしまい、結果表を裏付けているか判断できない。

**推奨修正**: README の「補足」に 1 行足す — 同一端末に android Sample のタスクが残っていたため、kmp Sample の終了後に前面化したのがランチャーではなく android Sample だった旨。あるいは android Sample のタスクを落としてから撮り直し、4 本の形を揃える。

### [🟡 Minor] kmp ルートの回転証跡 (06 / 07) が android の 04 / 05 とバイト同一で、画像としては裏付けになっていない

**該当箇所**: `verification/06-kmp-a36-dialog-landscape.png` / `verification/07-kmp-a33-dialog-landscape.png`

**問題点**: md5 を取ると 04 と 06、05 と 07 がそれぞれ完全に同一のファイルだった。README はこれを「パリティどおり描画が同一で、差は落とした帯の時計だけだったため」と開示しており隠していないが、結果として **kmp ルートを名乗る 2 枚が android ルートの画像と区別できない**。画像を証跡として残す目的は主張の裏付けなので、区別できないものを別名で 2 枚置くと、後から見た人が「kmp でも撮った」と誤読するか、逆に取り違えを疑うかのどちらかになる。

**推奨修正**: 次のいずれか。(a) 06 / 07 を削除し、README の証跡表に「kmp ルートの項目 5 の裏付けはログ (`kmp-a36.log` / `kmp-a33.log`)。画像は android と画素まで同一のため置かない」と書く。(b) ルートが判別できる状態 (結果表示エリアに値が出ている等) を含めて撮り直す。

### [🔵 Suggestion] handleOnBackPressed に else が無く、旧実装が持っていた OS 既定への逃げ道が消えている

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/MainActivity.kt:32-39`、`samples/kmp/androidApp/src/main/kotlin/jp/kamusoft/ksdialogs/samples/kmp/android/MainActivity.kt:35-42`

**問題点**: 撤去された `onBackPressed` は、どちらのパネルも非 attach なら `super.onBackPressed()` に落ちていた。新しい callback には else が無いため、`isEnabled == true` かつ両パネルとも非 attach という状態になると戻るが無反応になる (画面から出られない)。パネルの開閉が `openLayoutPanel` / `openTransitionPanel` / `closeLayoutPanel` / `closeTransitionPanel` の 4 経路に閉じているので現状の遷移では到達しないが、「有効かどうか」を `isEnabled` が持ち「どちらのパネルか」を `isAttachedToWindow` が持つ二重管理になっている以上、パネルを開く経路が増えたときに食い違いが起きうる。

**推奨修正**: どちらも非 attach の場合に `isEnabled = false` にしてから `onBackPressedDispatcher.onBackPressed()` へ流し直すか、開いているパネルを 1 つのプロパティで持って `isEnabled` をそこから導出し、食い違い自体を作れなくする。2 ルート同形で入れること。

### [🔵 Suggestion] Compose 登録経路 (Declarative Dialog) が結果表に無い

**該当箇所**: `verification/README.md` の結果表、`exploration.md` の未決の論点「実装時に確認: … Compose 登録経路に影響が出ないこと (基底クラス非依存のはずだが実機で踏む)」

**問題点**: 証跡が踏んでいるデモは `basic-dialog` と `layout-dialog` だけで、`ksdialogs-compose` を通る Declarative Dialog は踏まれていない。`ComponentActivity` は plain `Activity` と違って自身が LifecycleOwner / SavedStateRegistryOwner になり、`setContentView` 時に ViewTree owner を据えるため、owner の解決先が変わりうる箇所として探索が名指ししていた項目である。

**本レビューで実測済み**: API 36 実機で 2 ルートとも `--es demo declarative-dialog` を起動し、どちらも Declarative Dialog が正常に描画されることを確認した。コード側の理由も確認済みで、`android/ksdialogs-compose/src/main/kotlin/jp/kamusoft/ksdialogs/compose/DialogComposeContentView.kt:93-101` がダイアログのウィンドウ根に自分を owner として据え直し、`onDetachedFromWindow` で元の owner へ戻すため、提示元 Activity の基底クラスに依存しない。**修正作業は不要**。

**推奨修正**: 結果表に「Declarative Dialog (Compose 登録経路) が表示される」の行を足し、確認済みであることを記録に残す。

### [🔵 Suggestion] 回転の手順が撮影レシピに無い

**該当箇所**: `kasane/config.yaml` の `ui.screenshot` (Android エミュレータ / 実機の節)

**問題点**: レシピには「戻る」(`input keyevent 4`) はあるが回転の手順が無く、今回の検証は README 側に手順を書き起こしている。回転絡みの撮影は今後も出る (4 ルート揃った以上、回転後の並べ比較が可能になった)。

**推奨修正**: 蒸留時に `ui.screenshot` の Android の節へ、`settings put system accelerometer_rotation 0` → `user_rotation 1` と観測後の原状復帰を 1 行で足す。

### [🔵 Suggestion] 端末設定の原状復帰が事後に検証できない

**該当箇所**: `verification/README.md`「手順」節の「回転の設定 … は観測後に元の値へ戻してある」

**問題点**: 記録が「戻した」だけで、戻す前の値が残っていない。実際に確認したところ API 36 端末は `accelerometer_rotation=1`、API 33 端末は `0` と非対称で、後者が元からそうだったのか戻し漏れなのかを証跡から判定できない (端末側の常設設定である可能性が高く、戻し漏れと断ずる材料はない)。sample-parity の「観測用の一時改変を戻さないまま終える」を禁じる規律と同じ考え方が端末設定にも要る。

**推奨修正**: 変更する設定は「変更前の値 → 設定値 → 復帰後の値」で記録する。今回は API 33 端末の元の値を実施者が覚えていれば README に補記すれば足りる。

### [🔵 Suggestion] 蒸留への申し送り: 利用者向け写像の追随

**該当箇所**: `samples/README.md:42`、`kasane/handbook/cross/sample-parity.md`

**問題点**: `samples/README.md:42` はパネルからメニューへ戻る導線を `‹` だけで説明しており、端末の戻るでも戻ることが書かれていない。sample-parity 側も「OS 操作 (戻る・回転) への反応」を一致対象として持っていない。

**推奨修正**: `exploration.md` の未決の論点で蒸留時の作業として予定済みのため、そのまま蒸留で扱えばよい (本変更での対応は不要)。

## 確認した観点 (指摘に至らなかったもの)

- **決定事項との一致**: 両ルートの `AndroidManifest.xml` は互いに完全一致で、`configChanges` の集合 `orientation|screenSize|screenLayout|smallestScreenSize|uiMode|density` は `samples/maui/KsDialogs.Sample.Maui/Platforms/Android/MainActivity.cs` の `ConfigurationChanges` と同一。`onBackPressed` の残骸なし
- **callback の有効/無効の切り替え漏れ**: パネルを閉じる経路は 2 ルートとも `closeLayoutPanel` / `closeTransitionPanel` の 2 本だけで、ヘッダの `‹` (`SampleLayoutPanelView` / `SampleTransitionPanelView` の `onBack`) もそこへ集約されている。`isEnabled` の立て下ろしはこの 4 箇所に過不足なく入っている。`addCallback` は `autoPlay` より前に置かれており、起動引数でパネルを直接開くデモでも受け口が先に立つ
- **sample-parity**: 追加された callback ブロック 15 行は 2 ルートで字面一致 (`diff` 実施)。両ルートともコード共有はしておらず各自持ちの原則を守っている。文言・色トークン・デモ項目の増減なし
- **依存の判断**: `androidx.activity` を共有カタログへ足す形は、`samples/android/settings.gradle.kts` / `samples/kmp/settings.gradle.kts` が明示する「Sample だけ解決版が上がるのを避けるため本体のカタログを共有する」方針そのまま。`libs.androidx.activity` の参照は 2 つの Sample の `build.gradle.kts` だけで、本体 `android/ksdialogs` は参照していない。版 1.10.1 の選定理由 (宣言済み lifecycle 2.8.7 / savedstate 1.2.1 を押し上げない) も解決グラフで裏が取れた
- **証跡の個人情報**: README・ログとも端末シリアルは `<DEVICE-A33>` / `<DEVICE-A36>` のプレースホルダ。PNG 9 枚を目視し、ステータスバーは帯ごと落ちていて通知・アカウント・端末名の写り込みなし。ローカル絶対パスも無し
- **証跡の置き場**: `verification/` は ksn-core の媒体ホワイトリスト (`evidence/`) とは名前が違うが、本プロジェクトは `kasane/config.yaml` の `lint.exclude` に `kasane/**/verification/**/*.log` を持ち、アーカイブ済み変更も 5 件が同名を使っている既存慣行のため、逸脱として扱わない
- **回転後の見え方**: `08` / `09` を拡大確認。横向きでパネルのヘッダ・行・トグルが崩れず再レイアウトされている。`onConfigurationChanged` の追加対応が不要という README の記述と一致

## アクションプラン

1. (Minor) `verification/README.md` に kmp-a36 の項目 3 の遷移先が他と違う理由を 1 行補記する
2. (Minor) `verification/06-kmp-a36-dialog-landscape.png` / `07-kmp-a33-dialog-landscape.png` を削除して証跡表を書き換えるか、ルートが判別できる状態で撮り直す
3. (Suggestion) 結果表に Declarative Dialog の行を足す (実測はレビュー側で済んでおり、記録の追加のみ)
4. (Suggestion) `handleOnBackPressed` の else 欠落を 2 ルート同形で塞ぐ
5. (Suggestion) 端末設定の変更前の値を README に補記する
6. (蒸留で) `kasane/config.yaml` の `ui.screenshot` に回転手順、`samples/README.md` と sample-parity に「OS 操作への反応」を追記する
