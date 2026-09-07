# Proposal: add-sample-capture-automation

## Why

Sample の撮影証跡 (視覚照合・4ルート通し) の収集が実装フェーズの最大コストになっている。add-loading の実測では Samples フェーズ約 139 分の大半が「6アプリのビルド+デプロイ」「撮影用の一時改変 (刻み間隔延長・デモ先出し) の出し入れと復元確認」「状態を作る手動タップの再現」で、撮影そのものではなかった。一時改変の復元では実装の巻き戻し事故も発生している (lessons/inbox の verification-temp-edit-restore-by-copy-not-checkout)。撮影手順は kasane/config.yaml の `ui.screenshot` に文章メモがあるだけで、ワーカーが毎回手順を再構築している。

方式は探索で確定済み (ADR cross/0010、status: proposed)。

## What Changes

1. **Sample のデモ駆動モード** (影響能力: samples — 4ルート6アプリ): 起動時の引数渡し (iOS = launch argument / Android = intent extra) で「指定デモの自動再生 (安定デモ ID 9件を新設)」「Loading 進捗の刻み間隔の変更 (ms 直指定)」を受け付ける。引数なしの通常起動は挙動不変。自動再生はプロセス起動につき1回消費 (コールド起動前提)。デモ別のディスパッチ責務は design.md Decision 3 の表による — メニュー項目のタップハンドラと同じ入口を UI 層から呼ぶ (パネル系デモと KMP の Inline は OS UI 層)
2. **撮影スクリプト** (capture-tooling — 新設): **取り下げ (2026-08-27 改訂)**。実装フェーズで iOS シミュレータへの座標タップをスクリプトから注入する CLI 手段が環境に無いことが判明 (simctl 単体に入力注入なし。config.yaml の従来メモが挙げる「シミュレータ操作ツール」はエージェント専用 MCP でスクリプトから呼べない)。外部ツール導入 (idb 等)・XCUITest ランナー同梱はいずれもオーナー判断で不採用とし、撮影はエージェント手順 (デモ駆動モード + simctl / adb / シミュレータ操作ツール) で行う。効率化の本体 (一時改変の出し入れと追加ビルドの消滅・状態再現タップの消滅) はデモ駆動モードが担う
3. **ドキュメント追随**: config.yaml `ui.screenshot` をデモ駆動モード前提の撮影手順に書き直す / concepts の sample-parity.md に「撮影支援機構はデモ項目の一致要件の枠外 (画面に出ない・引数なしなら挙動不変が条件)」を追記 / samples/README.md に起動引数の説明を反映

## Non-Goals

- **証跡粒度の軽量化** (代表ルートのみフル撮影等): 検証ポリシーの判断であり本 change とは別軸 (探索時に分離を決定)
- **撮影フェーズの並列化** (orchestrator.parallel): Gradle 並行実行の罠の回避設計が前提の別軸
- **ビルド+デプロイの高速化そのもの**: 本 change は「ビルドし直しの回数を減らす」ことで効かせる。ビルド自体の最適化は扱わない
- **ライブラリ本体 (公開 API) の変更**: 一切触れない
- **撮影の入口ツール化** (Python / shell / XCUITest ランナーいずれの形も): 2026-08-27 改訂で What Changes から Non-Goals へ移動。iOS タップ手段の制約と外部依存の不採用による

## Impact

- 破壊的変更なし。ライブラリ本体には触れず、Sample も引数なしの挙動は不変 (デルタスペックで契約化)
- 4ルート一斉変更のためパリティ証跡 (同じ引数名・同じ挙動) が完了条件に入る
- sample-parity.md への追記は ADR-0007 の一致単位の解釈を明文化するもので、既存規約と矛盾しない (ADR cross/0010 の注意書きどおり実装と同時に入れる)
- ADR cross/0010 のうち撮影スクリプト部分 (2部構成の後半・`ui.screenshot` のポインタ化) は 2026-08-27 改訂と食い違うため、蒸留時に ADR の改訂が必要 (deviation.md に申し送り済み)

## 級: L

2能力横断 (samples + capture-tooling 新設) で6アプリと外部ツール (simctl / adb) 連携をまたぎ、引数表現・ライフサイクル・ディスパッチ責務・成果物契約の設計判断を design.md に固定する必要があるため。当初 M で起票したが、spec-review セカンドオピニオン (second-opinion-spec-001) の採用指摘を受けて L に再分類 (2026-08-27 オーナー確定)。画面の見た目に変化はなく ui/ (mock 承認) は不要。

2026-08-27 改訂で capture-tooling を取り下げ実体は samples 単能力 (4ルート) となったが、レビュー・verify は L 級の編成のまま実施する。

domain: cross
