---
kind: rule
applies-when:
  always: false
  tasks: [CI の検証範囲の確認, CI の失敗の切り分け, workflow の変更, 変更の完了判定]
title: 検証 CI の範囲と実行条件
description: 検証 CI (`.github/workflows/ci.yml` と platform 別 reusable workflow 5 本) が回す範囲と手元に残る範囲、各 job が回すテストルートと実行件数の検査、CI の ios job をスイート直列で回す理由と待ち不足との見分け方。決定の記録は cross/ADR-0017 (構成と保証範囲) / cross/ADR-0018 (toolchain の固定)
timestamp: 2026-09-08
---

# 検証 CI の範囲と実行条件

テストの実行コマンドと件数の得方は [テスト実行規約](test-execution.md) が持つ。本文書はそのうち CI が回す範囲と、CI だけの実行条件を定める。

`develop` への push と `main` 宛ての pull request で検証 CI (`.github/workflows/ci.yml`) が起動する (cross/ADR-0016)。CI は上表の実行を 5 つの job に分けて回し、どの job も終了コードだけでなく**実行件数を検査**して 0 件なら失敗させる。lint job は起動のたびに必ず走る。

| CI の job | 回す範囲 |
|---|---|
| ios | 上表 ios/ の全件。スイート同士の並列実行は止めて回す (後述「CI の ios job はスイートを直列で回す」) |
| android | 上表 android/ の全件 (後述の Compose 非依存の依存グラフ検査を含む) |
| android-instrumented | 上表 android/ (instrumented) の全件。API 36 の Emulator 1 台 |
| kmp | 上表 kmp/ の全件と、階層化 source set の metadata compile |
| maui | 上表 maui/ と maui/android/native/ と maui/macios/native/ の全件、および platform TFM と binding のビルド |
| lint | secret scan・ローカル絶対パス検査・個体情報検査・コメント規約検査・仕様とテストの対応の検査 |

CI に載らない検証は**手元の完了判定に残る**。変更の完了を判定するときは CI の緑だけでは足りず、該当するものを手で回す。

- **instrumented の API 29 (旧経路)** — CI が回すのは API 36 の 1 台だけ。API レベルで走り分ける Scenario (後述) の API 29 側は手元のエミュレータで回す
- **負のコンパイル検証 62 本** (後述) — 「成功したら失敗」の判定を要し、フラグごとに 1 ビルドが要るため CI には載せない
- **実行ホストを起動しての確認** — 判定手順は [実行時挙動の検証規約](runtime-behavior-verification.md) が持つ

## CI の ios job はスイートを直列で回す

CI の ios job は `xcodebuild test` に `-parallel-testing-enabled NO` を付け、スイート同士の並列実行を止めて回す。手元 (12 論理 CPU 級) では既定の並列で 277 件が安定するが、GitHub のランナー (`macos-26-arm64`) は CPU が少なく、並列に走るスイートが提示・待ち合わせのために MainActor を取り合って、提示待ちのテストが時間切れになる。**手元の実行条件は変えない** (並列のまま)。この差は実行機の容量差であってテストや実装の欠陥ではないため、待ち時間の延長や観測点の変更で吸収しない。

同じ落ち方かどうかは、失敗の形で見分ける。次の 2 つが揃えば並列スイートの飢餓であり、待ち不足として扱わない:

| 見るもの | 飢餓のとき | 待ち不足のとき |
|---|---|---|
| 失敗までの時間 | 待ちの上限 (`DialogTestWaiting` の既定 5 秒) を大きく超える (初回観測は 28〜57 秒)。待ちループ自体が実行機会を得ていない | 上限のすぐ後 (5 秒台) |
| 落ちるテスト | 回ごとに入れ替わり、Toast / Dialog / Loading / ModelBinding など無関係な領域に散る | 同じテストが同じ箇所で落ち続ける |

直列化を外してよいのは、ランナーの容量が上がるか、提示・待ち合わせの構造が変わって並列で 2 回以上連続して全件が通ることを実測で示したときだけ。戻すときは同じ表で失敗の形を見てから判断する。

## 関連

- [テスト実行規約](test-execution.md) — 各ビルドルートの実行コマンドと件数の得方、手元の完了判定
- [ローカル開発環境の準備](local-development-setup.md) — `global.json` による SDK / workload set の固定と Xcode の版
- cross/ADR-0017 (検証 CI の構成と保証範囲・トリガー) / cross/ADR-0018 (toolchain の固定境界)
