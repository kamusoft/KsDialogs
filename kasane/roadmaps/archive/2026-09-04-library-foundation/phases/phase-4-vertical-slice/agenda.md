# 縦串スライス + 最小 Sample + パリティ規約確定

シンプルな Dialog 1本を core 契約 + iOS Native + Android Native + MAUI binding + KMP API の全形態で貫通させ、最小 Sample を通す change フェーズ (KsSettingsView phase-1-native-bridge の LabelCell 縦疎通の写像)。全公開形態のアーキテクチャリスクをここで潰す。

## 論点

(全論点解消済み — 2026-08-14)

## 決定事項

- **縦串の題材: 最小カスタム View + 型付き結果通知あり** (2026-08-14) — カスタム View = ラベル1個 + OK/Cancel ボタン、VM = メッセージ文字列1個、結果 = completed(単純値) / cancelled。固定コンテンツでは VM 型キー → View factory レジストリ (core/ADR-0004) と Swift async → Kotlin suspend の結果経路 (core/ADR-0003) を通らず縦串の意味が薄れるため、最大リスク2箇所を踏む最小形を題材とする
- **全形態貫通の受け入れ条件: 手動確認 + 結果経路のみ自動テスト** (2026-08-14) — (1) 4ルート (iOS Native / Android Native / MAUI / KMP) の Sample から show → 表示 → OK で completed(値) / Cancel・外タップで cancelled を手動確認しスクショを artifacts に記録 (ui.screenshot 実測を兼ねる) (2) KMP は iOS / Android 両ターゲットで確認 (3) 結果経路 (特に Swift async → Kotlin suspend) は正しい型・値で返ることを自動テストで検証 (4) BuildProbe 削除後もビルドが通る。UI 自動テストの整備は縦串に過剰なため phase-5 以降
- **KMP の Swift async 変換は素の suspend 直接公開とし、第三者依存 (KMP-NativeCoroutines) を導入しない** (2026-08-14, kmp/ADR-0001) — 縦串の疎通確認で自動変換の粗 (nullable 化・sealed 型の見え方・エラーチャネル) が受け入れ条件を割ったら自前 @objc completion ラッパーにフォールバック
- **KMP の expect/actual 境界: commonMain は純粋な契約のみ、expect は既定 singleton エントリだけ、実体 (レジストリ含む) は各 Native lib へ全委譲** (2026-08-14, kmp/ADR-0002) — レジストリ実体は OS ごとに Native lib 側1個で、純 Native 利用者と KMP 利用者が共有 (core/ADR-0004 の成立形)。iosMain actual は Swift lib の @objc 互換面へ cinterop 委譲
- **共通仕様テストの器の初版: シナリオ表 + 同名テスト規約** (2026-08-14) — 共通仕様シナリオ表 (シナリオ ID + 前提 + 操作 + 期待される観察可能な結果 + OS 差の記録欄) を core の仕様文書として置き、各 platform はシナリオ ID を冠した同名テストで実装 (UI 操作が要るものは手動確認 + artifacts 記録可)。初版シナリオは phase-3 申し送りの4項目: (a) 2枚重ね→上から順に閉じる = 期待値あり (b) 下を先に閉じる = 実挙動記録枠 (iOS は提示元の動的解決で上の1枚が消える示唆) (c) 重ね出し中の外タップ→手前のみ cancelled = 期待値あり (d) Android 戻るボタンのキーボード表示中無視 = 判断待ち枠。データ駆動の共有テストベクター化は属性セット確定後 (phase-5 以降) に再検討
- **MAUI binding 構成: 使い捨て Bridge + 標準ビルドアイテム優先 + gateway 抽象踏襲** (2026-08-14, maui/ADR-0001・0002。先例要約は [artifacts/scout-kssettingsview-precedents.md](artifacts/scout-kssettingsview-precedents.md)) — (1) Bridge は Store 非搭載の呼び出しスコープ (show/dismiss の操作 1:1、show ごとの completion 1本、MAUI 慣例型公開・DTO 非公開)。MAUI レジストリは C# 層 (VM 型 → MAUI View factory) で、show 時に platform view へ実体化して Native lib へ渡す — Native 側レジストリとは層が別 (2) iOS は標準 XcodeProject アイテム。Android は単一モジュール (:ksdialogs) のため標準 AndroidGradleProject を実測し、失敗時のみ gradlew Exec + AndroidLibrary 束縛へフォールバック (3) TargetFrameworks に素の net10.0 を含め、Bridge 呼び出しは internal gateway 越し。fake gateway で結果経路の自動テストを素の dotnet test で回す
- **Sample 構成: 集約 samples/ 方式 + 先例の consumer 境界踏襲 + KMP Sample 新設** (2026-08-14, cross/ADR-0006) — samples/ios (Local Swift Package 参照) / samples/android (composite build + dependencySubstitution 明示) / samples/maui (facade への ProjectReference 1本) / samples/kmp (shared + androidApp + iosApp の3点構成、KMP 初の実物)。Sample を配布物・挙動契約の SSoT・自動テストの代替として扱わない禁止事項も踏襲
- **sample-parity 規約: KsSettingsView 規約を4ルート対応で翻案し、一致単位を「デモ項目」に再定義** (2026-08-14, cross/ADR-0007) — デモ項目 = ルートメニューの起動項目文言 + ダイアログ内容 (タイトル・本文・ボタン文言・デモデータ) + 結果表示のワンセット。対応はメニュー項目文言で取る。4ルート全部で同一メニュー構成・同一文言、KMP Sample の Native View は共有せず各自書く。色は共通 SampleTheme の同一 RGBA、許容差異・例外枠・禁止事項は先例踏襲。縦串の初版実物はメニュー1項目「Basic Dialog」で、蒸留時に実物を根拠に cross/conventions/sample-parity.md として concepts 化

## TODO

- [x] 論点の解消 (2026-08-14 全7論点を決定事項へ昇格)
- [x] ksn-propose で変更提案を起こす (2026-08-14 [add-vertical-slice](../../../../changes/add-vertical-slice/proposal.md) として作成。L 級一式 + mock 承認 + second-opinion 反映済み)

以下の申し送りは add-vertical-slice の tasks.md へ移管済み (実装フェーズで消化):

- [ ] Android 標準 `AndroidGradleProject` アイテムの成立を実測する → **tasks 5.1 / 5.2** (結果込みでビルド連携 ADR を起票、成立時は KsSettingsView へ知見還元)
- [ ] phase-1 申し送り: 最優先疎通確認 (キー同一性 / Swift async 変換) → **tasks 4.4 / 4.5** (4.4 は崩れたら実装停止の条件付き)
- [ ] phase-2 申し送り: BuildProbe 4ルート削除 → **tasks 7.1**
- [ ] phase-3 申し送り: ui.screenshot の実測記載 → **tasks 7.3**
- [ ] phase-2 申し送り: KMP deployment target 非保証フラグ → **tasks 4.7** (iOS 17 宣言を受け入れ条件化、TODO 送り禁止に強化)

## 実装結果 (2026-08-15 反映)

[add-vertical-slice](../../../../changes/archive/2026-08-15-add-vertical-slice/proposal.md) として実装完了 (レビュー2周 + 相方セカンドオピニオン、verify VALID)。Basic Dialog が4形態に貫通し、縦串の狙いだったリスクは全件実測で決着: キー同一性成立 (実 framework 越し確認)・Swift async 変換フォールバック不発動・MAUI Android 標準アイテム不成立 → gradlew Exec (maui/ADR-0003)。deviation 3件 (Swift 直接 await の phase-5 送り / KMP iosApp の内部互換面直接使用 / MD-b の OS 差明文化)。

申し送りのルーティング:

- Swift 向け KMP 面の一括設計・View 直接渡し系統・MAUI Register の書き味・MAUI iOS 当たり判定・Sample UI 未決4件・MD-d 期待値確定 → [phase-5 agenda](../phase-5-dialog-completion/agenda.md) の論点に追記済み
- deployment target 非保証フラグの正統化・リポジトリ内生成物 (.swiftpm-locks 等) のコミット方針 → [phase-10 agenda](../phase-10-packaging-model/agenda.md) の論点に収容済み
- 見送りとした申し送り: なし
