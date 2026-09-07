# 検証結果: add-sample-capture-automation (001 回目)

**日付**: 2026-08-27
**判定**: INVALID (❌ 2 件 — いずれも証跡・付随差分の側。実装とデルタスペックの一致そのものは全 Scenario で成立)

## 検証対象

- デルタスペック: `specs/samples/spec.md` (CA-SA-01〜07。Requirement 4 件 / Scenario 7 件)
- `specs/capture-tooling/spec.md` は 2026-08-27 のオーナー合意改訂で削除済みのため対象外 (CA-CT 系 Scenario は存在しない)
- 実装: `samples/` 4ルート6アプリのデモ駆動モード
- 証跡: `evidence/four-route-walkthrough/` (notes.md + PNG 21 枚) / `verification/parity-check.md`
- `deviation.md` の 3 項目は合意済み差分として扱った

## 対応表

「テスト」欄は本 change の検証割り付け (design.md Decision 5) に従い、自動テストではなく**証跡**を記す。CA-SA 系は `scripts/scenario-id-coverage.py` の `DEFAULT_ALLOW_MISSING` に 7 件とも理由付きで登録済み。

### Requirement: 撮影支援設定の起動引数受け口 (ADDED)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| [CA-SA-01] 引数なしの通常起動は不変 | android `samples/android/app/src/main/kotlin/jp/kamusoft/ksdialogs/samples/android/SampleCaptureOptions.kt:28`(値なしなら全項目 null) / ios `samples/ios/KsDialogsSample/SampleCaptureOptions.swift:45` / maui `samples/maui/KsDialogs.Sample.Maui/SampleCaptureArguments.cs:13` + 各 partial / kmp `samples/kmp/shared/src/commonMain/kotlin/jp/kamusoft/ksdialogs/samples/kmp/SampleCaptureOptions.kt:37`。いずれも受け口が null を返すだけで画面に何も足さない | `evidence/four-route-walkthrough/ca-sa-01-noarg-menus.png` (6アプリのメニューを実見。題字 `KsDialogs Sample` と9項目の文言・並びが一致、撮影支援由来の表示なし) / `ca-sa-01-manual-demo.png` | ✅ 一致 |

キー名 (`demo` / `loading-step-interval-ms`) の4ルート同一と外部表現 (iOS = `--キー 値` 隣接トークンペア / Android = 同名 string extra) は `verification/parity-check.md`「キー名 (2件)」で突き合わせ済み。異常系 (値なし・空文字・検証不通過はキー単位で無視) も4ルートのコードで確認した。

「同じキーが複数回現れた場合は最初の1組を採用する (SHALL)」は iOS 系のみ成立 (`firstIndex(of:)` / `Array.IndexOf` による最初のインデックス採用)。Android 系は Intent extra が Bundle 段階で1値に畳まれるため後勝ちで、`deviation.md:3` に記録済み → ⚠️ deviation 記録済み。ただし当該記録は「**暫定記録 — オーナー確認待ち**」の状態にある (後述)。

### Requirement: 指定デモの自動再生 (ADDED)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| [CA-SA-02] 各デモ ID の自動再生 | 安定デモ ID 9件の定義: android `SampleDemoId.kt:8` / ios `samples/ios/KsDialogsSample/SampleDemoId.swift:4` / maui `samples/maui/KsDialogs.Sample.Maui/SampleDemoId.cs:39` / kmp `samples/kmp/shared/.../SampleDemoId.kt:8` — 4ルートとも spec の表と同じ9件・同じ文字列・同じ並び。ディスパッチ: android `MainActivity.kt:64` の網羅 `when` / ios `SampleMenuScreen.swift` の `autoPlay()` / maui `SampleMenuPage.xaml.cs` の `PlayAsync` / kmp は `SamplePresenter.kt` の `autoPlay` + OS UI 層 (Inline・パネル系)。いずれもメニュー項目のタップハンドラと同じ入口を呼ぶ | `ca-sa-02-autoplay-{ios,maui-ios,kmp-ios,android,maui-android,kmp-android}.png` (6枚)。ios の1枚を実見し、9デモそれぞれが spec の表どおりの起動直後状態 (ダイアログ7件のうち5件・パネル2件・ローディング2件) になっていることを確認 | ✅ 一致 |
| [CA-SA-03] 定義外 ID の無視 | android `SampleDemoId.from()` (`SampleDemoId.kt:22`) が null / ios `SampleDemoId(rawValue:)` が nil / maui `SampleDemoIds.From()` (`SampleDemoId.cs:55`) が null / kmp 共有 `SampleDemoId.from()`。null なら自動再生を呼ばない | `ca-sa-03-unknown-id.png` (6アプリに `no-such-demo` を渡し、通常メニューのまま) | ✅ 一致 |
| [CA-SA-07] 画面再生成で再発火しない | ios `samples/ios/KsDialogsSample/SampleCaptureAutoPlay.swift:6` の `isConsumed` / maui `samples/maui/KsDialogs.Sample.Maui/SampleCaptureAutoPlay.cs:8` の `s_consumed` / kmp 共有 `SampleCaptureAutoPlay.kt:9` の `isConsumed` / android は `MainActivity.kt:279` の companion `autoPlayConsumed` + `savedInstanceState == null` (`MainActivity.kt:44`)、kmp-android も `savedInstanceState == null` で consume | `ca-sa-07-maui-android-recreate.png` を実見 (フォント倍率変更で Activity 再生成 → `直近の結果` が消える一方でダイアログは再表示されない)。`ca-sa-07-android.png` / `ca-sa-07-kmp-android.png` は回転による再生成。iOS 3アプリは実地証跡がなく構造的保証 (notes.md「CLI から再生成を起こせなかった経路」・tasks.md 5.3 に明記) | ⚠️ 実装は一致。証跡に欠落あり (❌-1・後述) |

### Requirement: Loading 刻み間隔の起動時指定 (ADDED)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| [CA-SA-04] 刻み間隔の延長 | 受理範囲 `1..600000` を4ルートで共有 — android `SampleCaptureOptions.kt:25,30-32` / ios `SampleCaptureOptions.swift:42` / maui `SampleCaptureOptions.cs:18-21,51` / kmp `SampleCaptureOptions.kt:27,40-43`。反映先: android `MainActivity.kt:178,197` / ios `SampleMenuModel.swift` の `stepIntervalMilliseconds` / maui `SampleMenuPage.xaml.cs` の `_loadingStepIntervalMilliseconds` / kmp `SamplePresenter.kt` の `loadingStepIntervalMilliseconds`。Default / Custom の両方に効く | `ca-sa-04-interval-2000-{ios,android}-{default,custom}-loading.png` (4枚)。notes.md に6アプリ × Default/Custom の t1→t2→t3 進捗表 | ✅ 一致 |
| [CA-SA-05] 不正値は既定値で動作 | 数値解析の失敗・範囲外はいずれも null → 既定 400ms。android `toLongOrNull()` / ios `Int(_:)` / maui `int.TryParse(NumberStyles.AllowLeadingSign)` / kmp `toLongOrNull()`。既定値の定数は4ルートとも 400 | `ca-sa-05-invalid-interval.png` を実見 (6アプリに `abc` を渡し、t1/t2 の観測時点で既に `結果: 完了`。同時点で 2000ms 指定はまだ進行中) | ✅ 一致 |

### Requirement: 撮影支援設定の4ルートパリティ (ADDED)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| [CA-SA-06] 4ルートで同じ引数・同じ挙動 | 4ルートのキー名・デモ ID 9件・異常系4種を突き合わせた `verification/parity-check.md` (一致判定)。受理文字列の唯一の食い違い (maui の `+2000` 不受理) は `SampleCaptureOptions.cs:46` の `NumberStyles.AllowLeadingSign` で解消済み | notes.md「CA-SA-06」節 — CA-SA-02/03/04/05 をすべて6アプリに同じキー・同じ値で実施した記録。文言一致は `ca-sa-01-noarg-menus.png` と `ca-sa-02-autoplay-*.png` | ⚠️ deviation 記録済み (重複キーの採用が iOS = 最初の1組 / Android = 後勝ち。`deviation.md:3`) |

## 追加検査

### tasks.md の完了状況と虚偽チェック

`[x]` は 1.1〜1.5 / 4.1〜4.3 / 5.1 / 5.3 の 10 件。2.x・3.1・5.2 は改訂による取り下げが本文に明記されている。全件、対応する実体を確認した:

| タスク | 実体 |
|---|---|
| 1.1〜1.4 | 4ルートの新規ファイル (`SampleCaptureOptions` / `SampleDemoId` / `SampleCaptureAutoPlay` / `SampleCaptureArguments`) と各 UI 層の自動再生入口 |
| 1.5 | `verification/parity-check.md` |
| 4.1 | `kasane/config.yaml` の `ui.screenshot` (デモ駆動モード前提の手順へ書き直し済み) |
| 4.2 | `kasane/concepts/cross/conventions/sample-parity.md` の例外枠に「撮影支援機構」追加 + `kasane/concepts/log.md` に記録 |
| 4.3 | `samples/README.md`「撮影のための起動引数」節 |
| 5.1 | `scripts/scenario-id-coverage.py` の `DEFAULT_ALLOW_MISSING` に CA-SA-01〜07 を理由付きで登録 |
| 5.3 | `evidence/four-route-walkthrough/` |

**虚偽チェックなし。**

### 足場の逆流検査

- **契約 (`specs/samples/spec.md`) は未改変** — 起票コミット (`9c70b56`) から一切変更なし。CA-SA-01〜07 の文面は凍結されている
- `proposal.md` / `design.md` / `tasks.md` の改訂と `specs/capture-tooling/spec.md` の削除は、2026-08-27 のオーナー合意による capture-tooling 取り下げに伴うもの。3ファイルとも改訂注記・改訂記録を持ち、経緯 (iOS シミュレータへの座標タップ注入 CLI の不在) が保存されている → **無断の逆流ではない**
- `scripts/capture/` は未作成 (取り下げどおり)。`scenario-id-coverage.py` にも CA-CT 系の残骸はない

### 検査・テストの実行結果

samples 配下に単体テストは存在せず (`scripts/scenario-id-coverage.py` の `DEFAULT_TEST_GLOBS` も samples を含まない)、本 change はライブラリ本体 (`ios/` `android/` `kmp/` `maui/` `core/`) に一切触れていないため、既存テストスイートへの影響はない。代わりに、適用可能な検査とビルドを実行した:

| 実行 | 結果 |
|---|---|
| `python3 scripts/scenario-id-coverage.py` | exit 0。141/157・除外16件 (CA-SA 0/7 除外7)・未網羅なし |
| `python3 scripts/local-path-lint.py` | exit 0 |
| `python3 scripts/identity-lint.py` | exit 0 |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 検査対象 754 ファイル |
| `samples/android` `./gradlew assembleDebug` | BUILD SUCCESSFUL |
| `samples/kmp` `./gradlew :androidApp:assembleDebug` | BUILD SUCCESSFUL |
| `samples/maui` `dotnet build -f net10.0-android` | 0 警告 0 エラー |
| `samples/maui` `dotnet build -f net10.0-ios` | 0 警告 0 エラー (`DEVELOPER_DIR` に Xcode 26.1.1 を指定。既定 Xcode との版数制約は本 change と無関係) |

レビュー後に入った実コード修正 (maui の `Dispatcher.Dispatch(async …)`・ローカル退避の削除・`NumberStyles.AllowLeadingSign`、kmp-android の網羅 `when`) は上記ビルドがすべてカバーしている。ios / kmp-ios の Xcode ビルドは再実行していないが、レビュー後の差分がコメントのみ (`SampleMenuScreen.swift` の `Task.yield()` 注釈・`SampleMenuModel.swift` の退避理由) であることを diff で確認した。

### 未記録乖離 / 付随修正

- `deviation.md` の3項目 (Android 重複 extra の後勝ち / ADR cross/0010 の改訂申し送り / maui `NumberStyles` の付随修正) はいずれも記録済み
- それ以外に、Scenario に対応せず `[付随修正]` にも記録がない差分が1件ある (❌-2)

### UI 変更

本 change に `ui/` はない (proposal「画面の見た目に変化はなく ui/ (mock 承認) は不要」)。撮影支援機構は画面に何も出さないため、モック承認ゲートの対象外で正しい。

### 証跡の規律

- 置き場は `evidence/` 配下に統一済み (レビュー指摘の `verification/` 配下からの移設が反映されている)。`verification/parity-check.md` はコードリーディングの記録でありテキストのみ — 媒体は含まない
- `identity-lint` は exit 0。Android 実機の画像は上端 6% を切り落とし済みで、実見した4枚に個人要素の写り込みはなかった
- notes.md が参照する画像 21 枚はすべて実在する (欠落なし)

## ❌ の一覧と見立て

### ❌-1 CA-SA-07 の実地証跡が1ルート分欠落している (android / kmp-android の画像がバイト同一)

**該当**: `evidence/four-route-walkthrough/ca-sa-07-android.png` と `evidence/four-route-walkthrough/ca-sa-07-kmp-android.png`

両ファイルは **md5 が完全一致する** (`9796af652d8312a5a14d4258f49f7c34`、サイズも 99749 バイトで同一)。証跡ディレクトリ内で重複しているのはこの1組だけで、他の19枚はすべて異なる。中身を実見すると「自動再生 → OK で結果 → 横向きへ回転 → 縦向きへ戻す」の4コマを収めた**単一アプリのグリッド画像**であり、2ルート分を並べたものではない。

notes.md は「android / kmp-android (回転): … **両アプリとも** manifest に画面向きの `configChanges` を持たないため回転で Activity が作り直され … → `ca-sa-07-{android,kmp-android}.png`」と、2ルートを独立に通した記録として書いている。実体は1枚しかないため、**どちらか一方の実地証跡が存在しない**。

結果として CA-SA-07 の実地証跡は「Android 系のいずれか1アプリ + maui-android」の2件にとどまり、notes.md・tasks.md 5.3 が主張する3件に届いていない。

**見立て**: 実装は正しい (消費フラグ + `savedInstanceState` 検知が両ルートにあることをコードで確認済み) ため、**deviation として合意する筋ではなく証跡側の是正**が妥当。(a) 欠けている側を撮り直して差し替える、または (b) 実際に通したのが1ルートだけだったのなら notes.md の記述を実態へ訂正し、他方を iOS 3アプリと同じ「構造的保証」の側へ移す。どちらを採るかは実施者の記憶とオーナー判断による。

**併せて要訂正**: notes.md の CA-SA-07 節は内訳の数え方が本文と食い違っている — 見出し (`notes.md:100`) は「4アプリは実地の再生成、iOS 2アプリは構造的保証」、小見出し (`notes.md:105`) は「実地に再生成を起こせた経路 (4アプリ)」だが、本文が挙げる実地の経路は android / kmp-android / maui-android の**3**アプリ、構造的保証は iOS の**3**アプリ (`notes.md:139,146,177`) で、tasks.md 5.3 も 3/3 と書いている。❌-1 の結論と合わせて内訳を確定させる必要がある。

### ❌-2 未記録の付随差分: MAUI Android `MainActivity.cs` の改行コードが全行 CRLF → LF に変わっている (軽微)

**該当**: `samples/maui/KsDialogs.Sample.Maui/Platforms/Android/MainActivity.cs`

機能差分は `OnCreate` オーバーライドの追加 7 行だけだが、ファイル全体の改行が CRLF から LF に変換されており、diff 上は全 10 行が置換として現れる (`git diff --stat` で 27 行 → `--ignore-cr-at-eol` で 7 行)。リポジトリに `.gitattributes` はないため、この変換はそのままコミットされる。Scenario に対応しない差分であり `deviation.md` の `[付随修正]` にも記録がない。

**見立て**: 実害はほぼないが、記録のない差分を残さないのが規律。**CRLF へ戻して機能差分だけの diff にする**のが最も素直 (他の MAUI ファイルとの改行の揃いも保てる)。意図的に LF へ寄せたのであれば `deviation.md` に `[付随修正]` として1行記録すれば足りる。

## 申し送り (❌ ではないが、アーカイブ前に決着が要る)

- **`deviation.md:3` が「暫定記録 — オーナー確認待ち」のまま**。デルタスペックの「同じキーが複数回現れた場合は最初の1組を採用する (SHALL)」は Android の Intent extra では観測不能で、実装の判断自体は妥当だが、これは**契約側が実現不能な要求を書いている**ことでもある。本検証はコンテキストパッケージの指示に従い合意済み差分 (⚠️) として扱ったが、蒸留前にオーナー確認を取って「暫定記録」を外し、必要なら後続 change で spec 文言を訂正するのが筋
- **CA-SA-07 の iOS 3アプリは実地証跡を持たない** (プロセスを保ったままルート画面を作り直す経路を CLI から誘発できないため、消費フラグの構造で成立としている)。tasks.md 5.3 と notes.md に明記されており本検証では ⚠️ 扱いとしたが、これも本来は `deviation.md` に「証跡の割り付けの合意済み例外」として1行あるほうが、後続の drift で拾い直さずに済む
- **ADR cross/0010 (status: proposed) と実装の矛盾**は `deviation.md:4` に蒸留時の改訂として申し送り済み。ksn-distill が拾うこと

## 総括

デルタスペック `specs/samples/spec.md` の 4 Requirement / 7 Scenario は、4ルート6アプリの実装として**すべて実体を持っている**。キー名・安定デモ ID 9件・異常系の倒れ方・one-shot 消費は4ルートで揃っており、契約の凍結も守られ、虚偽チェックも逆流もなく、適用可能な検査・ビルドは全件成功した。

一方で ❌ 2 件はいずれも**証跡と差分の記録の側**にある。特に ❌-1 は「4ルート通しの証跡で受け入れる」という本 change の検証割り付け (design.md Decision 5・scenario-id-coverage の allow-missing 登録) の土台に直接かかるため、証跡を整えたうえで再検証するのが妥当と考える。
