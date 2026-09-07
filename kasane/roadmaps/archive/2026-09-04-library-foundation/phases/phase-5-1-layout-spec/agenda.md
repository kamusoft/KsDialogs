# レイアウト仕様と共通テスト

ExtraView 相当のレイアウト属性一式を core 仕様化し、3形態同型の共通仕様テストで検証する change フェーズ (phase-5-dialog-completion から分割)。

## 論点

(すべて決定事項へ昇格済み)

## 素材

- [scout-origin-layout-attributes.md](artifacts/scout-origin-layout-attributes.md) — 原典レイアウト属性の全量 (型・既定値・iOS/Android 実装差・仕様の穴) と KsDialogs 現状の調査 (2026-08-17)

## 決定事項

- **レイアウト属性一式の取捨 (2026-08-17)**: 原典踏襲ベースで全コア属性を残し、原典実装の歪みは仕様の一貫性で上書きする。詳細は [scout-origin-layout-attributes.md](artifacts/scout-origin-layout-attributes.md) の属性表 + 以下の確定規則:
  - Offset 座標系は常に「+X 右 / +Y 下」(原典 Android の End 時符号反転は継承しない)
  - DialogMargin は最大サイズ制約に加えて位置決めにも効く (Start/End 時に端から Margin 分離れる)
  - Border は内側描画でコンテンツを押し出さない (iOS 型に統一)
  - OverlayColor 透明時に Android でステータスバーだけ暗転する問題は、原典ハックの意図を継承して正式対応を模索する
  - UseCurrentPageLocation は廃止せず再設計して維持: 両軸で効く形にし、基準は「システムバーを除いた領域」。プロパティ名は分かりやすく改名可
  - 初期サイズは VM Bind 完了後に確定する (描画後 Bind で内容依存の高さが反映されない原典の課題を初期化順序で解決)。Bind 後の継続的な動的リサイズは初版範囲外
  - IsCanceledOnTouchOutside は属性として存続 (規則詳細は 5-3)、AutoRotateForIOS は 5-3 へ移送
  - ADR: core/0008 (proposed) に原典からの意図的乖離として起票
- **レイアウト共通仕様テストの器 (2026-08-17)**: 共通ケース表方式を採用 — 「画面サイズ・属性入力 → 期待 rect」のデータ表 (JSON 等) を core 仕様の隣に置き、各形態のテストが同じ表を読み込んで検証する。責務分担は「ケース表の全量検証は Native 2実装 (iOS/Android)、MAUI/KMP は属性パススルーの同型テスト」(core/ADR-0001 の薄いラッパー構成に整合)。実行環境の詳細 (Android JVM テストでの実レイアウト検証範囲等) は spec 化で詰める。ADR: core/0009 (proposed)
- **MAUI iOS 当たり判定の扱い (2026-08-17)**: 共通ケース表の検証項目に hit-test (期待 rect の中心への合成タップでハンドラ発火) を含めて構造的に捕捉し、原因究明・修正は change の実装タスクへ送る。要求 (当たり領域 = 描画領域) は原因に依存しないため、議論フェーズでの原因特定は行わない。ADR は起こさず ADR-0009 のケース表仕様の増分として spec 化に反映
- **Sample UI の未決4件 (2026-08-17)**:
  1. 初期状態の結果表示エリア非表示: 実装者判断を正式採用し、モック側にも昇格する
  2. ボタンタップ領域 38pt: モックを改訂し HIG 44pt / Material 48dp 以上へ引き上げる
  3. surface-variant トークン: モック側のトークン定義へ昇格する
  4. samples Android の View 技術: 「素の View か Compose か」の二択ではなく、**ライブラリ本体がコンテンツ View 技術の両対応を必須とする** — Compose / SwiftUI でダイアログを書ける対応は必須、Android.View / UIView 対応も必須 (MAUI 連携の前提)。参考: KsSettingsView。ADR: core/0010 (proposed)。登録 API・ホスティングの設計論点は phase-5-2 (API 表面) へ追加。5-1 のレイアウト検証 Sample は現行 View 構成のまま進める

## TODO

- [x] 論点の解消
- [x] ksn-propose で変更提案を起こす (モック改訂 — タップ領域 44pt/48dp・surface-variant トークン・初期非表示の明記 — を含める)
- [x] 完了条件: パリティ準拠の Sample 通し (sample-walkthrough 証跡あり)

## 実装結果 (2026-08-19 反映)

- change: [add-layout-spec](../../../../changes/archive/2026-08-19-add-layout-spec/proposal.md) — 実装完了・main マージ (7b3f444)・蒸留済み。レビュー3周 (ホスト + 相方 codex) APPROVED・verify VALID
- **設計は実装フェーズで1度差し戻しになり全面改訂**: 初版「属性は VM 契約のオプションメンバ」→「器のメタ属性10個 (DialogOptions / DialogPlacement) + コンテンツ添付供給」(core/ADR-0014・0015)。経緯は [exploration-redesign.md](../../../../changes/archive/2026-08-19-add-layout-spec/exploration-redesign.md)
- **決定事項「MAUI iOS 当たり判定の扱い」の帰結**: phase-4 記録の不具合疑いは実環境再現で**非実在と解明** (描画中心座標の取り違えが原因。[verification/maui-hit-test](../../../../changes/archive/2026-08-19-add-layout-spec/verification/maui-hit-test/notes.md))。当たり領域 = 描画領域は Scenario と実測で担保。config.yaml のスクショ手順の誤記録も訂正済み
- deviation 2件 (パネル戻る導線 A案・パネル内結果表示 — いずれもオーナー指示) は sample-parity.md へ反映済み
- ADR: core/0007・0008・0009・0014・0015 を accepted 昇格 (2026-08-19)
- 申し送りのルーティング (すべて受け皿確定):
  - SwiftUI / Compose 添付 DSL・spec 検査方式の読み替え・パネル内操作部の a11y 残課題・MAUI スナップショット配線の実機確認 → [phase-5-2 agenda](../phase-5-2-api-surface/agenda.md) の申し送り節
  - immersive 時のシステムバー表示状態の引き継ぎ → [phase-5-3 agenda](../phase-5-3-presentation-behavior/agenda.md) の論点
  - 廃止5属性 (width / height / cornerRadius / border) の移行ガイド記載 → phase-9 (docs) のスコープ (core/ADR-0014 Consequences に明記済み)
  - クランプ時の中身の見え方 → 意図的未規定として見送り (layout-semantics.md「まだ決めていないこと」が正)
  - kmp/.swiftpm-locks の VCS 不整合 (review-004 Suggestion) → マージ時点で解消済みを蒸留時に確認 (3サブパッケージすべて追跡、生成 Package.swift と整合)。追加対応なし
