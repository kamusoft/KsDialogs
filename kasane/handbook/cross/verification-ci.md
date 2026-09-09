---
kind: rule
applies-when:
  always: false
  tasks: [CI の検証範囲の確認, CI の失敗の切り分け, workflow の変更, 変更の完了判定]
title: 検証 CI の範囲と実行条件
description: 検証 CI (`.github/workflows/ci.yml` と platform 別 reusable workflow 5 本・消費者検証 4 本) が回す範囲と手元に残る範囲、各 job が回すテストルートと実行件数の検査、CI の Swift テストをスイート直列で回す理由と待ち不足との見分け方、android-instrumented job の IME 系テストの既知の落ち方 2 型。決定の記録は cross/ADR-0017 (構成と保証範囲) / cross/ADR-0018 (toolchain の固定) / cross/ADR-0022 (lint の 8 検査)
timestamp: 2026-09-09
---

# 検証 CI の範囲と実行条件

テストの実行コマンドと件数の得方は [テスト実行規約](test-execution.md) が持つ。本文書はそのうち CI が回す範囲と、CI だけの実行条件を定める。

`develop` への push と `main` 宛ての pull request で検証 CI (`.github/workflows/ci.yml`) が起動する (cross/ADR-0016)。CI は上表の実行を本体検証 5 job に分けて回し、どの job も終了コードだけでなく**実行件数を検査**して 0 件なら失敗させる。lint job は起動のたびに必ず走る。消費者検証 4 job は `main` 宛ての pull request でだけ起動し、`develop` への push では起動しない。

| CI の job | 回す範囲 | 起動 |
|---|---|---|
| ios | 上表 ios/ の全件。スイート同士の並列実行は止めて回す (後述「CI の Swift テストはスイートを直列で回す」) | 常時 |
| android | 上表 android/ の全件 (後述の Compose 非依存の依存グラフ検査を含む) | 常時 |
| android-instrumented | 上表 android/ (instrumented) の全件。API 36 の Emulator 1 台 | 常時 |
| kmp | 上表 kmp/ の全件と、階層化 source set の metadata compile | 常時 |
| maui | 上表 maui/ と maui/android/native/ と maui/macios/native/ の全件、および platform TFM と binding のビルド。iOS 橋渡しは ios job と同じく直列で回す | 常時 |
| consumer-ios | `verification/ios/` の消費者パッケージを dry-run で Release ビルド (SwiftPM の product 参照) | `main` 宛て PR のみ |
| consumer-android | `verification/android/` の消費者アプリ 2 つを dry-run で release variant ビルドし、依存ツリーを検査 | `main` 宛て PR のみ |
| consumer-maui | `verification/maui/` の消費者アプリを dry-run で net10.0-android / net10.0-ios の Release ビルドし、解決版と取得元とアセットを検査 | `main` 宛て PR のみ |
| consumer-kmp | `verification/kmp/` の消費者を dry-run で 3 段 (Android release / 共有モジュールの iOS framework リンク / iOS アプリの Release) 通し、5 publication の解決と Swift 参照を検査 | `main` 宛て PR のみ |
| lint | 8 検査 (下表) | 常時 |

lint job の 8 検査は次のとおりで、いずれかの違反で job が失敗する。

| 検査 | 見るもの |
|---|---|
| secret scan (gitleaks) | 追跡中の内容を展開したディレクトリ。展開数が追跡ファイル数を下回れば検査対象不足として失敗 |
| ローカル絶対パス検査 | 追跡ファイル中の `/Users/<名前>/` 等 |
| 個体情報検査 | `kasane/config.yaml` の `lint.identity.scope` (`verification/` を含む) |
| コメント規約検査 | [ソースコメント規約](comment-policy.md) の禁止参照 |
| 仕様とテストの対応の検査 | 仕様の Scenario ID がテスト名に現れるか |
| CI 限定スキップの許可リスト検査 | 承認の無い skip (自己テストを含む。cross/ADR-0021) |
| SwiftPM スナップショット同期スクリプトの自己テスト | 同期スクリプトの検出器が退行していないか (cross/ADR-0020) |
| README 最小例の一致検査 | ルート README (英語) の最小例 4 つと `verification/` の消費者ソースの完全一致 (自己テストを含む。cross/ADR-0022) |

CI に載らない検証は**手元の完了判定に残る**。変更の完了を判定するときは CI の緑だけでは足りず、該当するものを手で回す。

- **instrumented の API 29 (旧経路)** — CI が回すのは API 36 の 1 台だけ。API レベルで走り分ける Scenario (後述) の API 29 側は手元のエミュレータで回す
- **負のコンパイル検証 62 本** (後述) — 「成功したら失敗」の判定を要し、フラグごとに 1 ビルドが要るため CI には載せない
- **実行ホストを起動しての確認** — 判定手順は [実行時挙動の検証規約](runtime-behavior-verification.md) が持つ

## CI の Swift テストはスイートを直列で回す

CI で `xcodebuild test` を回す job (ios と、maui job の iOS 橋渡し) は `-parallel-testing-enabled NO` を付け、スイート同士の並列実行を止めて回す。手元 (12 論理 CPU 級) では既定の並列で 277 件が安定するが、GitHub のランナー (`macos-26-arm64`) は CPU が少なく、並列に走るスイートが提示・待ち合わせのために MainActor を取り合って、提示待ちのテストが時間切れになる。**手元の実行条件は変えない** (並列のまま)。この差は実行機の容量差であってテストや実装の欠陥ではないため、待ち時間の延長や観測点の変更で吸収しない。

同じ落ち方かどうかは、失敗の形で見分ける。次の 2 つが揃えば並列スイートの飢餓であり、待ち不足として扱わない:

| 見るもの | 飢餓のとき | 待ち不足のとき |
|---|---|---|
| 失敗までの時間 | 待ちの上限 (`DialogTestWaiting` の既定 5 秒) を大きく超える (初回観測は 28〜57 秒)。待ちループ自体が実行機会を得ていない | 上限のすぐ後 (5 秒台) |
| 落ちるテスト | 回ごとに入れ替わり、Toast / Dialog / Loading / ModelBinding など無関係な領域に散る | 同じテストが同じ箇所で落ち続ける |

直列化を外してよいのは、ランナーの容量が上がるか、提示・待ち合わせの構造が変わって並列で 2 回以上連続して全件が通ることを実測で示したときだけ。戻すときは同じ表で失敗の形を見てから判断する。

## android-instrumented job の IME 系テストの既知の落ち方

IME の出し入れを観測するテストが落ちたときは、job の成果物 (logcat と失敗時の `dumpsys input_method`) の `ImeTracker` / `InsetsController` 行で、次のどちらの型かを先に見分ける。要求 ID (`ksdialogs.test:<id>`) で show / hide の各要求と `onShown` / `onHidden` / `onCancelled` の対応を追う。

| 型 | 署名 | 意味 |
|---|---|---|
| hide が捨てられる | 前の hide が show のアニメーション中に出て保留になり、保留の適用で `requestedVisibleTypes` が一瞬 hidden へ振れて戻る。本命の hide が `onCancelled at PHASE_CLIENT_ALREADY_HIDDEN` になり、待ちの窓に `onHidden` が 1 件も無い | テストが遷移途中の一瞬を終端と読んだ (偽の合格)。観測を終端状態の合意 (見え・枠・非進行・安定) で待つ形へ直す。[状態遷移の観測と CI 限定スキップ](ci-flaky-test-policy.md) |
| 最初の show が潰される | Activity 起動に伴う server 起源の `onRequestHide at ORIGIN_SERVER reason HIDE_UNSPECIFIED_WINDOW` がテスト最初の show と交差し、show が `PHASE_WM_ABORT_SHOW_IME_POST_LAYOUT` で中止される。IME は一度も出ず、枠 (`bottom`) は 0 のまま | 起動直後の要求の交差。テスト最初の要求の前に、この画面が入力の宛先になった状態の落ち着き待ちを置く |

どちらも Toast の器 (ウィンドウ) は引き金ではない。Toast のウィンドウ追加が誘発する `CONTROLS_CHANGED` の show 要求は成功回にも現れ、要求可視性が非表示なら `PHASE_CLIENT_ON_CONTROLS_CHANGED` で取り消される。

## 関連

- [状態遷移の観測と CI 限定スキップ](ci-flaky-test-policy.md) — 間欠失敗の切り分けフローと、CI 上だけ skip してよい条件
- [テスト実行規約](test-execution.md) — 各ビルドルートの実行コマンドと件数の得方、手元の完了判定
- [ローカル開発環境の準備](local-development-setup.md) — `global.json` による SDK / workload set の固定と Xcode の版
- cross/ADR-0017 (検証 CI の構成と保証範囲・トリガー) / cross/ADR-0018 (toolchain の固定境界)
