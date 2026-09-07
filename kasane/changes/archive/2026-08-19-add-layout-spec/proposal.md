# Proposal: add-layout-spec

改訂履歴: 2026-08-18 全面改訂 — design Decision 6 (属性は VM 契約) のオーナー差し戻しと KMP 実装ブロッカーを受け、core/ADR-0014 (属性の取捨)・core/ADR-0015 (供給機構) を反映。経緯は exploration-redesign.md。

## Why

core/ADR-0007 は「レイアウト計算は観察可能な規則を core 仕様として1本化し、実装は各 OS のレイアウト機構に委ねる」と方向を決めた。初版提案 (Decision 6: 属性は VM 契約のオプションメンバ) は実装フェーズで差し戻しになった — VM に UI 要素が入り責務が曖昧になる・インライン show (core/ADR-0013) に運び手がない・KMP では言語制約と interop 配管不在で実装不能。再設計議論で属性を「器にしか実現できないメタ要素」に絞り (core/ADR-0014)、供給は「コンテンツ定義への添付 + show 引数」に確定した (core/ADR-0015)。規則・ケース表・実装・検証を、この確定契約で一体導入する。

## What Changes

- **dialog-contract (core)**: メタ属性10個の契約化 — 静的メタ `DialogOptions` (layoutArea / dialogMargin / proportionalWidth / proportionalHeight / overlayColor / isCanceledOnTouchOutside) + 動的メタ `DialogPlacement` (horizontalAlignment / verticalAlignment / offsetX / offsetY)。供給は「コンテンツ (View) 定義への添付」+「show 引数の placement 上書き」(優先順位: show > 添付 > 既定)。サイズ・配置の決定規則 (優先順位は明示サイズ廃止により「比率 > Fill > 内容」に簡素化) と共通ケース表の改訂
- **ios-native**: `UIView` extension プロパティ (associated object) による添付 + show placement 引数 + レイアウト実装 + 提示前サイズ確定 + isCanceledOnTouchOutside。実装済みの VM 経路 (`DialogLayoutProviding`) は撤去・置換
- **android-native**: `View` extension プロパティ (setTag) による添付 + show placement 引数 + 同上の撤去・置換。透明オーバーレイの正式対応と基準領域修正 (実装・実証済み) は維持
- **maui-binding**: 添付プロパティ (`ksd:Dialog.*`) → `DialogOptions` / `DialogPlacement` に束ねて Native の同型オブジェクトへ写像 + Show の placement 引数。当たり判定検証 (完了済み — 不具合非実在の解明と証跡) は維持
- **kmp-facade**: `DialogPlacement` を commonMain の具象 data class として公開し、show に placement 引数を追加 (`DialogOptions` は供給経路が KMP に存在しないため公開しない — 各 OS の View 定義側で完結し境界を渡らない)。iOS interop 面に placement DTO の配管を1本追加 (MAUI で実証済みのパターン)
- **テスト**: ケース表 (改訂版) の Native 全量検証 + 添付・show 引数・優先順位の検証 + MAUI/KMP パススルー検証 + isCanceledOnTouchOutside の挙動検証
- **samples**: レイアウトデモ (属性調整パネル、承認済みモックのまま) — placement は show 引数、layoutArea トグルは factory 内での View 添付で実現 + モック改訂の反映 (タップ領域・surface-variant・初期非表示)

## Non-Goals

- **廃止5属性** (width / height / cornerRadius / borderWidth / borderColor): View 自身の責務 (core/ADR-0014)。原典ユーザー向けの移行ガイド記載は phase-9
- **SwiftUI / Compose の添付 DSL** (`.ksDialogOptions()` modifier / `KsDialogAttributes` composable): 宣言的 UI の登録経路自体が expand-api-surface のスコープのため、DSL 実装もそちらへ申し送る (本 change では実現可能性プローブの結果を design に記録し、契約の意味論だけ確定する)
- クランプで rect が内容サイズより小さい場合の中身の見え方 (意図的未規定のまま維持 — layout-semantics の「まだ決めていないこと」節が正)、immersive 時のシステムバー表示状態の引き継ぎ (phase-5-3 候補)。※ proportional = 0 は「0 以下は未指定」で確定済み (design Decision 6)
- アニメーション・キーボード/回転追随・多段表示 (phase-5-3)、登録 API・show 系統の拡張 (expand-api-surface)、DI 糖衣 (phase-6)

## Impact

- 破壊的変更なし (未リリース。既存 VM・既存登録・既存 show 呼び出しはすべて無変更で動作 = ソース互換、既定値時の初期 rect と見た目 = 現状の内容サイズ・中央配置を維持)。**操作面の例外が1つ**: 外側タップは現行実装に機構自体が無く、既定 true で新規に cancelled を返すようになる (原典既定への一致。視覚・ソース互換は維持)
- **実装済み資産の扱い**: レイアウト計算エンジン・ケース表検証基盤・透明オーバーレイ対応・hit-test 解明は温存。VM 経路 (`DialogLayoutProviding` 系、各形態) は撤去・置換の手戻り
- ケース表は本改訂で新版を作成し直して再凍結する (明示サイズ・描画系ケースの除去と優先順位簡素化の反映)
- samples のメニュー・mock は expand-api-surface と共有 — 実装順序は本変更 → expand-api-surface (変更なし)
- リスク: (1) SwiftUI / Compose の添付運搬が「初回レイアウトパスまでに届く」ことが未実証だと ADR-0015 の意味論が宙に浮く — 本提案フェーズでプローブ実施済みであること (2) 添付の読み取り境界 (factory 直後〜初回レイアウトパス完了) の器実装が形態間でずれると優先順位規則が実装差になる — ケース表と優先順位 Scenario で強制解消する

## 級: L

5ドメイン横断 + core 契約とテスト基盤の確立 (覆すコスト高) のため。

domain: cross
roadmap: library-foundation/phase-5-1-layout-spec
