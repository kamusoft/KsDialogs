# Proposal: add-toast

## Why

Toast は原典 AiForms.Maui.Dialogs の主要3機能の最後の1つで、原典では理由の記載なく Obsolete 宣言されていた (状況証拠: Android 実装が OS Toast のカスタム View 機構に依存し、API 30 で非推奨化)。phase-8 の議論で全8論点が解消済み (core/ADR-0028〜0032 起票済み、演出適用形など ADR 化しない決定は agenda 決定事項に記録) であり、本変更は OS Toast 依存を捨てた自前の器による再設計 Toast を4形態 (iOS / Android / MAUI / KMP) に新実装する。

## What Changes

- **Toast 公開 API** (core/ADR-0002 踏襲: 契約 interface + 既定シングルトンエントリ): `show(message)` 一発のメッセージ入口と、カスタム View 表示 (型指定レジストリ + インライン factory — core/ADR-0029) を全4形態に追加。show は戻り値なしの fire-and-forget、duration は show 引数の ms 指定 (既定 1500ms、原典 Android の 3.5 秒クランプは継承しない — core/ADR-0031)
- **非モーダルの Toast 器** (core/ADR-0030): Loading の器 (提示スタック不参加 — iOS = key window 直貼り / Android = 全画面透過 Window) をタッチを奪わない非モーダル版として派生。Toast の面もタッチ素通し (完全非対話 — core/ADR-0031)。1 Toast 1器で多重起動は追加順に重なり、Loading とは追加順によらず常に Loading が前面。Dialog との前後は core/ADR-0006 の線で保証しない
- **デフォルト View** (core/ADR-0028・0032): 半透明ダークピル + 白文字のライブラリ同梱コンテンツ (Loading の内蔵コンテンツ — core/ADR-0023 — と同じ共有経路)。既定配置は visibleArea 下部中央 + ボトムバー回避オフセット (Dialog の既定=中央からの意図的乖離 — core/ADR-0008 の線で明示)
- **ToastStyle** (core/ADR-0032): 背景色・文字色・フォントサイズ・角丸・既定 duration・アプリ既定配置を一括設定する値オブジェクト。規律は LoadingStyle (core/ADR-0023) と同型 (show 引数にしない・各表示開始時に読む・KMP commonMain から設定不可)。「アプリ既定配置」は Toast 固有項目 (タブバー等のアプリ側クロームは visibleArea では避けられないため)
- **演出**: カスタム Toast は `DialogTransition` 添付可、デフォルト View は器の既定クロスフェード固定 (Loading と完全同型。Toast は結果を持たないため core/ADR-0017 の結果ラッチ・配送は対象外)
- **Sample 通し**: デモ5項目 (メッセージ / カスタム View / 多重起動 / 配置・duration 変更 / 機能間多重起動 = Dialog・Loading・Toast 同時表示での重なり規則と非モーダル併存の検証) をパリティ準拠で4ルートに追加 (フェーズ完了条件 — cross/ADR-0007・0010)
- **テスト**: 新しい Scenario 領域プレフィックスで挙動テストを追加し、公開 API 形状検査を拡張

影響する能力: dialog-contract / ios-native / android-native / maui-binding / kmp-facade / samples

## Non-Goals

- **対話可能 Toast (タップ消し・カスタム View 内のボタン)** — 却下決定 (core/ADR-0031)。ページをまたいで生き残る Toast では対話要素のページ文脈が壊れる。対話が欲しいケースは Dialog が受け皿。将来必要になれば opt-in で非破壊追加できる
- **デフォルト View への演出選択の口** — 却下決定 (phase-8 演出論点 案B)。演出の供給経路を添付スロット1本に保つ。将来は ToastStyle への追加で互換対応可能
- **タブバー等のアプリ側 UI の自動検知・回避** — 却下決定 (core/ADR-0032 代替案)。器はページ構造を知らない設計 (core/ADR-0030) に反し、3形態×各 UI 系の検知は脆すぎる。逃げ道は ToastStyle のアプリ既定配置
- **OS ネイティブ Toast への委譲 (Android)** — 却下決定 (core/ADR-0030 代替案)。原典の死因と制約 (API 30 カスタム View 非推奨・3.5 秒クランプ・SetGravity 配置・多重直列化) を持ち込まない
- **Toast の結果通知・await** — 却下決定 (core/ADR-0031)。fire-and-forget に限定
- **原典互換 shim (`IToast.Show<TView>` 形状の再現)** — cross/ADR-0001 (独立ブランド・ベタ移植しない) の線

## Impact

- **破壊的変更なし**: 純粋な機能追加。既存 Dialog / Loading の公開 API・挙動には触れない
- 器は Loading 器の部品 (共有ホスティング・`DialogLayoutApplier`・`DialogTransitionRunner`) をそのまま使う見込みで、切り出し済み (core/ADR-0026) のため大規模リファクタリングは伴わない想定
- 互換面への波及: MAUI bridge (Swift / Kotlin) と KMP interop (ObjC ブリッジ) に Toast API 一式を追加する
- リスク: (1) **「Loading が常に前面」の順序規則は追加順だけでは成立しない** — Loading 表示中に出た Toast 器が上に載るため、器の載せ替え・挿入位置制御の実現経路を design で形態別に確定する必要 (iOS は subview 挿入位置で制御可能見込み、Android の Window 序列は再掲込みで要検討 — coordinator (core/ADR-0027) の再取り付け機構が流用候補)。(2) Android のタッチ素通し Window (非フォーカス + not-touchable) と全画面透過 Window の組み合わせは新規領域で、IME・システムジェスチャとの干渉を検証する必要。(3) 多重 Toast の器がそれぞれ回転・Activity 再生成をまたぐ挙動 (Loading の器使い捨て + 再取り付け方式の複数枚版) は実装論点

## 級: L

4形態の公開 API に新しい機能面を追加し、複数能力 (契約 + 4実装 + samples) にまたがり、非モーダル器の新設と機能間の前後規則という新規領域を含むため。

domain: cross
roadmap: library-foundation/phase-8-toast-rebuild
