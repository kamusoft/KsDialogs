# Tasks: add-sample-capture-automation

## 1. Sample デモ駆動モード (4ルート)

- [x] 1.1 Android native: `MainActivity` で string extra (`demo` / `loading-step-interval-ms`) を解析 (design Decision 1 の異常系込み)、安定デモ ID 9件の定数定義、タップハンドラと同じ入口を呼ぶ自動再生 (パネル系は openLayoutPanel / openTransitionPanel)、one-shot 消費 (savedInstanceState で再生成検知)、刻み間隔の可変化 (→ Requirement: 撮影支援設定の起動引数受け口 / 指定デモの自動再生 / Loading 刻み間隔の起動時指定)
- [x] 1.2 iOS native: `ProcessInfo` の `--キー 値` トークンペア解析、同上。パネル系 (`layout-dialog` / `transition-dialog`) は SampleMenuScreen の画面状態を開き、他は `SampleMenuModel` のデモ起動を呼ぶ。one-shot はプロセス内消費フラグ (→ 同上)
- [x] 1.3 MAUI: iOS 側 `NSProcessInfo` / Android 側 `MainActivity` に intent extra の読み口を追加して解析、同上。デモ起動をイベントハンドラから内部メソッドに切り出して自動再生から呼ぶ (→ 同上)
- [x] 1.4 KMP: 引数解析 (iOS / Android 各 OS 側) の結果を共有の設定型として共有 Presenter に渡す。自動再生のディスパッチは design Decision 3 の表どおり — Presenter 経由のデモは共有側、Inline とパネル系は OS UI 層 (→ 同上)
- [x] 1.5 4ルートの安定デモ ID・キー名・異常系挙動 (定義外 ID 無視・不正値既定動作・one-shot) が同一であることを突き合わせ確認 (→ Requirement: 撮影支援設定の4ルートパリティ)

## 2. 撮影スクリプト (scripts/capture/) — 取り下げ (2026-08-27 改訂)

- 2.x は全件取り下げ: iOS シミュレータへの座標タップをスクリプトから注入する CLI 手段が環境に無く (simctl 単体に入力注入なし)、外部ツール導入・ランナー同梱はオーナー判断で不採用。撮影はエージェント手順 (デモ駆動モード + simctl / adb / シミュレータ操作ツール) で行う

## 3. テスト (scripts/capture/tests/) — 取り下げ (2026-08-27 改訂)

- 3.1 は 2.x の取り下げに伴い消滅 (CA-CT 系 Scenario 自体を specs から削除)

## 4. ドキュメント追随

- [x] 4.1 kasane/config.yaml `ui.screenshot` をデモ駆動モード前提の撮影手順に書き直す: 一時改変・手動でのデモ到達の記述を起動引数 (`--demo` / `--es demo`、`loading-step-interval-ms`、デモ ID 9件は samples/README.md 参照) に置き換え、残す周辺知識 (座標の測り方の原則・IME・遷移確認の原則) は維持 (→ Requirement: 指定デモの自動再生 / Loading 刻み間隔の起動時指定)
- [x] 4.2 kasane/concepts/cross/conventions/sample-parity.md に「撮影支援機構 (画面に出ない・引数なしなら挙動不変) はデモ項目の一致要件の枠外」を追記し、concepts/log.md に記録 (→ Requirement: 撮影支援設定の起動引数受け口)
- [x] 4.3 samples/README.md に起動引数 (外部表現・キー名・デモ ID 一覧) の説明を反映 (→ Requirement: 指定デモの自動再生)

## 5. 検証

- [x] 5.1 scenario-id-coverage の allow-missing に CA-SA-01〜07 を理由付きで登録 (design Decision 5 の割り付け: Sample 専用・4ルート通し証跡で検証。LD-SA 系と同じ扱い) (→ samples 全 Scenario)
- 5.2 は取り下げ (2.x / 3.x の消滅に伴う)
- [x] 5.3 4ルート通し検証: 引数なし起動の不変 (CA-SA-01)、9デモの自動再生と期待状態 (CA-SA-02, 03, 07)、刻み間隔 (CA-SA-04, 05)、同一引数でのパリティ (CA-SA-06) を evidence 証跡として収集 — 収集はデモ駆動モードの起動引数 + simctl / adb / シミュレータ操作ツールで行う (→ samples 全 Scenario。CA-SA-07 は android / kmp-android / maui-android が実地の再生成、iOS 3アプリはオーナー手動回転の実地証跡 + 構造的保証。内訳は証跡の該当節が正)
