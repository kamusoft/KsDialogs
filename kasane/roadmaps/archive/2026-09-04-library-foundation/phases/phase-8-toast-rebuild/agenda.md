# Toast 新実装

原典で Obsolete 宣言されていた Toast を、ベタ移植せずコンセプトから再設計して KsDialogs で復活させる change フェーズ。

## 論点


## 決定事項

- **Toast の基本要件** (2026-08-27 オーナー提示):
  - ページをまたげる — 常に Window 最上位に表示される
  - ページの要素を妨げない — Toast 表示中もページは通常どおり操作可能 (非モーダル)
  - 多重起動可 — 起動順に重なるだけ (スタック管理はしない)
  - 出入りのアニメーションは Dialog / Loading の演出機構を踏襲
  - 配置 (レイアウト属性) も Dialog を踏襲
- **デフォルト View とカスタム View の両対応** (2026-08-27): メッセージ文字列だけで出せる入口 + ライブラリ同梱の既定見た目を設け、カスタム View も併設する (Loading の core/ADR-0023 と同型)。原典の「カスタム View 前提・message API なし」は継承しない → [core/ADR-0028](../../../../decisions/core/0028-toast-default-and-custom-view.md) (proposed)
- **カスタム View の供給経路は型指定レジストリ + インライン factory の両対応** (2026-08-27): レジストリは共有層 (KMP commonMain・MAUI の VM 層) が UI 型に触れずにカスタム Toast を呼ぶための間接層。Toast は結果を返さないため notifier / 結果型まわりを落とした軽い形。インライン factory は UI 層内で完結する単発表示向け → [core/ADR-0029](../../../../decisions/core/0029-toast-registry-and-inline-factory.md) (proposed)
- **器は Loading の器の非モーダル派生・1 Toast 1器・Loading が最上位** (2026-08-27): 提示スタック不参加の器 (iOS = key window 直貼り / Android = 全画面透過 Window、core/ADR-0026 の形) をタッチを奪わない非モーダル版として派生させ、レイアウト・演出は共有部品をそのまま通す。Toast 1つにつき器1つで多重の重なりは追加順。ただし Loading とは追加順によらず常に Loading が前面 → [core/ADR-0030](../../../../decisions/core/0030-toast-container-implementation-form.md) (proposed)
- **Toast は完全非対話・時間経過でのみ消滅・show は fire-and-forget** (2026-08-27): Toast の面もタッチ素通しで、真下のページ要素も操作可能。show は戻り値なし、duration は show 引数の ms 指定 (既定 1500ms 原典踏襲、OS 由来の 3.5 秒クランプは継承しない)。ページをまたいで生き残る以上、対話要素はページ文脈が壊れるため置けない → [core/ADR-0031](../../../../decisions/core/0031-toast-non-interactive-fire-and-forget.md) (proposed)
- **演出の適用形は Loading と完全同型** (2026-08-27、phase-5-3 申し送りの解消): カスタム Toast は `DialogTransition` (core/ADR-0017 のフック機構) をコンテンツ定義へ添付可、デフォルト View は器の既定クロスフェード固定。Toast は結果を持たないため ADR-0017 の「結果のラッチと配送」は対象外でフック機構のみが乗る。既定 View への演出選択の口は設けない (将来 `ToastStyle` への追加は互換で可能)。独立 ADR は起こさず propose 時に toast-semantics へ落とす
- **デフォルト View は OS 慣習寄せの角丸ピル・既定配置は下部中央 + ボトムバー回避オフセット・styling は `ToastStyle` で一括設定** (2026-08-27): 半透明ダークグレー背景 + 白文字の角丸ピル (Toast は覆いを持たないため既定 View が自分で背景を描く)。既定配置は visibleArea 基準の下部中央に、標準的なボトムバー1本ぶんを避ける上方向オフセット (具体値は propose で確定。iOS タブバー 49pt / Material ボトムナビ 80dp が目安)。タブバーはアプリ側 UI で visibleArea では避けられないため、`ToastStyle` に「アプリ既定の配置」を持たせて一括調整可能にする (LoadingStyle にない Toast 固有項目)。ToastStyle の規律は core/ADR-0023 同型 (背景色・文字色・フォントサイズ・角丸・既定 duration・アプリ既定配置 / show 引数にしない・各表示開始時に読む・KMP commonMain から設定不可)。既定配置が Dialog (中央) と違うのは core/ADR-0008 の意図的乖離として明示 → [core/ADR-0032](../../../../decisions/core/0032-toast-default-view-and-placement.md) (proposed)
- **コンセプトと OS ネイティブ機構との関係は決定事項の積み上げで定義済みとして解消** (2026-08-27): 原典の Obsolete 理由は一次情報なし (状況証拠: Android の OS Toast カスタム View 依存が API 30 で非推奨化)。自前の器 (ADR-0030) で原典の死因と制約 (3.5 秒クランプ・SetGravity 配置・多重不可) を構造的に解消。提供価値 = カスタム View・Dialog 踏襲の配置と演出・多重の重なり・クランプなし duration・3形態統一 API。意味論は OS Toast の「非対話・時間で消える」を踏襲し、対話が欲しいケースは Dialog が受け皿

- **Sample のデモ項目** (2026-08-27、完了条件 — cross/ADR-0007 パリティ・cross/ADR-0010 デモ駆動撮影に準拠):
  1. メッセージ Toast — `show(message)` 一発 (デフォルト View・既定配置)
  2. カスタム View Toast — レジストリ経由 + インライン factory の両方
  3. 多重起動 — 連打して起動順に重なることを見せる
  4. 配置・duration の変更 — `DialogPlacement` 上書きと長め duration
  5. 機能間多重起動 — Dialog / Loading / Toast を同時に出し、重なり規則 (Loading が常に前面 = core/ADR-0030、Dialog と Toast は保証なし = core/ADR-0006) と非モーダル併存を検証する

## TODO

- [x] 論点の解消 (2026-08-27 全8論点解消・決定事項へ昇格)
- [x] ksn-propose で変更提案を起こす (add-toast、L 級)

## 実装結果 (2026-08-28 反映)

[add-toast](../../../../changes/archive/2026-08-28-add-toast/proposal.md) として実装完了 (verify VALID・レビュー4回 + セカンドオピニオン、Sample 5デモ4ルート通し・失敗系実機観測まで実施)。決定事項からの主な乖離・追加は deviation.md が正。要点:

- **スコープ追加 (オーナー決定)**: MAUI iOS の factory 例外境界修正の過程で判明した Loading / Dialog の同型の穴を本変更に同梱して修正。iOS native の factory 閉包契約は Toast / Loading / Dialog とも throws 化 (公開 protocol への外部準拠には breaking — 一般公開前で既知の外部準拠なし)。この決定は core/ADR-0033 として蒸留時に起票
- **Sample メニューのスクロール化 (オーナー決定)**: デモ項目 9→14 件で小画面端末の結果表示が画面外に出るため、4ルートに同梱
- **付随修正 2 件**: Android 既定 announce の支援技術オフ端末でのクラッシュ (ガード + テスト追加)、Compose 観察テストのレイアウト待ち

申し送りのルーティング:

- **原典 `IToast.Show<TView>` からの移行者向け対応表** → phase-9-docs の agenda に追記済み (design Migration Plan の申し送り)
- **MAUI iOS 互換面 (bridge) の自動テスト標的** (nil 供給経路が自動テストの射程外 — review-003 Suggestion) → 簡易起票 [add-maui-ios-bridge-nil-supply-test](../../../../changes/add-maui-ios-bridge-verification/exploration.md) へ (2026-09-02 に add-maui-ios-bridge-verification へ統合)
- **Android の Loading / Dialog provider の `BridgeContentSupply` 対称化** → 見送り (Kotlin 側の `catch (Throwable)` で観察可能挙動は成立 — review-003 の整理。core/ADR-0033 の Consequences に将来課題として記録済み)
- **Android の Loading 再前面化の間引き** (高頻度 Toast + Loading 併用のちらつき対策) → 見送り (design Risks に記録した最適化余地。挙動契約は変わらず、実害の観測があれば別変更で対応)
- **ビルド衛生・環境系の発見事項** → 実装中に簡易起票済み: [fix-maui-ios-stale-native-link](../../../../changes/add-maui-ios-bridge-verification/exploration.md) (2026-09-02 に add-maui-ios-bridge-verification へ統合) / [fix-ios-toast-presentation-wait-flake](../../../../changes/fix-ios-toast-presentation-wait-flake/exploration.md)
