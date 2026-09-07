# 検証結果: add-sample-capture-automation (002 回目)

**日付**: 2026-08-27
**判定**: VALID (❌ 0 件。前回 ❌ 2 件はいずれも解消)
**追記** (同日・iOS CA-SA-07 証跡の差し替え後): **VALID 維持**。詳細は末尾「追記: iOS CA-SA-07 証跡の差し替え確認」を参照

## 検証対象

前回 `verify-001.md` (判定 INVALID・❌ 2 件) と `review-002.md` (CHANGES_REQUESTED) に対する**是正の差分検証**。
verify-001 で確定した Scenario ごとの対応表 (CA-SA-01〜06 の ✅ / ⚠️ 判定) は有効なままとし、本書では再掲しない。

verify-001 出力時刻 (14:31) 以降に変更されたファイルは以下の 4 件のみで、実装コードは `MainActivity.cs` 1 本だけ:

| ファイル | 変更の性質 |
|---|---|
| `samples/maui/KsDialogs.Sample.Maui/Platforms/Android/MainActivity.cs` | 改行コードの復旧のみ (❌-2) |
| `evidence/four-route-walkthrough/ca-sa-07-android.png` / `ca-sa-07-kmp-android.png` | 差し替え (❌-1) |
| `evidence/four-route-walkthrough/notes.md` | CA-SA-07 節の記述訂正・追記 (❌-1 / review-002 Minor) |
| `design.md` | Context への改訂注記 (review-002 Suggestion) |

デルタスペック `specs/samples/spec.md` は起票コミット (`9c70b56`) から一切変更なし (mtime 12:07 / HEAD と差分なし)。

## 前回 ❌ の解消状況

| # | 指摘 | 状態 |
|---|---|---|
| ❌-1 | CA-SA-07 の android / kmp-android 証跡がバイト同一。notes.md の内訳表記も本文と矛盾 | **解消** |
| ❌-2 | `MainActivity.cs` の改行が CRLF → LF に変わり未記録の付随差分になっていた | **解消** |

### ❌-1 → 解消

**(a) 2ファイルが別個の証跡になったか**

| ファイル | サイズ | MD5 |
|---|---|---|
| `evidence/four-route-walkthrough/ca-sa-07-android.png` | 214,707 | `7cde27b4d0369e177e897f253721f93e` |
| `evidence/four-route-walkthrough/ca-sa-07-kmp-android.png` | 214,855 | `9e391b4befb833e426fb610f5a3a9987` |

前回の同一 MD5 (`9796af65…`) は解消。証跡ディレクトリ内 21 枚の MD5 はすべて相異なる (重複なし)。

**(b) 見出し帯による自証**

両画像 (3411x960) の上端に見出し帯が入り、実見して以下を確認した:

- android: `CA-SA-07 / android — jp.kamusoft.ksdialogs.samples.android/.MainActivity — Pixel 4a 実機 — 撮影 2026-08-27 14:34:35 - 14:34:59 — PID 32519 (回転の前後で不変)`
- kmp-android: `CA-SA-07 / kmp-android — jp.kamusoft.ksdialogs.samples.kmp.android/.MainActivity — Pixel 4a 実機 — 撮影 2026-08-27 14:35:20 - 14:35:45 — PID 32637 (回転の前後で不変)`

対象パッケージ・撮影時刻帯・プロセス ID がすべて別で、どちらの経路の証跡かを画像自身が示す形になっている。
4コマ (自動再生のダイアログ → `OK` で `結果: completed(true)` → 横向き → 縦向きへ復帰) の構成も、回転後に `直近の結果` が消えている点も両画像で確認できる。

**(c) notes.md の記録**

`notes.md:113`〜`:134` に、review-002 のアクションプラン 1 が提示した2択のうち**後者 (「同一である根拠 + 撮影記録の追記」)** が実施されている:

- 2アプリで実行した `am force-stop` → `am start -n <component> --es demo basic-dialog` → `settings put system user_rotation 1` → `0` のコマンド列 (`<component>` だけが入れ替わることを明示)
- プロセス ID (32519 / 32637) と撮影時刻帯の記録、回転の前後で PID が不変 = 同一プロセスのままの再生成であること
- **画面部分が画素単位で一致することの明示的な開示と理由** (同一端末・同一解像度・パリティにより描画が一致し、両者を分ける時刻/通知アイコンは個人要素として上端 6% ごと切り落としている)
- そのため見出し帯を足したこと・画面の画素は無加工であること (`notes.md:129`〜`:134`、`:210`〜`:212` の撮影規律にも再掲)

**(d) 開示された「画素一致」の裏取り**

notes.md の主張を本検証でも独立に確かめた:

- 2ファイルの画素差分の bbox は `(139, 7)-(1899, 31)` — **見出し帯のテキスト行のみ**。それ以外の全画素が一致する (notes.md の開示どおり)
- 「この2アプリは同一端末で描画が一致する」の裏取りとして、別 Scenario の同一構図ペア `ca-sa-02-autoplay-android.png` / `ca-sa-02-autoplay-kmp-android.png` を比較した。差分は bbox `(2109, 254)-(2117, 264)` の 1 箇所のみで、実見するとスクリム上のローディングスピナーの点 1 個 = **アニメーション位相の差**だった。静止画面では 2 アプリの描画が実際に画素単位で一致することの傍証になる (review-002 が「26 px の差分がある」として同一性を疑った根拠は、静的描画の差ではなくアニメーションのタイミング差だった)

以上より、証跡の実体と notes.md の記述の食い違いは解消し、review-002 が禁じた「同一ファイルを2アプリの証跡として並べる形」ではなくなっている。

**(e) 内訳表記の矛盾**

3 + 3 で統一済み:

| 箇所 | 記述 |
|---|---|
| `notes.md:100` (見出し) | 成立 (実地の再生成**3アプリ**、iOS **3アプリ**は構造的保証) |
| `notes.md:105` (小見出し) | 実地に再生成を起こせた経路 (**3アプリ**) |
| `notes.md:162` (小見出し) | CLI から再生成を起こせなかった経路 (iOS **3アプリ**・構造的保証で足りるとする) |
| `tasks.md:29` (5.3) | android / kmp-android / maui-android が実地の再生成、iOS 3アプリは構造的保証。内訳は証跡の該当節が正 |

### ❌-2 → 解消

`samples/maui/KsDialogs.Sample.Maui/Platforms/Android/MainActivity.cs` は CRLF (BOM 付き UTF-8) に戻っている (`CRLF 17 / LF 17` — LF 単独の行なし)。

diff は機能差分だけになった:

```
git diff --stat HEAD -- .../Platforms/Android/MainActivity.cs
 → 1 file changed, 7 insertions(+)
git diff --stat --ignore-cr-at-eol HEAD -- .../Platforms/Android/MainActivity.cs
 → 1 file changed, 7 insertions(+)   ← 両者一致 = 改行のみの置換行は消えた
```

追加された 7 行は `OnCreate` オーバーライド (`SampleCaptureArguments.Capture(Intent)` を `base.OnCreate` の前に呼ぶ) のみで、Scenario に対応する機能差分。`deviation.md` への `[付随修正]` 追記は不要になった。

## review-002 の指摘の解消状況

| 指摘 | 状態 |
|---|---|
| [🟠 Major] CA-SA-07 の android / kmp-android 証跡が同一ファイル | **解消** (上記 ❌-1) |
| [🟡 Minor] notes.md の CA-SA-07 見出しの内訳が食い違う | **解消** (上記 ❌-1 (e)) |
| [🔵 Suggestion] design.md の Context だけ改訂注記が入っていない | **解消** — `design.md:5` に「**2026-08-27 改訂: 後半の撮影スクリプトは取り下げ (経緯は proposal 改訂記録・ADR の改訂申し送りは deviation.md 参照)。**」が入り、Context が取り下げ済みの対象を宣言し続ける形は解消 |
| [🔵 Suggestion] 「再生成の証跡ではない」画像が同じ名前空間に並ぶ | **未対応** (ファイル名は `ca-sa-07-*` のまま。索引表も未設置。位置づけは notes.md 本文で明示されている) |
| [🔵 Suggestion] iOS の「実地不能」の根拠を CLI 手段の不在だけに置いている | **部分対応** — `notes.md:164` は「シミュレータの向きを CLI から変える手段がなく、**プロセスを保ったままルート画面を作り直す経路を外から誘発できない**」となり結論の一般化は入ったが、review-002 が本質的理由として挙げた「iOS では回転しても SwiftUI のルートビューが破棄されない」は明示されていない |
| [🔵 Suggestion] `deviation.md:3` の「暫定記録 — オーナー確認待ち」 | **未解消 (申し送り継続)** |

未対応の 2 件はいずれも Suggestion であり、本差分検証の判定材料には含めない (今回のコンテキストパッケージの対象外)。

## 追加検査 (差分に対して再実行)

### 足場の逆流

- **契約 `specs/samples/spec.md` は依然として未改変** (起票コミットのまま。CA-SA-01〜07 の文面は凍結が保たれている)
- 今回の変更で足場に触れたのは `design.md` の Context 1 行のみで、内容は既存の改訂 (2026-08-27 のオーナー合意による capture-tooling 取り下げ) を Context にも反映した**注記の追加**。契約の書き換えではない → 無断の逆流なし
- `tasks.md` は今回未変更

### 虚偽チェック

`tasks.md` の `[x]` は前回から増減なし (1.1〜1.5 / 4.1〜4.3 / 5.1 / 5.3 の 10 件)。前回の突き合わせで全件に実体があることを確認済みで、今回の是正はいずれもその実体を**強化する方向**にのみ働いている (5.3 の証跡が 2 アプリぶんの独立記録になった)。**虚偽チェックなし。**

### 未記録乖離 / 付随修正

- ❌-2 の解消により、Scenario に対応せず `[付随修正]` にも記録がない差分は**なくなった**
- `deviation.md` の 3 項目 (Android 重複 extra の後勝ち / ADR cross/0010 の改訂申し送り / maui `NumberStyles` の付随修正) は前回から変更なし・すべて記録済み

### 検査・ビルド

| 実行 | 結果 |
|---|---|
| `python3 scripts/scenario-id-coverage.py` | exit 0。141/157・除外 16 件 (CA-SA 7 件は理由付き)・未網羅なし |
| `python3 scripts/local-path-lint.py` | exit 0 (新規追記の notes.md・見出し帯にローカル絶対パスなし。adb の端末シリアルは `<android-serial>` のプレースホルダ表記) |
| `python3 scripts/identity-lint.py` | exit 0 |
| `python3 scripts/comment-policy-lint.py` | 禁止 0 件 / 検査対象 754 ファイル |
| `samples/maui/KsDialogs.Sample.Maui` `dotnet build -f net10.0-android` | **ビルド成功・0 警告 0 エラー** (改行復旧後の再ビルド) |

samples 配下に単体テストは存在せず (前回確認どおり)、今回の差分は他ルート (ios / android / kmp) のコードに一切触れていないため、それらのビルドは再実行していない。

### 証跡の規律

- 置き場は `evidence/` 配下のまま。新規の見出し帯に含まれるのはパッケージ名・端末機種 (Pixel 4a)・撮影時刻・PID のみで、個人を特定する値の追加なし
- Android 実機画像の上端 6% 切り落としは維持されている (見出し帯は切り落とし後の画像の**上に追加**された行であり、画面領域を覆っていない — 画素差分が帯の行だけに閉じていることで確認)
- notes.md の相対参照 (`../../verification/parity-check.md` / `../../specs/samples/spec.md` / `../../deviation.md`) はすべて実在先を指す
- notes.md が参照する画像 21 枚はすべて実在し、MD5 の重複なし

## 総合判定

**VALID**。

前回 ❌ 2 件はいずれも解消した。デルタスペック `specs/samples/spec.md` の 4 Requirement / 7 Scenario は、verify-001 の対応表に加えて CA-SA-07 の証跡欠落が埋まったことで、**すべて「✅ 一致」または「⚠️ deviation 記録済み」**に収まる (⚠️ は CA-SA-06 の重複キー採用の非対称 1 件のみ・`deviation.md:3`)。契約の凍結は保たれ、虚偽チェック・逆流・未記録乖離はなく、適用可能な検査とビルドは全件成功した。

## 申し送り (❌ ではないが、アーカイブ前に決着が要る)

1. **`deviation.md:3` が「暫定記録 — オーナー確認待ち」のまま** (verify-001・review-002 から継続)。デルタスペックの「同じキーが複数回現れた場合は最初の1組を採用する (SHALL)」は Android の Intent extra では観測不能で、これは契約側が実現不能な要求を書いていることでもある。蒸留前にオーナー確認を取って「暫定」を外し、必要なら後続 change で spec 文言を訂正するのが筋
2. **CA-SA-07 の iOS 3アプリは実地証跡を持たない** (構造的保証で成立としている)。notes.md・tasks.md 5.3 に明記済みだが、`deviation.md` に「証跡の割り付けの合意済み例外」として 1 行あるほうが、後続の drift で拾い直さずに済む
3. **CA-SA-07 の android / kmp-android の画素一致は「開示された事実」であって「画像による独立の証明」ではない**。2 枚を分けているのは実装者が付した見出し帯 (パッケージ名・時刻・PID) と notes.md のコマンド記録であり、画素そのものは 2 回の実行を区別しない。review-002 が明示的に許した代替経路 (根拠 + 撮影記録の追記) を採ったものなので本検証では解消と判定したが、`evidence/` の画像は蒸留時に削除される (`distill.archive-media`) ため、**この 2 アプリの独立性の担保は最終的に notes.md の記述として残る**点をオーナーが承知したうえでアーカイブするのが望ましい
4. **ADR cross/0010 (status: proposed) と実装の矛盾**は `deviation.md:4` に蒸留時の改訂として申し送り済み。ksn-distill が拾うこと
5. review-002 の Suggestion 2 件 (証跡画像の名前空間での位置づけ・iOS 実地不能の本質的理由) は未対応。判定には影響しないが、蒸留時に notes.md へ反映しておくと後日の drift で再燃しない

---

# 追記: iOS CA-SA-07 証跡の差し替え確認 (2026-08-27)

上記「申し送り 2」について、オーナーが**「手動回転で実物証跡を撮る」**を選択し、証跡が差し替えられた。
その差分だけを確認した結果を以下に残す。**総合判定は VALID を維持する。**

差し替え以降に変更されたファイルは以下の 4 件のみで、**実装コード・`tasks.md`・`specs/samples/spec.md` はいずれも未変更** (spec は起票コミットのまま `git diff` 空)。他 change (`fix-sample-android-gesture-back-lint/exploration.md`) の更新は本検証の対象外。

| ファイル | 変更 |
|---|---|
| `evidence/four-route-walkthrough/ca-sa-07-{ios,maui-ios,kmp-ios}.png` | 差し替え |
| `evidence/four-route-walkthrough/notes.md` | CA-SA-07 節・撮影規律の節を改訂 |
| `deviation.md` | 1件目を「オーナー確定」に更新 + CA-SA-07 の検証方式エントリを新設 |

## 1. 新証跡 3 枚の独立性と、画像と主張の一致

**独立性 — 成立。** 3 枚は MD5 が相異なるだけでなく、**画素差分の bbox が画像のほぼ全域に及ぶ** (Android ペアのように見出し帯だけが違うのではなく、本文の描画そのものが違う):

| 比較 | 画素差分 bbox |
|---|---|
| ios vs maui-ios | `(83, 10)-(3315, 1006)` |
| ios vs kmp-ios | `(410, 10)-(3220, 992)` |
| maui-ios vs kmp-ios | `(83, 10)-(3315, 1006)` |

見出し帯 (実見):

| 画像 | 見出し帯 |
|---|---|
| `ca-sa-07-ios.png` | `jp.kamusoft.ksdialogs.samples.ios \| iPhone 17 Simulator (iOS 26.0) \| 2026-08-27 14:55:00-15:00:15 JST \| PID 31452` |
| `ca-sa-07-maui-ios.png` | `jp.kamusoft.ksdialogs.samples.maui \| iPhone 17 Simulator (iOS 26.0) \| 2026-08-27 15:04:18-15:05:18 JST \| PID 52870` |
| `ca-sa-07-kmp-ios.png` | `jp.kamusoft.ksdialogs.samples.kmp.ios \| iPhone 17 Simulator (iOS 26.0) \| 2026-08-27 15:06:58-15:09:56 JST \| PID 62259` |

3 枚とも 2 行目に「`回転はオーナー手動 (Cmd+←/→) / 03 は可読性のため 90° 回転して収載 (画素は無加工)`」の但し書きがあり、加工の範囲が画像自身に開示されている。bundle id・撮影時刻帯・PID がすべて別。

**画像と主張の一致 — 成立。** 3 枚とも 4 コマ (`01 自動再生` / `02 OK後` / `03 横向き` / `04 縦復帰`) を実見した:

- `01`: 自動再生されたダイアログ `こんにちは、KsDialogs!` + `キャンセル` / `OK` が表示されている
- `02`: ダイアログが消え `直近の結果` が出ている
- `03` (横向き): 横向きレイアウトのメニュー (Dynamic Island が横位置に来ている = 実際の横向きスクリーンショット)。**ダイアログの再表示なし**
- `04` (縦復帰): 通常のメニュー。**ダイアログの再表示なし**

`04` では `直近の結果` の表示が**残っている**。これは notes.md が明記する「iOS の回転は SwiftUI / MAUI のルート画面を破棄・再生成しない (レイアウトの更新のみ)」と整合し、証跡が「回転しても再発火しない」までを示し「再生成しても再発火しない」は構造で担う、という記述どおりの読み方になる。**画像が記述を裏切っていない** (Android 側は逆に `直近の結果` が消えることで再生成を示しており、両者の対比も筋が通っている)。

### 🟡 1 件だけ、画像とコマ見出し・手順記述の食い違いがある (Minor・判定には影響しない)

`ca-sa-07-kmp-ios.png` のコマ `02` / `04` の結果表示は **`結果: cancelled`** で、`completed(true)` ではない (拡大して確認)。他 2 枚 (`ios` / `maui-ios`) は `completed(true)`。

一方、コマ見出しは 3 枚とも `02 OK後` で、`notes.md` の手順も「自動再生されたダイアログを `OK` で閉じ `結果: completed(true)` にする」と 3 アプリ共通で書かれている。kmp-ios は実際には `キャンセル` 側が押された (もしくは別経路で閉じられた) と読める。

- **CA-SA-07 の成立には影響しない。** この Scenario の主張は「回転・再生成でダイアログが再表示されないこと」であり、ダイアログをどちらのボタンで閉じたかは要件外。3 枚とも `03` / `04` で再表示がないことは確認済み
- ただし**証跡のコマ見出しと本文が画像と食い違っている**のは、これまで本 change で 2 度指摘されてきた「記録と実体の不一致」と同じ型の瑕疵にあたる。`notes.md` 側で kmp-ios だけ `キャンセル` で閉じた旨を書くか、コマ見出しを実態に合わせるのが望ましい

## 2. notes.md / tasks.md 5.3 / deviation.md の整合

**notes.md ↔ deviation.md — 整合。**

- `notes.md` CA-SA-07 見出し: 「成立 (Android 3アプリ = 実地の再生成 / iOS 3アプリ = 実地の回転 + 構造的保証)」、小見出しも「実地に再生成を起こせた経路 (Android 3アプリ)」「実地の回転 + 構造的保証の経路 (iOS 3アプリ)」で **3 + 3** のまま一貫
- `deviation.md` に CA-SA-07 の検証方式エントリが新設され、「iOS 3アプリは CLI から画面再生成を誘発する手段が無いため、オーナー手動の回転 (Cmd+←/→) による実地証跡 (回転しても再発火しない) + 構造的保証 … の併記で成立とした。検証方式はオーナー確定 (2026-08-27)」と、notes.md と同じ切り分けで記述されている
- notes.md も「iOS で再生成そのものを外から起こす手段は引き続き存在しない」と限界を明示しており、**回転証跡を再生成証跡と偽っていない**

**tasks.md 5.3 — 軽微な古さあり (Minor)。**

`tasks.md:29` の括弧書きは「CA-SA-07 は android / kmp-android / maui-android が実地の再生成、**iOS 3アプリは構造的保証**。内訳は証跡の該当節が正」のままで、今回加わった「iOS 3アプリの実地の回転」が反映されていない。末尾の「内訳は証跡の該当節が正」で notes.md に委譲する形になっているため矛盾とまでは言えないが、単独で読むと iOS に実地証跡がないように見える。1 行の追記で揃う。

**撮影・保存の規律の節に自己矛盾が 1 件 (Minor)。**

同節の 2 つ目の箇条書きは「iOS は … **CA-SA-07 の3枚のみ**、Android の CA-SA-07 と同じ規律で上端 6% の切り落とし + 見出し帯 … を付けている」と正しく書いているのに、4 つ目の箇条書きは「CA-SA-07 の `ca-sa-07-android.png` / `ca-sa-07-kmp-android.png` **だけは** … 見出し帯を足している」と、旧記述のまま残っている。見出し帯を持つのは現在 5 枚 (Android 2 + iOS 3) なので、後者の「だけは」は成り立たない。前者の記述と実物 (実見) が正で、後者を「画素一致のため見出し帯が特に必要だったのは Android の 2 枚」といった趣旨へ直せば解消する。

## 3. 前回の申し送りの決着

| 申し送り | 状態 |
|---|---|
| 1. `deviation.md:3` の「暫定記録 — オーナー確認待ち」 | **決着** — 1 件目 (Android 重複 extra の後勝ち) の末尾が「**オーナー確定 (2026-08-27)**」に置き換わり、暫定の但し書きは消えた |
| 2. CA-SA-07 の iOS 3 アプリを `deviation.md` に記録 | **決着** — 検証方式のエントリが新設され「検証方式はオーナー確定 (2026-08-27)」。しかも記録だけでなく、実地の回転証跡そのものが追加された (当初の申し送りより強い決着) |
| 3. android / kmp-android の画素一致は開示された事実であって画像による独立の証明ではない | **継続** — Android ペアの状況は今回変わっていない。`evidence/` は蒸留時に削除されるため、独立性の担保が notes.md の記述として残る点は変わらず。なお iOS 3 枚は今回の差し替えで**画素そのものが相異なる**ため、この論点は Android ペアに限定される |
| 4. ADR cross/0010 の改訂 (蒸留時) | **継続** — `deviation.md` に申し送り済み。ksn-distill が拾う |
| 5. review-002 の Suggestion 2 件 | **1 件は実質解消** — 「iOS 実地不能の本質的理由」は、notes.md が「iOS の回転は SwiftUI / MAUI のルート画面を破棄・再生成しない」と本質的理由を明記する形に書き換わった。もう 1 件 (証跡画像の名前空間での位置づけ・索引) は未対応のまま |

## 4. 再実行した検査

| 実行 | 結果 |
|---|---|
| `python3 scripts/local-path-lint.py` | exit 0 (新規の見出し帯・notes.md 追記にローカル絶対パスなし) |
| `python3 scripts/identity-lint.py` | exit 0 |
| `python3 scripts/scenario-id-coverage.py` | exit 0 (CA-SA 7 件は理由付き除外のまま・未網羅なし) |

実装コードは今回一切変更されていないため、ビルドは再実行していない。

証跡の規律: 見出し帯に含まれるのは bundle id・シミュレータ機種 / OS 版・撮影時刻・PID のみで個人を特定する値の追加なし。iOS 3 枚は上端 6% を切り落とし済みで、実見した範囲に個人要素の写り込みはない。CA-SA-07 の画像 7 枚を含む証跡 21 枚の MD5 に重複なし。

## 追記時点の判定

**VALID を維持する。**

デルタスペック `specs/samples/spec.md` は依然として未改変で、CA-SA-07 は「実装が一致 + 証跡が主張どおり」を満たす。今回の差し替えで、前回 ⚠️ 扱いだった iOS 3 アプリの実地証跡欠落が解消し、Scenario の裏付けはむしろ強くなった。新たに見つかった 3 件 (kmp-ios の `cancelled` とコマ見出しの食い違い / tasks.md 5.3 の古い括弧書き / 撮影規律の「だけは」の残存) はいずれも**記録の文言側の Minor** で、❌ には当たらない。ただし本 change は「記録と実体の不一致」を 2 度指摘されてきた経緯があるため、アーカイブ前に 3 件とも 1 行ずつ直しておくことを勧める。
