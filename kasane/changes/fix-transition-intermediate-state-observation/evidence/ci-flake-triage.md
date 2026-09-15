# CI で落ちたテストの切り分け

中間状態 (出現中・出の途中) をポーリングで捕まえていたテスト 3 本を、切り分けフロー
(`kasane/handbook/cross/ci-flaky-test-policy.md`) にかけた記録。手元の反復はすべて下記の条件で行い、
実測の前に検証機の負荷を確認した。

## 手元の実行条件

| 項目 | 値 |
|---|---|
| Android | AVD `ksn_api36_headless` (API 36)、`-no-window -gpu swiftshader_indirect -noaudio -no-boot-anim -no-snapshot` |
| アニメーション | window / transition / animator の 3 種を 0 (CI の `disable-animations: true` と同条件) |
| 反復の起こし方 (Android) | `am instrument -w -e class <クラス#メソッド>` を 10 回 (対応後は 30 回) |
| iOS | この検証のために新規作成した iPhone 17 Simulator (iOS 26.5)。反復は `xcodebuild test-without-building -parallel-testing-enabled NO` の 2 本同時指定。終了後に削除。直列 (`-parallel-testing-enabled NO`) にしたのは対象 2 本の反復を 1 台で回すためで、手元の全件回帰は `cross/verification-ci.md` のとおり並列のままでも別途通している (277 tests / 50 suites・失敗 0) |
| 検証機の負荷 | 実測開始時 load average 18.79 / 13.94 / 10.04 (12 論理 CPU)。居座りの CPU 食いプロセスは無く、内訳は検証機オーナーの常用アプリ (ブラウザ・デスクトップアプリ) と別作業が起動中の Simulator 1 台。掃除できる汚染ではないため、この負荷のまま対応前・対応後を同条件で測った |

負荷が高い状態での実測になったことは、対応前の再現率 (下表) を押し上げている可能性がある。
対応後の反復は同じか、より高い負荷 (実測終了時 69.67 / 64.53 / 51.76 — 大半は本検証自身の
ビルドとテスト実行) の下で行っており、条件は対応前より緩くなっていない。

## 切り分けの表

| テスト | CI での落ち方 | 手元 (対応前) | 診断 | 対応 | 手元 (対応後) |
|---|---|---|---|---|---|
| Android `DialogTransitionTests` の `PB_TR_21` | `android-instrumented / verify` の 2 run (2026-09-09 / 2026-09-13。別 commit・別 PR) で「覆いの出現中はまだ退出しない expected PRESENTING but was SHOWN」 | 10 回中 5 回 (CI と同じ署名) | 通り過ぎる中間状態をポーリングで捕まえている。`none()` では PRESENTING の幅が覆いのフェードの実時間だけになり、ポーリングのヒットからメインスレッドのブロックが走るまでに出現が完了する | 出現フックを関門 (`DialogTransitionGate` + `DialogTransitionProbe.gatedHook`) で押さえ、「出現中」を確定させてから閉鎖を報告する | 30 回中 0 回 |
| iOS `DialogTransitionTests` の `PB-TR-21` | CI では未再現 (姉妹面) | 10 回中 0 回 | 機構は Android と同型 — `none()` の実時間に頼って `.presenting` をポーリングで捕まえている | 同上 (門つき出現フック) | 15 回中 0 回 |
| iOS `LoadingCoalescingTests` の `LD-CO-13` | CI では未再現 (姉妹面) | 10 回中 0 回 | `hide()` の実時間の出の演出を `isDismissing` のポーリングで捕まえてから `show()` を差し込んでいる | 出の完了を門で押さえるカスタム View を第 1 世代にし、出のフックの呼び出しを確かめてから新しい開始を差し込む (Android 版 `LD_CO_13` と同形) | 15 回中 0 回 |

### CI での落ち方の出典

| run | commit / 文脈 | 落ちたテスト |
|---|---|---|
| `android-instrumented / verify` 34328835867 (2026-09-09) | commit d165824 (kmp / android のビルドファイルのみ変更。本体・instrumented テストは無改変) | `PB_TR_21` — 覆いの出現中はまだ退出しない expected PRESENTING but was SHOWN |
| `android-instrumented / verify` 34752439146 (2026-09-13) | PR #4 (0.1.0-beta.2)、commit f3644e7 (release 機構とインストール例のみ変更) | 同上 (同じ 1 件・同じメッセージ) |

### 版の定義

| 版 | 内容 |
|---|---|
| A | 対応前 (`none()` の実時間と `isDismissing` のポーリングで中間状態を捕まえる形) |
| B | 対応後 (現行)。3 本とも中間状態を門で確定させ、門を開ける前に「まだ次へ進んでいない」ことを確かめる形。`LD-CO-13` は差し込みの経路も UI スレッド隔離の受理口へ付け替えている |

「手元 (対応前)」は版 A、「手元 (対応後)」は版 B で測った数字。

## 検出力の根拠

版 A の Android `PB_TR_21` は、この検証機・この実行条件で 10 回中 5 回、CI と同一の assertion・
同一のメッセージで落ちた。**版 A が捕まえようとしていた窓は実際に取り逃がされる**ことがこれで
実測できており、版 B の 30 回連続成功は「窓が消えたから通っている」のではなく「窓を門で
固定したから通っている」と読める。

版 B の iOS `LD-CO-13` で「新しい開始が出の途中に差し込まれた」ことを固定しているのは、
反復の回数ではなく**差し込みの経路**である。新しい開始は、公開入口ではなく、その入口が 1 行で
委譲している UI スレッド隔離の受理口を直接呼ぶ形で投入している。隔離された呼び出しだけを並べた
仕事は、最初の中断点 (= 撤去待ち) まで実行機を手放さずに進むため、待ち側が別の仕事として印を
観測できた時点で、その要求は撤去待ちの列に載り終えている。門はそれまで開けない。

公開入口 (`Loading.show()`) は隔離されていない async メソッドで、本体は呼び出し元の UI スレッドを
離れて走る。この経路では印と受理口の間に実行機の乗り換えが挟まり、印を観測した時点でまだ列に
載っていない回があり得る — その回は「撤去完了後に始まった開始」になり、後続の検査はすべて成立
するため**失敗せずに通り抜ける**。差し込み位置を反復回数で担保できないのはこのためで、経路を
変えることで順序の成立を構造として固定した。

この付け替えによって公開入口 `Loading.show()` そのものの経路はこの Scenario では踏まれなくなる。
入口から受理口までの委譲は同ファイルの他の Scenario (`LD-CO-01` ほか) が押さえている。

## CI 限定スキップの判定

3 本とも**観測の欠陥**として直せたため、CI 限定スキップは適用していない。
`PB_TR_21` は手元でも落ちた (5/10) ので、切り分けフローの「手元で落ちるなら直す」に当たり
スキップの候補にならない。iOS の 2 本は手元で通ったが、出現の完了を待ってから退出するという
公開契約の保証そのものを固定する Scenario であり、重要な機能のテストとしてスキップの対象外。

## 全件の実測 (版 B)

| ルート | 件数 |
|---|---|
| android instrumented (`connectedDebugAndroidTest`、AVD 1 台) | `:ksdialogs-core` 294 件 (skipped 1・失敗 0) / `:ksdialogs` 39 件 (skipped 0・失敗 0) |
| android instrumented のうち `DialogTransitionTests` 単体 | 29 件 / 失敗 0 |
| ios (`xcodebuild test -scheme KsDialogs -parallel-testing-enabled NO`) | 277 tests / 50 suites・失敗 0 |
| ios のうち `DialogTransitionTests` / `LoadingCoalescingTests` / `LoadingTransitionTests` | 53 tests / 3 suites・失敗 0 |

instrumented の skipped 1 件は `PB_SB_04` で、`assumeTrue` による API レベルの前提条件。
CI 限定スキップの印ではない。

## 検証に使った機材の後始末

| 機材 | 後始末 |
|---|---|
| AVD `ksn_api36_headless` | 実測後に停止 (AVD 定義は既存のものを流用しており残す)。アニメーション 3 種の設定は CI と同条件のまま |
| iPhone 17 Simulator (iOS 26.5) | この検証のために新規作成し、実測後に削除 (指摘への対応でもう 1 台を新規作成し、同じく削除) |

別作業が起動していた Simulator と接続中の実機は、この検証では使っていない。
