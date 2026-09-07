# Proposal: add-presentation-behavior

## Why

phase-5-3 (提示挙動の完成) の議論で確定した8決定を実装に落とす。多段表示・回転の挙動は実装済みだが共通仕様テストによる固定がなく退行を検知できない。提示/退出アニメーションは原典の主要機能でありながら未移植で、Android の既定トランジションは実質無演出。immersive アプリではダイアログ表示でシステムバーが再出現し、KMP Kotlin 経路だけが結果通知契約 (呼び出し元キャンセルで閉じる) に不適合のまま残っている。

## What Changes

1. **挙動系共通仕様テストの器** (core/ADR-0016): 多段表示の系列挙動 (重ね出し・下を先に閉じる・器消失→cancelled) を Native 2実装への同名 Scenario テストで固定する。OS 間差の正は concepts/core/api/multi-display-semantics.md の差分表
2. **回転追随の裏取り**: 既存の毎レイアウトパス再計算機構による回転追随を実機確認し Scenario テストで固定する。原典 iOS の「提示後サイズ固定」は継承しない意図的乖離 (core/ADR-0008 の枠)
3. **DialogTransition 添付スロット** (core/ADR-0017): presentation / dismissal の完了通知つき両フックを第3の添付物として4形態 × 2技術系に導入。フックは統一形 (コンテンツのホスト View を受け取る)。fade / slide 等のプリセット factory (時間・easing 引数、easing は形態ネイティブ表現)。既定トランジションは OS 既定からライブラリ実装のクロスフェード (250ms 相当) へ置き換え
4. **immersive 表示状態の引き継ぎ**: Android 11+ 経路で、ホストウィンドウのシステムバー表示/非表示状態と systemBarsBehavior をダイアログウィンドウへ引き継ぐ (DialogWindowSystemBars の宣言済み目的の完成)
5. **KMP Kotlin 経路のキャンセル追随** (kmp/ADR-0005 後続): IosDialogGateway が機械面の show ハンドルを invokeOnCancellation から引き、呼び出し元キャンセルで当該ダイアログを閉鎖。doc コメント訂正含む
6. **concepts 追随**: `transition-semantics.md` を新設 (トランジションの公開契約: 添付スロット・フック保証範囲・結果のラッチと配送・既定トランジション) し、result-notification-semantics.md (MD-d を「まだ決めていないこと」から「OS の既定処理に委ねる」へ、ラッチと配送の用語、形態別キャンセル観察)・multi-display-semantics.md (閉鎖アニメ時間をライブラリ既定へ、Scenario ID 参照)・layout-semantics.md (キーボードで動かさない・寸法/インセット変化への追随) を更新
7. **Sample**: transition デモ (プリセット選択 + カスタムフック実演) をパリティ準拠で4形態に追加 (完了条件)

## Non-Goals

- 原典 KeyboardListener の移植 — 唯一の実効果 (キーボード中の戻る無視) は契約化しないと決定済み (MD-d)
- AutoRotateForIOS (回転抑止スイッチ) — phase-7 (Loading) の agenda へ申し送り済み
- 宣言的 UI の phase 通知形フック (B案) — 将来の非破壊追加候補 (core/ADR-0017 Alternatives)
- キーボード回避 (ダイアログの位置調整・リサイズ) — 原典に無い機能追加
- Loading / Toast への transition 適用 — 各機能フェーズ (phase-7 / phase-8) で扱う

## Impact

- 公開 API 追加: DialogTransition + 添付面 (Android View 拡張プロパティ / Compose KsDialogAttributes 引数 / SwiftUI modifier + UIKit 拡張 / MAUI 添付プロパティ)。KMP 共有コード側の API 増なし (kmp/ADR-0002)。MAUI ブリッジ (iOS ObjC / Android Java 互換面) にトランジション実行口の ABI が増え、binding 再生成を伴う
- 観察可能な挙動変化 (API 非破壊): (1) 既定トランジションが OS 既定 → ライブラリクロスフェードに変わる (2) **添付の有無によらず、全 show の完了時刻が既定の退出処理分 (約 250ms) 遅れる** — 結果は最初の報告でラッチされ、配送が撤去後になるため。即時配送を前提にした利用コード・テストは影響を受ける
- リスク: 退出待ちの導入で「閉じたのに結果が返らない」経路が利用者のフック次第で生じうる。フックの例外・失敗は撤去・配送を続行して吸収するが、**完了しないフックは契約違反 (利用者責務) として扱い、タイムアウトは設けない** — 脱出口は呼び出し元キャンセル (Swift / Kotlin) と OS 発の器消失で、デバッグビルドでは警告ログを出す (design Decision 5-8)。immersive 引き継ぎの API レベル依存差

## 級: L

5ドメイン横断 (core 契約 + ios / android / maui / kmp の公開 API 表面追加) + 挙動テスト枠の新設。

domain: cross
roadmap: library-foundation/phase-5-3-presentation-behavior
