# Proposal: add-loading

## Why

Loading (処理中の操作ブロックとインジケータ表示) は原典 AiForms.Maui.Dialogs の主要3機能 (Dialog / Loading / Toast) の1つで、現状未実装。phase-7 の議論で設計判断は8論点すべて確定済み (core/ADR-0022〜0024 起票済み、ADR 化しない決定は agenda 決定事項に記録) であり、本変更はそれらを4形態 (iOS / Android / MAUI / KMP) に実装する。器を Dialog の提示スタックから分離する設計のため、Dialog 側の機能拡張と独立に完了できる。

## What Changes

- **Loading 公開 API** (core/ADR-0002 踏襲: 契約 interface + 既定シングルトンエントリ): show / hide / メッセージ更新と、処理ブロックを渡すスコープ形 (原典 StartAsync 相当) を全4形態に追加。進捗報告口は形態別イディオム (C# = IProgress&lt;double&gt; / Swift = クロージャ / Kotlin・KMP = 関数型)、値は 0〜1・表示は後勝ち
- **Loading 専用オーバーレイ器** (core/ADR-0022): OS の提示スタック (present / Dialog 積み重ね) に載せない専用の器を新設し、Dialog の共有部品 (コンテンツホスティング・レイアウト規則・トランジション添付) を再利用する。レイアウト系の器メタ属性は Dialog と同じ意味・同じ供給経路で効き、isCanceledOnTouchOutside のみ常に無効 (ユーザー操作では閉じない)
- **既定ローディングの内蔵コンテンツ** (core/ADR-0023): スピナー + メッセージ + 進捗表示のライブラリ同梱コンテンツ。styling (インジケータ色・フォント・既定メッセージ・進捗フォーマット) は一括設定のスタイル値オブジェクトで受け、各表示の開始時に読む
- **多重利用の合流** (core/ADR-0024): 重なった利用は1つの表示に合流し、表示は最初の開始から最後の終了まで。渡された処理は必ず実行。明示 hide は即閉じ
- **カスタム View 版**: Dialog と同じ登録・添付機構の上でカスタム Loading View を表示 (使い捨てモデル — core/ADR-0005)。`DialogTransition` 添付で演出差し替え可 (既定ローディングは既定クロスフェード固定)
- **Sample 通し**: 既定・カスタム両方のデモをパリティ準拠で4ルートに追加 (フェーズ完了条件)
- **テスト**: 新しい Scenario 領域プレフィックスで挙動テストを追加し、公開 API 形状検査 (正・負) を拡張

影響する能力: dialog-contract / ios-native / android-native / maui-binding / kmp-facade / samples

## Non-Goals

- **表示スコープ (原典 isCurrentScope 相当)** — 移植しない決定 (phase-7 論点5)。原典で iOS のみ有効の半端な機能で、必要になれば非破壊追加できる
- **回転ロック (原典 AutoRotateForIOS 系)** — 導入しない決定 (phase-7 論点7)。原典の Loading では事実上ノーオペだったと判明。表示中の回転は既存の再配置規則に従う
- **再利用インスタンス (原典 Create* / IsReusable 相当)** — core/ADR-0005 (使い捨て一本化) により持ち込まない
- **既定ローディングへの演出設定の開放** — 却下決定 (phase-7 論点6 案C)。演出の供給経路を添付スロット1本に保つ
- **進捗のストリーム API (Flow / AsyncSequence 受け)** — 却下決定 (phase-7 論点3 案C)。スコープ形で足りる
- **Toast** — 別フェーズ (phase-8-toast-rebuild) の責務

## Impact

- **破壊的変更なし**: 純粋な機能追加。既存 Dialog の公開 API・挙動には触れない
- Dialog の器 (Container) 内部に埋まっているトランジション実行・レイアウト適用の部品を Loading 器から使えるよう切り出すリファクタリングを伴う見込み — 既存テスト (Scenario PB/MB 97本 + レイアウト共通ケース表) が回帰ガードになる
- 互換面への波及: MAUI bridge (Swift / Kotlin) と KMP interop (ObjC ブリッジ) に Loading API 一式を追加する
- リスク: (1) トランジション実行部の切り出しコストは未計測 (器と密結合の可能性)。(2) Loading 器の直貼り実装で insets・回転再配置 (PB-WN 系相当) を Dialog と同水準で再現する必要。(3) 合流カウントと非同期キャンセルの絡み (処理中の hide・例外) はスペックで契約を固めてから実装する

## 級: L

4形態の公開 API に新しい機能面を追加し、複数能力 (契約 + 4実装 + samples) にまたがり、器の新設と共有部品の切り出しを含むため。

domain: cross
roadmap: library-foundation/phase-7-loading
