# UI Brief: add-vertical-slice

## 画面と状態

構造は Sample アプリ (4ルート共通・parity 対象) とダイアログ本体:

1. **Sample メニュー画面**: タイトル + デモ項目一覧 (今回は「Basic Dialog」1項目のみ) + 直近の結果表示エリア。状態は通常のみ (loading / empty / error なし)
2. **Basic Dialog (カスタム View ダイアログ)**: メッセージ文字列 (VM が保持) + 完了操作 + キャンセル操作。状態は表示中のみ
3. **結果表示**: ダイアログを閉じた後、メニュー画面の結果表示エリアに completed(値) / cancelled が表示される

文言 (4ルート parity の正。cross/ADR-0007):

- メニュー画面タイトル: `KsDialogs Sample`
- デモ項目: `Basic Dialog`
- ダイアログメッセージ (デモデータ初期値): `こんにちは、KsDialogs!`
- 完了操作の表示: `OK` / キャンセル操作の表示: `キャンセル`
- 結果表示: `結果: completed(true)` / `結果: cancelled`

## リファレンス注釈

references/ なし (先行する参考画像・スクリーンショットはこの変更では持ち込まない。移植元の視覚仕様の踏襲は phase-5 のレイアウト仕様化で扱う)。

## デザイントークン参照

concepts/ にデザイントークン定義はまだない。mock 内で定義する **SampleTheme** (4ルート同一 RGBA。platform 固有 semantic color 禁止 — cross/ADR-0007) を初版のトークンとし、蒸留時に concepts 化を検討する:

- primary: `#2563EB` / on-primary: `#FFFFFF`
- surface: `#FFFFFF` / on-surface: `#1F2937` / on-surface-muted: `#6B7280`
- scrim: `rgba(0, 0, 0, 0.4)`
- 区切り線: `#E5E7EB`

## 承認モック

mock/mock-b.html を採用 (approved.png、2026-08-14 オーナー承認)。角丸大きめ (20px 相当) + 塗りボタン (OK = primary 塗り・キャンセル = 無彩色塗り) の案。mock-a (水平ボタン列 + 区切り線の標準カード案) は不採用。

## 最終照合の記録 (tasks 7.2)

- **照合日**: 2026-08-15
- **照合環境**: iPhone 17 Simulator (iOS 26.5) / Android Emulator API 35 (1080x2340)
- **証跡**: `verification/` 配下 (下表のファイル名)。取得手順は `kasane/config.yaml` の `ui.screenshot`

### 6セル × 経路の確認結果

| セル | 表示 | 完了 (OK) | キャンセルボタン | 外側タップ | 戻るボタン |
|---|---|---|---|---|---|
| Native iOS | 済 (6.5) | 済 (6.5) | 済 (6.5) | 済 `ios-native-outside-tap-cancelled.png` | — (iOS に戻るボタンなし) |
| Native Android | 済 (6.5) | 済 (6.5) | 済 `android-native-cancel-button-cancelled.png` | 済 `android-native-outside-tap-cancelled.png` | 済 `android-native-back-cancelled.png` |
| MAUI iOS | 済 (6.3) | 済 (6.3) | 済 (6.3) | 済 `maui-ios-outside-tap-cancelled.png` | — |
| MAUI Android | 済 (6.3) | 済 (6.3) | 済 (6.3) | 済 `maui-android-outside-tap-cancelled.png` | 済 `maui-android-back-cancelled.png` |
| KMP iOS | 済 (6.5) | 済 (6.5) | 済 `kmp-ios-cancelled.png` | 済 `kmp-ios-outside-tap-cancelled.png` | — |
| KMP Android | 済 (6.5) | 済 (6.5) | 済 `kmp-android-cancel-button-cancelled.png` | 済 `kmp-android-outside-tap-cancelled.png` | 済 `kmp-android-back-cancelled.png` |

いずれも「直前の結果を completed(true) にしてから操作し、`結果: cancelled` へ遷移すること」まで撮って判定した。
戻るボタンは押下後も同じ Activity が resumed のまま (`dumpsys activity activities` で確認) で、画面ごと戻る挙動ではない。

### 多段表示の実挙動 (MD-a / MD-b / MD-c / MD-d)

観測は Native iOS / Native Android の 2 セルを基準として実施 (挙動を決めるのは Native 実装)。
samples は Basic Dialog 1 項目のみのため、**2枚重ね・下への先行報告・入力欄つき View は一時的な観測用の変更**を
samples に入れて観測し、観測後に原本へ戻してチェックサム一致 (全 60 ファイル) を確認した。

| ID | Native iOS | Native Android | 証跡 |
|---|---|---|---|
| MD-a | 上を閉じると上だけ閉じ下が残る → 下も独立に完了 | 同左 | `ios-native-md-a-*.png` / `android-native-md-a-*.png` |
| MD-c | 外側タップで上だけ cancelled、下は表示・未確定のまま | 同左 | `ios-native-md-c-outside-tap.png` / `android-native-md-c-outside-tap.png` |
| MD-b | **上下とも器が消え、下は completed(false)、上の show は cancelled で確定** (初回観測時は上が未完了のまま宙吊りだったが、レビュー対応の手当て後の再観測で cancelled 確定を実測 — `ios-native-md-b-recheck-*.png`) | **下の器だけ閉じ上は残る。上は後から通常どおり完了できる** (手当て後の再観測でも不変) | `ios-native-md-b-*.png` / `android-native-md-b-*.png` |
| MD-d | 非該当 (戻るボタンなし) | **1回目の戻るはキーボードのみ閉じダイアログは残る (未確定)。2回目で cancelled** | `android-native-md-d-*.png` |

### 妥協点 / 未確認

- **MAUI iOS のダイアログ内ボタンの当たり判定**: 描画上のボタン中心 (261,480 pt) への合成タップが反応せず、
  上端寄り (280,470 pt / 140,470 pt) では反応した。旧ビルド (BuildProbe 削除前) でも同じで**この変更による退行ではない**。
  実利用者の指タップは面で当たるため実害は小さいが、**有効なタップ領域が描画より狭い / 上へずれている疑い**があり、
  レイアウト仕様化 (phase-5) で当たり判定込みの確認が要る。Native iOS / Android・MAUI Android では中心で反応する
- **多段表示は MAUI / KMP の 4 セルでは未観測**。シナリオ表の規約どおり Native 2 セルを基準とし、
  MAUI / KMP は委譲による継承 + adapter 契約テストを根拠にしている。委譲経由で崩れる挙動は観測していない
- **初期状態 (一度も閉じていない) の結果表示エリアを非表示にした判断**は、実装者判断のままオーナー確認が残る
- ボタン高さ 38pt/dp が HIG 44pt・Material 48dp を下回る件は mock 忠実を優先した既知の妥協 (`verification/notes.md`「モックとの差」)
