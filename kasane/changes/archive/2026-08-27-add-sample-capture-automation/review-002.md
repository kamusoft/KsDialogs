# レビュー結果: add-sample-capture-automation (002 回目)

**日付**: 2026-08-27
**判定**: CHANGES_REQUESTED

## サマリー

review-001 / second-opinion-code-001 の突き合わせで確定した指摘は、ADR cross/0010 の申し送り・MAUI の Task 観測・証跡の `evidence/` 移設・design.md の改訂注記・Suggestion 3件のいずれも実体を伴って解消されている。修正で入った変更 (MAUI の async ラムダ化、kmp-android の網羅 `when`、iOS の退避理由コメント) にコードレベルのリグレッションは見当たらず、6アプリのビルドと全 lint も通る。

一方で、今回の目玉だった **CA-SA-07 証跡の再構成に整合しない点が1つ残っている** — `ca-sa-07-android.png` と `ca-sa-07-kmp-android.png` が**バイト単位で完全に同一のファイル**で、2アプリを別々に撮った証跡としては成立しない。前回の指摘が「CA-SA-07 は実際に再生成を起こせているのか」だっただけに、ここが確からしくないと再構成の目的が果たされない。iOS 3ルートを構造的保証で成立とする主張そのものは妥当と判断した (下記「確認した観点」参照)。

## 確認した観点と実行結果

- **ビルド (6アプリすべて成功)**:
  - `samples/android` `./gradlew assembleDebug` → exit 0
  - `samples/kmp` `./gradlew :androidApp:assembleDebug :shared:linkDebugFrameworkIosSimulatorArm64` → exit 0
  - `samples/ios` `xcodebuild` (iOS Simulator) → BUILD SUCCEEDED・コンパイラ警告なし
  - `samples/kmp/iosApp` `xcodebuild -destination 'platform=iOS Simulator,name=iPhone 17'` → BUILD SUCCEEDED
    (`generic/platform=iOS Simulator` だと x86_64 スライスも要求され、共有フレームワークが `iosSimulatorArm64` のみのためリンク失敗する。実機シミュレータ指定で成功 — 本 change と無関係の指定の問題)
  - `samples/maui` `net10.0-android` → 0 警告 0 エラー / `net10.0-ios` (`DEVELOPER_DIR=Xcode 26.1.1`) → 0 警告 0 エラー
- **テスト**: 本 change の diff はライブラリ本体に一切触れておらず (`samples/` `kasane/` `scripts/` のみ)、`samples/` 配下に単体テストの源セットは存在しない (`samples/kmp/shared/src` は `commonMain` のみ)。ライブラリのテスト資産への影響なし
- **lint**: `scripts/scenario-id-coverage.py` (141/157・除外16件・未網羅なし。CA-SA-01〜07 が理由付きで登録済み) / `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` すべて exit 0
- **足場**: `specs/samples/spec.md` は今回も未改変 (契約は凍結されたまま)
- **deviation.md**: 3件とも合意済み差分として扱い、違反としては指摘していない。`[付随修正]` の `NumberStyles.AllowLeadingSign` は本 change が新設したファイルの1行修正で CA-SA-06 の厳密一致に直結しており、同梱条件の内側
- **証跡の置き場**: `evidence/four-route-walkthrough/` に PNG 20件 + `notes.md`。`verification/` にはテキスト (`parity-check.md`) だけが残り、ksn-core の媒体ホワイトリストに適合。旧パス `verification/four-route-walkthrough` への参照はリポジトリ内に残っていない (前回のレビュー文書内の言及を除く)
- **iOS 3ルートの構造的保証の妥当性 (評価済み・成立と判断)**: 3ルートとも消費フラグを**自動再生の入口の最初の文**で読んで即座に立てる形になっており、画面が何度作り直されても2回目以降は必ず `return` へ倒れる。
  - `samples/ios/KsDialogsSample/SampleMenuScreen.swift` の `.task` 先頭が `guard let demo = SampleCaptureAutoPlay.consumeDemo() else { return }`
  - `samples/kmp/iosApp/KsDialogsSampleKmp/SampleMenuScreen.swift` も同型 (`SampleCaptureAutoPlay.shared.consumeDemo(options:)`)
  - `samples/maui/KsDialogs.Sample.Maui/SampleMenuPage.xaml.cs` の `AutoPlay()` 先頭で `SampleCaptureAutoPlay.ConsumeDemo()`。この `SampleCaptureAutoPlay.cs` は `Platforms/` の外にあり **maui-android と同一のコード**で、そちらはフォント倍率による実再生成で実証済み

    3ルートとも「画面状態を触る前に消費する」順序が守られており、`.task` の再実行やページの再構築で状態が二重に変わる経路は無い。実地の再生成が撮れていないことを明示したうえで構造で成立とする notes.md の主張は妥当

## 前回指摘の解消状況

| 前回の指摘 (出典) | 状況 | 根拠 |
|---|---|---|
| ADR cross/0010 と実装の矛盾に申し送りが無い (Major・ホスト) | **解消** | `deviation.md:4` と `proposal.md:28` に「蒸留時に ADR の改訂が必要」を明記。ADR 本体は未改変で、review-001 の推奨 (b) を採った形 |
| 証跡 PNG が `verification/` 配下 (Major・双方一致) | **解消** | PNG 20件と `notes.md` を `evidence/four-route-walkthrough/` へ移設。`notes.md` の `../../verification/parity-check.md` 等の相対参照も正しく張り直されている |
| CA-SA-07 証跡が実際の再生成を起こせていない (Major・相方) | **部分解消** | maui-android はフォント倍率で実再生成 (PID 同一・`直近の結果` 消失を撮影) を新規取得、iOS 3ルートは実地不能を明記して構造的保証へ切り替え。**android / kmp-android の証跡が同一ファイル**の点だけ未達 (指摘 1) |
| MAUI 自動再生の Task が観測されない (Minor・双方一致) | **解消** | `SampleMenuPage.xaml.cs:43` が `Dispatcher.Dispatch(async () => await PlayAsync(demo))` になり、例外が `async void` 経由で同期コンテキストへ再送出される (手動タップの `async void` ハンドラと同じ倒れ方) |
| design.md の取り下げ前提記述の残存 (Minor・ホスト) | **概ね解消** | Goals・Decision 2 見出し・Migration Plan に改訂注記が入った。Context だけ未着手 (指摘 3) |
| kmp-android `play()` の `else` 網羅性 (Suggestion) | **解消** | `MainActivity.kt` の `when` が9件を明示列挙。Presenter 側 `SamplePresenter.autoPlay` も網羅 `when` で、10件目の追加は両方でコンパイルエラーになる |
| MAUI のローカル退避削除 / iOS の理由コメント追加 (Suggestion) | **解消** | MAUI は `_loadingStepIntervalMilliseconds` を直接使う形へ。iOS は退避2箇所に理由コメントを追加。実際に `ios/Sources/KsDialogs/Presentation/Loading.swift:97` の `action` が `@Sendable` の非隔離クロージャであることを確認しており、コメントの因果は正しい |
| `Task.yield()` のコメントが手段の効果を言い過ぎ (Suggestion) | **解消** | ios / kmp-ios とも実測ベースの表現へ差し替え済み |
| deviation.md 1件目が「オーナー確認待ち」(Suggestion) | **未解消 (申し送り継続)** | `deviation.md:3` は「暫定記録 — オーナー確認待ち」のまま。アーカイブ前にオーナー確認が要る |

## 指摘事項

### [🟠 Major] CA-SA-07 の android / kmp-android 証跡が同一ファイルで、2アプリぶんの観察になっていない

**該当箇所**: `evidence/four-route-walkthrough/ca-sa-07-android.png` / `evidence/four-route-walkthrough/ca-sa-07-kmp-android.png` / `evidence/four-route-walkthrough/notes.md:107`〜`:111`

**問題点**: 2ファイルは**バイト単位で完全に同一**である。

- MD5 が一致 (`9796af652d8312a5a14d4258f49f7c34`)、ファイルサイズも一致 (99,749 bytes)
- ピクセル比較でも 1,517,046 px 中**差分 0 px** (差分 bbox = なし)

一方、同じ2アプリを同じやり方でグリッド化した `ca-sa-02-autoplay-android.png` と `ca-sa-02-autoplay-kmp-android.png` は **26 px の差分がある** (bbox `(2109, 254)-(2117, 264)`)。つまり、この端末でこの2アプリを別々に撮ると微差が出るのが実測値であり、CA-SA-07 の4コマ (自動再生 → OK で結果 → 横向き回転 → 縦向きへ戻す) が偶然すべて完全一致することは説明できない。どちらか一方が他方の複製である可能性が高い。

notes.md は `**android / kmp-android (回転)**: ... 両アプリとも manifest に画面向きの configChanges を持たないため回転で Activity が作り直され…` と、2アプリを別々に観察した記述になっている。実際には**片方しか観察の裏付けが無い**状態で、これは前回 review-001 / second-opinion-code-001 の双方が「CA-SA-07 は本当に再生成を起こせているのか」を問うたまさにその論点である。実装 (`savedInstanceState` 検知 + 消費フラグ) はコードを読む限り両アプリとも正しいが、証跡としては成立していない。

なお `evidence/` に置いた画像は蒸留時に削除される (`distill.archive-media`) ため、この不整合を残したままアーカイブすると、後から検証し直す手段が無くなる。

**推奨修正**: android / kmp-android のどちらかを撮り直して別ファイルとして保存し直す (回転手順は notes.md に記録済み)。仮に「2アプリの描画が実際に完全一致しうる」と判断するなら、その根拠 (同一端末・同一切り出し・パリティにより描画が一致する等) と、それでも別々に撮ったことが分かる記録 (撮影時刻・実行したコマンド列) を notes.md に残すこと。**現状のように同一ファイルを2アプリの証跡として並べる形は選べない。**

### [🟡 Minor] notes.md の CA-SA-07 見出しの内訳が本文・tasks.md と食い違う

**該当箇所**: `evidence/four-route-walkthrough/notes.md:100`, `:105`

**問題点**: 見出しは `成立 (4アプリは実地の再生成、iOS 2アプリは構造的保証)`、小見出しは `実地に再生成を起こせた経路 (4アプリ)` とあるが、実際にその節に並ぶのは android / kmp-android / maui-android の**3アプリ**で、構造的保証側の小見出し (`:139`) は `iOS 3アプリ` と書いている。`tasks.md:29` も「android / kmp-android / maui-android が実地の再生成、iOS 3アプリは構造的保証」で 3 + 3 としており、食い違っているのは見出し2箇所だけ。

CA-SA-07 は「どこまで実地に確かめられたか」が争点になった Scenario なので、証跡の冒頭に書かれた内訳が本文と合っていないと、読み手が実地の範囲を1アプリ多く見積もる。

**推奨修正**: `:100` を「3アプリは実地の再生成、iOS 3アプリは構造的保証」、`:105` を「実地に再生成を起こせた経路 (3アプリ)」へ揃える (指摘 1 の撮り直し結果に応じて数え直す)。

### [🔵 Suggestion] design.md の Context だけ改訂注記が入っていない

**該当箇所**: `design.md:5`

**問題点**: Goals・Decision 2 見出し・Decision 4・Decision 5・Migration Plan には改訂注記が入ったが、Context は `方式レベルの決定 (デモ駆動モード = … / 撮影スクリプト = 単一入口 + 宣言的操作列) は ADR cross/0010 で確定済み。本 design は、その方式を6アプリに実装するために必要な下位の設計判断 — 引数の外部表現・起動ライフサイクル・デモ別ディスパッチ・成果物契約・検証割り付け — を固定する` のまま。取り下げ済みの「撮影スクリプト」「成果物契約」を、この design が扱う対象として宣言し続けている。

Decision 2 の本文 (`design.md:42`) も `入口スクリプトは起動前に対象アプリを必ず終了してから起動する` のままだが、こちらは見出しの改訂注記が明示的に打ち消しているので実害は小さい。

**推奨修正**: Context の末尾に1行、「撮影スクリプト部分と成果物契約は 2026-08-27 改訂で取り下げ (Decision 4 参照)」を足す。

### [🔵 Suggestion] 「再生成の証跡ではない」と明記した画像が同じ名前空間に並んでいる

**該当箇所**: `evidence/four-route-walkthrough/ca-sa-07-ios.png` / `ca-sa-07-maui-ios.png` / `ca-sa-07-kmp-ios.png` / `ca-sa-07-maui-android.png`

**問題点**: notes.md はこの4枚を「再生成の証跡にはならない」と本文で明示している (`:114`〜`:116`, `:141`〜`:146`) が、ファイル名は成立した証跡 (`ca-sa-07-android.png` / `ca-sa-07-maui-android-recreate.png`) と同じ `ca-sa-07-*` の並びにある。本文を読まずに画像だけを拾う経路 (蒸留時の媒体棚卸し・後日の drift) では、CA-SA-07 が6アプリぶん揃っているように見える。

**推奨修正**: 名前で区別する (`ca-sa-07-not-a-recreation-*` 等) か、notes.md の CA-SA-07 節の冒頭に「この Scenario の画像とその位置づけ」の索引表を置く。

### [🔵 Suggestion] iOS の「実地不能」の根拠を CLI 手段の不在だけに置いている

**該当箇所**: `evidence/four-route-walkthrough/notes.md:141`〜`:146`

**問題点**: `シミュレータの向きを CLI から変える手段がなく` を実地不能の理由にしているが、本プロジェクトの撮影運用ではシミュレータの回転はオーナーに依頼して行う手段が使われてきた (端末制御の境界の取り決め)。「CLI に無い」だけを理由にすると、後日この節を読んだ人が「オーナーに頼めば撮れたのでは」と再燃させることになる。

より本質的な理由は、**iOS では回転しても SwiftUI のルートビューは破棄されない** — 直前の段落で `fullScreenCover` について書いているのと同じ理由 — で、回転を実施しても CA-SA-07 の「再生成」にはならない、という点にある。こちらを書いておけば、手段の有無に関係なく結論が動かなくなる。

**推奨修正**: 「回転はオーナー操作で可能だが、iOS では回転で SwiftUI のルートビューが作り直されないため CA-SA-07 の再生成にあたらない。プロセスを保ったままルート画面を作り直す誘発手段が外部から無い」という形へ書き換える。

## アクションプラン

1. **CA-SA-07 の android / kmp-android 証跡を別々に取り直す** (Major)。撮り直しか、同一である根拠 + 撮影記録の追記のいずれか
2. **notes.md の CA-SA-07 見出しの内訳を 3 + 3 に揃える** (Minor)。1 の結果に応じて数え直す
3. design.md Context への改訂注記、証跡画像の位置づけの明示、iOS 実地不能の理由の書き換え (Suggestion・まとめて実施可)
4. `deviation.md:3` の「暫定記録 — オーナー確認待ち」の解消 (アーカイブ前の申し送り・前回から継続)
