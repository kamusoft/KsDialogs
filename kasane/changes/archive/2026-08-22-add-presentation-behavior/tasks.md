# Tasks: add-presentation-behavior

Scenario ID (`PB-xx-NN`) は specs/ を参照。テスト名に ID を含める (design Decision 7)。

## 1. 挙動 Scenario テストの器 (ADR-0016 初適用)

- [x] 1.1 iOS/Android: 既存 `DialogMultiDisplayTests` 各4本を温存し、テスト名に PB-MD-01〜04 の ID を1対1で付与 (改名のみ、期待値不変。PB-MD-04 は共通 ID で THEN のみ OS 差) (→ Requirement: 多段表示の系列挙動の固定)
- [x] 1.2 iOS: 実提示機構での PB-MD-04 (下先閉じ → 上は cancelled・フック非実行) と PB-MD-05 (器消失) を追加 (→ 同上)
- [x] 1.3 Android: instrumented で PB-MD-04 (下先閉じ → 上は残る) と PB-MD-05 を追加 + 証跡記録 (→ 同上)
- [x] 1.5 ID 網羅検査スクリプト: 両 Native のテスト名から PB-ID を抽出し specs/ の Scenario ID と突合する検査を追加 (CI / ローカルで実行。spec にメタ Scenario は置かない — design Decision 7)
- [x] 1.4 iOS/Android: PB-WN-01〜03 (回転 / 寸法のみ / インセットのみ) の Scenario テスト + 実機裏取り証跡 (→ Requirement: 表示中のウィンドウ寸法変化への追随)

## 2. DialogTransition — 器の駆動機構 (design Decision 2・4・5・10)

- [x] 2.1 iOS/Android: 結果チャネルに内部 `DismissalOrigin` を追加し、報告 / 外側タップ / 戻る / 呼び出し元キャンセル / 器消失を識別 (公開面には出さない) (→ Requirement: 退出の開始条件と直列化)
- [x] 2.2 iOS/Android: 結果のラッチ (既存) と配送の分離 — 配送を dismissal 完了 + オーバーレイ消滅 + 撤去の後へ (→ Requirement: 結果のラッチと配送)
- [x] 2.3 iOS: crossDissolve を廃し、オーバーレイとコンテンツを別レイヤで器が自前駆動 (animated: false、attached 状態ではコンテンツ非表示、凍結後に presentation 開始、既定クロスフェード内蔵) (→ Requirement: トランジションの添付)
- [x] 2.4 Android: 同上の自前駆動 (show 後 presentation / dismiss 前 dismissal、オーバーレイ別レイヤ、既定クロスフェード内蔵) (→ 同上)
- [x] 2.5 iOS/Android: 状態機械 (design Decision 10 の遷移表: 提示前閉鎖は演出なし、presentation 完了 = フック + オーバーレイ、presentation 中の閉鎖は完走後に直列、dismissing 中の入力無視、dismissal は高々1回、OS 発消失は実行中フックをキャンセルして直行) とフック失敗の吸収 (ログのみ、撤去・配送続行)、UI スレッド開始 (→ Requirement: 退出の開始条件と直列化 / フックの失敗 / 結果のラッチと配送)
- [x] 2.5b iOS/Android: 終了しないフックの脱出口 (呼び出し元キャンセル / OS 発消失で実行中フックの Task / Job をキャンセル) とデバッグビルドの警告ログ (既定 5 秒) (→ Requirement: フックの完了は利用者の責務)
- [x] 2.6 Android: 呼び出し元キャンセル後の退出処理を `NonCancellable` 相当で完遂 (→ PB-AA-03)
- [x] 2.7 iOS/Android: PB-TR-01〜29 の Scenario テスト (添付・直列化・早期閉鎖・状態別の呼び出し元キャンセル・ラッチと配送・OS 発消失時のキャンセル・失敗・終了しないフックの脱出口・プリセット・none・duration の有効範囲外)、テスト名に ID を含める (→ 上記各 Requirement)
- [x] 2.8 既存の結果通知・レイアウト・多段表示テスト全通し (退行確認: 宙吊り新経路なし・スナップショット凍結不変・配送遅延の影響で即時配送を前提にしたテストがあれば更新)

## 3. DialogTransition — 添付面とプリセット (design Decision 1・3)

- [x] 3.1 iOS: `DialogTransition` (Hook は async throws) / `DialogTransitionEdge` / `UITimingCurveProvider.standard` + `UIView.ksDialogTransition` + `View.dialogTransition(_:)` + プリセット factory (design Decision 3 の完全シグネチャどおり) (→ Requirement: トランジション添付面 (iOS) / トランジションのプリセット)
- [x] 3.2 Android: `DialogTransition` / `DialogTransitionEdge` + `View.ksDialogTransition` + `KsDialogAttributes(transition)` + プリセット factory (→ Requirement: トランジション添付面 (Android) / トランジションのプリセット)
- [x] 3.3 MAUI: `DialogTransition` / `DialogTransitionEdge` + `Dialog.TransitionProperty` (Set/Get) + プリセット factory (MAUI アニメ API 実装) (→ Requirement: トランジション添付面 (MAUI) / プリセットの MAUI 表現)
- [x] 3.4 iOS/Android: PB-IA-01/02・PB-AA-01/02 の添付面 Scenario テスト (View / 宣言的 UI 両系でホスト View が渡る、UI スレッド) (→ 同上)
- [x] 3.5 api-surface-check (iOS / Android / MAUI) に添付面・プリセット・禁止形 (show 引数 transition なし・DialogOptions にトランジション系なし・none 引数なし) を追加

## 4. MAUI ブリッジ (design Decision 8)

- [x] 4.1 iOS ObjC 互換面: `runPresentation(view, completion)` / `runDismissal(view, completion)` 相当の実行口を内容物に追加、completion の多重呼び出し防止 (→ Requirement: トランジション添付面 (MAUI))
- [x] 4.2 Android Java 互換面: 同上
- [x] 4.3 MAUI binding 定義 (iOS / Android) の再生成と C# アダプタ (Task → completion 変換、fault / cancel は完了扱い + ログ、UI スレッド開始、delegate の寿命をダイアログに揃える) (→ 同上)
- [x] 4.4 MAUI: PB-MA-01〜05 のパススルー / 完了待ち / fault / 多重通知なし / プリセットのテスト (→ 同上)
- [x] 4.5 KMP: PB-KC-03 (KMP 登録コンテンツの添付) のパススルーテスト (→ Requirement: KMP 登録コンテンツへのトランジション添付)

## 5. システムバー表示状態の引き継ぎ (design Decision 9)

- [x] 5.1 DialogWindowSystemBars: API 30+ 経路に status / navigation 別の可視状態 + systemBarsBehavior の引き継ぎを追加 (旧経路は既存のまま) (→ Requirement: システムバー表示状態の引き継ぎ)
- [x] 5.2 Android: PB-SB-01〜07 の instrumented テスト (API 29 / 30+ エミュレータ) + PB-SB-01〜03 の実機証跡 (API 30+ 1台) — 受け入れ方式は design Decision 9 の表に従う (→ 同上)
- [x] 5.3 iOS: PB-IA-03 (ステータスバー非表示の提示元で再出現しない) の Scenario テスト (→ Requirement: ステータスバー表示状態の非干渉 (iOS))

## 6. KMP Kotlin 経路のキャンセル追随

- [x] 6.1 IosDialogGateway: showViewModel の戻りハンドルを invokeOnCancellation から引く + doc コメント訂正 (→ Requirement: Kotlin 経路の呼び出し元キャンセル追随)
- [x] 6.2 PB-KC-01/02 のテスト (表示中キャンセル → CancellationException + 閉鎖、提示前キャンセル) (→ 同上)

## 7. concepts 追随 (文書更新)

- [x] 7.1 `concepts/core/api/transition-semantics.md` 新設: 添付スロット・両フックと統一形・置き換えとオーバーレイ別レイヤ・プリセット・結果のラッチと配送・フック実行範囲と失敗・形態別キャンセル観察 (公開契約として)
- [x] 7.2 result-notification-semantics.md: MD-d を「まだ決めていないこと」から「OS の既定処理に委ねる (保証しない)」へ移記、ラッチと配送の用語導入、形態別キャンセル観察 (Swift `.cancelled` / Kotlin `CancellationException` / MAUI 経路なし)、KMP Kotlin 経路の適合を反映
- [x] 7.3 multi-display-semantics.md: 「閉じるアニメーション中のタイミングは OS・実装依存」をライブラリ既定の退出処理へ更新、Scenario ID (PB-MD) の参照を追記
- [x] 7.4 layout-semantics.md: キーボードで動かさない・ウィンドウ寸法/インセット変化への追随 (IME 対象外) を追記

## 8. Sample (完了条件)

- [x] 8.1 4形態にメニュー項目 `Transition Dialog` + デモ画面 (承認モック案B準拠、文言表一致) (→ Requirement: トランジションデモ)
- [x] 8.2 プリセット選択 + 時間/イージング調整 + Custom Hook 実演の動作実装 (ui/brief.md の調整面規範表どおり: 初期値・範囲・None 時の無効化・Custom は調整値を使わない・方向/イージング写像) (→ 同上)
- [x] 8.3 mock との視覚照合 (4ルートスクリーンショット vs approved.png、構造/トークン/状態/意図の4観点)
- [x] 8.4 パリティ準拠の Sample 通し + 演出の実機確認証跡 (動的モック mock-preset-motion.html の動作イメージとの突き合わせ。動的モックは参考であり受け入れ基準は dialog-contract の Scenario)
